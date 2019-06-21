package com.lansosdk.videoeditor;

import android.util.Log;

import java.io.File;

/**
 * 用来得到当前音视频中的各种信息,比如视频的宽高,码率, 帧率,
 * 旋转角度,解码器信息,总时长,总帧数;音频的码率,采样率,解码器信息,总帧数等,为其他各种编辑处理为参考使用.
 *
 * 使用方法是:创建对象, 执行prepare, 使用结果. 可以在任意线程中执行如下:
 *
 * MediaInfo info=new MediaInfo(videoPath);
 * if(info.prepare()){ //<==============主要是这里, 需要执行以下,
 * 		//可以使用MediaInfo中的各种成员变量, 比如vHeight, vWidth vBitrate等等.
 * }else{
 * 		//执行失败.....(大部分是视频路径不对,或Android6.0及以上设备没有打开权限导致)
 * }
 *
 *
 * 杭州蓝松科技有限公司
 * www.lansongtech.com
 */
public class MediaInfo {

	private static final String TAG = "MediaInfo";
	private static final boolean VERBOSE = true;
	public final String filePath;
	public final String fileName; // 视频的文件名, 路径的最后一个/后的字符串.
	public final String fileSuffix; // 文件的后缀名.
	/**
	 * 视频的显示高度,即正常视频高度. 如果视频旋转了90度或270度,则这里等于实际视频的宽度!!
	 */
	public int vHeight;
	/**
	 * 视频的显示宽度, 即正常视频宽度. 如果视频旋转了90度或270度,则这里等于实际视频的高度,请注意!!
	 */
	public int vWidth;
	/**
	 * 视频在编码时的高度,
	 */
	public int vCodecHeight;
	public int vCodecWidth;
	/**
	 * 视频的码率,注意,一般的视频编码时采用的是动态码率VBR,故这里得到的是平均值, 建议在使用时,乘以1.2后使用.
	 */
	public int vBitRate;
	/**
	 * 视频文件中的视频流总帧数.
	 */
	public int vTotalFrames;
	/**
	 * mp4文件中的视频轨道的总时长, 注意,一个mp4文件可能里面的音频和视频时长不同.//单位秒.
	 */
	public float vDuration;
	/**
	 * 视频帧率,可能有浮点数, 如用到MediaCodec中, 则需要(int)转换一下. 但如果要依此来计算时间戳, 尽量采用float类型计算,
	 * 这样可减少误差.
	 */
	public float vFrameRate;
	/**
	 * 视频旋转角度, 比如android手机拍摄的竖屏视频, 后置摄像头270度, 前置摄像头旋转了90度, 可以通过这个获取到.
	 * 如果需要进行画面处理, 需要测试下,是否需要宽度和高度对换下. 正常的网上视频, 是没有旋转的.
	 */
	public float vRotateAngle;

	/******************** audio track info ******************/
	/**
	 * 该视频是否有B帧, 即双向预测帧, 如果有的话, 在裁剪时需要注意, 目前大部分的视频不存在B帧.
	 */
	public boolean vHasBFrame;
	/**
	 * 视频可以使用的解码器,
	 */
	public String vCodecName;
	/**
	 * 视频的 像素格式.目前暂时没有用到.
	 */
	public String vPixelFmt;
	/**
	 * 音频采样率
	 */
	public int aSampleRate;
	/**
	 * 音频通道数量
	 */
	public int aChannels;
	/**
	 * 视频文件中的音频流 总帧数.
	 */
	public int aTotalFrames;
	/**
	 * 音频的码率,可用来检测视频文件中是否
	 */
	public int aBitRate;
	/**
	 * 音频的最大码率, 这里暂时没有用到.
	 */
	public int aMaxBitRate;
	/**
	 * 多媒体文件中的音频总时长
	 */
	public float aDuration;
	/**
	 * 音频可以用的 解码器, 由此可以判定音频是mp3格式,还是aac格式,如果是mp3则这里"mp3"; 如果是aac则这里"aac";
	 */
	public String aCodecName;
	private boolean getSuccess = false;

	/**
	 * 构造方法, 输入文件路径; 注意: 创建对象后, 需要执行 {@link #prepare()}后才可以使用.
	 *
	 * @param path
	 */
	public MediaInfo(String path) {
		filePath = path;
		fileName = getFileNameFromPath(path);
		fileSuffix = getFileSuffix(path);
	}

	/**
	 * 已废弃,不用.
	 */
	@Deprecated
	public MediaInfo(String path,boolean is) {
		filePath = path;
		fileName = getFileNameFromPath(path);
		fileSuffix = getFileSuffix(path);
	}
	/**
	 * 是否支持.
	 *
	 * @param videoPath
	 * @return
	 */
	public static boolean isSupport(String videoPath) {
		if (fileExist(videoPath)) {
			MediaInfo info = new MediaInfo(videoPath);
			return info.prepare();
		} else {
			if (VERBOSE)
				Log.i(TAG, "video:" + videoPath + " not support");
			return false;
		}
	}

	/**
	 * 如果在调试中遇到问题了, 首先应该执行这里, 这样可以检查出60%以上的错误信息. 在出错代码的上一行增加： 2018年1月4日20:54:07:
	 * 新增, 在内部直接打印, 外部无效增加Log
	 *
	 * @param videoPath
	 * @return
	 */
	public static String checkFile(String videoPath) {
		String ret = " ";
		if (videoPath == null) {
			ret = "文件名为空指针, null";
		} else {
			File file = new File(videoPath);
			if (file.exists() == false) {
				ret = "文件不存在," + videoPath;
			} else if (file.isDirectory()) {
				ret = "您设置的路径是一个文件夹," + videoPath;
			} else if (file.length() == 0) {
				ret = "文件存在,但文件的大小为0字节(可能您只创建文件,但没有进行各种调用设置导致的.)." + videoPath;
			} else {
				MediaInfo info = new MediaInfo(videoPath);
				if (info.fileSuffix.equals("pcm")
						|| info.fileSuffix.equals("yuv")) {
					String str = "文件路径:" + info.filePath + "\n";
					str += "文件名:" + info.fileName + "\n";
					str += "文件后缀:" + info.fileSuffix + "\n";
					str += "文件大小(字节):" + file.length() + "\n";
					ret = "文件存在,但文件的后缀可能表示是裸数据,我们的SDK需要多媒体格式的后缀是mp4/mp3/aac/m4a/mov/gif等常见格式";
					ret += str;
				} else if (info.prepare()) {
					ret = "文件内的信息是:\n";
					String str = "文件路径:" + info.filePath + "\n";
					str += "文件名:" + info.fileName + "\n";
					str += "文件后缀:" + info.fileSuffix + "\n";
					str += "文件大小(字节):" + file.length() + "\n";
					if (info.isHaveVideo()) {
						str += "视频信息-----:\n";
						str += "宽度:" + info.vWidth + "\n";
						str += "高度:" + info.vHeight + "\n";
						str += "时长:" + info.vDuration + "\n";
						str += "帧率:" + info.vFrameRate + "\n";
						str += "码率:" + info.vBitRate + "\n";
						str += "旋转角度:" + info.vRotateAngle + "\n";
					} else {
						str += "<无视频信息>\n";
					}

					if (info.isHaveAudio()) {
						str += "音频信息-----:\n";
						str += "采样率:" + info.aSampleRate + "\n";
						str += "通道数:" + info.aChannels + "\n";
						str += "码率:" + info.aBitRate + "\n";
						str += "时长:" + info.aDuration + "\n";
					} else {
						str += "<无音频信息>\n";
					}
					ret += str;
				} else {
					ret = "文件存在, 但MediaInfo.prepare获取媒体信息失败,请查看下 文件是否是音频或视频."
							+ videoPath;
				}
			}
		}
		Log.i(TAG, "当前文件的音视频信息是:" + ret);
		return ret;
	}

	private static boolean fileExist(String absolutePath) {
		if (absolutePath == null)
			return false;
		else
			return (new File(absolutePath)).exists();
	}

	/**
	 * 准备当前媒体的信息
	 * 去底层运行相关方法, 得到媒体信息.
	 *
	 * @return 如获得当前媒体信息并支持格式, 则返回true, 否则返回false;
	 */
	public boolean prepare() {
		int ret = 0;
		if (fileExist(filePath)) { // 这里检测下mfilePath是否是多媒体后缀.
			ret = nativePrepare(filePath, false);
			if (ret >= 0) {
				getSuccess = true;
				return isSupport();
			} else {
				if (ret == -13) {
					Log.e(TAG, "MediaInfo执行失败，可能您没有打开读写文件授权导致的，我们提供了PermissionsManager类来检测,可参考使用");
				} else {
					Log.e(TAG, "MediaInfo执行失败，" + prepareErrorInfo(filePath));
				}
				return false;
			}
		} else {
			Log.e(TAG, "MediaInfo执行失败,你设置的文件不存在.您的设置是:"+filePath );
			return false;
		}
	}

	/**
	 * [新增] 获取当前视频在显示的时候, 图像的宽度;
	 * <p>
	 * 因为有些视频是90度或270度旋转显示的, 旋转的话, 就宽高对调了
	 *
	 * @return
	 */
	public int getWidth() {
		if (getSuccess) {
			if (vRotateAngle == 90 || vRotateAngle == 270) {
				return vHeight;
			} else {
				return vWidth;
			}
		}
		return 0;
	}

	/**
	 * [新增]
	 * <p>
	 * 获取当前视频在显示的时候, 图像的高度; 因为有些视频是90度或270度旋转显示的, 旋转的话, 就宽高对调了
	 *
	 * @return
	 */
	public int getHeight() {
		if (getSuccess) {
			if (vRotateAngle == 90 || vRotateAngle == 270) {
				return vWidth;
			} else {
				return vHeight;
			}
		}
		return 0;
	}

	public void release() {
		// TODO nothing
		getSuccess = false;
	}

	/**
	 * 是否是竖屏的视频.
	 *
	 * @return
	 */
	public boolean isPortVideo() {
		if (vWidth > 0 && vHeight > 0) {
			// 高度大于宽度, 或者旋转角度等于90/270,则是竖屏, 其他认为是横屏.
			if ((vHeight > vWidth) && (vRotateAngle == 0)) {
				return true;
			} else if (vRotateAngle == 90 || vRotateAngle == 270) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public boolean isHaveAudio() {
		if (aBitRate > 0) // 有音频
		{
			if (aChannels == 0)
				return false;

			if (aCodecName == null || aCodecName.isEmpty())
				return false;

			return true;
		} else {
			return false;
		}
	}

	public boolean isHaveVideo() {
		if (vBitRate > 0 || vWidth > 0 || vHeight > 0) {
			if (vHeight == 0 || vWidth == 0) {
				return false;
			}

			if (vCodecName == null || vCodecName.isEmpty())
				return false;

			return true;
		}
		return false;
	}

	/**
	 * 传递过来的文件是否支持
	 *
	 * @return
	 */
	public boolean isSupport() {
		return isHaveAudio() || isHaveVideo();
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String info = "file name:" + filePath + "\n";
		info += "fileName:" + fileName + "\n";
		info += "fileSuffix:" + fileSuffix + "\n";
		info += "vHeight:" + vHeight + "\n";
		info += "vWidth:" + vWidth + "\n";
		info += "vCodecHeight:" + vCodecHeight + "\n";
		info += "vCodecWidth:" + vCodecWidth + "\n";
		info += "vBitRate:" + vBitRate + "\n";
		info += "vTotalFrames:" + vTotalFrames + "\n";
		info += "vDuration:" + vDuration + "\n";
		info += "vFrameRate:" + vFrameRate + "\n";
		info += "vRotateAngle:" + vRotateAngle + "\n";
		info += "vHasBFrame:" + vHasBFrame + "\n";
		info += "vCodecName:" + vCodecName + "\n";
		info += "vPixelFmt:" + vPixelFmt + "\n";

		info += "aSampleRate:" + aSampleRate + "\n";
		info += "aChannels:" + aChannels + "\n";
		info += "aTotalFrames:" + aTotalFrames + "\n";
		info += "aBitRate:" + aBitRate + "\n";
		info += "aMaxBitRate:" + aMaxBitRate + "\n";
		info += "aDuration:" + aDuration + "\n";
		info += "aCodecName:" + aCodecName + "\n";

		// if(getSuccess) //直接返回,更直接, 如果执行错误, 更要返回
		return info;
		// else
		// return "MediaInfo is not ready.or call failed";
	}

	public native int nativePrepare(String filepath, boolean checkCodec);

	// used by JNI
	private void setVideoCodecName(String name) {
		this.vCodecName = name;
	}

	// used by JNI
	private void setVideoPixelFormat(String pxlfmt) {
		this.vPixelFmt = pxlfmt;
	}

	// used by JNI
	private void setAudioCodecName(String name) {
		this.aCodecName = name;
	}

	private String prepareErrorInfo(String videoPath) {
		String ret = " ";
		if (videoPath == null) {
			ret = "文件名为空指针, null";
		} else {
			File file = new File(videoPath);
			if (file.exists() == false) {
				ret = "文件不存在," + videoPath;
			} else if (file.isDirectory()) {
				ret = "您设置的路径是一个文件夹," + videoPath;
			} else if (file.length() == 0) {
				ret = "文件存在,但文件的大小为0字节." + videoPath;
			} else {
				if (fileSuffix.equals("pcm") || fileSuffix.equals("yuv")) {
					String str = "文件路径:" + filePath + "\n";
					str += "文件名:" + fileName + "\n";
					str += "文件后缀:" + fileSuffix + "\n";
					str += "文件大小(字节):" + file.length() + "\n";
					ret = "文件存在,但文件的后缀可能表示是裸数据";
					ret += str;
				} else {
					ret = "文件存在, 但MediaInfo.prepare获取媒体信息失败,请查看下 文件是否是音频或视频, 或许演示工程APP名字不是我们demo中的名字:" + videoPath;
				}
			}

		}
		return ret;
	}

	private String getFileNameFromPath(String path) {
		if (path == null)
			return "";
		int index = path.lastIndexOf('/');
		if (index > -1)
			return path.substring(index + 1);
		else
			return path;
	}

	private String getFileSuffix(String path) {
		if (path == null)
			return "";
		int index = path.lastIndexOf('.');
		if (index > -1)
			return path.substring(index + 1);
		else
			return "";
	}
	/*
	 * ****************************************************************************
	 * 测试 // new Thread(new Runnable() { // // @Override // public void run() {
	 * // // TODO Auto-generated method stub // MediaInfo mif=new
	 * MediaInfo("/sdcard/2x.mp4"); //这里是我们的测试视频地址, 如您测试, 则需要修改视频地址. //
	 * mif.prepare(); // Log.i(TAG,"mif is:"+ mif.toString()); // mif.release();
	 * // } // },"testMediaInfo#1").start(); // new Thread(new Runnable() { //
	 * // @Override // public void run() { // // TODO Auto-generated method stub
	 * // MediaInfo mif=new MediaInfo("/sdcard/2x.mp4");//这里是我们的测试视频地址, 如您测试,
	 * 则需要修改视频地址. // mif.prepare(); // Log.i(TAG,"mif is:"+ mif.toString()); //
	 * mif.release(); // } // },"testMediaInfo#2").start();
	 */
}
