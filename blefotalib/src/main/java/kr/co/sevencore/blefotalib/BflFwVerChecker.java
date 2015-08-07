package kr.co.sevencore.blefotalib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import kr.co.sevencore.blefotalib.BflCodeList.FirmwareVersionStatusCode;
import kr.co.sevencore.blefotalib.BflCodeList.UpgradeVersionTypeCode;

/**
 * BflFwVerChecker.java
 * BLE FOTA Library Firmware Version Checker.
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-07-17
 * @see kr.co.sevencore.blefotalib.BflFwDownloader
 * @see kr.co.sevencore.blefotalib.BflFwUploader
 * @see kr.co.sevencore.blefotalib.BflCodeList
 */
public class BflFwVerChecker extends BroadcastReceiver {
    private final String BLE_FOTA_TAG = BflFwVerChecker.class.getSimpleName();

    private static String sTargetVersion, sServerVersion;

    public final static String ACTION_TARGET_VERSION = "kr.co.sevencore.ble.fota.lib.util.ACTION_TARGET_VERSION";
    public final static String ACTION_SERVER_VERSION = "kr.co.sevencore.ble.fota.lib.util.ACTION_SERVER_VERSION";
    public final static String VERSION_INFO = "kr.co.sevencore.ble.fota.lib.util.VERSION_INFO";


    /**
     * Compare firmware version information of the target device and the server updated asynchronously.
     *
     * @param context is used to check running state of the firmware download service.
     * @param intent identify each action of the broadcast.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(BLE_FOTA_TAG, "Firmware version checker received version information.");

        if (action != null) {
            if (ACTION_TARGET_VERSION.equals(action)) {
                sTargetVersion = intent.getStringExtra(VERSION_INFO);
                Log.d(BLE_FOTA_TAG, "Receive firmware version information of the target device.");

            } else if (ACTION_SERVER_VERSION.equals(action)) {
                sServerVersion = intent.getStringExtra(VERSION_INFO);
                Log.d(BLE_FOTA_TAG, "Receive firmware version information from the server.");
            }
            checkUpdate(context, sTargetVersion, sServerVersion);
        }
    }

    /**
     * Check firmware update is needed.
     * TODO: Version information control by ascending order and so on.
     *
     * @param targetVersion is firmware version information of the target device.
     * @param serverVersion is firmware version information from the server.
     */
    private int checkUpdate(Context context, String targetVersion, String serverVersion) {
        if ((targetVersion != null) && (serverVersion != null)) {
            initVersionInfo();

            if (BflUtil.compareVersion(targetVersion, serverVersion) ==
                    UpgradeVersionTypeCode.FIRMWARE_UP_TO_DATE.getCode()) {
                Log.i(BLE_FOTA_TAG, "Firmware version of the target device is up to date.");
                return FirmwareVersionStatusCode.FIRMWARE_VERSION_UP_TO_DATE.getCode();

            } else {
                // Check firmware download service is running.
                if (BflUtil.isServiceRunning(context, BflFwDownloadService.class)) {
                    try {
                        // Unit test mode downloads firmware data all the ways.
                        // TODO: If you wants to minimize networks use, add routine for checking the firmware data existence.
                        Log.i(BLE_FOTA_TAG, "Firmware is going to be upgraded of downgraded.");
                        BflFwDownloader.sBflDownloadBinder.getFirmwareUrl();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return FirmwareVersionStatusCode.FIRMWARE_VERSION_UP_TO_DATE.getCode();
    }

    /**
     * Initialize version information values.
     */
    private void initVersionInfo() {
        sTargetVersion = sServerVersion = null;
    }
}
