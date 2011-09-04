/** Copyright (C) 2009 by Aleksey Surkov.
 **
 ** Permission to use, copy, modify, and distribute this software and its
 ** documentation for any purpose and without fee is hereby granted, provided
 ** that the above copyright notice appear in all copies and that both that
 ** copyright notice and this permission notice appear in supporting
 ** documentation.  This software is provided "as is" without express or
 ** implied warranty.
 */

package com.example.AndroidTuner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.example.AndroidTuner.PitchDetectionRepresentation;

public class DrawableView extends View {
	
	private HashMap<Double, Double> frequencies_;
	private double pitch_;
	//private PitchDetectionRepresentation representation_;
	private Handler handler_;
	private Timer timer_;
	
	public DrawableView(Context context) {
		super(context);
		
		
		// UI update cycle.
		handler_ = new Handler();
		timer_ = new Timer();
		timer_.schedule(new TimerTask() {
				public void run() {
					handler_.post(new Runnable() {
						public void run() {
							invalidate();
						}
					});
				}
			},
			UI_UPDATE_MS ,
			UI_UPDATE_MS );
	}
	
	// NotePitches[i][j] is the pitch of i-th string on j-th fret. 0th fret means an open fret.
	private double[][] NotePitches = new double[6][6]; 
	private TreeMap<Double, Integer> NotePitchesMap = new TreeMap<Double, Integer>(); 
    
	private final static int MIN_AMPLITUDE = 40000;
	private final static int MAX_AMPLITUDE = 3200000;
	private final static double MAX_PITCH_DIFF = 20;  // in Hz
	private final static int UI_UPDATE_MS = 100;

	private int GetFingerboardCoord(double pitch) {
		final SortedMap<Double, Integer> tail_map = NotePitchesMap.tailMap(pitch);
		final SortedMap<Double, Integer> head_map = NotePitchesMap.headMap(pitch);
		final double closest_right = tail_map == null || tail_map.isEmpty() ? NotePitchesMap.lastKey() : tail_map.firstKey(); 
		final double closest_left = head_map == null || head_map.isEmpty() ? NotePitchesMap.firstKey() : head_map.lastKey();
		if (closest_right - pitch < pitch - closest_left) {
			return NotePitchesMap.get(closest_right);
		} else {
			return NotePitchesMap.get(closest_left);
		}
	}
	
	final int FINGERBOARD_PADDING = 10;
	final static int HEADSTOCK_HEIGHT = 10;
	final static int HEADSTOCK_WIDTH = 50;
	private void DrawFingerboard(Canvas canvas, Rect rect) {
		Paint paint = new Paint();
		paint.setARGB(255, 100, 200, 100);
        // Draw strings		
		for (int i = 0; i < 6; i++) {
			final int offset = Math.round((rect.height() - FINGERBOARD_PADDING * 2) / 5 * i) + FINGERBOARD_PADDING;
			canvas.drawLine(rect.left, rect.top + offset, rect.right, rect.top + offset, paint);
		}
		// Draw fingerboard's end.
		canvas.drawRect(rect.right - FINGERBOARD_PADDING, rect.top, rect.right, rect.bottom, paint);
		
        // Draw frets
		for (int i = 1; i < 6; i++) {
			final int offset = Math.round((rect.width() - FINGERBOARD_PADDING * 2) / 5 * i) + FINGERBOARD_PADDING;
			canvas.drawLine(rect.right - offset, rect.top, rect.right - offset, rect.bottom, paint);
		}

		// Draw guitar
		paint.setARGB(255, 195, 118, 27);  // a nice guitar color
		canvas.drawLine(rect.left, rect.top, rect.right, rect.top, paint);
		canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paint);
		canvas.drawLine(rect.right + HEADSTOCK_WIDTH, rect.top - HEADSTOCK_HEIGHT, rect.right, rect.top, paint);
		canvas.drawLine(rect.right + HEADSTOCK_WIDTH, rect.bottom + HEADSTOCK_HEIGHT, rect.right, rect.bottom, paint);
		
		// Marks on the 3rd and 5th frets.
		final long offset_3_mark = Math.round((rect.width() - FINGERBOARD_PADDING * 2) / 5 * 2.5) + FINGERBOARD_PADDING;
		final long offset_5_mark = Math.round((rect.width() - FINGERBOARD_PADDING * 2) / 5 * 4.5) + FINGERBOARD_PADDING;
		canvas.drawCircle(rect.right - offset_3_mark, rect.top, 3, paint);
		canvas.drawCircle(rect.right - offset_5_mark, rect.top, 3, paint);
		
		
		// Draw strings on the headstock
		paint.setARGB(255, 100, 200, 100);
		for (int i = 1; i <= 6; i++) {
			canvas.drawLine(rect.right + HEADSTOCK_WIDTH,
					        rect.top - HEADSTOCK_HEIGHT + 
				            Math.round((rect.height() + 2 * HEADSTOCK_HEIGHT - FINGERBOARD_PADDING * 2) 
				                / 5 * (i - 1)) + FINGERBOARD_PADDING,
					        rect.right,
					        rect.top + 
					            Math.round((rect.height() - FINGERBOARD_PADDING * 2) / 5 * (i - 1)) + FINGERBOARD_PADDING,
					        paint);
		}
	}
	
	private long GetAmplitudeScreenHeight(Canvas canvas, double amplitude, Rect histogram_rect) {
		return Math.round(amplitude / MAX_AMPLITUDE * histogram_rect.height());
	}
	

	private boolean DrawHistogram(Canvas canvas, Rect rect) {
		if (frequencies_ == null) {
			return false;
		}
		
		Paint paint = new Paint();
		// Draw border.
		paint.setARGB(80, 200, 200, 200);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(rect, paint);
		
		// Draw threshold.
		paint.setARGB(180, 200, 0, 0);
		final long threshold_screen_height = GetAmplitudeScreenHeight(canvas, MIN_AMPLITUDE, rect);
		canvas.drawLine(rect.left, rect.bottom - threshold_screen_height, rect.right, rect.bottom - threshold_screen_height, paint);

		// Draw histogram.
		paint.setARGB(255, 140, 140, 140);

		boolean above_threshold = false;
		int column_no = 0;
		Iterator<Entry<Double, Double>> it = frequencies_.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Double, Double> entry = it.next();
			// double frequency = entry.getKey();
			final double amplitude = Math.min(entry.getValue(), MAX_AMPLITUDE);
			final long height = GetAmplitudeScreenHeight(canvas, amplitude, rect);
			if (amplitude > MIN_AMPLITUDE) {
				above_threshold = true;
			}
			canvas.drawRect(
					rect.left + rect.width() * column_no / frequencies_.size(),
					rect.bottom - height, 
					rect.left + rect.width() * (column_no + 1) / frequencies_.size(),
					rect.bottom, 
					paint);
			column_no++;
		}
		return above_threshold;
	}

	private void DrawCurrentFrequency(Canvas canvas, int x, int y, double pitch) {
		final int alpha = 255;
		Paint paint = new Paint();
		paint.setARGB(alpha, 200, 0, 0);
		paint.setTextSize(35);
		final double pitchLessAccurate = Math.round(pitch * 10) / 10.0;
		canvas.drawText(pitchLessAccurate + " Hz ", 20, 40, paint);
		canvas.drawText(PitchDetector.HzToNote(pitch), 20, 80, paint);
	}
	
	protected void onDraw(Canvas canvas) {
		final int MARGIN = 20;
		final int effective_height = canvas.getHeight() - 4 * MARGIN;
		final int effective_width = canvas.getWidth() - 2 * MARGIN;
		
		final Rect histogram = new Rect(MARGIN, effective_height * 60 / 100 + 2 * MARGIN,
                effective_width + MARGIN, effective_height + MARGIN);
		
		if (DrawHistogram(canvas, histogram)) {
			// detected pitch
		}
		
		DrawCurrentFrequency(canvas, 20, 50, pitch_);
		DrawPitchPrecision(canvas, pitch_);
		
	}

	private void DrawPitchPrecision(Canvas canvas, double pitch) {
		if (pitch == 0.0) {
			return;
		}
		
		Paint notePaint = new Paint();
		final double distanceFromA4 = PitchDetector.distanceFromA4(pitch);
		final double pitchMistake = distanceFromA4 - Math.round(distanceFromA4);
		final int pitchMistakeColor = (int) (Math.abs(pitchMistake) * 500); // pitchMistake is -0.5 - 0.5, so color is 0 - 250.
		final int tuneNeedleY = 150;
		final int tuneHairRadius = 20;
		
		if (pitchMistake > 0) {
			// reddish
			notePaint.setARGB(180, pitchMistakeColor, 250 - pitchMistakeColor, 30);
		} else {
			// blueish
			notePaint.setARGB(180, 30, 250 - pitchMistakeColor, pitchMistakeColor);
		}
		
		final int width = canvas.getWidth();
		final int centerX = width / 2;
		
		float posX = (float) (width * (0.5 + pitchMistake));
		
		// hair
		canvas.drawLine(centerX, tuneNeedleY - tuneHairRadius, centerX, tuneNeedleY + tuneHairRadius, notePaint);
		
		// horizontal line
		canvas.drawLine(0, tuneNeedleY, width, tuneNeedleY, notePaint);
		
		// needle
		canvas.drawCircle(posX, tuneNeedleY, 5, notePaint);
	}

	public void setDetectionResults(final HashMap<Double, Double> frequencies, double pitch) {
		frequencies_ = frequencies;
		pitch_ = pitch;
	}

}