package kr.co.sevencore.blefotalib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * BflFwUploadService.java
 * BLE FOTA Library Firmware Upload Service.
 * This service executes Bluetooth properties of BLE FOTA.
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-04-08
 * @see kr.co.sevencore.blefotalib.BflFwUploader
 * @see kr.co.sevencore.blefotalib.IBflFwUploadSvc
 * @see kr.co.sevencore.blefotalib.BflAttributes
 */
public class BflFwUploadService extends Service {
    private final static String BLE_FOTA_TAG = BflFwUploadService.class.getSimpleName();

    private BluetoothManager mBflBluetoothManager;
    private BluetoothAdapter mBflBluetoothAdapter;
    private BluetoothGatt mBflBluetoothGatt;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mBflGattCharacteristics;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private String mBleDeviceAddress;
    private Context mContext;

    // Reserved for immortal background (device scanning) service.
    /*private static PowerManager sPowerManager;
    private static PowerManager.WakeLock sCpuWakeLock = null;*/

    private boolean mContinuousWriteFlag = false;      // When a GATT operation completed successfully, client is able to write continuously.
    private boolean mWritableNewVersionFlag = false;  // Writable firmware new version flag - true: Write, false: Read.
    private boolean mWritableSeqNumFlag = false;       // Writable sequence number flag - true: Write, false: Read.
    private boolean mReadableDataChkFlag = true;       // Readable firmware data check flag - true: Read, false: Notify.
    private boolean mReadableStatusFlag = true;        // Readable firmware status flag - true: Read, false: Notify.

    private final int VERSION_LENGTH = 8;               // Length of version information.

    public static int sLeftConnCnt = 0;
    private static boolean sConnCheck = true;

    public final static int PURE_EACH_CONN_DATA_SIZE = 509; // Maximum firmware data size of each connection event: 509 bytes.
    public final static int EACH_CONN_DATA_SIZE = 512;      // Maximum data size of each connection event: 512 bytes.
    public final static int EACH_CONN_DATA_INFO = 3;        // Sequence number & data size inforamtion of each connection event.

    public final static String ERROR_LOST_GATT =
            "kr.co.sevencore.ble.fota.lib.upload.ERROR_LOST_GATT";
    public final static String ERROR_LOST_DEVICE_INFORMATION =
            "kr.co.sevencore.ble.fota.lib.upload.ERROR_LOST_DEVICE_INFORMATION";
    public final static String ACTION_GATT_CONNECTING =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_CONNECTED =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTING =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_GATT_DISCONNECTING";
    public final static String ACTION_GATT_DISCONNECTED =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_DATA_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_GATT_DATA_AVAILABLE";
    public final static String ACTION_FIRMWARE_CURRENT_VERSION_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_FIRMWARE_VERSION_AVAILABLE";
    public final static String ACTION_FIRMWARE_NEW_VERSION_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_FIRMWARE_NEW_VERSION_AVAILABLE";
    public final static String ACTION_SEQUENCE_NUMBER_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_SEQUENCE_NUMBER_AVAILABLE";
    public final static String ACTION_FIRMWARE_DATA_CHECK_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_FIRMWARE_DATA_CHECK_AVAILABLE";
    public final static String ACTION_FIRMWARE_STATUS_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_FIRMWARE_STATUS_AVAILABLE";
    public final static String ACTION_MANUFACTURER_NAME_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_MANUFACTURER_NAME_AVAILABLE";
    public final static String ACTION_MODEL_NUMBER_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_MODEL_NUMBER_AVAILABLE";
    public final static String ACTION_SERIAL_NUMBER_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_SERIAL_NUMBER_AVAILABLE";
    public final static String ACTION_DATA_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_DATA_AVAILABLE";
    public final static String ACTION_FIRMWARE_NEW_VERSION_WRITABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_FIRMWARE_NEW_VERSION_WRITABLE";
    public final static String ACTION_FIRMWARE_DATA_WRITABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_FIRMWARE_DATA_WRITABLE";
    public final static String ACTION_SEQUENCE_NUMBER_WRITABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_SEQUENCE_NUMBER_WRITABLE";
    public final static String ACTION_CHECKSUM_DATA_WRITABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_CHECKSUM_DATA_WRITABLE";
    public final static String ACTION_FIRMWARE_UPGRADE_TYPE_WRITABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_FIRMWARE_UPGRADE_TYPE_WRITABLE";
    public final static String ACTION_RESET_WRITABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_RESET_WRITABLE";
    public final static String ACTION_DATA_WRITABLE =
            "kr.co.sevencore.ble.fota.lib.upload.ACTION_DATA_WRITABLE";
    public final static String GATT_SERVICE_DATA_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.GATT_SERVICE_DATA_AVAILABLE";
    public final static String GATT_CHARACTERISTIC_DATA_AVAILABLE =
            "kr.co.sevencore.ble.fota.lib.upload.GATT_CHARACTERISTIC_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "kr.co.sevencore.ble.fota.lib.upload.EXTRA_DATA";

    // Firmware upgrade profile attribute UUIDs.
    public final static UUID UUID_FIRMWARE_VERSION =
            UUID.fromString(BflAttributes.FIRMWARE_VERSION);
    public final static UUID UUID_FIRMWARE_NEW_VERSION =
            UUID.fromString(BflAttributes.FIRMWARE_NEW_VERSION);
    public final static UUID UUID_FIRMWARE_DATA =
            UUID.fromString(BflAttributes.FIRMWARE_DATA);
    public final static UUID UUID_SEQUENCE_NUMBER =
            UUID.fromString(BflAttributes.SEQUENCE_NUMBER);
    public final static UUID UUID_CHECKSUM_DATA =
            UUID.fromString(BflAttributes.CHECKSUM_DATA);
    public final static UUID UUID_FIRMWARE_DATA_CHECK =
            UUID.fromString(BflAttributes.FIRMWARE_DATA_CHECK);
    public final static UUID UUID_FIRMWARE_UPGRADE_TYPE =
            UUID.fromString(BflAttributes.FIRMWARE_UPGRADE_TYPE);
    public final static UUID UUID_FIRMWARE_STATUS =
            UUID.fromString(BflAttributes.FIRMWARE_STATUS);
    public final static UUID UUID_RESET =
            UUID.fromString(BflAttributes.RESET);
    public final static UUID UUID_MANUFACTURER_NAME =
            UUID.fromString(BflAttributes.MANUFACTURER_NAME);
    public final static UUID UUID_MODEL_NUMBER =
            UUID.fromString(BflAttributes.MODEL_NUMBER);
    public final static UUID UUID_SERIAL_NUMBER =
            UUID.fromString(BflAttributes.SERIAL_NUMBER);


    public BflFwUploadService() {}

    public BflFwUploadService(Context context) {
        //mContext = context;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBflUploadBinder;
    }

    IBflFwUploadSvc.Stub mBflUploadBinder = new IBflFwUploadSvc.Stub() {
        /**
         * Initializes a reference to the local Bluetooth adapter.
         *
         * @return Return true if the initialization is successful.
         * @throws RemoteException
         */
        @Override
        public boolean initUploader() throws RemoteException {
            return initialize();
        }

        /**
         * Create connection with the BLE device.
         *
         * @param address is the BLE device MAC address to be connected.
         * @return is a result of creating connection with the BLE device.
         * @throws RemoteException
         */
        @Override
        public boolean connect(final String address) throws RemoteException {
            return createConnection(address);
        }

        /**
         * Disconnect an existing connection or cancel a pending connection.
         * The disconnection result is reported asynchronously through the
         * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int}
         * callback.
         *
         * @throws RemoteException
         */
        @Override
        public void disconnect() throws RemoteException{
            if (mBflBluetoothAdapter == null || mBflBluetoothGatt == null) {
                return;
            }
            mBflBluetoothGatt.disconnect();
        }

        /**
         * Return system resources.
         * After using a given BLE device,
         * the app must call this method to ensure resources are released properly.
         *
         * @throws RemoteException
         */
        @Override
        public void close() throws RemoteException {
            if (mBflBluetoothGatt == null) {
                return;
            }

            mBflBluetoothGatt.close();
            mBflBluetoothGatt = null;
        }

        /**
         * Update GATT services.
         *
         * @throws RemoteException
         */
        @Override
        public void updateGatt () throws RemoteException{
            updateGattServices(getSupportedGattServices());
        }

        /**
         * Check BLE property.
         *
         * @param serviceIdx is the service index of the FOTA profile.
         * @param characteristicIdx is the characteristic index of the FOTA profile.
         * @return false, if mBflGattCharacteristics is not available.
         * @throws RemoteException
         */
        @Override
        public boolean checkProperty(int serviceIdx, int characteristicIdx) throws RemoteException {
            if (mBflGattCharacteristics != null) {
                final BluetoothGattCharacteristic characteristic =
                        mBflGattCharacteristics.get(serviceIdx).get(characteristicIdx);
                final int characteristicProperty = characteristic.getProperties();

                if ((characteristicProperty | BluetoothGattCharacteristic.PROPERTY_READ) > 0) { // READ property: 0X00000002
                    // If there is an active notification on a characteristic,
                    // clear it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        setCharacteristicNotification(mNotifyCharacteristic, false);
                        mNotifyCharacteristic = null;
                        Log.i(BLE_FOTA_TAG, "READ Characteristic property: " + characteristicProperty);
                    }
                    writeBflCharacteristic(characteristic);
                }
                if ((characteristicProperty | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) { // WRITE property: 0X00000008
                    // If there is an active notification on a characteristic,
                    // clear it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        setCharacteristicNotification(mNotifyCharacteristic, false);
                        mNotifyCharacteristic = null;
                        Log.i(BLE_FOTA_TAG, "WRITE Characteristic property: " + characteristicProperty);
                    }
                    writeBflCharacteristic(characteristic);
                }
                if ((characteristicProperty | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) { // NOTIFY property: 0X00000010
                    mNotifyCharacteristic = characteristic;
                    setCharacteristicNotification(characteristic, true);
                    Log.i(BLE_FOTA_TAG, "NOTIFICATION Characteristic property: " + characteristicProperty);
                }
                return true;
            }
            return false;
        }

        /**
         * Read request for FOTA profile execution.
         *
         * @param serviceIdx is FOTA profile service index.
         * @param characteristicIdx is a characteristic index of FOTA services.
         * @throws RemoteException
         */
        @Override
        public void executeReadCharacteristic(int serviceIdx, int characteristicIdx) throws RemoteException {
            if (mBflGattCharacteristics == null) {
                return;
            }
            try {
                final BluetoothGattCharacteristic characteristic = mBflGattCharacteristics.
                        get(serviceIdx).get(characteristicIdx);

                readBflCharacteristic(characteristic);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Write request for firmware version information of new firmware due to be upgraded.
         *
         * @param serviceIdx is FOTA profile service index.
         * @param characteristicIdx is a characteristic index of FOTA service.
         * @param firmwareVersion The Firmware Version is new one to write.
         * @throws RemoteException
         */
        @Override
        public void executeWriteFirmwareNewVersion(int serviceIdx, int characteristicIdx, String firmwareVersion) throws RemoteException {
            if (mBflGattCharacteristics == null) {
                return;
            }
            mWritableNewVersionFlag = true;
            mWritableSeqNumFlag = false;

            final BluetoothGattCharacteristic characteristic = mBflGattCharacteristics.
                    get(serviceIdx).get(characteristicIdx);

            byte[] byteData = firmwareVersion.getBytes();

            characteristic.setValue(byteData);
            writeBflCharacteristic(characteristic);
        }

        /**
         * Write request for firmware data of new version.
         *
         * @param serviceIdx is FOTA profile service index.
         * @param characteristicIdx is a characteristic index of FOTA service.
         * @param filePath is the location of the firmware data.
         * @param sequenceNumber is the size related sequence number information of the firmware data to be transmitted.
         */
        @Override
        public void executeWriteFirmwareData(int serviceIdx, int characteristicIdx, String filePath, int sequenceNumber) throws RemoteException {
            if (mBflGattCharacteristics == null) {
                return;
            }
            final BluetoothGattCharacteristic characteristic = mBflGattCharacteristics.
                    get(serviceIdx).get(characteristicIdx);

            File binFile = new File(filePath);
            long length = binFile.length();
            byte[] byteData = getByteFromFile(binFile, length);

            int checkedSeqNumber = checkNegative(sequenceNumber);

            SplitBytesThread splitBytesThread = new SplitBytesThread(byteData, length, checkedSeqNumber, characteristic);
            splitBytesThread.start();
        }

        /**
         * Sequence Number of Firmware Data Write Request.
         *
         * @param serviceIdx is FOTA profile service index.
         * @param characteristicIdx is a characteristic index of FOTA service.
         * @param index is a sequence number of the firmware data.
         * @throws RemoteException
         */
        @Override
        public void executeWriteSequenceNumber(int serviceIdx, int characteristicIdx, int index) throws RemoteException {
            if (mBflGattCharacteristics == null) {
                return;
            }
            mWritableSeqNumFlag = true;

            final BluetoothGattCharacteristic characteristic = mBflGattCharacteristics.
                    get(serviceIdx).get(characteristicIdx);

            byte[] byteData = new byte[1];
            byteData[0] = (byte) index;

            characteristic.setValue(byteData);
            writeBflCharacteristic(characteristic);
        }

        /**
         * Checksum Data of Firmware Data.
         *
         * @param serviceIdx is FOTA profile service index.
         * @param characteristicIdx is a characteristic index of FOTA service.
         * @param filePath is the location of the firmware data.
         * @throws RemoteException
         */
        @Override
        public void executeWriteChecksumData(int serviceIdx, int characteristicIdx, String filePath) throws RemoteException {
            if (mBflGattCharacteristics == null) {
                return;
            }
            final BluetoothGattCharacteristic characteristic = mBflGattCharacteristics.
                    get(serviceIdx).get(characteristicIdx);

            File binFile = new File(filePath);
            long length = binFile.length();

            try {
                byte[] byteData = makeChecksum(binFile, length);

                characteristic.setValue(byteData);
                writeBflCharacteristic(characteristic);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        /**
         * Set firmware upgrade type of the target device.
         *
         * @param serviceIdx is FOTA profile service index.
         * @param characteristicIdx is a characteristic index of FOTA service.
         * @param typeFlag is thr flag of firmware upgrade type.
         * @throws RemoteException
         */
        @Override
        public void executeWriteFirmwareUpgradeType(int serviceIdx, int characteristicIdx, byte typeFlag) throws RemoteException {
            if (mBflGattCharacteristics == null) {
                return;
            }
            final BluetoothGattCharacteristic characteristic = mBflGattCharacteristics.
                    get(serviceIdx).get(characteristicIdx);

            byte[] byteData = new byte[1];
            byteData[0] = typeFlag;

            characteristic.setValue(byteData);
            writeBflCharacteristic(characteristic);
        }

        /**
         * Reset the target device.
         *
         * @param serviceIdx is FOTA profile service index.
         * @param characteristicIdx is a characteristic index of FOTA service.
         * @param resetFlag is the flag of rest.
         * @throws RemoteException
         */
        @Override
        public void executeWriteReset(int serviceIdx, int characteristicIdx, byte resetFlag) throws RemoteException {
            if (mBflGattCharacteristics == null) {
                return;
            }
            final BluetoothGattCharacteristic characteristic = mBflGattCharacteristics.
                    get(serviceIdx).get(characteristicIdx);

            byte[] byteData = new byte[1];
            byteData[0] = resetFlag;

            characteristic.setValue(byteData);
            writeBflCharacteristic(characteristic);
        }

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
                               double aDouble, String aString) {}
    };

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    private boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBflBluetoothManager == null) {
            mBflBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBflBluetoothManager == null) {
                Log.e(BLE_FOTA_TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBflBluetoothAdapter = mBflBluetoothManager.getAdapter();
        if (mBflBluetoothAdapter == null) {
            Log.e(BLE_FOTA_TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Create connection with the BLE device.
     *
     * @param address is the BLE device MAC address to be connected.
     * @return is a result of creating connection with the BLE device.
     */
    private boolean createConnection(final String address) {
        if (mBflBluetoothAdapter == null || address == null) {
            Log.w(BLE_FOTA_TAG, "BluetoothAdapter is not initialized or unspecified address.");
            return false;
        }

        // If the device is previously connected device, try to re-connect,
        if (mBflBluetoothGatt != null) {
            if (mBleDeviceAddress != null && address.equals(mBleDeviceAddress)) {
                Log.i(BLE_FOTA_TAG, "Trying to use an existing Bluetooth GATT for connection.");

                if (mBflBluetoothGatt.connect()) {
                    Log.i(BLE_FOTA_TAG, "Creating connection with the existing Bluetooth GATT.");
                    return true;
                } else {
                    return false;
                }
            } else {
                broadcastUpdate(ERROR_LOST_DEVICE_INFORMATION);
                Log.d(BLE_FOTA_TAG, "ADDRESS is NULL");
            }
        } else {
            broadcastUpdate(ERROR_LOST_GATT);
            Log.d(BLE_FOTA_TAG, "BLUETOOTH GATT is NULL");
        }

        // Find remote device & make connection state. (Connection to GATT server)
        final BluetoothDevice device = mBflBluetoothAdapter.getRemoteDevice(address);
        if(device == null) {
            Log.w(BLE_FOTA_TAG, "Device not found. Unable to connect.");
            return false;
        }

        mBflGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        mContext = getApplicationContext();
        // Set the autoConnect parameter to false.
        // If you want to directly connect to the device,
        // parameter: device.connectCatt(CONTEXT, (IF FIND) AUTO_CONNECT, BLUETOOTH_CALLBACK);
        mBflBluetoothGatt = device.connectGatt(mContext, true, mGattCallback);
        Log.i(BLE_FOTA_TAG, "Trying to create a new connection.");
        mBleDeviceAddress = address;
        return true;
    }

    /**
     * Callback methods for GATT events that the app cares about.
     * For example, connection change and service discovered.
     *
     * onConnectionStateChange: Update connection state.
     * onServiceDiscovered: New services discovered.
     * onCharacteristicRead: Callback triggered as a result of a characteristic read operation.
     * onCharacteristicWrite: Callback triggered as a result of a characteristic write operation.
     * onCharacteristicChanged: Callback triggered as a result of a remote characteristic notification.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTING) {
                intentAction = ACTION_GATT_CONNECTING;
                broadcastUpdate(intentAction);
                Log.i(BLE_FOTA_TAG, "Connecting to GATT server.");

            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                // Attempt to discover services after successful connection.
                Log.i(BLE_FOTA_TAG, "Connected to GATT server. " +
                        "Attempting to start service discovery: " +
                        mBflBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                intentAction = ACTION_GATT_DISCONNECTING;
                broadcastUpdate(intentAction);
                Log.i(BLE_FOTA_TAG, "Disconnecting from GATT server.");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                broadcastUpdate(intentAction);
                Log.i(BLE_FOTA_TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                mContinuousWriteFlag = true;
                Log.i(BLE_FOTA_TAG, "GATT services discovered.");
            } else {
                Log.w(BLE_FOTA_TAG, "onServiceDiscovered received: " + status);

                /*try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBflBluetoothGatt.discoverServices();*/
                mBflBluetoothAdapter.disable();

                Timer initAdapterTimer = new Timer();
                initAdapterTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mBflBluetoothAdapter.enable();
                    }
                }, 1000);

                Timer connCtrlTimer = new Timer();
                connCtrlTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        initialize();
                        createConnection(mBleDeviceAddress);
                    }
                }, 2000);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (UUID_FIRMWARE_VERSION.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_FIRMWARE_CURRENT_VERSION_AVAILABLE, characteristic);
                } else if (UUID_FIRMWARE_NEW_VERSION.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_FIRMWARE_NEW_VERSION_AVAILABLE, characteristic);
                } else if (UUID_SEQUENCE_NUMBER.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_SEQUENCE_NUMBER_AVAILABLE, characteristic);
                } else if (UUID_FIRMWARE_DATA_CHECK.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_FIRMWARE_DATA_CHECK_AVAILABLE, characteristic);
                } else if (UUID_FIRMWARE_STATUS.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_FIRMWARE_STATUS_AVAILABLE, characteristic);
                } else if (UUID_MANUFACTURER_NAME.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_MANUFACTURER_NAME_AVAILABLE, characteristic);
                } else if (UUID_MODEL_NUMBER.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_MODEL_NUMBER_AVAILABLE, characteristic);
                } else if (UUID_SERIAL_NUMBER.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_SERIAL_NUMBER_AVAILABLE, characteristic);
                } else {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    Log.i(BLE_FOTA_TAG, "onCharacteristicRead callback.");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (UUID_FIRMWARE_NEW_VERSION.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_FIRMWARE_NEW_VERSION_WRITABLE, characteristic);
                } else if (UUID_FIRMWARE_DATA.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_FIRMWARE_DATA_WRITABLE, characteristic);
                } else if (UUID_SEQUENCE_NUMBER.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_SEQUENCE_NUMBER_WRITABLE, characteristic);
                } else if (UUID_CHECKSUM_DATA.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_CHECKSUM_DATA_WRITABLE, characteristic);
                } else if (UUID_FIRMWARE_UPGRADE_TYPE.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_FIRMWARE_UPGRADE_TYPE_WRITABLE, characteristic);
                } else if (UUID_RESET.equals(characteristic.getUuid())) {
                    broadcastUpdate(ACTION_RESET_WRITABLE, characteristic);
                } else {
                    broadcastUpdate(ACTION_DATA_WRITABLE, characteristic);
                    Log.i(BLE_FOTA_TAG, "onCharacteristicWrite callback.");
                }
                mContinuousWriteFlag = true;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    /**
     * Update GATT services & characteristics to be available.
     *
     * @param gattServices GATT services list.
     * @see kr.co.sevencore.blefotalib.BflAttributes
     */
    private void updateGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices != null || mContext != null) {
            String uuid = null;
            String unknownServiceStr = mContext.getResources().getString(R.string.unknown_service);
            String unknownServiceCharacteristicStr = mContext.getResources().getString(R.string.unknown_characteristic);
            ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
            ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData =
                    new ArrayList<ArrayList<HashMap<String, String>>>();
            mBflGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

            // Loops through available GATT services.
            for (BluetoothGattService gattService : gattServices) {
                HashMap<String, String> currentServiceData = new HashMap<String, String>();
                uuid = gattService.getUuid().toString();
                currentServiceData.put(
                        // Show service name, if the service is defined at BflAttributes of BLE FOTA Library.
                        BflCodeList.LIST_NAME, BflAttributes.lookup(uuid, unknownServiceStr));
                currentServiceData.put(BflCodeList.LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                        new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> characteristics =
                        new ArrayList<BluetoothGattCharacteristic>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    characteristics.add(gattCharacteristic);
                    HashMap<String, String> currentCharacteristicData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharacteristicData.put(
                            // Show characteristic name, if the characteristic is defined at BflAttributes of BLE FOTA Library.
                            BflCodeList.LIST_NAME, BflAttributes.lookup(uuid, unknownServiceCharacteristicStr));
                    currentCharacteristicData.put(BflCodeList.LIST_UUID, uuid);
                    gattCharacteristicGroupData.add(currentCharacteristicData);
                }
                mBflGattCharacteristics.add(characteristics);
                gattCharacteristicData.add(gattCharacteristicGroupData);
            }
            broadcastUpdate(ACTION_GATT_DATA_AVAILABLE, gattServiceData, gattCharacteristicData);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device.
     * This should be invoked only after
     * {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBflBluetoothGatt == null) {
            return null;
        }
        // Return a list of GATT services offered by the remote device.
        return mBflBluetoothGatt.getServices();
    }

    /**
     * Broadcast update to notify BLE connection status.
     *
     * @param action is one of upload service broadcast cations.
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Broadcast update to notify GATT service list information.
     *
     * @param action is one of upload service broadcast cations.
     * @param gattServiceData is a BLE service list.
     * @param gattCharacteristicData is a BLE characteristic list.
     */
    private void broadcastUpdate(final String action,
                                 final ArrayList gattServiceData, final ArrayList gattCharacteristicData) {
        final Intent intent = new Intent(action);
        intent.putExtra(GATT_SERVICE_DATA_AVAILABLE, gattServiceData);
        intent.putExtra(GATT_CHARACTERISTIC_DATA_AVAILABLE, gattCharacteristicData);
        sendBroadcast(intent);
    }

    /**
     * Broadcast update to notify FOTA progress information.
     *
     * @param action is one of upload service broadcast cations.
     * @param characteristic Bluetooth GATT characteristic to be executed.
     */
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is a special handling for the BLE FOTA profile.
        // Data parsing is carried out as per profile specifications.
        if (UUID_FIRMWARE_VERSION.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: READ
            //Log.d(BLE_FOTA_TAG, "UUID firmware version property value: " + flag);

            // Flag: Characteristic property - READ: 0x02
            if ((flag & 0x02) != 0) {
                final byte[] firmwareVersionData = characteristic.getValue();

                if (firmwareVersionData != null && firmwareVersionData.length > 0) {
                    intent.putExtra(EXTRA_DATA, new String(firmwareVersionData).substring(0, VERSION_LENGTH));
                }
            }
        } else if (UUID_FIRMWARE_NEW_VERSION.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: READ | WRITE
            //Log.d(BLE_FOTA_TAG, "UUID firmware new version property value: " + flag);

            // Flag: Characteristic property - READ: 0x02
            if (((flag & 0x02) != 0) && (!mWritableNewVersionFlag)) {
                final byte[] firmwareNewVersionData = characteristic.getValue();

                if (firmwareNewVersionData != null && firmwareNewVersionData.length > 0) {
                    intent.putExtra(EXTRA_DATA, new String(firmwareNewVersionData).substring(0, VERSION_LENGTH));
                }
            }

            // Flag: Characteristic property - WRITE: 0x08
            if (((flag & 0x08) != 0) && (mWritableNewVersionFlag)) {
                intent.putExtra(EXTRA_DATA, "FIRMWARE NEW VERSION SET");
            }
        } else if (UUID_FIRMWARE_DATA.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: WRITE
            //Log.d(BLE_FOTA_TAG, "UUID firmware data property value: " + flag);

            // Flag: Characteristic property - WRITE: 0x08
            if ((flag & 0x08) != 0) {
                intent.putExtra(EXTRA_DATA, Integer.toString(sLeftConnCnt));
            }
        } else if (UUID_SEQUENCE_NUMBER.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: READ | WRITE
            //Log.d(BLE_FOTA_TAG, "UUID sequence number property value: " + flag);

            // Flag: Characteristic property - READ: 0x02
            if (((flag & 0x02) != 0) && (!mWritableSeqNumFlag)) {
                final byte[] sequenceNumberInfo = characteristic.getValue();

                if (sequenceNumberInfo != null && sequenceNumberInfo.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(sequenceNumberInfo.length);

                    for (byte byteChar : sequenceNumberInfo) {
                        stringBuilder.append(String.format("%d", byteChar));
                    }
                    intent.putExtra(EXTRA_DATA, stringBuilder.toString());
                }
            }

            // Flag: Characteristic property - WRITE: 0x08
            if (((flag & 0x08) != 0) && (mWritableSeqNumFlag)) {
                intent.putExtra(EXTRA_DATA, "TRANSMITTED " + characteristic.getValue().toString() + " DATA");
            }
        } else if (UUID_CHECKSUM_DATA.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: WRITE
            //Log.d(BLE_FOTA_TAG, "UUID checksum data property value: " + flag);

            // Flag: Characteristic property - WRITE: 0x08
            if ((flag & 0x08) != 0) {
                intent.putExtra(EXTRA_DATA, "CHECKSUM DATA TRANSMITTED");
            }
        } else if (UUID_FIRMWARE_DATA_CHECK.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: NOTIFY | READ
            //Log.d(BLE_FOTA_TAG, "UUID firmware data check property value: " + flag);

            // Flag: Characteristic property - READ: 0x02
            if (((flag & 0x02) != 0) && (mReadableDataChkFlag)) {
                final byte[] firmwareDataCheckData = characteristic.getValue();

                if (firmwareDataCheckData != null && firmwareDataCheckData.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(firmwareDataCheckData.length);

                    for (byte byteChar : firmwareDataCheckData) {
                        stringBuilder.append(String.format("%d", byteChar));
                    }
                    intent.putExtra(EXTRA_DATA, stringBuilder.toString());
                }
            }

            if (!mReadableDataChkFlag) {
                int format = -1;

                // Flag: Characteristic property - NOTIFY: 0x10
                if ((flag & 0x10) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(BLE_FOTA_TAG, "BLE FOTA property - firmware data check: format UINT16");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d(BLE_FOTA_TAG, "BLE FOTA property - firmware data check: format UINT8");
                }

                final int firmwareDataCheck = characteristic.getIntValue(format, 1);
                Log.d(BLE_FOTA_TAG, String.format("FIRMWARE DATA CHECK: %d ", firmwareDataCheck));
                intent.putExtra(EXTRA_DATA, String.valueOf(firmwareDataCheck));
            }
        } else if (UUID_FIRMWARE_UPGRADE_TYPE.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: WRITE
            //Log.d(BLE_FOTA_TAG, "UUID firmware upgrade type property value: " + flag);

            // Flag: Characteristic property - WRITE: 0x08
            if ((flag & 0x08) != 0) {
                intent.putExtra(EXTRA_DATA, "FIRMWARE UPGRADE TYPE APPLIED");
            }
        } else if (UUID_FIRMWARE_STATUS.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: NOTIFY | READ
            //Log.d(BLE_FOTA_TAG, "UUID firmware status property value: " + flag);

            // Flag: Characteristic property - READ: 0x02
            if (((flag & 0x02) != 0) && (mReadableStatusFlag)) {
                final byte[] firmwareStatusData = characteristic.getValue();

                if (firmwareStatusData != null && firmwareStatusData.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(firmwareStatusData.length);

                    for (byte byteChar : firmwareStatusData) {
                        stringBuilder.append(String.format("%d", byteChar));
                    }
                    intent.putExtra(EXTRA_DATA, stringBuilder.toString());
                }
            }

            if (!mReadableStatusFlag) {
                int format = -1;

                // Flag: Characteristic property - NOTIFY: 0x10
                if ((flag & 0x10) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(BLE_FOTA_TAG, "FIRMWARE STATUS: format UINT16");
                }

                final int firmwareStatus = characteristic.getIntValue(format, 1);
                Log.d(BLE_FOTA_TAG, String.format("FIRMWARE STATUS: %d ", firmwareStatus));
                intent.putExtra(EXTRA_DATA, String.valueOf(firmwareStatus));
            }
        } else if (UUID_RESET.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: WRITE
            //Log.d(BLE_FOTA_TAG, "UUID reset property value: " + flag);

            // Flag: Characteristic property - WRITE: 0x08
            if ((flag & 0x08) != 0) {
                intent.putExtra(EXTRA_DATA, "RESET THE TARGET DEVICE NOW");
            }
        } else if (UUID_MANUFACTURER_NAME.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: READ
            //Log.d(BLE_FOTA_TAG, "UUID manufacturer name property value: " + flag);

            // Flag: Characteristic property - READ: 0x02
            if ((flag & 0x02) != 0) {
                final byte[] manufacturerName = characteristic.getValue();

                if (manufacturerName != null && manufacturerName.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(manufacturerName.length);

                    for (byte byteChar : manufacturerName) {
                        stringBuilder.append(String.format("%c", byteChar));
                    }
                    intent.putExtra(EXTRA_DATA, stringBuilder.toString());
                }
            }
        } else if (UUID_MODEL_NUMBER.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: READ
            //Log.d(BLE_FOTA_TAG, "UUID model number property value: " + flag);

            // Flag: Characteristic property - READ: 0x02
            if ((flag & 0x02) != 0) {
                final byte[] modelNumber = characteristic.getValue();

                if (modelNumber != null && modelNumber.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(modelNumber.length);

                    for (byte byteChar : modelNumber) {
                        stringBuilder.append(String.format("%c", byteChar));
                    }
                    intent.putExtra(EXTRA_DATA, stringBuilder.toString());
                }
            }
        } else if (UUID_SERIAL_NUMBER.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties(); // Returns the properties of this characteristic: READ
            //Log.d(BLE_FOTA_TAG, "UUID serial number property value: " + flag);

            // Flag: Characteristic property - READ: 0x02
            if ((flag & 0x02) != 0) {
                final byte[] serialNumber = characteristic.getValue();

                if (serialNumber != null && serialNumber.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(serialNumber.length);

                    for (byte byteChar : serialNumber) {
                        stringBuilder.append(String.format("%c", byteChar));
                    }
                    intent.putExtra(EXTRA_DATA, stringBuilder.toString());
                }
            }
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();

            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);

                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%d", byteChar));
                }
                intent.putExtra(EXTRA_DATA, new String(data));
            }
        }
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // CPU wake lock is used to keep service running.
        // CPU wake lock is managed by each service(especially scan service) or the main application.
        /*sPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        sCpuWakeLock = sPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FIRMWARE_DOWNLOAD");
        sCpuWakeLock.acquire();*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        /*if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }*/
    }

    /**
     * Enables or disables notification on a given characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     * @see kr.co.sevencore.blefotalib.BflAttributes
     */
    public void setCharacteristicNotification (BluetoothGattCharacteristic characteristic, boolean enabled) {
        // bluetoothAdapter Initialization check for perform fundamental Bluetooth tasks.
        // bluetoothGatt Public API for the Bluetooth GATT profile. {@code BluetoothGatt}
        if (mBflBluetoothAdapter == null || mBflBluetoothGatt == null) {
            Log.w(BLE_FOTA_TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBflBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // Firmware upgrade notifications - Client characteristic configuration descriptor: Set notification flag.
        if ((UUID_FIRMWARE_DATA_CHECK.equals(characteristic.getUuid())) && (!mReadableDataChkFlag)) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(BflAttributes.CLIENT_CHARACTERISTIC_CONFIG)
            );
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBflBluetoothGatt.writeDescriptor(descriptor);
            //Log.d(BLE_FOTA_TAG, "Notification enabled - Firmware data check.");
        }
        if ((UUID_FIRMWARE_STATUS.equals(characteristic.getUuid())) && (!mReadableStatusFlag)) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(BflAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBflBluetoothGatt.writeDescriptor(descriptor);
            //Log.d(BLE_FOTA_TAG, "Notification enabled - Firmware status.");
        }
    }

    /*
     * Request a read on a given {@code BluetoothGattCharacteristic}.
     * The read result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt,
     * android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    private void readBflCharacteristic(BluetoothGattCharacteristic characteristic) {
        // bluetoothAdapter Initialization check for perform fundamental Bluetooth tasks.
        // bluetoothGatt Public API for the Bluetooth GATT profile. {@code BluetoothGatt}.
        if (mBflBluetoothAdapter == null || mBflBluetoothGatt == null) {
            Log.w(BLE_FOTA_TAG, "BluetoothAdapter is not initialized.");
            return;
        }
        mWritableNewVersionFlag = false;

        mBflBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     * The write result is reported through the
     * {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt,
     * android.bluetooth.BluetoothGattCharacteristic, int)}
     *
     * @param characteristic The characteristic to write from.
     */
    private void writeBflCharacteristic(BluetoothGattCharacteristic characteristic) {
        // bluetoothAdapter Initialization check for perform fundamental Bluetooth tasks.
        // bluetoothGatt Public API for the Bluetooth GATT profile. {@code BluetoothGatt}.
        if (mBflBluetoothAdapter == null || mBflBluetoothGatt == null) {
            Log.w(BLE_FOTA_TAG, "BluetoothAdapter is not initialized.");
            return;
        }

        mBflBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Check a negative parameter to prevent overflow.
     *
     * @param sequenceNumber is 1 byte value.
     * @return Positive sequence number.
     */
    public static int checkNegative(int sequenceNumber) {
        Log.e(BLE_FOTA_TAG, "Firmware data sequence number overflowed.");
        if (sequenceNumber < -1) {
            sequenceNumber += 256;
        }
        return sequenceNumber;
    }

    /**
     * Binary firmware data convert into byte code.
     * If firmware data split into several bytes because of maximum size of each connection event,
     * insert sequence index number in front of each bytes (actually at bytes[0] ~ bytes[N])
     *
     * @param binData is the binary file of firmware data.
     * @param length of the firmware data.
     * @return Btye data converted from firmware data.
     */
    public static byte[] getByteFromFile(File binData, long length) {
        byte[] bytes = new byte[(int)length];

        int offset = 0;
        int numRead = 0;

        try {
            InputStream firmwareInputStream = new FileInputStream(binData);
            try {
                while (offset < bytes.length && (numRead = firmwareInputStream.read(bytes, offset, bytes.length - offset)) >= 0) {
                    offset += numRead;
                }
                firmwareInputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int indexStart = 0;
        int indexEnd = PURE_EACH_CONN_DATA_SIZE;
        int indexSeqInfo = 0;
        int pureConnDataCnt = (int) (length / PURE_EACH_CONN_DATA_SIZE);
        final int pureLastConnSize = (int) (length % PURE_EACH_CONN_DATA_SIZE);
        final int pureLastConnSizePlusThree = (int) ((length % PURE_EACH_CONN_DATA_SIZE) +3);
        //Log.d(BLE_FOTA_TAG, "Pure last connection size: " + pureLastConnSize);
        int pureTotalConnDataCnt;
        int totalLength;
        int seqNum;
        int addedBytesIndex;

        if(pureLastConnSize != 0) {
            pureTotalConnDataCnt = (pureConnDataCnt + 1);
            totalLength = (int) (length + ((pureConnDataCnt + 1) * EACH_CONN_DATA_INFO));
        } else {
            pureTotalConnDataCnt = pureConnDataCnt;
            totalLength = (int) (length + (pureConnDataCnt * EACH_CONN_DATA_INFO));
        }

        byte[] addedBytes = new byte[totalLength];
        //Log.d(BLE_FOTA_TAG, "Total length: " + totalLength);

        if(pureLastConnSize != 0) {
            for (int i=0; i<pureTotalConnDataCnt; i++) {
                seqNum = i;
                addedBytesIndex = ((i + 1) * EACH_CONN_DATA_INFO);

                if (i != pureConnDataCnt) {
                    //Log.d(BLE_FOTA_TAG, "Insert index number: " + i +
                    //        ", Total count: " + pureTotalConnDataCnt + ", Total length: " + totalLength);

                    for (int j = indexStart; j < indexEnd; j++) {
                        addedBytes[j + addedBytesIndex] = bytes[j];
                    }

                    addedBytes[indexSeqInfo] |= (byte) (seqNum & 0xFF);
                    addedBytes[indexSeqInfo + 1] |= (byte) ((EACH_CONN_DATA_SIZE & 0xFF00) >> 8);
                    addedBytes[indexSeqInfo + 2] |= (byte) (EACH_CONN_DATA_SIZE & 0xFF);

                    indexStart += PURE_EACH_CONN_DATA_SIZE;
                    indexEnd += PURE_EACH_CONN_DATA_SIZE;
                    indexSeqInfo += EACH_CONN_DATA_SIZE;
                } else {
                    //Log.d(BLE_FOTA_TAG , "Insert index number else case:: " + i);
                    indexEnd += (pureLastConnSize - PURE_EACH_CONN_DATA_SIZE);

                    for (int j = indexStart; j < indexEnd; j++) {
                        addedBytes[j + addedBytesIndex] = bytes[j];
                    }

                    addedBytes[indexSeqInfo] |= (byte) (seqNum & 0xFF);
                    addedBytes[indexSeqInfo + 1] |= (byte) ((pureLastConnSizePlusThree & 0xFF00) >> 8);
                    addedBytes[indexSeqInfo + 2] |= (byte) (pureLastConnSizePlusThree & 0xFF);
                }
            }
        } else {
            for (int i = 0; i < pureTotalConnDataCnt; i++) {
                seqNum = i;
                addedBytesIndex = ((i + 1) * EACH_CONN_DATA_INFO);
                //Log.d(BLE_FOTA_TAG, "Insert index number: " + i);
                for (int j = indexStart; j < indexEnd; j++) {
                    addedBytes[j + addedBytesIndex] = bytes[j];
                }

                addedBytes[indexSeqInfo] |= (byte) (seqNum & 0xFF);
                addedBytes[indexSeqInfo+1] |= (byte) ((EACH_CONN_DATA_SIZE & 0xFF00) >> 8);
                addedBytes[indexSeqInfo+2] |= (byte) (EACH_CONN_DATA_SIZE & 0xFF);
                indexStart += PURE_EACH_CONN_DATA_SIZE;
                indexEnd += PURE_EACH_CONN_DATA_SIZE;
                indexSeqInfo += EACH_CONN_DATA_SIZE;
            }
        }
        //Log.d(BLE_FOTA_TAG, "Input source sequence numbering: " + Arrays.toString(addedBytes));
        return addedBytes;
    }

    /**
     * Split thread in bytes to send a firmware data.
     *
     * @see kr.co.sevencore.blefotalib.BflFwUploadService
     */
    class SplitBytesThread extends Thread {
        ArrayList<byte[]> byteList = new ArrayList<byte[]>();

        private byte[] byteData;
        private long length;
        private int sequenceNum;
        private BluetoothGattCharacteristic characteristic;

        public SplitBytesThread(byte[] byteData, long length, int sequenceNum, BluetoothGattCharacteristic characteristic) {
            byteList = new ArrayList<byte[]>();

            this.byteData = byteData;
            this.length = length;
            this.sequenceNum = sequenceNum;
            this.characteristic = characteristic;
        }

        public void run() {
            // connDataCnt + (lastConnData != 0 ? 1 : 0) = TOTAL CONNECTION EVENT
            // MAXIMUM CONNECTION EVENTS: connDataCnt <= 200 (100KB)

            //Log.d(BLE_FOTA_TAG, "Sequence number at split bytes thread: " + sequenceNum);
            int sendConnDataCnt = sequenceNum + 1;
            int leftLength = (int) (length - (sendConnDataCnt * PURE_EACH_CONN_DATA_SIZE));
            int totalLength;

            int pureConnDataCnt = leftLength / PURE_EACH_CONN_DATA_SIZE;
            final int pureLastConnSize = leftLength % PURE_EACH_CONN_DATA_SIZE;
            //Log.d(BLE_FOTA_TAG, "Send connection data count: " + sendConnDataCnt + ", Left length: " + leftLength);

            if (pureLastConnSize != 0) {
                sLeftConnCnt = (pureConnDataCnt + 1);
                totalLength = leftLength + ((pureConnDataCnt + 1) * EACH_CONN_DATA_INFO);
            } else {
                sLeftConnCnt = pureConnDataCnt;
                totalLength = leftLength + (pureConnDataCnt * EACH_CONN_DATA_INFO);
            }

            int connDataCnt = totalLength / EACH_CONN_DATA_SIZE;
            int lastConnSize = totalLength % EACH_CONN_DATA_SIZE;
            byte[] container;
            byte[] lastContainer;

            int startPt = 0;
            int index = 0;

            if (sendConnDataCnt != 0) {
                startPt = sendConnDataCnt * EACH_CONN_DATA_SIZE;
                //Log.d(BLE_FOTA_TAG, "Start point: " + startPt);
            }

            for (int i = 0; i < connDataCnt; i++) {
                container = new byte[EACH_CONN_DATA_SIZE];
                System.arraycopy(byteData, startPt, container, 0, EACH_CONN_DATA_SIZE);
                byteList.add(container);
                //Log.d(BLE_FOTA_TAG, "Container - array copy routine: " + container[0]);
                //Log.d(BLE_FOTA_TAG, "Insert data from \"byteData\" to \"bytesList\" - Source array start point: " + startPt);
                startPt += EACH_CONN_DATA_SIZE;
            }
            //Log.d(BLE_FOTA_TAG, "Write characteristic - Make array list: " + connDataCnt + " columns.");

            if (lastConnSize != 0) {
                lastContainer = new byte[lastConnSize];
                System.arraycopy(byteData, startPt, lastContainer, 0, lastConnSize);
                byteList.add(lastContainer);
                //Log.d(BLE_FOTA_TAG, "Last container - Array copy routine: " + lastContainer[0]);
                //Log.d(BLE_FOTA_TAG, "Insert data from \"byteData\" to \"byesList\" - Make last column: "
                //        + lastConnSize + " items.");
            }

            while (totalLength > 0 && sConnCheck) {
                if (mContinuousWriteFlag && (totalLength > EACH_CONN_DATA_SIZE)) {
                    //Log.d(BLE_FOTA_TAG, "Write characteristic when GATT success - Index: "
                    //        + index + ", Length size: " + totalLength);
                    mContinuousWriteFlag = false;

                    if (sConnCheck && mBflBluetoothGatt != null && byteList != null) {
                        container = byteList.get(index);
                        //Log.d(BLE_FOTA_TAG, "Container [0], [1], [2] value : "
                        // + container[0] + ", " + container[1] + ", " + container[2]);

                        characteristic.setValue(container);
                        try {
                            mBflBluetoothGatt.writeCharacteristic(characteristic);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        //Log.d(BLE_FOTA_TAG, "Write characteristic executed - Write size: " + container.length);
                    }

                    totalLength -= EACH_CONN_DATA_SIZE;
                    index++;
                    sLeftConnCnt--;

                } else if (mContinuousWriteFlag && totalLength < EACH_CONN_DATA_SIZE) {
                    //Log.d(BLE_FOTA_TAG, "Write characteristic when GATT success - Index: "
                    //        + index + ", Length size: " + totalLength);
                    mContinuousWriteFlag = false;

                    if (sConnCheck && mBflBluetoothGatt != null && byteList != null) {
                        lastContainer = byteList.get(index);
                        //Log.d(BLE_FOTA_TAG, "Last container [0], [1], [2] value: "
                        //        + lastContainer[0] + ", " + lastContainer[1] + ", " + lastContainer[2]);

                        characteristic.setValue(lastContainer);
                        try {
                            mBflBluetoothGatt.writeCharacteristic(characteristic);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        //Log.d(BLE_FOTA_TAG, "Write characteristic executed - Write size: " + lastContainer.length);
                    }

                    totalLength -= lastConnSize;
                    sLeftConnCnt--;
                }
            }
            super.run();
        }
    }

    /**
     * SHA-1 Checksum data.
     * Input file without sequence number.
     *
     * @param binData is firmware data.
     * @param length is firmware data size.
     * @return Byte type digest message.
     * @throws NoSuchAlgorithmException
     */
    private byte[] makeChecksum (File binData, long length) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        try {
            FileInputStream fileInputStream = new FileInputStream(binData);
            byte[] byteData = new byte[(int) length];

            int nRead = 0;

            try {
                while ((nRead = fileInputStream.read(byteData)) != -1) {
                    messageDigest.update(byteData, 0, nRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        byte[] byteMessageDigest = messageDigest.digest();
        //Log.d(BLE_FOTA_TAG, "Make checksum - Byte message digest size: " + byteMessageDigest.length);
        return byteMessageDigest;
    }
}
