package com.lansosdk.videoeditor;

import android.util.Log;

import com.lansosdk.box.LSLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 视频布局
 * 把多个视频画面,并列放在一起, 合成一个画面, 当前默认是以最长的视频为准, 如果有短的视频,则短视频停止到最后一帧;
 *
 * 支持软编码和硬件编码;
 * 建议合成后的视频分辨率不要大于720*1280;
 *
 * ffmpeg -i p1080.jpg -i d2.mp4 -filter_complex "overlay=0:0;[0:a][1:a]amix=inputs=2" dd12.mp4
 *
 * 每个输入视频的路径,都可以是一张图片.图片支持png, jpg, tif格式.
 *
 * 杭州蓝松科技有限公司
 * www.lansongtech.com
 */
public class VideoLayout extends VideoEditor {

    //2018年5月29日16:52:43增加音频;

    private static final String TAG = LSLog.TAG;

    /**
     * 是否使用软件解码器
     */
    public static boolean isUseSoftDecoder=true;
    /**
     * 两个视频合并;
     * <p>
     * 原理是(一下所有多个视频类似):
     *
     * 设置一个输出视频画面的宽度和高度, 认为是一个区域, 然后把一个一个的输入视频完整的放到这个区域里; 区域的X,Y坐标是一个一个的像素点;
     * 比如设置宽度是1280, 宽度是720的一个区域; 则第一个视频A的宽高是640x360,开始坐标是0,0,则放到区域的左上角;
     *
     * 如果想第二个视频B放到第一个视频的下面,则B的x坐标和A视频的x坐标一致,Y坐标是A视频的高度;
     * 如果想第二个视频和第一个视频有一定的间隔,则Y就等于A视频的高度+几个像素;
     *
     *
     * 如果想第三个视频C放到第一个视频的坐标,则C的Y坐标和A视频的Y坐标一致, x坐标是A视频的宽度; 如果中间有间隔,则x等于A视频的宽度+几个像素;
     *
     * 注意,代码里没有做 每个视频是否存在的判断, 没有做宽度和高度判断;
     *
     * @param outW     所有视频或图片放置到的目标区域的宽高,也是输出视频的分辨率
     * @param outH
     * @param v1          第一个视频或图片的完整路径
     * @param v1X         放到区域的哪个坐标 XY
     * @param v1Y
     * @param v2          第二个视频或图片的完整路径
     * @param v2X         放到区域的哪个坐标上; xy坐标;
     * @param v2Y
     * @return
     */
    public String executeLayout2Video(int outW, int outH,
                                      String v1, int v1X, int v1Y,
                                      String v2, int v2X, int v2Y) {

        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];[base][0:v] overlay=x=%d:y=%d [tmp1]; [tmp1][1:v] overlay=x=%d:y=%d",
                outW, outH, v1X, v1Y, v2X, v2Y);


        List<String> cmdList = new ArrayList<String>();

        float duration = getMaxDuration(Arrays.asList(v1,v2));

        filter+=getAudioCmd();
        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }
        cmdList.add("-threads");
        cmdList.add(String.valueOf(16));

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(duration));

        return  doVideoLayout(cmdList,outW,outH);
    }

    /**
     *
     * @param outW   所有视频或图片放置到的目标区域的宽高,也是输出视频的分辨率
     * @param outH
     * @param p1   第一个视频或图片的参数
     * @param p2   第二个视频或图片的参数.
     * @return
     */
    public String executeLayoutScale2Video(int outW, int outH,
                                           VideoLayoutParam p1,
                                           VideoLayoutParam p2) {

        String filter = String.format(Locale.getDefault(), "nullsrc=size=%dx%d [base];"+
                        "[0:v] setpts=PTS-STARTPTS,scale=%dx%d [tmp1]; " +
                        "[1:v] setpts=PTS-STARTPTS,scale=%dx%d [upperright]; " +
                        "[base][tmp1] overlay=x=%d:y=%d [tmp2];"+
                        "[tmp2][upperright] overlay=x=%d:y=%d",
                outW, outH,p1.scaleW, p1.scaleH, p2.scaleW, p2.scaleH, p1.x, p1.y,p2.x, p2.y);


        float duration = getMaxDuration(Arrays.asList(p1.video, p2.video));

        filter+=getAudioCmd();
        List<String> cmdList = new ArrayList<String>();

        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }
        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(duration));

        return doVideoLayout(cmdList,outW,outH);
    }

    /**
     * 3个视频合并
     * 参数, 布局原理 见上面;
     * @return
     */
    public String executeLayout3Video(int outW, int outH,
                                      String v1, int v1X, int v1Y,
                                      String v2, int v2X, int v2Y,
                                      String v3, int v3X, int v3Y) {

        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];" +
                        "[base][0:v] overlay=x=%d:y=%d [tmp1];"+
                        "[tmp1][1:v] overlay=x=%d:y=%d [tmp2];"+
                        "[tmp2][2:v] overlay=x=%d:y=%d",outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y);




        List<String> cmdList = new ArrayList<String>();
        float duration = getMaxDuration(Arrays.asList(v1,v2,v3));
        filter+=getAudioCmd();

        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }
        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");

        cmdList.add(String.valueOf(duration));

        return doVideoLayout(cmdList,outW,outH);
    }

    public String executeLayoutScale3Video(int outW, int outH,
                                           VideoLayoutParam p1,
                                           VideoLayoutParam p2,
                                           VideoLayoutParam p3) {
        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];"+
                        "[0:v] setpts=PTS-STARTPTS,scale=%dx%d [scale0];" +
                        "[1:v] setpts=PTS-STARTPTS,scale=%dx%d [scale1];" +
                        "[2:v] setpts=PTS-STARTPTS,scale=%dx%d [scale2];" +
                        "[base][scale0] overlay=x=%d:y=%d [over1];"+
                        "[over1][scale1] overlay=x=%d:y=%d [over2];"+
                        "[over2][scale2] overlay=x=%d:y=%d",
                outW, outH,p1.scaleW, p1.scaleH, p2.scaleW, p2.scaleH,p3.scaleW, p3.scaleH, p1.x, p1.y,p2.x, p2.y,p3.x, p3.y);



        List<String> cmdList = new ArrayList<String>();

        float duration = getMaxDuration(Arrays.asList(p1.video, p2.video, p3.video));
        filter+=getAudioCmd();
        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }

        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-i");
        cmdList.add(p3.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(duration));

        return doVideoLayout(cmdList,outW,outH);
    }

    /**
     * 4个视频的合并
     */
    public String executeLayout4Video(int outW, int outH,
                                      String v1, int v1X, int v1Y,
                                      String v2, int v2X, int v2Y,
                                      String v3, int v3X, int v3Y,
                                      String v4, int v4X, int v4Y) {

        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];" +
                        "[base][0:v] overlay=x=%d:y=%d [tmp1];"+
                        "[tmp1][1:v] overlay=x=%d:y=%d [tmp2];"+
                        "[tmp2][2:v] overlay=x=%d:y=%d [tmp3];"+
                        "[tmp3][3:v] overlay=x=%d:y=%d",outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y,v4X,v4Y);
        List<String> cmdList = new ArrayList<String>();

        float dstDuration=getMaxDuration(Arrays.asList(v1,v2,v3,v4));
        filter+=getAudioCmd();
        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-i");
        cmdList.add(v4);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));

        return doVideoLayout(cmdList,outW,outH);
    }
    public String executeLayoutScale4Video(int outW, int outH,
                                           VideoLayoutParam p1,
                                           VideoLayoutParam p2,
                                           VideoLayoutParam p3,
                                           VideoLayoutParam p4)
    {

        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];"+
                        "[0:v] setpts=PTS-STARTPTS,scale=%dx%d [scale0]; " +
                        "[1:v] setpts=PTS-STARTPTS,scale=%dx%d [scale1]; " +
                        "[2:v] setpts=PTS-STARTPTS,scale=%dx%d [scale2]; " +
                        "[3:v] setpts=PTS-STARTPTS,scale=%dx%d [scale3]; " +
                        "[base][scale0] overlay=x=%d:y=%d [over1];"+
                        "[over1][scale1] overlay=x=%d:y=%d [over2];"+
                        "[over2][scale2] overlay=x=%d:y=%d [over3];"+
                        "[over3][scale3] overlay=x=%d:y=%d",
                outW, outH,p1.scaleW,
                p1.scaleH, p2.scaleW, p2.scaleH,p3.scaleW, p3.scaleH, p4.scaleW, p4.scaleH,
                p1.x, p1.y,p2.x, p2.y,p3.x, p3.y,p4.x, p4.y);

        List<String> cmdList = new ArrayList<String>();

        float dstDuration=getMaxDuration(Arrays.asList(p1.video,p2.video,p3.video,p4.video));
        filter+=getAudioCmd();
        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }
        cmdList.add("-threads");
        cmdList.add(String.valueOf(16));

        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-i");
        cmdList.add(p3.video);

        cmdList.add("-i");
        cmdList.add(p4.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));

        return doVideoLayout(cmdList,outW,outH);
    }

    /**
     * 5个视频拼接
     */
    public String executeLayout5Video(int outW, int outH,
                                      String v1, int v1X, int v1Y,
                                      String v2, int v2X, int v2Y,
                                      String v3, int v3X, int v3Y,
                                      String v4, int v4X, int v4Y,
                                      String v5, int v5X, int v5Y) {
        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];" +
                        "[base][0:v] overlay=x=%d:y=%d [tmp1];"+
                        "[tmp1][1:v] overlay=x=%d:y=%d [tmp2];"+
                        "[tmp2][2:v] overlay=x=%d:y=%d [tmp3];"+
                        "[tmp3][3:v] overlay=x=%d:y=%d [tmp4];"+
                        "[tmp4][4:v] overlay=x=%d:y=%d",outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y,v4X,v4Y,v5X,v5Y);

        List<String> cmdList = new ArrayList<String>();
        float dstDuration=getMaxDuration(Arrays.asList(v1,v2,v3,v4,v5));
        filter+=getAudioCmd();

        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-i");
        cmdList.add(v4);

        cmdList.add("-i");
        cmdList.add(v5);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");

        cmdList.add(String.valueOf(dstDuration));

        return doVideoLayout(cmdList,outW,outH);
    }

    public String executeLayoutScale5Video(int outW, int outH,
                                           VideoLayoutParam p1,
                                           VideoLayoutParam p2,
                                           VideoLayoutParam p3,
                                           VideoLayoutParam p4,
                                           VideoLayoutParam p5) {

        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];"+
                        "[0:v] setpts=PTS-STARTPTS,scale=%dx%d [scale0]; " +
                        "[1:v] setpts=PTS-STARTPTS,scale=%dx%d [scale1]; " +
                        "[2:v] setpts=PTS-STARTPTS,scale=%dx%d [scale2]; " +
                        "[3:v] setpts=PTS-STARTPTS,scale=%dx%d [scale3]; " +
                        "[4:v] setpts=PTS-STARTPTS,scale=%dx%d [scale4]; " +
                        "[base][scale0] overlay=x=%d:y=%d [over1];"+
                        "[over1][scale1] overlay=x=%d:y=%d [over2];"+
                        "[over2][scale2] overlay=x=%d:y=%d [over3];"+
                        "[over3][scale3] overlay=x=%d:y=%d [over4];"+
                        "[over4][scale4] overlay=x=%d:y=%d",
                outW, outH,p1.scaleW,
                p1.scaleH, p2.scaleW, p2.scaleH,p3.scaleW, p3.scaleH, p4.scaleW, p4.scaleH, p5.scaleW, p5.scaleH,
                p1.x, p1.y,p2.x, p2.y,p3.x, p3.y,p4.x, p4.y,p5.x, p5.y);

        List<String> cmdList = new ArrayList<String>();

        float dstDuration=getMaxDuration(Arrays.asList(p1.video,p2.video,p3.video,p4.video,p5.video));
        filter+=getAudioCmd();

        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }
        cmdList.add("-threads");
        cmdList.add(String.valueOf(16));

        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-i");
        cmdList.add(p3.video);

        cmdList.add("-i");
        cmdList.add(p4.video);

        cmdList.add("-i");
        cmdList.add(p5.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));


        return doVideoLayout(cmdList,outW,outH);
    }
    /**
     * 6个视频拼接
     * @return
     */
    public String executeLayout6Video(int outW, int outH,
                                      String v1, int v1X, int v1Y,
                                      String v2, int v2X, int v2Y,
                                      String v3, int v3X, int v3Y,
                                      String v4, int v4X, int v4Y,
                                      String v5, int v5X, int v5Y,
                                      String v6, int v6X, int v6Y) {

        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];" +
                        "[base][0:v] overlay=x=%d:y=%d [tmp1];"+
                        "[tmp1][1:v] overlay=x=%d:y=%d [tmp2];"+
                        "[tmp2][2:v] overlay=x=%d:y=%d [tmp3];"+
                        "[tmp3][3:v] overlay=x=%d:y=%d [tmp4];"+
                        "[tmp4][4:v] overlay=x=%d:y=%d [tmp5];"+
                        "[tmp5][5:v] overlay=x=%d:y=%d",outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y,v4X,v4Y,v5X,v5Y,v6X,v6Y);

        List<String> cmdList = new ArrayList<String>();
        float dstDuration=getMaxDuration(Arrays.asList(v1,v2,v3,v4,v5,v6));
        filter+=getAudioCmd();

        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-i");
        cmdList.add(v4);

        cmdList.add("-i");
        cmdList.add(v5);

        cmdList.add("-i");
        cmdList.add(v6);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");


        cmdList.add(String.valueOf(dstDuration));

        return doVideoLayout(cmdList,outW,outH);
    }

    public String executeLayoutScale6Video(int outW, int outH,
                                           VideoLayoutParam p1,
                                           VideoLayoutParam p2,
                                           VideoLayoutParam p3,
                                           VideoLayoutParam p4,
                                           VideoLayoutParam p5,
                                           VideoLayoutParam p6) {

        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];"+
                        "[0:v] setpts=PTS-STARTPTS,scale=%dx%d [scale0]; " +
                        "[1:v] setpts=PTS-STARTPTS,scale=%dx%d [scale1]; " +
                        "[2:v] setpts=PTS-STARTPTS,scale=%dx%d [scale2]; " +
                        "[3:v] setpts=PTS-STARTPTS,scale=%dx%d [scale3]; " +
                        "[4:v] setpts=PTS-STARTPTS,scale=%dx%d [scale4]; " +
                        "[5:v] setpts=PTS-STARTPTS,scale=%dx%d [scale5]; " +
                        "[base][scale0] overlay=x=%d:y=%d [over1];"+
                        "[over1][scale1] overlay=x=%d:y=%d [over2];"+
                        "[over2][scale2] overlay=x=%d:y=%d [over3];"+
                        "[over3][scale3] overlay=x=%d:y=%d [over4];"+
                        "[over4][scale4] overlay=x=%d:y=%d [over5];"+
                        "[over5][scale5] overlay=x=%d:y=%d",
                outW, outH,p1.scaleW,
                p1.scaleH, p2.scaleW, p2.scaleH,p3.scaleW, p3.scaleH, p4.scaleW, p4.scaleH, p5.scaleW, p5.scaleH,p6.scaleW, p6.scaleH,
                p1.x, p1.y,p2.x, p2.y,p3.x, p3.y,p4.x, p4.y,p5.x, p5.y,p6.x, p6.y);
        List<String> cmdList = new ArrayList<String>();

        float dstDuration=getMaxDuration(Arrays.asList(p1.video,p2.video,p3.video, p4.video,p5.video,p6.video));
        filter+=getAudioCmd();

        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }

        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-i");
        cmdList.add(p3.video);

        cmdList.add("-i");
        cmdList.add(p4.video);

        cmdList.add("-i");
        cmdList.add(p5.video);

        cmdList.add("-i");
        cmdList.add(p6.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(dstDuration));

        return doVideoLayout(cmdList,outW,outH);
    }
    /**
     * 9个视频拼接
     * @return
     */
    public String executeLayout9Video(int outW, int outH,
                                      String v1, int v1X, int v1Y,
                                      String v2, int v2X, int v2Y,
                                      String v3, int v3X, int v3Y,
                                      String v4, int v4X, int v4Y,
                                      String v5, int v5X, int v5Y,
                                      String v6, int v6X, int v6Y,
                                      String v7, int v7X, int v7Y,
                                      String v8, int v8X, int v8Y,
                                      String v9, int v9X, int v9Y) {

        //String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,pad=%d:%d:%d:%d:White [in1]; [1:v] setpts=PTS-STARTPTS [in2];[2:v] setpts=PTS-STARTPTS [in3];[3:v] setpts=PTS-STARTPTS [in4]; [4:v] setpts=PTS-STARTPTS [in5]; [5:v] setpts=PTS-STARTPTS [in6]; [6:v] setpts=PTS-STARTPTS [in7];[7:v] setpts=PTS-STARTPTS [in8]; [8:v] setpts=PTS-STARTPTS [in9]; [in1][in2] overlay=x=%d:y=%d [tmp1]; [tmp1][in3] overlay=x=%d:y=%d [tmp2]; [tmp2][in4] overlay=x=%d:y=%d [tmp3];[tmp3][in5] overlay=x=%d:y=%d [tmp4]; [tmp4][in6] overlay=x=%d:y=%d [tmp5]; [tmp5][in7] overlay=x=%d:y=%d [tmp6]; [tmp6][in8] overlay=x=%d:y=%d [tmp7]; [tmp7][in9] overlay=x=%d:y=%d", outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y, v4X, v4Y, v5X, v5Y, v6X, v6Y, v7X, v7Y, v8X, v8Y, v9X, v9Y);
        String filter = String.format(Locale.getDefault(),
                "nullsrc=size=%dx%d [base];" +
                        "[base][0:v] overlay=x=%d:y=%d [tmp1];"+
                        "[tmp1][1:v] overlay=x=%d:y=%d [tmp2];"+
                        "[tmp2][2:v] overlay=x=%d:y=%d [tmp3];"+
                        "[tmp3][3:v] overlay=x=%d:y=%d [tmp4];"+
                        "[tmp4][4:v] overlay=x=%d:y=%d [tmp5];"+
                        "[tmp5][5:v] overlay=x=%d:y=%d [tmp6];"+
                        "[tmp6][6:v] overlay=x=%d:y=%d [tmp7];"+
                        "[tmp7][7:v] overlay=x=%d:y=%d [tmp8];"+
                        "[tmp8][8:v] overlay=x=%d:y=%d",outW, outH, v1X, v1Y, v2X, v2Y, v3X, v3Y,v4X,v4Y,v5X,v5Y,v6X,v6Y
                ,v7X,v7Y,v8X,v8Y,v9X,v9Y);
        List<String> cmdList = new ArrayList<String>();
        float dstDuration=getMaxDuration(Arrays.asList(v1,v2,v3,v4,v5,v6,v7,v8,v9));
        filter+=getAudioCmd();

        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }

        cmdList.add("-i");
        cmdList.add(v1);

        cmdList.add("-i");
        cmdList.add(v2);

        cmdList.add("-i");
        cmdList.add(v3);

        cmdList.add("-i");
        cmdList.add(v4);

        cmdList.add("-i");
        cmdList.add(v5);

        cmdList.add("-i");
        cmdList.add(v6);

        cmdList.add("-i");
        cmdList.add(v7);

        cmdList.add("-i");
        cmdList.add(v8);

        cmdList.add("-i");
        cmdList.add(v9);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");

        cmdList.add(String.valueOf(dstDuration));


        return doVideoLayout(cmdList,outW,outH);
    }
    public String executeLayoutScale9Video(int outW, int outH,
                                           VideoLayoutParam p1,
                                           VideoLayoutParam p2,
                                           VideoLayoutParam p3,
                                           VideoLayoutParam p4,
                                           VideoLayoutParam p5,
                                           VideoLayoutParam p6,
                                           VideoLayoutParam p7,
                                           VideoLayoutParam p8,
                                           VideoLayoutParam p9) {

        String filter = String.format(Locale.getDefault(), "[0:v] setpts=PTS-STARTPTS,scale=%dx%d,pad=%d:%d:%d:%d:White [in1]; [1:v] setpts=PTS-STARTPTS,scale=%dx%d [in2];[2:v] setpts=PTS-STARTPTS,scale=%dx%d [in3];[3:v] setpts=PTS-STARTPTS,scale=%dx%d [in4]; [4:v] setpts=PTS-STARTPTS,scale=%dx%d [in5]; [5:v] setpts=PTS-STARTPTS,scale=%dx%d [in6]; [6:v] setpts=PTS-STARTPTS,scale=%dx%d [in7];[7:v] setpts=PTS-STARTPTS,scale=%dx%d [in8]; [8:v] setpts=PTS-STARTPTS,scale=%dx%d [in9]; [in1][in2] overlay=x=%d:y=%d [tmp1]; [tmp1][in3] overlay=x=%d:y=%d [tmp2]; [tmp2][in4] overlay=x=%d:y=%d [tmp3];[tmp3][in5] overlay=x=%d:y=%d [tmp4]; [tmp4][in6] overlay=x=%d:y=%d [tmp5]; [tmp5][in7] overlay=x=%d:y=%d [tmp6]; [tmp6][in8] overlay=x=%d:y=%d [tmp7]; [tmp7][in9] overlay=x=%d:y=%d",
                p1.scaleW,p1.scaleH,outW, outH,p1.x,p1.y,p2.scaleW,p2.scaleH,p3.scaleW,p3.scaleH,p4.scaleW,p4.scaleH,p5.scaleW,p5.scaleH,p6.scaleW,p6.scaleH,p7.scaleW,p7.scaleH,p8.scaleW,p8.scaleH,p9.scaleW,p9.scaleH,p2.x,p2.y,p3.x,p3.y,p4.x,p4.y,p5.x,p5.y,p6.x,p6.y,p7.x,p7.y,p8.x,p8.y,p9.x,p9.y);

        float duration=getMaxDuration(Arrays.asList(p1.video,p2.video,p3.video,p4.video,p5.video,p6.video,p7.video,p8.video,p9.video));
        filter+=getAudioCmd();
        List<String> cmdList = new ArrayList<String>();

        if(!isUseSoftDecoder){
            cmdList.add("-vcodec");
            cmdList.add("lansoh264_dec");
        }

        cmdList.add("-i");
        cmdList.add(p1.video);

        cmdList.add("-i");
        cmdList.add(p2.video);

        cmdList.add("-i");
        cmdList.add(p3.video);

        cmdList.add("-i");
        cmdList.add(p4.video);

        cmdList.add("-i");
        cmdList.add(p5.video);

        cmdList.add("-i");
        cmdList.add(p6.video);

        cmdList.add("-i");
        cmdList.add(p7.video);

        cmdList.add("-i");
        cmdList.add(p8.video);

        cmdList.add("-i");
        cmdList.add(p9.video);

        cmdList.add("-filter_complex");
        cmdList.add(filter);

        cmdList.add("-t");
        cmdList.add(String.valueOf(duration));

        return doVideoLayout(cmdList,outW,outH);
    }
    private int audioTrackCnt=0;
    private float getMaxDuration(List<String> array) {
        audioTrackCnt=0;
        float retDuration = 0;
        for (String str : array) {
            MediaInfo info = new MediaInfo(str);
            if (info.prepare() ){
                if(info.isHaveVideo()) {
                    if (info.vDuration > retDuration) {
                        retDuration = info.vDuration;
                    }
                }
                if(info.isHaveAudio()){
                    audioTrackCnt++;
                }
            }
        }
        if(retDuration==0){
            Log.e(TAG,"VideoLayout 没有检测到输入视频的长度, 默认设置为15s");
            retDuration=15;
        }
        return retDuration;
    }
    private String getAudioCmd(){
        String cmd="";
        if(audioTrackCnt>0){
            cmd=";amix=inputs=";
            cmd+=String.valueOf(audioTrackCnt);
        }
        return cmd;
    }

    public String doVideoLayout(List<String> cmdList, int width,int height)
    {
        if(encodeBitRate==0){
            setEncodeBitRate(getSuggestBitRate(width * height));
        }
        return executeAutoSwitch(cmdList);
    }
    public static int getSuggestBitRate(int wxh) {
        if (wxh < 480 * 480) {
            return 1000 * 1024;
        } else if (wxh <= 640 * 480) {
            return 1500 * 1024;
        } else if (wxh <= 800 * 480) {
            return 1800 * 1024;
        } else if (wxh <= 960 * 544) {
            return 2300 * 1024;
        } else if (wxh <= 1280 * 720) {
            return 2800 * 1024;
        } else if (wxh <= 1920 * 1088) {
            return 3000 * 1024;
        } else {
            return 3500 * 1024;
        }
    }
    //-------------------------
    public static void test() {
        long time=System.currentTimeMillis();

        VideoLayout layout=new VideoLayout();
        VideoLayoutParam p1= new VideoLayoutParam();
        p1.video="/sdcard/d3.mp4";
        p1.x=0;
        p1.y=0;
        p1.scaleW=720;
        p1.scaleH=720;

        VideoLayoutParam p2= new VideoLayoutParam();
        p2.video="/sdcard/d2.mp4";
        p2.x=0;
        p2.y=0;
        p2.scaleW=544/2;
        p2.scaleH=960/2;


        String dstPath=layout.executeLayout2Video(720,720,"/sdcard/d1.mp4",0,0,"/sdcard/d2.mp4",0,0);

        Log.i(TAG,"测试video Layout  耗时:"+(System.currentTimeMillis() - time)+ "dst:"+dstPath);
    }
}
