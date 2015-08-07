package kr.co.sevencore.blefotalib;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

/**
 * BflUtil.java
 * BLE FOTA Library utility methods.
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-04-08
 */
public class BflUtil {
    private final static String BLE_FOTA_TAG = BflUtil.class.getSimpleName();


    /**
     * Extract the firmware version information from the file name.
     *
     * @param fileName is the full name including extension.
     * @return file name without the extension.
     */
    public static String extractVersionName(String fileName) {
        String[] versionNameArr = fileName.split("\\.");

        return versionNameArr[0];
    }

    /**
     * Check the format of the firmware file name: 00-00-00
     *
     * @param fileVerName is the firmware version formed into file name.
     * @return true, if the firmware name conforms to the rules.
     */
    public static boolean checkFileName(String fileVerName) {
        String[] nameElementArr = fileVerName.split("\\-");
        int versionSize = 8;
        int verInfoElementCnt = 3;
        int verInfoElementSize = 2;
        int maxVerNum = 99;

        if((fileVerName.length() == versionSize) && (nameElementArr.length == verInfoElementCnt)) {
            for(int i = 0; i < verInfoElementCnt; i++) {
                for(int j = 0; j <= maxVerNum; j++) {
                    String number = Integer.toString(j);

                    if ((j < 10) && (nameElementArr[i].equals("0" + number)) && (i == verInfoElementSize)) {
                        return true;
                    } else if (nameElementArr[i].equals(number) && (i == verInfoElementSize)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check the maximum file size of the firmware data.
     *
     * @param filePath is the pull path of the firmware data saved.
     * @return true, if the firmware file size is small than 100 KB.
     */
    public static boolean checkFileSize(String filePath) {
        final int maxDataSize = 102400; // MAXIMUM FIRMWARE DATA SIZE: 100 KB
        int length = filePath.length();

        if(length < maxDataSize) {
            return true;
        }
        return false;
    }

    /**
     * Compare version information.
     * When version information format is AA-BB-CC,
     * AA: Major update
     * BB: Minor update
     * CC: Minor update
     *
     * @param firmwareVersion is current firmware version of the target device.
     * @param firmwareNewVersion is new firmware version from the server.
     * @return the information of update type.
     */
    public static int compareVersion(String firmwareVersion, String firmwareNewVersion) {
        String[] currentVersionHierarchy = firmwareVersion.split("\\-");
        String[] newVersionHierarchy = firmwareNewVersion.split("\\-");

        if (!currentVersionHierarchy[0].equals(newVersionHierarchy[0])) {
            Log.d(BLE_FOTA_TAG, "current version hierarchy: " + currentVersionHierarchy[0] + ", new version: " + newVersionHierarchy[0]);
            if (Integer.parseInt(currentVersionHierarchy[0]) < Integer.parseInt(newVersionHierarchy[0])) {
                return BflCodeList.UpgradeVersionTypeCode.FIRMWARE_MAJOR_UPGRADE.getCode();

            } else {
                return BflCodeList.UpgradeVersionTypeCode.FIRMWARE_MAJOR_DOWNGRADE.getCode();
            }
        } else if (!currentVersionHierarchy[1].equals(newVersionHierarchy[1])) {
            if (Integer.parseInt(currentVersionHierarchy[1]) < Integer.parseInt(newVersionHierarchy[1])) {
                return BflCodeList.UpgradeVersionTypeCode.FIRMWARE_MINOR_UPGRADE.getCode();

            } else {
                return BflCodeList.UpgradeVersionTypeCode.FIRMWARE_MINOR_DOWNGRADE.getCode();
            }
        } else if (!currentVersionHierarchy[2].equals(newVersionHierarchy[2])) {
            if (Integer.parseInt(currentVersionHierarchy[2]) < Integer.parseInt(newVersionHierarchy[2])) {
                return BflCodeList.UpgradeVersionTypeCode.FIRMWARE_MINOR_UPGRADE.getCode();

            } else {
                return BflCodeList.UpgradeVersionTypeCode.FIRMWARE_MINOR_DOWNGRADE.getCode();
            }
        }
        return BflCodeList.UpgradeVersionTypeCode.FIRMWARE_UP_TO_DATE.getCode();
    }

    /**
     * Check service running status.
     *
     * @param context is gettable from caller application.
     * @param className is the name of service class.
     * @return If service is running, return true.
     */
    public static boolean isServiceRunning(Context context, Class className) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for(ActivityManager.RunningServiceInfo runningServiceInfo :
                activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if(className.getName().
                    equals(runningServiceInfo.service.getClassName())) {
                Log.d(BLE_FOTA_TAG, "BLE FOTA service is running.");
                return true;
            }
        }
        Log.d(BLE_FOTA_TAG, "BLE FOTA service is NOT running.");
        return false;
    }
}
