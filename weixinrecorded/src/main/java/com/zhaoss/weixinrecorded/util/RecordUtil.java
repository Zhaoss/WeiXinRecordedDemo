package com.zhaoss.weixinrecorded.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordUtil {

    public static int TIMEOUT_USEC = 12000;
    public static int frameRate = 30;
    public static int sampleRateInHz = 44100;
    public static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    public static int channelCount = 1;

    private int bufferSize; //缓存大小

    private MediaCodec videoMediaCodec;
    private MediaMuxer mediaMuxer;
    private ArrayBlockingQueue<byte[]> videoQueue;
    private ArrayBlockingQueue<byte[]> audioQueue;
    private int width;
    private int height;
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private int videoTrackIndex = -1;
    private int audioTrackIndex = 1;
    private AudioRecord audioRecord;
    private MediaCodec audioMediaCodec;

    public RecordUtil(String videoPath, int width, int height, ArrayBlockingQueue<byte[]> videoQueue){
        this.videoQueue = videoQueue;
        this.width = width;
        this.height = height;

        audioQueue = new ArrayBlockingQueue<>(10);
        try {
            initVideoMediaCodec();
            initAudioMediaCodec();
            initMediaMuxer(videoPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initMediaMuxer(String videoPath)throws Exception{
        mediaMuxer = new MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
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

    private void initAudioMediaCodec()throws Exception{

        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, channelCount);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, sampleRateInHz*2);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        audioMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioMediaCodec.start();
    }

    public void start(){
        isRecording.set(true);
        audioRecord.startRecording();
        startRecordAudio();
        startWhile();
    }

    private void startRecordAudio(){
        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<Boolean>() {
            @Override
            public Boolean doInBackground() throws Throwable {
                //等待音频初始化完毕.
                audioRecord.startRecording();
                while (isRecording.get()) {
                    byte[] data = new byte[bufferSize];
                    if (audioRecord.read(data, 0, bufferSize) != AudioRecord.ERROR_INVALID_OPERATION) {
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

                while (isRecording.get() || videoQueue.size()>0 || audioQueue.size()>0) {
                    byte[] videoData = videoQueue.poll();
                    if(videoData != null){
                        encodeVideo(videoData);
                    }
                    byte[] audioData = audioQueue.poll();
                    if(audioData != null){
                        //encodeAudio(audioData);
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

    private void encodeAudio(byte[] data){
        int inputBufferIndex = audioMediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = audioMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(data);
            audioMediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length, System.nanoTime()/1000, 0);

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = audioMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if(outputBufferIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && audioTrackIndex==-1){
                audioTrackIndex = mediaMuxer.addTrack(audioMediaCodec.getOutputFormat());
                ready();
            }
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = audioMediaCodec.getOutputBuffer(outputBufferIndex);

                if(audioTrackIndex >= 0) {
                    mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo);
                }

                audioMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = audioMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            }
        }
    }

    private void encodeVideo(byte[] data){

        byte[] yuv420sp = new byte[width * height *3/2];
        NV21ToNV12(data, yuv420sp, width, height);
        data = yuv420sp;
        //得到要使用的缓存序列角标
        int inputBufferIndex = videoMediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = videoMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            //把要编码的数据添加进去
            inputBuffer.put(data);
            //塞到编码序列中, 等待MediaCodec编码
            videoMediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length, System.nanoTime()/1000, 0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        //读取MediaCodec编码后的数据
        int outputBufferIndex = videoMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        if(outputBufferIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && videoTrackIndex==-1){
            videoTrackIndex = mediaMuxer.addTrack(videoMediaCodec.getOutputFormat());
            ready();
        }
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = videoMediaCodec.getOutputBuffer(outputBufferIndex);

            if(videoTrackIndex>=0 && audioTrackIndex>=0){
                mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
            }

            //数据写入本地成功 通知MediaCodec释放data
            videoMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            //读取下一次编码数据
            outputBufferIndex = videoMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        }
    }

    private void ready(){
        if(videoTrackIndex>=0 && audioTrackIndex>=0){
            mediaMuxer.start();
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
            mediaMuxer.stop();
            mediaMuxer.release();
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
            audioMediaCodec.stop();
            audioMediaCodec.release();
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