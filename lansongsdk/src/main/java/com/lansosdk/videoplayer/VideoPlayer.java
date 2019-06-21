package com.lansosdk.videoplayer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.lansosdk.box.LSLog;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.Map;


/**
 * 
 */
public class VideoPlayer  {
    int MEDIA_INFO_UNKNOWN = 1;
    static int MEDIA_INFO_STARTED_AS_NEXT = 2;
    static  int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    static int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    static int MEDIA_INFO_BUFFERING_START = 701;
    static int MEDIA_INFO_BUFFERING_END = 702;
    static int MEDIA_INFO_NETWORK_BANDWIDTH = 703;
    static int MEDIA_INFO_BAD_INTERLEAVING = 800;
    static int MEDIA_INFO_NOT_SEEKABLE = 801;
    static int MEDIA_INFO_METADATA_UPDATE = 802;
    static int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
    static int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;
    static int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;

    static int MEDIA_INFO_VIDEO_ROTATION_CHANGED = 10001;
    static int MEDIA_INFO_AUDIO_RENDERING_START = 10002;

    static int MEDIA_INFO_MEDIA_ACCURATE_SEEK_COMPLETE = 10100;
    		
    static int MEDIA_ERROR_UNKNOWN = 1;
    static int MEDIA_ERROR_SERVER_DIED = 100;
    static int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    static int MEDIA_ERROR_IO = -1004;
    static int MEDIA_ERROR_MALFORMED = -1007;
    static int MEDIA_ERROR_UNSUPPORTED = -1010;
    static int MEDIA_ERROR_TIMED_OUT = -110;
    
    private static final int MEDIA_NOP = 0; // interface test message
    private static final int MEDIA_PREPARED = 1;
    /**
     * read_thread读取到文件结束的时候, 发出complete,但read_thread不会退出
     */
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_TIMED_TEXT = 99;
    private static final int MEDIA_ERROR = 100;
    private static final int MEDIA_INFO = 200;
    
    private static final int VIDEOEDIT_EVENT_COMPLETE=8001;
    
    protected static final int MEDIA_SET_VIDEO_SAR = 10001;

    protected static final int LANSONG_FRAME_UPDATE=80001;

    //----------------------------------------
    // options
    public static final int LOG_UNKNOWN = 0;
    public static final int LOG_DEFAULT = 1;

    public static final int LOG_VERBOSE = 2;
    public static final int LOG_DEBUG = 3;
    public static final int LOG_INFO = 4;
    public static final int LOG_WARN = 5;
    public static final int LOG_ERROR = 6;
    public static final int LOG_FATAL = 7;
    public static final int LOG_SILENT = 8;

    public static final int OPT_CATEGORY_FORMAT     = 1;
    public static final int OPT_CATEGORY_CODEC      = 2;
    public static final int OPT_CATEGORY_SWS        = 3;
    public static final int OPT_CATEGORY_PLAYER     = 4;

    public static final int SDL_FCC_YV12 = 0x32315659; // YV12
    public static final int SDL_FCC_RV16 = 0x36315652; // RGB565
    public static final int SDL_FCC_RV32 = 0x32335652; // RGBX8888
    //----------------------------------------

    //----------------------------------------
    // properties
    public static final int PROP_FLOAT_VIDEO_DECODE_FRAMES_PER_SECOND = 10001;
    public static final int PROP_FLOAT_VIDEO_OUTPUT_FRAMES_PER_SECOND = 10002;
    public static final int FFP_PROP_FLOAT_PLAYBACK_RATE              = 10003;

    public static final int FFP_PROP_INT64_SELECTED_VIDEO_STREAM      = 20001;
    public static final int FFP_PROP_INT64_SELECTED_AUDIO_STREAM      = 20002;
    //----------------------------------------

    private long mNativeMediaPlayer;
    private long mNativeMediaDataSource;

    private int mNativeSurfaceTexture;

    private int mListenerContext;

    private SurfaceHolder mSurfaceHolder;
    private EventHandler mEventHandler;
    private PowerManager.WakeLock mWakeLock = null;
    private boolean mScreenOnWhilePlaying;
    private boolean mStayAwake;

    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;

    private String mDataSource;

   
    private static volatile boolean mIsNativeInitialized = false;
    private static void initNativeOnce() {
        synchronized (VideoPlayer.class) {
            if (!mIsNativeInitialized) {
                native_init();
                mIsNativeInitialized = true;
            }
        }
    }

    public interface OnPlayerPreparedListener {
        void onPrepared(VideoPlayer mp);
    }

    public interface OnPlayerCompletionListener {
        void onCompletion(VideoPlayer mp);
    }

    public interface OnPlayerBufferingUpdateListener {
        void onBufferingUpdate(VideoPlayer mp, int percent);
    }

    public  interface OnPlayerSeekCompleteListener {
        void onSeekComplete(VideoPlayer mp);
    }
    public  interface OnPlayerExactlySeekCompleteListener {
        void onExactlySeekComplete(VideoPlayer mp);
    }
    public  interface OnPlayerVideoSizeChangedListener {
        void onVideoSizeChanged(VideoPlayer mp, int width, int height,
                                int sar_num, int sar_den);
    }

    public interface OnPlayerErrorListener {
        boolean onError(VideoPlayer mp, int what, int extra);
    }

    public  interface OnPlayerInfoListener {
        boolean onInfo(VideoPlayer mp, int what, int extra);
    }

    public  interface OnPlayeFrameUpdateListener {
        void onFrameUpdate(VideoPlayer mp, int currentMs);
    }
    
    private OnPlayerPreparedListener mOnPreparedListener;
    private OnPlayerCompletionListener mOnCompletionListener;
    private OnPlayerBufferingUpdateListener mOnBufferingUpdateListener;
    private OnPlayerSeekCompleteListener mOnSeekCompleteListener;
    
    
    private OnPlayerExactlySeekCompleteListener mOnExactlySeekCompleteListener;
    
    private OnPlayerVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnPlayerErrorListener mOnErrorListener;
    private OnPlayerInfoListener mOnInfoListener;
    private OnPlayeFrameUpdateListener mOnPlayeFrameUpdateListener;

    public final void setOnPreparedListener(OnPlayerPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public final void setOnCompletionListener(OnPlayerCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public final void setOnBufferingUpdateListener(
            OnPlayerBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    public final void setOnSeekCompleteListener(OnPlayerSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }
    
    public final void setOnExactlySeekCompleteListener(OnPlayerExactlySeekCompleteListener listener) {
    	mOnExactlySeekCompleteListener = listener;
    }

    /**
     * 当前即将要显示这一帧画面的时间戳; 监听
     * @param listener
     */
    public void setOnPlayeFrameUpdateListener(OnPlayeFrameUpdateListener listener){
        mOnPlayeFrameUpdateListener=listener;
    }

    public final void setOnVideoSizeChangedListener(
            OnPlayerVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    public final void setOnErrorListener(OnPlayerErrorListener listener) {
        mOnErrorListener = listener;
    }

    public final void setOnInfoListener(OnPlayerInfoListener listener) {
        mOnInfoListener = listener;
    }


    protected final void notifyOnPrepared() {
        if (mOnPreparedListener != null)
            mOnPreparedListener.onPrepared(this);
    }

    protected final void notifyFrameUpdate(int ptsMs){
        if(mOnPlayeFrameUpdateListener!=null){
            mOnPlayeFrameUpdateListener.onFrameUpdate(this,ptsMs);
        }
    }
    protected final void notifyOnCompletion() {
        if (mOnCompletionListener != null)
            mOnCompletionListener.onCompletion(this);
    }

    protected final void notifyOnBufferingUpdate(int percent) {
        if (mOnBufferingUpdateListener != null)
            mOnBufferingUpdateListener.onBufferingUpdate(this, percent);
    }

    protected final void notifyOnSeekComplete() {
        if (mOnSeekCompleteListener != null)
            mOnSeekCompleteListener.onSeekComplete(this);
    }
    
    protected final void notifyOnExactlySeekComplete() {
        if (mOnExactlySeekCompleteListener != null)
            mOnExactlySeekCompleteListener.onExactlySeekComplete(this);
    }

    protected final void notifyOnVideoSizeChanged(int width, int height,
                                                  int sarNum, int sarDen) {
        if (mOnVideoSizeChangedListener != null)
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height,
                    sarNum, sarDen);
    }

    protected final boolean notifyOnError(int what, int extra) {
        return mOnErrorListener != null && mOnErrorListener.onError(this, what, extra);
    }

    protected final boolean notifyOnInfo(int what, int extra) {
        return mOnInfoListener != null && mOnInfoListener.onInfo(this, what, extra);
    }

    /**
     * Default constructor. Consider using one of the create() methods for
     * synchronously instantiating a VideoPlayer from a Uri or resource.
     * <p>
     * When done with the VideoPlayer, you should call {@link #release()}, to
     * free the resources. If not released, too many VideoPlayer instances
     * may result in an exception.
     * </p>
     */
    public VideoPlayer() {
    	initPlayer();
    }

    
    private void initPlayer() {
        initNativeOnce();

        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }
        native_setup(new WeakReference<VideoPlayer>(this));
    }

    private native void _setVideoSurface(Surface surface);

    
    public void setDisplay(SurfaceHolder sh) {
        mSurfaceHolder = sh;
        Surface surface;
        if (sh != null) {
            surface = sh.getSurface();
        } else {
            surface = null;
        }
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }
    public void setSurface(Surface surface) {
        if (mScreenOnWhilePlaying && surface != null) {
            LSLog.e(
                    "setScreenOnWhilePlaying(true) is ineffective for Surface");
        }
        mSurfaceHolder = null;
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }
    public void setDataSource(Context context, Uri uri)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(context, uri, null);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void setDataSource(Context context, Uri uri, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            setDataSource(uri.getPath());
            return;
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                && Settings.AUTHORITY.equals(uri.getAuthority())) {
            uri = RingtoneManager.getActualDefaultRingtoneUri(context,
                    RingtoneManager.getDefaultType(uri));
            if (uri == null) {
                throw new FileNotFoundException("Failed to resolve default ringtone");
            }
        }

        AssetFileDescriptor fd = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            fd = resolver.openAssetFileDescriptor(uri, "r");
            if (fd == null) {
                return;
            }
            if (fd.getDeclaredLength() < 0) {
                setDataSource(fd.getFileDescriptor());
            } else {
                setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
            }
            return;
        } catch (SecurityException ignored) {
        } catch (IOException ignored) {
        } finally {
            if (fd != null) {
                fd.close();
            }
        }
        setDataSource(uri.toString(), headers);
    }

    public void setDataSource(String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mDataSource = path;
        _setDataSource(path, null, null);
    }

    public void setDataSource(String path, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException
    {
        if (headers != null && !headers.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for(Map.Entry<String, String> entry: headers.entrySet()) {
                sb.append(entry.getKey());
                sb.append(":");
                String value = entry.getValue();
                if (!TextUtils.isEmpty(value))
                    sb.append(entry.getValue());
                sb.append("\r\n");
                setOption(OPT_CATEGORY_FORMAT, "headers", sb.toString());
            }
        }
        setDataSource(path);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void setDataSource(FileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            int native_fd = -1;
            try {
                Field f = fd.getClass().getDeclaredField("descriptor"); //NoSuchFieldException
                f.setAccessible(true);
                native_fd = f.getInt(fd); //IllegalAccessException
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            _setDataSourceFd(native_fd);
        } else {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.dup(fd);
            try {
                _setDataSourceFd(pfd.getFd());
            } finally {
                pfd.close();
            }
        }
    }

  
    private void setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException {
        setDataSource(fd);
    }

    public void setDataSource(IMediaDataSource mediaDataSource)
            throws IllegalArgumentException, SecurityException, IllegalStateException {
        _setDataSource(mediaDataSource);
    }

    private native void _setDataSource(String path, String[] keys, String[] values)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    private native void _setDataSourceFd(int fd)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    private native void _setDataSource(IMediaDataSource mediaDataSource)
            throws IllegalArgumentException, SecurityException, IllegalStateException;

    public String getDataSource() {
        return mDataSource;
    }

    public void prepareAsync() throws IllegalStateException {
        _prepareAsync();
    }

    public native void _prepareAsync() throws IllegalStateException;

    
    public void start() throws IllegalStateException {
        stayAwake(true);
        _start();
    }

    private native void _start() throws IllegalStateException;

    public void stop() throws IllegalStateException {
        stayAwake(false);
        _stop();
    }

    private native void _stop() throws IllegalStateException;

    public void pause() throws IllegalStateException {
        stayAwake(false);
        _pause();
    }

    private native void _pause() throws IllegalStateException;
    
    /**
     * 设置是否精确seek;
     * @param is
     */
    public void setExactlySeekEnable(boolean is)
    {
    	int en=is?1:0;
    	_setAccurateSeekEnable(en, 5000);
    }
    /**
     * [新增]
     */
    public void setSpeedEnable()
    {
    	_setSpeedEnable();
    }
    
    public void setSpeedPitchEnable()
    {
    	_setSpeedPitchEnable();
    }
    public void setSpeed(float rate)
    {
    	_setSpeed(rate);
    }
    /**
     * 获取当前画面位置,单位毫秒
     * ms
     * @return 
     */
    public long getCurrentFramePosition()
    {
    	return getCurrentVideoFramePts();
    }
    /**
     * 获取当前正在显示这一帧画面的时间戳.
     * 
     * 此时间戳为视频本身这一帧的原始时间戳, 不随播放快慢影响;
     * @return  时间戳, 单位毫秒 1s=1000ms
     */
    private native long getCurrentVideoFramePts();
    /**
     * 是否加速seek, 
     * @param selected 0不加速, 1加速;
     * @param seektimeOut  加速seek的超时时间;默认最大5000ms;
     */
    private native void _setAccurateSeekEnable(int selected,int seektimeOut);
    /**
     * 是否使能 播放速度, 
     * 
     */
    private native void _setSpeedEnable();
    /**
     * 是否在 播放速度 ,加减速的时候, 变调.
     */
    private native void _setSpeedPitchEnable();
    /**
     * 范围是0.5---2.0;
     * 0.5是最慢
     * 2.0是最快.
     * @param rate
     */
    private native void _setSpeed(float rate);
    
    private native void _seekback100() throws IllegalStateException;
    private native void _seekfront100() throws IllegalStateException;
    
    

    @SuppressLint("Wakelock")
    public void setWakeMode(Context context, int mode) {
        boolean washeld = false;
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                washeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        PowerManager pm = (PowerManager) context
                .getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE,
                VideoPlayer.class.getName());
        mWakeLock.setReferenceCounted(false);
        if (washeld) {
            mWakeLock.acquire();
        }
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (mScreenOnWhilePlaying != screenOn) {
            mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
    }

    @SuppressLint("Wakelock")
    private void stayAwake(boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
        }
    }

    public void selectTrack(int track) {
        _setStreamSelected(track, true);
    }

    public void deselectTrack(int track) {
        _setStreamSelected(track, false);
    }

    private native void _setStreamSelected(int stream, boolean select);

    /**
     * 如果视频旋转90或270度,这里等于高度;
     * @return
     */
    public int getVideoWidth() {
        return mVideoWidth;
    }

    /**
     * 如果视频旋转90或270度,这里等于
     * @return
     */
    public int getVideoHeight() {
        return mVideoHeight;
    }

    public int getVideoSarNum() {
        return mVideoSarNum;
    }

    public int getVideoSarDen() {
        return mVideoSarDen;
    }

    public native boolean isPlaying();

    public native void seekTo(long msec) throws IllegalStateException;

    /**
     * 获取当前播放器进度. 单位微秒
     * 此时间进度, 会随着设置加减速而变化.
     * 如果您想获取到视频本身的原始时间戳, 则通过 {@link #getCurrentFramePosition()}来得到;
     * 比如:视频本身20s, 加速一倍播放, 则最终播放的时间点是10s左右;
     * @return
     */
    public native long getCurrentPosition();


    public native long setLanSongPosition();
    
    /**
     * 视频的总长度, 单位毫秒.
     * @return
     */
    public native long getDuration();

    public void release() {
        stayAwake(false);
        updateSurfaceScreenOn();
        resetListeners();
        _release();
    }

    private native void _release();

    public void reset() {
        stayAwake(false);
        _reset();
        // make sure none of the listeners get called anymore
        mEventHandler.removeCallbacksAndMessages(null);

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    private native void _reset();

    public void setLooping(boolean looping) {
        int loopCount = looping ? 0 : 1;
        setOption(OPT_CATEGORY_PLAYER, "loop", loopCount);
        _setLoopCount(loopCount);
    }

    private native void _setLoopCount(int loopCount);

    public boolean isLooping() {
        int loopCount = _getLoopCount();
        return loopCount != 1;
    }

    private native int _getLoopCount();

    public float getVideoOutputFramesPerSecond() {
        return _getPropertyFloat(PROP_FLOAT_VIDEO_OUTPUT_FRAMES_PER_SECOND, 0.0f);
    }

    public float getVideoDecodeFramesPerSecond() {
        return _getPropertyFloat(PROP_FLOAT_VIDEO_DECODE_FRAMES_PER_SECOND, 0.0f);
    }

    private native float _getPropertyFloat(int property, float defaultValue);
    private native void  _setPropertyFloat(int property, float value);
    private native long  _getPropertyLong(int property, long defaultValue);
    private native void  _setPropertyLong(int property, long value);

    public native void setVolume(float leftVolume, float rightVolume);

    public native int getAudioSessionId();

    public void setLogEnabled(boolean enable) {
        // do nothing
    }
    public boolean isPlayable() {
        return true;
    }

    private native String _getVideoCodecInfo();
    private native String _getAudioCodecInfo();

    public void setOption(int category, String name, String value)
    {
        _setOption(category, name, value);
    }

    public void setOption(int category, String name, long value)
    {
        _setOption(category, name, value);
    }

    /**
     * 类似ffplay后面的各种参数一样, 是通过这里传递过去的.  TODO 没有完全测试.在ff_ffplay_options.h中的ffp_context_options中,暂时不支持af和vf
     * @param category
     * @param name
     * @param value
     */
    private native void _setOption(int category, String name, String value);
    private native void _setOption(int category, String name, long value);

    public Bundle getMediaMeta() {
        return _getMediaMeta();
    }
    private native Bundle _getMediaMeta();

    public static String getColorFormatName(int mediaCodecColorFormat) {
        return _getColorFormatName(mediaCodecColorFormat);
    }

    private static native String _getColorFormatName(int mediaCodecColorFormat);


    private static native void native_init();

    private native void native_setup(Object VideoPlayer_this);

    private native void native_finalize();

    private native void native_message_loop(Object VideoPlayer_this);

    protected void finalize() throws Throwable {
        super.finalize();
        native_finalize();
    }

    private static class EventHandler extends Handler {
        private final WeakReference<VideoPlayer> mWeakPlayer;

        public EventHandler(VideoPlayer mp, Looper looper) {
            super(looper);
            mWeakPlayer = new WeakReference<VideoPlayer>(mp);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoPlayer player = mWeakPlayer.get();
            if (player == null || player.mNativeMediaPlayer == 0) {
                LSLog.e(
                        "VideoPlayer went away with unhandled events");
                return;
            }

//            Log.i(TAG,"得到的msge is :"+ msg.what);

            switch (msg.what) {
            case MEDIA_PREPARED:
                player.notifyOnPrepared();
                return;

            case MEDIA_PLAYBACK_COMPLETE:
                player.notifyOnCompletion();
                player.stayAwake(false);
                return;
            case VIDEOEDIT_EVENT_COMPLETE:
            	return ;
            case MEDIA_BUFFERING_UPDATE:
                long bufferPosition = msg.arg1;
                if (bufferPosition < 0) {
                    bufferPosition = 0;
                }

                long percent = 0;
                long duration = player.getDuration();
                if (duration > 0) {
                    percent = bufferPosition * 100 / duration;
                }
                if (percent >= 100) {
                    percent = 100;
                }
                player.notifyOnBufferingUpdate((int)percent);
                return;

            case MEDIA_SEEK_COMPLETE:
                player.notifyOnSeekComplete();
                return;

            case MEDIA_SET_VIDEO_SIZE:
                player.mVideoWidth = msg.arg1;
                player.mVideoHeight = msg.arg2;
                player.notifyOnVideoSizeChanged(player.mVideoWidth, player.mVideoHeight,
                        player.mVideoSarNum, player.mVideoSarDen);
                return;

            case MEDIA_ERROR:
                LSLog.e( "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                if (!player.notifyOnError(msg.arg1, msg.arg2)) {
                    player.notifyOnCompletion();
                }
                player.stayAwake(false);
                return;

            case MEDIA_INFO:
            	if(msg.arg1==MEDIA_INFO_MEDIA_ACCURATE_SEEK_COMPLETE){
            		  player.notifyOnExactlySeekComplete();
            	}else{
                    player.notifyOnInfo(msg.arg1, msg.arg2);
            	}
                return;
            case MEDIA_TIMED_TEXT:
                // do nothing
                break;

            case MEDIA_NOP: // interface test message - ignore
                break;

            case MEDIA_SET_VIDEO_SAR:
                player.mVideoSarNum = msg.arg1;
                player.mVideoSarDen = msg.arg2;
                player.notifyOnVideoSizeChanged(player.mVideoWidth, player.mVideoHeight,
                        player.mVideoSarNum, player.mVideoSarDen);
                break;
             case LANSONG_FRAME_UPDATE:
                 player.notifyFrameUpdate(msg.arg1);
                 break;
            default:
                LSLog.e( "Unknown message type " + msg.what);
            }
        }
    }

    /*
     * Called from native code when an interesting event happens. This method
     * just uses the EventHandler system to post the event back to the main app
     * thread. We use a weak reference to the original VideoPlayer object so
     * that the native code is safe from the object disappearing from underneath
     * it. (This is the cookie passed to native_setup().)
     */
    private static void postEventFromNative(Object weakThiz, int what,
            int arg1, int arg2, Object obj) {
        if (weakThiz == null)
            return;

        @SuppressWarnings("rawtypes")
        VideoPlayer mp = (VideoPlayer) ((WeakReference) weakThiz).get();
        if (mp == null) {
            return;
        }

        if (what == MEDIA_INFO && arg1 == MEDIA_INFO_STARTED_AS_NEXT) {
            // this acquires the wakelock if needed, and sets the client side
            // state
            mp.start();
        }
        if (mp.mEventHandler != null) {
            Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            mp.mEventHandler.sendMessage(m);
        }
    }

    /*
     * ControlMessage
     */

    private OnControlMessageListener mOnControlMessageListener;
    public void setOnControlMessageListener(OnControlMessageListener listener) {
        mOnControlMessageListener = listener;
    }

    public interface OnControlMessageListener {
        String onControlResolveSegmentUrl(int segment);
    }

    /*
     * NativeInvoke
     */

    private OnNativeInvokeListener mOnNativeInvokeListener;
    public void setOnNativeInvokeListener(OnNativeInvokeListener listener) {
        mOnNativeInvokeListener = listener;
    }

    public interface OnNativeInvokeListener {
        int ON_CONCAT_RESOLVE_SEGMENT = 0x10000;
        int ON_TCP_OPEN = 0x10001;
        int ON_HTTP_OPEN = 0x10002;
        // int ON_HTTP_RETRY = 0x10003;
        int ON_LIVE_RETRY = 0x10004;

        String ARG_URL = "url";
        String ARG_SEGMENT_INDEX = "segment_index";
        String ARG_RETRY_COUNTER = "retry_counter";

        boolean onNativeInvoke(int what, Bundle args);
    }
/**
 * 底层调用.
 * @param weakThiz
 * @param what
 * @param args
 * @return
 */
    private static boolean onNativeInvoke(Object weakThiz, int what, Bundle args) {
//        Log.ifmt(TAG, "onNativeInvoke %d", what);
        
        if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
            throw new IllegalStateException("<null weakThiz>.onNativeInvoke()");

        @SuppressWarnings("unchecked")
        WeakReference<VideoPlayer> weakPlayer = (WeakReference<VideoPlayer>) weakThiz;
        VideoPlayer player = weakPlayer.get();
        if (player == null)
            throw new IllegalStateException("<null weakPlayer>.onNativeInvoke()");

        OnNativeInvokeListener listener = player.mOnNativeInvokeListener;
        if (listener != null && listener.onNativeInvoke(what, args))
            return true;

        switch (what) {
            case OnNativeInvokeListener.ON_CONCAT_RESOLVE_SEGMENT: {
                OnControlMessageListener onControlMessageListener = player.mOnControlMessageListener;
                if (onControlMessageListener == null)
                    return false;

                int segmentIndex = args.getInt(OnNativeInvokeListener.ARG_SEGMENT_INDEX, -1);
                if (segmentIndex < 0)
                    throw new InvalidParameterException("onNativeInvoke(invalid segment index)");

                String newUrl = onControlMessageListener.onControlResolveSegmentUrl(segmentIndex);
                if (newUrl == null)
                    throw new RuntimeException(new IOException("onNativeInvoke() = <NULL newUrl>"));

                args.putString(OnNativeInvokeListener.ARG_URL, newUrl);
                return true;
            }
            default:
                return false;
        }
    }


    public interface OnMediaCodecSelectListener {
        String onMediaCodecSelect(VideoPlayer mp, String mimeType, int profile, int level);
    }
    private OnMediaCodecSelectListener mOnMediaCodecSelectListener;
    public void setOnMediaCodecSelectListener(OnMediaCodecSelectListener listener) {
        mOnMediaCodecSelectListener = listener;
    }

    public void resetListeners() {
    	 mOnPreparedListener = null;
         mOnBufferingUpdateListener = null;
         mOnCompletionListener = null;
         mOnSeekCompleteListener = null;
         mOnVideoSizeChangedListener = null;
         mOnErrorListener = null;
         mOnInfoListener = null;
        mOnMediaCodecSelectListener = null;
    }

    public static native void native_profileBegin(String libName);
    public static native void native_profileEnd();
    public static native void native_setLogLevel(int level);

}
