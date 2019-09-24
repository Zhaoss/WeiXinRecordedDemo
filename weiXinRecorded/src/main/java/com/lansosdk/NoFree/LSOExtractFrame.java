package com.lansosdk.NoFree;

import android.content.Context;
import android.graphics.Bitmap;

import com.lansosdk.box.ExtractVideoFrame;
import com.lansosdk.box.onExtractVideoFrameCompletedListener;
import com.lansosdk.box.onExtractVideoFrameErrorListener;
import com.lansosdk.box.onExtractVideoFrameProgressListener;

import java.util.List;

public class LSOExtractFrame {
    private ExtractVideoFrame extractFrame;

    public LSOExtractFrame(Context ctx, String path) {
        if (extractFrame == null) {
            extractFrame = new ExtractVideoFrame(ctx, path);
        }
    }

    /**
     * 设置在获取图片的时候, 可以指定图片的宽高, 指定后, 视频帧画面会被缩放到指定的宽高.
     *
     * @param width  缩放宽度
     * @param height 缩放高度
     */
    public void setBitmapWH(int width, int height) {
        if (extractFrame != null) {
            extractFrame.setBitmapWH(width, height);
        }
    }

    public int getBitmapWidth() {
        if (extractFrame != null) {
            return extractFrame.getBitmapWidth();
        } else {
            return 0;
        }
    }


    public int getBitmapHeight() {
        if (extractFrame != null) {
            return extractFrame.getBitmapHeight();
        } else {
            return 0;
        }
    }
    /**
     * 取bitmap的间隔, 即解码好后, 每隔几个返回一个bitmap, 用在需要列出视频一部分,但不需要全部的场合,比如预览缩略图.
     * <p>
     * 如果设置时间,则从开始时间后, 查当前解码好几个图层,然后做间隔返回bitmap
     * <p>
     * 比如设置间隔是3, 则bitmap在第 0个返回, 第4 8,12,16个返回.
     * <p>
     * 可以用MediaInfo或FrameInfo得到当前视频中总共有多少帧,用FrameInfo可以得到每一帧的时间戳.
     *
     * @param frames
     */
    public void setExtractInterval(int frames) {
        if (extractFrame != null) {
            extractFrame.setExtractInterval(frames);
        }
    }

    /**
     * 设置提取的时间间隔;
     *
     * @param intervalUs
     */
    public void setExtractIntervalWithTimeUs(long intervalUs) {
        if (extractFrame != null) {
            extractFrame.setExtractIntervalWithTimeUs(intervalUs);
        }
    }

    /**
     * 平均获取25帧数据.
     */
    public void setExtract25Frame() {
        if (extractFrame != null) {
            extractFrame.setExtract25Frame();
        }
    }

    /**
     * 平均提取60帧
     */
    public void setExtract60Frame() {
        if (extractFrame != null) {
            extractFrame.setExtract60Frame();
        }
    }

    /**
     * 设置提取多少帧.
     *
     * @param some
     */
    public void setExtractSomeFrame(int some) {
        if (extractFrame != null) {
            extractFrame.setExtractSomeFrame(some);
        }
    }

    /**
     * 设置在哪几个时间点取视频帧
     * @param timeArray
     */
    public void setExtractFrame(List<Long> timeArray) {
        if (extractFrame != null) {
            extractFrame.setExtractFrame(timeArray);
        }
    }

    /**
     * 开始执行
     */
    public void start() {
        if (extractFrame != null) {
            setAllListener();
            extractFrame.start();
        }
    }
    /**
     * 开始执行, 可以指定从某个地方开始解码
     *
     * @param startTimeUS 指定开始时间,单位微秒.
     */
    public void start(long startTimeUS) {
        if (extractFrame != null) {
            setAllListener();
            extractFrame.start(startTimeUS);
        }
    }

    /**
     * //内部开启一个线程去执行, 利用Handler机制把数据传递过来, 因为Handler队列可能有积压的message, 可能在停止后,
     * 还会收到一两帧的数据.这是正常的情况.
     */
    public void stop() {
        if (extractFrame != null) {
            extractFrame.stop();
        }
    }
    /**
     * 您可以一帧一帧的读取, seekPause是指:seek到指定帧后, 调用回调就暂停. 单位是us, 微秒. 单独读取的话, 把这里打开
     * 如果您频繁的读取, 建议直接一次性读取完毕,放到sd卡里,然后用的时候, 从sd卡中读取.
     */
    public void seekPause(long timeUs) {
        if (extractFrame != null) {
            extractFrame.seekPause(timeUs);
        }
    }

    /**
     * 释放
     */
    public void release() {
        stop();
    }


    private onExtractFrameProgressListener monExtractFrameProgressListener;
    private onExtractFrameCompletedListener monExtractFrameCompletedListener;
    private onExtractFrameErrorListener monExtractFrameErrorListener;
    /**
     * 进度监听
     * @param listener
     */
    public void setOnExtractProgressListener(
            onExtractFrameProgressListener listener) {
        monExtractFrameProgressListener=listener;
    }

    /**
     * 完成监听
     * @param listener
     */
    public void setOnExtractCompletedListener(onExtractFrameCompletedListener listener) {
        monExtractFrameCompletedListener=listener;
    }

    /**
     * 错误监听;
     * @param listener
     */
    public void setOnExtractFrameErrorListener(onExtractFrameErrorListener listener) {
        monExtractFrameErrorListener=listener;
    }
    private void setAllListener()
    {
        if(extractFrame!=null){

            extractFrame.setOnExtractVideoFrameErrorListener(new onExtractVideoFrameErrorListener() {
                @Override
                public void onError(ExtractVideoFrame v) {
                    if(monExtractFrameErrorListener!=null){
                        monExtractFrameErrorListener.onError();
                    }
                }
            });
            extractFrame.setOnExtractProgressListener(new onExtractVideoFrameProgressListener() {
                @Override
                public void onExtractBitmap(Bitmap bmp, long ptsUS) {
                    if(monExtractFrameProgressListener!=null){
                        monExtractFrameProgressListener.onExtractBitmap(bmp,ptsUS);
                    }
                }
            });
            extractFrame.setOnExtractCompletedListener(new onExtractVideoFrameCompletedListener() {
                @Override
                public void onCompleted(ExtractVideoFrame v) {
                    if(monExtractFrameCompletedListener!=null){
                        monExtractFrameCompletedListener.onCompleted();
                    }
                }
            });
        }
    }
}
