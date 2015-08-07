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

import kr.co.sevencore.blefotalib.BflCodeList.DownloadCode;

/**
 * BflFwDownloader.java
 * BLE FOTA Library Firmware Download.
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-06-12
 * @see kr.co.sevencore.blefotalib.BflFwDownloadService
 * @see kr.co.sevencore.blefotalib.IBflFwDownloadSvc
 */
public class BflFwDownloader {
    private final static String BLE_FOTA_TAG = BflFwDownloader.class.getSimpleName();

    private Context mContext;

    public static IBflFwDownloadSvc sBflDownloadBinder;         // Firmware data download service AIDL.
    private Intent mDownloadServiceIntent;
    private String mAddress;                                   // The target device MAC address.

    private boolean mDownloadProgressFlag = false;         // Prevent creation of download progress AsyncTask.

    private OnDownloadSvcInit mDownloadSvcInitCallback;     // Service initialization result of the firmware download callback.
    private OnFirmwareInfoListener mFirmwareInfoCallback;   // New firmware data information of the target device.
    private OnProgressCountListener mProgressCountCallback; // Firmware download progress information callback.


    public BflFwDownloader(Context context) {
        mContext = context;
    }

    /**
     * OnDownloadSvcInit interface is used to get the result of firmware download service initialization.
     */
    public interface OnDownloadSvcInit {
        /**
         * Initialization firmware download service by checking network available status of the smart device.
         *
         * @param initResult true: The smart device network is available.
         *                   false: The smart device network is not available.
         */
        void onDownloadSvcInit(boolean initResult);
    }

    /**
     * OnFirmwareInfoListener interface is used to get firmware download related data.
     * If a main application needs a firmware information of the server,
     * use onFirmwareInfoListener method.
     */
    public interface OnFirmwareInfoListener {
        /**
         * The firmware related information of the device which is going to be updated from the server.
         *
         * ============== Code Table ==============
         * Code | Information
         * ----------------------------------------
         * 7000 | Unknown device error
         * 7100 | Product name information
         * 7102 | Firmware version information
         * 7103 | Firmware download URL information
         * 7200 | Invalid firmware data error
         *
         * @param code is a identifier of each information.
         * @param info is a firmware and device information.
         * @see kr.co.sevencore.blefotalib.BflCodeList.DownloadCode
         */
        void onFirmwareInfoListener(String code, String info);
    }

    /**
     * OnProgressCountListener interface is used to get a firmware download status.
     * If a main application shows firmware download progress,
     * use onProgressCountListener.
     */
    public interface OnProgressCountListener {
        /**
         * This method is used to update firmware download progress.
         * When firmware download is started, send firmware data size information.
         * After firmware data size updated, send received data size.
         * Progress count starts at "0" when firmware download is started,
         * and it is set to "0" when firmware download is finished.
         * Each state sends below the information.
         *
         * ============================= State Table ==============================
         * Code | State                                        | Information
         * ------------------------------------------------------------------------
         * 7200 | FIRMWARE_DOWNLOAD_PROCESS_STARTING_DOWNLOAD  | Firmware data size
         * 7201 | FIRMWARE_DOWNLOAD_PROGRESSING                | Received data size
         * 7203 | FIRMWARE_DOWNLOAD_PROCESS_FINISHING_DOWNLOAD | 0 (Initialization)
         *
         * @param state is a progression of firmware data download.
         * @param size is a firmware data size or received data size.
         * @see kr.co.sevencore.blefotalib.BflCodeList.DownloadCode
         */
        void onProgressCountListener(String state, int size);
    }

    /**
     * Save a callback object to mDownloadSvcInitCallback.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloader.OnDownloadSvcInit
     */
    public void setOnDownloadSvcInit(OnDownloadSvcInit callback) {
        mDownloadSvcInitCallback = callback;
    }

    /**
     * Save a callback object to mFirmwareInfoCallback.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloader.OnFirmwareInfoListener
     */
    public void setOnFirmwareInfoListener(OnFirmwareInfoListener callback) {
        mFirmwareInfoCallback = callback;
    }

    /**
     * Save a callback object to mProgressCountCallback.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloader.OnProgressCountListener
     */
    public void setOnProgressCountListener(OnProgressCountListener callback) {
        mProgressCountCallback = callback;
    }

    /**
     * Create an object to implement a service connection interface.
     *
     * @see kr.co.sevencore.blefotalib.IBflFwDownloadSvc
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService
     */
    private final ServiceConnection mBflDownloadSvcConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            sBflDownloadBinder = IBflFwDownloadSvc.Stub.asInterface(service);
            boolean initResult = true;

            try {
                if (!sBflDownloadBinder.initDownloader()) {
                    initResult = false;
                    Log.e(BLE_FOTA_TAG, "Unable to initialize firmware downloader");
                }

                if (mDownloadSvcInitCallback != null) {
                    mDownloadSvcInitCallback.onDownloadSvcInit(initResult);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sBflDownloadBinder = null;
        }
    };

    /**
     * Broadcast receiver of firmware data download service.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService
     * @see kr.co.sevencore.blefotalib.BflUtil
     */
    private final BroadcastReceiver mBflDownloadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null) {
                if (BflFwDownloadService.ACTION_ERROR_UNKNOWN_DEVICE.equals(action)) {
                    if (mFirmwareInfoCallback != null) {
                        mFirmwareInfoCallback.onFirmwareInfoListener(
                                BflCodeList.DownloadCode.SERVER_CONN_ERROR_UNKNOWN_DEVICE.getCode(), null
                        );
                    }
                    Log.e(BLE_FOTA_TAG, "Bluetooth Smart device is not registered with the server.");

                } else if (BflFwDownloadService.ACTION_ERROR_FIRMWARE_DATA_INTEGRITY.equals(action)) {
                    if (mFirmwareInfoCallback != null) {
                        mFirmwareInfoCallback.onFirmwareInfoListener(
                                DownloadCode.FIRMWARE_DOWNLOAD_ERROR_INVALIDATE_DATA.getCode(), null
                        );
                    }
                    Log.e(BLE_FOTA_TAG, "Existing firmware data is invalid.");

                } else if (BflFwDownloadService.ACTION_PRODUCT_NAME.equals(action)) {
                    String productName = intent.getStringExtra(BflFwDownloadService.EXTRA_DATA);

                    if (mFirmwareInfoCallback != null) {
                        mFirmwareInfoCallback.onFirmwareInfoListener(
                                DownloadCode.SERVER_CONN_PROCESS_PRODUCT_INFO.getCode(),
                                productName
                        );
                    }
                    Log.d(BLE_FOTA_TAG, "Device name: " + productName);

                } else if (BflFwDownloadService.ACTION_FIRMWARE_VERSION.equals(action)) {
                    String firmwareNewVersion = intent.getStringExtra(BflFwDownloadService.EXTRA_DATA);

                    // Compare the device firmware version and the server firmware version.
                    Intent versionInfoIntent = new Intent(BflFwVerChecker.ACTION_SERVER_VERSION);
                    versionInfoIntent.putExtra(BflFwVerChecker.VERSION_INFO, firmwareNewVersion);
                    versionInfoIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    context.sendBroadcast(versionInfoIntent);

                    if (mFirmwareInfoCallback != null) {
                        mFirmwareInfoCallback.onFirmwareInfoListener(
                                DownloadCode.SERVER_CONN_PROCESS_GETTING_VERSION_NAME.getCode(),
                                firmwareNewVersion
                        );
                    }
                    Log.d(BLE_FOTA_TAG, "Firmware version from the server: " + firmwareNewVersion);

                } else if (BflFwDownloadService.ACTION_FIRMWARE_URL.equals(action)) {
                    String downloadUrl = intent.getStringExtra(BflFwDownloadService.EXTRA_DATA);

                    if (mFirmwareInfoCallback != null) {
                        mFirmwareInfoCallback.onFirmwareInfoListener(
                                DownloadCode.SERVER_CONN_PROCESS_URL_INFO.getCode(),
                                downloadUrl
                        );
                    }
                    Log.d(BLE_FOTA_TAG, "Firmware download URL: " + downloadUrl);

                } else if (BflFwDownloadService.ACTION_FIRMWARE_DOWNLOAD_START.equals(action)) {
                    int dataSize = intent.getIntExtra(BflFwDownloadService.EXTRA_DATA, 0);

                    if (!mDownloadProgressFlag) {
                        mDownloadProgressFlag = true;

                        if (mProgressCountCallback != null) {
                            mProgressCountCallback.onProgressCountListener(
                                    DownloadCode.FIRMWARE_DOWNLOAD_PROCESS_STARTING_DOWNLOAD.getCode(),
                                    dataSize
                            );
                        }
                        Log.d(BLE_FOTA_TAG, "Firmware download started. Firmware file size: " + dataSize);
                    }
                } else if (BflFwDownloadService.ACTION_FIRMWARE_DOWNLOADING.equals(action)) {
                    int transmissionSize = intent.getIntExtra(BflFwDownloadService.EXTRA_DATA, 0);

                    if (mDownloadProgressFlag) {
                        if (mProgressCountCallback != null) {
                            mProgressCountCallback.onProgressCountListener(
                                    DownloadCode.FIRMWARE_DOWNLOAD_PROGRESSING.getCode(),
                                    transmissionSize
                            );
                        }
                        Log.d(BLE_FOTA_TAG, "Firmware download transmission size: " + transmissionSize);
                    }

                } else if (BflFwDownloadService.ACTION_FIRMWARE_DOWNLOAD_FINISH.equals(action)) {
                    if (mDownloadProgressFlag) {
                        mDownloadProgressFlag = false;
                        // Initialize the reception data size.
                        if (mProgressCountCallback != null) {
                            mProgressCountCallback.onProgressCountListener(
                                    DownloadCode.FIRMWARE_DOWNLOAD_PROCESS_FINISHING_DOWNLOAD.getCode(), 0
                            );
                        }
                    }
                    // Finish firmware download service.
                    disconnectDownloadSvc();
                }
            } else {
                Log.e(BLE_FOTA_TAG, "Firmware downloader broadcast data is NULL.");
            }
        }
    };

    /**
     * BLE FOTA firmware download service connection.
     * Create service connection & download firmware from the sever.
     *
     * @param address is the device MAC address.
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService
     */
    public void connectDownloadSvc(String address) {
        mAddress = address;

        mDownloadServiceIntent = new Intent(mContext, BflFwDownloadService.class);
        mDownloadServiceIntent.putExtra("MAC_ADDRESS", mAddress);
        // BLE FOTA Library uses a daemon(local) background service using startService method to run independently
        // & a remote service using bindService method to communicate by AIDL.
        mContext.startService(mDownloadServiceIntent);
        mContext.bindService(mDownloadServiceIntent, mBflDownloadSvcConnection, mContext.BIND_ADJUST_WITH_ACTIVITY);
    }

    /**
     * BLE FOTA firmware download service disconnection.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService
     * @see kr.co.sevencore.blefotalib.BflUtil
     */
    public void disconnectDownloadSvc() {
        if (BflUtil.isServiceRunning(mContext, BflFwDownloadService.class)) {
            try {
                mContext.unbindService(mBflDownloadSvcConnection);
                mContext.stopService(mDownloadServiceIntent);
                sBflDownloadBinder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Register broadcast receiver.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService
     */
    public void registerBflDownloadReceiver() {
        mContext.registerReceiver(mBflDownloadBroadcastReceiver, makeDownloadProgressIntentFilter());
    }

    /**
     * Unregister broadcast receiver.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService
     */
    public void unregisterBflDownloadReceiver() {
        try {
            mContext.unregisterReceiver(mBflDownloadBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Intent filter for BflFwDownloadService.
     * ACTION_ERROR_UNKNOWN_DEVICE: The device is not registered on the server.
     * ACTION_ERROR_FIRMWARE_DATA_INTEGRITY: Firmware data integrity check from the server.
     * ACTION_PRODUCT_NAME: The device name from the server.
     * ACTION_FIRMWARE_VERSION: Firmware version information from the server.
     * ACTION_FIRMWARE_URL: Firmware download URL information.
     * ACTION_FIRMWARE_DOWNLOAD_START: Firmware download started. It is used to notify file size.
     * ACTION_FIRMWARE_DOWNLOADING: Firmware is downloading. It is used to update download progress.
     * ACTION_FIRMWARE_DOWNLOAD_FINISH: Firmware download finished.
     *
     * @return Intent filter.
     */
    private static IntentFilter makeDownloadProgressIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BflFwDownloadService.ACTION_ERROR_UNKNOWN_DEVICE);
        intentFilter.addAction(BflFwDownloadService.ACTION_ERROR_FIRMWARE_DATA_INTEGRITY);
        intentFilter.addAction(BflFwDownloadService.ACTION_PRODUCT_NAME);
        intentFilter.addAction(BflFwDownloadService.ACTION_FIRMWARE_VERSION);
        intentFilter.addAction(BflFwDownloadService.ACTION_FIRMWARE_URL);
        intentFilter.addAction(BflFwDownloadService.ACTION_FIRMWARE_DOWNLOAD_START);
        intentFilter.addAction(BflFwDownloadService.ACTION_FIRMWARE_DOWNLOADING);
        intentFilter.addAction(BflFwDownloadService.ACTION_FIRMWARE_DOWNLOAD_FINISH);
        intentFilter.addAction(BflFwDownloadService.EXTRA_DATA);
        return intentFilter;
    }
}
