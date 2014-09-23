package com.baidu.mediarecorder.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

public class CameraHelper {

	// 根据预设宽高获取最合适的相机支持尺寸
	public static Size getOptimalPreviewSize(Camera camera, int width,
			int height) {
		Size previewSize = null;
		List<Size> supportedSizes = camera.getParameters()
				.getSupportedPreviewSizes();
		Collections.sort(supportedSizes, new SizeComparator());
		if (null != supportedSizes && supportedSizes.size() > 0) {
			boolean hasSize = false;
			for (Size size : supportedSizes) {
				Log.d("wzy.size", "当前手机支持的分辨率：" + size.width + "*" + size.height);
				if (null != size && size.width == width
						&& size.height == height) {
					previewSize = size;
					hasSize = true;
					break;
				}
			}
			if (!hasSize) {
				previewSize = supportedSizes.get(supportedSizes.size() / 2);
			}
		}
		return previewSize;
	}

	// 摄像头对焦
	
	
	private static class SizeComparator implements Comparator<Size> {
		@Override
		public int compare(Size size1, Size size2) {
			if (size1.height != size2.height)
				return size1.height - size2.height;
			else
				return size1.width - size2.width;
		}
	}
}
