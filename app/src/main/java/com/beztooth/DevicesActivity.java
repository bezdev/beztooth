package com.beztooth;

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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class DevicesActivity extends Activity
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
        super.onStop();
        unbindService(m_ConnectionManagerConnection);
        m_IsConnectionManagerBound = false;
    }

    @Override
    protected void onDestroy()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_BroadcastReceiver);
        super.onDestroy();
    }

    private View.OnClickListener m_OnDeviceClick  = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            String address = v.getTag().toString();
            Logger.Debug(TAG, "OnDeviceClick: " + address);

            ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);

            Intent intent = new Intent(v.getContext(), DeviceActivity.class);
            intent.putExtra(ConnectionManager.ADDRESS, device.GetAddress());
            intent.putExtra(ConnectionManager.NAME, device.GetName());
            v.getContext().startActivity(intent);
        }
    };

    private void AddDeviceToUI(String address)
    {
        if (!m_IsConnectionManagerBound) return;

        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.device_select, null);

        TextView textView = v.findViewById(R.id.device_name);
        textView.setText(device.GetName());
        if (!device.GetName().equals(device.GetAddress())) {
            textView = v.findViewById(R.id.device_mac);
            textView.setText(device.GetAddress());
        }

        v.setTag(device.GetAddress());
        v.setClickable(true);
        v.setOnClickListener(m_OnDeviceClick);

        LinearLayout insertPoint = findViewById(R.id.device_scroll);
        insertPoint.addView(v);
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
