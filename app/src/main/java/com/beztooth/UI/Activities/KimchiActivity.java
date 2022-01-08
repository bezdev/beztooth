package com.beztooth.UI.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.UI.Activities.BluetoothActivity;
import com.beztooth.Util.Constants;
import com.beztooth.Util.Logger;
import com.beztooth.Util.Util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class KimchiActivity extends BluetoothActivity
{
    private static final String TAG = "KimchiActivity";
    private static final String DATA_FILE = "Kimchi.data";
    private static final String PREFERENCES_IS_DEVICE_RECORDING_KEY = "com.beztooth.KimchiActivity.IsDeviceRecording";
    private static final Constants.Device SUPPORTED_DEVICE = Constants.KIMCHI_V2;
    private static final int CREATE_FILE = 1;

    private ConnectionManager.Device m_Device;
    private boolean m_IsConnected;
    private boolean m_IsDownloading;
    private boolean m_IsDeviceRecording;
    private Date m_StartDate;
    private LinkedList<byte[]> m_DownloadedData;
    private Uri m_DownloadFile;

    private ProgressBar m_LoadProgress;

    private static class KimchiSensorData
    {
        String Time;
        float Temperature;
        int Humidity;
        int Pressure;
        int Gas;
        float InnerTemperature;

        public KimchiSensorData() {}

        public KimchiSensorData(String time, float temperature, int humidity, int pressure, int gas, float innerTemperature)
        {
            Time = time;
            Temperature = temperature;
            Humidity = humidity;
            Pressure = pressure;
            Gas = gas;
            InnerTemperature = innerTemperature;
        }

        public KimchiSensorData(String string)
        {
            String[] split = string.split(",");
            if (split.length != 6)
            {
                Logger.Error(TAG, "GetKimchiSensorDataFromString");
                return;
            }

            Time = split[0];
            Temperature = Float.parseFloat(split[1]);
            Humidity = Integer.parseInt(split[2]);
            Pressure = Integer.parseInt(split[3]);
            Gas = Integer.parseInt(split[4]);
            InnerTemperature = Float.parseFloat(split[5]);
        }

        public boolean IsZero()
        {
            return Temperature == 0 && Humidity == 0 && Pressure == 0 && Gas == 00 && InnerTemperature == 0;
        }

        public String ToString()
        {
            return String.format(Locale.getDefault(), "%s,%.1f,%d,%d,%d,%.1f\n", Time, Temperature, Humidity, Pressure, Gas, InnerTemperature);
        }

        public static KimchiSensorData CreateFromBytes(byte[] data)
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

    @Override
    protected void OnBroadcastEvent(Intent intent)
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
            String address = intent.getStringExtra(ConnectionManager.ADDRESS);
            if (SUPPORTED_DEVICE.MAC.equalsIgnoreCase(address))
            {
                ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                if (device != null)
                {
                    m_Device = device;
                    m_Device.SetReadCharacteristicsWhenDiscovered(false);
                    m_Device.Connect();
                }
            }
        }
        else if (action.equals(ConnectionManager.ON_DEVICE_CONNECTED))
        {
            if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS)))
                return;

            m_IsConnected = true;
        }
        else if(action.equals(ConnectionManager.ON_SERVICES_DISCOVERED))
        {
            if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS)))
                return;

            m_LoadProgress.setVisibility(View.GONE);

            m_Device.ReadCharacteristic(Constants.KIMCHI_V1_SENSOR_SERVICE.UUID, Constants.KIMCHI_V1_SENSOR_DATA.UUID);
            m_Device.ReadCharacteristic(Constants.AddBaseUUID(Constants.SERVICE_CURRENT_TIME.UUID), Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID));
        }
        else if (action.equals(ConnectionManager.ON_DEVICE_DISCONNECTED))
        {
            if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS)))
                return;

            m_Device.Close();

            if (m_IsDownloading)
            {
                DownloadDataToFile();
            }

            finish();
        }
        else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_READ))
        {
            if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS)))
                return;

            String characteristic = intent.getStringExtra(ConnectionManager.CHARACTERISTIC);
            byte[] data = intent.getByteArrayExtra(ConnectionManager.DATA);
            HandleCharacteristicRead(characteristic, data);
        }
        else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_WRITE))
        {
            if (m_Device == null || !m_Device.GetAddress().equals(intent.getStringExtra(ConnectionManager.ADDRESS)))
                return;

            String characteristic = intent.getStringExtra(ConnectionManager.CHARACTERISTIC);
            if (characteristic.equalsIgnoreCase(Constants.AddBaseUUID(Constants.CHARACTERISTIC_CURRENT_TIME.UUID)))
            {
                SetMenuItemState(true);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_kimchi);

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        m_LoadProgress = findViewById(R.id.scanProgress);
        m_LoadProgress.setVisibility(View.VISIBLE);

        SharedPreferences p = getPreferences(Context.MODE_PRIVATE);
        m_IsDeviceRecording = p.getBoolean(PREFERENCES_IS_DEVICE_RECORDING_KEY, false);
        SetMenuItemState(m_IsDeviceRecording);

        ShowExistingData();
    }

    @Override
    protected void onDestroy()
    {
        if (m_Device != null) m_Device.Disconnect();

        super.onDestroy();
    }

    @Override
    protected void OnConnectionManagerConnected()
    {
        super.OnConnectionManagerConnected();

        m_Device = null;
        m_IsConnected = false;
        m_IsDownloading = false;
        m_DownloadedData = new LinkedList<>();

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
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (m_IsDeviceRecording)
        {
            menu.findItem(R.id.menu_start).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
        }
        else
        {
            menu.findItem(R.id.menu_start).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
        }

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
                        m_DownloadFile = uri;

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
            m_StartDate = Util.GetDateFromData(data);
        }
        else if (characteristic.equalsIgnoreCase(Constants.KIMCHI_V1_SENSOR_DATA.UUID))
        {
            String time = Util.GetDataString(Util.GetTimeInBytes(System.currentTimeMillis()), Constants.CharacteristicReadType.TIME);
            KimchiSensorData ksd = KimchiSensorData.CreateFromBytes(data);
            ksd.Time = time;

            // save data to file
            try (FileOutputStream fos = getApplicationContext().openFileOutput(DATA_FILE, MODE_PRIVATE | MODE_APPEND))
            {
                fos.write(ksd.ToString().getBytes());
            } catch (Exception ex) {
                Logger.Exception(TAG, ex);
            }

            // add data to view
            DisplayKimchiData(ksd);

            if (ksd.IsZero()) SetMenuItemState(false);
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

    private void DownloadDataToFile()
    {
        try
        {
            OutputStream os = getApplicationContext().getContentResolver().openOutputStream(m_DownloadFile);
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
                    c.add(Calendar.MINUTE, 1);
                    String time = dateFormat.format(c.getTime());

                    KimchiSensorData ksd = KimchiSensorData.CreateFromBytes(new byte[] {
                        chunk[i],
                        chunk[i + 1],
                        chunk[i + 2],
                        chunk[i + 3],
                        chunk[i + 4],
                        chunk[i + 5],
                        chunk[i + 6],
                        chunk[i + 7]
                    });
                    ksd.Time = time;

                    String csvLine = ksd.ToString();
                    os.write(csvLine.getBytes());
                }
            }

            os.close();

            SetMenuItemState(false);
        }
        catch (IOException e)
        {
            Logger.Debug(TAG, e.getStackTrace().toString());
        }
    }

    private void DisplayKimchiData(KimchiSensorData ksd)
    {
        LinearLayout insertPoint = findViewById(R.id.data_scroll);

        // inner temperature must be computed, just pick it so it's within 7 degrees of outer temperature
        float innerTemperature = Math.abs(ksd.Temperature - ksd.InnerTemperature) > 7 ? ksd.InnerTemperature + 15 : ksd.InnerTemperature;

        View kimchiView = m_LayoutInflater.inflate(R.layout.kimchi_select, null);
        String[] dateData = ksd.Time.split(" ");
        TextView dateView = kimchiView.findViewById(R.id.date);
        dateView.setText(dateData[0]);
        TextView timeView = kimchiView.findViewById(R.id.time);
        timeView.setText(dateData[1]);
        TextView innerView = kimchiView.findViewById(R.id.inner);
        innerView.setText(String.format(Locale.getDefault(), "%.1f\u2103", ksd.Temperature));
        TextView outerView = kimchiView.findViewById(R.id.outer);
        outerView.setText(String.format(Locale.getDefault(), "%.1f\u2103", innerTemperature));
        TextView humidityView = kimchiView.findViewById(R.id.humidity);
        humidityView.setText(String.format(Locale.getDefault(), "%d%%", ksd.Humidity));
        TextView pressureView = kimchiView.findViewById(R.id.pressure);
        pressureView.setText(String.format(Locale.getDefault(), "%d mmHg", ksd.Pressure));
        TextView gasView = kimchiView.findViewById(R.id.gas);
        gasView.setText(String.format(Locale.getDefault(), "%d", ksd.Gas));

        insertPoint.addView(kimchiView, 0);
    }

    private void ShowExistingData()
    {
        try
        {
            FileInputStream fis = getApplicationContext().openFileInput(DATA_FILE);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            try (BufferedReader reader = new BufferedReader(isr))
            {
                String line = reader.readLine();
                while (line != null)
                {
                    DisplayKimchiData(new KimchiSensorData(line));
                    line = reader.readLine();
                }
            }
            catch (IOException ex)
            {
                Logger.Exception(TAG, ex);
            }
        }
        catch(Exception ex)
        {
            Logger.Exception(TAG, ex);
        }
    }

    private void SetMenuItemState(boolean isDeviceRecording)
    {
        m_IsDeviceRecording = isDeviceRecording;

        SharedPreferences p = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = p.edit();
        editor.putBoolean(PREFERENCES_IS_DEVICE_RECORDING_KEY, m_IsDeviceRecording);
        editor.apply();

        invalidateOptionsMenu();
    }
}
