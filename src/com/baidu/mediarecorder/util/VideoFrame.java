package com.baidu.mediarecorder.util;

import android.os.Parcel;
import android.os.Parcelable;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class VideoFrame implements Parcelable {

	private long timeStamp;
	private IplImage image;
	private byte[] data;

	public VideoFrame() {
	}

	public VideoFrame(long timeStamp, IplImage image, byte[] data) {
		this.timeStamp = timeStamp;
		this.image = image;
		this.data = data;
	}

	public static final Parcelable.Creator<VideoFrame> CREATOR = new Creator<VideoFrame>() {
		@Override
		public VideoFrame createFromParcel(Parcel source) {
			VideoFrame videoFrame = new VideoFrame();
			return null;
		}

		public VideoFrame[] newArray(int size) {
			return new VideoFrame[size];
		};
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeLong(timeStamp);
		out.writeByteArray(data);
	}

	private void readFromParcel(Parcel in) {
		timeStamp = in.readLong();
		data = new byte[1024];
		in.readByteArray(data);
	}

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
