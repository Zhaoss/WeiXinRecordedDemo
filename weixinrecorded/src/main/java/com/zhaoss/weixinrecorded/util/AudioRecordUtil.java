package com.zhaoss.weixinrecorded.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecordUtil {

    public static int TIMEOUT_USEC = 10000;
    public static int sampleRateInHz = 44100;
    public static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    public static int channelCount = 1;

    private int bufferSize; //缓存大小
    private volatile AtomicBoolean isRecording = new AtomicBoolean(false);
    private AudioRecord audioRecord;
    private BufferedOutputStream fileOut;

    private MediaCodec mediaCodec;
    private MediaCodec.BufferInfo bufferInfo;
    private ArrayBlockingQueue<byte[]> mYUVQueue = new ArrayBlockingQueue<>(10);

    public AudioRecordUtil(){
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        //麦克风 采样率 单声道 音频格式, 缓存大小
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    private void initMediaCodec(){

        try{
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, channelCount);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, sampleRateInHz*2);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startRecord(final String path, final boolean isPcmData){

        isRecording.set(true);

        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<String>() {
            @Override
            public String doInBackground() throws Throwable {
                //等待音频初始化完毕.
                while (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                    Thread.sleep(10);
                }

                if(!isPcmData){
                    initMediaCodec();
                }
                audioRecord.startRecording();
                while (isRecording.get()) {
                    byte[] data = new byte[bufferSize];
                    int read = audioRecord.read(data, 0, bufferSize);
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        mYUVQueue.add(data);
                    }
                }
                return "";
            }
            @Override
            public void onFinish(String result) {
            }
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        });

        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<String>() {
            @Override
            public String doInBackground() throws Throwable {

                fileOut = new BufferedOutputStream(new FileOutputStream(path));
                bufferInfo = new MediaCodec.BufferInfo();

                while (isRecording.get() || mYUVQueue.size()>0){
                    byte[] data = mYUVQueue.poll();
                    if(data != null){
                        if(isPcmData){
                            fileOut.write(data);
                        }else{
                            fileOut.write(encodeData(data));
                        }
                    }
                }
                return "";
            }
            @Override
            public void onFinish(String result) {
                release();
            }
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                release();
            }
        });
    }

    public void stopRecord(){
        isRecording.set(false);
    }

    /**
     * 开始编码
     **/
    private byte[] encodeData(byte[] bytes) throws Exception {

        //其中需要注意的有dequeueInputBuffer（-1），参数表示需要得到的毫秒数，-1表示一直等，0表示不需要等，传0的话程序不会等待，但是有可能会丢帧。
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.limit(bytes.length);
            inputBuffer.put(bytes);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytes.length, 0, 0);
        }

        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        while (outputBufferIndex >= 0) {
            int outBitsSize = bufferInfo.size;
            int outPacketSize = outBitsSize + 7; // 7 is ADTS size
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + outBitsSize);

            //添加ADTS头
            byte[] outData = new byte[outPacketSize];
            addADTStoPacket(outData, outPacketSize);

            outputBuffer.get(outData, 7, outBitsSize);
            outputBuffer.position(bufferInfo.offset);

            //写到输出流里
            outputStream.write(outData);

            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        }

        byte[] out = outputStream.toByteArray();
        outputStream.close();

        return out;
    }

    /**
     * 给编码出的aac裸流添加adts头字段
     *
     * @param packet    要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private void release(){

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if(mediaCodec != null){
            try {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(fileOut != null){
            try {
                fileOut.close();
                fileOut = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
