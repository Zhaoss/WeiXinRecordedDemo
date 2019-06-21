package com.lansosdk.videoeditor;

public interface onVideoEditorProgressListener {
	/**
	 * 
	 * @param v
	 * @param percent  正在处理进度的百分比;
	 */
    void onProgress(VideoEditor v, int percent);
}
