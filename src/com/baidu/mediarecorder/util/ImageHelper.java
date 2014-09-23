package com.baidu.mediarecorder.util;

public class ImageHelper {

	public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth,
			int imageHeight) {
		byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
		// Rotate the Y luma
		int i = 0;
		for (int x = 0; x < imageWidth; x++) {
			for (int y = imageHeight - 1; y >= 0; y--) {
				yuv[i] = data[y * imageWidth + x];
				i++;
			}
		}
		// Rotate the U and V color components
		i = imageWidth * imageHeight * 3 / 2 - 1;
		for (int x = imageWidth - 1; x > 0; x = x - 2) {
			for (int y = 0; y < imageHeight / 2; y++) {
				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
				i--;
				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
						+ (x - 1)];
				i--;
			}
		}
		return yuv;
	}

	private byte[] rotateYUV420Degree180(byte[] data, int imageWidth,
			int imageHeight) {
		byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
		int i = 0;
		int count = 0;
		for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
			yuv[count] = data[i];
			count++;
		}
		i = imageWidth * imageHeight * 3 / 2 - 1;
		for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
				* imageHeight; i -= 2) {
			yuv[count++] = data[i - 1];
			yuv[count++] = data[i];
		}
		return yuv;
	}

	private byte[] rotateYUV420Degree270(byte[] data, int imageWidth,
			int imageHeight) {
		byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
		int nWidth = 0, nHeight = 0;
		int wh = 0;
		int uvHeight = 0;
		if (imageWidth != nWidth || imageHeight != nHeight) {
			nWidth = imageWidth;
			nHeight = imageHeight;
			wh = imageWidth * imageHeight;
			uvHeight = imageHeight >> 1;// uvHeight = height / 2
		}
		// 旋转Y
		int k = 0;
		for (int i = 0; i < imageWidth; i++) {
			int nPos = 0;
			for (int j = 0; j < imageHeight; j++) {
				yuv[k] = data[nPos + i];
				k++;
				nPos += imageWidth;
			}
		}
		for (int i = 0; i < imageWidth; i += 2) {
			int nPos = wh;
			for (int j = 0; j < uvHeight; j++) {
				yuv[k] = data[nPos + i];
				yuv[k + 1] = data[nPos + i + 1];
				k += 2;
				nPos += imageWidth;
			}
		}
		return rotateYUV420Degree180(yuv, imageWidth, imageHeight);
	}
}
