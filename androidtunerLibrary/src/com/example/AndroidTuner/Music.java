package com.example.AndroidTuner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Music {
	
	public final static String[] notes = {"a", "a#", "b", "c", "c#", "d", "d#", "e", "f", "f#", "g", "g#"};
	
	// if (A=0, A#=1, B=2...) then these are the black notes.
	public static Set<Integer> sBlackNotes = new HashSet<Integer>(Arrays.asList(new Integer[] {1, 4, 6, 9, 11}));
	
	public static double distanceFromA4(double frequency) {
		return Math.log(frequency / 440) * 12 / Math.log(2);
	}
	
	/*
	 * Modulus without negative results
	 */
	private static long mod(long x, long y)
	{
	    long result = x % y;
	    if (result < 0)
	    {
	        result += y;
	    }
	    return result;
	}
	
	public static int closestNoteIndex(double distanceFromA4)
	{
		return (int) mod(Math.round(distanceFromA4), 12);
	}
	
	public static String noteName(double distanceFromA4) {
		// 440 Hz is A4 and there are 9 half-steps from C4 to A4
		long octaveNumber = 4 + (long) Math.floor( (9.0 + Math.round(distanceFromA4)) / 12.0);
		return notes[closestNoteIndex(distanceFromA4)] + octaveNumber;
	}
	
	public static String hzToNoteName(double frequency) {
		// distance in half-steps
		double distanceFromA4 = distanceFromA4(frequency);
		return noteName(distanceFromA4);
	}

}
