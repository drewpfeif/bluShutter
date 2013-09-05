package com.blushutter.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.view.Surface;
import android.view.WindowManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraHelper {

    private static final String LOG_TAG = MainActivity.LOG_TAG + ".CameraHelper";

    public static int getDisplayOrientationForCamera(Context context, int cameraId) {
        final int DEGREES_IN_CIRCLE = 360;
        int temp = 0;
        int previewOrientation = 0;

        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);

            int deviceOrientation = getDeviceOrientationDegrees(context);
            switch (cameraInfo.facing) {
                case Camera.CameraInfo.CAMERA_FACING_BACK:
                    temp = cameraInfo.orientation - deviceOrientation + DEGREES_IN_CIRCLE;
                    previewOrientation = temp % DEGREES_IN_CIRCLE;
                    break;
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    temp = cameraInfo.orientation + deviceOrientation % DEGREES_IN_CIRCLE;
                    previewOrientation = (DEGREES_IN_CIRCLE - temp) % DEGREES_IN_CIRCLE;
                    break;
            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in getDisplayOrientationForCamera: " + e.getMessage());
        }

        return previewOrientation;
    }

    public static int getDeviceOrientationDegrees(Context context) {
        int degrees = 0;

        try {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            int rotation = windowManager.getDefaultDisplay().getRotation();

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
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in getDeviceOrientationDegrees: " + e.getMessage());
        }

        return degrees;
    }

    public static File generateTimeStampPhotoFile() {

        // request the path to the ssx folder in the photo folder
        File photoFile = null;
        File outputDir = getPhotoDirectory();

        try {
            if (outputDir != null) {
                // create a file name for the photo based on current date/time
                String timestamp = new SimpleDateFormat("yyyyMMDD_HHmmss").format(new Date());
                String photoFileName = "IMG_" + timestamp + ".jpg";

                // create File instance representing the photo file
                // within the ssx subfolder of the photos folder
                photoFile = new File(outputDir, photoFileName);


            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in generateTimeStampPhotoFile: " + e.getMessage());
        }

        return  photoFile;

    }

    public static Uri generateTimeStampPhotoFileUri() {
        Uri photoFileUri = null;

        // request the path to the ssx folder in the photo folder
        File outputDir = getPhotoDirectory();

        try {
            if (outputDir != null) {
                // create a file name for the photo based on current date/time
                String timestamp = new SimpleDateFormat("yyyyMMDD_HHmmss").format(new Date());
                String photoFileName = "IMG_" + timestamp + ".jpg";

                // create File instance representing the photo file
                // within the ssx subfolder of the photos folder
                File photoFile = new File(outputDir, photoFileName);
                // convert the file path to a URI
                photoFileUri = Uri.fromFile(photoFile);
            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in generateTimeStampPhotoFileUri: " + e.getMessage());
        }

        return photoFileUri;
    }

    public static File getPhotoDirectory() {

        File outputDir = null;

        try {
            // Confirm that External Storage (SD Card) is mounted
            String externalStorageState = Environment.getExternalStorageState();
            if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {

                // request the folder where the photos are supposed to be stored
                File pictureDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

                // create a subfolder named ssx
                outputDir = new File(pictureDir, "ssx");
                if (!outputDir.exists()) {
                    if (!outputDir.mkdirs()) {
                        //Log.e(LOG_TAG, "Failed to create directory: " + outputDir.getAbsolutePath());
                        outputDir = null;
                    }
                }
            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in getPhotoDirectory: " + e.getMessage());
        }

        return  outputDir;
    }

    public static int getFacingCameraId(int facing) {
        int cameraId = MainActivity.NOT_SET;

        try {
            int nCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            for (int cameraInfoId = 0; cameraInfoId < nCameras; cameraInfoId++) {
                Camera.getCameraInfo(cameraInfoId, cameraInfo);
                if (cameraInfo.facing == facing) {
                    cameraId = cameraInfoId;
                    break;
                }
            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in getFacingCameraId: " + e.getMessage());
        }

        return  cameraId;
    }

    public static Camera openSelectedCamera(int cameraId, Activity activity) {

        Camera camera = null;
        if (cameraId != MainActivity.NOT_SET) {
            try {
                camera = Camera.open(cameraId);

                CameraPreview cameraPreview = (CameraPreview) activity.findViewById(R.id.cameraPreview);
                cameraPreview.connectCamera(camera);

                Camera.Parameters cameraParameters = camera.getParameters();

                Camera.Size size;

                ((MainActivity) activity).SupportedPictureSizes = cameraParameters.getSupportedPictureSizes();

                if (((MainActivity) activity).SelectedPictureSize != null) {
                    size = ((MainActivity) activity).SelectedPictureSize;
                }
                else {
                    size = cameraParameters.getPictureSize();
                    // this picture size seems to be too big
                    // getting OOM error when trying to decode they bytes
                    size.width = 2592;
                    size.height = 1944;
//                    size.width = 1024;
//                    size.height = 768;
                    ((MainActivity) activity).SelectedPictureSize = size;
                }

                int i = 0;
                for (Camera.Size pictureSize:((MainActivity) activity).SupportedPictureSizes) {
                    if (size.width == pictureSize.width && size.height == pictureSize.height) {
                        ((MainActivity) activity).SelectedPictureSizeIndex = i;
                        break;
                    }
                    i++;
                }

                cameraParameters.setPictureSize(size.width,size.height);
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                cameraParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);

                //Log.v(LOG_TAG, String.valueOf(cameraParameters.isSmoothZoomSupported()));

                ((MainActivity) activity).CameraParameters = cameraParameters;

//                ((MainActivity) activity)._currentZoom = currentZoom;
//                ((MainActivity) activity)._maxZoom = maxZoom;

                // smooth zoom is not supported on Galaxy Camera
                //camera.setZoomChangeListener((MainActivity)activity);

            } catch (Exception ex) {
                //Log.e(LOG_TAG, "Error opening camera: " + ex.getMessage());
            }

        }
        return  camera;
    }

    public static Camera releaseSelectedCamera(Camera camera, Activity activity) {
        if (camera != null) {

            try {

                CameraPreview cameraPreview = (CameraPreview) activity.findViewById(R.id.cameraPreview);
                cameraPreview.releaseCamera();

                camera.release();
                camera = null;
            }
            catch (Exception ex) {
                //Log.e(LOG_TAG, "Error releasing camera: " + ex.getMessage());
            }
        }

        return  camera;
    }
}
