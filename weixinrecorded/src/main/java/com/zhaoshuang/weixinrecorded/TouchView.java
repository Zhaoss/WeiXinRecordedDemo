package com.zhaoshuang.weixinrecorded;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by zhaoshuang on 16/6/17.
 * 手势控制旋转放大缩小View
 */
public class TouchView extends View {

    private float downX;
    private float downY;
    private float firstX;
    private float firstY;
    private OnClickListener listener;
    private boolean clickable = true;
    private int minX = -1;
    private int maxX = -1;
    private int minY = -1;
    private int maxY = -1;
    private OnLimitsListener onLimitsListener;
    private OnTouchListener onTouchListener;
    private boolean isOutLimits;
    private float whRatio;
    private int minWidth;
    private int maxWidth;
    private int minHeight;
    private int maxHeight;

    public TouchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TouchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchView(Context context) {
        super(context);
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    /**
     * 设置边界X
     */
    public void setLimitsX(int minX, int maxX){
        this.minX = minX;
        this.maxX = maxX;
    }

    /**
     * 设置边界Y
     */
    public void setLimitsY(int minY, int maxY){
        this.minY = minY;
        this.maxY = maxY;
    }

    /**
     * 超出边界的回调
     */
    public interface OnLimitsListener {
        void OnOutLimits(float x, float y);
        void OnInnerLimits(float x, float y);
    }

    public void setOnLimitsListener(OnLimitsListener onLimitsListener){
        this.onLimitsListener = onLimitsListener;
    }

    /**
     * 手指触摸事件
     */
    public interface OnTouchListener{
        void onDown(TouchView view, MotionEvent event);
        void onMove(TouchView view, MotionEvent event);
        void onUp(TouchView view, MotionEvent event);
    }

    public void setOnTouchListener(OnTouchListener listener){
        this.onTouchListener = listener;
    }

    /**
     * 是否超出范围
     */
    public boolean isOutLimits(){
        return isOutLimits;
    }

    private float lastDis;
    private float coreX;
    private float coreY;
    private boolean doubleMove = false;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(minWidth == 0){
            whRatio = getWidth()*1f/getHeight();
            minWidth = getWidth()/2;
            ViewGroup parent = (ViewGroup) getParent();
            maxWidth = parent.getWidth();
            minHeight = getHeight()/2;
            maxHeight = (int) (maxWidth / whRatio);
        }
    }

    View tempView;
    float lastRota;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(onTouchListener != null) onTouchListener.onDown(this, event);
                firstX = downX = event.getRawX();
                firstY = downY = event.getRawY();
                coreX = getWidth()/2+getX();//view的中心点坐标
                coreY = getHeight()/2+getY();
                break;
            case MotionEvent.ACTION_MOVE:
                int pointerCount = event.getPointerCount();
                if(pointerCount >= 2){//双点触摸事件
                    doubleMove = true;
                    float distance = getSlideDis(event);
                    float spaceRotation = getRotation(event);
                    if(tempView == null){//创建镜像
                        tempView = new View(getContext());
                        tempView.setX(getX());
                        tempView.setY(getY());
                        tempView.setRotation(getRotation());
                        tempView.setBackground(getBackground());
                        tempView.setLayoutParams(new ViewGroup.LayoutParams(getWidth(), getHeight()));
                        ViewGroup parent = (ViewGroup) getParent();
                        parent.addView(tempView);
                        setAlpha(0);
                    }else{
                        float slide = lastDis - distance;
                        ViewGroup.LayoutParams layoutParams = getLayoutParams();
                        layoutParams.width = (int) (layoutParams.width - slide);
                        float slide2 = slide/whRatio;
                        layoutParams.height = (int) (layoutParams.height - slide2);

                        if(layoutParams.width > maxWidth || layoutParams.height > maxHeight){
                            layoutParams.width = maxWidth;
                            layoutParams.height = maxHeight;
                        }else if(layoutParams.width < minWidth || layoutParams.height < minHeight){
                            layoutParams.width = minWidth;
                            layoutParams.height = minHeight;
                        }

                        setLayoutParams(layoutParams);

                        float x = coreX - getWidth() / 2;
                        float y = coreY - getHeight() / 2;
                        setX(x);
                        setY(y);

                        tempView.setX(x);
                        tempView.setY(y);
                        ViewGroup.LayoutParams layoutParams1 = tempView.getLayoutParams();
                        layoutParams1.width = layoutParams.width;
                        layoutParams1.height = layoutParams.height;
                        tempView.setLayoutParams(layoutParams1);
                        if(lastRota != 0){
                            float f = lastRota-spaceRotation;
                            tempView.setRotation(tempView.getRotation()-f);
                        }
                    }
                    lastRota = spaceRotation;
                    lastDis = distance;
                }else if(!doubleMove && pointerCount == 1){//单点移动事件
                    if(onTouchListener != null) onTouchListener.onMove(this, event);
                    float moveX = event.getRawX();
                    float moveY = event.getRawY();
                    if(moveX != -1 && moveY != -1){
                        if(moveX<=minX || moveX>=maxX || moveY<=minY || moveY>=maxY){
                            if(onLimitsListener != null) onLimitsListener.OnOutLimits(moveX, moveY);
                            isOutLimits = true;
                        }else if(moveX>minX && moveX<maxX && moveY>minY && moveY<maxY){
                            if(onLimitsListener != null) onLimitsListener.OnInnerLimits(moveX, moveY);
                            isOutLimits = false;
                        }
                    }
                    float slideX = moveX - downX + getX();
                    float slideY = moveY - downY + getY();
                    setX(slideX);
                    setY(slideY);
                    downX = moveX;
                    downY = moveY;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_UP:
                if(tempView != null){//镜像赋值回去
                    setAlpha(1);
                    setRotation(tempView.getRotation());
                    ViewGroup parent = (ViewGroup) getParent();
                    parent.removeView(tempView);
                }
                lastRota = 0;
                tempView = null;
                doubleMove = false;
                lastDis = 0;

                if(onTouchListener != null) onTouchListener.onUp(this, event);

                float upX = event.getRawX();
                float upY = event.getRawY();
                if (Math.abs(upX - firstX) < 10 && Math.abs(upY - firstY) < 10 && clickable) {
                    if (listener != null) listener.onClick(this);//单击事件
                }
                break;
        }
        return true;
    }

    /**
     * 获取手指间的旋转角度
     */
    private float getRotation(MotionEvent event) {

        double deltaX = event.getX(0) - event.getX(1);
        double deltaY = event.getY(0) - event.getY(1);
        double radians = Math.atan2(deltaY, deltaX);
        return (float) Math.toDegrees(radians);
    }

    /**
     * 获取手指间的距离
     */
    private float getSlideDis(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}
