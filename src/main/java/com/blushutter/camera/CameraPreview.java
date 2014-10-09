package com.blushutter.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    // for debugging...
    //private static final String LOG_TAG = AppConstants.LOG_TAG + ".CameraPreview";

    private Camera mCamera;
    private SurfaceHolder mHolder;

    /**
     * Constructor
     * @param context
     * @param attributeSet
     */
    public CameraPreview(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /**
     * Constructor
     * @param context
     */
    public CameraPreview(Context context) {
        super(context);
    }

    /**
     * Connect the camera to the surface holder.
     * @param camera
     */
    public void connectCamera(Camera camera) {
        mCamera = camera;

        mHolder = getHolder();
        mHolder.addCallback(this);

        // start preview
        startPreview();
    }

    /**
     * Release the camera
     */
    public void releaseCamera() {
        if (mCamera != null) {
            // stop preview
            stopPreview();

            mCamera = null;
        }
    }

    /**
     * Start viewing the camera preview.
     */
    void startPreview() {
        if (mCamera != null && mHolder.getSurface() != null) {
            try {

                if (mHolder == null) {
                    mHolder = getHolder();
                    mHolder.addCallback(this);
                }

                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            }
            catch (Exception e) {
                //Log.e(LOG_TAG, "Error setting preview display: " + e.getMessage());
            }
        }
    }

    /**
     * Stop viewing the camera preview.
     */
    void stopPreview() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            }
            catch (Exception e) {
                //Log.e(LOG_TAG, "Error stopping preview: " + e.getMessage());
            }

        }
    }

    /**
     * When the surface is created then start viewing the camera preview.
     * @param surfaceHolder
     */
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startPreview();
    }

    /**
     * When the surface changes (i.e. change in orientation) we need to
     * stop and restart the camera preview.
     * @param surfaceHolder
     * @param i
     * @param i2
     * @param i3
     */
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        stopPreview();
        startPreview();
    }

    /**
     * When the surface is destroyed then stop viewing the camera preview.
     * @param surfaceHolder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopPreview();
    }
}
