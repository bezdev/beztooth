package com.beztooth.UI.Activities;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.UI.Util.BezButton;
import com.beztooth.UI.Util.DeviceSelectView;
import com.beztooth.UI.Util.ViewInputHandler;
import com.beztooth.Util.Constants;
import com.beztooth.Util.Util;

import java.util.Calendar;
import java.util.Locale;

public class CounterActivity extends BluetoothActivity
{
    private static final String TAG = "CounterActivity";
    private static final String COUNTER_DEVICE_PREFIX = "Sergei";

    private static final String COUNTER_SERVICE = "046f72a4-4e37-4a70-a825-c21f257cde16";
    private static final String COUNTER_ONE_CHARACTERISTIC = "046f72a4-4e37-4a70-a825-c21f257cde17";
    private static final String COUNTER_TWO_CHARACTERISTIC = "046f72a4-4e37-4a70-a825-c21f257cde18";
    private static final String CONTROL_CHARACTERISTIC = "046f72a4-4e37-4a70-a825-c21f257cde19";

    private ProgressBar m_ScanProgress;
    private DeviceSelectView m_DeviceSelectView;

    protected void OnBroadcastEvent(Intent intent) {
        String action = intent.getAction();

        if (action.equals(ConnectionManager.ON_SCAN_STARTED))
        {
            m_ScanProgress.setVisibility(View.VISIBLE);
        }
        else if (action.equals(ConnectionManager.ON_SCAN_STOPPED))
        {
            m_ScanProgress.setVisibility(View.GONE);
        }
        else if (action.equals(ConnectionManager.ON_DEVICE_SCANNED))
        {
            AddDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
        }
        else if (action.equals(ConnectionManager.ON_SERVICES_DISCOVERED))
        {
            final ConnectionManager.Device device = m_ConnectionManager.GetDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
            if (device == null) return;

            AddExtra(device);
        }
        else if (action.equals(ConnectionManager.ON_DEVICE_CONNECTED))
        {
            m_DeviceSelectView.OnDeviceConnectionStatusChanged(intent.getStringExtra(ConnectionManager.ADDRESS), true, false);
        }
        else if (action.equals(ConnectionManager.ON_DEVICE_DISCONNECTED))
        {
            String address = intent.getStringExtra(ConnectionManager.ADDRESS);
            m_DeviceSelectView.OnDeviceConnectionStatusChanged(address, false, intent.getBooleanExtra(ConnectionManager.DATA, false));
            m_DeviceSelectView.SetDeviceSelectState(address, true);
        }
        else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_READ))
        {
            if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(COUNTER_ONE_CHARACTERISTIC))
            {
                String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                TextView counterOneValue = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.counterOneValue);
                counterOneValue.setText(Util.GetDataString(intent.getByteArrayExtra(ConnectionManager.DATA), Constants.CharacteristicReadType.TIME));
            }
            else if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(COUNTER_TWO_CHARACTERISTIC))
            {
                String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                TextView counterTwoValue = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.counterTwoValue);
                counterTwoValue.setText(Util.GetDataString(intent.getByteArrayExtra(ConnectionManager.DATA), Constants.CharacteristicReadType.TIME));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_clock);

        // Initialize views
        m_DeviceSelectView = new DeviceSelectView(getApplicationContext(), (LinearLayout)findViewById(R.id.device_scroll));

        AddEventListeners();

        m_ScanProgress = findViewById(R.id.scanProgress);
        m_ScanProgress.setVisibility(View.GONE);
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
        super.onDestroy();
    }

    @Override
    protected void OnConnectionManagerConnected()
    {
        super.OnConnectionManagerConnected();

        if (m_ConnectionManager.IsScanning())
        {
            for (String mac : m_ConnectionManager.GetScannedDevices())
            {
                AddDevice(mac);
            }
        }

        Scan();
    }

    private void AddEventListeners()
    {
        // Scan onClick
        Button button = findViewById(R.id.toolbarScanButton);
        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                Scan();
            }
        });
    }

    private void Scan()
    {
        if (!m_ConnectionManager.IsScanning())
        {
            // Fresh scan, clear devices
            m_DeviceSelectView.ClearDevices();
        }

        m_ScanProgress.setVisibility(View.VISIBLE);

        m_ConnectionManager.Scan(false, 30000);
    }

    private void AddDevice(String address)
    {
        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        if (!device.GetName().startsWith(COUNTER_DEVICE_PREFIX)) return;

        m_DeviceSelectView.AddDevice(device.GetName(), device.GetAddress(), null);

        device.Connect();
    }

    private void AddExtra(final ConnectionManager.Device device)
    {
        View extra = m_LayoutInflater.inflate(R.layout.counter_select, null);

        BezButton counterOneResetButton = extra.findViewById(R.id.counterOneReset);
        counterOneResetButton.SetOnClick(new ViewInputHandler.OnClick() {
            @Override
            public void Do(View view) {
                device.WriteCharacteristic(COUNTER_SERVICE, CONTROL_CHARACTERISTIC, Util.GetByteArrayFromInteger(1, 4));
                device.ReadCharacteristic(COUNTER_SERVICE, COUNTER_ONE_CHARACTERISTIC);
            }
        });

        BezButton counterTwoResetButton = extra.findViewById(R.id.counterTwoReset);
        counterTwoResetButton.SetOnClick(new ViewInputHandler.OnClick() {
            @Override
            public void Do(View view) {
                device.WriteCharacteristic(COUNTER_SERVICE, CONTROL_CHARACTERISTIC, Util.GetByteArrayFromInteger(2, 4));
                device.ReadCharacteristic(COUNTER_SERVICE, COUNTER_TWO_CHARACTERISTIC);
            }
        });

        BezButton counterOneUpdateButton = extra.findViewById(R.id.counterOneUpdate);
        counterOneUpdateButton.SetOnClick(new ViewInputHandler.OnClick() {
            @Override
            public void Do(View view) {
                device.ReadCharacteristic(COUNTER_SERVICE, COUNTER_ONE_CHARACTERISTIC);
            }
        });

        BezButton counterTwoUpdateButton = extra.findViewById(R.id.counterTwoUpdate);
        counterTwoUpdateButton.SetOnClick(new ViewInputHandler.OnClick() {
            @Override
            public void Do(View view) {
                device.ReadCharacteristic(COUNTER_SERVICE, COUNTER_TWO_CHARACTERISTIC);
            }
        });

        m_DeviceSelectView.SetExtra(device.GetAddress(), extra);
    }
}
