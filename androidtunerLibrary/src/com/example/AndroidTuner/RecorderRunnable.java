package com.example.AndroidTuner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import com.example.AndroidTuner.PitchDetector.PitchListener;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class RecorderRunnable implements Runnable {
	private static String LOG_TAG = "RecorderRunnable";
	
	public final int maxTime = 60; // in seconds
	public final int readSamples = 0x100;
	private final static int CHANNEL_MODE = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private final static int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	public final int totalSamples;
	public final int fftChunkSize;
	public int rate;
	public PitchListener pcl_;
	
	private short[] audioData;
	private int filled;
	private ReentrantLock audioReadLock;
	private AudioRecord recorder_;
	
	private BlockingQueue<Integer> queue;
	
	private int lastReadIndex;
	
	public RecorderRunnable(BlockingQueue<Integer> queue_, int rate, int fftChunkSize, PitchListener pcl) {
		super();
		queue = queue_;
		totalSamples = maxTime * rate;
		this.rate = rate;
		this.fftChunkSize = fftChunkSize;
		lastReadIndex = 0;
		pcl_ = pcl;
		
		audioReadLock = new ReentrantLock();
	}
	
	public int get(short[] buffer, int skip, int howMany) throws InterruptedException {
		//get
		// from lastReadIndex + skip -> lastReadIndex + skip + howMany
		// 
		lastReadIndex += skip;
		
		// wait for (lastReadIndex + howMany) to be available
		while (filled < lastReadIndex + howMany) {
			queue.take();
		}
		
		while(null != queue.poll()) {
			// nop
		}
		
		// take it
		// NOTE: if the lock was taken between here and above it's ok.
		audioReadLock.lock();
		try {
			System.arraycopy(audioData, lastReadIndex, buffer, 0,
					howMany);
		} finally {
			audioReadLock.unlock();
		}
		
		// how many left over
		return filled - lastReadIndex;
	}
	
	public short[] get(int howMany) {
		short[] audioBuffer = new short[howMany];

		audioReadLock.lock();
		try {
			System.arraycopy(audioData, filled - howMany, audioBuffer, 0,
					howMany);
		} finally {
			audioReadLock.unlock();
		}

		return audioBuffer;
	}
	
	public short[] getLatest(int howMany) throws InterruptedException {
		// wait so you get something new and not something old twice.
		queue.take();
		
		if (filled < howMany) {
			while (true) {
				queue.take();
				if (filled >= howMany) {
					// got enough stuff to give back.
					break;
				}
			}
		}
		
		// empty the queue now so we don't have it fill up needlessly.
		// A better solution would be a lock and not a queue, maybe a TODO.
		while(null != queue.poll()) {
			// nop
		}
		
		return get(howMany);
		
	}
	
	public void reset() {
		audioReadLock.lock();
	     try {
	         // fill yourself from the end
	    	 System.arraycopy(audioData, filled - fftChunkSize, audioData, 0, fftChunkSize);
	    	 filled = fftChunkSize;
	    	 lastReadIndex = 0;
	     } finally {
	    	 audioReadLock.unlock();
	     }
	}
	
	@Override
	public void run() {
		int res;
		filled = 0;
		audioData = new short[totalSamples];
		
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		recorder_ = new AudioRecord(AudioSource.MIC, rate, CHANNEL_MODE,
				ENCODING, 6144);
		
		if (recorder_.getState() != AudioRecord.STATE_INITIALIZED) {
			pcl_.onError("Can't initialize AudioRecord");
			return;
		}
		
		recorder_.startRecording();
		try {
			while (!Thread.interrupted()) {
				if (readSamples + filled > totalSamples) {
					reset();
				}
				
				//res = recorder_.read(audio_data, 0, audio_data.length);
				res = recorder_.read(audioData, filled, readSamples);
				if((res == AudioRecord.ERROR_INVALID_OPERATION) || (res == AudioRecord.ERROR_BAD_VALUE)) {
					Log.e(LOG_TAG, "audio record failed: " + res);
					pcl_.onError("failed reading audio");
					return;
				}
				
				if (res == 0) {
					pcl_.onError("failed reading audio zero buffer");
					return;
				}
				
				filled += res;
				queue.put(res);
				
			}
		} catch (InterruptedException e) {
			Log.e(LOG_TAG, "recording interrupted");
			e.printStackTrace();
		} finally {
			Log.e(LOG_TAG, "RecorderRunnable interrupted.");
			recorder_.stop();
		}
	}
	
}	
