package com.zhaoss.weixinrecorded.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import com.libyuv.LibyuvUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordUtil {

    public final static int TIMEOUT_USEC = 10000;
    public final static int frameRate = 25;
    public final static int frameTime = 1000/frameRate;
    public final static int sampleRateInHz = 44100;
    public final static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    public final static int channelCount = 1;

    private int audioBufferSize; //缓存大小

    private ArrayBlockingQueue<byte[]> videoQueue = new ArrayBlockingQueue<>(3);
    private int videoWidth;
    private int videoHeight;
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private AudioRecord audioRecord;
    private MediaCodec videoMediaCodec;
    private FileOutputStream videoOut;
    private FileOutputStream audioOut;
    private int rotation;
    private boolean isFrontCamera;

    public RecordUtil(String videoPath, String audioPath, int videoWidth, int videoHeight, int rotation, boolean isFrontCamera){

        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.rotation = rotation;
        this.isFrontCamera = isFrontCamera;

        try {
            initVideoMediaCodec();
            initAudioRecord();

            File videoFile = new File(videoPath);
            if(videoFile.exists()){
                videoFile.delete();
            }
            videoFile.createNewFile();
            videoOut = new FileOutputStream(videoFile);

            File audioFile = new File(audioPath);
            if(audioFile.exists()){
                audioFile.delete();
            }
            audioFile.createNewFile();
            audioOut = new FileOutputStream(audioFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initVideoMediaCodec()throws Exception{
        MediaFormat mediaFormat;
        if(rotation==90 || rotation==270){
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoHeight, videoWidth);
        }else{
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight);
        }
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoWidth*videoHeight*3);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        videoMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        videoMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        videoMediaCodec.start();
    }

    private void initAudioRecord(){
        audioBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT, audioBufferSize);
    }

    public OnPreviewFrameListener start(){
        isRecording.set(true);
        startRecordAudio();
        startWhile();
        return mOnPreviewFrameListener;
    }

    private OnPreviewFrameListener mOnPreviewFrameListener = new OnPreviewFrameListener() {
        @Override
        public void onPreviewFrame(final byte[] data) {
            if (videoQueue.size() < 3) {
                videoQueue.add(data);
            }
        }
    };

    public interface OnPreviewFrameListener{
        void onPreviewFrame(byte[] data);
    }

    private void startRecordAudio(){
        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<Boolean>() {
            @Override
            public Boolean doInBackground() throws Throwable {
                audioRecord.startRecording();
                while (isRecording.get()) {
                    byte[] data = new byte[audioBufferSize];
                    if (audioRecord.read(data, 0, audioBufferSize) != AudioRecord.ERROR_INVALID_OPERATION) {
                        audioOut.write(data);
                    }
                }
                return true;
            }
            @Override
            public void onFinish(Boolean result) {
            }
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        });
    }

    private void startWhile(){
        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<Boolean>() {
            @Override
            public Boolean doInBackground() throws Throwable {

                startTime = System.currentTimeMillis();
                while (isRecording.get() || videoQueue.size()>0) {
                    byte[] videoData = videoQueue.poll();
                    if(videoData != null){
                        encodeVideo(videoData);
                    }
                }
                return true;
            }
            @Override
            public void onFinish(Boolean result) {
                release();
            }
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                release();
            }
        });
    }

    private byte[] configByte;
    private void encodeVideo(byte[] nv21)throws IOException {

        currFrame++;
        if(checkMaxFrame()){
            currFrame--;
            return ;
        }

        byte[] nv12 = new byte[nv21.length];
        byte[] yuvI420 = new byte[nv21.length];
        byte[] tempYuvI420 = new byte[nv21.length];

        LibyuvUtil.convertNV21ToI420(nv21, yuvI420, videoWidth, videoHeight);
        LibyuvUtil.compressI420(yuvI420, videoWidth, videoHeight, tempYuvI420, videoWidth, videoHeight, rotation, isFrontCamera);
        LibyuvUtil.convertI420ToNV12(tempYuvI420, nv12, videoWidth, videoHeight);

        //得到编码器的输入和输出流, 输入流写入源数据 输出流读取编码后的数据
        //得到要使用的缓存序列角标
        int inputIndex = videoMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputIndex >= 0) {
            ByteBuffer inputBuffer = videoMediaCodec.getInputBuffer(inputIndex);
            inputBuffer.clear();
            //把要编码的数据添加进去
            inputBuffer.put(nv12);
            //塞到编码序列中, 等待MediaCodec编码
            videoMediaCodec.queueInputBuffer(inputIndex, 0, nv12.length,  System.nanoTime()/1000, 0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        //读取MediaCodec编码后的数据
        int outputIndex = videoMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        byte[] frameData = null;
        int destPos = 0;
        while (outputIndex >= 0) {
            ByteBuffer outputBuffer = videoMediaCodec.getOutputBuffer(outputIndex);
            byte[] h264 = new byte[bufferInfo.size];
            //这步就是编码后的h264数据了
            outputBuffer.get(h264);
            switch (bufferInfo.flags) {
                case MediaCodec.BUFFER_FLAG_CODEC_CONFIG://视频信息
                    configByte = new byte[bufferInfo.size];
                    configByte = h264;
                    break;
                case MediaCodec.BUFFER_FLAG_KEY_FRAME://关键帧
                    videoOut.write(configByte, 0, configByte.length);
                    videoOut.write(h264, 0, h264.length);
                    break;
                default://正常帧
                    videoOut.write(h264, 0, h264.length);
                    if(frameData == null) {
                        frameData = new byte[bufferInfo.size];
                    }
                    System.arraycopy(h264, 0, frameData, destPos, h264.length);
                    break;
            }
            videoOut.flush();
            //数据写入本地成功 通知MediaCodec释放data
            videoMediaCodec.releaseOutputBuffer(outputIndex, false);
            //读取下一次编码数据
            outputIndex = videoMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

            if(frameData != null){
                if(outputIndex >= 0){
                    destPos = frameData.length;
                    byte[] temp = new byte[frameData.length + bufferInfo.size];
                    System.arraycopy(frameData, 0, temp, 0, frameData.length);
                    frameData = temp;
                }else{
                    if(checkMinFrame()){
                        //currFrame++;
                        //videoOut.write(frameData, 0, frameData.length);
                        //Log.i("Log.i", frameData.length+"   aaa");
                    }
                }
            }
        }
    }

    private long startTime = 0;
    private int currFrame = 0;
    private boolean checkMaxFrame(){

        int rightFrame = (int) ((System.currentTimeMillis()-startTime)/frameTime);
        if (currFrame > rightFrame) {
            return true;
        }else{
            return false;
        }
    }

    private boolean checkMinFrame(){

        int rightFrame = (int) ((System.currentTimeMillis()-startTime)/frameTime);
        if (currFrame < rightFrame) {
            return true;
        }else{
            return false;
        }
    }

    public void stop() {
        isRecording.set(false);
    }

    public Boolean isRecording() {
        return isRecording.get();
    }

    public void release(){
        try {
            audioRecord.stop();
            audioRecord.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            videoMediaCodec.stop();
            videoMediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] rotateYUVDegree270AndMirror(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate and mirror the Y luma
        int i = 0;
        int maxY = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            maxY = imageWidth * (imageHeight - 1) + x * 2;
            for (int y = 0; y < imageHeight; y++) {
                yuv[i] = data[maxY - (y * imageWidth + x)];
                i++;
            }
        }
        // Rotate and mirror the U and V color components
        int uvSize = imageWidth * imageHeight;
        i = uvSize;
        int maxUV = 0;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            maxUV = imageWidth * (imageHeight / 2 - 1) + x * 2 + uvSize;
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[maxUV - 2 - (y * imageWidth + x - 1)];
                i++;
                yuv[i] = data[maxUV - (y * imageWidth + x)];
                i++;
            }
        }
        return yuv;
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
}