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
import android.util.AttributeSet;
import android.view.View;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.example.AndroidTuner.PitchDetectionRepresentation;
import com.example.AndroidTuner.PitchDetector.FreqResult;

public class DrawableView extends View {
	
	private FreqResult fr_;
	private Handler handler_;
	private Timer timer_;
	

	private void initView() {
		
		fr_ = new FreqResult();
		
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
	
	
    /**
     * Constructor.  This version is only needed if you will be instantiating
     * the object manually (not from a layout XML file).
     * @param context
     */
	public DrawableView(Context context) {
		super(context);
		
		
		initView();
	}
	
    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
	
    public DrawableView(Context context, AttributeSet attrs) {
        super(context, attrs);
		initView();
		
    }
    
    
	private final static int UI_UPDATE_MS = 50;

	

	protected void onDraw(Canvas canvas) {
		final int MARGIN = 20;
		final int effective_height = canvas.getHeight() - 4 * MARGIN;
		final int effective_width = canvas.getWidth() - 2 * MARGIN;
		
		final Rect histogramRect = new Rect(MARGIN, effective_height * 60 / 100 + 2 * MARGIN,
                effective_width + MARGIN, effective_height - MARGIN);
		
		Histogram.DrawHistogram(canvas, histogramRect, fr_);
		if (fr_.isPitchDetected) {
			PitchDrawings.DrawPitchPrecision(canvas, fr_.bestFrequency);
			PitchDrawings.DrawCurrentFrequency(canvas, 20, 50, fr_.bestFrequency);
		}		
	}

	public void setDetectionResults(FreqResult fr) {
		fr_ = fr;
	}

}