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

import java.lang.Thread;
import java.util.HashMap;

import com.example.AndroidTuner.DrawableView;
import com.example.AndroidTuner.PitchDetector;
import com.example.AndroidTuner.PitchDetector.FreqResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;

public class AndroidTunerActivity extends Activity {
	
	public DrawableView tv_;
	public Thread pitch_detector_thread_;
	public PitchDetector pd_;
	public GuiPitchListener gpl_;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		tv_ = (DrawableView) findViewById(R.id.drawview);
		
		// manually invoking the view and thus ignoring layout/main.xml
		//tv_ = new DrawableView(this);
		//setContentView(tv_);
	}

	public class GuiPitchListener implements PitchDetector.PitchListener {
		private AndroidTunerActivity parent_;
		private Handler handler_;
		
		public GuiPitchListener(AndroidTunerActivity parent, Handler handler) {
			parent_ = parent;
			handler_ = handler;			
		}
		
		public void PostToUI(final FreqResult fr) {
			handler_.post(new Runnable() {
				public void run() {
					parent_.ShowPitchDetectionResult(fr);
					tv_.FFTPerSecond = parent_.pd_.FFTPerSecond; 
				}
			});
		}

		private void ShowError(final String msg) {
			handler_.post(new Runnable() {
				public void run() {
					new AlertDialog.Builder(parent_).setTitle("GuitarTuner")
							.setMessage(msg).show();
				}
			});
		}

		@Override
		public void onAnalysis(FreqResult fr) {
			// TODO Auto-generated method stub
			PostToUI(fr);
		}

		@Override
		public void onError(String error) {
			// TODO Auto-generated method stub
			ShowError(error);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		gpl_ = new GuiPitchListener(this, new Handler());
		pd_ = new PitchDetector(gpl_);
		pitch_detector_thread_ = new Thread(pd_);
		pitch_detector_thread_.start();
	}

	@Override
	public void onStop() {
		super.onStop();
		pitch_detector_thread_.interrupt();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	public void ShowPitchDetectionResult(FreqResult fr) {
		tv_.setDetectionResults(fr);
	}
}