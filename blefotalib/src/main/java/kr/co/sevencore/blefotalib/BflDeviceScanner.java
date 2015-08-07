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

/**
 * BflDeviceScanner.java
 * BLE FOTA Library Scanning BLE devices.
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-04-08
 * @see kr.co.sevencore.blefotalib.BflDeviceScanService
 * @see kr.co.sevencore.blefotalib.IBflDeviceScanSvc
 * @see kr.co.sevencore.blefotalib.BflDeviceListAdapter
 */
public class BflDeviceScanner {
    private final static String BLE_FOTA_TAG = BflDeviceScanner.class.getSimpleName();

    public IBflDeviceScanSvc mBflScanBinder;    // BLE device scan service AIDL.
    private Intent mScanServiceIntent;
    private boolean mScanning;                 // The flag used to check scanning or not.
    private boolean mScanPeriodFlag = false; // The flag used to customize scanning period.
    private long mScanPeriod;                  // Scanning period defined by user.

    private Context mContext;

    public static BflDeviceListAdapter sLeDeviceListAdapter; // The adapter saving information of scanned BLE devices.

    private OnScanningSvcInit mScanningSvcInitCallback;   // Service initialization result of scanning callback.
    private OnScannedInfo mScannedCallback;                // BLE scan information callback.
    private OnScanningState mScanningCallback;             // BLE scan state callback.


    public BflDeviceScanner(Context context) {
        mContext = context;
    }

    /**
     * OnScanningSvcInit interface is used to get result of BLE device scanning service initialization.
     */
    public interface OnScanningSvcInit {
        /**
         * Initialization using BLE of the smart device.
         *
         * @param initResult true: All of BLE related feature is normal state.
         *                   false: Unable to initialize BLE related feature.
         */
        void onScanningSvcInit(boolean initResult);
    }

    /**
     * OnScannedInfo interface is used to get BLE device information from scanned devices.
     */
    public interface OnScannedInfo {
        /**
         * Scanned BLE device information.
         *
         * @param address is MAC address.
         * @param name is device name.
         * @param rssi is RSSI(Received Signal Strength Indicator) value.
         */
        void onDeviceInfo(String address, String name, int rssi);
    }

    /**
     * OnScanningState interface is used to check scanning state.
     */
    public interface OnScanningState {
        /**
         * Notify current scanning state.
         *
         * @param state true: scanning, false: not scanning.
         */
        void onScanningState(boolean state);
    }

    /**
     * Save a callback object to mScanningSvcInitCallback.
     *
     * @param callback is OnScanningSvcInit.
     * @see kr.co.sevencore.blefotalib.BflDeviceScanner.OnScanningSvcInit
     */
    public void setOnScanningSvcInit(OnScanningSvcInit callback) {
        mScanningSvcInitCallback = callback;
    }

    /**
     * Save a callback object to mScannedCallback.
     *
     * @param callback is OnScannedInfo.
     * @see kr.co.sevencore.blefotalib.BflDeviceScanner.OnScannedInfo
     */
    public void setOnDeviceInfo(OnScannedInfo callback) {
        mScannedCallback = callback;
    }

    /**
     * Save a callback object to mScanningCallback.
     *
     * @param callback is OnScanningState.
     * @see kr.co.sevencore.blefotalib.BflDeviceScanner.OnScanningState
     */
    public void setOnScanningState(OnScanningState callback) {
        mScanningCallback = callback;
    }

    /**
     * Create an object of device list adapter.
     *
     * @param context is gettable from caller application.
     * @see kr.co.sevencore.blefotalib.BflDeviceListAdapter
     */
    public void createAdapter(Context context) {
        sLeDeviceListAdapter = new BflDeviceListAdapter(context.getApplicationContext());
    }

    /**
     * Create an object to implement a service connection interface.
     *
     * @see kr.co.sevencore.blefotalib.IBflDeviceScanSvc
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     */
    private final ServiceConnection mBflScanSvcConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBflScanBinder = IBflDeviceScanSvc.Stub.asInterface(service);
            boolean initResult = true;

            Log.d(BLE_FOTA_TAG, "Unit test mode BLE scanning started.");
            // When a user app customizes the scan period, setScanPeriod method is used.
            if(mScanPeriodFlag) {
                setScanPeriod(mScanPeriod);
                mScanPeriodFlag = false;
            }

            try {
                if (!mBflScanBinder.initScan()) {
                    initResult = false;
                    Log.e(BLE_FOTA_TAG, "Unable to initialize Bluetooth");
                }

                if (mScanningSvcInitCallback != null) {
                    mScanningSvcInitCallback.onScanningSvcInit(initResult);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBflScanBinder = null;
        }
    };

    /**
     * Broadcast receiver of BLE device scan service.
     *
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     */
    private final BroadcastReceiver mBflScanBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            String macAddress = intent.getStringExtra(BflDeviceScanService.EXTRA_BFL_MAC_ADDRESS_DATA);
            String deviceName = intent.getStringExtra(BflDeviceScanService.EXTRA_BFL_DEVICE_NAME_DATA);
            int rssi = intent.getIntExtra(BflDeviceScanService.EXTRA_BFL_RSSI_DATA, 0);

            if (action != null) {
                if (BflDeviceScanService.ACTION_BFL_SCAN_DATA.equals(action)) {
                    if (sLeDeviceListAdapter != null) {
                        try {
                            sLeDeviceListAdapter.addDevice(macAddress, deviceName, rssi);
                            sLeDeviceListAdapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // If a user app doesn't use above list adapter,
                    // use below BLE device information callback to use on a user app.
                    if (mScannedCallback != null) {
                        mScannedCallback.onDeviceInfo(macAddress, deviceName, rssi);
                    }
                } else if (BflDeviceScanService.ACTION_BFL_SCAN_STATE_DATA.equals(action)) {
                    if (mScanning ^ intent.getBooleanExtra(BflDeviceScanService.EXTRA_BFL_SCAN_STATE_DATA, false)) {
                        mScanning = intent.getBooleanExtra(BflDeviceScanService.EXTRA_BFL_SCAN_STATE_DATA, false);

                        if (mScanningCallback != null) {
                            mScanningCallback.onScanningState(mScanning);
                        }
                    }
                }
            } else {
                Log.e(BLE_FOTA_TAG, "Device scanner broadcast data is NULL.");
            }
        }
    };

    /**
     * BLE FOTA scan service connection.
     * Create service connection & start scanning.
     *
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     */
    public void connectScanSvc() {
        mScanServiceIntent = new Intent(mContext, BflDeviceScanService.class);
        // BLE FOTA Library uses daemon(local) background service using startService method to run independently
        // & remote service using bindService method to communicate by AIDL.
        mContext.startService(mScanServiceIntent);
        mContext.bindService(mScanServiceIntent, mBflScanSvcConnection, mContext.BIND_ADJUST_WITH_ACTIVITY);
    }

    /**
     * BLE FOTA Service disconnection.
     *
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     * @see kr.co.sevencore.blefotalib.BflUtil
     */
    public void disconnectScanSvc() {
        if (BflUtil.isServiceRunning(mContext, BflDeviceScanService.class)) {
            try {
                mContext.unbindService(mBflScanSvcConnection);
                mContext.stopService(mScanServiceIntent);
                mBflScanBinder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Customize scanning period.
     * The period is have to be set before execution of scanInit method.
     *
     * @param period is scanning time.
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     */
    public void setScanPeriod(long period) {
        mScanPeriodFlag = true;

        if (mBflScanBinder != null) {
            try {
                mBflScanBinder.setScanPeriod(period);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Start scanning BLE devices.
     * Default scan period is 10 seconds.
     *
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     */
    public void startScanning() {
        if (sLeDeviceListAdapter != null) {
            sLeDeviceListAdapter.clear();
        }

        if (mBflScanBinder != null) {
            disconnectScanSvc();
        }

        if(!BflUtil.isServiceRunning(mContext, BflDeviceScanService.class)) {
            connectScanSvc();
            setScanningState(false);
        }
    }

    /**
     * Start scanning BLE devices with scanning period customized.
     *
     * @param period is scanning period.
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     */
    public void startScanning(long period) {
        if (sLeDeviceListAdapter != null) {
            sLeDeviceListAdapter.clear();
        }

        if (mBflScanBinder != null) {
            disconnectScanSvc();
        }

        if(!BflUtil.isServiceRunning(mContext, BflDeviceScanService.class)) {
            mScanPeriodFlag = true;
            mScanPeriod = period;
            connectScanSvc();
            setScanPeriod(mScanPeriod);
            setScanningState(false);
        }
    }

    /**
     * Stop scanning BLE devices.
     *
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     */
    public void stopScanning() {
        if (mBflScanBinder != null) {
            try {
                mBflScanBinder.setScanState(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            disconnectScanSvc();
        }
        setScanningState(true);
    }

    /**
     * Register broadcast receiver.
     *
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     */
    public void registerBflScanReceiver() {
        mContext.registerReceiver(mBflScanBroadcastReceiver, makeStateUpdateIntentFilter());
    }

    /**
     * Unregister broadcast receiver.
     *
     * @see kr.co.sevencore.blefotalib.BflDeviceScanService
     */
    public void unregisterBflScanReceiver() {
        try {
            mContext.unregisterReceiver(mBflScanBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Update scanning state.
     *
     * @param state is BLE device scan state enabled or disabled.
     */
    public void setScanningState(boolean state) {
        mScanning = state;
    }

    /**
     * Intent filter for BflDeviceScanService.
     * ACTION_SCAN_DATA: BLE device information.
     * ACTION_SCAN_STATE_DATA: Current scan status.
     *
     * @return intentFilter including ACTIONS.
     */
    private static IntentFilter makeStateUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BflDeviceScanService.ACTION_BFL_SCAN_DATA);
        intentFilter.addAction(BflDeviceScanService.ACTION_BFL_SCAN_STATE_DATA);
        return intentFilter;
    }
}
