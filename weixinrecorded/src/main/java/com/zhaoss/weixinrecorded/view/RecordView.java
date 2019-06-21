package com.zhaoss.weixinrecorded.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.zhaoss.weixinrecorded.R;

public class RecordView extends View {

    private Paint paint;
    private OnGestureListener mOnGestureListener;

    private int downColor;
    private int upColor;

    private float slideDis;

    private float radiusDis;
    private float currentRadius;
    private float downRadius;
    private float upRadius;

    private float strokeWidthDis;
    private float currentStrokeWidth;
    private float minStrokeWidth;
    private float maxStrokeWidth;

    public RecordView(Context context) {
        super(context);
        init();
    }

    public RecordView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        downColor = R.color.video_gray;
        upColor = R.color.white;

        paint = new Paint();
        paint.setAntiAlias(true);//抗锯齿
        paint.setStyle(Paint.Style.STROKE);//画笔属性是空心圆
        currentStrokeWidth = getResources().getDimension(R.dimen.dp10);
        paint.setStrokeWidth(currentStrokeWidth);//设置画笔粗细

        slideDis = getResources().getDimension(R.dimen.dp10);
        radiusDis = getResources().getDimension(R.dimen.dp3);
        strokeWidthDis =  getResources().getDimension(R.dimen.dp1)/4;

        minStrokeWidth = currentStrokeWidth;
        maxStrokeWidth = currentStrokeWidth*2;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(downRadius == 0){
            downRadius = getWidth()*0.5f-currentStrokeWidth;
            upRadius = getWidth()*0.3f-currentStrokeWidth;
        }
    }

    private boolean isTouch = true;
    public void setTouch(boolean isTouch){
        this.isTouch = isTouch;
    }

    private boolean isDown;
    private float downX;
    private float downY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(!isTouch){
            return true;
        }

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                ViewGroup parent = (ViewGroup) getParent();
                parent.requestDisallowInterceptTouchEvent(true);

                downX = event.getRawX();
                downY = event.getRawY();
                if(mOnGestureListener != null) {
                    mOnGestureListener.onDown();
                }
                isDown = true;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:

                break;
            case MotionEvent.ACTION_UP:
                ViewGroup parent1 = (ViewGroup) getParent();
                parent1.requestDisallowInterceptTouchEvent(false);
                float upX = event.getRawX();
                float upY = event.getRawY();
                if (Math.abs(upX - downX) < slideDis && Math.abs(upY - downY) < slideDis) {
                    if(mOnGestureListener != null) {
                        mOnGestureListener.onClick();
                    }
                }
                if(mOnGestureListener != null) {
                    mOnGestureListener.onUp();
                }
                initState();
                break;
        }
        return true;
    }

    public void initState(){
        isDown = false;
        invalidate();
    }

    public void setOnGestureListener(OnGestureListener listener){
        this.mOnGestureListener = listener;
    }

    public interface OnGestureListener{
        void onDown();
        void onUp();
        void onClick();
    }

    public void setPaintColor(@ColorInt int downColor, @ColorInt int upColor){
        this.downColor = downColor;
        this.upColor = upColor;
    }

    boolean changeStrokeWidth;
    boolean isAdd;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(isDown){
            paint.setColor(ContextCompat.getColor(getContext(), downColor));
            if(changeStrokeWidth){
                if (isAdd) {
                    currentStrokeWidth += strokeWidthDis;
                    if (currentStrokeWidth > maxStrokeWidth) isAdd = false;
                } else {
                    currentStrokeWidth -= strokeWidthDis;
                    if (currentStrokeWidth < minStrokeWidth) isAdd = true;
                }
                paint.setStrokeWidth(currentStrokeWidth);
                currentRadius = getWidth()*0.5f-currentStrokeWidth;
            }else {
                if (currentRadius < downRadius) {
                    currentRadius += radiusDis;
                } else if (currentRadius >= downRadius) {
                    currentRadius = downRadius;
                    isAdd = true;
                    changeStrokeWidth = true;
                }
            }
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, currentRadius, paint);
            invalidate();
        }else {
            changeStrokeWidth = false;
            currentStrokeWidth = minStrokeWidth;
            paint.setStrokeWidth(currentStrokeWidth);
            paint.setColor(ContextCompat.getColor(getContext(), upColor));
            if(currentRadius > upRadius){
                currentRadius -= radiusDis;
                invalidate();
            }else if(currentRadius < upRadius){
                currentRadius = upRadius;
                invalidate();
            }
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, currentRadius, paint);
        }
    }
}
