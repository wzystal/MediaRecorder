package com.baidu.mediarecorder;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore.Video;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.baidu.mediarecorder.ProgressView.State;
import static com.baidu.mediarecorder.contant.RecorderEnv.*;
import com.baidu.mediarecorder.util.CameraHelper;
import com.baidu.mediarecorder.util.FFmpegFrameRecorder;
import com.baidu.mediarecorder.util.YuvHelper;
import com.baidu.mediarecorder.util.VideoFrame;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

public class RecorderActivity extends Activity implements OnClickListener,
		OnTouchListener {

	private final String TAG = getClass().getSimpleName();

	private DisplayMetrics displayMetrics;
	private float perWidth;
	private int screenWidth, screenHeight;// 竖屏为准
	private int previewWidth, previewHeight;// 横屏为准
	private Button btnBack, btnFlash, btnSwitch, btnRollback, btnRecord,
			btnFinish;
	private ProgressView progressView;
	private TextView tv_total_time;
	private RelativeLayout surfaceLayout;
	private RelativeLayout.LayoutParams timeLayoutParams;

	private FFmpegFrameRecorder mediaRecorder;
	// 用于暂存录制的视频数据
	private ArrayList<VideoFrame> tempVideoList = new ArrayList<VideoFrame>();
	// 保存要录制的所有视频数据
	private LinkedList<ArrayList<VideoFrame>> allVideoList = new LinkedList<ArrayList<VideoFrame>>();
	// 用于暂存录制的音频数据
	private ArrayList<ShortBuffer> tempAudioList = new ArrayList<ShortBuffer>();
	// 保存要录制的音频数据
	private LinkedList<ArrayList<ShortBuffer>> allAudioList = new LinkedList<ArrayList<ShortBuffer>>();

	private String videoPath;
	private File videoFile;
	private Uri uriVideoPath;// 视频文件在系统中存放的url

	private long frameTime = 0;

	private long startTime = 0;// 第一次按下屏幕的时间
	private long startPauseTime = 0;// 暂停录制的开始时间(手指抬起)
	private long stopPauseTime = 0;// 暂停录制的结束时间(手指重新按下)
	private long curPausedTime = 0;// 本次暂停的时长
	private long totalPauseTime = 0;// 总的暂停时长
	private long rollbackTime = 0;// 回删的视频时长
	private long totalTime = 0; // = 当前时间 - firstTime - totalPauseTime -
								// rollbackTime - frameTime

	private int frameNum = 0;
	private long audioTimeStamp = 0;
	private long videoTimeStamp = 0;
	private long rollbackTimeStamp = 0;// 回删的视频戳时长

	/**
	 * 系统状态及时间定义
	 */
	private boolean isRecordStart = false;
	private boolean isRecordFinish = false;
	private boolean recording = false;
	private boolean isFlashOn = false;// 是否开启闪光灯
	private boolean isFrontCamera = false;// 是否为前置摄像头
	private boolean isFirstFrame = true;// 是否为第一帧
	private boolean isRollbackSate = false;// 回删状态标识，点击"回删"标记为true，再次点击"回删"会删除最近的视频片段
	private boolean isRecordingSaved = false;// 是否保存过视频文件

	private Camera camera;
	private Parameters cameraParams;
	private int cameraId = -1, cameraFacing = CameraInfo.CAMERA_FACING_BACK;// 默认为后置摄像头
	private CameraView cameraView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("wzy.lifecycle", TAG + ".onCreate() called!");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recorder);
		initView();
	}

	@Override
	protected void onResume() {
		Log.d("wzy.lifecycle", TAG + ".onResume() called!");
		super.onResume();
		initCamera();
	}

	private void initView() {
		displayMetrics = getResources().getDisplayMetrics();
		screenWidth = displayMetrics.widthPixels;
		screenHeight = displayMetrics.heightPixels;
		perWidth = screenWidth / MAX_RECORD_TIME;

		btnBack = (Button) findViewById(R.id.btn_recorder_back);
		btnBack.setOnClickListener(this);
		btnFlash = (Button) findViewById(R.id.btn_recorder_flash);
		btnFlash.setOnClickListener(this);
		btnSwitch = (Button) findViewById(R.id.btn_recorder_switch_camera);
		if (getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA_FRONT)) {
			btnSwitch.setVisibility(View.VISIBLE);
			btnSwitch.setOnClickListener(this);
		}
		btnRollback = (Button) findViewById(R.id.btn_recorder_rollback);
		btnRollback.setOnClickListener(this);
		btnRecord = (Button) findViewById(R.id.btn_recorder_record);
		btnRecord.setOnTouchListener(this);
		btnFinish = (Button) findViewById(R.id.btn_recorder_finish);
		btnFinish.setOnClickListener(this);

		progressView = (ProgressView) findViewById(R.id.progress_recorder);
		tv_total_time = (TextView) findViewById(R.id.tv_total_time);
		timeLayoutParams = (RelativeLayout.LayoutParams) tv_total_time
				.getLayoutParams();
	}

	private void initCamera() {
		new AsyncTask<Void, Integer, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					// 对于SDK2.2以上的，可能有多个摄像头
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
						int numberOfCameras = Camera.getNumberOfCameras();
						CameraInfo cameraInfo = new CameraInfo();
						for (int i = 0; i < numberOfCameras; i++) {
							Camera.getCameraInfo(i, cameraInfo);
							if (cameraInfo.facing == cameraFacing) {
								cameraId = i;
							}
						}
					}
					if (cameraId >= 0) {
						camera = Camera.open(cameraId);
					} else {
						camera = Camera.open();
					}
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
				initRecorder();
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				if (!result) {
					finish();
					return;
				}
				cameraParams = camera.getParameters();
				cameraView = new CameraView(RecorderActivity.this);
				handleSurfaceChanged();
				surfaceLayout = (RelativeLayout) findViewById(R.id.layout_recorder_surface);
				if (null != surfaceLayout && surfaceLayout.getChildCount() > 0)
					surfaceLayout.removeAllViews();
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
						screenWidth,
						(int) (screenWidth * (previewWidth / (previewHeight * 1f))));
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,
						RelativeLayout.TRUE);
				surfaceLayout.addView(cameraView, layoutParams);
			}
		}.execute();
	}

	private void initRecorder() {
		frameTime = VIDEO_BIT_RATE / VIDEO_FRAME_RATE;
		videoPath = SAVE_DIR_VIDEO + System.currentTimeMillis()
				+ VIDEO_EXTENSION;
		videoFile = new File(videoPath);
		mediaRecorder = new FFmpegFrameRecorder(videoPath, 480, 480, 1);
		mediaRecorder.setFormat(OUTPUT_FORMAT);
		mediaRecorder.setSampleRate(AUDIO_SAMPLE_RATE);
		mediaRecorder.setFrameRate(VIDEO_FRAME_RATE);
		mediaRecorder.setVideoCodec(VIDEO_CODEC);
		mediaRecorder.setVideoQuality(VIDEO_QUALITY);
		mediaRecorder.setAudioQuality(VIDEO_QUALITY);
		mediaRecorder.setAudioCodec(AUDIO_CODEC);
		mediaRecorder.setVideoBitrate(VIDEO_BIT_RATE);
		mediaRecorder.setAudioBitrate(AUDIO_BIT_RATE);

		mediaRecorder.setVideoOption("preset", "superfast");// 调节编码速度，速度越快，文件体积越大
		mediaRecorder.setVideoOption("tune", "zerolatency");// 根据tune指定的需求做视觉优化

		new Thread(new AudioRecordRunnable()).start();
		try {
			mediaRecorder.start();
		} catch (com.googlecode.javacv.FrameRecorder.Exception e) {
			e.printStackTrace();
		}

	}

	class CameraView extends SurfaceView implements SurfaceHolder.Callback,
			Camera.PreviewCallback {
		private SurfaceHolder mHolder;

		public CameraView(Context context) {
			super(context);
			mHolder = getHolder();
			mHolder.addCallback(CameraView.this);
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			camera.setPreviewCallback(CameraView.this);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.d("wzy.lifecycle", TAG + ".surfaceCreated() called!");
			try {
				camera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.d("wzy.lifecycle", TAG + ".surfaceChanged() called!");
			handleSurfaceChanged();
			camera.startPreview();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.d("wzy.lifecycle", TAG + ".surfaceDestroyed() called!");
			if (null != camera) {
				camera.stopPreview();
				camera.release();
				camera = null;
			}
		}

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			if (null != data && !isRecordFinish && recording) {
				totalTime = System.currentTimeMillis() - startTime
						- totalPauseTime - rollbackTime
						- ((long) (1.0 / (double) VIDEO_FRAME_RATE) * 1000);
				Log.d("wzy.logic", "开始录制视频...totalTime=" + totalTime);
				if (totalTime > MAX_RECORD_TIME)
					return;
				if (totalTime > 0)
					btnRollback.setEnabled(true);
				// videoTimeStamp = audioTimeStamp;
				IplImage iplImage = IplImage.create(previewHeight,
						previewWidth, IPL_DEPTH_8U, 2);
				byte[] tempData = YuvHelper.rotateYUV420Degree90(data,
						previewWidth, previewHeight);// 竖屏相机拍摄的图像，会逆时针翻转90度
				iplImage.getByteBuffer().put(tempData);
				long timestamp = frameTime * frameNum;
				frameNum++;
				VideoFrame videoFrame = new VideoFrame(timestamp, data,
						iplImage);
				tempVideoList.add(videoFrame);
			}
		}
	}

	private void handleSurfaceChanged() {
		if (null == camera) {
			return;
		}
		cameraParams.setPreviewFrameRate(VIDEO_FRAME_RATE);
		// 根据预设宽高获取相机支持的预览尺寸
		Size previewSize = CameraHelper.getOptimalPreviewSize(camera,
				previewWidth, previewHeight);
		if (null != previewSize) {
			previewWidth = previewSize.width;
			previewHeight = previewSize.height;
			cameraParams.setPreviewSize(previewWidth, previewHeight);
		}
		camera.setDisplayOrientation(90);
		// 摄像头自动对焦,SDK2.2以上不支持
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
			List<String> focusModes = cameraParams.getSupportedFocusModes();
			if (focusModes != null) {
				if (((Build.MODEL.startsWith("GT-I950"))
						|| (Build.MODEL.endsWith("SCH-I959")) || (Build.MODEL
							.endsWith("MEIZU MX3")))
						&& focusModes
								.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
					cameraParams
							.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				} else if (focusModes
						.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
					cameraParams
							.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				} else
					cameraParams
							.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
			}
		}
		camera.setParameters(cameraParams);
	}

	class AudioRecordRunnable implements Runnable {
		int bufferSize;
		short[] buffer;
		private final AudioRecord audioRecord;
		private int mCount = 0;

		private AudioRecordRunnable() {
			bufferSize = AudioRecord
					.getMinBufferSize(AUDIO_SAMPLE_RATE,
							AudioFormat.CHANNEL_IN_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
			audioRecord = new AudioRecord(AudioSource.MIC, AUDIO_SAMPLE_RATE,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, bufferSize);
			buffer = new short[bufferSize];
		}

		public void run() {
			android.os.Process
					.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			if (audioRecord != null) {
				// 判断音频录制是否被初始化
				while (audioRecord.getState() == 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				audioRecord.startRecording();
				while (recording && totalTime <= MAX_RECORD_TIME) {
					audioTimeStamp = (long) (1000 * mCount / (AUDIO_SAMPLE_RATE * 1f));
					int readSize = audioRecord.read(buffer, 0, buffer.length);
					if (readSize > 0) {
						short[] tempBuf = new short[readSize];
						System.arraycopy(buffer, 0, tempBuf, 0, readSize);
						ShortBuffer shortBuffer = ShortBuffer.wrap(tempBuf);
						mCount += shortBuffer.limit();
						tempAudioList.add(shortBuffer);
					}
				}
				audioRecord.stop();
				audioRecord.release();
			}
		}
	}

	private void rollbackVideo() {
		ArrayList<VideoFrame> lastVideoList = null;
		long timeStamp1 = 0L, timeStamp2 = 0L;
		if (allVideoList != null && allVideoList.size() > 0) {
			lastVideoList = allVideoList.getLast();
			if (lastVideoList.size() > 0) {
				timeStamp1 = lastVideoList.get(lastVideoList.size() - 1)
						.getTimeStamp();
			}
			allVideoList.removeLast();
		}
		if (allAudioList != null && allAudioList.size() > 0) {
			allAudioList.removeLast();
		}
		if (allVideoList != null && allVideoList.size() > 0) {
			lastVideoList = allVideoList.getLast();
			if (lastVideoList.size() > 0) {
				timeStamp2 = lastVideoList.get(lastVideoList.size() - 1)
						.getTimeStamp();
			}
		}
		rollbackTimeStamp += (timeStamp1 - timeStamp2);// 计算回删视频片段的时间戳
		int frontTime = progressView.getLastTime();
		progressView.setCurrentState(State.DELETE);
		isRollbackSate = false;
		// 若进度条队列为空，设置回删按钮不可点击
		if (progressView.isTimeListEmpty()) {
			btnRollback.setEnabled(false);
			totalTime = 0;
		}
		int lastTime = progressView.getLastTime();
		rollbackTime += (frontTime - lastTime);
		btnFinish.setEnabled(lastTime >= MIN_RECORD_TIME ? true : false);
	}

	private void saveRecorder() {
		new AsyncTask<Void, Integer, Void>() {
			private Dialog savingDialog;
			private ProgressBar progressBar;
			private TextView percent;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				savingDialog = new Dialog(RecorderActivity.this,
						R.style.Dialog_loading_noDim);
				savingDialog.setCanceledOnTouchOutside(false);
				savingDialog
						.setContentView(R.layout.activity_recorder_progress);
				progressBar = (ProgressBar) savingDialog
						.findViewById(R.id.recorder_progress_bar);
				percent = (TextView) savingDialog
						.findViewById(R.id.recorder_progress_percent);
				savingDialog.show();
			}

			@Override
			protected Void doInBackground(Void... params) {
				publishProgress(20);
				Iterator<ArrayList<VideoFrame>> videoIterator = allVideoList
						.iterator();
				ArrayList<VideoFrame> videoList = null;
				VideoFrame videoFrame = null;
				int allSize1 = allVideoList.size(), perProgress1 = 0, count1 = 0;
				// if (allSize1 == 0) {
				// publishProgress(40);
				// } else {
				// perProgress1 = (int) 40 / allSize1;
				// }
				while (videoIterator.hasNext()) {
					videoList = videoIterator.next();
					count1++;
					// publishProgress(20 + perProgress1 * count1);
					for (int i = 0; i < videoList.size(); i++) {
						videoFrame = videoList.get(i);
						mediaRecorder.setTimestamp(videoFrame.getTimeStamp());
						try {
							mediaRecorder.record(videoFrame.getIplImage());
						} catch (com.googlecode.javacv.FrameRecorder.Exception e) {
							e.printStackTrace();
						}
					}
				}
				publishProgress(60);
				Iterator<ArrayList<ShortBuffer>> audioIterator = allAudioList
						.iterator();
				ArrayList<ShortBuffer> audioList = null;
				int allSize2 = allVideoList.size(), perProgress2 = 0, count2 = 0;
				// if (allSize2 == 0) {
				// publishProgress(90);
				// } else {
				// perProgress2 = (int) 40 / allSize2;
				// }
				while (audioIterator.hasNext()) {
					audioList = audioIterator.next();
					count2++;
					// publishProgress(60 + perProgress2 * count2);
					for (ShortBuffer shortBuffer : audioList) {
						try {
							Buffer[] samples = new Buffer[] { shortBuffer };
							mediaRecorder.record(AUDIO_SAMPLE_RATE, samples);
						} catch (com.googlecode.javacv.FrameRecorder.Exception e) {
							e.printStackTrace();
						}
					}
				}
				publishProgress(90);
				Uri videoTable = Uri.parse(VIDEO_CONTENT_URI);
				ContentValues values = new ContentValues(7);
				values.put(Video.Media.TITLE, "video");
				values.put(Video.Media.DISPLAY_NAME, "video.mp4");
				values.put(Video.Media.DATE_TAKEN, System.currentTimeMillis());
				values.put(Video.Media.MIME_TYPE, "video/3gpp");
				values.put(Video.Media.DATA, videoPath);
				try {
					uriVideoPath = getContentResolver().insert(videoTable,
							values);
				} catch (Throwable e) {
					uriVideoPath = null;
					e.printStackTrace();
				}
				publishProgress(100);
				return null;
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				progressBar.setProgress(values[0]);
				percent.setText(values[0] + "%");
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				savingDialog.dismiss();
			}
		}.execute();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btn_recorder_rollback:
			if (!isRollbackSate) {
				progressView.setCurrentState(State.ROLLBACK);
				isRollbackSate = true;
			} else {
				rollbackVideo();
			}
			break;
		case R.id.btn_recorder_finish:
			isRecordFinish = true;
			saveRecorder();
			break;
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.d("wzy.lifecycle", TAG + ".onTouch() called ! ACTION_DOWN");
			recording = true;
			btnFinish.setEnabled(false);
			btnRollback.setEnabled(false);
			btnRecord.setSelected(true);
			if (!isRecordStart) {
				isRecordStart = true;
				startTime = System.currentTimeMillis();
			} else {
				stopPauseTime = System.currentTimeMillis();
				curPausedTime = stopPauseTime - startPauseTime
						- ((long) (1 / (double) VIDEO_FRAME_RATE) * 1000);
				totalPauseTime += curPausedTime;
			}
			progressView.setCurrentState(State.START);
			break;
		case MotionEvent.ACTION_UP:
			Log.d("wzy.lifecycle", TAG + ".onTouch() called ! ACTION_UP");
			recording = false;
			if (totalTime > MIN_RECORD_TIME)
				btnFinish.setEnabled(true);
			btnRecord.setSelected(false);
			progressView.setCurrentState(State.PAUSE);
			progressView.putTimeList((int) totalTime);
			startPauseTime = System.currentTimeMillis();
			break;
		}
		return true;
	}

	@Override
	protected void onStop() {
		Log.d("wzy.lifecycle", TAG + ".onStop() called!");
		super.onStop();
		if (null != camera) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	@Override
	protected void onDestroy() {
		Log.d("wzy.lifecycle", TAG + ".onDestroy() called!");
		super.onDestroy();
		if (null != camera) {
			camera.release();
			camera = null;
		}
	}
}
