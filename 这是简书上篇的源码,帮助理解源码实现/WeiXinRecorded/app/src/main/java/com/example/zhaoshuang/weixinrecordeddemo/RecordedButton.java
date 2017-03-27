package com.example.zhaoshuang.weixinrecordeddemo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * 拍摄button的自定义view
 * Created by zhaoshuang on 17/2/8.
 */

public class RecordedButton extends View {

    private int measuredWidth = -1;
    private Paint paint;
    private int colorGray;
    private float radius1;
    private float radius2;
    private float zoom = 0.7f;//初始化缩放比例
    private int dp5;
    private Paint paintProgress;
    private int colorBlue;
    private float girth;
    private RectF oval;
    private int max;
    private OnGestureListener onGestureListener;
    private int animTime = 200;
    private float downX;
    private float downY;
    private boolean closeMode = true;

    public RecordedButton(Context context) {
        super(context);
        init();
    }

    public RecordedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecordedButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        dp5 = (int) getResources().getDimension(R.dimen.dp5);
        colorGray = getResources().getColor(R.color.gray);
        colorBlue = getResources().getColor(R.color.blue);

        paint = new Paint();
        paint.setAntiAlias(true);

        paintProgress = new Paint();
        paintProgress.setAntiAlias(true);
        paintProgress.setColor(colorBlue);
        paintProgress.setStrokeWidth(dp5);
        paintProgress.setStyle(Paint.Style.STROKE);
    }

    public interface OnGestureListener {
        void onLongClick();
        void onClick();
        void onLift();
        void onOver();
    }

    public void setOnGestureListener(OnGestureListener onGestureListener){
        this.onGestureListener = onGestureListener;
    }

    private Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(onGestureListener != null) {
                startAnim(0, 1-zoom);
                onGestureListener.onLongClick();
                closeMode = true;
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                myHandler.sendEmptyMessageDelayed(0, animTime);
                downX = event.getX();
                downY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float upX = event.getX();
                float upY = event.getY();
                if(myHandler.hasMessages(0)){
                    myHandler.removeMessages(0);
                    if (Math.abs(upX - downX) < dp5 && Math.abs(upY - downY) < dp5) {
                        if(onGestureListener != null) onGestureListener.onClick();
                    }
                }else if(onGestureListener != null && closeMode){
                    onGestureListener.onLift();
                    closeButton();
                }
                break;
        }
        return true;
    }

    public void closeButton(){

        if(closeMode) {
            closeMode = false;
            startAnim(1 - zoom, 0);
            girth = 0;
            invalidate();
        }
    }

    private void startAnim(float start, float end){

        ValueAnimator va = ValueAnimator.ofFloat(start, end).setDuration(animTime);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                radius1 = measuredWidth * (zoom + value) / 2;
                radius2 = measuredWidth * (zoom - value) / 2.5f;
                invalidate();
            }
        });
        va.start();
    }

    public void setMax(int max){
        this.max = max;
    }

    public void setProgress(float progress){

        float ratio = progress/max;
        girth = 370*ratio;
        invalidate();

        if(ratio >= 1){
            if(onGestureListener != null) onGestureListener.onOver();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if(measuredWidth == -1) {
            measuredWidth = getMeasuredWidth();

            radius1 = measuredWidth* zoom /2;
            radius2 = measuredWidth* zoom /2.5f;

            //设置绘制大小
            oval = new RectF();
            oval.left = dp5/2;
            oval.top = dp5/2;
            oval.right = measuredWidth-dp5/2;
            oval.bottom = measuredWidth-dp5/2;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //绘制外圈圆 radius1代表绘制半径
        paint.setColor(colorGray);
        canvas.drawCircle(measuredWidth/2, measuredWidth/2, radius1, paint);

        //绘制内圈圆 radius2代表绘制半径
        paint.setColor(Color.WHITE);
        canvas.drawCircle(measuredWidth/2, measuredWidth/2, radius2, paint);

        //绘制进度 270表示以圆的270度为起点, 绘制girth长度的弧线
        canvas.drawArc(oval, 270, girth, false, paintProgress);
    }
}
