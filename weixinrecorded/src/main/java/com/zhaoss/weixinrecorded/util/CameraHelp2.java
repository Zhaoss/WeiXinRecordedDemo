package com.zhaoss.weixinrecorded.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;
import android.widget.ImageView;

import com.lansosdk.videoeditor.LanSongFileUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by zhaoshuang on 2018/7/20.
 */

public class CameraHelp2 {

    private Context mContext;
    private int defaultSize = 1920 * 1080;
    private CameraManager cameraManager;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder previewRequestBuilder;
    private SurfaceTexture surfaceTexture;
    private Handler mHandler;
    private int[] previewSize;
    private CameraCaptureSession mCameraCaptureSession;
    private ImageReader mImageReader;
    private int mOrientation;

    public CameraHelp2(Context context){

        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("CameraHelp2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @SuppressLint("MissingPermission")
    public void openCamera(SurfaceTexture surfaceTexture, final ImageView imageView) {

        try {
            this.surfaceTexture = surfaceTexture;
            String[] ids = cameraManager.getCameraIdList();
            String cameraId = "";
            for (String id : ids) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if(cameraOrientation == CameraCharacteristics.LENS_FACING_BACK){
                    cameraId = id;
                    mOrientation = sensorOrientation;
                }
            }
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = getPreviewSize(map);
            mImageReader = ImageReader.newInstance(previewSize[0], previewSize[1], ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] imgs = new byte[buffer.remaining()];
                    buffer.get(imgs);
                    try {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imgs, 0, imgs.length);
                        Matrix matrix = new Matrix();
                        matrix.setRotate(mOrientation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);

                        String photoPath = LanSongFileUtil.DEFAULT_DIR+System.currentTimeMillis()+".jpeg";
                        FileOutputStream fos = new FileOutputStream(photoPath);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    image.close();
                }
            }, mHandler);

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                    createCameraSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    mCameraDevice = null;
                }
            }, mHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shotPhoto(){

        try {
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mOrientation);
            CaptureRequest captureRequest = captureRequestBuilder.build();
            mCameraCaptureSession.capture(captureRequest, null, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void openFlash(){
        try {
            previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            CaptureRequest previewRequest = previewRequestBuilder.build();
            mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeFlash(){
        try {
            previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            CaptureRequest previewRequest = previewRequestBuilder.build();
            mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraSession() {

        try {
            previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface previewSurface = new Surface(surfaceTexture);
            previewRequestBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        mCameraCaptureSession = cameraCaptureSession;
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        cameraCaptureSession.setRepeatingRequest(previewRequest, null, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //摄像大小
    private int[] getPreviewSize(StreamConfigurationMap map){

        Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);

        int[] previewSize = new int[2];

        boolean flag = false;
        for (Size size : sizes) {
            if (size.getWidth() * size.getHeight() == defaultSize) {
                previewSize[0] = size.getWidth();
                previewSize[1] = size.getHeight();
                flag = true;
            }
        }
        if (!flag) {
            int difference = 0;
            int position = 0;
            for (int x=0; x<sizes.length; x++) {
                Size size = sizes[x];
                int n = Math.abs(defaultSize - size.getWidth() * size.getHeight());
                if (x == 0 || difference > n) {
                    difference = n;
                    position = x;
                }
            }
            previewSize[0] = sizes[position].getWidth();
            previewSize[1] = sizes[position].getHeight();
        }

        return previewSize;
    }

    private byte[] getYUVI420(Image image){

        int width = image.getWidth();
        int height = image.getHeight();

        byte[] yuvI420 = new byte[image.getWidth()*image.getHeight()*3/2];

        byte[] yData = new byte[image.getPlanes()[0].getBuffer().remaining()];
        byte[] uData = new byte[image.getPlanes()[1].getBuffer().remaining()];
        byte[] vData = new byte[image.getPlanes()[2].getBuffer().remaining()];
        image.getPlanes()[0].getBuffer().get(yData);
        image.getPlanes()[1].getBuffer().get(uData);
        image.getPlanes()[2].getBuffer().get(vData);

        System.arraycopy(yData, 0, yuvI420, 0, yData.length);
        int index = yData.length;

        for (int r = 0; r < height / 2; ++r) {
            for (int c = 0; c < width; c += 2) { //各一个byte存一个U值和V值
                yuvI420[index++] = uData[r * width + c];
            }
        }
        for (int r = 0; r < height / 2; ++r) {
            for (int c = 0; c < width; c += 2) { //各一个byte存一个U值和V值
                yuvI420[index++] = vData[r * width + c];
            }
        }
        return yuvI420;
    }
}
