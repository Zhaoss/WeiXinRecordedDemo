package com.zhaoss.weixinrecorded.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecordUtil {

    private static int sampleRateInHz = 44100;

    private int bufferSize; //缓存大小
    private volatile AtomicBoolean isRecording = new AtomicBoolean(false);
    private AudioRecord audioRecord;
    private BufferedOutputStream out;

    private MediaCodec mediaCodec;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo bufferInfo;
    //pts时间基数
    private long presentationTimeUs = 0;
    private ByteArrayOutputStream outputStream;

    public AudioRecordUtil(){
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }

    public void startRecord(final String path){

        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<String>() {
            @Override
            public String doInBackground() throws Throwable {
                out = new BufferedOutputStream(new FileOutputStream(path));
                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,//麦克风
                        sampleRateInHz,//采样率
                        AudioFormat.CHANNEL_IN_MONO,//单声道
                        AudioFormat.ENCODING_PCM_16BIT,//音频格式
                        bufferSize);
                initMediaCodec();

                //等待音频初始化完毕.
                while (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                    Thread.sleep(10);
                }

                isRecording.set(true);
                audioRecord.startRecording();
                while (isRecording.get()) {
                    byte[] data = new byte[bufferSize];
                    int read = audioRecord.read(data, 0, bufferSize);
                    byte[] cordByte = encodeData(data);
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        out.write(cordByte);
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

    private void release(){

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if(out != null){
            try {
                out.close();
                out = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        closeMediaCodec();
    }

    //创建一个输入流用来输出转换的数据

    private void initMediaCodec()throws IOException {

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);

        //初始化   此格式使用的音频编码技术、音频采样率、使用此格式的音频信道数（单声道为 1，立体声为 2）
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);

        mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        //比特率 声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3000*1024);

        //传入的数据大小
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 2048);
        //设置相关参数
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //开始
        mediaCodec.start();

        inputBuffers = mediaCodec.getInputBuffers();
        outputBuffers = mediaCodec.getOutputBuffers();
        bufferInfo = new MediaCodec.BufferInfo();

        outputStream = new ByteArrayOutputStream();
    }

    /**
     * 关闭释放资源
     **/
    public void closeMediaCodec() {
        try {
            mediaCodec.stop();
            mediaCodec.release();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始编码
     **/
    public byte[] encodeData(byte[] bytes) throws Exception {

        //其中需要注意的有dequeueInputBuffer（-1），参数表示需要得到的毫秒数，-1表示一直等，0表示不需要等，传0的话程序不会等待，但是有可能会丢帧。
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(bytes);
            inputBuffer.limit(bytes.length);

            //计算pts
            long pts = computePresentationTime(presentationTimeUs);

            mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytes.length, pts, 0);
            presentationTimeUs += 1;
        }


        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

        while (outputBufferIndex >= 0) {
            int outBitsSize = bufferInfo.size;
            int outPacketSize = outBitsSize + 7; // 7 is ADTS size
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

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
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }

        byte[] out = outputStream.toByteArray();
        outputStream.flush();
        outputStream.reset();

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

    //计算PTS，实际上这个pts对应音频来说作用并不大，设置成0也是没有问题的
    private long computePresentationTime(long frameIndex) {
        return frameIndex * 90000 * 1024 / 44100;
    }
}
