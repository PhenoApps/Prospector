//package consumerphysics.com.myscioapplication.activities;
package org.phenoapps.prospector.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.consumerphysics.android.scioconnection.services.SCiOBLeService;
import com.consumerphysics.android.scioconnection.utils.BLEUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//import consumerphysics.com.myscioapplication.R;
import org.phenoapps.prospector.R;
import consumerphysics.com.myscioapplication.config.Constants;

public final class DiscoverActivity extends Activity {

    private static final String TAG = DiscoverActivity.class.getSimpleName();

    private Map<String, String> devices;
    private DevicesAdapter devicesAdapter;
    private BluetoothAdapter bluetoothAdapter;

    // BlueTooth scan callback
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            String deviceName = device.getName();

            // Show only SCiO devices
            if (deviceName != null && deviceName.startsWith("SCiO")) {
                deviceName = deviceName.substring(4);
                String scio = devices.get(device.getAddress());

                if (scio == null) {
                    Log.d(TAG, "found scio device device: " + deviceName + ", " + device.getAddress());
                    addDevice(deviceName, device.getAddress());
                }
            }
        }
    };

    private final class Device {
        private String address;
        private String name;

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public Device(final String address, final String name) {
            this.name = name;
            this.address = address;
        }
    }

    public class DevicesAdapter extends ArrayAdapter<Device> {
        public DevicesAdapter(final Context context, final List<Device> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            final Device dev = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.simple_item, parent, false);
            }

            final TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
            tvName.setText(dev.getName());

            return convertView;
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kill the previous service
        stopService(new Intent(this, SCiOBLeService.class));

        setContentView(R.layout.activity_discover);
        setTitle("Select SCiO Device");

        devices = new LinkedHashMap<>();

        final List<Device> arrayOfDevices = new ArrayList<>();
        devicesAdapter = new DevicesAdapter(this, arrayOfDevices);

        final ListView lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(devicesAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Device dev = devicesAdapter.getItem(position);
                storeDevice(dev);
                Toast.makeText(getApplicationContext(), dev.getName() + " was selected", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        bluetoothAdapter = BLEUtils.getBluetoothAdapter(this);

        // Start Scan
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop Scan
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    private void storeDevice(final Device device) {
        final SharedPreferences pref = this.getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE);
        final SharedPreferences.Editor edit = pref.edit();

        edit.putString(Constants.SCIO_ADDRESS, device.getAddress());
        edit.putString(Constants.SCIO_NAME, device.getName());

        edit.commit();
    }

    private void addDevice(final String name, final String address) {
        devices.put(address, name);

        final Device dev = new Device(address, name);
        devicesAdapter.add(dev);
    }
}
