package com.baidu.mediarecorder;

import java.util.Iterator;
import java.util.LinkedList;

import com.baidu.mediarecorder.contant.RecorderEnv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class ProgressView extends View {
	private final String TAG = getClass().getSimpleName();

	public ProgressView(Context context) {
		super(context);
		init(context);
	}

	public ProgressView(Context paramContext, AttributeSet paramAttributeSet) {
		super(paramContext, paramAttributeSet);
		init(paramContext);

	}

	public ProgressView(Context paramContext, AttributeSet paramAttributeSet,
			int paramInt) {
		super(paramContext, paramAttributeSet, paramInt);
		init(paramContext);
	}

	private DisplayMetrics displayMetrics;
	private int screenWidth, progressHeight;
	private Paint progressPaint, flashPaint, minTimePaint, breakPaint,
			rollbackPaint;
	private float perWidth, flashWidth = 20f, minTimeWidth = 5f,
			breakWidth = 2f;
	private LinkedList<Integer> timeList = new LinkedList<Integer>();// 每次暂停录制时，将录制时长记录到队列中

	private void init(Context paramContext) {
		displayMetrics = getResources().getDisplayMetrics();
		screenWidth = displayMetrics.widthPixels;
		perWidth = screenWidth / RecorderEnv.MAX_RECORD_TIME;
//		Log.d("wzy.size", TAG + ".perWidth=" + perWidth);

		progressPaint = new Paint();
		flashPaint = new Paint();
		minTimePaint = new Paint();
		breakPaint = new Paint();
		rollbackPaint = new Paint();

		setBackgroundColor(Color.parseColor("#222222"));

		progressPaint.setStyle(Paint.Style.FILL);
		progressPaint.setColor(Color.parseColor("#19E3CF"));

		flashPaint.setStyle(Paint.Style.FILL);
		flashPaint.setColor(Color.parseColor("#FFFFFF"));

		minTimePaint.setStyle(Paint.Style.FILL);
		minTimePaint.setColor(Color.parseColor("#FF0000"));

		breakPaint.setStyle(Paint.Style.FILL);
		breakPaint.setColor(Color.parseColor("#000000"));

		rollbackPaint.setStyle(Paint.Style.FILL);
		rollbackPaint.setColor(Color.rgb(255, 98, 89));
	}

	private volatile State currentState = State.PAUSE;
	private boolean isVisible = true;// 一闪一闪的黄色区域是否可见
	private float countWidth = 0;// 每次绘制完成后，进度条的长度
	private float perProgress = 0;// 手指按下时，进度条每次增长的长度
	private long initTime;// 绘制完成时的时间戳
	private long drawFlashTime = 0;// 闪动的黄色区域时间戳

	private long lastStartTime = 0; // 最近视频片段的开始时间
	private long lastEndTime = 0; // 最近视频片段的结束时间

	public static enum State {
		START(0x1), PAUSE(0x2), ROLLBACK(0x3), DELETE(0x4);

		static State mapIntToValue(final int stateInt) {
			for (State value : State.values()) {
				if (stateInt == value.getIntValue()) {
					return value;
				}
			}
			return PAUSE;
		}

		private int mIntValue;

		State(int intValue) {
			mIntValue = intValue;
		}

		int getIntValue() {
			return mIntValue;
		}
	}

	public void setCurrentState(State state) {
		currentState = state;
		if (state != State.START)
			perProgress = perWidth;
		if (state == State.DELETE) {
			if ((timeList != null) && (!timeList.isEmpty())) {
				timeList.removeLast();
			}
		}
	}

	protected void onDraw(Canvas canvas) {
//		Log.d("wzy.lifecycle", TAG + ".onDraw() called!");
		super.onDraw(canvas);
		progressHeight = getMeasuredHeight();
//		Log.d("wzy.size", TAG + ".progressHeight=" + progressHeight);
		long curSystemTime = System.currentTimeMillis();
		countWidth = 0;
//		Log.d("wzy.logic", TAG + ".timeList.isEmpty()=" + timeList.isEmpty());
		if (!timeList.isEmpty()) {
			long preTime = 0;
			long curTime = 0;
			Iterator<Integer> iterator = timeList.iterator();
			while (iterator.hasNext()) {
				lastStartTime = preTime;
				curTime = iterator.next();
				lastEndTime = curTime;
				float left = countWidth;
				countWidth += (curTime - preTime) * perWidth;
				canvas.drawRect(left, 0, countWidth, progressHeight,
						progressPaint);
				canvas.drawRect(countWidth, 0, countWidth + breakWidth,
						progressHeight, breakPaint);
				countWidth += breakWidth;
				preTime = curTime;
			}
		}
		if (timeList.isEmpty()
				|| (!timeList.isEmpty() && timeList.getLast() <= RecorderEnv.MIN_RECORD_TIME)) {
			float left = perWidth * RecorderEnv.MIN_RECORD_TIME;
			canvas.drawRect(left, 0, left + minTimeWidth, progressHeight,
					minTimePaint);
		}
		// 将回滚状态下的视频片段进度条绘制成红色
		if (currentState == State.ROLLBACK) {
			float left = countWidth - (lastEndTime - lastStartTime) * perWidth;
			float right = countWidth;
			canvas.drawRect(left, 0, right, progressHeight, rollbackPaint);
		}
		// 手指按下时，绘制新进度条
		if (currentState == State.START) {
			perProgress += perWidth * (curSystemTime - initTime);
			if (countWidth + perProgress <= getMeasuredWidth())
				canvas.drawRect(countWidth, 0, countWidth + perProgress,
						getMeasuredHeight(), progressPaint);
			else
				canvas.drawRect(countWidth, 0, getMeasuredWidth(),
						getMeasuredHeight(), progressPaint);
		}
		if (currentState == State.START) {
			canvas.drawRect(countWidth + perProgress, 0, countWidth
					+ flashWidth + perProgress, getMeasuredHeight(), flashPaint);
		} else {
			if (drawFlashTime == 0 || curSystemTime - drawFlashTime >= 800) {
				isVisible = !isVisible;
				drawFlashTime = System.currentTimeMillis();
			}
			if (isVisible){
				canvas.drawRect(countWidth, 0, countWidth + flashWidth,
						getMeasuredHeight(), flashPaint);
			}
		}
		initTime = System.currentTimeMillis();
		invalidate();
	}

	public void putTimeList(int time) {
		timeList.add(time);
	}

	public int getLastTime() {
		if ((timeList != null) && (!timeList.isEmpty())) {
			return timeList.getLast();
		}
		return 0;
	}

	public boolean isTimeListEmpty() {
		return timeList.isEmpty();
	}
}
