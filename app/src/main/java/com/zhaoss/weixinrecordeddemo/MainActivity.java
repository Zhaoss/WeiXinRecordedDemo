package com.zhaoss.weixinrecordeddemo;

import android.Manifest;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;
import com.zhaoss.weixinrecorded.activity.RecordedActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tv_path;
    private TextureView textureView;
    private MediaPlayer mMediaPlayer;
    private ImageView iv_photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        AndPermission.with(this).permission(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .requestCode(0).callback(new PermissionListener() {
            @Override
            public void onSucceed(int requestCode, @NonNull List<String> grantPermissions) {
            }
            @Override
            public void onFailed(int requestCode, @NonNull List<String> deniedPermissions) {
            }
        }).start();

        tv_path = findViewById(R.id.tv_path);
        textureView = findViewById(R.id.textureView);
        iv_photo = findViewById(R.id.iv_photo);
    }

    public void startRecord(View v){
        Intent intent = new Intent(this, RecordedActivity.class);
        startActivityForResult(intent, 1);

        if(mMediaPlayer != null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode==RESULT_OK && data!=null){
            if(requestCode == 1){
                textureView.setVisibility(View.GONE);
                iv_photo.setVisibility(View.GONE);
                int dataType = data.getIntExtra(RecordedActivity.INTENT_DATA_TYPE, RecordedActivity.RESULT_TYPE_VIDEO);
                if(dataType == RecordedActivity.RESULT_TYPE_VIDEO){
                    String videoPath = data.getStringExtra(RecordedActivity.INTENT_PATH);
                    tv_path.setText("视频地址: "+videoPath);
                    textureView.setVisibility(View.VISIBLE);
                    playVideo(videoPath);
                }else if(dataType == RecordedActivity.RESULT_TYPE_PHOTO){
                    String photoPath = data.getStringExtra(RecordedActivity.INTENT_PATH);
                    tv_path.setText("图片地址: "+photoPath);
                    iv_photo.setVisibility(View.VISIBLE);
                    iv_photo.setImageBitmap(BitmapFactory.decodeFile(photoPath));
                }
            }
        }
    }

    private void playVideo(String videoPath){

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(videoPath);
            mMediaPlayer.setSurface(new Surface(textureView.getSurfaceTexture()));
            mMediaPlayer.setLooping(true);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();

                    float ratio = mp.getVideoWidth()*1f/mp.getVideoHeight();
                    int width = textureView.getWidth();
                    ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
                    layoutParams.height = (int) (width/ratio);
                    textureView.setLayoutParams(layoutParams);
                }
            });
            mMediaPlayer.prepareAsync();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}