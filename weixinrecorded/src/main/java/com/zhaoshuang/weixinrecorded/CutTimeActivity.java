package com.zhaoshuang.weixinrecorded;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yixia.camera.MediaRecorderBase;
import com.yixia.videoeditor.adapter.UtilityAdapter;

/**
 * Created by zhaoshuang on 2017/9/30.
 */

public class CutTimeActivity extends BaseActivity{

    private MediaPlayer mMediaPlayer;
    private String path;
    private TextureView textureView;
    private int videoWidth;
    private int videoHeight;
    private RelativeLayout rl_video;
    private int videoDuration;
    private LinearLayout ll_thumbnail;
    private ThumbnailView thumbnailView;
    private RelativeLayout rl_close;
    private TextView tv_finish_video;
    private int startTime;
    private int endTime;
    private int windowWidth;
    private int windowHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_cut_time);

        windowWidth = getWindowManager().getDefaultDisplay().getWidth();
        windowHeight = getWindowManager().getDefaultDisplay().getHeight();

        Intent intent = getIntent();
        path = intent.getStringExtra("path");

        initUI();
    }

    private void initUI() {

        rl_close = (RelativeLayout) findViewById(R.id.rl_close);
        tv_finish_video = (TextView) findViewById(R.id.tv_finish_video);
        textureView = (TextureView) findViewById(R.id.textureView);
        rl_video = (RelativeLayout) findViewById(R.id.rl_video);
        ll_thumbnail = (LinearLayout) findViewById(R.id.ll_thumbnail);
        thumbnailView = (ThumbnailView) findViewById(R.id.thumbnailView);

        rl_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        tv_finish_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cutVideo();
            }
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                initMediaPlay(surface);
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        //监听裁剪器滑动
        thumbnailView.setOnScrollBorderListener(new ThumbnailView.OnScrollBorderListener() {
            @Override
            public void OnScrollBorder(float start, float end) {
                changeTime();
            }

            @Override
            public void onScrollStateChange() {
                changeVideoPlay();
            }
        });
    }

    private void cutVideo() {

        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                TextView textView = showProgressDialog();
                textView.setText("视频剪切中");
            }
            @Override
            protected String doInBackground(Void... params) {

                //ffmpeg -ss 00:00:15 -t 00:00:05 -i input.mp4 -vcodec copy -acodec copy output.mp4
                String output = SDKUtil.VIDEO_PATH+"/"+ System.currentTimeMillis()+".mp4";

                int startM = startTime/1000;
                int endM = (endTime-startTime)/1000;

                String startStr;
                String endStr;

                if(startM < 10){
                    startStr = "00:00:0"+startM;
                }else{
                    startStr = "00:00:"+startM;
                }

                if(endM < 10){
                    endStr = "00:00:0"+endM;
                }else{
                    endStr = "00:00:"+endM;
                }

                StringBuilder sb = new StringBuilder("ffmpeg");
                sb.append(" -i");
                sb.append(" "+path);
                sb.append(" -vcodec");
                sb.append(" copy");
                sb.append(" -acode00c");
                sb.append(" copy");
                sb.append(" -ss");
                sb.append(" "+startStr);
                sb.append(" -t");
                sb.append(" "+endStr);
                sb.append(" "+output);
                int i = UtilityAdapter.FFmpegRun("", sb.toString());
                if(i == 0){
                    return output;
                }else{
                    return "";
                }
            }
            @Override
            protected void onPostExecute(String result) {
                closeProgressDialog();
                if(!TextUtils.isEmpty(result)){
                    Toast.makeText(mContext, "剪切成功", Toast.LENGTH_SHORT).show();
                    CutSizeActivity.renameFile(result, path);
                    setResult(RESULT_OK);
                    finish();
                }
            }
        }.execute();
    }

    /**
     * 更改选择的裁剪区间的时间
     */
    private void changeTime(){

        float left = thumbnailView.getLeftInterval();
        float pro1 = left/ll_thumbnail.getWidth();

        startTime = (int) (videoDuration*pro1);

        float right = thumbnailView.getRightInterval();
        float pro2 = right/ll_thumbnail.getWidth();
        endTime = (int) (videoDuration*pro2);
    }

    private void changeVideoPlay(){
        if(mMediaPlayer != null) {
            mMediaPlayer.seekTo(startTime);
        }
    }

    /**
     * 初始化视频播放器
     */
    private void initVideoSize(){

        float ra = videoWidth*1f/videoHeight;

        float widthF = videoWidth*1f/ MediaRecorderBase.VIDEO_HEIGHT;
        float heightF = videoHeight*1f/MediaRecorderBase.VIDEO_WIDTH;
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        layoutParams.width = (int) (windowWidth*widthF);
        layoutParams.height = (int) (layoutParams.width/ra);
        textureView.setLayoutParams(layoutParams);

        //最小剪切时间500毫秒
        int pxWidth = (int) (500f/videoDuration*thumbnailView.getWidth());
        thumbnailView.setMinInterval(pxWidth);
    }

    /**
     * 初始化缩略图
     */
    private void initThumbs(){

        final int frame = 15;
        final int frameTime = videoDuration/frame*1000;

        int thumbnailWidth =  ll_thumbnail.getWidth()/frame;
        for (int x=0; x<frame; x++){
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(thumbnailWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setBackgroundColor(Color.parseColor("#666666"));
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            ll_thumbnail.addView(imageView);
        }

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                MediaMetadataRetriever mediaMetadata= new MediaMetadataRetriever();
                mediaMetadata.setDataSource(mContext, Uri.parse(path));
                for (int x=0; x<frame; x++){
                    Bitmap bitmap = mediaMetadata.getFrameAtTime(frameTime*x, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    Message msg = myHandler.obtainMessage();
                    msg.obj = bitmap;
                    msg.arg1 = x;
                    myHandler.sendMessage(msg);
                }
                mediaMetadata.release();
                return true;
            }
            @Override
            protected void onPostExecute(Boolean result) {
            }
        }.execute();
    }

    private Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            ImageView imageView = (ImageView) ll_thumbnail.getChildAt(msg.arg1);
            Bitmap bitmap = (Bitmap) msg.obj;
            if(imageView!=null && bitmap!=null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    };

    private void initMediaPlay(SurfaceTexture surface){

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.setSurface(new Surface(surface));
            mMediaPlayer.setLooping(true);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();

                    videoDuration = mMediaPlayer.getDuration();
                    videoWidth = mMediaPlayer.getVideoWidth();
                    videoHeight = mMediaPlayer.getVideoHeight();
                    initVideoSize();
                    initThumbs();
                }
            });
            mMediaPlayer.prepareAsync();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
