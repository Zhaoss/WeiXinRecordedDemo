package com.zhaoss.weixinrecorded.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.VideoEditor;
import com.lansosdk.videoeditor.onVideoEditorProgressListener;
import com.zhaoss.weixinrecorded.R;
import com.zhaoss.weixinrecorded.util.MyVideoEditor;
import com.zhaoss.weixinrecorded.util.RxJavaUtil;
import com.zhaoss.weixinrecorded.util.Utils;
import com.zhaoss.weixinrecorded.view.TouchView;
import com.zhaoss.weixinrecorded.view.TuyaView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by zhaoshuang on 17/2/21.
 * 视频编辑界面
 */

public class EditVideoActivity extends BaseActivity {

    private int[] drawableBg = new int[]{R.drawable.color1, R.drawable.color2, R.drawable.color3, R.drawable.color4, R.drawable.color5};
    private int[] colors = new int[]{R.color.color1, R.color.color2, R.color.color3, R.color.color4, R.color.color5};
    private int[] expressions = new int[]{R.mipmap.expression1, R.mipmap.expression2, R.mipmap.expression3, R.mipmap.expression4,
            R.mipmap.expression5, R.mipmap.expression6, R.mipmap.expression7, R.mipmap.expression8};

    private TextureView textureView;
    private LinearLayout ll_color;
    private RelativeLayout rl_tuya;
    private RelativeLayout rl_close;
    private RelativeLayout rl_title;
    private RelativeLayout rl_bottom;
    private TextView tv_hint_delete;
    private RelativeLayout rl_expression;
    private RelativeLayout rl_touch_view;
    private RelativeLayout rl_edit_text;
    private TuyaView tv_video;
    private ImageView iv_pen;
    private ImageView iv_icon;
    private EditText et_tag;
    private TextView tv_tag;
    private ImageView iv_speed;
    private LinearLayout ll_progress;
    private SeekBar sb_speed;
    private TextView tv_speed;
    private SurfaceTexture surfaceTexture;
    private RelativeLayout rl_cut_size;
    private RelativeLayout rl_cut_time;
    private RelativeLayout rl_back;
    private RelativeLayout rl_speed;
    private TextView tv_finish_video;
    private TextView tv_finish;
    private TextView tv_close;
    private RelativeLayout rl_pen;
    private RelativeLayout rl_icon;
    private RelativeLayout rl_text;

    private int currentColorPosition;
    private InputMethodManager manager;
    private String path;
    private int dp100;
    private float videoSpeed = 1;
    private MediaInfo mMediaInfo;
    private MyVideoEditor myVideoEditor = new MyVideoEditor();
    private TextView editorTextView;
    private int windowWidth;
    private int windowHeight;

    private int executeCount;//总编译次数
    private float executeProgress;//编译进度
    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_edit_video);

        manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        dp100 = (int) getResources().getDimension(R.dimen.dp100);
        windowWidth = Utils.getWindowWidth(mContext);
        windowHeight = Utils.getWindowHeight(mContext);

        initUI();
        initData();
        initVideoSize();
    }

    private void initUI() {

        textureView = findViewById(R.id.textureView);
        rl_pen = findViewById(R.id.rl_pen);
        rl_icon = findViewById(R.id.rl_icon);
        rl_text = findViewById(R.id.rl_text);
        ll_color = findViewById(R.id.ll_color);
        tv_video = findViewById(R.id.tv_video);
        rl_expression = findViewById(R.id.rl_expression);
        rl_touch_view = findViewById(R.id.rl_touch_view);
        tv_close = findViewById(R.id.tv_close);
        tv_finish = findViewById(R.id.tv_finish);
        rl_edit_text = findViewById(R.id.rl_edit_text);
        et_tag = findViewById(R.id.et_tag);
        tv_tag = findViewById(R.id.tv_tag);
        tv_finish_video = findViewById(R.id.tv_finish_video);
        iv_pen = findViewById(R.id.iv_pen);
        iv_icon = findViewById(R.id.iv_icon);
        iv_speed = findViewById(R.id.iv_speed);
        rl_tuya = findViewById(R.id.rl_tuya);
        rl_close = findViewById(R.id.rl_close);
        rl_title = findViewById(R.id.rl_title);
        rl_bottom = findViewById(R.id.rl_bottom);
        tv_hint_delete = findViewById(R.id.tv_hint_delete);
        rl_speed = findViewById(R.id.rl_speed);
        ll_progress = findViewById(R.id.ll_progress);
        sb_speed = findViewById(R.id.sb_speed);
        tv_speed = findViewById(R.id.tv_speed);
        rl_cut_size = findViewById(R.id.rl_cut_size);
        rl_cut_time = findViewById(R.id.rl_cut_time);
        rl_back = findViewById(R.id.rl_back);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                surfaceTexture = surface;
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

        rl_pen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePenState(!(ll_color.getVisibility() == View.VISIBLE));
                changeIconState(false);
                changeTextState(false);
                changeSpeedState(false);
            }
        });

        rl_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeIconState(!(rl_expression.getVisibility() == View.VISIBLE));
                changePenState(false);
                changeTextState(false);
                changeSpeedState(false);
            }
        });

        rl_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTextState(!(rl_edit_text.getVisibility() == View.VISIBLE));
                changePenState(false);
                changeIconState(false);
                changeSpeedState(false);
            }
        });

        rl_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_video.backPath();
            }
        });

        tv_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTextState(!(rl_edit_text.getVisibility() == View.VISIBLE));
            }
        });

        tv_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTextState(!(rl_edit_text.getVisibility() == View.VISIBLE));
                if (et_tag.getText().length() > 0) {
                    addTextToWindow();
                }
            }
        });

        tv_finish_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishVideo();
            }
        });

        rl_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        rl_speed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTextState(false);
                changePenState(false);
                changeIconState(false);
                changeSpeedState(!(ll_progress.getVisibility() == View.VISIBLE));
            }
        });

        rl_cut_size.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.release();
                }
                Intent intent = new Intent(mContext, CutSizeActivity.class);
                intent.putExtra(RecordedActivity.INTENT_PATH, path);
                startActivityForResult(intent, 1);
            }
        });

        rl_cut_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.release();
                }
                Intent intent = new Intent(mContext, CutTimeActivity.class);
                intent.putExtra(RecordedActivity.INTENT_PATH, path);
                startActivityForResult(intent, 2);
            }
        });

        initColors();
        initExpression();
        initSpeed();

        et_tag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                tv_tag.setText(s.toString());
            }
        });
    }

    private void initData() {

        Intent intent = getIntent();
        path = intent.getStringExtra(RecordedActivity.INTENT_PATH);

        //当进行涂鸦操作时, 隐藏标题栏和底部工具栏
        tv_video.setOnTouchListener(new TuyaView.OnTouchListener() {
            @Override
            public void onDown() {
                changeMode(false);
            }

            @Override
            public void onUp() {
                changeMode(true);
            }
        });

        myVideoEditor.setOnProgessListener(new onVideoEditorProgressListener() {
            @Override
            public void onProgress(VideoEditor v, int percent) {

                float stepPro = 100f/executeCount;
                int temp = (int) (percent/100f*stepPro);
                editorTextView.setText("视频编辑中"+(int)(executeProgress+temp)+"%");
                if(percent==100){
                    executeProgress += stepPro;
                }
            }
        });
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

    private void initVideoSize(){

        mMediaInfo = new MediaInfo(path);
        mMediaInfo.prepare();

        float ra = mMediaInfo.getWidth() * 1f / mMediaInfo.getHeight();
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        layoutParams.width = windowWidth;
        layoutParams.height = (int) (layoutParams.width / ra);
        textureView.setLayoutParams(layoutParams);

        ViewGroup.LayoutParams layoutParams1 = rl_tuya.getLayoutParams();
        layoutParams1.width = layoutParams.width;
        layoutParams1.height = layoutParams.height;
        rl_tuya.setLayoutParams(layoutParams1);
    }

    private void initSpeed() {

        sb_speed.setMax(200);
        sb_speed.setProgress(100);
        sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 50) {
                    progress = 50;
                    sb_speed.setProgress(50);
                }
                videoSpeed = progress / 100f;
                tv_speed.setText(videoSpeed + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void finishVideo() {

        final boolean isPen = tv_video.getPathSum() != 0;
        final boolean isImage = rl_touch_view.getChildCount() != 0;
        final boolean isSpeed = videoSpeed != 1;

        if(isPen || isImage){
            executeCount++;
        }
        if(isSpeed){
            executeCount++;
        }

        mMediaPlayer.stop();
        editorTextView = showProgressDialog();
        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<String>() {
            @Override
            public String doInBackground() throws Throwable {
                String videoPath = path;
                if(isPen || isImage){
                    videoPath = mergeImage(path);
                }
                if (isSpeed) {
                    videoPath = myVideoEditor.executeAdjustVideoSpeed2(path, videoSpeed);
                }
                return videoPath;
            }
            @Override
            public void onFinish(String result) {
                closeProgressDialog();
                if (!TextUtils.isEmpty(result)) {
                    Intent intent = new Intent();
                    intent.putExtra(RecordedActivity.INTENT_PATH, result);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
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

    //更改界面模式
    private void changeMode(boolean flag) {
        if (flag) {
            rl_title.setVisibility(View.VISIBLE);
            rl_bottom.setVisibility(View.VISIBLE);
        } else {
            rl_title.setVisibility(View.GONE);
            rl_bottom.setVisibility(View.GONE);
        }
    }

    /**
     * 初始化表情
     */
    private void initExpression() {

        int dp80 = (int) getResources().getDimension(R.dimen.dp80);
        int dp10 = (int) getResources().getDimension(R.dimen.dp10);
        for (int x = 0; x < expressions.length; x++) {
            ImageView imageView = new ImageView(this);
            imageView.setPadding(dp10, dp10, dp10, dp10);
            final int result = expressions[x];
            imageView.setImageResource(result);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(windowWidth / 4, dp80));
            imageView.setX(x % 4 * windowWidth / 4);
            imageView.setY(x / 4 * dp80);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rl_expression.setVisibility(View.GONE);
                    iv_icon.setImageResource(R.mipmap.icon);
                    addExpressionToWindow(result);
                }
            });
            rl_expression.addView(imageView);
        }
    }

    /**
     * 添加表情到界面上
     */
    private void addExpressionToWindow(int result) {

        TouchView touchView = new TouchView(this);
        touchView.setBackgroundResource(result);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(dp100, dp100);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        touchView.setLayoutParams(layoutParams);

        touchView.setLimitsX(0, windowWidth);
        touchView.setLimitsY(0, windowHeight - dp100 / 2);
        touchView.setOnLimitsListener(new TouchView.OnLimitsListener() {
            @Override
            public void OnOutLimits(float x, float y) {
                tv_hint_delete.setTextColor(Color.RED);
            }

            @Override
            public void OnInnerLimits(float x, float y) {
                tv_hint_delete.setTextColor(Color.WHITE);
            }
        });
        touchView.setOnTouchListener(new TouchView.OnTouchListener() {
            @Override
            public void onDown(TouchView view, MotionEvent event) {
                tv_hint_delete.setVisibility(View.VISIBLE);
                changeMode(false);
            }

            @Override
            public void onMove(TouchView view, MotionEvent event) {

            }

            @Override
            public void onUp(TouchView view, MotionEvent event) {
                tv_hint_delete.setVisibility(View.GONE);
                changeMode(true);
                if (view.isOutLimits()) {
                    rl_touch_view.removeView(view);
                }
            }
        });

        rl_touch_view.addView(touchView);
    }

    /**
     * 添加文字到界面上
     */
    private void addTextToWindow() {

        TouchView touchView = new TouchView(getApplicationContext());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(tv_tag.getWidth(), tv_tag.getHeight());
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        touchView.setLayoutParams(layoutParams);
        Bitmap bitmap = Bitmap.createBitmap(tv_tag.getWidth(), tv_tag.getHeight(), Bitmap.Config.ARGB_8888);
        tv_tag.draw(new Canvas(bitmap));
        touchView.setBackground(new BitmapDrawable(bitmap));

        touchView.setLimitsX(0, windowWidth);
        touchView.setLimitsY(0, windowHeight - dp100 / 2);
        touchView.setOnLimitsListener(new TouchView.OnLimitsListener() {
            @Override
            public void OnOutLimits(float x, float y) {
                tv_hint_delete.setTextColor(Color.RED);
            }

            @Override
            public void OnInnerLimits(float x, float y) {
                tv_hint_delete.setTextColor(Color.WHITE);
            }
        });
        touchView.setOnTouchListener(new TouchView.OnTouchListener() {
            @Override
            public void onDown(TouchView view, MotionEvent event) {
                tv_hint_delete.setVisibility(View.VISIBLE);
                changeMode(false);
            }

            @Override
            public void onMove(TouchView view, MotionEvent event) {

            }

            @Override
            public void onUp(TouchView view, MotionEvent event) {
                tv_hint_delete.setVisibility(View.GONE);
                changeMode(true);
                if (view.isOutLimits()) {
                    rl_touch_view.removeView(view);
                }
            }
        });

        rl_touch_view.addView(touchView);

        et_tag.setText("");
        tv_tag.setText("");
    }

    /**
     * 初始化底部颜色选择器
     */
    private void initColors() {

        int dp20 = (int) getResources().getDimension(R.dimen.dp20);
        int dp25 = (int) getResources().getDimension(R.dimen.dp25);

        for (int x = 0; x < drawableBg.length; x++) {
            RelativeLayout relativeLayout = new RelativeLayout(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            layoutParams.weight = 1;
            relativeLayout.setLayoutParams(layoutParams);

            View view = new View(this);
            view.setBackgroundDrawable(getResources().getDrawable(drawableBg[x]));
            RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(dp20, dp20);
            layoutParams1.addRule(RelativeLayout.CENTER_IN_PARENT);
            view.setLayoutParams(layoutParams1);
            relativeLayout.addView(view);

            final View view2 = new View(this);
            view2.setBackgroundResource(R.mipmap.color_click);
            RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(dp25, dp25);
            layoutParams2.addRule(RelativeLayout.CENTER_IN_PARENT);
            view2.setLayoutParams(layoutParams2);
            if (x != 0) {
                view2.setVisibility(View.GONE);
            }
            relativeLayout.addView(view2);

            final int position = x;
            relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentColorPosition != position) {
                        view2.setVisibility(View.VISIBLE);
                        ViewGroup parent = (ViewGroup) v.getParent();
                        ViewGroup childView = (ViewGroup) parent.getChildAt(currentColorPosition);
                        childView.getChildAt(1).setVisibility(View.GONE);
                        tv_video.setNewPaintColor(getResources().getColor(colors[position]));
                        currentColorPosition = position;
                    }
                }
            });

            ll_color.addView(relativeLayout, x);
        }
    }

    boolean isFirstShowEditText;

    /**
     * 弹出键盘
     */
    public void popupEditText() {

        isFirstShowEditText = true;
        et_tag.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isFirstShowEditText) {
                    isFirstShowEditText = false;
                    et_tag.setFocusable(true);
                    et_tag.setFocusableInTouchMode(true);
                    et_tag.requestFocus();
                    isFirstShowEditText = !manager.showSoftInput(et_tag, 0);
                }
            }
        });
    }

    /**
     * 执行文字编辑区域动画
     */
    private void startAnim(float start, float end, AnimatorListenerAdapter listenerAdapter) {

        ValueAnimator va = ValueAnimator.ofFloat(start, end).setDuration(200);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                rl_edit_text.setY(value);
            }
        });
        if (listenerAdapter != null) va.addListener(listenerAdapter);
        va.start();
    }

    /**
     * 更改画笔状态的界面
     */
    private void changePenState(boolean flag) {

        if (flag) {
            tv_video.setDrawMode(flag);
            tv_video.setNewPaintColor(getResources().getColor(colors[currentColorPosition]));
            iv_pen.setImageResource(R.mipmap.pen_click);
            ll_color.setVisibility(View.VISIBLE);
        } else {
            tv_video.setDrawMode(flag);
            ll_color.setVisibility(View.GONE);
            iv_pen.setImageResource(R.mipmap.pen);
        }
    }

    /**
     * 更改表情状态的界面
     */
    private void changeIconState(boolean flag) {

        if (flag) {
            iv_icon.setImageResource(R.mipmap.icon_click);
            rl_expression.setVisibility(View.VISIBLE);
        } else {
            rl_expression.setVisibility(View.GONE);
            iv_icon.setImageResource(R.mipmap.icon);
        }
    }

    /**
     * 更改文字输入状态的界面
     */
    private void changeTextState(boolean flag) {

        if (flag) {
            rl_edit_text.setY(windowHeight);
            rl_edit_text.setVisibility(View.VISIBLE);
            startAnim(rl_edit_text.getY(), 0, null);
            popupEditText();
        } else {
            manager.hideSoftInputFromWindow(et_tag.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            startAnim(0, windowHeight, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    rl_edit_text.setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * 更改视频加速
     */
    private void changeSpeedState(boolean flag) {

        if (flag) {
            ll_progress.setVisibility(View.VISIBLE);
            iv_speed.setImageResource(R.mipmap.speed_click);
        } else {
            ll_progress.setVisibility(View.GONE);
            iv_speed.setImageResource(R.mipmap.speed);
        }
    }

    /**
     * 合成图片到视频里
     */
    private String mergeImage(String path)throws IOException {

        //得到涂鸦view的bitmap图片
        Bitmap bitmap = Bitmap.createBitmap(rl_tuya.getWidth(), rl_tuya.getHeight(), Bitmap.Config.ARGB_8888);
        rl_tuya.draw(new Canvas(bitmap));
        //这步是根据视频尺寸来调整图片宽高,和视频保持一致
        Matrix matrix = new Matrix();
        matrix.postScale(mMediaInfo.getWidth() * 1f / bitmap.getWidth(), mMediaInfo.getHeight() * 1f / bitmap.getHeight());
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        String imagePath =  LanSongFileUtil.DEFAULT_DIR+System.currentTimeMillis()+".png";
        FileOutputStream fos = new FileOutputStream(new File(imagePath));
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

        return myVideoEditor.executeOverLayVideoFrame(path, imagePath, 0, 0);
    }


    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        if(rl_edit_text.getVisibility() == View.VISIBLE){
            changeTextState(false);
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mMediaPlayer != null){
            mMediaPlayer.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mMediaPlayer != null){
            initMediaPlay(surfaceTexture);
            initVideoSize();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            path = data.getStringExtra(RecordedActivity.INTENT_PATH);
        }
    }
}
