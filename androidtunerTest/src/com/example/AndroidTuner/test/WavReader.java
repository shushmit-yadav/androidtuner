package com.example.AndroidTuner.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class WavReader {
	private static final String RIFF_HEADER = "RIFF";
	private static final String WAVE_HEADER = "WAVE";
	private static final String FMT_HEADER = "fmt ";
	private static final String DATA_HEADER = "data";

	private static final int HEADER_SIZE = 44;

	private static final String CHARSET = "ASCII";

	private static String LOG_TAG = "PitchDetector";
	
	public static class WavInfo {
		public int rate;
		public int channels;
		public int dataSize;
		
		public WavInfo(int rate, int channels, int dataSize) {
			this.rate = rate;
			this.channels = channels;
			this.dataSize = dataSize;
		}
	}

	public static class WavException extends Exception {
		
	}
	
	public static void checkFormat(boolean bSuccess, String message) throws WavException {
		if (!bSuccess) {
			Log.e(LOG_TAG, message);
			throw new WavException();
		}
	}
	
	public static WavInfo readHeader(InputStream wavStream) throws IOException, WavException {

		ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());

		buffer.rewind();
		buffer.position(buffer.position() + 20);
		int format = buffer.getShort();
		checkFormat(format == 1, "Unsupported encoding: " + format); // 1 means
																		// Linear
																		// PCM
		int channels = buffer.getShort();
		checkFormat(channels == 1 || channels == 2, "Unsupported channels: "
				+ channels);
		int rate = buffer.getInt();
		checkFormat(rate <= 48000 && rate >= 11025, "Unsupported rate: " + rate);
		buffer.position(buffer.position() + 6);
		int bits = buffer.getShort();
		checkFormat(bits == 16, "Unsupported bits: " + bits);
		int dataSize = 0;
		while (buffer.getInt() != 0x61746164) { // "data" marker
			Log.d(LOG_TAG, "Skipping non-data chunk");
			int size = buffer.getInt();
			wavStream.skip(size);

			buffer.rewind();
			wavStream.read(buffer.array(), buffer.arrayOffset(), 8);
			buffer.rewind();
		}
		dataSize = buffer.getInt();
		checkFormat(dataSize > 0, "wrong datasize: " + dataSize);

		return new WavInfo(rate, channels, dataSize);
	}

	public static byte[] readWavPcm(WavInfo info, InputStream stream)
			throws IOException {
		byte[] data = new byte[info.dataSize];
		stream.read(data, 0, data.length);
		return data;
	}
	
	public static void play(byte[] byteData)
	{
		// Set and push to audio track..
		int intSize = android.media.AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_MONO,
		AudioFormat.ENCODING_PCM_16BIT); 
		AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO,
		AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM); 
		if (at!=null) { 
			at.play();
			// Write the byte array to the track
			at.write(byteData, 0, byteData.length); 
			at.stop();
			at.release();
		}
		else {
			Log.d("TCAudio", "audio track is not initialised ");
		}


	}
}

