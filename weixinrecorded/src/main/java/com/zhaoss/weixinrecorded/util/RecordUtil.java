package com.zhaoss.weixinrecorded.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

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

    private ArrayBlockingQueue<byte[]> videoQueue;
    private ArrayBlockingQueue<byte[]> audioQueue = new ArrayBlockingQueue<>(10);
    private int width;
    private int height;
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private AudioRecord audioRecord;
    private MediaCodec videoMediaCodec;
    private FileOutputStream videoOut;
    private FileOutputStream audioOut;
    private boolean isFrontCamera;

    public RecordUtil(String videoPath, String audioPath, int width, int height, ArrayBlockingQueue<byte[]> videoQueue){

        this.videoQueue = videoQueue;
        this.width = width;
        this.height = height;

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
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width*height*3);
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

    public void start(boolean isFrontCamera){
        this.isFrontCamera = isFrontCamera;
        isRecording.set(true);
        startRecordAudio();
        startWhile();
    }

    private void startRecordAudio(){
        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<Boolean>() {
            @Override
            public Boolean doInBackground() throws Throwable {
                audioRecord.startRecording();
                while (isRecording.get()) {
                    byte[] data = new byte[audioBufferSize];
                    if (audioRecord.read(data, 0, audioBufferSize) != AudioRecord.ERROR_INVALID_OPERATION) {
                        if(audioQueue.size() >= 10){
                            audioQueue.poll();
                        }
                        audioQueue.add(data);
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
                while (isRecording.get() || videoQueue.size()>0 || audioQueue.size()>0) {
                    byte[] videoData = videoQueue.poll();
                    if(videoData != null){
                        encodeVideo(videoData);
                    }
                    byte[] audioData = audioQueue.poll();
                    if(audioData != null){
                        audioOut.write(audioData);
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

    private long startTime = 0;
    private int frameNum = 0;
    private byte[] configByte;
    private void encodeVideo(byte[] data)throws IOException {

        frameNum++;
        int rightTime = frameNum*frameTime;
        int runTime = (int) (System.currentTimeMillis()-startTime+frameTime);
        if (runTime < rightTime) {
            frameNum--;
            return ;
        }

        byte[] yuv420sp = new byte[width * height * 3 / 2];
        NV21ToNV12(data, yuv420sp, width, height);
        if (isFrontCamera) {
            yuv420sp = rotateYUV420Degree180(yuv420sp, width, height);
        }
        data = yuv420sp;
        //得到编码器的输入和输出流, 输入流写入源数据 输出流读取编码后的数据
        //得到要使用的缓存序列角标
        int inputBufferIndex = videoMediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = videoMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            //把要编码的数据添加进去
            inputBuffer.put(data);
            //塞到编码序列中, 等待MediaCodec编码
            videoMediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length,  System.nanoTime()/1000, 0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        //读取MediaCodec编码后的数据
        int outputBufferIndex = videoMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = videoMediaCodec.getOutputBuffer(outputBufferIndex);
            byte[] outData = new byte[bufferInfo.size];
            //这步就是编码后的h264数据了
            outputBuffer.get(outData);
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {//视频信息
                configByte = new byte[bufferInfo.size];
                configByte = outData;
            } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {//关键帧
                byte[] keyframe = new byte[bufferInfo.size + configByte.length];
                System.arraycopy(configByte, 0, keyframe, 0, configByte.length);
                System.arraycopy(outData, 0, keyframe, configByte.length, outData.length);
                videoOut.write(keyframe, 0, keyframe.length);
            } else {//正常的媒体数据
                videoOut.write(outData, 0, outData.length);
            }
            videoOut.flush();
            //数据写入本地成功 通知MediaCodec释放data
            videoMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            //读取下一次编码数据
            outputBufferIndex = videoMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        }
    }

    public void stop() {
        isRecording.set(false);
    }

    public Boolean isRecording() {
        return isRecording.get();
    }

    private void release(){
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