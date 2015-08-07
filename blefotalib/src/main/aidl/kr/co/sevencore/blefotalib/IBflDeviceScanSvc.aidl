// IBflDeviceScanSvc.aidl
package kr.co.sevencore.blefotalib;

// Declare any non-default types here with import statements

/**
 * IBflDeviceScanSvc.aidl
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-05-04
 * @see kr.co.sevencore.blefotalib.BflDeviceScanService
 */
interface IBflDeviceScanSvc {

    boolean initScan();

    void setScanPeriod(long period);

    void setScanState(boolean enable);

    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
}
