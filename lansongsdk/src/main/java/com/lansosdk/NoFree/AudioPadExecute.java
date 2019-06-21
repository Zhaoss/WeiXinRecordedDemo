package com.lansosdk.NoFree;

import android.content.Context;

import com.lansosdk.box.AudioLayer;
import com.lansosdk.box.AudioPad;
import com.lansosdk.box.LSLog;
import com.lansosdk.box.onAudioPadCompletedListener;
import com.lansosdk.box.onAudioPadProgressListener;
import com.lansosdk.box.onAudioPadThreadProgressListener;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * 音频容器;
 *
 *  使用1:
 *      给视频增加其他声音,并设置各自的音量,循环,快慢,变声等.
 *      视频可以有声音,或无声音.
 *
 *  使用2:
 *      设置容器的固定时长, 分别在指定的时间点增加上声音.
 *
 *
 * 当前的音频格式支持 MP3,WAV, AAC(m4a后缀)
 */
public class AudioPadExecute {

    private static final String TAG = LSLog.TAG;
    static AudioLayer audioSrc1;
    static long starttime = 0;
    AudioPad audioPad;
    private  String audioPadSavePath;
    private MediaInfo mediaInfo;
    private AudioLayer mainSource;
    private  String inputPath;
    private  onAudioPadExecuteCompletedListener monAudioPadExecuteCompletedListener;
    private  ArrayList<String> deleteArray=new ArrayList<>();

    public interface onAudioPadExecuteCompletedListener {
        void onCompleted(String path);
    }
    /**
     * 构造方法
     * @param ctx
     * @param input  输入如是音频则返回的是m4a的音频文件; 如是视频 则返回的是mp4的视频文件
     */
    public AudioPadExecute(Context ctx, String input) {
        mediaInfo=new MediaInfo(input);
        if(mediaInfo.prepare()){
            audioPadSavePath = LanSongFileUtil.createM4AFileInBox();
            inputPath=input;
            audioPad = new AudioPad(ctx, audioPadSavePath);
            if(mediaInfo.isHaveAudio()){
                mainSource=audioPad.addMainAudio(input);
            }else{
                mainSource=audioPad.addMainAudio(mediaInfo.vDuration,44100);
            }
        }else{
            LSLog.e("您输入的视频不正常, 请检查您的视频, 信息是:"+mediaInfo);
        }
    }

    /**
     * 构造方法
     * @param ctx
     * @param input  输入如是音频则返回的是m4a的音频文件; 如是视频 则返回的是mp4的视频文件
     * @param isMute 如果是视频的话,则视频中的声音是否会静音;
     */
    public AudioPadExecute(Context ctx, String input, boolean isMute) {
        mediaInfo=new MediaInfo(input);
        if(mediaInfo.prepare()){
            audioPadSavePath = LanSongFileUtil.createM4AFileInBox();

            inputPath=input;
            audioPad = new AudioPad(ctx, audioPadSavePath);

            if(mediaInfo.isHaveAudio()){
                if(isMute) {
                    audioPad.addMainAudio(mediaInfo.aDuration, mediaInfo.aSampleRate);
                } else {
                    mainSource = audioPad.addMainAudio(input);
                }
            }else{
                mainSource=audioPad.addMainAudio(mediaInfo.vDuration,44100);
            }
        }else{
            LSLog.e("您输入的视频不正常, 请检查您的视频, 信息是:"+mediaInfo);
        }
    }

    /**
     * 构造方法.
     * 先设置音频容器的总时长, 然后一一增加各种声音;
     *
     * @param ctx
     * @param durationS  生成音频的总时长, 单位秒;
     */
    public AudioPadExecute(Context ctx, float durationS) {
        if(durationS>0){
            audioPadSavePath = LanSongFileUtil.createM4AFileInBox();
            audioPad = new AudioPad(ctx, audioPadSavePath);
            mainSource=audioPad.addMainAudio(durationS,44100);
        }else{
            LSLog.e("AudioPadExecute错误, 时间为0;");
        }
    }
    /**
     *
     * @param ctx
     * @param durationS 生成音频的总时长, 单位秒;
     * @param sampleRate 生成音频的采样率;默认44100
     */
    public AudioPadExecute(Context ctx, float durationS, int sampleRate) {
        if(durationS>0 && sampleRate>0){
            audioPadSavePath = LanSongFileUtil.createM4AFileInBox();
            audioPad = new AudioPad(ctx, audioPadSavePath);
            mainSource=audioPad.addMainAudio(durationS,sampleRate);
        }else{
            LSLog.e("AudioPadExecute错误, 时间为0;");
        }
    }

    /**
     * 在构造方法设置后, 会生成一个主音频的AudioSource,
     *
     * 拿到这个AudioSource,从而对音频做调节;
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer getMainAudioLayer() {
        return mainSource;
    }

    /**
     * 增加其他音频;
     * 支持mp4,wav,mp3,m4a文件;
     *
     * @param srcPath
     * @param isLoop  是否循环;
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, boolean  isLoop) {
        if(audioPad==null){
            LSLog.e(" AudioPadExecute addAudioLayer:失败, 可能你的构造方法的传入的参数有问题,请检查.");
            return null;
        }
        AudioLayer ret= audioPad.addSubAudio(srcPath);
        if(ret!=null){
            ret.setLooping(isLoop);
        }else{
            LSLog.e("AudioPadExecute addAudioLayer Error.MediaInfo  is:"+MediaInfo.checkFile(srcPath));
        }

        return ret;
    }

    /**
     * 增加其他音频;
     * 支持mp4,wav,mp3,m4a文件;
     *
     * @param srcPath
     * @param isLoop
     * @param valume 音频的音量; 范围是0--10; 1.0正常;大于1.0提高音量;小于1.0降低音量;
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, boolean  isLoop, float valume) {
        if(audioPad==null){
            LSLog.e(" AudioPadExecute addAudioLayer:失败, 可能你的构造方法的传入的参数有问题,请检查.");
            return null;
        }
        AudioLayer ret= audioPad.addSubAudio(srcPath);
        if(ret!=null){
            ret.setLooping(isLoop);
            ret.setVolume(valume);
        }else{
            LSLog.e("AudioPadExecute addAudioLayer Error.MediaInfo  is:"+MediaInfo.checkFile(srcPath));
        }
        return ret;
    }

    /**
     * 增加音频容器, 从容器的什么位置开始增加,
     *
     *
     * @param srcPath
     * @param startPadUs
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long startPadUs) {
        if (audioPad != null) {
            return audioPad.addSubAudio(srcPath, startPadUs, 0,-1);
        } else {
            LSLog.e(" AudioPadExecute addAudioLayer:失败, 可能你的构造方法的传入的参数有问题,请检查.");
            return null;
        }
    }
    /**
     * 把音频的 指定时间段, 增加到audiopad音频容器里.
     *
     *
     * 如果有循环或其他操作, 可以在获取的AudioSource对象中设置.
     *
     * @param srcPath      音频文件路径, 可以是有音频的视频路径;
     * @param offsetUs   从容器的什么时间开始增加(偏移多少).
     * @param startAudioUs 该音频的开始时间
     * @param endAudioUs   该音频的结束时间. 如果要增加到文件尾,则可以直接填入-1;
     * @return  返回增加后音频层, 可以用来设置音量,快慢,变声等.
     */
    public AudioLayer addAudioLayer(String srcPath, long offsetUs,
                                    long startAudioUs, long endAudioUs) {
        if (audioPad != null) {
            return audioPad.addSubAudio(srcPath, offsetUs, startAudioUs,
                    endAudioUs);
        } else {
            LSLog.e(" AudioPadExecute addAudioLayer:失败, 可能你的构造方法的传入的参数有问题,请检查.");
            return null;
        }
    }
    /**
     * 设置监听当前audioPad的处理进度.
     * <p>
     * 此监听是通过handler机制,传递到UI线程的, 你可以在里面增加ui的代码. 因为经过了handler机制,
     * 可能会进度比正在处理延迟一些,不完全等于当前处理的帧时间戳.
     *
     * @param listener
     */
    public void setOnAudioPadProgressListener(onAudioPadProgressListener listener) {
        if (audioPad != null) {
            audioPad.setAudioPadProgressListener(listener);
        }
    }

    /**
     * 设置监听当前audioPad的处理进度. 一个音频帧处理完毕后, 直接执行您listener中的代码.
     * 在audioPad线程中执行,不能在里面增加UI代码.
     * <p>
     * 建议使用这个.
     * <p>
     * 如果您声音在40s一下,建议使用这个, 因为音频本身很短,处理时间很快.
     *
     * @param listener
     */
    public void setOnAudioPadThreadProgressListener(
            onAudioPadThreadProgressListener listener) {
        if (audioPad != null) {
            audioPad.setAudioPadThreadProgressListener(listener);
        }
    }


    /**
     * 完成监听. 经过handler传递到主线程, 可以在里面增加UI代码.
     * @param listener
     */
    public void setOnAudioPadCompletedListener(
            onAudioPadExecuteCompletedListener listener) {
        monAudioPadExecuteCompletedListener=listener;
    }

    /**
     * 开启另一个线程, 并开始音频处理
     *
     * @return
     */
    public boolean start() {
        if (audioPad != null) {
            audioPad.setAudioPadCompletedListener(new onAudioPadCompletedListener() {
                public void onCompleted(AudioPad v) {
                    if(monAudioPadExecuteCompletedListener!=null){
                        monAudioPadExecuteCompletedListener.onCompleted(fileCompleted());
                    }
                }
            });
            return audioPad.start();
        } else {
            return false;
        }
    }

    /**
     * 等待执行完毕;[大部分情况下不需要调用]
     *
     * 适合在音频较短,为了代码的整洁, 不想设置listener回调的场合;
     *
     * 注意:这里设置后,
     * 当前线程将停止在这个方法处,直到音频执行完毕退出为止.建议放到另一个线程中执行. 可选使用.
     */
    public String waitComplete() {
        if (audioPad != null) {
            audioPad.joinSampleEnd();
            return fileCompleted();
        }
        return null;
    }
    /**
     * 停止当前audioPad的处理;
     */
    public void stop() {
        if (audioPad != null) {
            audioPad.stop();
            LanSongFileUtil.deleteFile(audioPadSavePath);
        }
    }

    /**
     * 释放AudioPad容器;
     */
    public void release() {
        if (audioPad != null) {
            audioPad.release();
            audioPad = null;
        }
        if(mediaInfo!=null){
            mediaInfo.release();
            mediaInfo=null;
        }
    }
    private String fileCompleted()
    {
        for (String item : deleteArray){
            LanSongFileUtil.deleteFile(item);
        }

        if(mediaInfo!=null){
            if(mediaInfo.isHaveVideo()){
                return mergeAudioVideo(inputPath, audioPadSavePath,true);
            }else{
                return audioPadSavePath;
            }
        }else{
            return audioPadSavePath;
        }
    }
    private  String mergeAudioVideo(String video, String audio,boolean deleteAudio) {
        String retPath=LanSongFileUtil.createMp4FileInBox();
        List<String> cmdList = new ArrayList<String>();

        cmdList.add("-i");
        cmdList.add(audio);
        cmdList.add("-i");
        cmdList.add(video);

        cmdList.add("-map");
        cmdList.add("0:a");
        cmdList.add("-map");
        cmdList.add("1:v");

        cmdList.add("-acodec");
        cmdList.add("copy");
        cmdList.add("-vcodec");
        cmdList.add("copy");

        cmdList.add("-absf");
        cmdList.add("aac_adtstoasc");

        cmdList.add("-y");
        cmdList.add(retPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        VideoEditor editor = new VideoEditor();
        int ret = editor.executeVideoEditor(command);
        if (ret == 0) {
            if(deleteAudio){
                LanSongFileUtil.deleteFile(audio);
            }
            return retPath;
        } else {
            return video;
        }
    }


    // ----------------------------一下为测试代码-------------------------------------------

//          AudioPadExecute execute = new AudioPadExecute(getApplicationContext(), "/sdcard/d1.mp4");
//        execute.setOnAudioPadCompletedListener(new AudioPadExecute.onAudioPadExecuteCompletedListener() {
//            @Override
//            public void onCompleted(String path) {
//                MediaInfo.checkFile(path);
//            }
//        });
//        //增加一个音频
//        AudioLayer source = execute.addAudioLayer("/sdcard/huo48000.wav", 0, 110 * 1000 * 1000, -1);
//        if (source != null) {
//            source.setVolume(0.2f);
//        }
//        //增加另一个音频
//        execute.addAudioLayer("/sdcard/Rear_Right.wav", true);
//
//        //主音频禁止;
//        AudioLayer audioSource = execute.getMainAudioLayer();
//        if (audioSource != null) {
//            audioSource.setMute(true);
//        }
//        execute.start();
}
