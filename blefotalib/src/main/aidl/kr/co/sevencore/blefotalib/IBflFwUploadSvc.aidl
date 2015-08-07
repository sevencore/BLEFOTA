// IBflFwUploadSvc.aidl
package kr.co.sevencore.blefotalib;

// Declare any non-default types here with import statements

/**
 * IBflFwUploadSvc.aidl
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-05-04
 * @see kr.co.sevencore.blefotalib.BflFwUploadService
 */
interface IBflFwUploadSvc {

    boolean initUploader();

    boolean connect(String address);

    void disconnect();

    void close();

    void updateGatt();

    boolean checkProperty(int serviceIdx, int characteristicIdx);

    void executeReadCharacteristic(int serviceIdx, int characteristicIdx);

    void executeWriteFirmwareNewVersion(int serviceIdx, int characteristicIdx, String firmwareVersion);

    void executeWriteFirmwareData(int serviceIdx, int characteristicIdx, String filePath, int sequenceNumber);

    void executeWriteSequenceNumber(int serviceIdx, int characteristicIdx, int index);

    void executeWriteChecksumData(int serviceIdx, int characteristicIdx, String filePath);

    void executeWriteFirmwareUpgradeType(int serviceIdx, int characteristicIdx, byte typeFlag);

    void executeWriteReset(int serviceIdx, int characteristicIdx, byte resetFlag);

    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
}
