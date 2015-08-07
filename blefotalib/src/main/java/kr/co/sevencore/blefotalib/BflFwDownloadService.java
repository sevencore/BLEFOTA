package kr.co.sevencore.blefotalib;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import kr.co.sevencore.blefotalib.BflCodeList.DownloadCode;

/**
 * BflFwDownloadService.java
 * BLE FOTA Library Firmware Download service.
 * This service downloads the firmware data from the firmware managing server.
 *
 * Debugging mode:
 * - sDeviceAddress = intent.getStringExtra("MAC_ADDRESS"); is disabled.
 * - Firmware saved directory is changed.
 *  -> Internal directory (disabled): dir = getDir(sProductName, Context.MODE_PRIVATE); dirPath = dir.getAbsolutePath() + "/" + fileName;
 *  -> External storage for debugging: dirPath = "/storage/sdcard0/Download/" + fileName;
 * 
 * !Notice: 
 *  - Insert your server URL. (FIRMWARE_MANAGING_SERVER_URL)
 *  - Insert server DB account ID. (FIRMWARE_MANAGING_SERVER_SERVER_ACCOUNT_ID)
 *  - Insert server DB passwd. (FIRMWARE_MANAGING_SERVER_ACCOUNT_PWD)
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-06-12
 * @see kr.co.sevencore.blefotalib.BflFwDownloader
 * @see kr.co.sevencore.blefotalib.IBflFwDownloadSvc
 */
public class BflFwDownloadService extends Service {
    private final static String BLE_FOTA_TAG = BflFwDownloadService.class.getSimpleName();

    private ServerConnection mServerConnection;
    private FirmwareDownload mFirmwareDownload;
    private int mSvcId;

    private static PowerManager sPowerManager;
    private static PowerManager.WakeLock sCpuWakeLock = null;

    private static String sDeviceAddress;// MAC address of the device.
    private static String sProductName;  // Product name of the device.
    private static String sFirmwareName; // Firmware file name.
    private static String sVersion;      // Firmware version information from the server.
    private static String sUrl;          // Firmware download URL.

    private final static String FIRMWARE_MANAGING_SERVER_URL = ""; //Insert your server URL.
    private final static String FIRMWARE_MANAGING_SERVER_DIR = "BLE_FIRMWARE/";
    private final static String FIRMWARE_MANAGING_SERVER_GETPRODUCTNAME = "getproductname.php";
    private final static String FIRMWARE_MANAGING_SERVER_GETURL = "geturl.php";
    private final static String FIRMWARE_MANAGING_SERVER_GETVERSION = "getversion.php";
    private final static String FIRMWARE_MANAGING_SERVER_SERVER_ACCOUNT_ID = "dbuser=account_ID"; // Insert server DB account ID.
    private final static String FIRMWARE_MANAGING_SERVER_ACCOUNT_PWD = "dbpasswd=pwd";      // Insert server DB passwd.
    private final static String FIRMWARE_MANAGING_SERVER_MAC_ID_IDENTIFIER = "?mac=";

    public final static String ACTION_ERROR_UNKNOWN_DEVICE =
            "kr.co.sevencore.ble.fota.lib.download.ACTION_ERROR_UNKNOWN_DEVICE";
    public final static String ACTION_ERROR_FIRMWARE_DATA_INTEGRITY =
            "kr.co.sevencore.ble.fota.lib.download.ACTION_ERROR_FIRMWARE_DATA_INTEGRITY";
    public final static String ACTION_PRODUCT_NAME =
            "kr.co.sevencore.ble.fota.lib.download.ACTION_PRODUCT_NAME";
    public final static String ACTION_FIRMWARE_VERSION =
            "kr.co.sevencore.ble.fota.lib.download.ACTION_FIRMWARE_VERSION";
    public final static String ACTION_FIRMWARE_URL =
            "kr.co.sevencore.ble.fota.lib.download.ACTION_FIRMWARE_URL";
    public final static String ACTION_FIRMWARE_DOWNLOAD_START =
            "kr.co.sevencore.ble.fota.lib.download.ACTION_FIRMWARE_DOWNLOAD_START";
    public final static String ACTION_FIRMWARE_DOWNLOADING =
            "kr.co.sevencore.ble.fota.lib.download.ACTION_FIRMWARE_DOWNLOADING";
    public final static String ACTION_FIRMWARE_DOWNLOAD_FINISH =
            "kr.co.sevencore.ble.fota.lib.download.ACTION_FINISHING_DOWNLOAD";
    public final static String EXTRA_DATA =
            "kr.co.sevencore.ble.fota.lib.download.ACTION_EXTRA_DATA";


    public BflFwDownloadService() {}

    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getStringExtra("MAC_ADDRESS") != null) {
            //sDeviceAddress = intent.getStringExtra("MAC_ADDRESS"); // Used for debugging.
            sDeviceAddress = "80:EA:CA:00:00:01";
        }

        // Get the product name of the device.
        getFirmwareInfo(FIRMWARE_MANAGING_SERVER_URL +
                FIRMWARE_MANAGING_SERVER_DIR + FIRMWARE_MANAGING_SERVER_GETPRODUCTNAME +
                FIRMWARE_MANAGING_SERVER_MAC_ID_IDENTIFIER + sDeviceAddress);

        return mBflFwDownloadBinder;
    }

    IBflFwDownloadSvc.Stub mBflFwDownloadBinder = new IBflFwDownloadSvc.Stub() {

        /**
         * Check initial state of network to download firmware data from the server.
         *
         * @return true, if mobile or WiFi network is available.
         * @throws RemoteException
         */
        @Override
        public boolean initDownloader() throws RemoteException {
            // TODO: Classifying a kind of available networks to response to charged network usage.
            // Default network check item: Mobile communication & WiFi.
            ConnectivityManager connectivityManager;
            NetworkInfo networkInfo;

            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo != null) {
                if (networkInfo.getType() == 0 || networkInfo.getType() == 1) {
                    // Type 0: Mobile communication network enabled.
                    // Type 1: WiFi network enabled.
                    return true;
                }
            }

            Log.e(BLE_FOTA_TAG, "Network is not able to use");
            return false;
        }

        /**
         * Get URL information of firmware managing server to update the firmware of the device.
         * TODO: This method have to be located in sandbox in the future because of the server related information.
         *
         * @throws RemoteException
         */
        @Override
        public void getFirmwareUrl() throws RemoteException {
            getFirmwareInfo(FIRMWARE_MANAGING_SERVER_URL +
                    FIRMWARE_MANAGING_SERVER_DIR + FIRMWARE_MANAGING_SERVER_GETURL +
                    FIRMWARE_MANAGING_SERVER_MAC_ID_IDENTIFIER + sDeviceAddress);
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
     * Make a firmware file name by firmware version information.
     *
     * @param firmwareName is a firmware file name.
     */
    private void makeFirmwareFileName(String firmwareName) {
        String sFirmwareFileExt = ".BIN"; // Firmware file name extension.
        sFirmwareName = firmwareName + sFirmwareFileExt;
    }

    /**
     * Get firmware information to be updated.
     *
     * @param serverUrl is the URL of firmware managing server to be able to request firmware information.
     */
    private void getFirmwareInfo(String serverUrl) {
        mServerConnection = new ServerConnection();
        mServerConnection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverUrl);
    }

    /**
     * Get firmware data.
     *
     * @param downloadUrl is the URL of firmware managing server to get firmware data.
     */
    private void getFirmware(String downloadUrl) {
        mFirmwareDownload = new FirmwareDownload();
        mFirmwareDownload.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, downloadUrl);
    }

    /**
     * Update firmware download progress information.
     *
     * @param action used for filtering.
     */
    private void broadcastUpdate(final String action) {
        final Intent fwDownloadIntent = new Intent(action);
        sendBroadcast(fwDownloadIntent);
    }

    /**
     * Update firmware data information.
     *
     * @param action used for filtering.
     * @param info is firmware data information.
     */
    private void broadcastUpdate(final String action, final String info) {
        final Intent fwDownloadIntent = new Intent(action);
        fwDownloadIntent.putExtra(EXTRA_DATA, info);
        sendBroadcast(fwDownloadIntent);
    }

    /**
     * Update firmware data information.
     *
     * @param action used for filtering.
     * @param info is firmware data information.
     */
    private void broadcastUpdate(final String action, final int info) {
        final Intent fwDownloadIntent = new Intent(action);
        fwDownloadIntent.putExtra(EXTRA_DATA, info);
        sendBroadcast(fwDownloadIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSvcId = startId;

        Log.d(BLE_FOTA_TAG, "onStartCommand service start ID: " + mSvcId);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // CPU wake lock is used to keep service running.
        // CPU wake lock is managed by each service(especially scan service) or the main application.
        sPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        sCpuWakeLock = sPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FIRMWARE_DOWNLOAD");
        sCpuWakeLock.acquire();
    }

    /**
     * FOTA is managed by firmware managing server.
     * Server connection is used to get information to manage a firmware version.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloader
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService.FirmwareDownload
     */
    private class ServerConnection extends AsyncTask<String, Integer, String> {

        private final static int SERVER_CONNECTION_TIMEOUT = 3000;

        private String mEncoding = "UTF-8";
        private int mEventType;

        private String mTagMac = "mac";
        private String mTagProduct = "product";
        private String mTagFwVer = "firmwareversion";
        private String mTagFwUrl = "firmwareurl";

        @Override
        protected String doInBackground(String... urls) {
            try {
                InputStream inputStream = null;

                URL url = new URL(urls[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                if (httpURLConnection != null) {
                    httpURLConnection.setConnectTimeout(SERVER_CONNECTION_TIMEOUT);
                    httpURLConnection.setUseCaches(false);

                    // Numeric status code HttpURLConnection.HTTP_OK: 200
                    if (httpURLConnection.getResponseCode() == 200) {
                        inputStream = url.openStream();
                    }
                    httpURLConnection.disconnect();
                }

                return parseData(inputStream);

            } catch (Exception e) {
                mServerConnection.cancel(true);
                Log.e(BLE_FOTA_TAG, "Firmware information download error.");

                if (sCpuWakeLock != null) {
                    sCpuWakeLock.release();
                    sCpuWakeLock = null;
                }

                e.printStackTrace();
            }
            return null;
        }

        /**
         * Get firmware information by parsing XML data.
         *
         * @param inputStream is XML data.
         * @return firmware information.
         */
        private String parseData(InputStream inputStream) {
            try {
                XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
                XmlPullParser xmlPullParser = xmlPullParserFactory.newPullParser();
                xmlPullParser.setInput(inputStream, mEncoding);
                mEventType = xmlPullParser.getEventType();

                while (mEventType != XmlPullParser.END_DOCUMENT) {
                    if (isCancelled()) {
                        Log.i(BLE_FOTA_TAG, "AsyncTask of ServerConnection is cancelled.");
                        //break; // "break" is replaced with "return null".
                        return null;
                    }

                    switch (mEventType) {
                        case XmlPullParser.START_TAG:
                            String startTag = xmlPullParser.getName();

                            if (startTag.equals(mTagMac)) {
                                mEventType = xmlPullParser.next();

                                if (sDeviceAddress.equals(xmlPullParser.getText())) {
                                    sDeviceAddress = xmlPullParser.getText();
                                    Log.i(BLE_FOTA_TAG, "Device MAC address: " + sDeviceAddress);

                                } else {
                                    return DownloadCode.SERVER_CONN_ERROR_UNKNOWN_DEVICE.getCode();
                                }

                            } else if (startTag.equals(mTagProduct)) {
                                mEventType = xmlPullParser.next();
                                sProductName = xmlPullParser.getText();

                                broadcastUpdate(ACTION_PRODUCT_NAME, sProductName);
                                Log.d(BLE_FOTA_TAG, "Product name from the server: " + sProductName);

                                return DownloadCode.SERVER_CONN_PROCESS_CHECKING_VERSION.getCode();

                            } else if (startTag.equals(mTagFwVer)) {
                                mEventType = xmlPullParser.next();
                                sVersion = xmlPullParser.getText();

                                broadcastUpdate(ACTION_FIRMWARE_VERSION, sVersion);
                                Log.d(BLE_FOTA_TAG, "Firmware version from the server: " + sVersion);

                                return DownloadCode.SERVER_CONN_PROCESS_GETTING_VERSION_NAME.getCode();

                            } else if (startTag.equals(mTagFwUrl)) {
                                mEventType = xmlPullParser.next();
                                sUrl = xmlPullParser.getText();

                                broadcastUpdate(ACTION_FIRMWARE_URL, sUrl);
                                Log.d(BLE_FOTA_TAG, "Firmware download URL: " + sUrl);

                                return DownloadCode.SERVER_CONN_PROCESS_GETTING_FIRMWARE.getCode();
                            }
                            break;

                        case XmlPullParser.END_TAG:
                            mEventType = xmlPullParser.next();
                            break;
                    }
                    mEventType = xmlPullParser.next();
                }
            } catch (Exception e) {
                if (sCpuWakeLock != null) {
                    sCpuWakeLock.release();
                    sCpuWakeLock = null;
                }
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {
            Log.d(BLE_FOTA_TAG,
                    "onPostExecute result code of ServerConnection AsyncTask: " + result);

            if (result != null) {
                if (DownloadCode.SERVER_CONN_ERROR_UNKNOWN_DEVICE.getCode().equals(result)) {
                    broadcastUpdate(ACTION_ERROR_UNKNOWN_DEVICE);

                } else if (DownloadCode.SERVER_CONN_PROCESS_CHECKING_VERSION.getCode().equals(result)) {
                    // Get the new version information of the device.
                    getFirmwareInfo(FIRMWARE_MANAGING_SERVER_URL +
                            FIRMWARE_MANAGING_SERVER_DIR + FIRMWARE_MANAGING_SERVER_GETVERSION +
                            FIRMWARE_MANAGING_SERVER_MAC_ID_IDENTIFIER + sDeviceAddress);

                } else if (DownloadCode.SERVER_CONN_PROCESS_GETTING_VERSION_NAME.getCode().equals(result)) {
                    makeFirmwareFileName(sVersion);

                } else if (DownloadCode.SERVER_CONN_PROCESS_GETTING_FIRMWARE.getCode().equals(result)) {
                    getFirmware(sUrl + sFirmwareName);
                }
            } else {
                stopSelf(mSvcId);
            }
        }
    }

    /**
     * Firmware download from the server.
     *
     * @see kr.co.sevencore.blefotalib.BflFwDownloader
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService
     * @see kr.co.sevencore.blefotalib.BflFwDownloadService.ServerConnection
     */
    private class FirmwareDownload extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();

                File file, dir;
                String dirPath, fileName;
                int fileSize;
                int count;
                int progressCount = 0;

                fileSize = httpURLConnection.getContentLength();
                broadcastUpdate(ACTION_FIRMWARE_DOWNLOAD_START, fileSize);
                Log.i(BLE_FOTA_TAG, "Firmware data size from the server: " + fileSize);

                fileName = new File(urls[0]).getName();
                Log.i(BLE_FOTA_TAG, "Firmware file name from the server: " + fileName);

                //dir = getDir(sProductName, Context.MODE_PRIVATE);
                //dirPath = dir.getAbsolutePath() + "/" + fileName;
                dirPath = "/storage/sdcard0/Download/" + fileName; // Used for debugging.
                Log.d(BLE_FOTA_TAG, "Firmware data stored at: " + dirPath);

                file = new File(dirPath);

                // If existing file is valid, finish firmware download task.
                // If existing file is invalid, delete file and resume firmware download process.
                if (file.exists()) {
                    if (!checkIntegrity(file, fileSize)) {
                        file.delete();
                        broadcastUpdate(ACTION_ERROR_FIRMWARE_DATA_INTEGRITY);
                        Log.d(BLE_FOTA_TAG, "New firmware file is invalid data.");
                    } else {
                        //file.delete(); // Used for debug. Unconditionally delete file & download firmware data.
                        httpURLConnection.disconnect();
                        return DownloadCode.FIRMWARE_DOWNLOAD_PROCESS_FINISHING_DOWNLOAD.getCode();
                    }
                }

                InputStream firmwareInputData = new BufferedInputStream(url.openStream());
                OutputStream firmwareOutputData = new FileOutputStream(file);

                if (fileSize > 0) {
                    byte data[] = new byte[fileSize];

                    try {
                        while ((count = firmwareInputData.read(data)) != -1) {
                            if (isCancelled()) {
                                Log.i(BLE_FOTA_TAG, "AsyncTask of FirmwareDownload is cancelled.");
                                break; // "break" is replaced with "return null".
                                //return null;
                            }
                            firmwareOutputData.write(data, 0, count);

                            // Used for notifying progress.
                            progressCount += count;
                            broadcastUpdate(ACTION_FIRMWARE_DOWNLOADING, progressCount);
                        }
                    } catch (Exception e) {
                        Log.e(BLE_FOTA_TAG, "Firmware download error.");
                        e.printStackTrace();
                    }
                }

                firmwareOutputData.flush();
                firmwareOutputData.close();
                firmwareInputData.close();
                httpURLConnection.disconnect();
                return DownloadCode.FIRMWARE_DOWNLOAD_PROCESS_FINISHING_DOWNLOAD.getCode();

            } catch (Exception e) {
                if (sCpuWakeLock != null) {
                    sCpuWakeLock.release();
                    sCpuWakeLock = null;
                }
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Check download file integrity.
         * TODO: checkIntegrity method is going to be replaced with checksum check method.
         *
         * @param file is firmware data.
         * @param fileSize is firmware data size information from the remote server.
         * @return true, if the firmware data integrity is valid.
         */
        private boolean checkIntegrity(File file, int fileSize) {
            return (fileSize == (int) file.length());
        }

        protected void onPostExecute(String result) {
            if (result != null && DownloadCode.FIRMWARE_DOWNLOAD_PROCESS_FINISHING_DOWNLOAD.getCode().equals(result)) {
                broadcastUpdate(ACTION_FIRMWARE_DOWNLOAD_FINISH);

                if (sCpuWakeLock != null) {
                    sCpuWakeLock.release();
                    sCpuWakeLock = null;
                }
            } else {
                stopSelf(mSvcId);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mServerConnection != null) {
            mServerConnection.cancel(true);
        }

        if (mFirmwareDownload != null) {
            mFirmwareDownload.cancel(true);
        }

        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }
}
