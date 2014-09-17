package com.baidu.mediarecorder.util;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class VideoFrame {

	private long timeStamp;
	private IplImage image;
	private byte[] data;

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public IplImage getImage() {
		return image;
	}

	public void setImage(IplImage image) {
		this.image = image;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}
