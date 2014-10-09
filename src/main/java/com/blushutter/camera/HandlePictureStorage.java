package com.blushutter.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
    //private static final String LOG_TAG = MainActivity.LOG_TAG + ".HandlePictureStorage";

    private static Context mFromContext = null;
    private static BluetoothCommandService mCommandService = null;
    private Handler mHandler = null;

    File file = null;
    String filePath = null;

    ByteArrayOutputStream stream;
    Bitmap bmp = null;
    byte[] b = null;

    /**
     * Constructor
     * @param context
     * @param bluetoothCommandService
     */
    public HandlePictureStorage(Context context, BluetoothCommandService bluetoothCommandService) {
        super();
        mFromContext = context;
        mCommandService = bluetoothCommandService;
        mHandler = new Handler();
    }

    /**
     * This event is triggered when the user presses the picture button on the camera.
     * @param bytes
     * @param camera
     */
    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {

        onPictureJpeg(bytes);

        // restart the camera preview after taking the picture
        camera.startPreview();

    }

    void onPictureJpeg(byte[] bytes){

        try {

            // if we need to save the file locally then create a file to store the photo in.
            if (MainActivity.SavePhotos || !((MainActivity) mFromContext).ConnectionIsOpen) {
                file = CameraHelper.generateTimeStampPhotoFile();
                filePath = file.getPath();
            }

            // save the bytes to a local variable
            b = bytes;

            // create a "Fire & Forget" thread
            final Runnable r = new Runnable()
            {
                public void run()
                {
                    saveFileAndSendViaBluetooth();
                }
            };

            // run the thread
            mHandler.post(r);

            // if saving photos is turned off and we don't have a bluetooth connection
            // then the photo has been saved to the camera - let the user know
            if (!MainActivity.SavePhotos && !((MainActivity) mFromContext).ConnectionIsOpen) {
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

            if (MainActivity.SavePhotos || !((MainActivity) mFromContext).ConnectionIsOpen) {
            // add photo to media store so that it shows up in the Android Gallery App
                // create a "Fire & Forget" thread
                final Runnable r2 = new Runnable()
                {
                    public void run()
                    {
                        addPhotoToMediaStore(filePath);
                    }
                };

                // run the thread
                mHandler.post(r2);
            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error accessing photo output file: " + e.getMessage());
            Toast.makeText(mFromContext, "Error saving photo", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Save the bytes to a file (if required) and send the bytes via Bluetooth.
     */
    private void saveFileAndSendViaBluetooth() {

        byte[] saveBytes = null;

        try {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;

            //options.inSampleSize = 4;

            if (bmp != null) {
                bmp.recycle();
            }
            bmp = BitmapFactory.decodeByteArray(b, 0, b.length);

            // rotate photo if needed
            // create a matrix object
            Matrix matrix = new Matrix();
            matrix.postRotate(((MainActivity) mFromContext).ScreenRotation);
            bmp = Bitmap.createBitmap(bmp , 0, 0, bmp .getWidth(), bmp .getHeight(), matrix, true);


            stream = new ByteArrayOutputStream();

            // compress file to 50% quality
            bmp.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            saveBytes = stream.toByteArray();
            stream.flush();
            stream.close();
            bmp.recycle();
            bmp = null;

            // save the file to the device if the user has selected to save photo option or
            // the Bluetooth connection is not open.
            if (MainActivity.SavePhotos || !((MainActivity) mFromContext).ConnectionIsOpen) {
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

            // make sure we have bytes to send,
            // the bluetooth service is created,
            // and the bluetooth connection is open.
            if (saveBytes!=null && mCommandService !=null && ((MainActivity) mFromContext).ConnectionIsOpen) {

                // prepend the length of the file
                byte[] byteLength = ("SSX" + String.format("%09d", saveBytes.length)).getBytes();
                // append some sort of eof placeholder
                byte[] endTransfer = "SSXTHISISTHEENDSSX".getBytes();
                // create a new byte array to hold the length, actual bytes from the photo, and the eof placeholder.
                byte[] destination = new byte[byteLength.length + saveBytes.length + endTransfer.length];

                System.arraycopy(byteLength, 0, destination, 0, byteLength.length);
                System.arraycopy(saveBytes, 0, destination, byteLength.length, saveBytes.length);
                System.arraycopy(endTransfer, 0, destination, byteLength.length + saveBytes.length, endTransfer.length);

                byteLength = null;
                endTransfer = null;
                b = null;

                // Tell the Bluetooth Service to write a byte stream.
                mCommandService.write(destination);

                // play sound
                if (MainActivity.SoundOn)
                    SoundManager.getSingleton().play(SoundManager.SOUND_PROCESS_DONE);

            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in SendViaBluetooth: " + e.getMessage());
        }

    }

    /**
     * Add photo to media store so that it shows up in the Android Gallery App.
     * @param pathName
     */
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

    /**
     * This method is called when the media scan is complete.
     * @param filePath
     * @param uri
     */
    private static void mediaFileScanComplete(String filePath, Uri uri) {
        // nothing to do
    }
}
