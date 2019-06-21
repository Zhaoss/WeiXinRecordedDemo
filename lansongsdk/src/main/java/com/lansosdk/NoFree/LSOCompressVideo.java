package com.lansosdk.NoFree;

import android.content.Context;

import com.lansosdk.box.CompressVideo;
import com.lansosdk.box.OnCompressCompletedListener;
import com.lansosdk.box.OnCompressProgressListener;

import java.io.IOException;

/**
 * 视频压缩
 */
public class LSOCompressVideo {

    CompressVideo compressVideo;

    /**
     * 构造方法
     * @param ctx
     * @param path 视频的完整路径
     * @throws IOException
     */
    public LSOCompressVideo(Context ctx, String path) throws IOException
    {
        compressVideo=new CompressVideo(ctx,path);
    }

    /**
     * 设置码率, 可选
     * @param bitRate
     */
    public void setEncoderBitRate(int bitRate) {
        if(compressVideo!=null){
            compressVideo.setEncoderBitRate(bitRate);
        }
    }

    /**
     * 开始执行
     */
    public boolean start() {
        if(compressVideo!=null && !compressVideo.isRunning()){
            return compressVideo.start();
        }
        return true;
    }

    /**
     * 停止执行
     */
    public void stop() {
        if(compressVideo!=null && compressVideo.isRunning()){
            compressVideo.stop();
        }
    }

    /**
     * 进度监听
     * @param listener
     */
    public void setOnCompressProgressListener(OnCompressProgressListener listener) {
        if(compressVideo!=null){
            compressVideo.setOnCompressProgressListener(listener);
        }
    }
    /**
     * 完成监听
     * @param listener
     */
    public void setOnCompressCompletedListener(OnCompressCompletedListener listener) {
        if(compressVideo!=null){
            compressVideo.setOnCompressCompletedListener(listener);
        }
    }

    /**
     * 释放
     */
    public void release(){
        if(compressVideo!=null){
            compressVideo.release();
            compressVideo=null;
        }
    }
}
