package com.beztooth.UI;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.Util.Constants;

public class ThermometerActivity extends BluetoothActivity
{
    private static final String TAG = "ThermometerActivity";
    private static final Constants.Device[] SUPPORTED_DEVICES = { Constants.LEO_SERVER_V2 };

    private ConnectionManager.Device m_Device;
    private boolean m_IsConnected;

    private ProgressBar m_LoadProgress;

    private BroadcastReceiver m_BroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(ConnectionManager.ON_SCAN_STOPPED))
            {
                if (m_Device == null)
                {
                    // Start fresh scan again.
                    m_ConnectionManager.StopScan();
                    m_ConnectionManager.Scan();
                }
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_SCANNED))
            {
                if (m_Device != null) return;

                String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                for (Constants.Device supportedDevice : SUPPORTED_DEVICES)
                {
                    if (supportedDevice.MAC.equalsIgnoreCase(address))
                    {
                         ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                         if (device != null)
                         {
                             m_Device = device;
                             m_Device.SetReadCharacteristicsWhenDiscovered(true);
                             m_Device.Connect();
                             break;
                         }
                    }
                }
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_CONNECTED))
            {
                if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS))) return;

                m_IsConnected = true;
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_DISCONNECTED))
            {
                if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS))) return;

                finish();
            }
            else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_READ))
            {
                if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS))) return;

                m_LoadProgress.setVisibility(View.GONE);

                String characteristic = intent.getStringExtra(ConnectionManager.CHARACTERISTIC);
                byte[] data = intent.getByteArrayExtra(ConnectionManager.DATA);
                UpdateCharacteristicData(characteristic, data);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermometer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        m_LoadProgress = findViewById(R.id.scanProgress);
        m_LoadProgress.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_BroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void OnConnectionManagerConnected()
    {
        m_Device = null;
        m_IsConnected = false;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_SCAN_STOPPED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_SCANNED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_DISCONNECTED);
        intentFilter.addAction(ConnectionManager.ON_CHARACTERISTIC_READ);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);

        for (Constants.Device supportedDevice : SUPPORTED_DEVICES)
        {
            ConnectionManager.Device device = m_ConnectionManager.GetDevice(supportedDevice.MAC);
            if (device != null)
            {
                m_Device = device;

                device.SetReadCharacteristicsWhenDiscovered(true);
                if (m_Device.IsConnected())
                {
                    m_Device.DiscoverServices();
                    m_IsConnected = true;
                }
                else
                {
                    m_Device.Connect();
                }

                break;
            }
        }

        if (!m_IsConnected)
        {
            // Start fresh scan.
            m_ConnectionManager.StopScan();
            m_ConnectionManager.Scan();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thermometer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (m_Device == null || !m_IsConnected) return true;

        switch (item.getItemId()) {
            case R.id.menu_sync_time:
                BluetoothGattCharacteristic c = m_Device.GetCharacteristic(Constants.AddBaseUUID(Constants.SERVICE_CURRENT_TIME.UUID), Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID));
                c.setValue(ConnectionManager.GetTimeInBytes(System.currentTimeMillis()));
                m_Device.WriteCharacteristic(c);
                m_Device.ReadCharacteristic(c);
                return true;
            case R.id.menu_live_time:
                item.setChecked(!item.isChecked());
                m_Device.SetCharacteristicNotification(m_Device.GetCharacteristic(Constants.AddBaseUUID(Constants.SERVICE_CURRENT_TIME.UUID), Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID)), item.isChecked());
                return true;
            case R.id.menu_live_sensors:
                item.setChecked(!item.isChecked());
                m_Device.SetCharacteristicNotification(m_Device.GetCharacteristic(Constants.AddBaseUUID(Constants.SERVICE_ENVIRONMENTAL_SENSING.UUID), Constants.LEO_SERVER_V2_ALL_SENSOR_DATA.UUID), item.isChecked());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void UpdateCharacteristicData(String characteristic, byte[] data)
    {
        if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_TEMPERATURE.UUID)))
        {
            TextView temperatureView = findViewById(R.id.temperature);
            temperatureView.setText(ConnectionManager.GetDataString(data, Constants.CharacteristicReadType.INTEGER) + "\u2103");
        }
        else if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_HUMIDITY.UUID)))
        {
            TextView humidityView = findViewById(R.id.humidity);
            humidityView.setText(ConnectionManager.GetDataString(data, Constants.CharacteristicReadType.INTEGER) + "%");
        }
        else if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_PRESSURE.UUID)))
        {
            TextView pressureView = findViewById(R.id.pressure);
            pressureView.setText(ConnectionManager.GetDataString(data, Constants.CharacteristicReadType.INTEGER) + " Pa");
        }
        else if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID)))
        {
            String[] dateData = ConnectionManager.GetDataString(data, Constants.CharacteristicReadType.TIME).split(" ");
            TextView dateView = findViewById(R.id.date);
            dateView.setText(dateData[0]);
            TextView timeView = findViewById(R.id.time);
            timeView.setText(dateData[1]);
        }
        else if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.LEO_SERVER_V2_ALL_SENSOR_DATA.UUID)))
        {
            // TODO: move to util
            boolean isZero = true;
            for (byte b : data)
            {
                if (b != 0)
                {
                    isZero = false;
                    break;
                }
            }
            if (isZero) return;

            TextView temperatureView = findViewById(R.id.temperature);
            temperatureView.setText(ConnectionManager.GetDataString(new byte[] { data[0] }, Constants.CharacteristicReadType.INTEGER) + " \u2103");
            TextView humidityView = findViewById(R.id.humidity);
            humidityView.setText(ConnectionManager.GetDataString(new byte[] { data[1] }, Constants.CharacteristicReadType.INTEGER) + " %");
            TextView pressureView = findViewById(R.id.pressure);
            pressureView.setText(ConnectionManager.GetDataString(new byte[] { data[2], data[3], data[4], data[5] }, Constants.CharacteristicReadType.INTEGER) + " Pa");
        }
    }
}
