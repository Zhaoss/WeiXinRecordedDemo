package com.zhaoss.weixinrecorded.util;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhaoshuang on 2018/7/20.
 */

public class CameraHelp {

    //默认录制大小
    private int[] previewSize = new int[2];
    private final static int defaultSize = 1280*720;
    private final static float defaultRatio = 9f/16f;

    private Camera mCamera;
    private Camera.PreviewCallback previewCallback;
    private int displayOrientation;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean isFlashOpen;

    public void openCamera(Activity activity, int cameraId, SurfaceHolder surfaceHolder){

        try {
            if(mCamera != null){
                release();
            }
            this.cameraId = cameraId;

            mCamera = Camera.open(cameraId);
            displayOrientation = getCameraDisplayOrientation(activity, cameraId);
            mCamera.setDisplayOrientation(displayOrientation);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.setPreviewCallback(previewCallback);

            Camera.Parameters parameters = mCamera.getParameters();
            previewSize = getPreviewSize();
            parameters.setPreviewSize(previewSize[0], previewSize[1]);
            parameters.setFocusMode(getAutoFocus());
            parameters.setPictureFormat(ImageFormat.JPEG);
            parameters.setPreviewFormat(ImageFormat.NV21);

            mCamera.setParameters(parameters);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //自动对焦
    private String  getAutoFocus(){

        Camera.Parameters parameters = mCamera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (((Build.MODEL.startsWith("GT-I950")) || (Build.MODEL.endsWith("SCH-I959")) || (Build.MODEL.endsWith("MEIZU MX3")))
                && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            return Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            return Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
        } else {
            return Camera.Parameters.FOCUS_MODE_FIXED;
        }
    }

    //摄像大小
    private int[] getPreviewSize(){

        int[] previewSize = new int[2];
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();

        for (Camera.Size size : supportedPreviewSizes) {
            if ((size.width*size.height==defaultSize) && (size.width*1f/size.height==defaultRatio)) {
                previewSize[0] = size.width;
                previewSize[1] = size.height;
                return previewSize;
            }
        }

        int difference = 0;
        int position = 0;
        for (int x = 0; x < supportedPreviewSizes.size(); x++) {
            Camera.Size size = supportedPreviewSizes.get(x);
            int n = Math.abs(defaultSize - size.width * size.height);
            if (x == 0 || difference > n) {
                difference = n;
                position = x;
            }
        }
        Camera.Size size = supportedPreviewSizes.get(position);
        previewSize[0] = size.width;
        previewSize[1] = size.height;

        return previewSize;
    }

    public int getCameraId(){
        return cameraId;
    }

    public void changeFlash(){
        if(mCamera != null){
            Camera.Parameters parameters = mCamera.getParameters();
            String flashMode = parameters.getFlashMode();
            List<String> flashModeList = parameters.getSupportedFlashModes();
            if(!isFlashOpen){
                if (flashModeList!=null && flashModeList.contains("torch") && !"torch".equals(flashMode)) {
                    try{
                        parameters.setFlashMode("torch");
                        mCamera.setParameters(parameters);
                        isFlashOpen = true;
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }else{
                if (flashModeList != null && flashModeList.contains("off") && !"off".equals(flashMode)) {
                    try{
                        parameters.setFlashMode("off");
                        mCamera.setParameters(parameters);
                        isFlashOpen = false;
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public boolean isFlashOpen(){
        return isFlashOpen;
    }

    public String getFlashMode(){
        if(mCamera != null){
            Camera.Parameters parameters = mCamera.getParameters();
            return parameters.getFlashMode();
        }
        return "";
    }

    public void release(){
        if(mCamera != null){
            try {
                isFlashOpen = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public int getDisplayOrientation(){
        return displayOrientation;
    }

    public int getWidth(){
        return previewSize[0];
    }

    public int getHeight(){
        return previewSize[1];
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback){
        this.previewCallback = previewCallback;
    }

    public void callFocusMode(){

        try {
            List<String> focusModes = mCamera.getParameters().getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                focusOnTouch(previewSize[0]/2, previewSize[1]/2);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void focusOnTouch(int x, int y) {

        Rect rect = new Rect(x - 100, y - 100, x + 100, y + 100);

        int left = rect.left * 2000 / previewSize[0] - 1000;
        int top = rect.top * 2000 / previewSize[1] - 1000;
        int right = rect.right * 2000 / previewSize[0] - 1000;
        int bottom = rect.bottom * 2000 / previewSize[1] - 1000;

        // 如果超出了(-1000,1000)到(1000, 1000)的范围，则会导致相机崩溃
        if(left < -1000){
            left = 1000;
        }
        if(top < -1000){
            top = -1000;
        }
        if(right > 1000){
            right = 1000;
        }
        if(bottom > 1000){
            bottom = 1000;
        }

        final String focusMode = mCamera.getParameters().getFocusMode();

        Rect rect1 = new Rect(left, top, right, bottom);
        //先获取当前相机的参数配置对象
        Camera.Parameters parameters = mCamera.getParameters();
        //设置聚焦模式
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        if (parameters.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(rect1, 1000));
            parameters.setFocusAreas(focusAreas);
        }
        //先要取消掉进程中所有的聚焦功能
        mCamera.cancelAutoFocus();
        //一定要记得把相应参数设置给相机
        mCamera.setParameters(parameters);
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Camera.Parameters parame = camera.getParameters();
                parame.setFocusMode(focusMode);
                camera.setParameters(parame);
            }
        });
    }

    //得到摄像旋转角度
    private int getCameraDisplayOrientation(Activity activity, int cameraId) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }
}
