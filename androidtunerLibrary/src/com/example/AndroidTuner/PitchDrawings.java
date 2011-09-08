package com.example.AndroidTuner;

import android.graphics.Canvas;
import android.graphics.Paint;

public class PitchDrawings {
	public static void drawCurrentFrequency(Canvas canvas, int x, int y, double pitch) {
		final int alpha = 255;
		Paint paint = new Paint();
		paint.setARGB(alpha, 200, 200, 250);
		paint.setTextSize(35);
		final double pitchLessAccurate = Math.round(pitch * 10) / 10.0;
		canvas.drawText(pitchLessAccurate + " Hz ", x, y, paint);
		canvas.drawText(PitchDetector.HzToNote(pitch), x, y + 40, paint);
	}
	
	public static void PitchMistakeColor(Paint paint, double pitchMistake) {
		final int pitchMistakeColor = (int) (Math.abs(pitchMistake) * 500); // pitchMistake is -0.5 - 0.5, so color is 0 - 250.
		
		if (pitchMistake > 0) {
			// reddish
			paint.setARGB(180, pitchMistakeColor, 250 - pitchMistakeColor, 30);
		} else {
			// blueish
			paint.setARGB(180, 30, 250 - pitchMistakeColor, pitchMistakeColor);
		}
		
	}
	
	public static double PitchMistake(double pitch) {
		final double distanceFromA4 = PitchDetector.distanceFromA4(pitch);
		final double pitchMistake = distanceFromA4 - Math.round(distanceFromA4);
		return pitchMistake;
	}

	public static void drawPitchPrecision(Canvas canvas, double pitch) {
		final int tuneNeedleY = 150;
		final int tuneHairRadius = 20;
		Paint notePaint = new Paint();
		
		final double pitchMistake = PitchMistake(pitch);
		PitchMistakeColor(notePaint, pitchMistake);
		
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
	
}
