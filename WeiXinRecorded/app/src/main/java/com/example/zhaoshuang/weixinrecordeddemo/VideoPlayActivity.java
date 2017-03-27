package com.example.zhaoshuang.weixinrecordeddemo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.yixia.camera.MediaRecorderBase;

/**
 * Created by zhaoshuang on 17/2/24.
 */

public class VideoPlayActivity extends BaseActivity {

    private MyVideoView vv_play;
    private int windowWidth;
    private int windowHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video_play);

        vv_play = (MyVideoView) findViewById(R.id.vv_play);

        windowWidth = getWindowManager().getDefaultDisplay().getWidth();
        windowHeight = getWindowManager().getDefaultDisplay().getHeight();

        Intent intent = getIntent();
        String path = intent.getStringExtra("path");

        vv_play.setVideoPath(path);
        vv_play.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                vv_play.setLooping(true);
                vv_play.start();

                float widthF = vv_play.getVideoWidth()*1f/ MediaRecorderBase.VIDEO_HEIGHT;
                float heightF = vv_play.getVideoHeight()*1f/MediaRecorderBase.VIDEO_WIDTH;
                ViewGroup.LayoutParams layoutParams = vv_play.getLayoutParams();
                layoutParams.width = (int) (windowWidth *widthF);
                layoutParams.height = (int) (windowHeight *heightF);
                vv_play.setLayoutParams(layoutParams);
            }
        });
    }
}
