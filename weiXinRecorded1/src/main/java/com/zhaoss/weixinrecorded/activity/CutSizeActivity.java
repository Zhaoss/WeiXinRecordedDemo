package com.zhaoss.weixinrecorded.activity;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoEditor;
import com.lansosdk.videoeditor.onVideoEditorProgressListener;
import com.zhaoss.weixinrecorded.R;
import com.zhaoss.weixinrecorded.util.MyVideoEditor;
import com.zhaoss.weixinrecorded.util.RxJavaUtil;
import com.zhaoss.weixinrecorded.util.Utils;
import com.zhaoss.weixinrecorded.view.CutView;

/**
 * Created by zhaoshuang on 17/3/21.
 */

public class CutSizeActivity extends BaseActivity implements View.OnClickListener{

    private TextureView textureView;
    private TextView editorTextView;
    private CutView cv_video;

    private String path;
    private MediaInfo mMediaInfo;
    private MyVideoEditor myVideoEditor = new MyVideoEditor();
    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_cut_size);

        initUI();
        initData();
        initVideoSize();
    }

    private void initUI() {

        textureView = findViewById(R.id.textureView);
        cv_video = findViewById(R.id.cv_video);
        RelativeLayout rl_close = findViewById(R.id.rl_close);
        TextView rl_finish = findViewById(R.id.rl_finish);

        rl_close.setOnClickListener(this);
        rl_finish.setOnClickListener(this);
    }

    private void initData(){

        Intent intent = getIntent();
        path = intent.getStringExtra(RecordedActivity.INTENT_PATH);

        textureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                cv_video.setMargin(textureView.getLeft(), textureView.getTop(), textureView.getRight()-textureView.getWidth(), textureView.getBottom()-textureView.getHeight());
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

        myVideoEditor.setOnProgessListener(new onVideoEditorProgressListener() {
            @Override
            public void onProgress(VideoEditor v, int percent) {
                editorTextView.setText("视频编辑中"+percent+"%");
            }
        });
    }

    private void initVideoSize(){

        mMediaInfo = new MediaInfo(path);
        mMediaInfo.prepare();

        int windowWidth = Utils.getWindowWidth(mContext);
        float ra = mMediaInfo.getWidth()*1f/mMediaInfo.getHeight();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) textureView.getLayoutParams();
        layoutParams.width = windowWidth-layoutParams.leftMargin-layoutParams.rightMargin;
        layoutParams.height = (int) (layoutParams.width/ra);
        textureView.setLayoutParams(layoutParams);
    }

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
                }
            });
            mMediaPlayer.prepareAsync();
        }catch (Exception e){
            e.printStackTrace();
        }
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
        int cropWidth = (int) (mMediaInfo.getWidth()*(rightPro-leftPro));
        int cropHeight = (int) (mMediaInfo.getHeight()*(bottomPro-topPro));
        int x = (int) (leftPro*mMediaInfo.getWidth());
        int y = (int) (topPro*mMediaInfo.getHeight());

        return  myVideoEditor.executeCropVideoFrame(path, cropWidth, cropHeight, x, y);
    }

    @Override
    public void onClick(View v) {

        int i = v.getId();
        if (i == R.id.rl_close) {
            finish();

        } else if (i == R.id.rl_finish) {
            editorTextView = showProgressDialog();
            RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<String>() {
                @Override
                public String doInBackground() throws Throwable {
                    mMediaPlayer.stop();
                    return editVideo();
                }
                @Override
                public void onFinish(String result) {
                    closeProgressDialog();
                    if (!TextUtils.isEmpty(result)) {
                        Intent intent = new Intent();
                        intent.putExtra(RecordedActivity.INTENT_PATH, result);
                        setResult(RESULT_OK, intent);
                        finish();
                    }else{
                        Toast.makeText(getApplicationContext(), "视频编辑失败", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onError(Throwable e) {
                    closeProgressDialog();
                    Toast.makeText(getApplicationContext(), "视频编辑失败", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }
}
