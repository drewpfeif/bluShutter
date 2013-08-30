package com.blushutter.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

public class HandlePictureStorage implements Camera.PictureCallback {
    private static final String LOG_TAG = MainActivity.LOG_TAG + ".HandlePictureStorage";

    private static Context mFromContext = null;
    private static BluetoothCommandService mCommandService = null;
    private Handler mHandler = null;

//    File file = null;
//    String filePath = null;

    ByteArrayOutputStream stream;
    //    OutputStream outputStream;
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

//            file = CameraHelper.generateTimeStampPhotoFile();
//            filePath = file.getPath();
//            String fileName = file.getName();

            b = bytes;

            final Runnable r = new Runnable()
            {
                public void run()
                {
                    saveFileAndSendViaBluetooth();
                }
            };

            mHandler.post(r);

            //Toast.makeText(mFromContext, "Picture saved as " + fileName, Toast.LENGTH_LONG).show();
            Toast.makeText(mFromContext, "Picture sent...", Toast.LENGTH_LONG).show();

            // add photo to media store
//            final Runnable r2 = new Runnable()
//            {
//                public void run()
//                {
//                    addPhotoToMediaStore(filePath);
//                }
//            };
//
//            mHandler.post(r2);

        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Error accessing photo output file: " + e.getMessage());
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

            // save the file
//            outputStream = new BufferedOutputStream(new FileOutputStream(file));
//            outputStream.write(saveBytes);
//            outputStream.flush();
//            outputStream.close();
//            outputStream = null;
//
//            file = null;

        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Error in saveFile: " + e.getMessage());
        }

        // transfer photo via bluetooth
        try {

            if (saveBytes!=null && mCommandService !=null) {

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
                if (SoundManager.getSingleton().SoundOn)
                    SoundManager.getSingleton().play(SoundManager.SOUND_PROCESS_DONE);

                //destination = null;
                //bytes = null;
            }
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Error in SendViaBluetooth: " + e.getMessage());
        }

    }

//    public static void addPhotoToMediaStore(String pathName) {
//        String[] filesToScan = {pathName};
//
//        try {
//
//            // request Media Scanner to add photo into the Media System
//            MediaScannerConnection.scanFile(mFromContext, filesToScan, null,
//                    new MediaScannerConnection.OnScanCompletedListener() {
//                        public void onScanCompleted(String filePath, Uri uri) {
//                            mediaFileScanComplete(filePath, uri);
//                        }
//                    }
//            );
//
//        }
//        catch (Exception e) {
//            Log.e(LOG_TAG, "Error in AddPhotoToMediaStore: " + e.getMessage());
//        }
//    }
//
//    private static void mediaFileScanComplete(String filePath, Uri uri) {
//
//    }
}
