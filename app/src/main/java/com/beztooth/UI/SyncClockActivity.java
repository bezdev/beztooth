package com.beztooth.UI;

import android.bluetooth.BluetoothGattCharacteristic;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;

public class SyncClockActivity extends BluetoothActivity
{
    private static final String TAG = "SyncClockActivity";
    private static final String CLOCK_DEVICE_PREFIX = "Sergei";

    private ConnectionManager m_ConnectionManager;
    private boolean m_IsConnectionManagerBound;

    // TODO: put this logic into BluetoothActivity
    private ServiceConnection m_ConnectionManagerConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            ConnectionManager.LocalBinder binder = (ConnectionManager.LocalBinder) service;
            m_ConnectionManager = binder.getService();
            m_IsConnectionManagerBound = true;

            m_ConnectionManager.ScanDevices();
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
                if (!intent.getStringExtra(ConnectionManager.NAME).startsWith(CLOCK_DEVICE_PREFIX))
                {
                    return;
                }

                AddDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
            }
            else if (intent.getAction().equals(ConnectionManager.ON_DEVICE_CONNECTED))
            {
                ConnectionManager.Device device = m_ConnectionManager.GetDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
                if (device == null) return;

                BluetoothGattCharacteristic c = device.GetCharacteristic("00001805-0000-1000-8000-00805F9B34FB", "00002A2B-0000-1000-8000-00805F9B34FB");
                if (c == null) return;
                c.setValue(ConnectionManager.GetTimeInBytes(System.currentTimeMillis()));
                device.WriteCharacteristic(c);
                // TODO: figure out why this doesn't disconnect
                device.Disconnect();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_sync);

        m_IsConnectionManagerBound = false;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_DEVICE_SCANNED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_DISCONNECTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);

        Intent intent = new Intent(this, ConnectionManager.class);
        bindService(intent, m_ConnectionManagerConnection, Context.BIND_AUTO_CREATE);
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
        unbindService(m_ConnectionManagerConnection);
        m_IsConnectionManagerBound = false;
        super.onDestroy();
    }


    private void AddDevice(String address)
    {
        if (!m_IsConnectionManagerBound) return;

        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        LinearLayout insertPoint = findViewById(R.id.device_scroll);

        // If device is already visible, skip.
        if (insertPoint.findViewWithTag(device.GetAddress()) != null)
        {
            return;
        }

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = vi.inflate(R.layout.device_select, null);

        // Set the device name and address, if there is no name, only display the address.
        TextView textView = view.findViewById(R.id.device_select_name);
        textView.setText(device.GetName());
        if (!device.GetName().equals(device.GetAddress()))
        {
            textView = view.findViewById(R.id.device_select_mac);
            textView.setText(device.GetAddress());
        }

        // Make clickable and set onClick event handler.
        BezContainer container = view.findViewById(R.id.device_select_container);
        container.setTag(device.GetAddress());
        container.setClickable(true);
        container.SetOnClick(new BezContainer.OnClick()
        {
            @Override
            public void Do(View view)
            {
                String address = view.getTag().toString();

                ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                if (device == null) return;

                device.Connect();
            }
        });

        insertPoint.addView(view);
    }
}
