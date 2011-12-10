
package com.example.AndroidTuner;

import java.lang.Runnable;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.SystemClock;
import android.util.Log;

public class PitchDetector implements Runnable {
	private static String LOG_TAG = "PitchDetector";

	private PitchListener mPitchListener;
	
	// Currently, only this combination of rate, encoding and channel mode
	// actually works.
	public static final int fftChunkSize = 0x1000;
	private final static int RATE = 8000;
	//private final static int RATE = 44100;
	
	
	private final static int BUFFER_SIZE_IN_MS = 3000;
	private final static int CHUNK_SIZE_IN_SAMPLES = 4096; // = 2 ^	// CHUNK_SIZE_IN_SAMPLES_POW2
	private final static int CHUNK_SIZE_IN_MS = 1000 * CHUNK_SIZE_IN_SAMPLES / RATE;
	private final static int BUFFER_SIZE_IN_BYTES = RATE * BUFFER_SIZE_IN_MS / 1000 * 2;
	private final static int CHUNK_SIZE_IN_BYTES = RATE * CHUNK_SIZE_IN_MS / 1000 * 2;

	
	public final static int MIN_FREQUENCY = 49; // 49.0 HZ of G1 - lowest note
													// for crazy Russian choir.
	public final static int MAX_FREQUENCY = 1568; // 1567.98 HZ of G6 - highest
													// demanded note in the
													// classical repertoire
	
	public final static int SPECTRUM_HZ = MAX_FREQUENCY - MIN_FREQUENCY;
	
	private final static int DRAW_FREQUENCY_STEP = 5;
	
	
	public static native void DoFFT(double[] data, int size); // an NDK library
														// 'fft-jni'
	
	// measured by yelling in to the mic
	public static double MAX_AMPLITUDE = 1.2e5;
	
	// this first noise value is ignored, eg 2e12 is a normal amplitude for singing.
	public static double noiseLevel = 4e4;
	public static boolean mIsNoiseInitialized = false;
	
	public static double FFTPerSecond = 0;
	public int skip = 1;

	public interface PitchListener{
		public void onAnalysis(FreqResult fr);
		public void onError(String error);
	}
	
	public PitchDetector(PitchListener pcl) {
		super();
		this.mPitchListener = pcl;
		System.loadLibrary("fft-jni");
	}


	public void resetNoiseLevel() {
		mIsNoiseInitialized = false;
	}
	
	public void run() {
		Log.e(LOG_TAG, "starting to detect pitch");

		LinkedBlockingQueue<Integer> samplesReadQueue = new LinkedBlockingQueue<Integer>();
		RecorderRunnable recorder = new RecorderRunnable(samplesReadQueue, RATE, fftChunkSize, mPitchListener);
		Thread recThread = new Thread(recorder);
		recThread.start();
		
		short[] audioBuffer;
		//audioBuffer = new short[fftChunkSize];
		skip = 800;
		long startTime;
		int leftOver = 0;
		int lastLeftOver = 0;
		while (!Thread.interrupted()) {
			startTime = SystemClock.elapsedRealtime();
			//short[] audio_data = new short[BUFFER_SIZE_IN_BYTES / 2];
			//recorder_.read(audio_data, 0, CHUNK_SIZE_IN_BYTES / 2);
			//short[] audio_data = new short[CHUNK_SIZE_IN_BYTES / 2];
			// 4096=0x1000, 8192=0x2000
			//short[] audio_data = new short[0x1000];
			
			try {
				audioBuffer = recorder.getLatest(fftChunkSize);
				//leftOver = recRun.get(audioBuffer, skip, fftChunkSize);
			} catch (InterruptedException e) {
				Log.e(LOG_TAG, "InterruptedException for getting audio data.");
				e.printStackTrace();
				break;
			}
			
			/*if (leftOver > lastLeftOver) {
				// catch up!
				skip += 10;
			} else {
				// you're ahead, do more fft's.
				skip -= 10;
				if (skip < 100) {
					skip = 100;
				}
			}
			lastLeftOver = leftOver;*/
			
			// NOTE: It's critical get only the newest samples here. Otherwise
			// we could accidentally thrash insanely fast if the recorder_ was on for too long.
			// and now we have a lot of reads to go through.
			// A solution is to stop/startRecording.
			
			// TODO: implement a solution with setRecordPositionUpdateListener or something
			// similar to be able to read only the latest parts of the buffer without
			// startRecording/stop.
			
			
			//long prerecTime = SystemClock.elapsedRealtime();
			
			//short [] subArray = Arrays.copyOfRange(audio_data, 4, 6);
			FreqResult fr = AnalyzeFrequencies(audioBuffer);
			this.mPitchListener.onAnalysis(fr);
			FFTPerSecond = 1000 / (SystemClock.elapsedRealtime() - startTime);
			
			//Log.e(LOG_TAG, "calced in: " + (SystemClock.elapsedRealtime() - startTime) + " " + (SystemClock.elapsedRealtime() - prerecTime));
		}
		
		Log.e(LOG_TAG, "PitchDetector interrupted.");
		recThread.interrupt();
	}	
	
	

	
	public static class FreqResult {
		public HashMap<Double, Double> frequencies;
		public double bestFrequency;
		public double maxAmp;
		public boolean isPitchDetected;
		public double noiseLevel;
		
		public FreqResult() {
			this.frequencies = null;
			this.bestFrequency = 0.0;
			this.isPitchDetected = false;
			this.noiseLevel = 0.0;

		}
		
//		
//		public FreqResult(HashMap<Double, Double> frequencies, double bestFrequency, boolean isPitchDetected) {
//			this.frequencies = frequencies;
//			this.bestFrequency = bestFrequency;
//			this.isPitchDetected = isPitchDetected;
//		}
//		
		
		@Override public String toString() {
			return "<FreqResult: " + bestFrequency + " Hz>";
		}
		
		public void destroy() {
			frequencies.clear();
		}
	}

	public static class FrequencyCluster {
		public double average_frequency = 0;
		public double total_amplitude = 0;
		
		public void add(double freq, double amplitude) {
			double new_total_amp = total_amplitude + amplitude;
			average_frequency = (total_amplitude * average_frequency + freq * amplitude) / new_total_amp;
			total_amplitude = new_total_amp;
		}
		
		public boolean isNear(double freq) {
			if (Math.abs(1 - (average_frequency / freq)) < 0.05) {
				// only 5% difference
				return true;
			} else {
				return false;
			}
		}
		
		public boolean isHarmonic(double freq) {
			double harmonic_factor = freq / average_frequency;
			double distance_from_int = Math.abs(Math.round(harmonic_factor) - harmonic_factor);
			if (distance_from_int < 0.05) {
				// only 5% distance
				return true;
			} else {
				return false;
			}			
		}

		public void addHarmony(double freq, double amp) {
			double exactHarmonicFactor = Math.round(freq / average_frequency);
			double newFrequency = freq / exactHarmonicFactor;
			this.add(newFrequency, amp);
			//total_amplitude += amp;
			//currentCluster.total_amplitude += nextCluster.total_amplitude * 10;
			
		}
		
		@Override public String toString() {
			return "(" + average_frequency + ", " + total_amplitude + ")";
		}
	}
	
	public FreqResult AnalyzeFrequencies(short[] audio_data) {
		if (audio_data.length * 2 < 0) {
			Log.e(LOG_TAG, "awkward fail: " + (audio_data.length * 2));
		}
		
		double[] frequencyData = new double[audio_data.length * 2];
		
		//final int min_frequency_fft = Math.round(MIN_FREQUENCY * CHUNK_SIZE_IN_SAMPLES / RATE);
		//final int max_frequency_fft = Math.round(MAX_FREQUENCY * CHUNK_SIZE_IN_SAMPLES / RATE);
		
		
		// TODO: Somewhere in this for loop there's a crash!
		//for (int i = 0; i < CHUNK_SIZE_IN_SAMPLES; i++) {
		for (int i = 0; i < audio_data.length; i++) {
			frequencyData[i * 2] = audio_data[i];
			frequencyData[i * 2 + 1] = 0;
		}
		
		
		//DoFFT(data, CHUNK_SIZE_IN_SAMPLES);
		DoFFT(frequencyData, audio_data.length);
		
		
		return AnalyzeFFT(audio_data.length, frequencyData);
	}
	
	public FreqResult AnalyzeFFT(int audioDataLength, double[] frequencyData) {
		boolean pitchDetected = false;
		double best_frequency = 0;
		double bestAmplitude = 0;
		HashMap<Double, Double> frequencies = new HashMap<Double, Double>();
		List<Double> bestFrequencies = new ArrayList<Double>();
		List<Double> bestAmps = new ArrayList<Double>();
		
		FreqResult fr = new FreqResult();

		// final double normalFreqAmp = Math.pow(MIN_FREQUENCY * MAX_FREQUENCY, 0.5);
		final int min_frequency_fft = (int) Math.round(1.0 * MIN_FREQUENCY * audioDataLength / RATE);
		final int max_frequency_fft = (int) Math.round(1.0 * MAX_FREQUENCY * audioDataLength / RATE);

		
		for (int i = min_frequency_fft; i <= max_frequency_fft; i++) {

			final double currentFrequency = i * 1.0 * RATE
					/ audioDataLength;
			
			// round to nearest DRAW_FREQUENCY_STEP (eg 63/64/65 -> 65 hz)
			//final double draw_frequency = Math
			//		.round(current_frequency
			//				/ DRAW_FREQUENCY_STEP)
			//		* DRAW_FREQUENCY_STEP;

			final double current_amplitude = Math.pow(frequencyData[i * 2], 2)
					+ Math.pow(frequencyData[i * 2 + 1], 2);
			
			//final double normalized_amplitude = current_amplitude * normalFreqAmp / current_frequency;
			final double normalizedAmplitude = Math.pow(current_amplitude, 0.5) / currentFrequency;

			// divide the amps to bins for drawing later
			//Double current_sum_for_this_slot = frequencies.get(draw_frequency);
			//if (current_sum_for_this_slot == null) {
			//	current_sum_for_this_slot = 0.0;
			//}
			
			//frequencies.put(draw_frequency, Math.pow(current_amplitude, 0.5) / draw_frequency_step + current_sum_for_this_slot);
			//frequencies.put(draw_frequency, normalized_amplitude / DRAW_FREQUENCY_STEP + current_sum_for_this_slot);
			frequencies.put(currentFrequency, normalizedAmplitude);
			
			
			// find peaks
			// NOTE: this finds all the relevant peaks because their
			//		amplitude usually keeps rising with the frequency.
			if (normalizedAmplitude > bestAmplitude) {
				
				// it's important to note the best_amplitude also for noise level measurement.
				best_frequency = currentFrequency;
				bestAmplitude = normalizedAmplitude;
				
				// make sure this isn't the 48.44970703125 FFT artifact
				// and that this isn't some background noise
				if ((currentFrequency > MIN_FREQUENCY)  && (normalizedAmplitude > noiseLevel)) {
					bestFrequencies.add(currentFrequency);
					bestAmps.add(bestAmplitude);
				}
			}
			// test for harmonics
			// e.g. 220 is a harmonic of 110, so the harmonic factor is 2.0
			// and thus the decimal part is 0.0.
			//		230 isn't a harmonic of 110, the harmonic_factor would be 
			//		2.09 and 0.09 > 0.05
			//double harmonic_factor = current_frequency / best_frequency;
			//if ((best_amplitude == 0) || (harmonic_factor - Math.floor(harmonic_factor) > 0.05)) {
		}

		best_frequency = clusterFrequencies(bestFrequencies, bestAmps);
		
		
		if ( (bestAmplitude > noiseLevel) && (best_frequency > 0)) {
			pitchDetected = true;
		}
		
		if ( ! mIsNoiseInitialized) {
			// the first sample + 50% means we catch some jitter in the noise amplitude too.
			noiseLevel = bestAmplitude * 1.5;
			mIsNoiseInitialized = true;
			if (noiseLevel > MAX_AMPLITUDE / 2) {
				noiseLevel = MAX_AMPLITUDE / 2;
				mPitchListener.onError("Noise levels are too high.");
			}
		}
		
		fr.bestFrequency = best_frequency;
		fr.frequencies = frequencies;
		fr.isPitchDetected = pitchDetected;
		fr.noiseLevel = noiseLevel;
		fr.maxAmp = bestAmplitude;
		
		//data = null;
		//System.gc();
		return fr;		
	}

	public static double clusterFrequencies(List<Double> bestFrequencies, List<Double> bestAmps) {

		List<FrequencyCluster> clusters = new ArrayList<FrequencyCluster>();
		FrequencyCluster currentCluster = new FrequencyCluster();
		clusters.add(currentCluster);
		
		
		if (bestFrequencies.size() > 0)
		{
			currentCluster.add(bestFrequencies.get(0), bestAmps.get(0));
		}
		
		// init clusters and
		// join near clusters
		for(int i = 1; i < bestFrequencies.size(); i++)
		{
			double freq = bestFrequencies.get(i);
			double amp = bestAmps.get(i);
			
			//if (currentCluster.isNear(freq)) {
			//	currentCluster.add(freq, amp);
			//	continue;
			//}
			
			// this isn't near
			// NOTE: assuming harmonies are consecutive (no unharmonics in between harmonies)
			currentCluster = new FrequencyCluster();
			clusters.add(currentCluster);
			currentCluster.add(freq, amp);
		}
		
		
		
		// join harmonies
		// there should be only 6-10 peaks, so this isn't too bad that it's O(n�)
		List<FrequencyCluster> harmonies = new ArrayList<FrequencyCluster>();
		FrequencyCluster nextCluster;
		for(int i = 0; i < clusters.size(); i ++) {
			currentCluster = clusters.get(i);
			for(int j = i + 1; j < clusters.size(); j++) {
				nextCluster = clusters.get(j);
				if (currentCluster.isHarmonic(nextCluster.average_frequency)) {
					// Give harmonies a x10 bonus point because I see there are strange stuff like
					// 12.2222 and 12.444 harmonies which are the strongest but obviously wrong.
					currentCluster.addHarmony(nextCluster.average_frequency, nextCluster.total_amplitude);
					//harmonies.add(currentCluster);
					//harmonies.add(nextCluster);
				}
			}
		}
		
		// integrate harmonies to boost the relevant clusters.
		//for(int i = 1; i < harmonies.size(); i ++) {
		//	FrequencyCluster harmony = harmonies.get(i);
		//	harmonies.get(0).addHarmony(harmony.average_frequency, harmony.total_amplitude);
		//}
		
		// find best cluster
		double bestClusterAmplitude = 0;
		double bestFrequency = 0;
		for(int i = 0; i < clusters.size(); i ++) {
			FrequencyCluster clu = clusters.get(i);
			if (bestClusterAmplitude < clu.total_amplitude) {
				bestClusterAmplitude = clu.total_amplitude;
				bestFrequency = clu.average_frequency;
			}
		}
		
		return bestFrequency;
	}

	
}


/** Copyright (C) 2009 by Aleksey Surkov.
 **
 ** Permission to use, copy, modify, and distribute this software and its
 ** documentation for any purpose and without fee is hereby granted, provided
 ** that the above copyright notice appear in all copies and that both that
 ** copyright notice and this permission notice appear in supporting
 ** documentation.  This software is provided "as is" without express or
 ** implied warranty.
 */
