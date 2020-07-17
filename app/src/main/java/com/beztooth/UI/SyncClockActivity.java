package com.beztooth.UI;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.Util.Constants;

public class SyncClockActivity extends BluetoothActivity
{
    private static final String TAG = "SyncClockActivity";
    private static final String CLOCK_DEVICE_PREFIX = "Sergei";

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
            else if (intent.getAction().equals(ConnectionManager.ON_SERVICES_DISCOVERED))
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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_sync);
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
        intentFilter.addAction(ConnectionManager.ON_DEVICE_SCANNED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_DISCONNECTED);
        intentFilter.addAction(ConnectionManager.ON_SERVICES_DISCOVERED);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);

        m_ConnectionManager.ScanDevices();
    }

    private void AddDevice(String address)
    {
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
        container.SetOnClick(new ViewInputHandler.OnClick()
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

        insertPoint.addView(view);
    }
}
