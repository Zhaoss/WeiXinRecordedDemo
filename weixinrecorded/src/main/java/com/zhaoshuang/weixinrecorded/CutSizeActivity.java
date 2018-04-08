package com.zhaoshuang.weixinrecorded;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yixia.camera.MediaRecorderBase;
import com.yixia.videoeditor.adapter.UtilityAdapter;

import java.io.File;
import java.util.Locale;

/**
 * Created by zhaoshuang on 17/3/21.
 */

public class CutSizeActivity extends BaseActivity implements View.OnClickListener{

    private MyVideoView vv_play;
    private String path;
    private CutView cv_video;
    private int windowWidth;
    private int windowHeight;
    private int dp50;
    private int videoWidth;
    private int videoHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_cut_size);

        windowWidth = getWindowManager().getDefaultDisplay().getWidth();
        windowHeight = getWindowManager().getDefaultDisplay().getHeight();
        dp50 = (int) getResources().getDimension(R.dimen.dp50);

        initUI();

        Intent intent = getIntent();
        path = intent.getStringExtra("path");
        vv_play.setVideoPath(path);
        vv_play.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                vv_play.setLooping(true);
                vv_play.start();

                videoWidth = vv_play.getVideoWidth();
                videoHeight = vv_play.getVideoHeight();

                float ra = videoWidth*1f/videoHeight;

                float widthF = videoWidth*1f/ MediaRecorderBase.VIDEO_HEIGHT;
                float heightF = videoHeight*1f/MediaRecorderBase.VIDEO_WIDTH;
                ViewGroup.LayoutParams layoutParams = vv_play.getLayoutParams();
                layoutParams.width = (int) (windowWidth*widthF);
                layoutParams.height = (int) (layoutParams.width/ra);
                vv_play.setLayoutParams(layoutParams);
            }
        });

        vv_play.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                cv_video.setMargin(vv_play.getLeft(), vv_play.getTop(), windowWidth-vv_play.getRight(), windowHeight-vv_play.getBottom()-dp50);
            }
        });
    }

    private void initUI() {

        vv_play = (MyVideoView) findViewById(R.id.vv_play);
        cv_video = (CutView) findViewById(R.id.cv_video);
        RelativeLayout rl_close = (RelativeLayout) findViewById(R.id.rl_close);
        TextView rl_finish = (TextView) findViewById(R.id.rl_finish);

        rl_close.setOnClickListener(this);
        rl_finish.setOnClickListener(this);
    }

    /**
     * 裁剪视频大小
     */
    private String cutVideo(String path, int cropWidth, int cropHeight, int x, int y){

        String outPut = SDKUtil.VIDEO_PATH+"/cutVideo.mp4";

        //./ffmpeg -i 2x.mp4 -filter_complex "[0:v]setpts=0.5*PTS[v];[0:a]atempo=2.0[a]" -map "[v]" -map "[a]" output3.mp4
        String filter = String.format(Locale.getDefault(), "crop=%d:%d:%d:%d", cropWidth, cropHeight, x, y);
        StringBuilder sb = new StringBuilder("ffmpeg");
        sb.append(" -i");
        sb.append(" "+path);
        sb.append(" -vf");
        sb.append(" "+filter);
        sb.append(" -acodec");
        sb.append(" copy");
        sb.append(" -b:v");
        int rate = (int) (MediaRecorderBase.VIDEO_BITRATE_HIGH*1.5f);
        sb.append(" "+rate+"k");
        sb.append(" -y");
        sb.append(" "+outPut);

        int i = UtilityAdapter.FFmpegRun("", sb.toString());
        if(i == 0){
            return outPut;
        }else{
            return "";
        }
    }

    /**
     * 更改file名字
     */
    public static boolean renameFile(String filePath, String rename){

        File file = new File(filePath);
        if(file.exists()){
            File refile = new File(rename);
            if(refile.exists()) {
                refile.delete();
            }
            return file.renameTo(refile);
        }
        return false;
    }

    private String editVideo(){

        //得到裁剪后的margin值
        float[] cutArr = cv_video.getCutArr();
        float left = cutArr[0];
        float top = cutArr[1];
        float right = cutArr[2];
        float bottom = cutArr[3];
        int cutWidth = cv_video.getRectWidth();
        int cutHeight= cv_video.getRectHeight();

        //计算宽高缩放比
        float leftPro = left/cutWidth;
        float topPro = top/cutHeight;
        float rightPro = right/cutWidth;
        float bottomPro = bottom/cutHeight;

        //得到裁剪位置
        int cropWidth = (int) (videoWidth*(rightPro-leftPro));
        int cropHeight = (int) (videoHeight*(bottomPro-topPro));
        int x = (int) (leftPro*videoWidth);
        int y = (int) (topPro*videoHeight);

        return cutVideo(path, cropWidth, cropHeight, x, y);
    }

    @Override
    public void onClick(View v) {

        int i = v.getId();
        if (i == R.id.rl_close) {
            finish();

        } else if (i == R.id.rl_finish) {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected void onPreExecute() {
                    showProgressDialog();
                }

                @Override
                protected String doInBackground(Void... params) {
                    return editVideo();
                }

                @Override
                protected void onPostExecute(String result) {
                    closeProgressDialog();
                    if (!TextUtils.isEmpty(result)) {
                        renameFile(result, path);
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            }.execute();

        }
    }
}
