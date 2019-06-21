package com.zhaoss.weixinrecorded.util;

import android.app.Activity;

import java.text.DecimalFormat;

public class Utils {

    public static float formatFloat(float value){
        DecimalFormat decimalFormat = new DecimalFormat(".0");
        return Float.valueOf(decimalFormat.format(value));
    }

    public static int getWindowWidth(Activity activity){
        return activity.getWindowManager().getDefaultDisplay().getWidth();
    }

    public static int getWindowHeight(Activity activity){
        return activity.getWindowManager().getDefaultDisplay().getHeight();
    }
}
