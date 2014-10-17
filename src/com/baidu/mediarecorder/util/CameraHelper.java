package com.baidu.mediarecorder.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

/**
 * CameraHelper
 * @author wzystal@gmail.com
 * 2014-10-14
 */
public class CameraHelper {

	/**
	 * 根据预设宽高获取匹配的相机分辨率,若没有则返回中间值
	 * @param camera 相机
	 * @param width 预设宽度
	 * @param height 预设高度
	 * @return
	 */
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
	
	private static class SizeComparator implements Comparator<Size> {
		@Override
		public int compare(Size size1, Size size2) {
			if (size1.height != size2.height)
				return size1.height - size2.height;
			else
				return size1.width - size2.width;
		}
	}
	
	/**
	 * 将屏幕坐标系转化成对焦坐标系,返回要对焦的矩形框
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param w 相机宽度
	 * @param h 相机高度
	 * @param areaSize 对焦区域大小
	 * @return
	 */
	public static Rect getFocusArea(int x, int y, int w, int h, int areaSize) {
		int centerX = x / w * 2000 - 1000;
		int centerY = y / h * 2000 - 1000;
		int left = clamp(centerX - areaSize / 2, -1000, 1000);
		int right = clamp(left + areaSize, -1000, 1000);
		int top = clamp(centerY - areaSize / 2, -1000, 1000);
		int bottom = clamp(top + areaSize, -1000, 1000);
		return new Rect(left, top, right, bottom);
	}

	/**
	 * 限定x取值范围为[min,max]
	 * @param x
	 * @param min
	 * @param max
	 * @return
	 */
	public static int clamp(int x, int min, int max) {
		if (x > max) {
			return max;
		}
		if (x < min) {
			return min;
		}
		return x;
	}
}
