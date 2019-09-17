package com.lansosdk.videoeditor;

import com.lansosdk.box.LSLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.lansosdk.videoeditor.LanSongFileUtil.fileExist;

/**
 * 音频编辑类.
 *
 *
 * 使用方法和VideoEditor一样;
 */
public class AudioEditor {
    private VideoEditor editor;
    // ffmpeg -f s16le -ar 44100 -ac 2 -i hongdou_44100_2.pcm -f s16le -ar 48000
// -ac 2 hongdou_48000_2.pcm
    public AudioEditor()
    {
        editor=new VideoEditor();
        editor.setOnProgessListener(new onVideoEditorProgressListener() {
            @Override
            public void onProgress(VideoEditor v, int percent) {
                if(monAudioEditorProgressListener!=null){
                    monAudioEditorProgressListener.onProgress(AudioEditor.this,percent);
                }
            }
        });
    }

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
     * LSTODO增加静音举例.
     *
     * @param filelength
     * @param channel
     * @param sampleRate
     * @param bitperSample
     * @return
     */
    public static byte[] getWavheader(int filelength, int channel, int sampleRate, int bitperSample) {
        byte header[] = new byte[44];
        VideoEditor.createWavHeader(filelength, channel, sampleRate, bitperSample, header);
        return header;
    }

    private onAudioEditorProgressListener monAudioEditorProgressListener=null;
    public interface  onAudioEditorProgressListener{
        void onProgress(AudioEditor v, int percent);
    }

    public void setOnAudioEditorProgressListener(onAudioEditorProgressListener listener){
        monAudioEditorProgressListener=listener;
    }

    /**
     * 把pcm格式的数据, 转换采样率
     * @param srcPcm
     * @param srcSample pcm的采样率
     * @param srcChannel  pcm的通道数
     * @param dstSample 要转换的采样率
     * @return 返回wav格式的数据, 或null;
     */
    public String executePcmConvertToWav(String srcPcm, int srcSample,
                                              int srcChannel, int dstSample) {

        //            ffmpeg -f s16le -ac 2 -ar 48000 -i huo_48000_2.pcm -ac 2 -ar 44100 huo.wav
        if(fileExist(srcPcm) && dstSample>0){
            String dstPath= LanSongFileUtil.createWAVFileInBox();


            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-f");
            cmdList.add("s16le");
            cmdList.add("-ac");
            cmdList.add(String.valueOf(srcChannel));
            cmdList.add("-ar");
            cmdList.add(String.valueOf(srcSample));
            cmdList.add("-i");
            cmdList.add(srcPcm);

            cmdList.add("-ac");
            cmdList.add("2");  //目标通道数默认是双通道;
            cmdList.add("-ar");
            cmdList.add(String.valueOf(dstSample));

            //wav格式的数据, 不压缩, 没有码率
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }

            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSLog.e("executePcmConvertSamplerate 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSLog.e("executePcmConvertSamplerate 执行失败, 文件不存在");
            return null;
        }
    }


    /**
     * 把视频中的声音 mp4,或mp3, 或m4a类型的音频文件, 转换为wav格式;
     *
     * @param inputAudio 输入带音轨的视频,或mp3/m4a格式的音频文件;
     * @param dstSample  设置输出的采样率, 如果不需要更改采样率,则设置为0;
     * @return
     */
    public String  executeConvertToWav(String inputAudio,int dstSample) {
        if(fileExist(inputAudio)){

            String dstPath= LanSongFileUtil.createWAVFileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(inputAudio);

            cmdList.add("-vn");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }

            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSLog.e("executeConvertToWav 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSLog.e("executeConvertToWav 执行失败, 文件不存在");
            return null;
        }
    }
    /**
     * 把输入的音频(或含有音频视频)转为单声道,并可以设置采样率,然后wav格式输出.
     * @param inputAudio    输入原文件的完整路径
     * @param dstSample  要设置的采样率, 如果不变,则设置为-1;
     * @return
     */
    public String  executeConvertToMonoWav(String inputAudio,int dstSample) {
        if(fileExist(inputAudio)){

            String dstPath= LanSongFileUtil.createWAVFileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(inputAudio);

            cmdList.add("-vn");

            cmdList.add("-f");
            cmdList.add("s16le");

            cmdList.add("-ac");
            cmdList.add("1");
            cmdList.add("-acodec");
            cmdList.add("pcm_s16le");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }

            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSLog.e("executeConvertToWav 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSLog.e("executeConvertToWav 执行失败, 文件不存在");
            return null;
        }
    }

    /**
     * 把wav格式的音频转换为mp3;
     * @param wavInput  原声音;
     * @param dstSample  在转换为mp3的时候, 调整mp3的采样率;如不变,设置为0
     * @return
     */
    public String executeConvertWavToMp3(String wavInput,int dstSample)
    {
        if(fileExist(wavInput)){

            String dstPath= LanSongFileUtil.createMP3FileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(wavInput);

            cmdList.add("-acodec");
            cmdList.add("libmp3lame");
            cmdList.add("-b:a");
            cmdList.add("128000");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSLog.e("executeConvertWavToMp3 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSLog.e("executeConvertWavToMp3 执行失败, 文件不存在");
            return null;
        }
    }

    /**
     * 把wav转换为m4a;
     *
     * [建议用AudioPadExecute]
     *
     * @param wavInput wav输入源文件;
     * @param dstSample  在转换为m4a的时候, 调整m4a的采样率;如不变,设置为0
     * @return
     */
    public String executeConvertWavToM4a(String wavInput,int dstSample)
    {
        if(fileExist(wavInput)){

            String dstPath= LanSongFileUtil.createM4AFileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(wavInput);

            cmdList.add("-acodec");
            cmdList.add("libfaac");
            cmdList.add("-b:a");
            cmdList.add("128000");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSLog.e("executeConvertWavToM4a 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSLog.e("executeConvertWavToM4a 执行失败, 文件不存在");
            return null;
        }
    }

    /**
     * 把m4a音频转换为mp3;
     *
     * [建议用AudioPadExecute]
     * m4a可以是带音轨的视频,也可以是独立的m4a文件;
     * @param input   带音轨的视频,或独立的m4a文件;
     * @param dstSample 在转换为mp3的时候, 调整mp3的采样率;如不变,设置为0
     * @return
     */
    public String executeConvertM4aToMp3(String input,int dstSample)
    {
        if(fileExist(input)){

            String dstPath= LanSongFileUtil.createMP3FileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(input);

            cmdList.add("-vn");

            cmdList.add("-acodec");
            cmdList.add("libmp3lame");
            cmdList.add("-b:a");
            cmdList.add("128000");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSLog.e("executeConvertM4aToMp3 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSLog.e("executeConvertM4aToMp3 执行失败, 文件不存在");
            return null;
        }
    }

    /**
     * 把mp3音频转换为m4a
     *
     * [建议用AudioPadExecute]
     *
     * @param input   带音轨的视频,或独立的mp3文件;
     * @param dstSample 在转换为m4a的时候, 调整m4a的采样率;如不变,设置为0
     * @return
     */
    public String executeConvertMp3ToM4a(String input,int dstSample)
    {
        if(fileExist(input)){

            String dstPath= LanSongFileUtil.createM4AFileInBox();

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(input);

            cmdList.add("-vn");

            cmdList.add("-acodec");
            cmdList.add("libfaac");
            cmdList.add("-b:a");
            cmdList.add("128000");

            cmdList.add("-ac");
            cmdList.add("2");

            if(dstSample>0){
                cmdList.add("-ar");
                cmdList.add(String.valueOf(dstSample));
            }
            cmdList.add("-y");
            cmdList.add(dstPath);

            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            int ret=editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LSLog.e("executeConvertMp3ToM4a 失败, 请查看打印信息");
                return null;
            }
        }else{
            LSLog.e("executeConvertMp3ToM4a 执行失败, 文件不存在");
            return null;
        }
    }
    /**
     *
     * 两个裸数据混合.
     *
     * @param srcPach1    pcm格式的主音频
     * @param samplerate  主音频采样率
     * @param channel     主音频通道数
     * @param srcPach2    pcm格式的次音频
     * @param samplerate2 次音频采样率
     * @param channel2    次音频通道数
     * @param value1      主音频的音量
     * @param value2      次音频的音量
     * @return 输出文件.输出也是pcm格式的音频文件.
     */
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
        int  ret= editor.executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }

    /**
     * 把pcm格式的音频文件编码成AAC
     *
     * @param srcPach    源pcm文件
     * @param samplerate pcm的采样率
     * @param channel    pcm的通道数
     * @return  输出的m4a合成后的文件
     */
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
        int ret= editor.executeVideoEditor(command);
        if(ret==0){
            return dstPath;
        }else{
            LanSongFileUtil.deleteFile(dstPath);
            return null;
        }
    }
    /**
     * 把mp3转换为AAC;
     *
     * @param mp3Path
     * @param startS
     * @param durationS
     * @return
     */
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
            int ret= editor.executeVideoEditor(command);
            if(ret==0){
                return dstPath;
            }else{
                LanSongFileUtil.deleteFile(dstPath);
                return null;
            }
        }
        return null;
    }
    /**
     * 音频裁剪,截取音频文件中的一段.
     * 需要注意到是: 尽量保持裁剪文件的后缀名和源音频的后缀名一致.
     *
     * @param srcFile   源音频
     * @param startS    开始时间,单位是秒. 可以有小数
     * @param durationS 裁剪的时长.
     * @return
     */
    public String executeCutAudio(String srcFile, float startS, float durationS) {
        if (fileExist(srcFile)) {

            List<String> cmdList = new ArrayList<String>();

            String dstFile= LanSongFileUtil.createFileInBox(LanSongFileUtil.getFileSuffix(srcFile));

            cmdList.add("-i");
            cmdList.add(srcFile);

            cmdList.add("-ss");
            cmdList.add(String.valueOf(startS));

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
            int ret= editor.executeVideoEditor(command);
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
     * 用其他音频替换视频中的声音;
     *
     * 音频可以是 mp3, wav, m4a格式;
     *
     * 输出的音频采样率是44100;
     *
     * [如果此方法无法满足您的方法或不够灵活,请使用音频容器: AudioPadExecute来操作]
     *
     * @param video
     * @param audio
     * @return  替换后的输出文件;
     */
    public String  executeVideoReplaceAudio(String video,String audio){
        return executeVideoMergeAudio(video,audio,0.0f,1.0f);
    }
    /**
     * 把一个音乐合并到视频中;
     *
     *  [如果此方法无法满足您的方法或不够灵活,请使用音频容器: AudioPadExecute来操作]
      * @param video  原视频
     * @param audio 要合并的音频
     * @param volume1  在合并的时候, 原视频中的音频音量, 如果为0,则删除原有的视频声音;  范围是0--10.0f, 1.0为原音量,2.0是放大一倍.0.5是降低一倍;
     * @param volume2  合并时的, 音频音量; 范围是0--10.0f
     * @return 合并后的返回目标视频;
     */
    public String executeVideoMergeAudio(String video, String  audio,float volume1,float volume2) {

        if(volume2<=0){
            return video;
        }

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

            if(volume1>0 && vInfo.isHaveAudio()){//两个声音混合;
                String filter = String.format(Locale.getDefault(), "[0:a]volume=volume=%f[a1]; [1:a]volume=volume=%f[a2]; " +
                        "[a1][a2]amix=inputs=2:duration=first:dropout_transition=2", volume1, volume2);

                cmdList.add("-filter_complex");
                cmdList.add(filter);

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
            }else if(isAAC && volume2==1.0f) {  //删去视频的原音,直接增加音频

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

                String filter=String.format(Locale.getDefault(), "volume=%f",volume2);
                cmdList.add("-af");
                cmdList.add(filter);

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

    public int concatAudio(String[] tsArray, String dstFile) {
        // ffmpeg -i "concat:a.mp3|a1.mp3" -acodec copy a2.mp3
        if (LanSongFileUtil.filesExist(tsArray)) {
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

            cmdList.add("-y");

            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            return editor.executeVideoEditor(command);
        } else {
            return -1;
        }
    }
}
