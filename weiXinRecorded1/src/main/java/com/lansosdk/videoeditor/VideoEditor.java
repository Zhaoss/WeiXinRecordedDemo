package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.lansosdk.box.LSLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.lansosdk.videoeditor.LanSongFileUtil.fileExist;

/**
 * 最简单的调用方法:
 *  //step1. 放在主线程中执行;
 * VideoEditor veditor=new VideoEditor();
 * veditor.setOnProgessListener(xxx)进度;
 *
 * //step2.
 * 然后在AsyncTask或Thread中执行如下;
 * veditor.executeXXXXX();
 *
 *
 * 杭州蓝松科技有限公司
 * www.lansongtech.com
 */
public class VideoEditor {

    private static final String TAG = LSLog.TAG;

    public static final String version="VideoEditor_20100101";
    /**
     * 使用软件编码的列表;
     */
    public static String[] qilinCpulist ={
            "EML-AL00",
            "EML-AL01",
            "LON-AL00",
            "MHA-AL00",
            "STF-AL00",
            "CLT-AL00",
            "ALP-AL00",
            "COL-AL10",
            "FRD-AL00",
            "EVR-AL00",
            "LYA-AL00",
            "PAR-AL00",
            "LON-AL00-PD",
            "VKY-AL00",
            "EDI-AL10",
            "DLI-AL10",
            "PRA-AL00X",
            "DUK-AL20",
            "DUK-AL20",
            "DUK-AL20",
            "WAS-AL00",
            "HUAWEI NXT-CL00",
            "RVL-AL09",
            "COR-AL00",
            "PAR-AL00",
            "INE-AL00",


            "EML-TL00",
            "EML-TL01",
            "LON-TL00",
            "MHA-TL00",
            "STF-TL00",
            "CLT-TL00",
            "ALP-TL00",
            "COL-TL10",
            "FRD-TL00",
            "EVR-TL00",
            "LYA-TL00"
    };

    public static String[] useSoftDecoderlist ={
            "SM919",
            "SM901"
    };
    /**
     * 是否强制使用硬件编码器;
     *
     * 全局变量
     *
     * 默认先硬件编码,如果无法完成则切换为软编码
     */
    public static boolean  isForceHWEncoder=false;
    /**
     * 强制使用软件编码器
     *
     * 全局变量
     * 默认先硬件编码,如果无法完成则切换为软编码
     */
    public static boolean  isForceSoftWareEncoder=false;


    /**
     * 强制使用软解码器
     *
     * 全局变量
     */
    public static boolean  isForceSoftWareDecoder=false;


    /**
     * 不检查是否是16的倍数.
     *
     */
    private static boolean noCheck16Multi=false;

    /**
     * 给当前方法指定码率.
     * 此静态变量, 在execute执行后, 默认恢复为0;
     */
    public int encodeBitRate=0;
    /**
     * 解析参数失败 返回1
     无输出文件 2；
     输入文件为空：3
     sdk未授权 -1；
     解码器错误：69
     收到线程的中断信号：255
     如硬件编码器错误，则返回：26625---26630
     */
    public static final int VIDEO_EDITOR_EXECUTE_SUCCESS1 = 0;
    public static final int VIDEO_EDITOR_EXECUTE_SUCCESS2 = 1;
    public static final int VIDEO_EDITOR_EXECUTE_FAILED = -101;  //文件不存在。


    private final int VIDEOEDITOR_HANDLER_PROGRESS = 203;
    private final int VIDEOEDITOR_HANDLER_COMPLETED = 204;
    private final int VIDEOEDITOR_HANDLER_ENCODERCHANGE = 205;


    private static LanSongLogCollector lanSongLogCollector =null;


    public void setEncodeBitRate(int bitRate){
        encodeBitRate=bitRate;
    }
    /**
     * 使能在ffmpeg执行的时候, 收集错误信息;
     *
     * @param ctx
     */
    public static void logEnable(Context ctx){

        if(ctx!=null){
            lanSongLogCollector =new LanSongLogCollector(ctx);
        }else{
            if(lanSongLogCollector !=null && lanSongLogCollector.isRunning()){
                lanSongLogCollector.stop();
                lanSongLogCollector =null;
            }
        }
    }
    /**
     * 当执行失败后,返回错误信息;
     * @return
     */
    public static String getErrorLog(){
        if(lanSongLogCollector !=null && lanSongLogCollector.isRunning()){
            return lanSongLogCollector.stop();
        }else{
            return null;
        }
    }
    /**
     * 构造方法.
     * 如果您想扩展ffmpeg的命令, 可以继承这个类,
     * 在其中像我们的各种executeXXX的举例一样来拼接ffmpeg的命令;
     *
     * 不要直接修改我们的这个文件, 以方便以后的sdk更新升级.
     */

    public VideoEditor() {
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
            Log.w(TAG, "cannot get Looper handler. may be cannot receive video editor progress!!");
        }
    }
    public onVideoEditorEncodeChangedListener mEncoderChangeListener=null;
    public void setOnEncodeChangedListener(onVideoEditorEncodeChangedListener listener) {
        mEncoderChangeListener = listener;
    }

    private void doEncoderChangedListener(boolean isSoft) {
        if (mEncoderChangeListener != null)
            mEncoderChangeListener.onChanged(this,isSoft);
    }



    public onVideoEditorProgressListener mProgressListener = null;

    public void setOnProgessListener(onVideoEditorProgressListener listener) {
        mProgressListener = listener;
    }

    private void doOnProgressListener(int timeMS) {
        if (mProgressListener != null)
            mProgressListener.onProgress(this, timeMS);
    }

    private EventHandler mEventHandler;

    private class EventHandler extends Handler {
        private final WeakReference<VideoEditor> mWeakExtract;

        public EventHandler(VideoEditor mp, Looper looper) {
            super(looper);
            mWeakExtract = new WeakReference<VideoEditor>(mp);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoEditor videoextract = mWeakExtract.get();
            if (videoextract == null) {
                Log.e(TAG, "VideoEditor went away with unhandled events");
                return;
            }
            switch (msg.what) {
                case VIDEOEDITOR_HANDLER_PROGRESS:
                    videoextract.doOnProgressListener(msg.arg1);
                    break;
                case VIDEOEDITOR_HANDLER_ENCODERCHANGE:
                    videoextract.doEncoderChangedListener(true);  //暂停只要改变,就变成软编码;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 异步线程执行的代码.
     */
    public int executeVideoEditor(String[] array) {
        return execute(array);
    }

    @SuppressWarnings("unused") /* Used from JNI */
    private void postEventFromNative(int what, int arg1, int arg2) {
        if (mEventHandler != null) {
            Message msg = mEventHandler.obtainMessage(VIDEOEDITOR_HANDLER_PROGRESS);
            msg.arg1 = what;
            mEventHandler.sendMessage(msg);
        }
    }
    protected void sendEncoderEnchange()
    {
        if (mEventHandler != null) {
            Message msg = mEventHandler.obtainMessage(VIDEOEDITOR_HANDLER_ENCODERCHANGE);
            mEventHandler.sendMessage(msg);
        }
    }

    public static native int getLimitYear();

    public static native int getLimitMonth();
    /**
     * 获取当前版本号
     * @return
     */
    public static native String getSDKVersion();


    /**
     * 执行成功,返回0, 失败返回错误码.
     *
     * 解析参数失败 返回1
     sdk未授权 -1；
     解码器错误：69
     收到线程的中断信号：255
     如硬件编码器错误，则返回：26625---26630

     * @param cmdArray ffmpeg命令的字符串数组, 可参考此文件中的各种方法举例来编写.
     * @return 执行成功, 返回0, 失败返回错误码.
     */
    private native int execute(Object cmdArray);
    private native int setForceColorFormat(int format);

    /**
     * 新增 在执行过程中取消的方法.
     * 如果在执行中调用了这个方法, 则会直接终止当前的操作.
     * 此方法仅仅是在ffmpeg线程中设置一个标志位,当前这一帧处理完毕后, 会检测到这个标志位,从而退出.
     * 因为execute是阻塞执行, 你可以判断execute有没有执行完,来判断是否完成.
     */
    public native void cancel();

    /**
     * @param filelength
     * @param channel
     * @param sampleRate
     * @param bitperSample
     * @param outData
     */
    public static native void createWavHeader(int filelength, int channel, int sampleRate, int bitperSample, byte[] outData);

    //-----------------
    public static String mergeAVDirectly(String audio, String video,boolean deleteVideo) {
        MediaInfo info=new MediaInfo(audio);
        if(info.prepare() && info.isHaveAudio()){
            String retPath= LanSongFileUtil.createMp4FileInBox();

            String inputAudio = audio;
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(inputAudio);
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
            if(ret==0){
                if(deleteVideo){
                    LanSongFileUtil.deleteFile(video);
                }
                return retPath;
            }else{
                return video;
            }
        }
        return video;
    }
    /**
     * 把一张图片变成视频
     *
     * @param srcPath
     * @param duration  形成视频的总时长;
     * @return  返回处理后的视频;
     */
    public String executePicture2Video(String srcPath, float duration) {
        if (fileExist(srcPath)) {
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-loop");
            cmdList.add("1");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-t");
            cmdList.add(String.valueOf(duration));

            return executeAutoSwitch(cmdList);
        }
        return null;
    }

    @Deprecated
    public String executePcmMix(String srcPach1, int samplerate, int channel, String srcPach2, int samplerate2, int
            channel2,float value1, float value2) {
        List<String> cmdList = new ArrayList<String>();

        String filter = String.format(Locale.getDefault(), "[0:a]volume=volume=%f[a1]; [1:a]volume=volume=%f[a2]; " +
                "[a1][a2]amix=inputs=2:duration=first:dropout_transition=2", value1, value2);

        String  dstPath= LanSongFileUtil.createFileInBox("pcm");

        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-ar");
        cmdList.add(String.valueOf(samplerate));
        cmdList.add("-ac");
        cmdList.add(String.valueOf(channel));
        cmdList.add("-i");
        cmdList.add(srcPach1);

        cmdList.add("-f");
        ;
        cmdList.add("s16le");
        cmdList.add("-ar");
        cmdList.add(String.valueOf(samplerate2));
        cmdList.add("-ac");
        cmdList.add(String.valueOf(channel2));
        cmdList.add("-i");
        cmdList.add(srcPach2);

        cmdList.add("-y");
        cmdList.add("-filter_complex");
        cmdList.add(filter);
        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-acodec");
        cmdList.add("pcm_s16le");
        cmdList.add(dstPath);


        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int  ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }


    @Deprecated
    public String executePcmEncodeAac(String srcPach, int samplerate, int channel) {
        List<String> cmdList = new ArrayList<String>();

        String dstPath= LanSongFileUtil.createM4AFileInBox();
        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-ar");
        cmdList.add(String.valueOf(samplerate));
        cmdList.add("-ac");
        cmdList.add(String.valueOf(channel));
        cmdList.add("-i");
        cmdList.add(srcPach);


        cmdList.add("-acodec");
        cmdList.add("libfaac");
        cmdList.add("-b:a");
        cmdList.add("64000");
        cmdList.add("-y");

        cmdList.add(dstPath);


        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }

    /**
     * 把 pcm和视频文件合并在一起, pcm数据会编码成aac格式.
     * 注意:需要原视频文件里没有音频部分,
     * 如果有, 则需要先用 {@link #executeGetVideoTrack(String)} 拿到视频轨道, 在输入到这里.
     *
     * @param srcPcm     原pcm音频文件,
     * @param samplerate pcm的采样率
     * @param channel    pcm的通道数
     * @param srcVideo   原视频文件, 没有音频部分
     * @return  输出的视频文件路径, 需后缀是mp4格式.
     */
    public String executePcmComposeVideo(String srcPcm, int samplerate, int channel, String srcVideo) {
        List<String> cmdList = new ArrayList<String>();

        String dstPath= LanSongFileUtil.createMp4FileInBox();
        cmdList.add("-f");
        cmdList.add("s16le");
        cmdList.add("-ar");
        cmdList.add(String.valueOf(samplerate));
        cmdList.add("-ac");
        cmdList.add(String.valueOf(channel));
        cmdList.add("-i");
        cmdList.add(srcPcm);

        cmdList.add("-i");
        cmdList.add(srcVideo);

        cmdList.add("-acodec");
        cmdList.add("libfaac");
        cmdList.add("-b:a");
        cmdList.add("64000");
        cmdList.add("-y");

        cmdList.add("-vcodec");
        cmdList.add("copy");

        cmdList.add(dstPath);


        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }
    /**
     * 两个音频文件延迟混合, 即把第二个音频延迟多长时间后, 与第一个音频混合.
     * 混合后的编码为aac格式的音频文件.
     * 注意,如果两个音频的时长不同, 以第一个音频的音频为准. 如需修改可联系我们或查询ffmpeg命令即可.
     *
     * @param audioPath1
     * @param audioPath2
     * @param leftDelayMS  第二个音频的左声道 相对 于第一个音频的延迟时间
     * @param rightDelayMS 第二个音频的右声道 相对 于第一个音频的延迟时间
     * @return
     */
    public String executeAudioDelayMix(String audioPath1, String audioPath2, int leftDelayMS, int rightDelayMS) {
        List<String> cmdList = new ArrayList<String>();
        String overlayXY = String.format(Locale.getDefault(), "[1:a]adelay=%d|%d[delaya1]; " +
                "[0:a][delaya1]amix=inputs=2:duration=first:dropout_transition=2", leftDelayMS, rightDelayMS);


        String dstPath= LanSongFileUtil.createM4AFileInBox();
        cmdList.add("-i");
        cmdList.add(audioPath1);

        cmdList.add("-i");
        cmdList.add(audioPath2);

        cmdList.add("-filter_complex");
        cmdList.add(overlayXY);

        cmdList.add("-acodec");
        cmdList.add("libfaac");

        cmdList.add("-y");
        cmdList.add(dstPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }

    /**
     * 两个音频文件混合.
     * 混合后的文件压缩格式是aac格式, 故需要您dstPath的后缀是aac或m4a.
     *
     * @param audioPath1 主音频的完整路径
     * @param audioPath2 次音频的完整路径
     * @param value1     主音频的音量, 浮点类型, 大于1.0为放大音量, 小于1.0是减低音量.比如设置0.5则降低一倍.
     * @param value2     次音频的音量, 浮点类型.
     * @return 输出保存的完整路径. m4a格式.
     */
    public String executeAudioVolumeMix(String audioPath1, String audioPath2, float value1, float value2) {
        List<String> cmdList = new ArrayList<String>();

        String filter = String.format(Locale.getDefault(), "[0:a]volume=volume=%f[a1]; [1:a]volume=volume=%f[a2]; " +
                "[a1][a2]amix=inputs=2:duration=first:dropout_transition=2", value1, value2);

        String dstPath= LanSongFileUtil.createM4AFileInBox();
        cmdList.add("-i");
        cmdList.add(audioPath1);

        cmdList.add("-i");
        cmdList.add(audioPath2);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("libfaac");

        cmdList.add("-b:a");
        cmdList.add("128000");

        cmdList.add("-ac");
        cmdList.add("2");

        //LSNEW 增加这行和上面的 码率, 通道数
        cmdList.add("-vn");

        cmdList.add("-y");
        cmdList.add(dstPath);

        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int ret= executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }
    /**
     * 视频转码.
     * 通过调整视频的bitrate来对视频文件大小的压缩,降低视频文件的大小, 注意:压缩可能导致视频画质下降.
     *
     *
     * @param srcPath 源视频
     * @param percent 压缩百分比.值从0--1
     * @return
     */
    public String executeVideoCompress(String srcPath, float percent) {
        if (fileExist(srcPath)) {

            MediaInfo info = new MediaInfo(srcPath);
            if (info.prepare()) {

                setEncodeBitRate((int)(info.vBitRate *percent));


                List<String> cmdList = new ArrayList<String>();
                cmdList.add("-vcodec");
                cmdList.add("lansoh264_dec");

                cmdList.add("-i");
                cmdList.add(srcPath);
                cmdList.add("-acodec");
                cmdList.add("copy");

                return executeAutoSwitch(cmdList);
            }

        }
        return null;
    }

    /**
     * 分离mp4文件中的音频,并返回音频的路径,
     */
    public String executeGetAudioTrack(String srcMp4Path) {
        MediaInfo info = new MediaInfo(srcMp4Path);
        if(info.prepare()){
            String audioPath = null;
            if ("aac".equalsIgnoreCase(info.aCodecName)) {
                audioPath = LanSongFileUtil.createFileInBox(".m4a");
            } else if ("mp3".equalsIgnoreCase(info.aCodecName))
                audioPath = LanSongFileUtil.createFileInBox(".mp3");

            if (audioPath != null) {

                List<String> cmdList = new ArrayList<String>();
                cmdList.add("-i");
                cmdList.add(srcMp4Path);
                cmdList.add("-acodec");
                cmdList.add("copy");
                cmdList.add("-vn");
                cmdList.add("-y");
                cmdList.add(audioPath);
                String[] command = new String[cmdList.size()];
                for (int i = 0; i < cmdList.size(); i++) {
                    command[i] = (String) cmdList.get(i);
                }
                int ret= executeVideoEditor(command);
                if(ret==0){
                    return audioPath;
                }else{
                    LanSongFileUtil.deleteFile(audioPath);
                }
            }
        }
        return null;
    }

    /**
     *
     * 获取视频中的视频轨道.
     * (一个mp4文件, 里面可能有音频和视频, 这个是获取视频轨道, 获取后的视频里面将没有音频部分)
     * @param srcMp4Path
     * @return
     */
    public String executeGetVideoTrack(String srcMp4Path) {
        if(fileExist(srcMp4Path)){
            String videoPath  = LanSongFileUtil.createMp4FileInBox();
            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(srcMp4Path);
            cmdList.add("-vcodec");
            cmdList.add("copy");
            cmdList.add("-an");
            cmdList.add("-y");
            cmdList.add(videoPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return videoPath;
            }else{
                LanSongFileUtil.deleteFile(videoPath);
            }
        }
        Log.e(TAG,"执行获取视频轨道 错误, !!!!");
        return null;
    }

    public String executeVideoMergeAudio(String video, String audio) {

        MediaInfo vInfo=new MediaInfo(video);
        MediaInfo aInfo=new MediaInfo(audio);

        if(vInfo.prepare() && aInfo.prepare() && aInfo.isHaveAudio()){

            String retPath= LanSongFileUtil.createMp4FileInBox();
            boolean isAAC="aac".equals(aInfo.aCodecName);

            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-i");
            cmdList.add(audio);

            cmdList.add("-t");
            cmdList.add(String.valueOf(vInfo.vDuration));

            if(isAAC) {  //删去视频的原音,直接增加音频

                cmdList.add("-map");
                cmdList.add("0:v");

                cmdList.add("-map");
                cmdList.add("1:a");

                cmdList.add("-vcodec");
                cmdList.add("copy");

                cmdList.add("-acodec");
                cmdList.add("copy");

                cmdList.add("-absf");
                cmdList.add("aac_adtstoasc");

            }else { //删去视频的原音,并对音频编码
                cmdList.add("-map");
                cmdList.add("0:v");

                cmdList.add("-map");
                cmdList.add("1:a");

                cmdList.add("-vcodec");
                cmdList.add("copy");

                cmdList.add("-acodec");
                cmdList.add("libfaac");

                cmdList.add("-ac");
                cmdList.add("2");

                cmdList.add("-ar");
                cmdList.add("44100");

                cmdList.add("-b:a");
                cmdList.add("128000");
            }

            cmdList.add("-y");
            cmdList.add(retPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            VideoEditor editor = new VideoEditor();
            int ret = editor.executeVideoEditor(command);
            if(ret==0){
                return retPath;
            }else{
                return video;
            }
        }
        return video;
    }
    /**
     * 建议用  AudioEditor中的executeVideoMergeAudio
     */
    @Deprecated
    public String executeVideoMergeAudio(String video, String audio,float audiostartS) {
        MediaInfo vInfo=new MediaInfo(video);
        MediaInfo aInfo=new MediaInfo(audio);

        if(vInfo.prepare() && aInfo.prepare() && aInfo.isHaveAudio()){

            String retPath= LanSongFileUtil.createMp4FileInBox();
            boolean isAAC="aac".equals(aInfo.aCodecName);

            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(audiostartS));

            cmdList.add("-i");
            cmdList.add(audio);

            cmdList.add("-t");
            cmdList.add(String.valueOf(vInfo.vDuration));

            if(isAAC) {  //删去视频的原音,直接增加音频
                cmdList.add("-map");
                cmdList.add("0:v");

                cmdList.add("-map");
                cmdList.add("1:a");

                cmdList.add("-vcodec");
                cmdList.add("copy");

                cmdList.add("-acodec");
                cmdList.add("copy");

                cmdList.add("-absf");
                cmdList.add("aac_adtstoasc");

            }else { //删去视频的原音,并对音频编码
                cmdList.add("-map");
                cmdList.add("0:v");

                cmdList.add("-map");
                cmdList.add("1:a");

                cmdList.add("-vcodec");
                cmdList.add("copy");

                cmdList.add("-acodec");
                cmdList.add("libfaac");

                cmdList.add("-ac");
                cmdList.add("2");

                cmdList.add("-ar");
                cmdList.add("44100");

                cmdList.add("-b:a");
                cmdList.add("128000");
            }

            cmdList.add("-y");
            cmdList.add(retPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            VideoEditor editor = new VideoEditor();
            int ret = editor.executeVideoEditor(command);
            if(ret==0){
                return retPath;
            }else{
                return video;
            }
        }
        return video;
    }


    /**
     * 用AudioEditor中的方法
     */
    @Deprecated
    public String executeCutAudio(String srcFile, float startS, float durationS) {
        if (fileExist(srcFile)) {

            List<String> cmdList = new ArrayList<String>();

            String dstFile= LanSongFileUtil.createFileInBox(LanSongFileUtil.getFileSuffix(srcFile));
            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-i");
            cmdList.add(srcFile);

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-acodec");
            cmdList.add("copy");
            cmdList.add("-y");
            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstFile;
            }else{
                LanSongFileUtil.deleteFile(dstFile);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 剪切mp4文件.
     * (包括视频文件中的音频部分和视频部分),即把mp4文件中的一段剪切成独立的一个视频文件, 比如把一个1分钟的视频,裁剪其中的10秒钟等.
     * <p>
     * 注意: 此方法裁剪不是精确裁剪,而是从视频的IDR帧开始裁剪的, 没有精确到您指定的那一帧的时间, 如果您指定的时间不是IDR帧上的时间,则退后到上一个IDR帧开始.
     *
     * @param videoFile 原视频文件 文件格式是mp4
     * @param startS    开始裁剪位置，单位是秒，
     * @param durationS 需要裁剪的时长，单位秒，比如您可以从原视频的8.9秒出开始裁剪，裁剪2分钟，则这里的参数是120
     * @return
     */
    public String executeCutVideo(String videoFile, float startS, float durationS) {
        if (fileExist(videoFile)) {

            String dstFile= LanSongFileUtil.createMp4FileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-vcodec");
            cmdList.add("copy");
            cmdList.add("-acodec");
            cmdList.add("copy");
            cmdList.add("-y");
            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstFile;
            }else{
                LanSongFileUtil.deleteFile(dstFile);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 精确裁剪
     * @param videoFile 输入源视频的完整路径
     * @param startS    开始裁剪时间点, 单位秒
     * @param durationS 要裁剪的总长度,单位秒
     * @return 裁剪后返回的目标视频
     */
    public String executeCutVideoExact(String videoFile, float startS, float durationS) {
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    /**
     * 精确裁剪的同时,缩放到指定位置,不同于上面的命令,这个可以设置宽度和高度. 其中宽度和高度是采用缩放来完成.
     * <p>
     * <p>
     * 采用的是软缩放的形式.
     *
     * @param videoFile
     * @param startS
     * @param durationS
     * @param width     要缩放到的宽度 建议是16的倍数 ,
     * @param height    要缩放到的高度, 建议是16的倍数
     * @return
     */
    public String executeCutVideoExact(String videoFile, float startS, float durationS, int width, int height) {
        if (fileExist(videoFile)) {
            List<String> cmdList = new ArrayList<String>();

            String scalecmd = String.format(Locale.getDefault(), "scale=%d:%d", width, height);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-vf");
            cmdList.add(scalecmd);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }
    /**
     * 获取视频的所有帧图片,并保存到指定路径.
     * 所有的帧会按照后缀名字加上_001.jpeg prefix_002.jpeg的顺序依次生成, 如果发现之前已经有同样格式的文件,则在原来数字后缀的基础上增加, 比如原来有prefix_516.jpeg;则这个方法执行从
     * prefix_517.jpeg开始生成视频帧.
     * <p>
     * <p>
     * 如果您使用的是专业版,则建议用ExtractVideoFrameDemoActivity来获取视频图片,
     * 因为直接返回bitmap,不存到文件中,速度相对快很多
     * <p>
     * 这条命令是把视频中的所有帧都提取成图片，适用于视频比较短的场合，比如一秒钟是25帧，视频总时长是10秒，则会提取250帧图片，保存到您指定的路径
     *
     * @param videoFile
     * @param dstDir    目标文件夹绝对路径.
     * @param jpgPrefix 保存图片文件的前缀，可以是png或jpg
     * @return
     */
    public int executeGetAllFrames(String videoFile, String dstDir, String jpgPrefix) {
        String dstPath = dstDir + jpgPrefix + "_%3d.jpeg";
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-qscale:v");
            cmdList.add("2");

            cmdList.add(dstPath);

            cmdList.add("-y");

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            return executeVideoEditor(command);

        } else {
            return VIDEO_EDITOR_EXECUTE_FAILED;
        }
    }

    /**
     * 根据设定的采样,获取视频的几行图片.
     * 假如视频时长是30秒,想平均取5张图片,则sampleRate=5/30;
     * <p>
     * <p>
     * 如果您使用的是专业版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
     *
     * @param videoFile
     * @param dstDir
     * @param jpgPrefix
     * @param sampeRate 一秒钟采样几张图片. 可以是小数.
     * @return
     */
    public int executeGetSomeFrames(String videoFile, String dstDir, String jpgPrefix, float sampeRate) {
        String dstPath = dstDir + jpgPrefix + "_%3d.jpeg";
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

//					cmdList.add("-qscale:v");
//					cmdList.add("2");

            cmdList.add("-vsync");
            cmdList.add("1");

            cmdList.add("-r");
            cmdList.add(String.valueOf(sampeRate));

//					cmdList.add("-f");
//					cmdList.add("image2");

            cmdList.add("-y");

            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            return executeVideoEditor(command);

        } else {
            return VIDEO_EDITOR_EXECUTE_FAILED;
        }
    }

    /**
     * 读取视频中的关键帧(IDR帧), 并把关键帧保存图片. 因是IDR帧, 在编码时没有起帧做参考,故提取的最快.
     * <p>
     * <p>
     * 如果您使用的是专业版本,则建议用ExtractVideoFrameDemoActivity来获取视频图片,因为直接返回bitmap,不存到文件中,速度相对快很多
     * <p>
     * <p>
     * 经过我们SDK编码后的视频, 是一秒钟一个帧,如果您视频大小是30秒,则大约会提取30张图片.
     *
     * @param videoFile 视频文件
     * @param dstDir    保持的文件夹
     * @param jpgPrefix 文件前缀.
     * @return
     */
    public int executeGetKeyFrames(String videoFile, String dstDir, String jpgPrefix) {
        String dstPath = dstDir + "/" + jpgPrefix + "_%3d.png";
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-vf");
            cmdList.add("select=eq(pict_type\\,I)");

            cmdList.add("-vsync");
            cmdList.add("vfr");

            Log.i(TAG, " vsync is vfr");

            cmdList.add("-y");

            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            return executeVideoEditor(command);
        } else {
            return VIDEO_EDITOR_EXECUTE_FAILED;
        }
    }

    /**
     * 来自于网络, 没有全部测试.
     * 获取视频的缩略图
     * 提供了一个统一的接口用于从一个输入媒体文件中取得帧和元数据。
     *
     * @param path   视频的路径
     * @param width  缩略图的宽
     * @param height 缩略图的高
     * @return 缩略图
     */
    public static Bitmap createVideoThumbnail(String path, int width, int height) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        try {
            retriever.setDataSource(path);
            bitmap = retriever.getFrameAtTime(-1); //取得指定时间的Bitmap，即可以实现抓图（缩略图）功能
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }

        if (bitmap == null) {
            return null;
        }

        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        return bitmap;
    }

    /**
     *
     * 从视频的指定位置中获取一帧图片. 因为这个是精确提取视频的一帧,
     * 不建议作为提取缩略图来使用,用mediametadataRetriever最好.
     *
     * @param videoSrcPath 源视频的完整路径
     * @param postionS     时间点，单位秒，类型float，可以有小数，比如从视频的2.35秒的地方获取一张图片。
     * @return
     */
    public String executeGetOneFrame(String videoSrcPath,float postionS) {
        if (fileExist(videoSrcPath)) {

            List<String> cmdList = new ArrayList<String>();
//
            String dstPng= LanSongFileUtil.createFileInBox("png");
            cmdList.add("-i");
            cmdList.add(videoSrcPath);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(postionS));

            cmdList.add("-vframes");
            cmdList.add("1");

            cmdList.add("-y");

            cmdList.add(dstPng);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstPng;
            }else{
                LanSongFileUtil.deleteFile(dstPng);
                return null;
            }
        } else {
            return null;
        }
    }
    /**
     * 从视频的指定位置中获取一帧图片,得到图片后,把图片缩放到指定的宽高. 因为这个是精确提取视频的一帧, 不建议作为提取缩略图来使用.
     *
     * @param videoSrcPath 源视频的完整路径
     * @param postionS     时间点，单位秒，类型float，可以有小数，比如从视频的2.35秒的地方获取一张图片。
     * @param pngWidth     得到目标图片后缩放的宽度.
     * @param pngHeight    得到目标图片后需要缩放的高度.
     * @return 得到目标图片的完整路径名.
     */
    public String executeGetOneFrame(String videoSrcPath, float postionS, int pngWidth, int pngHeight) {
        if (fileExist(videoSrcPath)) {

            List<String> cmdList = new ArrayList<String>();

            String dstPng= LanSongFileUtil.createFileInBox("png");

            String resolution = String.valueOf(pngWidth);
            resolution += "x";
            resolution += String.valueOf(pngHeight);

            cmdList.add("-i");
            cmdList.add(videoSrcPath);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(postionS));

            cmdList.add("-s");
            cmdList.add(resolution);

            cmdList.add("-vframes");
            cmdList.add("1");

            cmdList.add("-y");

            cmdList.add(dstPng);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstPng;
            }else {
                LanSongFileUtil.deleteFile(dstPng);
                return null;
            }
        }
        return null;
    }


    /**
     * 请用VideoEditor中的方法;
     */
    @Deprecated
    public String executeConvertMp3ToAAC(String mp3Path) {
        if (fileExist(mp3Path)) {

            String dstFile= LanSongFileUtil.createFileInBox("m4a");

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(mp3Path);

            cmdList.add("-acodec");
            cmdList.add("libfaac");

            cmdList.add("-y");
            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstFile;
            }else{
                LanSongFileUtil.deleteFile(dstFile);
                return null;
            }
        } else {
            return null;
        }
    }
    public String executeConvertMp3ToAAC(String mp3Path,float startS,float durationS) {
        if (fileExist(mp3Path)) {

            List<String> cmdList = new ArrayList<String>();

            String  dstPath= LanSongFileUtil.createM4AFileInBox();
            cmdList.add("-i");
            cmdList.add(mp3Path);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            cmdList.add("-acodec");
            cmdList.add("libfaac");

            cmdList.add("-y");
            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        }
        return null;
    }
    public String executeConvertMp4toTs(String mp4Path) {
        if (fileExist(mp4Path)) {

            List<String> cmdList = new ArrayList<String>();

            String dstTs = LanSongFileUtil.createFileInBox("ts");
            cmdList.add("-i");
            cmdList.add(mp4Path);

            cmdList.add("-c");
            cmdList.add("copy");

            cmdList.add("-bsf:v");
            cmdList.add("h264_mp4toannexb");

            cmdList.add("-f");
            cmdList.add("mpegts");

            cmdList.add("-y");
            cmdList.add(dstTs);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret = executeVideoEditor(command);
            if (ret == 0) {
                return dstTs;
            } else {
                LanSongFileUtil.deleteFile(dstTs);
                return null;
            }
        }
        return null;
    }
    public String executeConvertTsToMp4(String[] tsArray) {
        if (LanSongFileUtil.filesExist(tsArray)) {

            String dstFile= LanSongFileUtil.createMp4FileInBox();
            String concat = "concat:";
            for (int i = 0; i < tsArray.length - 1; i++) {
                concat += tsArray[i];
                concat += "|";
            }
            concat += tsArray[tsArray.length - 1];

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(concat);

            cmdList.add("-c");
            cmdList.add("copy");

            cmdList.add("-bsf:a");
            cmdList.add("aac_adtstoasc");

            cmdList.add("-y");

            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstFile;
            }else{
                LanSongFileUtil.deleteFile(dstFile);
                return null;
            }
        } else {
            return null;
        }
    }
    /**
     * 把分段录制的视频, 拼接在一起;
     *
     * 注意:此方法仅仅使用在分段录制的场合
     * 注意:此方法仅仅使用在分段录制的场合
     * 注意:此方法仅仅使用在分段录制的场合
     * @param mp4Array
     */
    public String  executeConcatMP4(String[] mp4Array) {

        //第一步,先把所有的mp4转换为ts流
        ArrayList<String> tsPathArray = new ArrayList<String>();
        for (int i = 0; i < mp4Array.length; i++) {
            String segTs1 = executeConvertMp4toTs(mp4Array[i]);
            tsPathArray.add(segTs1);
        }

        //第二步: 把ts流拼接成mp4
        String[] tsPaths = new String[tsPathArray.size()];
        for (int i = 0; i < tsPathArray.size(); i++) {
            tsPaths[i] = (String) tsPathArray.get(i);
        }
        String dstVideo=executeConvertTsToMp4(tsPaths);


        //第三步:删除临时生成的ts文件.
        for (int i = 0; i < tsPathArray.size(); i++) {
            LanSongFileUtil.deleteFile(tsPathArray.get(i));
        }
        return dstVideo;
    }
    public String  executeConcatMP4(List<String> mp4Array) {

        //第一步,先把所有的mp4转换为ts流
        ArrayList<String> tsPathArray = new ArrayList<String>();
        for (int i = 0; i < mp4Array.size(); i++) {
            String segTs1 = executeConvertMp4toTs(mp4Array.get(i));
            tsPathArray.add(segTs1);
        }

        //第二步: 把ts流拼接成mp4
        String[] tsPaths = new String[tsPathArray.size()];
        for (int i = 0; i < tsPathArray.size(); i++) {
            tsPaths[i] = (String) tsPathArray.get(i);
        }
        String dstVideo=executeConvertTsToMp4(tsPaths);


        //第三步:删除临时生成的ts文件.
        for (int i = 0; i < tsPathArray.size(); i++) {
            LanSongFileUtil.deleteFile(tsPathArray.get(i));
        }
        return dstVideo;
    }
    /**
     * 不同来源的mp4文件进行拼接;
     * 拼接的所有视频分辨率必须一致;
     *
     *
     * @param videos  所有的视频
     * @param ignorecheck  是否要忽略检测每个每个视频的分辨率, 如果您确信已经相等,则设为false;
     * @return  输出视频的路径
     */
    public String executeConcatDiffentMp4(ArrayList<String> videos,boolean ignorecheck) {
        if(videos!=null && videos.size()>1){
            if(ignorecheck || checkVideoSizeSame(videos)){
                String dstPath= LanSongFileUtil.createMp4FileInBox();

                String filter = String.format(Locale.getDefault(), "concat=n=%d:v=1:a=1", videos.size());

                List<String> cmdList = new ArrayList<String>();
                cmdList.add("-vcodec");
                cmdList.add("lansoh264_dec");

                cmdList.add("-i");
                cmdList.add(videos.get(0));

                for (int i=1;i<videos.size();i++){
                    cmdList.add("-i");
                    cmdList.add(videos.get(i));
                }
                cmdList.add("-filter_complex");
                cmdList.add(filter);

                cmdList.add("-acodec");
                cmdList.add("libfaac");
                cmdList.add("-b:a");
                cmdList.add("128000");


                return executeAutoSwitch(cmdList);
            }
        }
        return null;
    }

    private boolean checkVideoSizeSame(ArrayList<String> videos){
        int w=0;
        int h=0;
        for (String item: videos){
            MediaInfo info=new MediaInfo(item);
            if(info.prepare()){

                if(w ==0&& h==0){
                    w=info.getWidth();
                    h=info.getHeight();
                }else if(info.getWidth()!=w || info.getHeight() !=h){
                    Log.e(TAG,"视频拼接中, 有个视频的分辨率不等于其他分辨率");
                    return false;
                }
            }else{
                return false;
            }
        }
        return true;  //返回正常;
    }


    /**
     * 裁剪视频画面
     *
     * @param videoFile  　需要裁剪的视频文件
     * @param cropWidth  　裁剪后的目标宽度
     * @param cropHeight 　裁剪后的目标高度
     * @param x          　视频画面开始的Ｘ坐标，　从画面的左上角开始是0.0坐标
     * @param y          视频画面开始的Y坐标，
     * @return  处理后保存的路径,后缀需要是mp4
     */
    public String executeCropVideoFrame(String videoFile, int cropWidth, int cropHeight, int x, int y) {
        if (fileExist(videoFile)) {
            String filter = String.format(Locale.getDefault(), "crop=%d:%d:%d:%d", cropWidth, cropHeight, x, y);
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        }
        return null;
    }

    /**
     *  缩放视频画面
     *
     * @param videoFile
     * @param scaleWidth
     * @param scaleHeight
     * @return
     */
    public String executeScaleVideoFrame(String videoFile, int scaleWidth, int scaleHeight) {
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();
            scaleWidth=(scaleWidth/2)*2;
            scaleHeight=(scaleHeight/2)*2;

            String scalecmd = String.format(Locale.getDefault(), "scale=%d:%d", scaleWidth, scaleHeight);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-vf");
            cmdList.add(scalecmd);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        }
        return null;
    }

    /**
     * 缩放的同时增加logo水印.
     *
     * @param videoFile 原视频路径
     * @param pngPath  增加图片路径
     * @param scaleWidth  要缩放到的宽度
     * @param scaleHeight 要缩放到的高度
     * @param overX  图片的左上角 放到视频的 X坐标
     * @param overY  图片的左上角 放到视频的 坐标
     * @return
     */
    public String executeScaleOverlay(String videoFile, String pngPath, int scaleWidth, int scaleHeight, int overX,
                                      int overY) {
        if (fileExist(videoFile)) {

            List<String> cmdList = new ArrayList<String>();
            String filter = String.format(Locale.getDefault(), "[0:v]scale=%d:%d [scale];[scale][1:v] overlay=%d:%d",
                    scaleWidth, scaleHeight, overX, overY);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-i");
            cmdList.add(pngPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 给视频增加图片
     *
     * @param videoFile  原视频完整路径
     * @param picturePath  图片完整路径
     * @param overX 图片的左上角 放到视频的X坐标
     * @param overY  图片的左上角 放到视频的X坐标
     * @return  返回目标文件
     */
    public String  executeOverLayVideoFrame(String videoFile, String picturePath, int overX, int overY)
    {
        String filter = String.format(Locale.getDefault(), "overlay=%d:%d", overX, overY);

        List<String> cmdList = new ArrayList<String>();

        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-i");
        cmdList.add(videoFile);

        cmdList.add("-i");
        cmdList.add(picturePath);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");

        return executeAutoSwitch(cmdList);

    }

    /**
     * 对视频画面进行裁剪,裁剪后叠加一个png类型的图片,
     * <p>
     * 等于把裁剪,叠加水印,压缩三条命令放在一次执行, 这样只解码一次,和只编码一次,极大的加快了处理速度.
     *
     * @param videoFile  原视频
     * @param pngPath
     * @param cropX      画面裁剪的X坐标, 左上角为0:0
     * @param cropY      画面裁剪的Y坐标
     * @param cropWidth  画面裁剪宽度. 须小于等于源视频宽度
     * @param cropHeight 画面裁剪高度, 须小于等于源视频高度
     * @param overX      画面和png图片开始叠加的X坐标.
     * @param overY      画面和png图片开始叠加的Y坐标
     * @return
     */
    public String  executeCropOverlay(String videoFile, String pngPath, int cropX, int cropY, int
            cropWidth, int cropHeight, int overX, int overY)
    {
        if (fileExist(videoFile)) {
            String filter = String.format(Locale.getDefault(), "[0:v]crop=%d:%d:%d:%d [crop];[crop][1:v] " +
                    "overlay=%d:%d", cropWidth, cropHeight, cropX, cropY, overX, overY);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-i");
            cmdList.add(pngPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 画面裁剪.
     *
     * @param videoFile 输入文件完整路径
     * @param cropX  画面裁剪的开始X坐标
     * @param cropY  画面裁剪的开始Y坐标
     * @param cropWidth  画面裁剪的宽度
     * @param cropHeight  画面裁剪的高度.
     * @return
     */
    public String executeCrop(String videoFile, int cropX, int cropY, int cropWidth, int cropHeight) {

        String filter = String.format(Locale.getDefault(), "crop=%d:%d:%d:%d", cropWidth, cropHeight, cropX, cropY);

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-i");
        cmdList.add(videoFile);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");

        return executeAutoSwitch(cmdList);
    }

    /**
     * 时长剪切的同时, 做画面裁剪.
     *
     * @param videoFile 输入文件完整路径
     * @param startTimeS  开始时间,单位秒
     * @param duationS  要剪切的时长;
     * @param cropX  画面裁剪的开始X坐标
     * @param cropY  画面裁剪的开始Y坐标
     * @param cropWidth  画面裁剪的宽度
     * @param cropHeight  画面裁剪的高度.
     * @return
     */
    public String executeCutCrop(String videoFile, float startTimeS, float
            duationS, int cropX, int cropY, int cropWidth, int cropHeight) {

        String filter = String.format(Locale.getDefault(), "crop=%d:%d:%d:%d", cropWidth, cropHeight, cropX, cropY);

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-ss");
        cmdList.add(String.valueOf(startTimeS));

        cmdList.add("-t");
        cmdList.add(String.valueOf(duationS));

        cmdList.add("-i");
        cmdList.add(videoFile);


        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");

        return executeAutoSwitch(cmdList);
    }
    /**
     * 同时执行 视频时长剪切, 画面裁剪和增加水印的功能.
     *
     * @param videoFile  源视频文件.
     * @param pngPath    增加的水印文件路径
     * @param startTimeS 时长剪切的开始时间
     * @param duationS   时长剪切的 总长度
     * @param cropX      画面裁剪的 X坐标,(最左边坐标是0)
     * @param cropY      画面裁剪的Y坐标,(最上面坐标是0)
     * @param cropWidth  画面裁剪宽度
     * @param cropHeight 画面裁剪高度
     * @param overX      增加水印的X坐标
     * @param overY      增加水印的Y坐标
     * @return
     */
    public String executeCutCropOverlay(String videoFile, String pngPath, float startTimeS, float
            duationS, int cropX, int cropY, int cropWidth, int cropHeight, int overX, int overY) {

        String filter = String.format(Locale.getDefault(), "[0:v]crop=%d:%d:%d:%d [crop];[crop][1:v] " +
                "overlay=%d:%d", cropWidth, cropHeight, cropX, cropY, overX, overY);

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-ss");
        cmdList.add(String.valueOf(startTimeS));

        cmdList.add("-t");
        cmdList.add(String.valueOf(duationS));

        cmdList.add("-i");
        cmdList.add(videoFile);

        cmdList.add("-i");
        cmdList.add(pngPath);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");

        return executeAutoSwitch(cmdList);
    }


    /**
     * 把多张图片转换为视频
     * 注意：　这里的多张图片必须在同一个文件夹下，并且命名需要有规律,比如名字是 r5r_001.jpeg r5r_002.jpeg, r5r_003.jpeg等
     * 多张图片，需要统一的分辨率，如分辨率不同，则以第一张图片的分辨率为准，后面的分辨率自动缩放到第一张图片的分辨率的大小
     *
     * @param picDir    　保存图片的文件夹
     * @param jpgprefix 　图片的文件名有规律的前缀
     * @param framerate 　每秒钟需要显示几张图片
     * @return
     */
    public String executeConvertPictureToVideo(String picDir, String jpgprefix, float framerate) {

        String picSet = picDir + jpgprefix + "_%3d.jpeg";

        List<String> cmdList = new ArrayList<String>();

        cmdList.add("-framerate");
        cmdList.add(String.valueOf(framerate));

        cmdList.add("-i");
        cmdList.add(picSet);

        cmdList.add("-r");
        cmdList.add("25");

        return executeAutoSwitch(cmdList);
    }
    /**
     * 把视频填充成指定大小的画面, 比视频的宽高 大的部分用黑色来填充.
     *
     * @param videoFile 源视频路径
     * @param padWidth  填充成的目标宽度 , 参数需要是16的倍数
     * @param padHeight 填充成的目标高度 , 参数需要是16的倍数
     * @param padX      把视频画面放到填充区时的开始X坐标
     * @param padY      把视频画面放到填充区时的开始Y坐标
     * @return
     */
    public String executePadVideo(String videoFile, int padWidth, int padHeight, int padX, int padY) {
        if (fileExist(videoFile)) {
            MediaInfo info = new MediaInfo(videoFile);
            if (info.prepare()) {
                int minWidth = info.vWidth + padX;
                int minHeight = info.vHeight + padY;
                if (minWidth > padWidth || minHeight > padHeight) {
                    Log.e(TAG, "pad set position is error. min Width>pading width.or min height > padding height");
                    return null;  //失败.
                }
            } else {
                Log.e(TAG, "media info prepare is error!!!");
                return null;
            }

            //第二步: 开始padding.
            String filter = String.format(Locale.getDefault(), "pad=%d:%d:%d:%d:black", padWidth, padHeight, padX,padY);

            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }
    /**
     * 给视频旋转角度,
     *
     * 注意这里 只是 旋转画面的的角度,而不会调整视频的宽高.
     * @param srcPath 　需要旋转角度的原视频
     * @param angle   　　角度
     * @return
     */
    public String executeRotateAngle(String srcPath, float angle) {
        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(), "rotate=%f*(PI/180),format=yuv420p", angle);
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-metadata:s:v");
            cmdList.add("rotate=0");

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 设置多媒体文件中的 视频元数据的角度.
     * 一个多媒体文件中有很多种元数据, 包括音频轨道, 视频轨道, 各种元数据, 字幕,其他文字等信息,
     * 这里仅仅更改元数据中的视频播放角度, 当视频播放器播放该视频时, 会得到"要旋转多少度"播放的信息,
     * 这样在播放时就会旋转后再播放画面
     * <p>
     * 此设置不改变音视频的各种参数, 仅仅是告诉播放器,"要旋转多少度"来播放而已.
     * 适用在拍摄的视频有90度和270的情况, 想更改这个角度参数的场合.
     *
     * @param srcPath 原视频
     * @param angle   需要更改的角度
     * @return
     */
    public String executeSetVideoMetaAngle(String srcPath, int angle) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            String dstPath= LanSongFileUtil.createMp4FileInBox();
            String filter = String.format(Locale.getDefault(), "rotate=%d", angle);


            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-c");
            cmdList.add("copy");

            cmdList.add("-metadata:s:v:0");
            cmdList.add(filter);

            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else {
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        } else {
            return null;
        }
    }
//---------------------------

    /**
     * 叠加并调速;
     * @param srcPath
     * @param pngPath
     * @param overX
     * @param overY
     * @param speed  速度值,范围0--1;
     * @return
     */
    public String executeOverLaySpeed(String srcPath, String pngPath,int overX,int overY, float speed){

        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(),
                    "[0:v][1:v]overlay=%d:%d[overlay];[overlay]setpts=%f*PTS[v];[0:a]atempo=%f[a]",overX,overY, 1 / speed,
                    speed);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-i");
            cmdList.add(pngPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-map");
            cmdList.add("[v]");
            cmdList.add("-map");
            cmdList.add("[a]");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }
    /**
     * 调整视频的播放速度
     * 范围0.5--2.0；
     * 0.5:放慢一倍;2:加快一倍
     * @param srcPath 　　源视频
     * @return
     */
    public String executeAdjustVideoSpeed(String srcPath, float speed){
        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(), "[0:v]setpts=%f*PTS[v];[0:a]atempo=%f[a]", 1 / speed,
                    speed);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-map");
            cmdList.add("[v]");
            cmdList.add("-map");
            cmdList.add("[a]");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 调整视频的播放速度，　
     * 可以把视频加快速度，或放慢速度。适用在希望缩短视频中不重要的部分的场景，比如走路等
     *
     * @param srcPath 　　源视频
     * @param speed   　　　　源视频中　　画面和音频同时改变的倍数，比如放慢一倍，则这里是0.5;加快一倍，这里是2；建议速度在0.5--2.0之间。
     * @return
     */
    public String executeAdjustVideoSpeed2(String srcPath, float speed) {
        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(), "[0:v]setpts=%f*PTS[v]", 1 / speed);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-filter_complex");
            cmdList.add(filter);

            cmdList.add("-map");
            cmdList.add("[v]");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 视频水平镜像，即把视频左半部分镜像显示在右半部分
     * 【此方法用到编解码】
     *
     * @param srcPath 　源视频路径
     * @return
     */
    public String executeVideoMirrorH(String srcPath) {
        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(), "crop=iw/2:ih:0:0,split[left][tmp];[tmp]hflip[right];" +
                    "[left][right] hstack");

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 视频垂直镜像，即把视频上半部分镜像显示在下半部分
     *
     * @param srcPath 　源视频路径
     * @return
     */
    public String executeVideoMirrorV(String srcPath) {
        if (fileExist(srcPath)) {

            String filter = String.format(Locale.getDefault(), "crop=iw:ih/2:0:0,split[top][tmp];[tmp]vflip[bottom];" +
                    "[top][bottom] vstack");

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 视频垂直方向反转
     * @param srcPath 　　原视频
     * @return
     */
    public String executeVideoRotateVertically(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("vflip");

            cmdList.add("-c:a");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 视频水平方向反转
     *
     * @param srcPath 　　原视频
     * @return
     */
    public String executeVideoRotateHorizontally(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("hflip");

            cmdList.add("-c:a");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 视频顺时针旋转90度
     *
     * @param srcPath 原视频
     * @return
     */
    public String  executeVideoRotate90Clockwise(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("transpose=1");

            cmdList.add("-c:a");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 视频逆时针旋转90度,也可以认为是顺时针旋转270度.
     *
     * @param srcPath 　原视频
     * @return
     */
    public String executeVideoRotate90CounterClockwise(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("transpose=2");

            cmdList.add("-c:a");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 视频倒序；比如正常的视频画面是一个人从左边走到右边，倒序后，人从右边倒退到左边，即视频画面发生了倒序
     * <p>
     * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
     * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
     * <p>
     * 如您的视频过大, 则可能导致:Failed to inject frame into filter network: Out of memory;这个是正常的.因为已超过APP可使用的内容范围, 内存不足.
     *
     * @param srcPath 　原视频
     * @return
     */
    public String executeVideoReverse(String srcPath) {
        if (fileExist(srcPath)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("reverse");

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);

        } else {
            return null;
        }
    }

    /**
     * 音频倒序，和视频倒序类似，把原来正常的声音，处理成从后向前的声音。　适合在搞怪的一些场合。
     * <p>
     * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
     * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
     *
     * @param srcPath 源文件完整路径. 原文件可以是mp4的视频, 也可以是mp3或m4a的音频文件
     * @return
     */
    public String executeAudioReverse(String srcPath) {
        if (fileExist(srcPath)) {

            String dstPath= LanSongFileUtil.createM4AFileInBox();
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-af");
            cmdList.add("areverse");

            cmdList.add("-c:v");
            cmdList.add("copy");

            cmdList.add("-y");
            cmdList.add("drawpadDstPath");

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret= executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 把一个mp4文件中的音频部分和视频都倒序播放。
     * <p>
     * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
     * 注意：此处理会占用大量的内存，建议视频最好是480x480的分辨率, 并且不要过长，尽量在15秒内
     * 如您的视频过大, 则可能导致:Failed to inject frame into filter network: Out of memory;这个是正常的.因为已超过APP可使用的内容范围, 内存不足.
     *
     * @param srcPath 　　原mp4文件
     * @return
     */
    public String executeAVReverse(String srcPath) {
        if (fileExist(srcPath)) {
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-vf");
            cmdList.add("reverse");

            cmdList.add("-af");
            cmdList.add("areverse");


            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }


    /**
     * 不建议使用,因为需要字体,比较麻烦, 建议把文字转图片,然后叠加.
     * @param videoPath
     * @param fontPath
     * @param text
     * @return
     */
    public String testVideoAddText(String videoPath,String fontPath,String  text) {
        List<String> cmdList = new ArrayList<String>();

        //"drawtext=fontfile=/system/fonts/DroidSansFallback.ttf: text='杭州蓝松科技001abc'");
        String  filter="drawtext=fontfile="+fontPath+": text= '"+text+ "'";

        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-i");
        cmdList.add(videoPath);

        cmdList.add("-vf");

        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");

        return executeAutoSwitch(cmdList);
    }

    /**
     * 删除视频中的logo,比如一般在视频的左上角或右上角有视频logo信息,类似"优酷","抖音"等;
     * 这里把指定位置的图像删除掉;
     *
     * @param video 原视频
     * @param startX  开始横坐标
     * @param startY  开始的横坐标
     * @param w 删除的宽度
     * @param h 删除的高度
     * @return
     */
    public String executeDeleteLogo(String video,int startX,int startY,int w,int h){
        if (fileExist(video)) {

            String filter = String.format(Locale.getDefault(), "delogo=x=%d:y=%d:w=%d:h=%d",startX,
                    startY,w,h);

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 删除视频中的最多4处有logo的信息.
     *
     *  4处logo, 是从1--4;
     *
     *  如果只用3处logo,则把startX4=0 startY4=0,width4=0,height4=0;
     *  如果只用到2处理,则把 3,4 设置为0;
     *  如果要设置编码后的码率,则用 setEncodeBitRate
     * @param video 输入视频,
     * @param startX1 第一处X坐标开始坐标
     * @param startY1 第一处Y坐标开始坐标
     * @param width1  第一处的宽度
     * @param height1 第一处的高度
     * @param startX2
     * @param startY2
     * @param width2
     * @param height2
     * @param startX3
     * @param startY3
     * @param width3
     * @param height3
     * @param startX4
     * @param startY4
     * @param width4
     * @param height4
     * @return
     */
    public String executeDeleteLogo(String video,int startX1,int startY1,int width1,int height1,
                                    int startX2,int startY2,int width2,int height2,
                                    int startX3,int startY3,int width3,int height3,
                                    int startX4,int startY4,int width4,int height4){
        if (fileExist(video)) {

            String filter = String.format(Locale.getDefault(), "delogo=x=%d:y=%d:w=%d:h=%d [d1]",
                    startX1,startY1,width1,height1);

            if(startX2>=0 && startY2>=0 && width2>0 && height2>0){
                String filter2 = String.format(Locale.getDefault(), ";[d1]delogo=x=%d:y=%d:w=%d:h=%d [d2]",
                        startX2,startY2,width2,height2);
                filter+=filter2;
            }

            if(startX3>=0 && startY3>=0 && width3>0 && height3>0){
                String filter3 = String.format(Locale.getDefault(), ";[d2]delogo=x=%d:y=%d:w=%d:h=%d [d3]",
                        startX3,startY3,width3,height3);
                filter+=filter3;
            }

            if(startX4>=0 && startY4>=0 && width4>0 && height4>0){
               String filter4 = String.format(Locale.getDefault(), ";[d3]delogo=x=%d:y=%d:w=%d:h=%d",
                        startX4,startY4,width4,height4);
                filter+=filter4;
            }
            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-vf");
            cmdList.add(filter);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }
    //----------增加压缩帧率
    /**
     * 对视频调整帧率, 码率
     * @param video
     * @param framerate 帧率
     * @param bitrate 码率
     * @return
     */
    public String executeAdjustFrameRate(String video,float framerate,int bitrate){
        if (fileExist(video)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-r");
            cmdList.add(String.valueOf(framerate));

            cmdList.add("-acodec");
            cmdList.add("copy");

            encodeBitRate=bitrate;

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     *时长剪切的同时, 做画面裁剪,并调整帧率
     *
     * @param videoFile 输入文件完整路径
     * @param startTimeS  开始时间,单位秒
     * @param duationS  要剪切的时长;
     * @param cropX  画面裁剪的开始X坐标
     * @param cropY  画面裁剪的开始Y坐标
     * @param cropWidth  画面裁剪的宽度
     * @param cropHeight  画面裁剪的高度.
     * @param framerate 要调整的视频帧率, 建议15--30;
     * @return
     */
    public String executeCutCropAdjustFps(String videoFile, float startTimeS, float
            duationS, int cropX, int cropY, int cropWidth, int cropHeight,float framerate) {

        String filter = String.format(Locale.getDefault(), "crop=%d:%d:%d:%d", cropWidth, cropHeight, cropX, cropY);

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("-vcodec");
        cmdList.add("lansoh264_dec");

        cmdList.add("-ss");
        cmdList.add(String.valueOf(startTimeS));

        cmdList.add("-t");
        cmdList.add(String.valueOf(duationS));

        cmdList.add("-i");
        cmdList.add(videoFile);

        cmdList.add("-r");
        cmdList.add(String.valueOf(framerate));

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-acodec");
        cmdList.add("copy");

        return executeAutoSwitch(cmdList);
    }

    /**
     *
     * @param input
     * @param interval 提取帧的间隔; 1秒种提取多少帧;
     * @param scaleW 提取帧的缩放的宽高, 如果为0, 则不缩放
     * @param scaleH
     * @param dstBmp 目标帧序列; 格式:lansonggif_%5d.jpg
     * @return
     */
    public boolean executeExtractFrame(String input,float interval, int scaleW, int scaleH,String dstBmp)
    {
        if (fileExist(input)) {

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(input);

            cmdList.add("-vsync");
            cmdList.add("1");

            cmdList.add("-qscale:v");
            cmdList.add("2");

            cmdList.add("-r");
            cmdList.add(String.valueOf(interval));

            if(scaleW>0 && scaleH>0){
                cmdList.add("-s");
                cmdList.add(String.format(Locale.getDefault(),"%dx%d",scaleW,scaleH));
            }

            cmdList.add("-f");
            cmdList.add("image2");

            cmdList.add("-y");

            cmdList.add(dstBmp);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int  ret= executeVideoEditor(command);
            if(ret==0){
                return true;
            }else{
                return false;
            }
        } else {
            return false;
        }
    }
    public String executeConvertBmpToGif(String bmpPaths,float framerate)
    {
        String gifPath= LanSongFileUtil.createGIFFileInBox();


        List<String> cmdList = new ArrayList<String>();

        cmdList.add("-f");
        cmdList.add("image2");

        cmdList.add("-framerate");
        cmdList.add(String.valueOf(framerate));

        cmdList.add("-i");
        cmdList.add(bmpPaths);

        cmdList.add("-y");
        cmdList.add(gifPath);
        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
        int  ret= executeVideoEditor(command);
        if(ret==0){
            return gifPath;
        }else{
            LanSongFileUtil.deleteFile(gifPath);
            return null;
        }
    }

    /**
     * 把视频转换为 gif
     * @param videoInput 输入视频
     * @param inteval  对输入的视频取帧间隔.
     * @param scaleW  取帧的同时是否要缩放, =0为不缩放
     * @param scaleH
     * @param frameRate 在编码成gif文件的时候的帧率;
     * @return
     */
    public String executeConvertToGif(String videoInput, float inteval,int scaleW,int scaleH,float frameRate)
    {
//        ffmpeg -i d1.mp4 -r 1 -f image2 foo-%03d.jpeg
//        ffmpeg -f image2 -framerate 5 -i foo-%03d.jpeg c.gif

        String  subfix="jpeg";
        LanSongFileUtil.deleteNameFiles("lansonggif",subfix);

        String bmpPaths= LanSongFileUtil.getCreateFileDir("");
        bmpPaths+="/lansonggif_%05d."+subfix;

        if(executeExtractFrame(videoInput,inteval,scaleW,scaleH,bmpPaths)){

            String  ret=executeConvertBmpToGif(bmpPaths,frameRate);

            LanSongFileUtil.deleteNameFiles("lansonggif",subfix);
            return ret;
        }
        return null;
    }


    /**
     * 在视频的指定时间范围内增加一张图片,图片从左上角00开始叠加到视频的上面
     *
     * 比如给视频的第一帧增加一张图片,时间范围是:0.0 --0.03;
     * 注意:如果你用这个给视频增加一张封面的话, 增加好后, 分享到QQ或微信或放到mac系统上, 显示的缩略图不一定是第一帧的画面.
     *
     * LSNEW: 增加在指定时间叠加图片
     * @param srcPath 视频的完整路径
     * @param picPath 图片的完整的路径, 增加后,会从上左上角覆盖视频的第一帧
     * @param startTimeS 开始时间,单位秒.float类型,可以有小数
     * @param endTimeS 结束时间,单位秒.
     * @return
     */
    public String executeAddPitureAtTime(String srcPath,String picPath,float startTimeS,float endTimeS)
    {
        if (fileExist(srcPath) && fileExist(picPath)) {

            List<String> cmdList = new ArrayList<String>();

            String filter = String.format(Locale.getDefault(), "[0:v][1:v] overlay=0:0:enable='between(t,%f,%f)'",
                    startTimeS, endTimeS);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-i");
            cmdList.add(picPath);


            cmdList.add("-filter_complex");
            cmdList.add(String.valueOf(filter));

            cmdList.add("-acodec");
            cmdList.add("copy");
            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 在视频的指定位置,指定时间内叠加一张图片
     *
     * @param srcPath 源视频的完整路径
     * @param picPath 图片的完整路径,png/ jpg
     * @param x  图片的左上角要叠加到源视频的X坐标哪里, 左上角为0,0
     * @param y
     * @param startTimeS 时间范围,开始时间,单位秒
     * @param endTimeS 时间范围, 结束时间, 单位秒.
     * @return
     */
    public String executeAddPitureAtXYTime(String srcPath,String picPath,int x,int y,float startTimeS,float endTimeS)
    {
        if (fileExist(srcPath) && fileExist(picPath)) {

            List<String> cmdList = new ArrayList<String>();

            String filter = String.format(Locale.getDefault(), "[0:v][1:v] overlay=%d:%d:enable='between(t,%f,%f)'",
                    x,y,startTimeS, endTimeS);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-i");
            cmdList.add(picPath);


            cmdList.add("-filter_complex");
            cmdList.add(String.valueOf(filter));

            cmdList.add("-acodec");
            cmdList.add("copy");
            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    /**
     * 给Mp4文件中增加一些描述文字.
     *
     * 比如您可以把一些对该视频的操作信息, 配置信息,服务器的说明信息等放到视频里面,和视频一起传输,
     * 注意:这个文字信息是携带到mp4文件中, 不会增加到每帧上.
     *
     *  这里是写入. 我们有另外的读取
     * LSNEW :
     * @param srcPath 原视频的完整路径
     * @param text 要携带的描述文字
     * @return 增加后的目标文件.
     */
    public String executeAddTextToMp4(String srcPath,String text)
    {
       // ffmpeg -i d1.mp4 -metadata description="LanSon\"g \"Text"
        //  -acodec copy -vcodec copy t1.mp4
        if(fileExist(srcPath) && text!=null) {
            String retPath = LanSongFileUtil.createMp4FileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(srcPath);

            cmdList.add("-metadata");
            cmdList.add("description=");
            cmdList.add(text);

            cmdList.add("-acodec");
            cmdList.add("copy");
            cmdList.add("-vcodec");
            cmdList.add("copy");

            cmdList.add("-y");
            cmdList.add(retPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret = executeVideoEditor(command);
            if (ret == 0) {
                return retPath;
            } else {
                LanSongFileUtil.deleteFile(retPath);
                return null;
            }
        }else{
            return null;
        }
    }

    /**
     * 从视频中获取该视频的描述信息
     * @param srcPath
     * @return
     */
    public String executeGetTextFromMp4(String srcPath)
    {
            //LSTODO. 暂时预留.
         return null;
    }
    /**
     * 获取lansosdk的建议码率;
     * 这个码率不是唯一的, 仅仅是我们建议这样设置, 如果您对码率理解很清楚或有一定的压缩要求,则完全可以不用我们的建议,自行设置.
     *
     * @param wxh 宽度和高度的乘积;
     * @return
     */
    public static int getSuggestBitRate(int wxh) {
        if (wxh <= 480 * 480) {
            return 1000 * 1024;
        } else if (wxh <= 640 * 480) {
            return 1500 * 1024;
        } else if (wxh <= 800 * 480) {
            return 1800 * 1024;
        } else if (wxh <= 960 * 544) {
            return 2000 * 1024;
        } else if (wxh <= 1280 * 720) {
            return 2500 * 1024;
        } else if (wxh <= 1920 * 1088) {
            return 3000 * 1024;
        } else {
            return 3500 * 1024;
        }
    }
    public static int checkSuggestBitRate(int wxh, int bitrate) {
        int sugg = getSuggestBitRate(wxh);
        return bitrate < sugg ? sugg : bitrate;   //如果设置过来的码率小于建议码率,则返回建议码率,不然返回设置码率
    }
    /**
     * 用在编码方法中;
     */
    private MediaInfo _inputInfo=null;
    /**
     * 编码执行, 如果您有特殊的需求, 可以重载这个方法;
     * @param cmdList
     * @return
     */
    public String executeAutoSwitch(List<String> cmdList)
    {
        int ret=0;
        int bitrate=0;
        boolean useSoftWareEncoder=false;
        if(encodeBitRate>0){
            bitrate=encodeBitRate;
        }
        String dstPath= LanSongFileUtil.createMp4FileInBox();

        if(isForceSoftWareDecoder || checkSoftDecoder()){
            for(int i=0;i<cmdList.size();i++){
                String cmd=cmdList.get(i);
                if("lansoh264_dec".equals(cmd)){
                    if(i>0){
                        cmdList.remove(i-1);
                        cmdList.remove(i-1);
                    }
                    break;
                }
            }
        }

//        if(isForceHWEncoder==false && noCheck16Multi==false){ //暂时不加.
//            for(int i=0;i<cmdList.size();i++){
//                String cmd=cmdList.get(i);
//                if("-i".equals(cmd) && i>0){  //  找到第一个输入项;
//                    String videoPath=cmdList.get(i+1);
//                    _inputInfo=new MediaInfo(videoPath);
//                    if(_inputInfo.prepare() && _inputInfo.vFrameRate>0){
//                        if(_inputInfo.getWidth()%16 !=0 || _inputInfo.getHeight()%16 !=0){
//                            Log.e(TAG,"您输入的视频分辨率宽度或高度不是16的倍数, 默认切换为软编码");
//                            useSoftWareEncoder=true;
//                        }
//                    }else{
//                        _inputInfo=null;
//                    }
//                }
//            }
//        }


        if(isForceHWEncoder){

            Log.d(TAG,"开始处理:硬解码+ 硬编码....");
            ret=executeWithEncoder(cmdList, bitrate, dstPath, true);
        }else if(isForceSoftWareEncoder || useSoftWareEncoder || checkSoftEncoder()) {

            Log.d(TAG,"开始处理:硬解码+ 软编码....");
            ret = executeWithEncoder(cmdList, bitrate, dstPath, false);
        }else{

            Log.d(TAG,"开始处理:硬解码+ 硬编码....");
            ret=executeWithEncoder(cmdList, bitrate, dstPath, true);

            if(ret!=0){
                Log.d(TAG,"开始处理:硬解码+ 软编码....");
                ret=executeWithEncoder(cmdList, bitrate, dstPath, false);
            }
        }
        if(ret!=0) {
            for(int i=0;i<cmdList.size();i++){
                String cmd=cmdList.get(i);
                if("lansoh264_dec".equals(cmd)){
                    if(i>0){
                        cmdList.remove(i-1);
                        cmdList.remove(i-1);
                    }
                    break;
                }
            }
            sendEncoderEnchange();
            Log.d(TAG,"开始处理:软解码+ 软编码....");
            ret=executeWithEncoder(cmdList, bitrate, dstPath, false);
        }

        if(ret!=0){
            if(lanSongLogCollector !=null){
                lanSongLogCollector.start();
            }
            Log.e("LanSoJni","编码失败, 开始搜集信息...use software decoder and encoder");
            //再次执行一遍, 读取错误信息;
            ret=executeWithEncoder(cmdList, bitrate, dstPath, false);
            if(lanSongLogCollector !=null && lanSongLogCollector.isRunning()){
                lanSongLogCollector.stop();
            }
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }else{
            return dstPath;
        }
    }
    /**
     * 增加编码器,并开始执行;
     * @param cmdList
     * @param bitrate
     * @param dstPath
     * @param isHWEnc  是否使用硬件编码器; 如果强制了,则以强制为准;
     * @return
     */
    public int executeWithEncoder(List<String> cmdList,int bitrate, String dstPath, boolean isHWEnc)
    {
        List<String> cmdList2 = new ArrayList<String>();
        for(String item: cmdList){
            cmdList2.add(item);
        }
        cmdList2.add("-vcodec");

        if(isHWEnc){
            cmdList2.add("lansoh264_enc");
            cmdList2.add("-pix_fmt");


            if(isQiLinSoc()){
                cmdList2.add("nv21");
                setForceColorFormat(21);  //LSTODO, 没有全面测试...
            }else{
                cmdList2.add("yuv420p");
            }
            cmdList2.add("-b:v");
            cmdList2.add(String.valueOf(bitrate));

        }else{
            cmdList2.add("libx264");

            cmdList2.add("-bf");
            cmdList2.add("0");

            cmdList2.add("-pix_fmt");
            cmdList2.add("yuv420p");

            cmdList2.add("-g");
            cmdList2.add("30");

            if(bitrate==0){
                if(_inputInfo!=null){
                    bitrate=getSuggestBitRate(_inputInfo.vWidth * _inputInfo.vHeight);
                }else{
                    bitrate=(int)(2.5f*1024*1024);
                }
            }

            cmdList2.add("-b:v");
            cmdList2.add(String.valueOf(bitrate));
        }


        cmdList2.add("-y");
        cmdList2.add(dstPath);
        String[] command = new String[cmdList2.size()];
        for (int i = 0; i < cmdList2.size(); i++) {
            command[i] = (String) cmdList2.get(i);
        }
        int ret=executeVideoEditor(command);
        return ret;
    }
    /**
     * 检测是否需要软编码;
     * @return
     */
    public boolean checkSoftEncoder()
    {
        for(String item: qilinCpulist){
            if(item.contains(Build.MODEL) && isSupportNV21ColorFormat()==false){
                isForceSoftWareEncoder=true;
                return true;
            }
        }

        if(Build.MODEL!=null && isSupportNV21ColorFormat()==false) {
            if (Build.MODEL.contains("-AL00") || Build.MODEL.contains("-CL00")) {
                isForceSoftWareEncoder = true;
                return true;
            }
        }
        return false;
    }

    //是否是麒麟处理器.
    public static boolean isQiLinSoc()
    {
        for(String item: qilinCpulist){
            if(item.contains(Build.MODEL)){
                return true;
            }
        }
        if(Build.MODEL!=null) {
            if (Build.MODEL.contains("-AL00") || Build.MODEL.contains("-CL00")) {
                return true;
            }
        }
        return false;
    }
    /**
     * 强制软编码器;
     * @return
     */
    public boolean checkSoftDecoder()
    {
        for(String item: useSoftDecoderlist){
            if(item.equalsIgnoreCase(Build.MODEL)){
                return true;
            }else if(item.contains(Build.MODEL)){
                return true;
            }
        }
        return false;
    }

    /**
     * 当数据不是16的倍数的时候,把他调整成16的倍数, 以最近的16倍数为准;
     * 举例如下:
     * 16, 17, 18, 19,20,21,22,23 ==>16;
     * 24,25,26,27,28,29,30,31,32==>32;
     *
     *
     * 如果是18,19这样接近16,则等于16, 等于缩小了原有的画面,
     * 如果是25,28这样接近32,则等于32,  等于稍微拉伸了原来的画面,
     * 因为最多缩小或拉伸8个像素, 还不至于画面严重变形,而又兼容编码器的要求,故可以这样做.
     *
     * @return
     */
    public static int make16Closest(int value) {

        if (value < 16) {
            return value;
        } else {
            value += 8;
            int val2 = value / 16;
            val2 *= 16;
            return val2;
        }
    }

    /**
     * 把数据变成16的倍数, 以大于等于16倍数的为准;
     * 比如:
     * 16-->返回16;
     * 17---31-->返回32;
     *
     * @param value
     * @return
     */
    public static int make16Next(int value) {
        if(value%16==0){
            return value;
        }else{
            return ((int)(value/16.0f +1)*16) ;
        }
    }


    private static int checkCPUName() {
        String str1 = "/proc/cpuinfo";
        String str2 = "";
        try {
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
            str2 = localBufferedReader.readLine();
            while (str2 != null) {
//                Log.i("testCPU","->"+str2+"<-");
                str2 = localBufferedReader.readLine();
                if(str2.contains("SDM845")){  //845的平台;

                }
            }
            localBufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }




    private static final String MIME_TYPE_AVC = "video/avc"; // H.264 Advanced

    private static boolean isSupportNV21=false;
    /**
     * 是否支持NV21的编码;
     * @return
     */
    public boolean isSupportNV21ColorFormat()
    {
        if(isSupportNV21){
            return true;
        }

        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE_AVC);
        if (codecInfo == null) {
            return false;
        }
        isSupportNV21=selectColorFormat(codecInfo, MIME_TYPE_AVC);
        return isSupportNV21;
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
    private static boolean selectColorFormat(MediaCodecInfo codecInfo,
                                        String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];

            if(colorFormat==21){
                return true;
            }
        }
        return false;
    }

    /**
     * 精确裁剪的同时,缩放到指定位置,不同于上面的命令,这个可以设置宽度和高度. 其中宽度和高度是采用缩放来完成.
     * <p>
     * <p>
     * 采用的是软缩放的形式.
     *
     * @param videoFile
     * @param startS
     * @param durationS
     * @param width     要缩放到的宽度 建议是16的倍数 ,
     * @param height    要缩放到的高度, 建议是16的倍数
     * @param framerate 帧率
     * @param bitrate   码率
     * @return
     */
    public String executeCutVideoExactTest(String videoFile,
                                           float startS,
                                           float durationS,
                                           int width,
                                           int height,
                                           float framerate,
                                           int bitrate) {
        if (fileExist(videoFile)) {
            List<String> cmdList = new ArrayList<String>();

            String scalecmd = String.format(Locale.getDefault(), "scale=%d:%d", width, height);

            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");

            cmdList.add("-i");
            cmdList.add(videoFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

            cmdList.add("-t");
            cmdList.add(String.valueOf(durationS));

            //---测试调整码率、帧率+裁剪
            cmdList.add("-r");
            cmdList.add(String.valueOf(framerate));
            encodeBitRate = bitrate;
            //---测试调整码率、帧率+裁剪

            cmdList.add("-vf");
            cmdList.add(scalecmd);

            cmdList.add("-acodec");
            cmdList.add("copy");

            return executeAutoSwitch(cmdList);
        } else {
            return null;
        }
    }

    public String testGetAudioM4a(String srcPath) {

        String dstPath= LanSongFileUtil.createM4AFileInBox();

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("-i");
        cmdList.add(srcPath);
        cmdList.add("-vn");
        cmdList.add("-acodec");
        cmdList.add("libfaac");
        cmdList.add("-b:a");
        cmdList.add("64000");
        cmdList.add("-y");
        cmdList.add(dstPath);

        String[] command = new String[cmdList.size()];
        for (int i = 0; i < cmdList.size(); i++) {
            command[i] = (String) cmdList.get(i);
        }
       if(executeVideoEditor(command)==0){
            return dstPath;
       }else{
            LSLog.e("executeGetAudioTrack error");
            return null;
       }
    }
    public int testVideoMergeAudio(String video, String audio,String dstPath) {


            List<String> cmdList = new ArrayList<String>();
            cmdList.add("-i");
            cmdList.add(video);

            cmdList.add("-i");
            cmdList.add(audio);


            cmdList.add("-map");
            cmdList.add("0:v");

            cmdList.add("-map");
            cmdList.add("1:a");

            cmdList.add("-vcodec");
            cmdList.add("copy");

            cmdList.add("-acodec");
            cmdList.add("copy");

            cmdList.add("-absf");
            cmdList.add("aac_adtstoasc");


            cmdList.add("-y");
            cmdList.add(dstPath);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            VideoEditor editor = new VideoEditor();
            return editor.executeVideoEditor(command);
    }
}
