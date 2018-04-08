package com.zhaoshuang.weixinrecorded;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.yixia.camera.MediaRecorderBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhaoshuang on 17/2/16.
 * 触摸对焦SurfaceView
 */

public class FocusSurfaceView extends SurfaceView {

    private ValueAnimator va;
    private String focusMode;
    private ImageView imageView;
    private MediaRecorderBase mediaRecorderBase;

    public FocusSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FocusSurfaceView(Context context) {
        super(context);
    }

    /**
     * 设置触摸对焦
     */
    public void setTouchFocus(MediaRecorderBase mediaRecorderBase){

        this.mediaRecorderBase = mediaRecorderBase;
    }

    private void focusOnTouch(int x, int y, Camera camera) {

        Rect rect = new Rect(x - 100, y - 100, x + 100, y + 100);
        int left = rect.left * 2000 / getWidth() - 1000;
        int top = rect.top * 2000 / getHeight() - 1000;
        int right = rect.right * 2000 / getWidth() - 1000;
        int bottom = rect.bottom * 2000 / getHeight() - 1000;
        // 如果超出了(-1000,1000)到(1000, 1000)的范围，则会导致相机崩溃
        left = left < -1000 ? -1000 : left;
        top = top < -1000 ? -1000 : top;
        right = right > 1000 ? 1000 : right;
        bottom = bottom > 1000 ? 1000 : bottom;
        try {
            focusOnRect(new Rect(left, top, right, bottom), camera);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected void focusOnRect(Rect rect, Camera camera) {

        if (TextUtils.isEmpty(focusMode)) {
            focusMode = camera.getParameters().getFocusMode();
        }
        Camera.Parameters parameters = camera.getParameters(); // 先获取当前相机的参数配置对象
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); // 设置聚焦模式
        if (parameters.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(rect, 1000));
            parameters.setFocusAreas(focusAreas);
        }
        camera.cancelAutoFocus(); // 先要取消掉进程中所有的聚焦功能
        camera.setParameters(parameters); // 一定要记得把相应参数设置给相机
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Camera.Parameters parame = camera.getParameters();
                parame.setFocusMode(focusMode);
                camera.setParameters(parame);
            }
        });
    }

    float downX;
    float downY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                //判断是否支持对焦模式
                if(mediaRecorderBase!=null && mediaRecorderBase.getCamera()!=null) {
                    List<String> focusModes = mediaRecorderBase.getCamera().getParameters().getSupportedFocusModes();
                    if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        focusOnTouch((int) event.getRawX(), (int) event.getRawY(), mediaRecorderBase.getCamera());
                        addFocusToWindow();
                    }
                }
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    private void addFocusToWindow(){

        if(va == null) {
            imageView = new ImageView(getContext());
            imageView.setImageResource(R.mipmap.video_focus);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.measure(0, 0);
            imageView.setX(downX - imageView.getMeasuredWidth() / 2);
            imageView.setY(downY - imageView.getMeasuredHeight() / 2);
            final ViewGroup parent = (ViewGroup) getParent();
            parent.addView(imageView);

            va = ValueAnimator.ofFloat(0, 1).setDuration(500);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if(imageView != null) {
                        float value = (float) animation.getAnimatedValue();
                        if (value <= 0.5f) {
                            imageView.setScaleX(1 + value);
                            imageView.setScaleY(1 + value);
                        } else {
                            imageView.setScaleX(2 - value);
                            imageView.setScaleY(2 - value);
                        }
                    }
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if(imageView != null) {
                        parent.removeView(imageView);
                        va = null;
                    }
                }
            });
            va.start();
        }
    }
}
