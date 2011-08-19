package com.example.AndroidTuner.test;

import java.io.IOException;
import java.io.InputStream;

import com.example.AndroidTuner.AndroidTunerActivity;
import com.example.AndroidTuner.DrawableView;
import com.example.AndroidTuner.PitchDetector;
import com.example.AndroidTuner.test.WavReader.WavException;
import com.example.AndroidTuner.test.WavReader.WavInfo;

import android.content.Context;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

public class AndroidTunerTest extends
		ActivityInstrumentationTestCase2<AndroidTunerActivity> {

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
		InputStream wavStream = this.getInstrumentation().getContext().getResources().openRawResource(R.raw.guitar_a);
		WavInfo wi = WavReader.readHeader(wavStream);
		byte[] data = WavReader.readWavPcm(wi, wavStream);
		//byte[] subSection = new byte[88000];
		//System.arraycopy(data, 0, subSection, 0, 88000);
		//WavReader.play(subSection);
		WavReader.play(data);
	}
}
