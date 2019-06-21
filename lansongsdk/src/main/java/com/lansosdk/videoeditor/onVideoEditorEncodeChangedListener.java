package com.lansosdk.videoeditor;

public interface onVideoEditorEncodeChangedListener {
	/**
	 *
	 * @param v
	 * @param isSoftencoder 当前修改后, 是否是软件编码;
	 */
    void onChanged(VideoEditor v,boolean isSoftencoder);
}
