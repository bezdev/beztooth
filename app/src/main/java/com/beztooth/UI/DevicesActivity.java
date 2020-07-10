package com.beztooth.UI;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.Util.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class DevicesActivity extends BluetoothActivity
{
    private static final String TAG = "DevicesActivity";

    private ConnectionManager m_ConnectionManager;
    private boolean m_IsConnectionManagerBound;

    private ServiceConnection m_ConnectionManagerConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            ConnectionManager.LocalBinder binder = (ConnectionManager.LocalBinder) service;
            m_ConnectionManager = binder.getService();
            m_IsConnectionManagerBound = true;

            if (m_ConnectionManager.GetConnectedDevices().size() == 0)
            {
                m_ConnectionManager.ScanDevices();
            }
            else
            {
                // Some devices are already connected, display the ones that are.
                HashSet<String> connectedDevices = m_ConnectionManager.GetConnectedDevices();
                Iterator<String> it = connectedDevices.iterator();
                while(it.hasNext())
                {
                    LinearLayout ll = findViewById(R.id.device_scroll);
                    String deviceAddress = it.next();
                    View device = ll.findViewWithTag(deviceAddress);
                    if (device == null)
                    {
                        AddDeviceToUI(deviceAddress);
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className)
        {
            m_IsConnectionManagerBound = false;
        }
    };

    private BroadcastReceiver m_BroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(ConnectionManager.ON_DEVICE_SCANNED))
            {
                AddDeviceToUI(intent.getStringExtra(ConnectionManager.ADDRESS));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        m_IsConnectionManagerBound = false;

        AddEventListeners();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_DEVICE_SCANNED);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Intent intent = new Intent(this, ConnectionManager.class);
        bindService(intent, m_ConnectionManagerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop()
    {
        unbindService(m_ConnectionManagerConnection);
        m_IsConnectionManagerBound = false;
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_BroadcastReceiver);
        super.onDestroy();
    }

    private void AddEventListeners()
    {
        Button button = findViewById(R.id.scanbutton);
        button.setOnClickListener(new View.OnClickListener()
        {
            // Scan OnClick
            public void onClick(View v)
            {
                ClearDevicesUI();

                if (m_IsConnectionManagerBound && m_ConnectionManager.Initialize()) {
                    m_ConnectionManager.ScanDevices();
                }
            }
        });
    }

    private void AddDeviceToUI(String address)
    {
        if (!m_IsConnectionManagerBound) return;

        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (View) vi.inflate(R.layout.device_select, null);

        // Set the device name and address, if there is no name, only display the address.
        TextView textView = view.findViewById(R.id.device_select_name);
        textView.setText(device.GetName());
        if (!device.GetName().equals(device.GetAddress()))
        {
            textView = view.findViewById(R.id.device_select_mac);
            textView.setText(device.GetAddress());
        }

        // Make clickable and set onClick event handler.
        BezContainer container = (BezContainer) view.findViewById(R.id.device_select_container);
        container.setTag(device.GetAddress());
        container.setClickable(true);
        container.SetOnClick(new BezContainer.OnClick()
        {
            @Override
            public void Do(View view)
            {
                String address = view.getTag().toString();
                Logger.Debug(TAG, "OnDeviceClick: " + address);

                ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                if (device == null) return;

                Intent intent = new Intent(view.getContext(), DeviceActivity.class);
                intent.putExtra(ConnectionManager.ADDRESS, device.GetAddress());
                intent.putExtra(ConnectionManager.NAME, device.GetName());
                m_Activity.startActivityForResult(intent, 2);
            }
        });

        LinearLayout insertPoint = findViewById(R.id.device_scroll);
        insertPoint.addView(view);
    }

    private void ClearDevicesUI()
    {
        LinearLayout ll = findViewById(R.id.device_scroll);

        // Keep connected devices
        Iterator it = m_ConnectionManager.GetDevices().entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry)it.next();
            ConnectionManager.Device d = (ConnectionManager.Device) pair.getValue();
            if (!d.IsConnected())
            {
                View deviceToRemove = ll.findViewWithTag(pair.getKey().toString());
                ll.removeView(deviceToRemove);
            }
        }
    }
}
