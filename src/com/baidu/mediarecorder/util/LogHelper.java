package com.baidu.mediarecorder.util;

import android.util.Log;

public class LogHelper {
	private static boolean isLog = true;

	public static void closeAllLogs() {
		isLog = false;
	}

	public static void openAllLogs() {
		isLog = true;
	}

	public static void d(String tag, String msg) {
		if (isLog) {
			Log.d(tag, msg);
		}
	}

	public static void e(String tag, String msg) {
		if (isLog) {
			Log.e(tag, msg);
		}
	}

	public static void v(String tag, String msg) {
		if (isLog) {
			Log.v(tag, msg);
		}
	}

	public static void w(String tag, String msg) {
		if (isLog) {
			Log.w(tag, msg);
		}
	}

	public static void i(String tag, String msg) {
		if (isLog) {
			Log.i(tag, msg);
		}
	}
}
