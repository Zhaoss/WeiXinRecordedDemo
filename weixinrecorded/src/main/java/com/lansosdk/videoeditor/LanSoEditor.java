package com.lansosdk.videoeditor;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class LanSoEditor {


    private static boolean isLoaded = false;

    public static void initSDK(Context context, String str) {
        loadLibraries(); // 拿出来单独加载库文件.
        LanSoEditor.initSo(context, str);

        checkCPUName();

    }


    private static synchronized void loadLibraries() {
        if (isLoaded)
            return;

        Log.d("lansoeditor", "load libraries.....LanSongffmpeg.");

        System.loadLibrary("LanSongffmpeg");
        System.loadLibrary("LanSongdisplay");
        System.loadLibrary("LanSongplayer");

        isLoaded = true;
    }

    public static void initSo(Context context, String argv) {
        nativeInit(context, context.getAssets(), argv);
    }

    public static void unInitSo() {
        nativeUninit();
    }

    public static native void nativeInit(Context ctx, AssetManager ass, String filename);

    public static native void nativeUninit();

    private static void checkCPUName() {
        String str1 = "/proc/cpuinfo";
        String str2 = "";
        try {
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
            str2 = localBufferedReader.readLine();
            while (str2 != null) {
                if(str2.contains("SDM845")){  //845的平台;
                    VideoEditor.isForceSoftWareEncoder=true;
                }
                else if(str2.contains("")){

                }

                str2 = localBufferedReader.readLine();
            }
            localBufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
