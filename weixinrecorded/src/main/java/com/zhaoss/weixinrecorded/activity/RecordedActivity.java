package com.zhaoss.weixinrecorded.activity;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lansosdk.videoeditor.LanSoEditor;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.VideoEditor;
import com.lansosdk.videoeditor.onVideoEditorProgressListener;
import com.zhaoss.weixinrecorded.R;
import com.zhaoss.weixinrecorded.util.AudioRecordUtil;
import com.zhaoss.weixinrecorded.util.AvcEncoder;
import com.zhaoss.weixinrecorded.util.CameraHelp;
import com.zhaoss.weixinrecorded.util.MyVideoEditor;
import com.zhaoss.weixinrecorded.util.RxJavaUtil;
import com.zhaoss.weixinrecorded.view.LineProgressView;
import com.zhaoss.weixinrecorded.view.RecordView;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 仿微信录制视频
 * 基于ffmpeg视频编译
 * Created by zhaoshuang on 19/6/18.
 */
public class RecordedActivity extends BaseActivity {

    public static final String INTENT_PATH = "intent_path";
    public static final int REQUEST_CODE_KEY = 100;

    public static final float MAX_VIDEO_TIME = 10f*1000;  //最大录制时间
    public static final float MIN_VIDEO_TIME = 2f*1000;  //最小录制时间

    private SurfaceView surfaceView;
    private RecordView recordView;
    private ImageView iv_delete;
    private ImageView iv_next;
    private ImageView iv_change_camera;
    private LineProgressView lineProgressView;
    private ImageView iv_flash_video;
    private TextView editorTextView;

    private ArrayList<String> segmentList = new ArrayList<>();//分段视频地址
    private ArrayList<String> aacList = new ArrayList<>();//分段音频地址
    private ArrayList<Long> timeList = new ArrayList<>();//分段录制时间

    //是否在录制视频
    private AtomicBoolean isRecordVideo = new AtomicBoolean(false);
    private CameraHelp mCameraHelp = new CameraHelp();
    private AudioRecordUtil audioRecordUtil;
    private ArrayBlockingQueue<byte[]> mYUVQueue = new ArrayBlockingQueue<>(10);
    private SurfaceHolder mSurfaceHolder;
    private AvcEncoder mAvcCodec;
    private MyVideoEditor mVideoEditor = new MyVideoEditor();

    private int executeCount;//总编译次数
    private float executeProgress;//编译进度
    private String audioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_recorded);

        LanSoEditor.initSDK(this, null);
        LanSongFileUtil.setFileDir("/sdcard/WeiXinRecorded/"+System.currentTimeMillis()+"/");

        initUI();
        initData();
        initMediaRecorder();
    }

    private void initUI() {

        surfaceView = findViewById(R.id.surfaceView);
        recordView = findViewById(R.id.recordView);
        iv_delete = findViewById(R.id.iv_delete);
        iv_next = findViewById(R.id.iv_next);
        iv_flash_video = findViewById(R.id.iv_flash_video);
        iv_change_camera = findViewById(R.id.iv_camera_mode);
        lineProgressView =  findViewById(R.id.lineProgressView);

        surfaceView.post(new Runnable() {
            @Override
            public void run() {
                int width = surfaceView.getWidth();
                int height = surfaceView.getHeight();
                float viewRatio = width*1f/height;
                float videoRatio = 9f/16f;
                ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
                if(viewRatio > videoRatio){
                    layoutParams.height = (int) (width/viewRatio);
                }else{
                    layoutParams.width = (int) (height*viewRatio);
                }
                surfaceView.setLayoutParams(layoutParams);
            }
        });
    }

    private void initMediaRecorder() {

        mCameraHelp.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if(isRecordVideo.get()){
                    if (mYUVQueue.size() >= 10) {
                        mYUVQueue.poll();
                    }
                    mYUVQueue.add(data);
                }
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mSurfaceHolder = holder;
                mCameraHelp.openCamera(mContext, Camera.CameraInfo.CAMERA_FACING_BACK, mSurfaceHolder);
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraHelp.release();
            }
        });

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraHelp.callFocusMode();
            }
        });

        mVideoEditor.setOnProgessListener(new onVideoEditorProgressListener() {
            @Override
            public void onProgress(VideoEditor v, int percent) {
                if(percent==100){
                    executeProgress++;
                }
                int pro = (int) (executeProgress/executeCount*100);
                editorTextView.setText("视频编辑中"+pro+"%");
            }
        });
    }

    private void initData() {

        lineProgressView.setMinProgress(MIN_VIDEO_TIME / MAX_VIDEO_TIME);
        recordView.setOnGestureListener(new RecordView.OnGestureListener() {
            @Override
            public void onDown() {
                //长按录像
                isRecordVideo.set(true);
                startRecord();
            }
            @Override
            public void onUp() {
                if(isRecordVideo.get()){
                    isRecordVideo.set(false);
                    upEvent();
                }
            }
            @Override
            public void onClick() {

            }
        });

        iv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSegment();
            }
        });

        iv_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorTextView = showProgressDialog();
                executeCount = segmentList.size()+4;
                RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<String>() {
                    @Override
                    public String doInBackground(){
                        return h264ToMp4();
                    }
                    @Override
                    public void onFinish(String result) {
                        closeProgressDialog();
                        Intent intent = new Intent(mContext, EditVideoActivity.class);
                        intent.putExtra(INTENT_PATH, result);
                        startActivityForResult(intent, REQUEST_CODE_KEY);
                    }
                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        closeProgressDialog();
                        Toast.makeText(getApplicationContext(), "视频编辑失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        iv_flash_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraHelp.changeFlash();
                if (mCameraHelp.isFlashOpen()) {
                    iv_flash_video.setImageResource(R.mipmap.video_flash_open);
                } else {
                    iv_flash_video.setImageResource(R.mipmap.video_flash_close);
                }
            }
        });

        iv_change_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraHelp.getCameraId() == Camera.CameraInfo.CAMERA_FACING_BACK){
                    mCameraHelp.openCamera(mContext, Camera.CameraInfo.CAMERA_FACING_FRONT, mSurfaceHolder);
                }else{
                    mCameraHelp.openCamera(mContext, Camera.CameraInfo.CAMERA_FACING_BACK, mSurfaceHolder);
                }
                iv_flash_video.setImageResource(R.mipmap.video_flash_close);
            }
        });
    }

    public String h264ToMp4(){

        ArrayList<String> tsList = new ArrayList<>();
        for (int x=0; x<segmentList.size(); x++){
            String tsPath = LanSongFileUtil.DEFAULT_DIR+System.currentTimeMillis()+".ts";
            mVideoEditor.h264ToTs(segmentList.get(x), tsPath);
            tsList.add(tsPath);
        }

        String aacPath = LanSongFileUtil.DEFAULT_DIR+System.currentTimeMillis()+".aac";
        mVideoEditor.concatAudio(aacList.toArray(new String[]{}), aacPath);

        String mp4Path = mVideoEditor.executeConvertTsToMp4(tsList.toArray(new String[]{}));
        mp4Path = mVideoEditor.executeSetVideoMetaAngle(mp4Path, mCameraHelp.getCameraDisplayOrientation(mContext, Camera.CameraInfo.CAMERA_FACING_BACK));
        mp4Path = mVideoEditor.executeVideoMergeAudio(mp4Path, aacPath);

        return mp4Path;
    }

    private long videoDuration;
    private long recordTime;
    private String videoPath;
    private void startRecord(){

        mAvcCodec = new AvcEncoder(mCameraHelp.getWidth(), mCameraHelp.getHeight(), mYUVQueue);
        videoPath = LanSongFileUtil.DEFAULT_DIR+System.currentTimeMillis()+".h264";
        mAvcCodec.startEncoder(videoPath, mCameraHelp.getCameraId()== Camera.CameraInfo.CAMERA_FACING_FRONT);

        audioRecordUtil = new AudioRecordUtil();
        audioPath = LanSongFileUtil.DEFAULT_DIR+System.currentTimeMillis()+".aac";
        audioRecordUtil.startRecord(audioPath);

        videoDuration = 0;
        lineProgressView.setSplit();
        recordTime = System.currentTimeMillis();
        RxJavaUtil.loop(30, new RxJavaUtil.OnRxLoopListener() {
            @Override
            public Boolean takeWhile(){
                return mAvcCodec.isRunning();
            }
            @Override
            public void onExecute() {
                long currentTime = System.currentTimeMillis();
                videoDuration += currentTime - recordTime;
                recordTime = currentTime;
                long countTime = videoDuration;
                for (long time : timeList) {
                    countTime += time;
                }
                if (countTime <= MAX_VIDEO_TIME) {
                    lineProgressView.setProgress(countTime/ MAX_VIDEO_TIME);
                }
            }
            @Override
            public void onFinish() {
                segmentList.add(videoPath);
                aacList.add(audioPath);
                timeList.add(videoDuration);
            }
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                lineProgressView.removeSplit();
            }
        });
    }

    private void upEvent(){
        if(mAvcCodec != null) {
            mAvcCodec.stopEncoder();
            mAvcCodec = null;
        }
        if(audioRecordUtil != null) {
            audioRecordUtil.stopRecord();
            audioRecordUtil = null;
        }
        initRecorderState();
    }

    private void deleteSegment(){

        showConfirm("确认删除上一段视频?", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeProgressDialog();

                if(segmentList.size()>0 && timeList.size()>0) {
                    segmentList.remove(segmentList.size() - 1);
                    aacList.remove(aacList.size() - 1);
                    timeList.remove(timeList.size() - 1);
                    lineProgressView.removeSplit();
                }
                if(lineProgressView.getSplitCount() == 0) {
                    iv_delete.setVisibility(View.INVISIBLE);
                    iv_next.setVisibility(View.INVISIBLE);
                    iv_flash_video.setVisibility(View.VISIBLE);
                }else if(lineProgressView.getProgress()* MAX_VIDEO_TIME < MIN_VIDEO_TIME){
                    iv_next.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    /**
     * 初始化视频拍摄状态
     */
    private void initRecorderState(){

        recordView.setTouch(true);
        if (lineProgressView.getSplitCount() > 0) {
            iv_delete.setVisibility(View.VISIBLE);
        }else{
            iv_delete.setVisibility(View.GONE);
        }

        if (lineProgressView.getProgress()* MAX_VIDEO_TIME < MIN_VIDEO_TIME) {
            iv_next.setVisibility(View.GONE);
        } else {
            iv_next.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 清除录制信息
     */
    private void cleanRecord(){

        recordView.initState();
        lineProgressView.cleanSplit();
        segmentList.clear();
        aacList.clear();
        timeList.clear();

        executeCount = 0;
        executeProgress = 0;

        iv_delete.setVisibility(View.INVISIBLE);
        iv_next.setVisibility(View.INVISIBLE);
        iv_flash_video.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cleanRecord();
        if(mCameraHelp != null){
            mCameraHelp.release();
        }
        if(mAvcCodec != null){
            mAvcCodec.stopEncoder();
        }
        if(audioRecordUtil != null){
            audioRecordUtil.stopRecord();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==RESULT_OK && data!=null){
            if(requestCode == REQUEST_CODE_KEY){
                Intent intent = new Intent();
                intent.putExtra(INTENT_PATH, data.getStringExtra(INTENT_PATH));
                setResult(RESULT_OK, intent);
                finish();
            }
        }else{
            cleanRecord();
            initRecorderState();
        }
    }
}
