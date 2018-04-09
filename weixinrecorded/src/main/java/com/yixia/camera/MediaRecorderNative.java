package com.yixia.camera;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;

import com.yixia.camera.model.MediaObject;
import com.yixia.videoeditor.adapter.UtilityAdapter;

/**
 * 视频录制：边录制边底层处理视频（旋转和裁剪）
 * 
 * @author yixia.com
 *
 */
public class MediaRecorderNative extends MediaRecorderBase implements MediaRecorder.OnErrorListener {

	/** 视频后缀 */
	private static final String VIDEO_SUFFIX = ".ts";
	private int cameraState = 1;

	/** 开始录制 */
	@Override
	public MediaObject.MediaPart startRecord() {
		//防止没有初始化的情况
		if (!UtilityAdapter.isInitialized()) {
			UtilityAdapter.initFilterParser();
		}
		
		MediaObject.MediaPart result = null;

		if (mMediaObject != null) {
			mRecording = true;
			result = mMediaObject.buildMediaPart(mCameraId, VIDEO_SUFFIX);
			String cmd = String.format("filename = \"%s\"; ", result.mediaPath);
			cmd += String.format("addcmd = %s; ", " -vf \"transpose="+cameraState+"\" ");
			UtilityAdapter.FilterParserAction(cmd, UtilityAdapter.PARSERACTION_START);
			if (mAudioRecorder == null && result != null) {
				mAudioRecorder = new AudioRecorder(this);
				mAudioRecorder.start();
			}
		}
		return result;
	}

	public boolean getRecordState(){
		return mRecording;
	}

	/** 切换前置/后置摄像头 */
	public void switchCamera() {
		if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
			switchCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
			cameraState = 2;
		} else {
			switchCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
			cameraState = 1;
		}
	}

	/** 停止录制 */
	@Override
	public void stopRecord() {
		UtilityAdapter.FilterParserAction("", UtilityAdapter.PARSERACTION_STOP);
		super.stopRecord();
	}

	/** 数据回调 */
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mRecording) {
			UtilityAdapter.RenderDataYuv(data);
		}
		super.onPreviewFrame(data, camera);
	}

	/** 预览成功，设置视频输入输出参数 */
	@Override
	protected void onStartPreviewSuccess() {
		if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
			UtilityAdapter.RenderInputSettings(MediaRecorderBase.VIDEO_WIDTH, MediaRecorderBase.VIDEO_HEIGHT, 0, UtilityAdapter.FLIPTYPE_NORMAL);
		} else {
			UtilityAdapter.RenderInputSettings(MediaRecorderBase.VIDEO_WIDTH, MediaRecorderBase.VIDEO_HEIGHT, 180, UtilityAdapter.FLIPTYPE_HORIZONTAL);
		}
		UtilityAdapter.RenderOutputSettings(MediaRecorderBase.VIDEO_WIDTH, MediaRecorderBase.VIDEO_HEIGHT, mFrameRate, UtilityAdapter.OUTPUTFORMAT_YUV | UtilityAdapter.OUTPUTFORMAT_MASK_MP4 /*| UtilityAdapter.OUTPUTFORMAT_MASK_HARDWARE_ACC*/);
	}

	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		try {
			if (mr != null)
				mr.reset();
		} catch (IllegalStateException e) {
			Log.w("Yixia", "stopRecord", e);
		} catch (Exception e) {
			Log.w("Yixia", "stopRecord", e);
		}
		if (mOnErrorListener != null)
			mOnErrorListener.onVideoError(what, extra);
	}

	/** 接收音频数据，传递到底层 */
	@Override
	public void receiveAudioData(byte[] sampleBuffer, int len) {
		if (mRecording && len > 0) {
			UtilityAdapter.RenderDataPcm(sampleBuffer);
		}
	}
}
