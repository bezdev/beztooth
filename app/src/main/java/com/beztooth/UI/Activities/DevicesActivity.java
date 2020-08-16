package com.beztooth.UI.Activities;

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
import com.beztooth.Util.Logger;

public class DevicesActivity extends BluetoothActivity
{
    private static final String TAG = "DevicesActivity";

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
            else if (action.equals(ConnectionManager.ON_DEVICE_CONNECTED))
            {
                m_DeviceSelectView.OnDeviceConnectionStatusChanged(intent.getStringExtra(ConnectionManager.ADDRESS), true, false);
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_DISCONNECTED))
            {
                m_DeviceSelectView.OnDeviceConnectionStatusChanged(intent.getStringExtra(ConnectionManager.ADDRESS), false, intent.getBooleanExtra(ConnectionManager.DATA, false));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        m_ScanProgress = findViewById(R.id.scanProgress);
        m_ScanProgress.setVisibility(View.GONE);

        m_DeviceSelectView  = new DeviceSelectView(getApplicationContext(), (LinearLayout)findViewById(R.id.device_scroll), true);

        AddEventListeners();
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

        m_DeviceSelectView.AddDevice(device.GetName(), device.GetAddress(), new ViewInputHandler.OnClick()
        {
            @Override
            public void Do(View view)
            {
                String address = view.getTag().toString();
                Logger.Debug(TAG, "OnDeviceClick: " + address);

                ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                if (device == null)
                {
                    return;
                }

                Intent intent = new Intent(view.getContext(), DeviceActivity.class);
                intent.putExtra(ConnectionManager.ADDRESS, device.GetAddress());
                intent.putExtra(ConnectionManager.NAME, device.GetName());
                m_Activity.startActivityForResult(intent, 0);
            }
        });

        if (device.IsConnected())
        {
            m_DeviceSelectView.OnDeviceConnectionStatusChanged(address, true, false);
        }
    }
}
