package com.baidu.mediarecorder.util;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class VideoFrame {

	private long timeStamp;
	private byte[] data;
	private IplImage iplImage;

	public VideoFrame() {
	}

	public VideoFrame(long timeStamp, byte[] data, IplImage iplImage) {
		this.timeStamp = timeStamp;
		this.data = data;
		this.iplImage = iplImage;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public IplImage getIplImage() {
		return iplImage;
	}

	public void setIplImage(IplImage iplImage) {
		this.iplImage = iplImage;
	}

}
