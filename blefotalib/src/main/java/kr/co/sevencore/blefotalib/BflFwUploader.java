package kr.co.sevencore.blefotalib;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.SimpleExpandableListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import kr.co.sevencore.blefotalib.BflCodeList.UploadCode;
import kr.co.sevencore.blefotalib.BflCodeList.ServiceIdxCode;
import kr.co.sevencore.blefotalib.BflCodeList.CharacteristicFotaIdxCode;
import kr.co.sevencore.blefotalib.BflCodeList.CharacteristicDeviceInfoIdxCode;

/**
 * BflFwUploader.java
 * BLE FOTA Library Firmware Upload.
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-04-08
 * @see kr.co.sevencore.blefotalib.BflFwUploadService
 * @see kr.co.sevencore.blefotalib.IBflFwUploadSvc
 * TODO.. Apply flag value - 00: The BLE device is rebooted by the old firmware, 01: The BLE device is rebooted by the new firmware.
 */
public class BflFwUploader {
    private final static String BLE_FOTA_TAG = BflFwUploader.class.getSimpleName();

    public IBflFwUploadSvc mBflUploadBinder;    // Firmware data upload service AIDL.
    private Intent mUploadServiceIntent;
    private String mAddress;                     // The target device MAC address.

    private Context mContext;

    private static String sFirmwareCurrentVersion;       // Current target device(BLE server) firmware version.
    private static String sFirmwareNewVersion;           // If another firmware data exists at the target device(BLE server), return firmware version of the target device,
    private static String sFirmwareVersion;              // Selected firmware version at the smart device(BLE client).

    private static String sFilePath;                     // Selected firmware data file path.
    private static int sLeftConnCnt = 0;                // Left firmware data count.
    private static int sSequenceNumber = -1;            // Sequence number starts at 0.

    private static String sFirmwareDataStatus;           // Firmware data integrity status.
    private static String sFirmwareStatus;               // Firmware software status.

    private static byte sFirmwareUpgradeTypeFlag = 0;   // Flag 0 : Normal upgrade | Flag 1 : Forced upgrade

    private static String sManufacturerName;             // Device manufacturer name.
    private static String sModelNumber;                  // Device model number.
    private static String sSerialNumber;                 // Device serial number.

    private static String sExtraData;                    // Extra data information by read or write characteristic.

    private static boolean sInitAutoProgressFlag = false; // Initialization for FOTA automation flag.
    private static boolean sAutoProgressFlag = false;     // Firmware upgrade automation flag.

    private final String NORMAL = "0";                // Firmware data check & status normal status.
    private final String VALIDATE = "1";             // Firmware data validate status.
    private final String INVALIDATE = "2";           // Firmware data invalidate status.
    private final String SUCCESSFUL = "1";           // Firmware status successful status.
    private final String ABNORMAL_FINISH = "2";     // Firmware status abnormal finish status.

    private OnUploadSvcInit mUploadSvcInitCallback;                 // Service initialization result of the firmware upload callback.
    private OnConnectionState mConnectionCallback;                   // BLE connection status callback.
    private OnErrorStateListener mErrorStateCallback;                // Error state for connection callback.
    private OnUpdateGattServiceListener mUpdateGattServiceCallback; // GATT service list adapter callback.
    private OnDeviceInfoListener mDeviceInfoCallback;                // Device and firmware status related information in doing FOTA callback.


    public BflFwUploader() {}

    public BflFwUploader(Context context) {
        mContext = context;
    }

    /**
     * OnUploadSvcInit interface is used to get result of firmware upload service initialization.
     */
    public interface OnUploadSvcInit {
        /**
         * Initialization using BLE of the smart device.
         *
         * @param initResult true: All of BLE related feature is normal state.
         *                   false: Unable to initialize BLE relate feature.
         */
        void onUploadSvcInit(boolean initResult);
    }

    /**
     * OnConnectionState interface is used to get state of connection.
     * If a main application needs to get state of BLE connection state,
     * use onConnectionState method.
     */
    public interface OnConnectionState {
        /**
         * Get BLE connection state.
         *
         * @param state true: connected
         *              false: disconnected.
         */
        void onConnectionState(boolean state);
    }

    /**
     * OnErrorStateListener interface is used to notify error state.
     * If a main application needs to get the error state of Bluetooth GATT or device information to used for connection,
     * use onErrorStateListener.
     */
    public interface OnErrorStateListener {
        /**
         * Get error state.
         *
         * @param state true: Error state.
         *              false: Normal state.
         */
        void onErrorStateListener(boolean state);
    }

    /**
     * OnUpdateGattServiceListener interface is used to get GATT service adapter.
     * If a main application needs to show service list by expandable list adapter,
     * use onUpdateGattServiceListener method.
     */
    public interface OnUpdateGattServiceListener {
        /**
         * Get GATT service list adapter of the target device.
         *
         * @param gattServiceAdapter includes GATT services lists.
         */
        void onUpdateGattServiceListener(SimpleExpandableListAdapter gattServiceAdapter);
    }

    /**
     * OnDeviceInfoListener interface is used to get firmware upgrade progress related data of the target device.
     * If a main application needs a firmware upgrade progress related information,
     * use onDeviceInfoListener method.
     */
    public interface OnDeviceInfoListener {
        /**
         * Used to get string information data of firmware upgrade progress.
         *
         * @param code is a progress identifier of firmware upgrade progress.
         * @param data is the information related each progress state.
         */
        void onDeviceInfoListener(String code, String data);

        /**
         * Used to get integer value of sequence number about firmware upgrade.
         *
         * @param code is a progress identifier of firmware upgrade progress.
         * @param data is the sequence number value.
         */
        void onDataSequenceNumberListener(String code, int data);
    }

    /**
     * Save a callback object to mUploadSvcInitCallback.
     *
     * @see kr.co.sevencore.blefotalib.BflFwUploader.OnUploadSvcInit
     */
    public void setOnUploadSvcInit(OnUploadSvcInit callback) {
        mUploadSvcInitCallback = callback;
    }

    /**
     * Save a callback object to mConnectionCallback.
     *
     * @see kr.co.sevencore.blefotalib.BflFwUploader.OnConnectionState
     */
    public void setOnConnectionState(OnConnectionState callback) {
        mConnectionCallback = callback;
    }

    /**
     * Save a callback object to mErrorStateCallback.
     *
     * @see kr.co.sevencore.blefotalib.BflFwUploader.OnErrorStateListener
     */
    public void setOnErrorStateListener(OnErrorStateListener callback) {
        mErrorStateCallback = callback;
    }

    /**
     * Save a callback object to mUpdateGattServiceCallback.
     *
     * @see kr.co.sevencore.blefotalib.BflFwUploader.OnUpdateGattServiceListener
     */
    public void setOnUpdateGattServiceListener(OnUpdateGattServiceListener callback) {
        mUpdateGattServiceCallback = callback;
    }

    /**
     * Save a callback object to mDeviceInfoCallback.
     *
     * @see kr.co.sevencore.blefotalib.BflFwUploader.OnDeviceInfoListener
     */
    public void setOnDeviceInfoListener(OnDeviceInfoListener callback) {
        mDeviceInfoCallback = callback;
    }

    /**
     * Get the new firmware version information.
     *
     * @return The new firmware version information.
     */
    public String getFirmwareVersion() {
        return sFirmwareVersion;
    }

    /**
     * Get the file path of the firmware data.
     *
     * @return The path of firmware data location.
     */
    public String getFilePath() {
        return sFilePath;
    }

    /**
     * Get the firmware upgrade type.
     *
     * @return The firmware upgrade type flag.
     */
    public byte getFirmwareUpgradeType() {
        return sFirmwareUpgradeTypeFlag;
    }

    /**
     * Set the new firmware version.
     *
     * @param version is extracted from the file name to be a new firmware version.
     */
    public void setFirmwareVersion(String version) {
        sFirmwareVersion = version;
    }

    /**
     * Set the file path of the firmware data.
     *
     * @param path is the location of the firmware data.
     */
    public void setFilePath(String path) {
        sFilePath = path;
    }

    /**
     * Set the firmware upgrade type.
     *
     * @param type is firmware upgrade type.
     */
    public void setFirmwareUpgradeType(byte type) {
        sFirmwareUpgradeTypeFlag = type;
    }

    /**
     * Create an object to implement a service connection interface.
     * The firmware upload service updates the firmware of the BLE device.
     *
     * @see kr.co.sevencore.blefotalib.IBflFwUploadSvc
     * @see kr.co.sevencore.blefotalib.BflFwUploadService
     */
    private final ServiceConnection mBflUploadSvcConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBflUploadBinder = IBflFwUploadSvc.Stub.asInterface(service);
            boolean initResult = true;

            try {
                if (!mBflUploadBinder.initUploader()) {
                    initResult = false;
                    Log.e(BLE_FOTA_TAG, "Unable to initialize Bluetooth.");
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mUploadSvcInitCallback != null) {
                    mUploadSvcInitCallback.onUploadSvcInit(initResult);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(mAddress != null) {
                    mBflUploadBinder.connect(mAddress);
                } else {
                    Log.e(BLE_FOTA_TAG, "Connection creation failed because MAC address value is NULL.");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBflUploadBinder = null;
        }
    };

    /**
     * Broadcast receiver of firmware upload service.
     * Handle various events fired by the Service.
     * ACTION_GATT_CONNECTED: Connected to a GATT server.
     * ACTION_GATT_DISCONNECTED: Disconnected from a GATT server.
     * ACTION_GATT_SERVICES_DISCOVERED: GATT services Discovered.
     * ACTION_GATT_DATA_AVAILABLE: Update GATT services & characteristics list to be used to do FOTA.
     * ACTION_DATA_AVAILABLE: Received data from the device. A result of read or notification operations.
     * ACTION_DATA_WRITABLE: Transmitting data from client device. A result of write operation.
     *
     * @see kr.co.sevencore.blefotalib.BflFwUploadService
     */
    private final BroadcastReceiver mBflUploadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null) {
                if (BflFwUploadService.ERROR_LOST_GATT.equals(action)) {

                    if (mErrorStateCallback != null) {
                        mErrorStateCallback.onErrorStateListener(true);
                    }
                } else if (BflFwUploadService.ERROR_LOST_DEVICE_INFORMATION.equals(action)) {

                    if (mErrorStateCallback != null) {
                        mErrorStateCallback.onErrorStateListener(true);
                    }
                } else if (BflFwUploadService.ACTION_GATT_CONNECTED.equals(action)) {
                    Log.i(BLE_FOTA_TAG, "The device is connected.");

                    // Update connection state by the callback.
                    if (mConnectionCallback != null) {
                        mConnectionCallback.onConnectionState(true);
                    }
                } else if (BflFwUploadService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    Log.i(BLE_FOTA_TAG, "The device is disconnected.");

                    initializeValue();

                    // Update connection state by the callback.
                    if (mConnectionCallback != null) {
                        mConnectionCallback.onConnectionState(false);
                    }
                } else if (BflFwUploadService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    try {
                        // Update GATT services to do FOTA.
                        mBflUploadBinder.updateGatt();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else if (BflFwUploadService.ACTION_GATT_DATA_AVAILABLE.equals(action)) {
                    // Show all the supported services and characteristics on the user interface.
                    if (mUpdateGattServiceCallback != null) {
                        mUpdateGattServiceCallback.onUpdateGattServiceListener(
                                updateGattServicesAdapter(
                                        (ArrayList<HashMap<String, String>>) intent.getExtras().
                                                getSerializable(BflFwUploadService.GATT_SERVICE_DATA_AVAILABLE),
                                        (ArrayList<ArrayList<HashMap<String, String>>>) intent.getExtras().
                                                getSerializable(BflFwUploadService.GATT_CHARACTERISTIC_DATA_AVAILABLE)
                                )
                        );
                    }

                    if (sInitAutoProgressFlag) {
                        // Firmware version characteristic read property execution.
                        executeReadFirmwareCurrentVersion();
                    }

                } else if (BflFwUploadService.ACTION_FIRMWARE_CURRENT_VERSION_AVAILABLE.equals(action)) {
                    sFirmwareCurrentVersion = intent.getStringExtra(BflFwUploadService.EXTRA_DATA);

                    // Compare the device firmware version and the server firmware version.
                    Intent versionInfoIntent = new Intent(BflFwVerChecker.ACTION_TARGET_VERSION);
                    versionInfoIntent.putExtra(BflFwVerChecker.VERSION_INFO, sFirmwareCurrentVersion);
                    versionInfoIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    context.sendBroadcast(versionInfoIntent);

                    if (sInitAutoProgressFlag) {
                        // Firmware new version characteristic read property execution.
                        executeReadFirmwareNewVersion();
                    }

                    if (mDeviceInfoCallback != null) {
                        mDeviceInfoCallback.onDeviceInfoListener(
                                UploadCode.DEVICE_FIRMWARE_CURRENT_VERSION.getCode(),
                                sFirmwareCurrentVersion
                        );
                    }
                    Log.d(BLE_FOTA_TAG, "Current firmware version: " + sFirmwareCurrentVersion);

                } else if (BflFwUploadService.ACTION_FIRMWARE_NEW_VERSION_AVAILABLE.equals(action)) {
                    sFirmwareNewVersion = intent.getStringExtra(BflFwUploadService.EXTRA_DATA);

                    final String defaultVersion = "00-00-00";

                    if (sInitAutoProgressFlag) {
                        // Manufacturer name characteristic read property execution.
                        executeReadManufacturerName();
                    }

                    // If you want to manage firmware version information
                    // between a new firmware data which is going to be transmitted
                    // and a existing new firmware data which exists on the device,
                    // use below conditional statement.
                    if (sFirmwareNewVersion.equals(defaultVersion)) {
                        if (mDeviceInfoCallback != null) {
                            mDeviceInfoCallback.onDeviceInfoListener(
                                    UploadCode.DEVICE_FIRMWARE_NEW_VERSION_EMPTINESS.getCode(),
                                    sFirmwareNewVersion
                            );
                        }
                    } else {
                        if (mDeviceInfoCallback != null) {
                            mDeviceInfoCallback.onDeviceInfoListener(
                                    UploadCode.DEVICE_FIRMWARE_NEW_VERSION_EXISTENCE.getCode(),
                                    sFirmwareNewVersion
                            );
                        }
                    }
                    Log.d(BLE_FOTA_TAG, "New firmware version: " + sFirmwareNewVersion);

                } else if (BflFwUploadService.ACTION_SEQUENCE_NUMBER_AVAILABLE.equals(action)) {
                    if (sAutoProgressFlag && (intent.getStringExtra(BflFwUploadService.EXTRA_DATA) != null)) {
                        sSequenceNumber = Integer.parseInt(intent.getStringExtra(BflFwUploadService.EXTRA_DATA));

                        // Check the existing file size to resume transmitting the firmware data
                        // or checking firmware data integrity.
                        if (!checkSequence(sFilePath, sSequenceNumber)) {
                            try {
                                mBflUploadBinder.executeWriteFirmwareData(
                                        ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                                        CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_DATA.getCode(),
                                        sFilePath, sSequenceNumber);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                mBflUploadBinder.executeWriteChecksumData(
                                        ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                                        CharacteristicFotaIdxCode.CHARACTERISTIC_CHECKSUM_DATA.getCode(),
                                        sFilePath);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        if (mDeviceInfoCallback != null) {
                            mDeviceInfoCallback.onDataSequenceNumberListener(
                                    UploadCode.DEVICE_FIRMWARE_AVAILABLE_SEQUENCE_NUMBER.getCode(),
                                    sSequenceNumber
                            );
                        }
                    }
                    Log.d(BLE_FOTA_TAG, "Data sequence number of the device: " + sSequenceNumber);

                } else if (BflFwUploadService.ACTION_FIRMWARE_DATA_CHECK_AVAILABLE.equals(action)) {
                    if (sAutoProgressFlag && (intent.getStringExtra(BflFwUploadService.EXTRA_DATA) != null)) {
                        sFirmwareDataStatus = intent.getStringExtra(BflFwUploadService.EXTRA_DATA);

                        // Firmware data check flag value - NORMAL: 0, VALIDATE: 1, INVALIDATE: 2
                        switch (sFirmwareDataStatus) {
                            case NORMAL:
                                if (mDeviceInfoCallback != null) {
                                    mDeviceInfoCallback.onDeviceInfoListener(
                                            UploadCode.DEVICE_FIRMWARE_DATA_STATUS_NORMAL.getCode(),
                                            sFirmwareDataStatus
                                    );
                                }
                                Log.i(BLE_FOTA_TAG, "Firmware data has not been verified yet.");
                                break;

                            case VALIDATE:
                                try {
                                    mBflUploadBinder.executeWriteFirmwareUpgradeType(
                                            ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                                            CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_UPGRADE_TYPE.getCode(),
                                            sFirmwareUpgradeTypeFlag
                                    );
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }

                                if (mDeviceInfoCallback != null) {
                                    mDeviceInfoCallback.onDeviceInfoListener(
                                            UploadCode.DEVICE_FIRMWARE_DATA_STATUS_VALID.getCode(),
                                            sFirmwareDataStatus
                                    );
                                }
                                Log.i(BLE_FOTA_TAG, "Firmware data validated.");
                                break;

                            case INVALIDATE:
                                sAutoProgressFlag = false;

                                if (mDeviceInfoCallback != null) {
                                    mDeviceInfoCallback.onDeviceInfoListener(
                                            UploadCode.DEVICE_FIRMWARE_DATA_STATUS_INVALID.getCode(),
                                            sFirmwareDataStatus
                                    );
                                }
                                Log.i(BLE_FOTA_TAG, "Firmware data invalidated");
                                break;
                        }
                    }
                } else if (BflFwUploadService.ACTION_FIRMWARE_STATUS_AVAILABLE.equals(action)) {
                    if (sAutoProgressFlag && (intent.getStringExtra(BflFwUploadService.EXTRA_DATA) != null)) {
                        sFirmwareStatus = intent.getStringExtra(BflFwUploadService.EXTRA_DATA);

                        // Firmware upgrade status flag value - NORMAL: 0, SUCCESSFUL: 1, ABNORMAL FINISH: 2
                        switch (sFirmwareStatus) {
                            case NORMAL:
                                if (mDeviceInfoCallback != null) {
                                    mDeviceInfoCallback.onDeviceInfoListener(
                                            UploadCode.DEVICE_FIRMWARE_STATUS_NORMAL.getCode(),
                                            sFirmwareStatus
                                    );
                                }
                                Log.i(BLE_FOTA_TAG, "Upgrade is not over yet.");
                                break;

                            case SUCCESSFUL:
                                if (mDeviceInfoCallback != null) {
                                    mDeviceInfoCallback.onDeviceInfoListener(
                                            UploadCode.DEVICE_FIRMWARE_STATUS_SUCCESSFUL_FINISH.getCode(),
                                            sFirmwareStatus
                                    );
                                }
                                Log.i(BLE_FOTA_TAG, "Firmware upgrade finished successfully.");
                                break;

                            case ABNORMAL_FINISH:
                                sAutoProgressFlag = false;

                                if (mDeviceInfoCallback != null) {
                                    mDeviceInfoCallback.onDeviceInfoListener(
                                            UploadCode.DEVICE_FIRMWARE_STATUS_ABNORMAL_FINISH.getCode(),
                                            sFirmwareStatus
                                    );
                                }
                                Log.i(BLE_FOTA_TAG, "Firmware upgrade finished abnormally.");
                                break;
                        }
                    }
                } else if (BflFwUploadService.ACTION_MANUFACTURER_NAME_AVAILABLE.equals(action)) {
                    sManufacturerName = intent.getStringExtra(BflFwUploadService.EXTRA_DATA);

                    if (sInitAutoProgressFlag) {
                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // Model number characteristic read property execution.
                        executeReadModelNumber();
                    }

                    if (mDeviceInfoCallback != null) {
                        mDeviceInfoCallback.onDeviceInfoListener(
                                UploadCode.DEVICE_INFORMATION_MANUFACTURER_NAME.getCode(),
                                sManufacturerName
                        );
                    }
                } else if (BflFwUploadService.ACTION_MODEL_NUMBER_AVAILABLE.equals(action)) {
                    sModelNumber = intent.getStringExtra(BflFwUploadService.EXTRA_DATA);

                    if (sInitAutoProgressFlag) {
                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // Serial number characteristic read property execution.
                        executeReadSerialNumber();
                    }

                    if (mDeviceInfoCallback != null) {
                        mDeviceInfoCallback.onDeviceInfoListener(
                                UploadCode.DEVICE_INFORMATION_MODEL_NUMBER.getCode(),
                                sModelNumber
                        );
                    }
                } else if (BflFwUploadService.ACTION_SERIAL_NUMBER_AVAILABLE.equals(action)) {
                    sSerialNumber = intent.getStringExtra(BflFwUploadService.EXTRA_DATA);

                    if (mDeviceInfoCallback != null) {
                        mDeviceInfoCallback.onDeviceInfoListener(
                                UploadCode.DEVICE_INFORMATION_SERIAL_NUMBER.getCode(),
                                sSerialNumber
                        );
                    }
                } else if (BflFwUploadService.ACTION_DATA_AVAILABLE.equals(action)) {
                    sExtraData = intent.getStringExtra(BflFwUploadService.EXTRA_DATA);

                    if (mDeviceInfoCallback != null) {
                        mDeviceInfoCallback.onDeviceInfoListener(
                                UploadCode.DEVICE_EXTRA_DATA.getCode(), sExtraData
                        );
                    }
                    Log.d(BLE_FOTA_TAG, "Read extra data: " + sExtraData);

                } else if (BflFwUploadService.ACTION_FIRMWARE_NEW_VERSION_WRITABLE.equals(action)) {
                    if (sAutoProgressFlag && intent.getStringExtra(BflFwUploadService.EXTRA_DATA) != null) {
                        try {
                            mBflUploadBinder.executeWriteFirmwareData(
                                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                                    CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_DATA.getCode(),
                                    sFilePath, sSequenceNumber);
                            Log.d(BLE_FOTA_TAG, "Execute firmware upgrade. Starting firmware data transmission.");
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(BLE_FOTA_TAG, "Transmit a new version information to the target device.");

                } else if (BflFwUploadService.ACTION_FIRMWARE_DATA_WRITABLE.equals(action)) {
                    sLeftConnCnt = Integer.parseInt(intent.getStringExtra(BflFwUploadService.EXTRA_DATA));

                    if (sAutoProgressFlag && (sLeftConnCnt == 0)) {
                        try {
                            mBflUploadBinder.executeWriteChecksumData(
                                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                                    CharacteristicFotaIdxCode.CHARACTERISTIC_CHECKSUM_DATA.getCode(),
                                    sFilePath);
                            Log.d(BLE_FOTA_TAG, "Generate & transmit checksum data.");
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    if (mDeviceInfoCallback != null) {
                        mDeviceInfoCallback.onDataSequenceNumberListener(
                                UploadCode.DEVICE_FIRMWARE_DATA_LEFT_COUNT.getCode(),
                                sLeftConnCnt
                        );
                    }
                    Log.d(BLE_FOTA_TAG, "Left data count: " + sLeftConnCnt);

                } else if (BflFwUploadService.ACTION_SEQUENCE_NUMBER_WRITABLE.equals(action)) {
                    int writableSequenceNumber = Integer.parseInt(intent.getStringExtra(BflFwUploadService.EXTRA_DATA));

                    if (mDeviceInfoCallback != null) {
                        mDeviceInfoCallback.onDataSequenceNumberListener(
                                UploadCode.DEVICE_FIRMWARE_WRITABLE_SEQUENCE_NUMBER.getCode(),
                                writableSequenceNumber
                        );
                    }
                    Log.i(BLE_FOTA_TAG, "Sequence number is " + writableSequenceNumber);

                } else if (BflFwUploadService.ACTION_CHECKSUM_DATA_WRITABLE.equals(action)) {
                    if (sAutoProgressFlag && (intent.getStringExtra(BflFwUploadService.EXTRA_DATA) != null)) {
                        executeReadFirmwareDataCheck();
                    }
                    Log.d(BLE_FOTA_TAG, "Transmitting checksum data.");

                } else if (BflFwUploadService.ACTION_FIRMWARE_UPGRADE_TYPE_WRITABLE.equals(action)) {
                    if (sAutoProgressFlag && (intent.getStringExtra(BflFwUploadService.EXTRA_DATA) != null)) {
                        executeReadFirmwareStatus();
                    }

                    switch (sFirmwareUpgradeTypeFlag) {
                        case 0:
                            if (mDeviceInfoCallback != null) {
                                mDeviceInfoCallback.onDeviceInfoListener(
                                        UploadCode.DEVICE_FIRMWARE_UPGRADE_TYPE_NORMAL.getCode(),
                                        "0"
                                );
                            }
                            break;

                        case 1:
                            if (mDeviceInfoCallback != null) {
                                mDeviceInfoCallback.onDeviceInfoListener(
                                        UploadCode.DEVICE_FIRMWARE_UPGRADE_TYPE_FORCED.getCode(),
                                        "1"
                                );
                            }
                            break;
                    }
                    Log.d(BLE_FOTA_TAG, "Firmware update type: " + sFirmwareUpgradeTypeFlag);

                } else if (BflFwUploadService.ACTION_RESET_WRITABLE.equals(action)) {
                    if (mDeviceInfoCallback != null) {
                        mDeviceInfoCallback.onDeviceInfoListener(
                                UploadCode.DEVICE_FIRMWARE_RESET.getCode(),
                                null
                        );
                    }
                    Log.d(BLE_FOTA_TAG, "The target device is reset");

                } else if (BflFwUploadService.ACTION_DATA_WRITABLE.equals(action)) {
                    if (mDeviceInfoCallback != null) {
                        mDeviceInfoCallback.onDeviceInfoListener(
                                UploadCode.DEVICE_EXTRA_DATA.getCode(),
                                intent.getStringExtra(BflFwUploadService.EXTRA_DATA)
                        );
                    }
                }
            } else {
                Log.e(BLE_FOTA_TAG, "Firmware uploader broadcast data is NULL.");
            }
        }
    };

    /**
     * BLE FOTA firmware upload service connection.
     * Create service connection.
     *
     * @param address is the device MAC address.
     * @param initAutoProgress is auto progress setting for initial value.
     * @see kr.co.sevencore.blefotalib.BflFwUploadService
     */
    public void connectUploadSvc(String address, boolean initAutoProgress) {
        mAddress = address;
        sInitAutoProgressFlag = initAutoProgress;

        mUploadServiceIntent = new Intent(mContext, BflFwUploadService.class);
        // BLE FOTA Library uses daemon(local) background service using startService method to run independently
        // & remote service using bindService method to communicate by AIDL.
        mContext.startService(mUploadServiceIntent);
        mContext.bindService(mUploadServiceIntent, mBflUploadSvcConnection, mContext.BIND_AUTO_CREATE);
    }

    /**
     * BLE FOTA firmware upload service disconnection.
     *
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     * @see kr.co.sevencore.blefotalib.BflUtil
     */
    public void disconnectUploadSvc() {
        sInitAutoProgressFlag = false;

        if (BflUtil.isServiceRunning(mContext, BflFwUploadService.class)) {
            try {
                mContext.unbindService(mBflUploadSvcConnection);
                mContext.stopService(mUploadServiceIntent);
                mBflUploadBinder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create connection with the BLE device.
     *
     * @param address of the BLE device is used for creating connection.
     */
    public void connect(String address) {
        if (mBflUploadBinder != null) {
            try {
                mBflUploadBinder.connect(address);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close connection with the BLE device.
     */
    public void disconnect() {
        if (mBflUploadBinder != null) {
            try {
                mBflUploadBinder.disconnect();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * After closing connection by disconnect method,
     * system resources have to be released properly by close method.
     */
    public void close() {
        if (mBflUploadBinder != null) {
            try {
                mBflUploadBinder.close();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Register broadcast receiver.
     *
     * @param context is gettable from the caller application.
     * @see kr.co.sevencore.blefotalib.BflFwUploadService
     */
    public void registerBflUploadReceiver(Context context) {
        context.registerReceiver(mBflUploadBroadcastReceiver, makeGattUpdateIntentFilter());
    }

    /**
     * Unregister broadcast receiver.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService
     */
    public void unregisterBflUploadReceiver() {
        try {
            mContext.unregisterReceiver(mBflUploadBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set FOTA progress automation flag.
     *
     * @param flag is boolean value to be set.
     */
    public void setAutoProgressFlag(boolean flag) {
        sAutoProgressFlag = flag;
    }

    /**
     * Initialize FOTA progress related values.
     */
    private void initializeValue() {
        sLeftConnCnt = 0;

        sSequenceNumber = -1;
        sFirmwareUpgradeTypeFlag = 0;
    }

    /**
     * Update GATT service adapter.
     *
     * @param gattServiceData is services list.
     * @param gattCharacteristicData is characteristics list.
     * @return GATT service adapter.
     * @see kr.co.sevencore.blefotalib.BflFwUploadService
     */
    public SimpleExpandableListAdapter updateGattServicesAdapter(
            ArrayList<HashMap<String, String>> gattServiceData, ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData) {
        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                mContext,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {BflCodeList.LIST_NAME, BflCodeList.LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {BflCodeList.LIST_NAME, BflCodeList.LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        return gattServiceAdapter;
    }

    /**
     * Check more data is needed to be transmitted to the target device.
     *
     * @param filePath is the location that firmware data stored.
     * @param sequenceNumber is amount of data going to be transmitted.
     * @return true: More data is needed to be transmitted.
     *         false: Firmware data transmission finished.
     */
    private boolean checkSequence(String filePath, int sequenceNumber) {
        File firmwareFile = new File(filePath);
        long length = firmwareFile.length();
        int sequence = sequenceNumber;
        int sendSize;

        if (sequence < -1) {
            sequence += 256; // Used for overflow.
        }

        if ((length % BflFwUploadService.PURE_EACH_CONN_DATA_SIZE) != 0) {
            sendSize = (sequence * BflFwUploadService.EACH_CONN_DATA_SIZE)
                    + ((int) length % BflFwUploadService.PURE_EACH_CONN_DATA_SIZE);
        } else {
            sendSize = (sequence + 1) * BflFwUploadService.EACH_CONN_DATA_SIZE;
        }

        return (sendSize >= (int) length);
    }

    /**
     * Execute the specific BLE property.
     *
     * @param serviceIdx is index of services in adapter.
     * @param characteristicIdx is index of characteristics in adapter.
     * @return false: sAutoProgressFlag is disabled or BluetoothGattCharacteristic is null.
     */
    public boolean executeProperty(int serviceIdx, int characteristicIdx) {
        if (!sAutoProgressFlag) {
            try {
                // Return true: onWriteCharacteristic | onReadCharacteristic | setCharacteristicNotification callback.
                return mBflUploadBinder.checkProperty(serviceIdx, characteristicIdx);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 18FF FOTA service.
     * 2AF0 Firmware version characteristic.
     * READ property function of FOTA profile.
     */
    public void executeReadFirmwareCurrentVersion() {
        try {
            mBflUploadBinder.executeReadCharacteristic(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_VERSION.getCode()
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 18FF FOTA service.
     * 2AF1 Firmware new version characteristic
     * READ (| WRITE) property function of FOTA profile.
     */
    public void executeReadFirmwareNewVersion() {
        try {
            mBflUploadBinder.executeReadCharacteristic(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_NEW_VERSION.getCode()
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 18FF FOTA service.
     * 2AF3 Sequence number characteristic.
     * READ property function of FOTA profile.
     */
    public void executeReadSequenceNumber() {
        try {
            mBflUploadBinder.executeReadCharacteristic(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_SEQUENCE_NUMBER.getCode()
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 18FF FOTA service.
     * 2AF6 Firmware data check characteristic.
     * READ | NOTIFY property of FOTA profile.
     */
    public void executeReadFirmwareDataCheck() {
        try {
            mBflUploadBinder.executeReadCharacteristic(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_DATA_CHECK.getCode()
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 18FF FOTA service.
     * 2AF7 Firmware status characteristic.
     * READ | WRITE property function of FOTA profile.
     */
    public void executeReadFirmwareStatus() {
        try {
            mBflUploadBinder.executeReadCharacteristic(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_STATUS.getCode()
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 180A Device information which is included service.
     * 2A29 Manufacturer name characteristic.
     * READ property function of FOTA profile.
     */
    public void executeReadManufacturerName() {
        try {
            mBflUploadBinder.executeReadCharacteristic(
                    ServiceIdxCode.SERVICE_DEVICE_INFO.getCode(),
                    CharacteristicDeviceInfoIdxCode.CHARACTERISTIC_MANUFACTURER_NAME.getCode()
            );
        }catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 180A Device information which is included service.
     * 2A24 Model number characteristic.
     * READ property function of FOTA profile.
     */
    public void executeReadModelNumber() {
        try {
            mBflUploadBinder.executeReadCharacteristic(
                    ServiceIdxCode.SERVICE_DEVICE_INFO.getCode(),
                    CharacteristicDeviceInfoIdxCode.CHARACTERISTIC_MODEL_NUMBER.getCode()
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 180A Device information which is included service.
     * 2A25 Serial number characteristic.
     * READ property function of FOTA profile.
     */
    public void executeReadSerialNumber() {
        try {
            mBflUploadBinder.executeReadCharacteristic(
                    ServiceIdxCode.SERVICE_DEVICE_INFO.getCode(),
                    CharacteristicDeviceInfoIdxCode.CHARACTERISTIC_SERIAL_NUMBER.getCode()
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 18FF FOTA service.
     * 2AF1 Firmware new version characteristic.
     * (READ |) WRITE property function of FOTA profile.
     *
     * @param firmwareVersion is new version information of firmware data which is going to be updated.
     */
    public void executeWriteFirmwareNewVersion(String firmwareVersion) {
        try {
            mBflUploadBinder.executeWriteFirmwareNewVersion(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_NEW_VERSION.getCode(),
                    firmwareVersion
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 18FF FOTA service.
     * 2AF2 Firmware data characteristic.
     * WRITE property function of FOTA profile.
     *
     * @param filePath is the location of the new firmware data in the smart device.
     * @param sequenceNumber is the start location information of the firmware data which is going to be transmitted.
     */
    public void executeFirmwareUpgrade(String filePath, int sequenceNumber) {
        try {
            mBflUploadBinder.executeWriteFirmwareData(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_DATA.getCode(),
                    filePath,
                    sequenceNumber
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 18FF FOTA service.
     * 2AF4 Checksum data characteristic.
     * WRITE property function of FOTA profile.
     *
     * @param filePath is the location of the checksum data which is generated from firmware data.
     */
    public void executeWriteChecksumData(String filePath) {
        try {
            mBflUploadBinder.executeWriteChecksumData(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_CHECKSUM_DATA.getCode(),
                    filePath
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 18FF FOTA service.
     * 2AF6 Firmware upgrade type characteristic.
     * WRITE property function of FOTA profile.
     *
     * @param typeFlag is normal or forced upgrade type.
     */
    public void executeWriteFirmwareUpgradeType(byte typeFlag) {
        try {
            mBflUploadBinder.executeWriteFirmwareUpgradeType(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_FIRMWARE_UPGRADE_TYPE.getCode(),
                    typeFlag
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 18FF FOTA service.
     * 2AF8 Reset characteristic.
     * WRITE property function of FOTA profile.
     *
     * @param resetFlag is reset command flag.
     */
    public void executeWriteReset(byte resetFlag) {
        try {
            mBflUploadBinder.executeWriteReset(
                    ServiceIdxCode.SERVICE_FIRMWARE_UPGRADE.getCode(),
                    CharacteristicFotaIdxCode.CHARACTERISTIC_RESET.getCode(),
                    resetFlag
            );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Intent filter for BflFwUploadService.
     *
     * ACTION_GATT_CONNECTING: Bluetooth GATT connection creating state.
     * ACTION_GATT_CONNECTED: Bluetooth GATT connection created state.
     * ACTION_GATT_DISCONNECTING: Bluetooth GATT connection closing state.
     * ACTION_GATT_DISCONNECTED: Bluetooth GATT connection closed state.
     * ACTION_GATT_SERVICES_DISCOVERED: Bluetooth GATT service list discovered.
     * ACTION_GATT_DATA_AVAILABLE: Bluetooth GATT service list get.
     *
     * Read properties of FOTA service.
     * ACTION_FIRMWARE_CURRENT_VERSION_AVAILABLE: The target device current firmware version information.
     * ACTION_FIRMWARE_NEW_VERSION_AVAILABLE: The target device new firmware version information to be going to be updated.
     * ACTION_SEQUENCE_NUMBER_AVAILABLE: The sequence number is used to manage firmware data transmission.
     * ACTION_FIRMWARE_DATA_CHECK_AVAILABLE: Firmware data integrity information.
     * ACTION_FIRMWARE_STATUS_AVAILABLE: Firmware status information.
     *
     * Read properties of device information service.
     * ACTION_MANUFACTURER_NAME_AVAILABLE: Manufacturer information.
     * ACTION_MODEL_NUMBER_AVAILABLE: Model number information.
     * ACTION_SERIAL_NUMBER_AVAILABLE: Serial number information.
     *
     * ACTION_DATA_AVAILABLE: Read data.
     *
     * Write properties of FOTA service.
     * ACTION_FIRMWARE_NEW_VERSION_WRITABLE: Write new firmware version information.
     * ACTION_FIRMWARE_DATA_WRITABLE: Firmware data transmission.
     * ACTION_SEQUENCE_NUMBER_WRITABLE: The sequence number is used to manage firmware data transmission.
     * ACTION_CHECKSUM_DATA_WRITABLE: Checksum data checks integrity of the firmware data.
     * ACTION_FIRMWARE_UPGRADE_TYPE_WRITABLE: Apply firmware upgrade type.
     * ACTION_RESET_WRITABLE: The target device reset.
     *
     * ACTION_DATA_WRITABLE: Write data.
     *
     * @return Intent filter.
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BflFwUploadService.ERROR_LOST_GATT);
        intentFilter.addAction(BflFwUploadService.ERROR_LOST_DEVICE_INFORMATION);

        intentFilter.addAction(BflFwUploadService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(BflFwUploadService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BflFwUploadService.ACTION_GATT_DISCONNECTING);
        intentFilter.addAction(BflFwUploadService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BflFwUploadService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BflFwUploadService.ACTION_GATT_DATA_AVAILABLE);
        // Read properties.
        intentFilter.addAction(BflFwUploadService.ACTION_FIRMWARE_CURRENT_VERSION_AVAILABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_FIRMWARE_NEW_VERSION_AVAILABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_SEQUENCE_NUMBER_AVAILABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_FIRMWARE_DATA_CHECK_AVAILABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_FIRMWARE_STATUS_AVAILABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_MANUFACTURER_NAME_AVAILABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_MODEL_NUMBER_AVAILABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_SERIAL_NUMBER_AVAILABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_DATA_AVAILABLE);
        // Write properties.
        intentFilter.addAction(BflFwUploadService.ACTION_FIRMWARE_NEW_VERSION_WRITABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_FIRMWARE_DATA_WRITABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_SEQUENCE_NUMBER_WRITABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_CHECKSUM_DATA_WRITABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_FIRMWARE_UPGRADE_TYPE_WRITABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_RESET_WRITABLE);
        intentFilter.addAction(BflFwUploadService.ACTION_DATA_WRITABLE);
        return intentFilter;
    }
}
