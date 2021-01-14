package com.beztooth.UI.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
import com.beztooth.Util.Logger;
import com.beztooth.Util.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class KimchiActivity extends BluetoothActivity
{
    private static final String TAG = "KimchiActivity";
    private static final Constants.Device SUPPORTED_DEVICE = Constants.KIMCHI_V2;
    private static final int CREATE_FILE = 1;

    private ConnectionManager.Device m_Device;
    private boolean m_IsConnected;
    private Date m_StartDate;
    private boolean m_IsDownloading;
    private LinkedList<byte[]> m_DownloadedData;
    private Uri m_URI;

    private ProgressBar m_LoadProgress;

    private class KimchiSensorData
    {
        float Temperature;
        int Humidity;
        int Pressure;
        int Gas;
        float InnerTemperature;
    }

    private final BroadcastReceiver m_BroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action == null) return;

            switch (action)
            {
                case ConnectionManager.ON_SCAN_STOPPED:
                    if (m_Device == null)
                    {
                        // Start fresh scan.
                        m_ConnectionManager.Scan(true);
                    }
                    break;
                case ConnectionManager.ON_DEVICE_SCANNED:
                    String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                    if (SUPPORTED_DEVICE.MAC.equalsIgnoreCase(address))
                    {
                        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                        if (device != null)
                        {
                            m_Device = device;
                            m_Device.SetReadCharacteristicsWhenDiscovered(false);
                            m_Device.Connect();
                            break;
                        }
                    }
                    break;
                case ConnectionManager.ON_DEVICE_CONNECTED:
                    if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS)))
                        return;

                    m_IsConnected = true;
                    break;
                case ConnectionManager.ON_SERVICES_DISCOVERED:
                    if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS)))
                        return;

                    m_LoadProgress.setVisibility(View.GONE);

                    m_Device.ReadCharacteristic(Constants.KIMCHI_V1_SENSOR_SERVICE.UUID, Constants.KIMCHI_V1_SENSOR_DATA.UUID);
                    m_Device.ReadCharacteristic(Constants.AddBaseUUID(Constants.SERVICE_CURRENT_TIME.UUID), Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID));
                    break;
                case ConnectionManager.ON_DEVICE_DISCONNECTED:
                    if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS)))
                        return;

                    m_Device.Close();

                    if (m_IsDownloading)
                    {
                        WriteDataToFile();
                    }

                    finish();
                    break;
                case ConnectionManager.ON_CHARACTERISTIC_READ:
                    if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS)))
                        return;

                    String characteristic = intent.getStringExtra(ConnectionManager.CHARACTERISTIC);
                    byte[] data = intent.getByteArrayExtra(ConnectionManager.DATA);
                    HandleCharacteristicRead(characteristic, data);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kimchi);

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
        if (m_Device != null) m_Device.Disconnect();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_BroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void OnConnectionManagerConnected()
    {
        m_Device = null;
        m_IsConnected = false;
        m_IsDownloading = false;

        m_DownloadedData = new LinkedList<>();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_SCAN_STOPPED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_SCANNED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_SERVICES_DISCOVERED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_DISCONNECTED);
        intentFilter.addAction(ConnectionManager.ON_CHARACTERISTIC_READ);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);

        ConnectionManager.Device device = m_ConnectionManager.GetDevice(SUPPORTED_DEVICE.MAC);
        if (device != null)
        {
            m_Device = device;

            // Refresh device if already connected
            if (m_Device.IsConnected())
            {
                device.SetReadCharacteristicsWhenDiscovered(false);
                m_Device.DiscoverServices();
                m_IsConnected = true;
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
        inflater.inflate(R.menu.kimchi_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);

        if (m_Device == null || !m_IsConnected) return true;

        int itemId = item.getItemId();
        if (itemId == R.id.menu_start)
        {
            m_Device.WriteCharacteristic(Constants.AddBaseUUID(Constants.SERVICE_CURRENT_TIME.UUID), Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID), Util.GetTimeInBytes(System.currentTimeMillis()));
            m_Device.ReadCharacteristic(Constants.AddBaseUUID(Constants.SERVICE_CURRENT_TIME.UUID), Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID));
            return true;
        }
        else if (itemId == R.id.menu_stop)
        {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "kimchi_data.csv");
            startActivityForResult(intent, CREATE_FILE);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) return;

        switch(requestCode)
        {
            case CREATE_FILE:
                if (data != null)
                {
                    Uri uri = data.getData();
                    if (uri != null)
                    {
                        m_IsDownloading = true;
                        m_DownloadedData.clear();
                        m_URI = uri;

                        m_Device.SetCharacteristicIndication(Constants.KIMCHI_V1_SENSOR_SERVICE.UUID, Constants.KIMCHI_V1_ALL_SENSOR_DATA.UUID, true);
                    }
                }
                break;
        }
    }

    private void HandleCharacteristicRead(String characteristic, byte[] data)
    {
        if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID)))
        {
            String[] dateData = Util.GetDataString(data, Constants.CharacteristicReadType.TIME).split(" ");
            TextView dateView = findViewById(R.id.date);
            dateView.setText(dateData[0]);
            TextView timeView = findViewById(R.id.time);
            timeView.setText(dateData[1]);

            m_StartDate = Util.GetDataFromData(data);
        }
        else if (characteristic.equalsIgnoreCase(Constants.KIMCHI_V1_SENSOR_DATA.UUID))
        {
            KimchiSensorData ksd = GetKimchiSensorData(data);

            TextView temperatureView = findViewById(R.id.temperature);
            temperatureView.setText(String.format(Locale.getDefault(), "%.1f\u2103", ksd.Temperature));
            TextView humidityView = findViewById(R.id.humidity);
            humidityView.setText(String.format(Locale.getDefault(), "%d%%", ksd.Humidity));
            TextView pressureView = findViewById(R.id.pressure);
            pressureView.setText(String.format(Locale.getDefault(), "%d mmHg", ksd.Pressure));
            TextView gasView = findViewById(R.id.gas);
            gasView.setText(String.format(Locale.getDefault(), "%d???", ksd.Gas));
            TextView innerTemperature = findViewById(R.id.inner_temperature);
            innerTemperature.setText(String.format(Locale.getDefault(), "%.1f\u2103 or %.1f\u2103", ksd.InnerTemperature, ksd.InnerTemperature + 15));
        }
        else if (characteristic.equalsIgnoreCase(Constants.KIMCHI_V1_ALL_SENSOR_DATA.UUID))
        {
            Logger.Debug(TAG, "KIMCHI_V1_ALL_SENSOR_DATA: " + Util.GetDataString(data, Constants.CharacteristicReadType.CUSTOM));
            if (m_IsDownloading)
            {
                m_DownloadedData.add(data);
            }
        }
    }

    private void WriteDataToFile()
    {
        try
        {
            OutputStream os = getApplicationContext().getContentResolver().openOutputStream(m_URI);
            if (os == null) return;

            Calendar c = Calendar.getInstance();
            c.setTime(m_StartDate);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);

            os.write("time,temperature,humidity,pressure,gas,inner temperature\n".getBytes());
            for (byte[] chunk : m_DownloadedData)
            {
                // Read 8 bytes at a time
                for (int i = 0; i < chunk.length; i += 8)
                {
                    //c.add(Calendar.MINUTE, 1);
                    c.add(Calendar.SECOND, 5);
                    String time = dateFormat.format(c.getTime());

                    KimchiSensorData ksd = GetKimchiSensorData(new byte[] {
                            chunk[i],
                            chunk[i + 1],
                            chunk[i + 2],
                            chunk[i + 3],
                            chunk[i + 4],
                            chunk[i + 5],
                            chunk[i + 6],
                            chunk[i + 7]
                    });

                    String csvLine = String.format(Locale.getDefault(), "%s,%.1f,%d,%d,%d,%.1f\n", time, ksd.Temperature, ksd.Humidity, ksd.Pressure, ksd.Gas, ksd.InnerTemperature);
                    os.write(csvLine.getBytes());
                }
            }

            os.close();
        }
        catch (IOException e)
        {
            Logger.Debug(TAG, e.getStackTrace().toString());
        }
    }

    private KimchiSensorData GetKimchiSensorData(byte[] data)
    {
        KimchiSensorData result = new KimchiSensorData();

        result.Temperature = Float.parseFloat(Util.GetDataString(new byte[] { data[0] }, Constants.CharacteristicReadType.INTEGER));
        result.Humidity = Integer.parseInt(Util.GetDataString(new byte[] { data[1] }, Constants.CharacteristicReadType.INTEGER));
        result.Pressure = Integer.parseInt(Util.GetDataString(new byte[] { data[2], data[3] }, Constants.CharacteristicReadType.INTEGER));
        result.Gas = Integer.parseInt(Util.GetDataString(new byte[] { data[4], data[5], data[6] }, Constants.CharacteristicReadType.INTEGER));
        result.InnerTemperature = Integer.parseInt(Util.GetDataString(new byte[] { data[7] }, Constants.CharacteristicReadType.INTEGER)) / 10.f;

        return result;
    }
}
