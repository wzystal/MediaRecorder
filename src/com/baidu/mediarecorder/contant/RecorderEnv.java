package com.baidu.mediarecorder.contant;

import com.googlecode.javacv.cpp.avcodec;

import android.os.Build;
import android.os.Environment;

public class RecorderEnv {
	public final static float MAX_RECORD_TIME = 8000f;
	public final static float MIN_RECORD_TIME = 2000f;

	public final static String SD_PATH = Environment
			.getExternalStorageDirectory().toString();
	public final static String SAVE_DIR_VIDEO = SD_PATH + "/DCIM/video/";
	public final static String VIDEO_EXTENSION = ".mp4";
	public final static String VIDEO_CONTENT_URI = "content://media/external/video/media";
	public final static int RESOLUTION_HIGH = 1300;
	public final static int RESOLUTION_MEDIUM = 500;
	public final static int RESOLUTION_LOW = 180;

	public final static int RESOLUTION_HIGH_VALUE = 2;
	public final static int RESOLUTION_MEDIUM_VALUE = 1;
	public final static int RESOLUTION_LOW_VALUE = 0;

	public final static boolean AAC_SUPPORTED = Build.VERSION.SDK_INT >= 10;
	public final static int VIDEO_CODEC = avcodec.AV_CODEC_ID_MPEG4;
//	public final static int VIDEO_CODEC = avcodec.AV_CODEC_ID_H264;
	public final static int VIDEO_FRAME_RATE = 30;
	public final static int VIDEO_QUALITY = 12;
	public final static int AUDIO_CODEC = AAC_SUPPORTED ? avcodec.AV_CODEC_ID_AAC
			: avcodec.AV_CODEC_ID_AMR_NB;
	public final static int AUDIO_CHANNEL = 1;
	public final static int AUDIO_BIT_RATE = 96000;// 192000;//AAC_SUPPORTED ?
													// 96000 : 12200;
	// public final static int VIDEO_BIT_RATE = 1000000;
	public final static int VIDEO_BIT_RATE = 1000000;
	public final static int AUDIO_SAMPLE_RATE = AAC_SUPPORTED ? 44100 : 8000;
	public final static String OUTPUT_FORMAT = AAC_SUPPORTED ? "mp4" : "3gp";

}
