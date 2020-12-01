package com.beztooth.UI.Activities;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.Util.Constants;
import com.beztooth.Util.Util;

import java.util.Locale;

public class ThermometerActivity extends BluetoothActivity
{
    private static final String TAG = "ThermometerActivity";
    private static final Constants.Device[] SUPPORTED_DEVICES = { Constants.LEO_SERVER_V2 };

    private static final Util.Color COLD_COLOR = new Util.Color(69, 128, 186);
    private static final Util.Color HOT_COLOR = new Util.Color(254, 0, 0);
    private static final Util.Color NOT_HUMID_COLOR = COLD_COLOR;
    private static final Util.Color HUMID_COLOR = new Util.Color(34, 139, 34);
    private static final float COLD_TEMPERATURE = 4.44444f;
    private static final float HOT_TEMPERATURE = 35.2222f;


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
                    // Start fresh scan.
                    m_ConnectionManager.Scan(true);
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
            m_ConnectionManager.Scan(true);
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
                m_Device.WriteCharacteristic(Constants.AddBaseUUID(Constants.SERVICE_CURRENT_TIME.UUID), Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID), Util.GetTimeInBytes(System.currentTimeMillis()));
                m_Device.ReadCharacteristic(Constants.AddBaseUUID(Constants.SERVICE_CURRENT_TIME.UUID), Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID));
                return true;
            case R.id.menu_live_time:
                item.setChecked(!item.isChecked());
                m_Device.SetCharacteristicNotification(Constants.AddBaseUUID(Constants.SERVICE_CURRENT_TIME.UUID), Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID), item.isChecked());
                return true;
            case R.id.menu_live_sensors:
                item.setChecked(!item.isChecked());
                m_Device.SetCharacteristicNotification(Constants.AddBaseUUID(Constants.SERVICE_ENVIRONMENTAL_SENSING.UUID), Constants.LEO_SERVER_V2_ALL_SENSOR_DATA.UUID, item.isChecked());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void UpdateCharacteristicData(String characteristic, byte[] data)
    {
        if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_TEMPERATURE.UUID)))
        {
            UpdateTemperature(Util.GetDataString(data, Constants.CharacteristicReadType.INTEGER));
        }
        else if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_HUMIDITY.UUID)))
        {
            UpdateHumidity(Util.GetDataString(data, Constants.CharacteristicReadType.INTEGER));
        }
        else if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_PRESSURE.UUID)))
        {
            UpdatePressure(Util.GetDataString(data, Constants.CharacteristicReadType.INTEGER));
        }
        else if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID)))
        {
            String[] dateData = Util.GetDataString(data, Constants.CharacteristicReadType.TIME).split(" ");
            TextView dateView = findViewById(R.id.date);
            dateView.setText(dateData[0]);
            TextView timeView = findViewById(R.id.time);
            timeView.setText(dateData[1]);
        }
        else if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.LEO_SERVER_V2_ALL_SENSOR_DATA.UUID)))
        {
            if (Util.IsBufferZero(data)) return;

            UpdateTemperature(Util.GetDataString(new byte[] { data[0] }, Constants.CharacteristicReadType.INTEGER));
            UpdateHumidity(Util.GetDataString(new byte[] { data[1] }, Constants.CharacteristicReadType.INTEGER));
            UpdatePressure(Util.GetDataString(new byte[] { data[2], data[3], data[4], data[5] }, Constants.CharacteristicReadType.INTEGER));
        }
    }

    private void UpdateTemperature(String temperature)
    {
        TextView temperatureView = findViewById(R.id.temperature);
        temperatureView.setText(String.format(Locale.getDefault(), "%s\u2103", temperature));

        float temp = Float.parseFloat(temperature);
        int color;
        if (temp < COLD_TEMPERATURE)
        {
            color = COLD_COLOR.GetColor();
        }
        else if (temp > HOT_TEMPERATURE)
        {
            color = HOT_COLOR.GetColor();
        }
        else
        {
            color = Util.Color.GetColorInSpectrum(COLD_COLOR, HOT_COLOR, 100.f * (temp - COLD_TEMPERATURE) / (HOT_TEMPERATURE-COLD_TEMPERATURE));
        }

        temperatureView.setTextColor(color);
    }

    private void UpdateHumidity(String humidity)
    {
        TextView humidityView = findViewById(R.id.humidity);
        humidityView.setText(String.format(Locale.getDefault(), "%s%%", humidity));

        float humid = Float.parseFloat(humidity);
        int color = Util.Color.GetColorInSpectrum(NOT_HUMID_COLOR, HUMID_COLOR, humid);

        humidityView.setTextColor(color);
    }

    private void UpdatePressure(String pressure)
    {
        String pressureText = String.format(Locale.getDefault(), "%d mmHg", Math.round(Util.ConvertPascalToMMHG(Float.parseFloat(pressure))));
        int unitIndex = pressureText.indexOf("mmHg");

        TextView pressureView = findViewById(R.id.pressure);
        SpannableString ss = new SpannableString(pressureText);
        ss.setSpan(new RelativeSizeSpan(.5f), unitIndex, pressureText.length(), 0);
        pressureView.setText(ss);
    }
}
