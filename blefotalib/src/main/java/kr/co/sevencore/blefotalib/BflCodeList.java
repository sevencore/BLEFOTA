package kr.co.sevencore.blefotalib;

/**
 * BflCodeList.java
 * BLE FOTA Library code list.
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-07-15
 * @see kr.co.sevencore.blefotalib.BflFwDownloader
 * @see kr.co.sevencore.blefotalib.BflFwUploader
 */
public class BflCodeList {
    public final static String LIST_NAME = "NAME";  // Services or characteristics name.
    public final static String LIST_UUID = "UUID";  // Services or characteristics UUID.


    /**
     * Firmware upload each state message.
     */
    public enum UploadCode {
        DEVICE_FIRMWARE_CURRENT_VERSION("3000"),
        DEVICE_FIRMWARE_NEW_VERSION("3010"),
        DEVICE_FIRMWARE_NEW_VERSION_EMPTINESS("3011"),
        DEVICE_FIRMWARE_NEW_VERSION_EXISTENCE("3012"),
        DEVICE_FIRMWARE_SEQUENCE_NUMBER("3020"),
        DEVICE_FIRMWARE_AVAILABLE_SEQUENCE_NUMBER("3021"),
        DEVICE_FIRMWARE_DATA_LEFT_COUNT("3022"),
        DEVICE_FIRMWARE_WRITABLE_SEQUENCE_NUMBER("3023"),
        DEVICE_FIRMWARE_DATA_STATUS("3030"),
        DEVICE_FIRMWARE_DATA_STATUS_NORMAL("3031"),
        DEVICE_FIRMWARE_DATA_STATUS_VALID("3032"),
        DEVICE_FIRMWARE_DATA_STATUS_INVALID("3033"),
        DEVICE_FIRMWARE_UPGRADE_TYPE("3040"),
        DEVICE_FIRMWARE_UPGRADE_TYPE_NORMAL("3041"),
        DEVICE_FIRMWARE_UPGRADE_TYPE_FORCED("3042"),
        DEVICE_FIRMWARE_STATUS("3050"),
        DEVICE_FIRMWARE_STATUS_NORMAL("3051"),
        DEVICE_FIRMWARE_STATUS_SUCCESSFUL_FINISH("3052"),
        DEVICE_FIRMWARE_STATUS_ABNORMAL_FINISH("3053"),
        DEVICE_FIRMWARE_RESET("3060"),
        DEVICE_INFORMATION_MANUFACTURER_NAME("3100"),
        DEVICE_INFORMATION_MODEL_NUMBER("3101"),
        DEVICE_INFORMATION_SERIAL_NUMBER("3102"),
        DEVICE_EXTRA_DATA("3200");

        private final String code;

        UploadCode(String code) {
            this.code = code;
        }

        /**
         * Get code value.
         *
         * @return String type code number.
         */
        public String getCode() {
            return code;
        }
    }

    /**
     * FOTA profile services index.
     */
    public enum ServiceIdxCode {
        SERVICE_FIRMWARE_UPGRADE(2), // FOTA service index in mBflGattCharacteristics.
        SERVICE_DEVICE_INFO(3);       // Device info service index in mBflGattCharacteristics.

        private final int code;

        ServiceIdxCode(int code) {
            this.code = code;
        }

        /**
         * Get code value.
         *
         * @return Integer type index value.
         */
        public int getCode() {
            return code;
        }
    }

    /**
     * FOTA characteristic index.
     */
    public enum CharacteristicFotaIdxCode {
        CHARACTERISTIC_FIRMWARE_VERSION(0),     // Firmware version characteristic in firmware upgrade service.
        CHARACTERISTIC_FIRMWARE_NEW_VERSION(1),// Firmware new version characteristic in firmware upgrade service.
        CHARACTERISTIC_FIRMWARE_DATA(2),        // Firmware data characteristic in firmware upgrade service.
        CHARACTERISTIC_SEQUENCE_NUMBER(3),      // Sequence number characteristic in firmware upgrade service.
        CHARACTERISTIC_CHECKSUM_DATA(4),        // Checksum characteristic in firmware upgrade service.
        CHARACTERISTIC_FIRMWARE_DATA_CHECK(5), // Firmware data check characteristic in firmware upgrade service.
        CHARACTERISTIC_FIRMWARE_UPGRADE_TYPE(6),// Firmware upgrade type characteristic in firmware upgrade service.
        CHARACTERISTIC_FIRMWARE_STATUS(7),      // Firmware status characteristic in firmware upgrade service.
        CHARACTERISTIC_RESET(8);                  // Reset characteristic in firmware upgrade service.

        private final int code;

        CharacteristicFotaIdxCode(int code) {
            this.code = code;
        }

        /**
         * Get code value.
         *
         * @return Integer type index value.
         */
        public int getCode() {
            return code;
        }
    }

    /**
     * Device information characteristic index.
     */
    public enum CharacteristicDeviceInfoIdxCode {
        CHARACTERISTIC_MANUFACTURER_NAME(0),    // Manufacturer name characteristic in device information service.
        CHARACTERISTIC_MODEL_NUMBER(1),          // Model number characteristic in device information service.
        CHARACTERISTIC_SERIAL_NUMBER(2);         // Serial number characteristic in device information service.

        private final int code;

        CharacteristicDeviceInfoIdxCode(int code) {
            this.code = code;
        }

        /**
         * Get code value.
         *
         * @return Integer type index value.
         */
        public int getCode() {
            return code;
        }
    }

    /**
     * Firmware version status classification.
     */
    public enum FirmwareVersionStatusCode {
        FIRMWARE_VERSION_UP_TO_DATE(0),
        FIRMWARE_VERSION_DOWNGRADE(1),
        FIRMWARE_VERSION_UPGRADE(2);

        private final int code;

        FirmwareVersionStatusCode(int code) {
            this.code = code;
        }

        /**
         * Get code value.
         *
         * @return Integer code value.
         */
        public int getCode() {
            return code;
        }
    }

    /**
     * Firmware upgrade type classification.
     */
    public enum UpgradeVersionTypeCode {
        FIRMWARE_MAJOR_UPGRADE(0),
        FIRMWARE_MINOR_UPGRADE(1),
        FIRMWARE_MAJOR_DOWNGRADE(2),
        FIRMWARE_MINOR_DOWNGRADE(3),
        FIRMWARE_UP_TO_DATE(4);

        private final int code;

        UpgradeVersionTypeCode(int code) {
            this.code = code;
        }

        /**
         * Get code value.
         *
         * @return Integer code value.
         */
        public int getCode() {
            return code;
        }
    }

    /**
     * Firmware data integrity status code.
     */
    public enum FirmwareDataCheckCode {
        FIRMWARE_DATA_CHECK_NORMAL("0"),
        FIRMWARE_DATA_CHECK_VALIDATE("1"),
        FIRMWARE_DATA_CHECK_INVALIDATE("2");

        private final String code;

        FirmwareDataCheckCode(String code) {
            this.code = code;
        }

        /**
         * Get code value.
         *
         * @return String type code value.
         */
        public String getCode() {
            return code;
        }
    }

    /**
     * Firmware status code.
     */
    public enum FirmwareStatusCode {
        FIRMWARE_STATUS_NORMAL("0"),
        FIRMWARE_STATUS_SUCCESSFUL("1"),
        FIRMWARE_STATUS_ABNORMAL_FINISH("2");

        private final String code;

        FirmwareStatusCode(String code) {
            this.code = code;
        }

        /**
         * Get code value.
         *
         * @return String type code value.
         */
        public String getCode() {
            return code;
        }
    }

    /**
     * Firmware download each state message.
     */
    public enum DownloadCode {
        SERVER_CONN_ERROR_UNKNOWN_DEVICE("7000"),
        SERVER_CONN_PROCESS_PRODUCT_INFO("7100"),
        SERVER_CONN_PROCESS_CHECKING_VERSION("7101"),
        SERVER_CONN_PROCESS_GETTING_VERSION_NAME("7102"),
        SERVER_CONN_PROCESS_URL_INFO("7103"),
        SERVER_CONN_PROCESS_GETTING_FIRMWARE("7104"),
        FIRMWARE_DOWNLOAD_PROCESS_STARTING_DOWNLOAD("7200"),
        FIRMWARE_DOWNLOAD_PROGRESSING("7201"),
        FIRMWARE_DOWNLOAD_ERROR_INVALIDATE_DATA("7202"),
        FIRMWARE_DOWNLOAD_PROCESS_FINISHING_DOWNLOAD("7203");

        private final String code;

        DownloadCode(String code) {
            this.code = code;
        }

        /**
         * Get code value.
         *
         * @return String type code number.
         */
        public String getCode() {
            return code;
        }
    }

    /**
     * BLE FOTA Library error code.
     * Not in use now. Reserved.
     */
    public enum BflErrorCode {
        NOT_ERROR(1000),
        BLE_NOT_SUPPORT(1010),
        BT_NOT_SUPPORT(1011),
        BT_DISABLED(1020),
        BT_MANAGER_INIT_FAIL(1021),
        BT_ADAPTER_OBTAINMENT_FAIL(1022),
        WIFI_DISABLED(1030),
        MOBILE_NOT_AVAILABLE(1040),
        ERROR_UNKNOWN_DEVICE(1050),
        ERROR_INVALIDATE_DATA(1060);

        private final int errCode;

        BflErrorCode(int errCode) {
            this.errCode = errCode;
        }

        /**
         * Get error code value.
         *
         * @return Error code number.
         */
        public int getErrCode() {
            return errCode;
        }
    }
}
