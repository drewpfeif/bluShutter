package com.blushutter.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class HandlePictureStorage implements Camera.PictureCallback {
    private static final String LOG_TAG = MainActivity.LOG_TAG + ".HandlePictureStorage";

    private static Context mFromContext = null;
    private static BluetoothCommandService mCommandService = null;
    private Handler mHandler = null;

    File file = null;
    String filePath = null;

    ByteArrayOutputStream stream;
    Bitmap bmp = null;
    byte[] b = null;

    public HandlePictureStorage(Context context, BluetoothCommandService bluetoothCommandService) {
        super();
        mFromContext = context;
        mCommandService = bluetoothCommandService;
        mHandler = new Handler();
    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {

        onPictureJpeg(bytes);

        // restart the camera preview after taking the picture
        camera.startPreview();

    }

    void onPictureJpeg(byte[] bytes){

        try {

            if (MainActivity.SavePhotos || !((MainActivity) mFromContext).mConnectionIsOpen) {
                file = CameraHelper.generateTimeStampPhotoFile();
                filePath = file.getPath();
            }

            b = bytes;

            final Runnable r = new Runnable()
            {
                public void run()
                {
                    saveFileAndSendViaBluetooth();
                }
            };

            mHandler.post(r);

            // if saving photos is turned off and we don't have a bluetooth connection
            // then the photo has been saved to the camera - let the user know
            if (!MainActivity.SavePhotos && !((MainActivity) mFromContext).mConnectionIsOpen) {
                Toast.makeText(mFromContext, "NO BLUETOOTH CONNECTION - Picture saved to camera." , Toast.LENGTH_LONG).show();
            }
            else {
                if (!MainActivity.SavePhotos) {
                    Toast.makeText(mFromContext, "Picture sent...", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(mFromContext, "Picture saved and sent...", Toast.LENGTH_LONG).show();
                }
            }

            if (MainActivity.SavePhotos || !((MainActivity) mFromContext).mConnectionIsOpen) {
            // add photo to media store
                final Runnable r2 = new Runnable()
                {
                    public void run()
                    {
                        addPhotoToMediaStore(filePath);
                    }
                };

                mHandler.post(r2);
            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error accessing photo output file: " + e.getMessage());
            Toast.makeText(mFromContext, "Error saving photo", Toast.LENGTH_LONG).show();
        }

    }

    private void saveFileAndSendViaBluetooth() {

        byte[] saveBytes = null;

        try {

            // compress file to 50% quality

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            //options.inSampleSize = 4;

            if (bmp != null) {
                bmp.recycle();
            }
            bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
            stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            saveBytes = stream.toByteArray();
            stream.flush();
            stream.close();
            bmp.recycle();
            bmp = null;

            if (MainActivity.SavePhotos || !((MainActivity) mFromContext).mConnectionIsOpen) {
                // save_off the file
                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                outputStream.write(saveBytes);
                outputStream.flush();
                outputStream.close();
                outputStream = null;

                file = null;
            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in saveFile: " + e.getMessage());
        }

        // transfer photo via bluetooth
        try {

            if (saveBytes!=null && mCommandService !=null && ((MainActivity) mFromContext).mConnectionIsOpen) {

                byte[] byteLength = ("SSX" + String.format("%09d", saveBytes.length)).getBytes();
                byte[] endTransfer = "SSXTHISISTHEENDSSX".getBytes();
                byte[] destination = new byte[byteLength.length + saveBytes.length + endTransfer.length];

                System.arraycopy(byteLength, 0, destination, 0, byteLength.length);
                System.arraycopy(saveBytes, 0, destination, byteLength.length, saveBytes.length);
                System.arraycopy(endTransfer, 0, destination, byteLength.length + saveBytes.length, endTransfer.length);

                byteLength = null;
                endTransfer = null;
                b = null;

                //mCommandService.write(saveBytes.length);
                mCommandService.write(destination);

                // play sound
                if (MainActivity.SoundOn)
                    SoundManager.getSingleton().play(SoundManager.SOUND_PROCESS_DONE);

                //destination = null;
                //bytes = null;
            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in SendViaBluetooth: " + e.getMessage());
        }

    }

    public static void addPhotoToMediaStore(String pathName) {
        String[] filesToScan = {pathName};

        try {

            // request Media Scanner to add photo into the Media System
            MediaScannerConnection.scanFile(mFromContext, filesToScan, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String filePath, Uri uri) {
                            mediaFileScanComplete(filePath, uri);
                        }
                    }
            );

        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in AddPhotoToMediaStore: " + e.getMessage());
        }
    }

    private static void mediaFileScanComplete(String filePath, Uri uri) {

    }
}
