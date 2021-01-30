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

import java.util.Arrays;

public class GarageDoorActivity extends BluetoothActivity
{
    private static final String TAG = "GarageDoorActivity";
    private static final String[] SUPPORTED_DEVICES = new String[] {
            "08:6B:D7:6E:E1:7F",
            "84:2E:14:23:C3:AE"
    };

    private static final int SCAN_INTERVAL = 5000;

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
                Scan();
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_SCANNED))
            {
                AddDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage_door);

        m_DeviceSelectView = new DeviceSelectView(getApplicationContext(), (LinearLayout)findViewById(R.id.device_scroll), false, true);

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
            //m_DeviceSelectView.ClearDevices();
        }

        m_ScanProgress.setVisibility(View.VISIBLE);

        m_ConnectionManager.Scan(true, SCAN_INTERVAL);
    }

    private void AddDevice(String address)
    {
        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        // filter devices
        boolean isSupportedDevice = false;
        for (String supportedDevice : SUPPORTED_DEVICES)
        {
            if (device.GetAddress().equals(supportedDevice))
            {
                isSupportedDevice = true;
                break;
            }
        }
        if (!isSupportedDevice) return;

        m_DeviceSelectView.AddDevice(device.GetName(), device.GetAddress(), new ViewInputHandler.OnClick()
        {
            @Override
            public void Do(View view) { }
        });

        // If device is already connected, we can't sync.
        if (device.IsConnected())
        {
            m_DeviceSelectView.OnDeviceConnectionStatusChanged(address, true, false);
            m_DeviceSelectView.SetDeviceSelectState(address, false);
        }
    }
}
