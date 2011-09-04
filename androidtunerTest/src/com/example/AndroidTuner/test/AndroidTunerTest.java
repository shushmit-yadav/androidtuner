package com.example.AndroidTuner.test;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.example.AndroidTuner.AndroidTunerActivity;
import com.example.AndroidTuner.DrawableView;
import com.example.AndroidTuner.PitchDetector;
import com.example.AndroidTuner.PitchDetector.FreqResult;
import com.example.AndroidTuner.test.WavStream.WavException;
import com.example.AndroidTuner.test.WavStream.WavInfo;

import android.content.Context;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.TextView;

public class AndroidTunerTest extends
		ActivityInstrumentationTestCase2<AndroidTunerActivity> {

	private static String LOG_TAG = "TunerTest";
	
	private AndroidTunerActivity mActivity;
	//private TextView mView;
	private DrawableView mView;
	private String resourceString;

	// public AndroidTunerTest(Class<AndroidTunerActivity> activityClass) {
	public AndroidTunerTest() {
		super("com.example.AndroidTuner", AndroidTunerActivity.class);

	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mActivity = this.getActivity();
		//mView = (TextView) mActivity.findViewById(com.example.AndroidTuner.R.id.textview);
		mView = mActivity.tv_;
		resourceString = mActivity.getString(com.example.AndroidTuner.R.string.hello);
	}

	public void testPreconditions() {
		assertNotNull(mView);
	}

	public void testPitchDetection() throws IOException, WavException {
		//assertEquals(resourceString, (String) mView.getText());
		//InputStream wavStream = getApplicationContext().getResources().openRawResource(R.raw.guitar_eadgbebgdae);
		InputStream wavStream = this.getInstrumentation().getContext().getResources().openRawResource(R.raw.sine_a4_d5_c3);
		WavStream ws = new WavStream(wavStream);
		short[] data;
		ws.play();
		
		/* expected frequencies, 2 seconds each:
		 * 	440
			587.330
			130.813
		 */
		// no need for anything besides the test
		mActivity.pitch_detector_thread_.interrupt();
		
		try {
			while (true) {
				data = ws.readShorts(4096);
				if (data.length < 4096) {
					// data that isn't a round (2^n) number makes the fft crash the process
					break;
				}
				FreqResult fr = PitchDetector.AnalyzeFrequencies(data);
				Log.e(LOG_TAG, "detected pitch " + fr);
				mActivity.gpl_.PostToUI(fr.frequencies, fr.best_frequency);
			}
		}
		catch (EOFException e) {
			
		}
		//byte[] subSection = new byte[88000];
		//System.arraycopy(data, 0, subSection, 0, 88000);
		//WavReader.play(subSection);
		//ws.play();
	}
}
