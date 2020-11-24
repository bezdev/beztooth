package com.beztooth.UI.Activities;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.UI.Util.DeviceSelectView;
import com.beztooth.UI.Util.ViewInputHandler;
import com.beztooth.Util.Constants;
import com.beztooth.Util.Util;

public class SyncClockActivity extends BluetoothActivity
{
    private static final String TAG = "SyncClockActivity";
    private static final String CLOCK_DEVICE_PREFIX = "Clock";

    private ProgressBar m_ScanProgress;
    private DeviceSelectView m_DeviceSelectView;

    private BroadcastReceiver m_BroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
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
                ConnectionManager.Device device = m_ConnectionManager.GetDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
                if (device == null) return;

                // Set value to current time
                device.WriteCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID(), Util.GetTimeInBytes(System.currentTimeMillis()));
                device.Disconnect();
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
            else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_WRITE))
            {
                String syncedTime = Util.GetDataString(intent.getByteArrayExtra(ConnectionManager.DATA), Constants.CharacteristicReadType.TIME);
                m_DeviceSelectView.SetDeviceStatusMessage(intent.getStringExtra(ConnectionManager.ADDRESS), "Synced time: " + syncedTime);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_clock);

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_BroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void OnConnectionManagerConnected()
    {
        if (m_ConnectionManager.IsScanning())
        {
            for (String mac : m_ConnectionManager.GetScannedDevices())
            {
                AddDevice(mac);
            }
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_SCAN_STARTED);
        intentFilter.addAction(ConnectionManager.ON_SCAN_STOPPED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_SCANNED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_DISCONNECTED);
        intentFilter.addAction(ConnectionManager.ON_SERVICES_DISCOVERED);
        intentFilter.addAction(ConnectionManager.ON_CHARACTERISTIC_WRITE);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);

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

        m_ConnectionManager.Scan(false);
    }

    private void AddDevice(String address)
    {
        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        if (!device.GetName().startsWith(CLOCK_DEVICE_PREFIX)) return;

        m_DeviceSelectView.AddDevice(device.GetName(), device.GetAddress(), new ViewInputHandler.OnClick()
        {
            @Override
            public void Do(View view)
            {
                String address = view.getTag().toString();

                m_DeviceSelectView.SetDeviceSelectState(address, false);
                m_DeviceSelectView.SetDeviceStatusMessage(address, "");

                ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                device.SetReadCharacteristicsWhenDiscovered(false);
                device.Connect();
            }
        });

        // If device is already connected, we can't sync.
        if (device.IsConnected())
        {
            m_DeviceSelectView.OnDeviceConnectionStatusChanged(address, true, false);
            m_DeviceSelectView.SetDeviceSelectState(address, false);
        }
    }
}
