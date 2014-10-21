package com.blushutter.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraHelper {

    // for debugging...
    //private static final String LOG_TAG = AppConstants.LOG_TAG + ".CameraHelper";

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

    // Create a file for storing the photo and put a time stamp in the file name.
    public static File generateTimeStampPhotoFile() {

        // request the path to the bluShutter folder in the photo folder
        File photoFile = null;
        File outputDir = getPhotoDirectory();

        try {
            if (outputDir != null) {
                // create a file name for the photo based on current date/time
                String timestamp = new SimpleDateFormat("yyyyMMDD_HHmmss").format(new Date());
                String photoFileName = "IMG_" + timestamp + ".jpg";

                // create File instance representing the photo file
                // within the bluShutter subfolder of the photos folder
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

        // request the path to the bluShutter folder in the photo folder
        File outputDir = getPhotoDirectory();

        try {
            if (outputDir != null) {
                // create a file name for the photo based on current date/time
                String timestamp = new SimpleDateFormat("yyyyMMDD_HHmmss").format(new Date());
                String photoFileName = "IMG_" + timestamp + ".jpg";

                // create File instance representing the photo file
                // within the bluShutter subfolder of the photos folder
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

    // Create and return the "bluShutter" directory for storing photos locally.
    public static File getPhotoDirectory() {

        File outputDir = null;

        try {
            // Confirm that External Storage (SD Card) is mounted
            String externalStorageState = Environment.getExternalStorageState();
            if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {

                // request the folder where the photos are supposed to be stored
                File pictureDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

                // create a subfolder named bluShutter
                outputDir = new File(pictureDir, "bluShutter");
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

    // Get the camera based on the facing parameter passed in (front or back)
    public static int getFacingCameraId(int facing) {
        int cameraId = AppConstants.NOT_SET;

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

    // Get access to the device's camera and set its default configuration for this app.
    public static Camera openSelectedCamera(int cameraId, Activity activity) {

        Camera camera = null;
        if (cameraId != AppConstants.NOT_SET) {
            try {
                camera = Camera.open(cameraId);

                // Display what the camera sees in our cameraPreview.
                // (which is displayed in our activity_main Layout.)
                CameraPreview cameraPreview = (CameraPreview) activity.findViewById(R.id.cameraPreview);
                ViewGroup.LayoutParams params = cameraPreview.getLayoutParams();
                DisplayMetrics dm = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
                params.width =dm.widthPixels;
                params.height = dm.heightPixels;
                cameraPreview.setLayoutParams(params);

                // Connect the camera to our layout via the cameraPreview object.
                cameraPreview.connectCamera(camera);

                // Get the settings from the camera so that we can change them.
                Camera.Parameters cameraParameters = camera.getParameters();

                // Get the current resolution of the camera.
                Camera.Size size;

                // Get a list of supported camera resolutions.
                ((MainActivity) activity).SupportedPictureSizes = cameraParameters.getSupportedPictureSizes();

                // If a resolution has already been selected in our MainActivity then we should use it.
                if (((MainActivity) activity).SelectedPictureSize != null) {
                    size = ((MainActivity) activity).SelectedPictureSize;
                }
                else {

                    // Otherwise use this default resolution.

                    size = cameraParameters.getPictureSize();

                    size.width = 2592;
                    size.height = 1944;
//                    size.width = 1024;
//                    size.height = 768;

                    //
                    ((MainActivity) activity).SelectedPictureSize = size;
                }

                int i = 0;

                boolean sizeFound = false;

                // for each size in our list of supported pictures sized we want to
                // see if the size that was set above is in our list.  If it is then set
                // the MainActivity's SelectedPictureSizeIndex so that when the list of
                // resolutions is displayed we can automatically select it.
                for (Camera.Size pictureSize:((MainActivity) activity).SupportedPictureSizes) {
                    if (size.width == pictureSize.width && size.height == pictureSize.height) {
                        ((MainActivity) activity).SelectedPictureSizeIndex = i;
                        sizeFound = true;
                        break;
                    }
                    i++;
                }

                if (sizeFound) {
                    cameraParameters.setPictureSize(size.width,size.height);
                }
                else {

                }


                // set focus to auto
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // set white balance to auto
                cameraParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                // set flash to auto
                cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);

                //Log.v(LOG_TAG, String.valueOf(cameraParameters.isSmoothZoomSupported()));

                // Save the camera settings to a variable in the MainActivity.
                // We will apply these settings in the MainActivity.
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

    // Release the camera so some other application can use it if needed.
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
