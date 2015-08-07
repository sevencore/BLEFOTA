package kr.co.sevencore.blefotalib;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * BflDeviceListAdapter.java
 * BLE FOTA Library Device List Adapter.
 *
 * 2015 SEVENCORE Co., Ltd.
 *
 * @author Jungwoo Park
 * @version 1.0.0
 * @since 2015-06-05
 * @see kr.co.sevencore.blefotalib.BflDeviceScanner
 */
public class BflDeviceListAdapter extends BaseAdapter {
    private final static String BLE_FOTA_TAG = BflDeviceListAdapter.class.getSimpleName();

    private ArrayList<BleDevice> mLeDevices;
    private LayoutInflater mInflater;


    public BflDeviceListAdapter(Context context) {
        this.mLeDevices = new ArrayList<BleDevice>();
        this.mInflater = LayoutInflater.from(context);
    }

    public class BleDevice {
        public String macAddress;
        public String bleDeviceName;
        public int bleRssi;
        // add unit

        public BleDevice(String macAddress, String bleDeviceName, int bleRssi) {
            this.macAddress = macAddress;
            this.bleDeviceName = bleDeviceName;
            this.bleRssi = bleRssi;
        }
    }

    /**
     * Add the device information in the list.
     *
     * @param macAddress is MAC address of the device.
     * @param deviceName is device name composed of string.
     */
    public void addDevice(String macAddress, String deviceName, int rssi) {
        boolean uniqueFlag = true;

        if (!mLeDevices.isEmpty()) {
            for(int i = 0; i < mLeDevices.size(); i++) {
                if (mLeDevices.get(i).macAddress.equals(macAddress)) {
                    uniqueFlag = false;
                    break;
                }
            }
        }

        if (uniqueFlag) {
            mLeDevices.add(new BleDevice(macAddress, deviceName, rssi));
        }
    }

    /**
     * Get the device information of the selected position.
     *
     * @param position is the selected item in the list.
     * @return the device information.
     */
    public BleDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    /**
     * Clear device list.
     */
    public void clear() {
        mLeDevices.clear();
    }

    /**
     * Get the total count of the data.
     *
     * @return The total count.
     */
    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    /**
     * Get data of the specific position.
     *
     * @param i is the selected position.
     * @return Data of the selected position.
     */
    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    /**
     * Get the item position of the selected item.
     *
     * @param i is the selected item.
     * @return is the position of the selected item.
     */
    @Override
    public long getItemId(int i) {
        return i;
    }

    /**
     * The adapter & view are used to show scanned device list on the list view.
     */
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflater.inflate(R.layout.bfl_listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        final String deviceName = mLeDevices.get(i).bleDeviceName;
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);
        viewHolder.deviceAddress.setText(mLeDevices.get(i).macAddress);
        viewHolder.deviceRssi.setText(Integer.toString(mLeDevices.get(i).bleRssi));

        return view;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
    }
}
