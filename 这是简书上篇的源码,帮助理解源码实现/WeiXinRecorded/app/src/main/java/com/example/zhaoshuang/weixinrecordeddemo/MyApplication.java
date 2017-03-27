package com.example.zhaoshuang.weixinrecordeddemo;

import android.app.Application;

import com.yixia.camera.VCamera;

import java.io.File;

/**
 * Created by zhaoshuang on 17/2/8.
 */

public class MyApplication extends Application {

    public static String VIDEO_PATH =  "/sdcard/WeiXinRecordedDemo/";

    @Override
    public void onCreate() {

        VIDEO_PATH += String.valueOf(System.currentTimeMillis());
        File file = new File(VIDEO_PATH);
        if(!file.exists()) file.mkdirs();

        //设置视频缓存路径
        VCamera.setVideoCachePath(VIDEO_PATH);

        // 开启log输出,ffmpeg输出到logcat
        VCamera.setDebugMode(true);

        // 初始化拍摄SDK，必须
        VCamera.initialize(this);
    }
}
