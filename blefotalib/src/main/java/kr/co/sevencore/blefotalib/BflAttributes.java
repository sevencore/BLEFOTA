package kr.co.sevencore.blefotalib;

import java.util.HashMap;

/**
 * BflAttributes.java
 * BLE FOTA profile Attributes.
 * This class includes a GATT based attributes of "Firmware upgrade profile".
 *
 * Copyright 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-04-08
 */
public class BflAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    // PRIMARY SERVICE: GAP SERVICE
    // CHARACTERISTIC: GAP DEVICE NAME | GAP APPEARANCE | PERIPHERAL PREFERRED CONNECTION PARAMETERS
    public static String DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
    public static String APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb";
    public static String PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = "00002a04-0000-1000-8000-00805f9b34fb";

    // PRIMARY SERVICE: GATT SERVICE
    // CHARACTERISTIC: SERVICE CHANGED
    public static String SERVICE_CHANGED = "00001801-0000-1000-8000-00805f9b34fb";

    // PRIMARY SERVICE: FIRMWARE UPGRADE - INCLUDE: DEVICE INFORMATION
    // CHARACTERISTIC: FIRMWARE VERSION | FIRMWARE NEW VERSION | FIRMWARE DATA | SEQUENCE NUMBER | CHECKSUM DATA | FIRMWARE DATA CHECK | FIRMWARE UPGRADE TYPE | FIRMWARE STATUS | RESET
    public static String FIRMWARE_VERSION = "00002af0-0000-1000-8000-00805f9b34fb";         // READ
    public static String FIRMWARE_NEW_VERSION = "00002af1-0000-1000-8000-00805f9b34fb";     // READ | WRITE
    public static String FIRMWARE_DATA = "00002af2-0000-1000-8000-00805f9b34fb";            // WRITE
    public static String SEQUENCE_NUMBER = "00002af3-0000-1000-8000-00805f9b34fb";          // READ | WRITE
    public static String CHECKSUM_DATA = "00002af4-0000-1000-8000-00805f9b34fb";            // WRITE
    public static String FIRMWARE_DATA_CHECK = "00002af5-0000-1000-8000-00805f9b34fb";      // NOTIFY | READ
    public static String FIRMWARE_UPGRADE_TYPE = "00002af6-0000-1000-8000-00805f9b34fb";    // WRITE
    public static String FIRMWARE_STATUS = "00002af7-0000-1000-8000-00805f9b34fb";          // NOTIFY | READ
    public static String RESET ="00002af8-0000-1000-8000-00805f9b34fb";                     // WRITE

    // SECONDARY SERVICE: DEVICE INFORMATION
    // CHARACTERISTIC: MANUFACTURER NAME | MODEL NUMBER | SERIAL NUMBER
    public static String MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb";        // READ
    public static String MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb";             // READ
    public static String SERIAL_NUMBER = "00002a25-0000-1000-8000-00805f9b34fb";            // READ

    // FIRMWARE_DATA_CHECK & FIRMWARE_STATUS NOTIFICATION USING THIS CHARACTERISTIC
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";


    static {
        // FIRMWARE UPGRADE RELATED SERVICES
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Generic Access");
        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attributes");
        attributes.put("000018ff-0000-1000-8000-00805f9b34fb", "Firmware Upgrade Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");

        // GENERIC ACCESS SERVICE
        attributes.put(DEVICE_NAME,"Device Name");
        attributes.put(APPEARANCE,"Appearance");
        attributes.put(PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS,"Peripheral Pref. Conn. Params");

        // GENERIC ATTRIBUTE SERVICE
        attributes.put(SERVICE_CHANGED, "Service Changed");

        // FIRMWARE UPGRADE SERVICE
        attributes.put(FIRMWARE_VERSION, "Firmware Version");
        attributes.put(FIRMWARE_NEW_VERSION, "Firmware New Version");
        attributes.put(FIRMWARE_DATA, "Firmware Data");
        attributes.put(SEQUENCE_NUMBER, "Sequence Number");
        attributes.put(CHECKSUM_DATA, "Checksum Data");
        attributes.put(FIRMWARE_DATA_CHECK, "Firmware Data Check");
        attributes.put(FIRMWARE_UPGRADE_TYPE, "Firmware Upgrade Type");
        attributes.put(FIRMWARE_STATUS, "Firmware Status");
        attributes.put(RESET, "Reset");

        // DEVICE INFORMATION SERVICE
        attributes.put(MANUFACTURER_NAME, "Manufacturer Name");
        attributes.put(MODEL_NUMBER, "Model Number");
        attributes.put(SERIAL_NUMBER, "Serial Number");
    }

    /**
     * Search a content of attributes.
     *
     * @param uuid is unique value of each attributes.
     * @param defaultName is a default value for not reserved attribute.
     * @return a content of the specific attribute.
     */
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
