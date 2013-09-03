package com.blushutter.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String LOG_TAG = MainActivity.LOG_TAG + ".CameraPreview";

    private Camera mCamera;
    private SurfaceHolder mHolder;

    public CameraPreview(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CameraPreview(Context context) {
        super(context);
    }

    public void connectCamera(Camera camera) {
        mCamera = camera;

        mHolder = getHolder();
        mHolder.addCallback(this);

        // start preview
        startPreview();
    }

    public void releaseCamera() {
        if (mCamera != null) {
            // stop preview
            stopPreview();

            mCamera = null;
        }
    }

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

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        stopPreview();
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopPreview();
    }
}
