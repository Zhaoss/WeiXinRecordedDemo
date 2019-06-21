package com.lansosdk.videoplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;

import com.lansosdk.box.LSLog;

import java.io.IOException;


/**
 *  注意!!!
 *  此代码仅仅是 VideoPlayer 的简易封装, 目的是让代码更清晰一些. 
 *  
 *  一切API 以 {@link VideoPlayer}中的为准.
 *  
 *
 */
public class VPlayer {
    private Uri mUri;

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;   //没有stop类型,因为stop就是release
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private int mCurrentState = STATE_IDLE;

    // All the stuff we need for playing and showing a video
    private VideoPlayer mMediaPlayer = null;
    private int mMainVideoWidth;
    private int mMainVideoHeight;
    
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoRotationDegree;
    
    private VideoPlayer.OnPlayerVideoSizeChangedListener mOnSizeChangedListener;
    private VideoPlayer.OnPlayerCompletionListener mOnCompletionListener;
    private VideoPlayer.OnPlayeFrameUpdateListener mOnPlayerFrameUpdateListener;


    private VideoPlayer.OnPlayerPreparedListener mOnPreparedListener;
    private VideoPlayer.OnPlayerErrorListener mOnErrorListener;
    private VideoPlayer.OnPlayerInfoListener mOnInfoListener;
    private VideoPlayer.OnPlayerSeekCompleteListener mOnSeekCompleteListener;
    
    
    
    private int mCurrentBufferPercentage;
    
    
    private int mSeekWhenPrepared;  // recording the seek position while preparing  
    private boolean mCanPause = true;
    private boolean mCanSeekBack = true;
    private boolean mCanSeekForward = true;

    private Context mAppContext;
    private int mVideoSarNum;
    private int mVideoSarDen;

    public VPlayer(Context context) {
        mAppContext = context.getApplicationContext();
        mMainVideoWidth = 0;
        mMainVideoHeight = 0;
        mCurrentState = STATE_IDLE;
    }
    public void setVideoPath(String path) {
    	if(mCurrentState == STATE_IDLE){
            mUri = Uri.parse(path);
            mSeekWhenPrepared = 0;
    	}
    }
    public void setVideoURI(Uri path) {
    	if(mCurrentState == STATE_IDLE){
            mUri =path;
            mSeekWhenPrepared = 0;
    	}
    }
   
   public void setSurface(Surface surface)
   {
		mMediaPlayer.setSurface(surface);
   }
   public void setSpeedEnable()
   {
	   if(mMediaPlayer!=null){
		   mMediaPlayer.setSpeedEnable();   
	   }
   }
   public void setSpeed(float speed)
   {
	   if(mMediaPlayer!=null){
		   mMediaPlayer.setSpeed(speed);   
	   }
   }
   public void setExactlySeekEnable(boolean is)
   {
	   if(mMediaPlayer!=null){
		   mMediaPlayer.setExactlySeekEnable(is);
	   }
   }
    public void prepareAsync() {
        if (mUri == null) {
            LSLog.e("mUri==mull, open video error.");
            return;
        }
        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        try {
            mMediaPlayer = createPlayer();
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            mMediaPlayer.setOnPlayeFrameUpdateListener(mOnPlayerFrameUpdateListener);
            
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mAppContext, mUri);

            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            LSLog.e( "Unable to open content: " + mUri);
            mCurrentState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            LSLog.e( "Unable to open content: " + mUri);
            mCurrentState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }
    }

    VideoPlayer.OnPlayerVideoSizeChangedListener mSizeChangedListener =
            new VideoPlayer.OnPlayerVideoSizeChangedListener() {
                public void onVideoSizeChanged(VideoPlayer mp, int width, int height, int sarNum, int sarDen) {
                    mMainVideoWidth = mp.getVideoWidth();
                    mMainVideoHeight = mp.getVideoHeight();
                    mVideoSarNum = mp.getVideoSarNum();
                    mVideoSarDen = mp.getVideoSarDen();
                    if (mMainVideoWidth != 0 && mMainVideoHeight != 0) {
                        if(mOnSizeChangedListener!=null)
                        	mOnSizeChangedListener.onVideoSizeChanged(mp, width, height, sarNum, sarDen);
                    }
                }
            };

            VideoPlayer.OnPlayerPreparedListener mPreparedListener = new VideoPlayer.OnPlayerPreparedListener() {
        public void onPrepared(VideoPlayer mp) {
            mCurrentState = STATE_PREPARED;

            mMainVideoWidth = mp.getVideoWidth();
            mMainVideoHeight = mp.getVideoHeight();
            
            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            
        }
    };

    private VideoPlayer.OnPlayerCompletionListener mCompletionListener =
            new VideoPlayer.OnPlayerCompletionListener() {
                public void onCompletion(VideoPlayer mp) {
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }
                }
   };

    private VideoPlayer.OnPlayerInfoListener mInfoListener =
            new VideoPlayer.OnPlayerInfoListener() {
                public boolean onInfo(VideoPlayer mp, int arg1, int arg2) {
                    if (mOnInfoListener != null) {
                     return   mOnInfoListener.onInfo(mp, arg1, arg2);
                    }
                    return true;
                }
            };

    private VideoPlayer.OnPlayerErrorListener mErrorListener =
            new VideoPlayer.OnPlayerErrorListener() {
                public boolean onError(VideoPlayer mp, int framework_err, int impl_err) {
                    mCurrentState = STATE_ERROR;
                    if (mOnErrorListener != null) {
                        if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                            return true;
                        }
                    }
                    return true;
                }
            };

    private VideoPlayer.OnPlayerBufferingUpdateListener mBufferingUpdateListener =
            new VideoPlayer.OnPlayerBufferingUpdateListener() {
                public void onBufferingUpdate(VideoPlayer mp, int percent) {
                    mCurrentBufferPercentage = percent;
                }
            };

  
    public void setOnPreparedListener(VideoPlayer.OnPlayerPreparedListener l) {
        mOnPreparedListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(VideoPlayer.OnPlayerCompletionListener l) {
        mOnCompletionListener = l;
    }
    public void setOnFrameUpateListener(VideoPlayer.OnPlayeFrameUpdateListener listener){
        mOnPlayerFrameUpdateListener=listener;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(VideoPlayer.OnPlayerErrorListener l) {
        mOnErrorListener = l;
    }
    
    
    public void setOnSeekCompleteListener(VideoPlayer.OnPlayerSeekCompleteListener l)
    {
    	mOnSeekCompleteListener=l;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public void setOnInfoListener(VideoPlayer.OnPlayerInfoListener l) {
        mOnInfoListener = l;
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }
    public void start() {
        if (isInPlaybackState()) {
        	mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }else if(mUri!=null && mCurrentState==STATE_IDLE){
        	setVideoURI(mUri);
        }
    }

    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
    }
    
    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }
    public void setLooping(boolean looping){
    	if(mMediaPlayer!=null)
    		mMediaPlayer.setLooping(looping);
    }
    
    public   boolean isLooping(){
    	 return (mMediaPlayer!=null) ? mMediaPlayer.isLooping(): false;
    }
    
    public void setVolume(float leftVolume, float rightVolume){
    	if(mMediaPlayer!=null)
    		mMediaPlayer.setVolume(leftVolume, rightVolume);
    }
    
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return -1;
    }

    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }
    public int setLanSongPosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.setLanSongPosition();
        }
        return 0;
    }

    public int getCurrentFramePosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentFramePosition();
        }
        return 0;
    }
    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    /**
     * 如果视频旋转90或270度,这里等于高度;
     * @return
     */
    public int getVideoWidth()
    {
    	return mMediaPlayer!=null? mMediaPlayer.getVideoWidth():0;
    }

    /**
     * 如果视频旋转90或270度,这里等于宽度.;
     * @return
     */
    public int getVideoHeight()
    {
    	return mMediaPlayer!=null? mMediaPlayer.getVideoHeight():0;
    }

    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    public boolean canPause() {
        return mCanPause;
    }

    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    public int getAudioSessionId() {
        return 0;
    }
    static final int AR_ASPECT_FIT_PARENT = 0; // without clip
    static final int AR_ASPECT_FILL_PARENT = 1; // may clip
    static final int AR_ASPECT_WRAP_CONTENT = 2;
    static final int AR_MATCH_PARENT = 3;
    static final int AR_16_9_FIT_PARENT = 4;
    static final int AR_4_3_FIT_PARENT = 5;

    private static final int[] s_allAspectRatio = {
            AR_ASPECT_FIT_PARENT,
            AR_ASPECT_FILL_PARENT,
            AR_ASPECT_WRAP_CONTENT,
            AR_16_9_FIT_PARENT,
            AR_4_3_FIT_PARENT};
    private int mCurrentAspectRatioIndex = 0;
    private int mCurrentAspectRatio = s_allAspectRatio[0];

    public int toggleAspectRatio() {
        mCurrentAspectRatioIndex++;
        mCurrentAspectRatioIndex %= s_allAspectRatio.length;
        mCurrentAspectRatio = s_allAspectRatio[mCurrentAspectRatioIndex];
        return mCurrentAspectRatio;
    }

    private VideoPlayer createPlayer() {
    	VideoPlayer mediaPlayer = null;

    	VideoPlayer player = null;
                if (mUri != null) {
                    player = new VideoPlayer();
                        player.setOption(VideoPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
                        player.setOption(VideoPlayer.OPT_CATEGORY_PLAYER, "overlay-format", VideoPlayer.SDL_FCC_RV32);
                        player.setOption(VideoPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
                        player.setOption(VideoPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
                        player.setOption(VideoPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
                        player.setOption(VideoPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
                        
                }
                mediaPlayer = player;
        return mediaPlayer;
    }
    /**
     * 可以获取mediaPlayer,然后如果后台操作,则可以把MediaPlayer放到service中进行.
     * @return
     */
    public VideoPlayer getMediaPlayer()
    {
    	return mMediaPlayer;
    }
}

