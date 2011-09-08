package com.example.AndroidTuner;

import java.util.Iterator;
import java.util.Map.Entry;

import com.example.AndroidTuner.PitchDetector.FreqResult;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class Histogram {
	private final static int MIN_AMPLITUDE = 40000;
	//private final static int MAX_AMPLITUDE = 3200000;
	private final static int MAX_AMPLITUDE = 20000;
	private final static int MAX_LN_AMP = 30;
	public static Paint paint = new Paint();
	
	private static long GetAmplitudeScreenHeight(Canvas canvas, double amplitude, Rect histogram_rect) {
		return Math.round(amplitude / MAX_AMPLITUDE * histogram_rect.height());
		//return Math.round(Math.log(amplitude) / MAX_LN_AMP * histogram_rect.height());
	}
	

	public static boolean drawHistogram(Canvas canvas, Rect rect, FreqResult fr) {
		if (fr.frequencies == null) {
			return false;
		}
		
		// Draw border.
		paint.setARGB(80, 200, 200, 200);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(rect, paint);
		
		// Draw threshold.
		paint.setARGB(180, 200, 0, 0);
		//final long threshold_screen_height = GetAmplitudeScreenHeight(canvas, MIN_AMPLITUDE, rect);
		//final long threshold_screen_height = GetAmplitudeScreenHeight(canvas, Math.sqrt(fr.noiseLevel), rect);
		final long threshold_screen_height = GetAmplitudeScreenHeight(canvas, fr.noiseLevel, rect);
		canvas.drawLine(rect.left, rect.bottom - threshold_screen_height, rect.right, rect.bottom - threshold_screen_height, paint);

		// Draw histogram.
		paint.setARGB(255, 140, 140, 140);

		boolean above_threshold = false;
		int column_no = 0;
		
		float deadFrequencyPixels = rect.width() * PitchDetector.MIN_FREQUENCY / PitchDetector.SPECTRUM_HZ;
		float remainingWidth = rect.width() - deadFrequencyPixels;
		Iterator<Entry<Double, Double>> it = fr.frequencies.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Double, Double> entry = it.next();
			// double frequency = entry.getKey();
			final double amplitude = Math.min(entry.getValue(), MAX_AMPLITUDE);
			final long height = GetAmplitudeScreenHeight(canvas, amplitude, rect);
			if (amplitude > MIN_AMPLITUDE) {
				above_threshold = true;
			}
			canvas.drawRect(
					deadFrequencyPixels + rect.left + remainingWidth * column_no / fr.frequencies.size(),
					rect.bottom - height, 
					deadFrequencyPixels + rect.left + remainingWidth * (column_no + 1) / fr.frequencies.size(),
					rect.bottom, 
					paint);
			column_no++;
		}
		return above_threshold;
	}
}
