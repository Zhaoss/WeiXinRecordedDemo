package com.example.zhaoshuang.weixinrecordeddemo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * Created by zhaoshuang on 17/2/24.
 */

public class VideoPlayActivity extends BaseActivity {

    private MyVideoView vv_play;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video_play);

        vv_play = (MyVideoView) findViewById(R.id.vv_play);

        Intent intent = getIntent();
        String path = intent.getStringExtra("path");

        vv_play.setVideoPath(path);
        vv_play.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                vv_play.setLooping(true);
                vv_play.start();
            }
        });
    }
}
