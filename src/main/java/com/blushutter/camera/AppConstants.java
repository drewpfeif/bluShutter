// Constant utility class
package com.blushutter.camera;

public class AppConstants {

    private AppConstants() {}  // prevents instantiation

    public static final String LOG_TAG = "com.blushutter.camera";
    public static final int NOT_SET = -1;

    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;
    public static final String SELECTED_SOUND_ON = "SoundOn";
    public static final String SELECTED_CAMERA_ID_KEY = "mSelectedCameraId";
    public static final String SELECTED_PICTURE_SIZE = "SelectedPictureSizeIndex";
    public static final String SELECTED_BLUETOOTH_ID_KEY = "mSelectedBluetoothId";
    public static final String CAMERA_SIZE_DISPLAY_FORMAT = "%d x %d";
    public static final long ZOOM_TIME = 500;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothCommandService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

}
