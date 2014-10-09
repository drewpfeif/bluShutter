package com.blushutter.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    // private variables
    private final Context mContext = this;
    private static Toast mToastText;
    private Camera mSelectedCamera;
    private List<String> mBluetoothList = new ArrayList<String>();
    private int mSelectedCameraId = AppConstants.NOT_SET;
    private boolean mIsFocusing = false;
    private String[] mZoomLevels = null;
    private String mSelectedBluetoothId = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothCommandService mCommandService = null;
    private long mStartPressTime = 0;
    private Boolean mZoomStarted = false;
    private AlertDialog mAlertDialog = null;
    private Boolean mRestartingBluetooth = false;
    private int mFailedConnectionCount = 0;

    // public variables
    public Boolean ConnectionIsOpen = false;
    public Camera.Parameters CameraParameters = null;
    public Camera.Size SelectedPictureSize = null;
    public List<Camera.Size> SupportedPictureSizes = null;
    public int SelectedPictureSizeIndex = AppConstants.NOT_SET;
    public static Boolean SavePhotos = false;
    public static Boolean SoundOn = true;
    public int ScreenRotation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {

            // any sounds that this app creates will go through the Android system stream
            setVolumeControlStream(AudioManager.STREAM_SYSTEM);

            LoadPreferences();

            // create class-wide toast instance
            mToastText = Toast.makeText(this, "", Toast.LENGTH_SHORT);


            if (savedInstanceState == null) {
                setContentView(R.layout.activity_main);

                //setupUiVisibility();

                updateStatus("Not Connected");

                boolean hasCamera = checkCameras(this);

                if (!hasCamera) {
                    showNoCameraDialog();
                }
                else {
                    setupButtons();
                    setupCamera();
                    setupBluetooth();
                }
            }

        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in onCreate: " + e.getMessage());
            updateStatus("Error");
        }
    }

//    private void setupUiVisibility() {
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//    }

    private void setupButtons() {

        ImageButton mImageButton;
        ToggleButton mToggleButton;

        // btnSound
        mToggleButton = (ToggleButton) findViewById(R.id.btnSound);

        mToggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SoundOn = ((ToggleButton) v).isChecked();
                if (SoundOn) {
                    displayText("Sound is On");
                }
                else {
                    displayText("Sound is Off");
                }
            }
        });

        // btnSavePhoto
        mToggleButton = (ToggleButton) findViewById(R.id.btnSavePhoto);

        mToggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SavePhotos = ((ToggleButton) v).isChecked();
                if (SavePhotos) {
                    displayText("Save to Camera is On");
                }
                else {
                    displayText("Save to Camera is Off");
                }
            }
        });

        // btnResolution
        mImageButton = (ImageButton) findViewById(R.id.btnResolution);

        mImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnResolutionClicked();
            }
        });

        // btnBluetoothDevices
        mImageButton = (ImageButton) findViewById(R.id.btnBluetoothDevices);

        mImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showBluetoothDeviceSelection();
            }
        });
    }

    boolean checkCameras(Context context) {

        boolean hasCamera = false;

        try {
            PackageManager packageManager = context.getPackageManager();
            if (packageManager!=null)
                hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in checkCameras: " + e.getMessage());
        }

        return hasCamera;
    }

    private void setupCamera() {

        if (mSelectedCamera != null)
            return;

        try {
            mSelectedCameraId = CameraHelper.getFacingCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in setupCamera: " + e.getMessage());
        }
    }

    private void displayZoom() {

        TextView textView = (TextView) this.findViewById(R.id.textView);

        if (CameraParameters != null) {
            int currentZoom = CameraParameters.getZoom();
            textView.setText(mZoomLevels[currentZoom] + " Zoom");
        }
        else {
            textView.setText("");
        }

    }

    private void setupBluetooth() {

        try {
            // Get local Bluetooth adapter
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                updateStatus("Bluetooth is not available");
                finish();
            }
            else {

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, AppConstants.REQUEST_ENABLE_BT);
                }

                if (mSelectedBluetoothId == null) {
                    showBluetoothDeviceSelection();
                }
                else {

                    if (mCommandService != null) {
                        connectToDevice();
                    }
                    else {
                        setupCommand();
                    }

                    updateStatus("Connecting to " + mSelectedBluetoothId + "...");

                }
            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in setupBluetooth: " + e.getMessage());
        }
    }

    private void showLostConnectionMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Connection Lost");
        builder.setMessage("Bluetooth Connection Was Lost.  Restart the Listener in AIM and click OK.");
        builder.setNeutralButton(R.string.buttonOk, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Do nothing here. We override the onclick
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,int which)
                    {
                        // just close the dialog
                    }

                });

        // create alert dialog
        if (mAlertDialog == null || !mAlertDialog.isShowing()) {
            mAlertDialog = builder.create();

            mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialog) {

                    Button b = mAlertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    if (b!=null) {
                        b.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {

                                if (mSelectedBluetoothId == null)
                                    return;

                                //setupCommand();
                                RestartCommandService();

                                updateStatus("Connecting to " + mSelectedBluetoothId + "...");

                                //Dismiss once everything is OK.
                                mAlertDialog.dismiss();
                            }
                        });
                    }
                }
            });

            // show it
            mAlertDialog.show();
        }
    }

    private void showBluetoothDeviceSelection() {

        reloadBluetoothDeviceList();

        // if there is only one device to select then
        // automatically try to connect to it
        if (mBluetoothList.size() == 1) {
            if (mFailedConnectionCount > 2) {

                mFailedConnectionCount = 0;
                return;

            }
            else {

                updateStatus("Connecting to " + mSelectedBluetoothId + "...");
                mSelectedBluetoothId = mBluetoothList.get(0)
                        .substring(mBluetoothList.get(0).length() - 17);
                RestartCommandService();
                return;

            }
        }

        CharSequence[] items = mBluetoothList.toArray(new CharSequence[mBluetoothList.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.bluetoothDialog));
        builder.setSingleChoiceItems(items, -1,
                new DialogInterface.OnClickListener() {
                    // indexSelected contains the index of item (of which checkbox checked)
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected) {
                        mSelectedBluetoothId = mBluetoothList.get(indexSelected)
                                .substring(mBluetoothList.get(indexSelected).length() - 17);
                    }
                })
                // Set the action buttons
                .setNeutralButton(R.string.buttonOk, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Do nothing here. We override the onclick
                    }

                })
                .setPositiveButton("Refresh", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,int which)
                    {
                        showBluetoothDeviceSelection();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,int which)
                    {
                        // just close the dialog
                    }

                });

        // create alert dialog
        if (mAlertDialog == null || !mAlertDialog.isShowing()) {
            mAlertDialog = builder.create();

            mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialog) {

                    Button b = mAlertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    if (b!=null) {
                        b.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {

                                if (mSelectedBluetoothId == null)
                                    return;

                                RestartCommandService();

                                //setupCommand();

                                updateStatus("Connecting to " + mSelectedBluetoothId + "...");

                                //Dismiss once everything is OK.
                                mAlertDialog.dismiss();
                            }
                        });
                    }
                }
            });

            // show it
            mAlertDialog.show();
        }
    }

    private void reloadBluetoothDeviceList() {

        if (mBluetoothAdapter != null) {
            mBluetoothList.clear();

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices!=null) {
                // If there are paired devices
                if (pairedDevices.size() > 0) {
                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        // Add the name and address to an array adapter to show in a ListView
                        mBluetoothList.add(device.getName() + "\n" + device.getAddress());
                    }
                }
            }
        }

    }

    private void setupCommand() {

        try {
            // Initialize the BluetoothChatService to perform bluetooth connections
            mCommandService = new BluetoothCommandService(this, mHandler);
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in setupCommand: " + e.getMessage());
        }

        connectToDevice();

    }

    private void connectToDevice() {

        try {
            // Get the BLuetoothDevice object
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mSelectedBluetoothId);
            // Attempt to connect to the device
            mCommandService.connect(device);
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in connectToDevice: " + e.getMessage());
        }

    }

    // not using this method at all
    // but maybe in the future??
//    private void ensureDiscoverable() {
//        if (mBluetoothAdapter.getScanMode() !=
//                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            startActivity(discoverableIntent);
//        }
//    }

    private void takePicture() {
        try {

//            if (mConnectionIsOpen) {
                // play sound
                if (SoundOn)
                    SoundManager.getSingleton().play(SoundManager.SOUND_SHUTTER);

                // set rotation (portrait or landscape)
                //CameraParameters.setRotation(ScreenRotation);
                //mSelectedCamera.setParameters(CameraParameters);

                // take picture
                mSelectedCamera.takePicture(null, null, new HandlePictureStorage(this, mCommandService));
//            }
//            else {
//                AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
//                myAlertDialog.setTitle(getString(R.string.bluetoothDialogTitle));
//                myAlertDialog.setMessage(getString(R.string.bluetoothDialogMessage));
//                myAlertDialog.setPositiveButton(getString(R.string.buttonYes), new DialogInterface.OnClickListener() {
//
//                    public void onClick(DialogInterface arg0, int arg1) {
//                        setupBluetooth();
//                    }});
//
//                myAlertDialog.setNegativeButton(getString(R.string.buttonNo), new DialogInterface.OnClickListener() {
//
//                    public void onClick(DialogInterface arg0, int arg1) {
//                        // nothing to do here
//                    }});
//
//                myAlertDialog.show();
//            }
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in takePicture: " + e.getMessage());
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            System.out.println("In handler");

            try {
                switch (msg.what) {
                    case AppConstants.MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case BluetoothCommandService.STATE_CONNECTED:
                                ConnectionIsOpen = true;
                                mFailedConnectionCount = 0;
                                System.out.println("State Connected");

                                break;
                            case BluetoothCommandService.STATE_CONNECTING:
                                System.out.println("State Connecting");

                                break;
                            case BluetoothCommandService.STATE_LISTEN:
                                //TODO: will be implemented for client-server comm
                            case BluetoothCommandService.STATE_NONE:
                                System.out.println("State None");

                                break;
                        }
                        break;
                    case AppConstants.MESSAGE_DEVICE_NAME:
                        // save_off the connected device's name
                        String mConnectedDeviceName = msg.getData().getString(AppConstants.DEVICE_NAME);

                        updateStatus("Connected to " + mConnectedDeviceName);

                        break;
                    case AppConstants.MESSAGE_TOAST:

                        String t = msg.getData().getString(AppConstants.TOAST);
                        updateStatus(t);

                        if (t!=null && t.equals("Unable to connect device")) {
                            mFailedConnectionCount++;
                            ConnectionIsOpen = false;
                            // show bluetooth device selector
                            showBluetoothDeviceSelection();
                        }
                        else if (t!=null && t.equals("Device connection was lost")) {
                            ConnectionIsOpen = false;
                            // let the user know that we lost connection
                            if (!mRestartingBluetooth)
                                showLostConnectionMessage();
                        }

                        break;
                }
            }
            catch (Exception e) {
                //Log.e(LOG_TAG, "Error in handleMessage: " + e.getMessage());
            }
        }
    };

    private void StopCommandService() {

        if (mCommandService != null)
            mCommandService.stop();

    }

    private void StartCommandService() {

        if (mCommandService != null) {
            if (mCommandService.getState() == BluetoothCommandService.STATE_NONE) {
                mCommandService.start();

                connectToDevice();

            }
        }

    }

    private void RestartCommandService() {

        mRestartingBluetooth = true;
        StopCommandService();
        StartCommandService();
        mRestartingBluetooth = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {

            StopCommandService();

        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in onDestroy: " + e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        LoadPreferences();

        try {

            if (mSelectedCamera != null) {
                mSelectedCamera = CameraHelper.releaseSelectedCamera(mSelectedCamera, this);
            }

            if (mSelectedCameraId == AppConstants.NOT_SET) {
                mSelectedCameraId = CameraHelper.getFacingCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
            }

            mSelectedCamera = CameraHelper.openSelectedCamera(mSelectedCameraId, this);
            mSelectedCamera.setParameters(CameraParameters);

            displayZoom();

            StartCommandService();

        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in onResume: " + e.getMessage());
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        try {

            SavePreferences();

            mSelectedCamera = CameraHelper.releaseSelectedCamera(mSelectedCamera, this);

            StopCommandService();

        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in onPause: " + e.getMessage());
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = true;

        int id = item.getItemId();
        switch (id) {
            case R.id.action_refreshcamera:
                refreshCameraClicked();
                break;
            default:
                handled = super.onOptionsItemSelected(item);
        }

        return handled;
    }

    private void refreshCameraClicked() {

        setupCamera();

        try {
        // restart the camera preview
        mSelectedCamera.startPreview();
        }
        catch (Exception e) {
            updateStatus("CAMERA ERROR!!!");
        }

    }

    private void btnResolutionClicked() {

        if (SupportedPictureSizes == null)
            return;

        // create an array of strings of the form 320x240
        final String[] pictureSizesAsString = new String[SupportedPictureSizes.size()];
        int index = 0;

        try {
            for (Camera.Size pictureSize: SupportedPictureSizes)
                pictureSizesAsString[index++] = String.format(AppConstants.CAMERA_SIZE_DISPLAY_FORMAT, pictureSize.width, pictureSize.height);

            // show list in dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(getString(R.string.resolutionDialogTitle));
            builder.setSingleChoiceItems(pictureSizesAsString, SelectedPictureSizeIndex,
                    new DialogInterface.OnClickListener() {
                        // indexSelected contains the index of item (of which checkbox checked)
                        @Override
                        public void onClick(DialogInterface dialog, int indexSelected) {

                            SelectedPictureSizeIndex = indexSelected;
                            SelectedPictureSize = SupportedPictureSizes.get(indexSelected);
                        }
                    })
                    // Set the action buttons
                    .setPositiveButton(getString(R.string.buttonOk), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {

                            //Do nothing here. We override the onclick

                        }
                    });

            // create alert dialog
            final AlertDialog alertDialog = builder.create();

            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialog) {

                    Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    if (b != null) {
                        b.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {

                                if (SelectedPictureSize != null) {
                                    if (CameraParameters != null) {

                                        CameraParameters.setPictureSize(SelectedPictureSize.width, SelectedPictureSize.height);
                                        mSelectedCamera.setParameters(CameraParameters);

                                        displayText("Resolution is set to " + pictureSizesAsString[SelectedPictureSizeIndex]);

                                    }
                                }

                                //Dismiss once everything is OK.
                                alertDialog.dismiss();
                            }
                        });
                    }
                }
            });

            // show it
            alertDialog.show();
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in settingsMenuClick: " + e.getMessage());
        }
    }

//    private void resolutionMenuClicked() {
//
//        if (SupportedPictureSizes == null)
//            return;
//
//        // create an array of strings of the form 320x240
//        final String[] pictureSizesAsString = new String[SupportedPictureSizes.size()];
//        int index = 0;
//
//        try {
//            for (Camera.Size pictureSize: SupportedPictureSizes)
//                pictureSizesAsString[index++] = String.format(CAMERA_SIZE_DISPLAY_FORMAT, pictureSize.width, pictureSize.height);
//
//            // show list in dialog
//            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//            builder.setTitle(getString(R.string.resolutionDialogTitle));
//            builder.setSingleChoiceItems(pictureSizesAsString, SelectedPictureSizeIndex,
//                    new DialogInterface.OnClickListener() {
//                        // indexSelected contains the index of item (of which checkbox checked)
//                        @Override
//                        public void onClick(DialogInterface dialog, int indexSelected) {
//
//                            SelectedPictureSizeIndex = indexSelected;
//
//                        }
//                    })
//                    // Set the action buttons
//                    .setPositiveButton(getString(R.string.buttonOk), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int id) {
//
//                            //Do nothing here. We override the onclick
//
//                        }
//                    });
//
//            // create alert dialog
//            final AlertDialog alertDialog = builder.create();
//
//            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
//
//                @Override
//                public void onShow(DialogInterface dialog) {
//
//                    Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
//                    if (b != null) {
//                        b.setOnClickListener(new View.OnClickListener() {
//
//                            @Override
//                            public void onClick(View view) {
//
//                                if (SelectedPictureSize != null) {
//                                    if (CameraParameters != null) {
//                                        CameraParameters.setPictureSize(SelectedPictureSize.width, SelectedPictureSize.height);
//                                        mSelectedCamera.setParameters(CameraParameters);
//                                    }
//
//                                    displayText("Resolution is set to " + pictureSizesAsString[SelectedPictureSizeIndex]);
//
//                                }
//
//                                //Dismiss once everything is OK.
//                                alertDialog.dismiss();
//                            }
//                        });
//                    }
//                }
//            });
//
//            // show it
//            alertDialog.show();
//        }
//        catch (Exception e) {
//            Log.e(LOG_TAG, "Error in settingsMenuClick: " + e.getMessage());
//        }
//    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        super.onTouchEvent(event);
//
//        try {
//            if (event.getAction() == MotionEvent.ACTION_UP) {
//                setupUiVisibility();
//            }
//        }
//        catch (Exception e) {
//            //Log.e(LOG_TAG, "Error in onTouchEvent: " + e.getMessage());
//        }
//        return true;
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            SavePreferences();
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in onSaveInstanceState: " + e.getMessage());
        }

    }

    @Override
    public boolean dispatchKeyEvent( KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                if (action == KeyEvent.ACTION_UP) {
                    try {
                        takePicture();
                    }
                    catch (Exception e) {
                        //Log.e(LOG_TAG, "Error Taking Photo: " + e.getMessage());

                        displayText("Error Taking Photo");

                    }
                }
                return true;
            case KeyEvent.KEYCODE_FOCUS:
                // this event is continuously called
                // when the shutter button is pressed half way
                if (action == KeyEvent.ACTION_DOWN && event.getFlags() != 40) {
                    //call autofocus
                    if (!mIsFocusing) {

                        // play sound

                        mSelectedCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (success) {
                                    if (SoundOn)
                                        SoundManager.getSingleton().play(SoundManager.SOUND_FOCUS_END);
                                } else {
                                    if (SoundOn)
                                        SoundManager.getSingleton().play(SoundManager.SOUND_FOCUS_FAIL);
                                }
                            }
                        });
                    }

                    mIsFocusing = true;
                }
                else {
                    mSelectedCamera.cancelAutoFocus();
                    mIsFocusing = false;
                }
                return true;
            case KeyEvent.KEYCODE_ZOOM_IN:

                // I think scan code 545 means full press and 533 means half press
                // so...every time there is a full press there is also a half press

                if (action == KeyEvent.ACTION_DOWN && event.getScanCode() == 545) {

                    // check the amount of time the zoom button has been press
                    long eventTime = event.getEventTime();

                    if (mStartPressTime == 0) {
                        mStartPressTime = eventTime;
                    }

                    // increment the zoom
                    if (eventTime - mStartPressTime >= AppConstants.ZOOM_TIME) {
                        mStartPressTime = 0;
                        zoomIn();
                    }
                    else if (!mZoomStarted) {
                        mZoomStarted = true;
                        zoomIn();
                    }
                }
                else if (action == KeyEvent.ACTION_DOWN && event.getScanCode() != 545) {
                    // do nothing
                }
                else if (action == KeyEvent.ACTION_UP && event.getScanCode() == 545) {
                    displayZoom();
                }
                else {
                    mStartPressTime = 0;
                    mZoomStarted = false;
                }

                return true;

            case KeyEvent.KEYCODE_ZOOM_OUT:

                // I think scan code 546 means full press and 534 means half press
                // so...every time there is a full press there is also a half press

                if (action == KeyEvent.ACTION_DOWN && event.getScanCode() == 546) {

                    // check the amount of time the zoom button has been press
                    long eventTime = event.getEventTime();

                    if (mStartPressTime == 0) {
                        mStartPressTime = eventTime;
                    }

                    // increment the zoom
                    if (eventTime - mStartPressTime >= AppConstants.ZOOM_TIME) {
                        mStartPressTime = 0;
                        zoomOut();
                    }
                    else if (!mZoomStarted) {
                        mZoomStarted = true;
                        zoomOut();
                    }
                }
                else if (action == KeyEvent.ACTION_DOWN && event.getScanCode() != 546) {
                    // do nothing
                }
                else if (action == KeyEvent.ACTION_UP && event.getScanCode() == 546) {
                    displayZoom();
                }
                else {
                    mStartPressTime = 0;
                    mZoomStarted = false;
                }

                return true;

            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public void onBackPressed() {
        SavePreferences();
        super.onBackPressed();
    }

    // save our apps settings so that we can reload the settings if the
    // user exits the app and returns at a later time.
    private void SavePreferences(){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // The following will save the preferences as a Key/Value pair.
        // The first parameter is the Key which is defined as a Constant at the top of this file.
        // The second parameter is the Value that is associated with the Key.
        editor.putString(AppConstants.SELECTED_BLUETOOTH_ID_KEY , mSelectedBluetoothId);
        editor.putInt(AppConstants.SELECTED_CAMERA_ID_KEY, mSelectedCameraId);
        editor.putInt(AppConstants.SELECTED_PICTURE_SIZE, SelectedPictureSizeIndex);
        editor.putBoolean(AppConstants.SELECTED_SOUND_ON, SoundOn);
        editor.commit();
    }

    private void LoadPreferences(){

        mZoomLevels = new String[] {
                "1.0x",
                "1.2x",
                "1.5x",
                "1.8x",
                "2.2x",
                "2.8x",
                "3.4x",
                "4.0x",
                "5.0x",
                "6.1x",
                "7.5x",
                "9.4x",
                "11.4x",
                "13.9x",
                "17.9x",
                "21.0x"};

        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        mSelectedBluetoothId = sharedPreferences.getString(AppConstants.SELECTED_BLUETOOTH_ID_KEY, null);
        mSelectedCameraId = sharedPreferences.getInt(AppConstants.SELECTED_CAMERA_ID_KEY, AppConstants.NOT_SET);
        SelectedPictureSizeIndex = sharedPreferences.getInt(AppConstants.SELECTED_PICTURE_SIZE, AppConstants.NOT_SET);
        SoundManager.getSingleton().preload(this);
        SoundOn = sharedPreferences.getBoolean(AppConstants.SELECTED_SOUND_ON, true);
    }

//    private void submitFocusAreaRect(final Rect touchRect)
//    {
//        if (mSelectedCamera != null && CameraParameters != null) {
//
//            try {
//                Camera.Parameters cameraParameters = mSelectedCamera.getParameters();
//
//                if (cameraParameters.getMaxNumFocusAreas() == 0)
//                {
//                    return;
//                }
//
//                // Convert from View's width and height to +/- 1000
//                Rect focusArea = new Rect();
//
//                CameraPreview cameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);
//                focusArea.set(touchRect.left * 2000 / cameraPreview.getWidth() - 1000,
//                        touchRect.top * 2000 / cameraPreview.getHeight() - 1000,
//                        touchRect.right * 2000 / cameraPreview.getWidth() - 1000,
//                        touchRect.bottom * 2000 / cameraPreview.getHeight() - 1000);
//
//                // Submit focus area to camera
//
//                ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
//                focusAreas.add(new Camera.Area(focusArea, 1000));
//
//                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//                cameraParameters.setFocusAreas(focusAreas);
//                mSelectedCamera.setParameters(cameraParameters);
//
//                // play sound
//                if (SoundManager.getSingleton().SoundOn)
//                    SoundManager.getSingleton().play(SoundManager.SOUND_FOCUS_END);
//
//                // Start the autofocus operation
//                mSelectedCamera.autoFocus(null);
//            }
//            catch (Exception e) {
//                Log.e(LOG_TAG, "Error in submitFocusAreaRect: " + e.getMessage());
//                //Toast.makeText(mContext, "Error Focusing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                // play sound
//                if (SoundManager.getSingleton().SoundOn)
//                    SoundManager.getSingleton().play(SoundManager.SOUND_FOCUS_FAIL);
//
//            }
//        }
//    }

    void zoomIn() {
        try {

            if (!mZoomStarted) return;

            int currZoom = mSelectedCamera.getParameters().getZoom();
            int maxZoom = mSelectedCamera.getParameters().getMaxZoom();

            if (currZoom != maxZoom) {
//
//            Log.v(LOG_TAG, "Current Zoom: " + currZoom);
//
                currZoom++;
                CameraParameters = mSelectedCamera.getParameters();
                CameraParameters.setZoom(Math.min(currZoom, maxZoom));
                mSelectedCamera.setParameters(CameraParameters);

                displayZoom();

            }

        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in zoomIn: " + e.getMessage());
        }
    }

    void zoomOut() {
        try {

            if (!mZoomStarted) return;

            int currZoom = mSelectedCamera.getParameters().getZoom();

            if (currZoom != 0) {

//
//            Log.v(LOG_TAG, "Current Zoom: " + currZoom);
//
                currZoom--;
                if (currZoom == 0) {
                    CameraParameters.setZoom(0);
                }
                else {
                    CameraParameters.setZoom(Math.max(currZoom, 0));
                }
                mSelectedCamera.setParameters(CameraParameters);

                displayZoom();

            }

        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in zoomOut: " + e.getMessage());
        }
    }

    // SmoothZoom is not available on Samsung Galaxy Camera
//    void zoomTo(int value) {
//        try {
//            //if (_currentZoom != value) {
//            mSelectedCamera.stopSmoothZoom();
//            mSelectedCamera.startSmoothZoom(value);
//            //}
//        }
//        catch (Exception e) {
//            Log.e(LOG_TAG, "Error in zoomTo: " + e.getMessage());
//        }
//    }

    void showNoCameraDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.noCameraDialogTitle));
            builder.setMessage(
                    getString(R.string.noCameraDialogMessage)
            );
            builder.setPositiveButton(getString(R.string.noCameraDialogPositiveButton), null);
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
        catch (Exception e) {
            //Log.e(LOG_TAG, "Error in showNoCameraDialog: " + e.getMessage());
        }
    }

    private void displayText(final String message) {
        //mToastText.cancel();
        mToastText.setText(message);
        mToastText.show();
    }

    public void updateStatus(final String status) {

        TextView textView = (TextView) this.findViewById(R.id.textView2);
        if (textView != null) {
            textView.setText(status);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mSelectedCameraId, info);
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        mSelectedCamera.setDisplayOrientation((info.orientation - degrees + 360) % 360);
//        CameraParameters.setRotation((info.orientation - degrees + 360) % 360);
//        CameraParameters.set("orientation", "portrait");
//        mSelectedCamera.setParameters(CameraParameters);
        ScreenRotation = (info.orientation - degrees + 360) % 360;

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) (findViewById(R.id.sidebar)).getLayoutParams();

        // Checks the orientation of the screen for landscape and portrait and set landscape mode always
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            (findViewById(R.id.cameraPreview)).setPadding(0, 0, 0, 0);
            ((LinearLayout) findViewById(R.id.blushutter_root)).setOrientation(LinearLayout.HORIZONTAL);
            ((LinearLayout) findViewById(R.id.sidebar)).setOrientation(LinearLayout.VERTICAL);
            ((LinearLayout) findViewById(R.id.sidebar)).setGravity(Gravity.LEFT);
            setButtonMargins_Horizontal();
            layoutParams.width = 116;
            layoutParams.height = -1;
            (findViewById(R.id.sidebar)).setLayoutParams(layoutParams);
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            //setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            (findViewById(R.id.cameraPreview)).setPadding(40, 0, 40, 0);
            ((LinearLayout) findViewById(R.id.blushutter_root)).setOrientation(LinearLayout.VERTICAL);
            ((LinearLayout) findViewById(R.id.sidebar)).setOrientation(LinearLayout.HORIZONTAL);
            ((LinearLayout) findViewById(R.id.sidebar)).setGravity(Gravity.TOP);
            setButtonMargins_Vertical();
            layoutParams.width = -1;
            layoutParams.height = 116;
            (findViewById(R.id.sidebar)).setLayoutParams(layoutParams);
            //((LinearLayout) findViewById(R.id.sidebar)).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,58));
            //setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            //setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

    }

    private void setButtonMargins_Vertical(){
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnBluetoothDevices)).getLayoutParams()).topMargin = 10;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnBluetoothDevices)).getLayoutParams()).bottomMargin = 0;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnBluetoothDevices)).getLayoutParams()).leftMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnBluetoothDevices)).getLayoutParams()).rightMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSound)).getLayoutParams()).topMargin = 10;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSound)).getLayoutParams()).bottomMargin = 0;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSound)).getLayoutParams()).leftMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSound)).getLayoutParams()).rightMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnResolution)).getLayoutParams()).topMargin = 10;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnResolution)).getLayoutParams()).bottomMargin = 0;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnResolution)).getLayoutParams()).leftMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnResolution)).getLayoutParams()).rightMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSavePhoto)).getLayoutParams()).topMargin = 10;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSavePhoto)).getLayoutParams()).bottomMargin = 0;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSavePhoto)).getLayoutParams()).leftMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSavePhoto)).getLayoutParams()).rightMargin = 40;
    }

    private void setButtonMargins_Horizontal(){
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnBluetoothDevices)).getLayoutParams()).topMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnBluetoothDevices)).getLayoutParams()).bottomMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnBluetoothDevices)).getLayoutParams()).leftMargin = 10;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnBluetoothDevices)).getLayoutParams()).rightMargin = 0;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSound)).getLayoutParams()).topMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSound)).getLayoutParams()).bottomMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSound)).getLayoutParams()).leftMargin = 10;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSound)).getLayoutParams()).rightMargin = 0;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnResolution)).getLayoutParams()).topMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnResolution)).getLayoutParams()).bottomMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnResolution)).getLayoutParams()).leftMargin = 10;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnResolution)).getLayoutParams()).rightMargin = 0;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSavePhoto)).getLayoutParams()).topMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSavePhoto)).getLayoutParams()).bottomMargin = 40;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSavePhoto)).getLayoutParams()).leftMargin = 10;
        ((ViewGroup.MarginLayoutParams) (findViewById(R.id.btnSavePhoto)).getLayoutParams()).rightMargin = 0;
    }
}
