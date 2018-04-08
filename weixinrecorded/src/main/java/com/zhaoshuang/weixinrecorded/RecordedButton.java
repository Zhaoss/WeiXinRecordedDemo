package com.zhaoshuang.weixinrecorded;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhaoshuang on 17/2/8.
 */

public class RecordedButton extends View {

    private int measuredWidth = -1;
    private Paint paint;
    private int colorGray;
    private float radius1;
    private float radius2;
    private float zoom = 0.8f;//初始化缩放比例
    private int dp5;
    private Paint paintProgress;
    private int colorBlue;
    /** 当前进度 以角度为单位 */
    private float girthPro;
    private RectF oval;
    private int max;
    private OnGestureListener onGestureListener;
    private int animTime = 150;
    private float downX;
    private float downY;
    /** button是否处于打开状态 */
    private boolean isOpenMode = true;
    private List<Float> splitList = new ArrayList<>();
    private Paint paintSplit;
    private boolean isDeleteMode;
    private Paint paintDelete;
    private ValueAnimator buttonAnim;
    private float progress;
    private boolean isResponseLongTouch = true;
    private float rawX = -1;
    private float rawY = -1;

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

        dp5 = (int) getResources().getDimension(R.dimen.dp6);
        colorGray = getResources().getColor(R.color.video_gray);
        colorBlue = getResources().getColor(R.color.blue);

        paint = new Paint();
        paint.setAntiAlias(true);

        paintProgress = new Paint();
        paintProgress.setAntiAlias(true);
        paintProgress.setColor(colorBlue);
        paintProgress.setStrokeWidth(dp5);
        paintProgress.setStyle(Paint.Style.STROKE);

        paintSplit = new Paint();
        paintSplit.setAntiAlias(true);
        paintSplit.setColor(Color.WHITE);
        paintSplit.setStrokeWidth(dp5);
        paintSplit.setStyle(Paint.Style.STROKE);

        paintDelete = new Paint();
        paintDelete.setAntiAlias(true);
        paintDelete.setColor(Color.RED);
        paintDelete.setStrokeWidth(dp5);
        paintDelete.setStyle(Paint.Style.STROKE);

        //设置绘制大小
        oval = new RectF();
    }

    /**
     * 设置是否响应长按事件
     * @param isResponseLongTouch
     */
    public void setResponseLongTouch(boolean isResponseLongTouch){
        this.isResponseLongTouch = isResponseLongTouch;
    }

    public int getSplitCount() {
        return splitList.size();
    }

    public float getCurrentPro(){
        return  progress;
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
                isOpenMode = true;
                onGestureListener.onLongClick();
            }
        }
    };

    private float firstX;
    private float firstY;
    private boolean cleanResponse;//清除所有响应
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isResponseLongTouch) myHandler.sendEmptyMessageDelayed(0, animTime);
                firstX = downX = event.getRawX();
                firstY = downY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getRawX();
                float moveY = event.getRawY();

                if (Math.abs(moveX - firstX) > dp5 || Math.abs(moveY - firstY) > dp5) {
                    if (myHandler.hasMessages(0)) {
                        cleanResponse = true;
                        myHandler.removeMessages(0);
                    }
                }

                float slideX = moveX - downX;
                float slideY = moveY - downY;
                //跟随手指移动
                setX(getX() + slideX);
                setY(getY() + slideY);
                downX = moveX;
                downY = moveY;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float upX = event.getRawX();
                float upY = event.getRawY();

                if (!cleanResponse){
                    if (isResponseLongTouch && !myHandler.hasMessages(0)) {
                        if (isOpenMode) {
                            if (onGestureListener != null) onGestureListener.onLift();
                            closeButton();
                        }
                    } else {
                        myHandler.removeMessages(0);
                        if (Math.abs(upX - firstX) < dp5 && Math.abs(upY - firstY) < dp5) {
                            if (onGestureListener != null) onGestureListener.onClick();
                        }
                    }
                }

                cleanResponse = false;
                if(upX != firstX || upY != firstY){//回到原坐标
                    startMoveAnim();
                }
                break;
        }
        return true;
    }

    private void startMoveAnim(){

        final float slideX = rawX-getX();
        final float slideY = rawY-getY();

        final float rX = getX();
        final float rY = getY();

        ValueAnimator va = ValueAnimator.ofFloat(0, 1).setDuration(50);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                setX(rX+slideX*value);
                setY(rY+slideY*value);
            }
        });

        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(Math.abs(slideX) > Math.abs(slideY)){
                    jitterAnim(slideX/5, true);
                }else{
                    jitterAnim(slideY/5, false);
                }
            }
        });
        va.start();
    }

    boolean flag;
    private void jitterAnim(float slide, final boolean isX){

        ValueAnimator va = ValueAnimator.ofFloat(slide, 0).setDuration(100);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                if(flag){
                    value = -value;
                }
                if(isX){
                    setX(rawX+value);
                }else{
                    setY(rawY+value);
                }
                flag = !flag;
            }
        });
        va.start();
    }

    public void closeButton(){
        if(isOpenMode) {
            isOpenMode = false;
            startAnim(1-zoom, 0);
        }
    }

    private void startAnim(float start, float end){

        if(buttonAnim == null || !buttonAnim.isRunning()) {
            buttonAnim = ValueAnimator.ofFloat(start, end).setDuration(animTime);
            buttonAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    radius1 = measuredWidth * (zoom + value) / 2;
                    radius2 = measuredWidth * (zoom - value) / 2 - dp5;

                    value = 1 - zoom - value;
                    oval.left = measuredWidth * value / 2 + dp5 / 2;
                    oval.top = measuredWidth * value / 2 + dp5 / 2;
                    oval.right = measuredWidth * (1 - value / 2) - dp5 / 2;
                    oval.bottom = measuredWidth * (1 - value / 2) - dp5 / 2;

                    invalidate();
                }
            });
            buttonAnim.start();
        }
    }

    public void setMax(int max){
        this.max = max;
    }

    /**
     * 设置进度
     */
    public void setProgress(float progress){

        this.progress = progress;
        float ratio = progress/max;
        girthPro = 365*ratio;
        invalidate();

        if(ratio >= 1){
            if(onGestureListener != null) onGestureListener.onOver();
        }
    }

    /**
     * 设置段点
     */
    public void setSplit(){
        splitList.add(girthPro);
        invalidate();
    }

    /**
     * 删除最后一个段点
     */
    public void deleteSplit(){
        if(isDeleteMode && splitList.size() > 0){
            splitList.remove(splitList.size()-1);
            isDeleteMode = false;
            invalidate();
        }
    }

    /**
     * 清除断点
     */
    public void cleanSplit(){
        if(splitList.size() > 0) {
            splitList.clear();
            invalidate();
        }
    }

    /**
     * 设置删除模式
     */
    public void setDeleteMode(boolean isDeleteMode){
        this.isDeleteMode = isDeleteMode;
        invalidate();
    }

    /**
     * 是否正在删除模式
     */
    public boolean isDeleteMode(){
        return isDeleteMode;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if(measuredWidth == -1) {
            measuredWidth = getMeasuredWidth();

            radius1 = measuredWidth* zoom /2;
            radius2 = measuredWidth* zoom /2 - dp5;

            oval.left = dp5/2;
            oval.top = dp5/2;
            oval.right = measuredWidth-dp5/2;
            oval.bottom = measuredWidth-dp5/2;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(rawX == -1) {
            rawX = getX();
            rawY = getY();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //绘制外圈
        paint.setColor(colorGray);
        canvas.drawCircle(measuredWidth/2, measuredWidth/2, radius1, paint);
        //绘制内圈
        paint.setColor(Color.WHITE);
        canvas.drawCircle(measuredWidth/2, measuredWidth/2, radius2, paint);
        //绘制进度
        canvas.drawArc(oval, 270, girthPro, false, paintProgress);
        //绘制段点
        for (int x=0; x<splitList.size(); x++){
            if(x != 0) canvas.drawArc(oval, 270+splitList.get(x), 1, false, paintSplit);
        }
        //绘制删除模式的段落
        if(isDeleteMode && splitList.size()>0){
            float split = splitList.get(splitList.size() - 1);
            canvas.drawArc(oval, 270+split, girthPro -split, false, paintDelete);
        }
    }
}