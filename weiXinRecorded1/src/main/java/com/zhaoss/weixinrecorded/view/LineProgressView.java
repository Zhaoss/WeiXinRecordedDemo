package com.zhaoss.weixinrecorded.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class LineProgressView extends View {

    private Paint paint;
    private float progress;
    private float tagProgress;
    private ArrayList<Float> splitList = new ArrayList<>();

    public LineProgressView(Context context) {
        super(context);
        init();
    }

    public LineProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setMinProgress(float tagProgress) {
        this.tagProgress = tagProgress;
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        paint.setStrokeWidth(getHeight());
    }

    /**
     * 设置段点
     */
    public void setSplit() {
        splitList.add(progress);
        invalidate();
    }

    public void setTagProgress(float tagProgress) {
        this.tagProgress = tagProgress;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(Color.BLACK);
        canvas.drawLine(0, 0, getWidth(), 0, paint);

        paint.setColor(Color.WHITE);
        canvas.drawLine(0, 0, getWidth() * progress, 0, paint);

        paint.setColor(Color.WHITE);
        canvas.drawLine(getWidth() * tagProgress, 0, getWidth() * tagProgress + getHeight(), 0, paint);

        paint.setColor(Color.BLACK);
        for (int x = 0; x < splitList.size(); x++) {
            if (x > 0) {
                canvas.drawLine(getWidth() * splitList.get(x), 0, getWidth() * splitList.get(x) + getHeight(), 0, paint);
            }
        }
    }

    public int getSplitCount() {
        return splitList.size();
    }

    public void cleanSplit() {
        progress = 0;
        splitList.clear();
        invalidate();
    }

    public void removeSplit() {
        if (splitList.size() > 0) {
            progress = splitList.get(splitList.size() - 1);
            splitList.remove(progress);
            invalidate();
        }
    }
}
