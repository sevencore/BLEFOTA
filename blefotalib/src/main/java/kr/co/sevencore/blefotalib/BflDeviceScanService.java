package kr.co.sevencore.blefotalib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * BflDeviceScanService.java
 * BLE FOTA Library Scanning BLE devices Service.
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-04-08
 * @see kr.co.sevencore.blefotalib.BflDeviceScanner
 * @see kr.co.sevencore.blefotalib.IBflDeviceScanSvc
 */
public class BflDeviceScanService extends Service {
    private final static String BLE_FOTA_TAG = BflDeviceScanService.class.getSimpleName();

    private BluetoothManager mBflBluetoothManager;
    private BluetoothAdapter mBflBluetoothAdapter;

    private Handler mBflScanHandler;
    private long mBflScanPeriod = 10000; // Stops scanning after 10 seconds.

    // Reserved for immortal background (device scanning) service.
    /*private static PowerManager sPowerManager;
    private static PowerManager.WakeLock sCpuWakeLock = null;*/

    public final static String ACTION_BFL_SCAN_STATE_DATA =
            "kr.co.sevencore.ble.fota.lib.ACTION_SCAN_STATE_DATA";
    public final static String ACTION_BFL_SCAN_DATA =
            "kr.co.sevencore.ble.fota.lib.ACTION_SCAN_DATA";
    public final static String EXTRA_BFL_SCAN_STATE_DATA =
            "kr.co.sevencore.ble.fota.lib.SCAN_STATE_DATA";
    public final static String EXTRA_BFL_MAC_ADDRESS_DATA =
            "kr.co.sevencore.ble.fota.lib.MAC_ADDRESS_DATA";
    public final static String EXTRA_BFL_DEVICE_NAME_DATA =
            "kr.co.sevencore.ble.fota.lib.DEVICE_NAME_DATA";
    public final static String EXTRA_BFL_RSSI_DATA =
            "kr.co.sevencore.ble.fota.lib.RSSI_DATA";


    public BflDeviceScanService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return mBflScanBinder;
    }

    IBflDeviceScanSvc.Stub mBflScanBinder = new IBflDeviceScanSvc.Stub() {

        /**
         * Initializes a reference to the local Bluetooth adapter.
         *
         * @return Return true if the initialization is successful.
         * @throws RemoteException
         */
        @Override
        public boolean initScan() throws RemoteException {
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

            scanBflLeDevice(true);
            return true;
        }

        /**
         * Customize scanning period by a user.
         *
         * @param period of scanning.
         * @throws RemoteException
         */
        @Override
        public void setScanPeriod(long period) throws RemoteException {
            mBflScanPeriod = period;
        }

        /**
         * Start scanning or stop scanning.
         *
         * @param enable or disable scanning state.
         * @throws RemoteException
         */
        @Override
        public void setScanState(boolean enable) throws RemoteException {
            scanBflLeDevice(enable);
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
     * Update scanning state.
     *
     * @param action used for filtering.
     * @param scanState is current scanning state.
     */
    private void broadcastUpdate(final String action, final boolean scanState) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_BFL_SCAN_STATE_DATA, scanState);
        sendBroadcast(intent);
    }

    /**
     * Update BLE device information.
     *
     * @param action used to filtering.
     * @param macAddr is BLE MAC address.
     * @param deviceName is BLE device name.
     * @param bleRssi is RSSI value, when the device is scanned.
     */
    private void broadcastUpdate(final String action, final String macAddr,
                                 final String deviceName, final int bleRssi) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_BFL_MAC_ADDRESS_DATA, macAddr);
        intent.putExtra(EXTRA_BFL_DEVICE_NAME_DATA, deviceName);
        intent.putExtra(EXTRA_BFL_RSSI_DATA, bleRssi);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO: START_REDELIVER_INTENT is used for immortal background (BLE device scanning) service.
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

        mBflScanHandler = new Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        /*if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }*/

        mBflBluetoothAdapter = null;
        mBflBluetoothManager = null;

        mBflScanHandler.removeMessages(0);
        //mBflScanHandler = null;
    }

    /**
     * Scan BLE device during scanning period.
     * Each broadcastUpdates are used to update scanning state.
     *
     * @param enable is the flag to start or stop scanning.
     * @see kr.co.sevencore.blefotalib.BflDeviceScanner
     */
    public void scanBflLeDevice(final boolean enable) {
        if (enable) {
            mBflScanHandler.postDelayed(
                    new Runnable() {
                @Override
                public void run() {
                    broadcastUpdate(ACTION_BFL_SCAN_STATE_DATA, false);
                    mBflBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, mBflScanPeriod);

            broadcastUpdate(ACTION_BFL_SCAN_STATE_DATA, true);

            mBflBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            broadcastUpdate(ACTION_BFL_SCAN_STATE_DATA, false);

            mBflBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * BLE scan callback.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            Log.d(BLE_FOTA_TAG, " Scanning information: " + bluetoothDevice.getAddress() + " " +  bluetoothDevice.getName() + "'s RSSI is " + i);
            broadcastUpdate(ACTION_BFL_SCAN_DATA, bluetoothDevice.getAddress(), bluetoothDevice.getName(), i);
        }
    };
}
