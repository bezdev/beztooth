package com.beztooth.UI;

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
import com.beztooth.Util.Constants;

public class SyncClockActivity extends BluetoothActivity
{
    private static final String TAG = "SyncClockActivity";
    private static final String CLOCK_DEVICE_PREFIX = "Sergei";

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
                if (!intent.getStringExtra(ConnectionManager.NAME).startsWith(CLOCK_DEVICE_PREFIX))
                {
                    return;
                }

                AddDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
            }
            else if (action.equals(ConnectionManager.ON_SERVICES_DISCOVERED))
            {
                ConnectionManager.Device device = m_ConnectionManager.GetDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
                if (device == null) return;

                BluetoothGattCharacteristic c = device.GetCharacteristic(Constants.AddBaseUUID("1805"), Constants.AddBaseUUID("2A2B"));
                if (c == null) return;

                // Set value to current time
                c.setValue(ConnectionManager.GetTimeInBytes(System.currentTimeMillis()));
                device.WriteCharacteristic(c);
                device.Disconnect();
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_CONNECTED))
            {
                m_DeviceSelectView.OnDeviceConnectionStatusChanged(intent.getStringExtra(ConnectionManager.ADDRESS), true);
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_DISCONNECTED))
            {
                m_DeviceSelectView.OnDeviceConnectionStatusChanged(intent.getStringExtra(ConnectionManager.ADDRESS), false);
            }
            else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_WRITE))
            {
                String syncedTime = ConnectionManager.GetDataString(intent.getByteArrayExtra(ConnectionManager.DATA), Constants.CharacteristicReadType.TIME);
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
            public void onClick(View v)
            {
                Scan();
            }
        });
    }

    private void Scan()
    {
        m_ConnectionManager.StopScan();
        m_DeviceSelectView.ClearDevices();
        m_ConnectionManager.Scan();
    }

    private void AddDevice(String address)
    {
        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        m_DeviceSelectView.AddDevice(device.GetName(), device.GetAddress(), new ViewInputHandler.OnClick()
        {
            @Override
            public void Do(View view)
            {
                String address = view.getTag().toString();

                ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                if (device == null) return;

                device.Connect(true, false);
            }
        });
    }
}
