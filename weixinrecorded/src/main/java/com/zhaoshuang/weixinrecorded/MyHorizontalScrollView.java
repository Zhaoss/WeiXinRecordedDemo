package com.zhaoshuang.weixinrecorded;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * Created by zhaoshuang on 17/8/23.
 */

public class MyHorizontalScrollView extends HorizontalScrollView {

    private OnScrollXListener onScrollXListener;
    private int scrollX;
    private int currX;

    public MyHorizontalScrollView(Context context) {
        super(context);
    }

    public MyHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);

        if(onScrollXListener != null){
            onScrollXListener.onScrollX(x);
        }
        this.scrollX = x;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if(currX != scrollX){
                    currX = scrollX;
                    if(onScrollXListener != null){
                        onScrollXListener.onScrollStateChange();
                    }
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    public int getScroll(){
        return scrollX;
    }

    public void setOnScrollXListener(OnScrollXListener listener){
        this.onScrollXListener = listener;
    }

    public interface OnScrollXListener {
        void onScrollX(int scrollX);
        void onScrollStateChange();
    }
}
