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
    public final static int frameRate = 24;
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
    private ByteBuffer frameBuffer;

    public RecordUtil(String videoPath, String audioPath, int videoWidth, int videoHeight, int rotation, boolean isFrontCamera){

        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.rotation = rotation;
        this.isFrontCamera = isFrontCamera;
        frameBuffer = ByteBuffer.allocateDirect(1024*100);

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
            //设置视频宽高
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoHeight, videoWidth);
        }else{
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight);
        }
        //图像数据格式 YUV420
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        //码率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoWidth*videoHeight*3);
        //每秒30帧
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        //1秒一个关键帧
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

        boolean keyFrame = false;
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
                    frameBuffer.put(configByte);
                    frameBuffer.put(h264);
                    keyFrame = true;
                    break;
                default://正常帧
                    frameBuffer.put(h264);
                    break;
            }
            //数据写入本地成功 通知MediaCodec释放data
            videoMediaCodec.releaseOutputBuffer(outputIndex, false);
            //读取下一次编码数据
            outputIndex = videoMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        }

        if(frameBuffer.position() > 0){
            byte[] frameByte = new byte[frameBuffer.position()];
            frameBuffer.flip();
            frameBuffer.get(frameByte);
            frameBuffer.clear();

            currFrame++;
            videoOut.write(frameByte, 0, frameByte.length);
            videoOut.flush();

            while (keyFrame && checkMinFrame()){
                currFrame++;
                videoOut.write(frameByte, 0, frameByte.length);
                videoOut.flush();
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
        try {
            if(audioOut!=null){
                audioOut.close();
            }
            if(videoOut!=null){
                videoOut.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}