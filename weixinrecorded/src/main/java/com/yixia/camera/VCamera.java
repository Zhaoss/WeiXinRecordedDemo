package com.yixia.camera;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

import com.yixia.camera.util.DeviceUtils;
import com.yixia.camera.util.Log;
import com.yixia.videoeditor.adapter.UtilityAdapter;

import java.io.File;

/**
 * 
 * 拍摄SDK
 * 
 * @author yixia.com
 *
 */
public class VCamera {
	/** 应用包名 */
	private static String mPackageName;
	/** 应用版本名称 */
	private static String mAppVersionName;
	/** 应用版本号 */
	private static int mAppVersionCode;
	/** 视频缓存路径 */
	private static String mVideoCachePath;
	/** SDK版本号 */
	public final static String VCAMERA_SDK_VERSION = "1.2.0";

	/**
	 * 初始化SDK
	 */
	public static void initialize(Context context) {
		mPackageName = context.getPackageName();

		mAppVersionName = getVerName(context);
		mAppVersionCode = getVerCode(context);

		//初始化底层库
		UtilityAdapter.FFmpegInit(context, String.format("versionName=%s&versionCode=%d&sdkVersion=%s&android=%s&device=%s",
				mAppVersionName, mAppVersionCode, VCAMERA_SDK_VERSION, DeviceUtils.getReleaseVersion(), DeviceUtils.getDeviceModel()));
	}

	/**
	 * 获取当前应用的版本号
	 * @param context
	 * @return
	 */
	public static int getVerCode(Context context) {
		int verCode = -1;
		try {
			verCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
		}
		return verCode;
	}

	/** 获取当前应用的版本名称 */
	public static String getVerName(Context context) {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
		}
		return "";
	}

	/** 是否开启log输出 */
	public static boolean isLog() {
		return Log.getIsLog();
	}

	public static String getPackageName() {
		return mPackageName;
	}

	/** 是否开启Debug模式，会输出log */
	public static void setDebugMode(boolean enable) {
		Log.setLog(enable);
	}

	/** 获取视频缓存文件夹 */
	public static String getVideoCachePath() {
		return mVideoCachePath;
	}

	/** 设置视频缓存路径 */
	public static void setVideoCachePath(String path) {
		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}

		mVideoCachePath = path;
	}
}
