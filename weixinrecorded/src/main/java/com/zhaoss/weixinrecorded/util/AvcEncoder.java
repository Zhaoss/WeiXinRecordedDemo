package com.zhaoss.weixinrecorded.util;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AvcEncoder {

    private int TIMEOUT_USEC = 12000;
    private int width;
    private int height;
    private int frameRate;
    private byte[] configByte;
    private ArrayBlockingQueue<byte[]> YUVQueue;
    private MediaCodec mediaCodec;
    private BufferedOutputStream outputStream;
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public AvcEncoder(int width, int height, ArrayBlockingQueue<byte[]> YUVQueue) {
        this.YUVQueue = YUVQueue;
        this.width = width;
        this.height = height;
        frameRate = 24;

        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width*height*5);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning(){
        return isRunning.get();
    }

    public void stopEncoder(){
        try {
            isRunning.set(false);
            mediaCodec.stop();
            mediaCodec.release();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startEncoder(final String videoPath, final boolean isFrontCamera){

        isRunning.set(true);
        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<String>() {
            @Override
            public String doInBackground() throws Throwable {

                outputStream = new BufferedOutputStream(new FileOutputStream(videoPath));
                byte[] input = null;
                long pts = 0;
                long generateIndex = 0;

                while (isRunning.get()) {
                    if (YUVQueue.size() >0){
                        input = YUVQueue.poll();
                        byte[] yuv420sp = new byte[width * height *3/2];
                        NV21ToNV12(input,yuv420sp, width, height);

                        if(isFrontCamera){
                            yuv420sp = rotateYUV420Degree180(yuv420sp, width, height);
                        }

                        input = yuv420sp;
                    }
                    if (input != null) {
                        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            pts = computePresentationTime(generateIndex);
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(input);
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                            generateIndex += 1;
                        }

                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                            byte[] outData = new byte[bufferInfo.size];
                            outputBuffer.get(outData);
                            if(bufferInfo.flags == 2){
                                configByte = new byte[bufferInfo.size];
                                configByte = outData;
                            }else if(bufferInfo.flags == 1){
                                byte[] keyframe = new byte[bufferInfo.size + configByte.length];
                                System.arraycopy(configByte, 0, keyframe, 0, configByte.length);
                                System.arraycopy(outData, 0, keyframe, configByte.length, outData.length);

                                outputStream.write(keyframe, 0, keyframe.length);
                            }else{
                                outputStream.write(outData, 0, outData.length);
                            }
                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                        }
                    } else {
                        Thread.sleep(500);
                    }
                }
                return null;
            }
            @Override
            public void onFinish(String result) {

            }
            @Override
            public void onError(Throwable e) {

            }
        });
    }

    private byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        int count = 0;

        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }

        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

    private void NV21ToNV12(byte[] nv21,byte[] nv12,int width,int height){
        if(nv21 == null || nv12 == null)return;
        int framesize = width*height;
        int i = 0,j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for(i = 0; i < framesize; i++){
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize/2; j+=2) {
            nv12[framesize + j-1] = nv21[j+framesize];
        }
        for (j = 0; j < framesize/2; j+=2) {
            nv12[framesize + j] = nv21[j+framesize-1];
        }
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / frameRate;
    }
}