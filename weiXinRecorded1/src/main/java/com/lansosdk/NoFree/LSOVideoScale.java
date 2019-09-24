package com.lansosdk.NoFree;

import android.content.Context;

import com.lansosdk.box.ScaleExecute;
import com.lansosdk.box.onScaleCompletedListener;
import com.lansosdk.box.onScaleProgressListener;
import com.lansosdk.videoeditor.AudioEditor;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;

public class LSOVideoScale {

    private Context context;
    private String srcVideo;
    private String dstVideo;
    private MediaInfo mediaInfo;
    private boolean isExecuting = false;
    private onVideoCompressCompletedListener compressCompletedListener;
    private onVideoCompressProgressListener progressListener;

    public LSOVideoScale(Context ctx, String video){
        context=ctx;
        srcVideo=video;
    }
    public void setOnVideoCompressCompletedListener(onVideoCompressCompletedListener listener){
        compressCompletedListener=listener;
    }
    public void setOnVideoCompressProgressListener(onVideoCompressProgressListener listener){
        progressListener=listener;
    }
    public static boolean isNeedCompress(String path)
    {
        MediaInfo info=new MediaInfo(path);
        if(LanSongFileUtil.getFileSize(path)<=3.0f) {  //3M
            return false;

        }else if(info.prepare()) {
            if (info.getWidth() * info.getHeight() >= 1080 * 1920) { //1080p
                if (info.vBitRate >= 15 * 1024 * 1024) {
                    return true;
                }
            }else if(info.getWidth() * info.getHeight() >=1280*720){ //720P
                if(info.vBitRate>=8*1024*1024){
                    return true;
                }
            }else if(info.getWidth() * info.getHeight()>=540*960){//540P
                if(info.vBitRate>=6*1024*1024){
                    return true;
                }
            }
            if(info.getHeight()* info.getWidth()>=1280*720){
                int bitrate=getSuggestBitRate(544*960);
                if(info.vBitRate >(bitrate*5)){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean start() {
        if (isExecuting)
            return false;

        mediaInfo=new MediaInfo(srcVideo);
        if(mediaInfo.prepare()){
            isExecuting = true;

            dstVideo= LanSongFileUtil.createMp4FileInBox();
            ScaleExecute vScale = new ScaleExecute(context, srcVideo);  //videoPath是路径
            vScale.setOutputPath(dstVideo);

            int scaleWidth=mediaInfo.getWidth();
            int scaleHeight=mediaInfo.getHeight();

            if(mediaInfo.vBitRate>10*1024*1024){
                scaleWidth=mediaInfo.getWidth();
                scaleHeight=mediaInfo.getHeight();
            }else{  //做缩放处理.
                if(mediaInfo.vWidth* mediaInfo.vHeight==1280*720){
                    if(mediaInfo.getWidth()>mediaInfo.getHeight()){
                        scaleWidth=960;
                        scaleHeight=544;
                    }else {
                        scaleWidth=544;
                        scaleHeight=960;
                    }
                }else if(mediaInfo.vWidth * mediaInfo.vHeight==1920*1080){
                    if(mediaInfo.getWidth()>mediaInfo.getHeight()){
                        scaleWidth=960;
                        scaleHeight=544;
                    }else {
                        scaleWidth=544;
                        scaleHeight=960;
                    }
                }else if(mediaInfo.vWidth * mediaInfo.vHeight>1920*1080){
                    scaleWidth=mediaInfo.getWidth()/2;
                    scaleHeight=mediaInfo.getHeight()/2;
                }else if(mediaInfo.vWidth * mediaInfo.vHeight>1280*720){
                    scaleWidth=mediaInfo.getWidth()/2;
                    scaleHeight=mediaInfo.getHeight()/2;
                }
            }

            vScale.setScaleSize(scaleWidth, scaleHeight, getSuggestBitRate(scaleHeight * scaleWidth));

            //设置缩放进度监听.currentTimeUS当前处理的视频帧时间戳.
            vScale.setScaleProgessListener(new onScaleProgressListener() {

                @Override
                public void onProgress(ScaleExecute v, long currentTimeUS) {
                    if(progressListener!=null){
                        float time = (float) currentTimeUS / (float)(mediaInfo.vDuration*1000000);
                        int b = Math.round(time * 100);
                        progressListener.onProgress(b);
                    }
                }
            });

            //设置缩放进度完成后的监听.
            vScale.setScaleCompletedListener(new onScaleCompletedListener() {

                @Override
                public void onCompleted(ScaleExecute v) {
                    isExecuting = false;

                    if(compressCompletedListener!=null){

                        if (LanSongFileUtil.fileExist(dstVideo) && mediaInfo.isHaveAudio()) {
                            String retPath = AudioEditor.mergeAVDirectly(srcVideo, dstVideo,true);
                            compressCompletedListener.onCompleted(retPath);
                        }else{
                            compressCompletedListener.onCompleted(dstVideo);
                        }
                    }
                }
            });
            return vScale.start();
        }else {
            return false;
        }
    }
    public static int getSuggestBitRate(int wxh) {
        if (wxh <= 480 * 480) {
            return 1000 * 1024;
        } else if (wxh <= 640 * 480) {
            return 1500 * 1024;
        } else if (wxh <= 800 * 480) {
            return 1800 * 1024;
        } else if (wxh <= 960 * 544) {
            return 2048 * 1024;
        } else if (wxh <= 1280 * 720) {
            return (int)(2.5*1024 * 1024);
        } else if (wxh <= 1920 * 1088) {
            return (3*1024 * 1024);
        } else {
            return 3500 * 1024;
        }
    }
}
