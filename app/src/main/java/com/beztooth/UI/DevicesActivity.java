package com.beztooth.UI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

public class DevicesActivity extends BluetoothActivity
{
    private static final String TAG = "DevicesActivity";

    private BroadcastReceiver m_BroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(ConnectionManager.ON_DEVICE_SCANNED))
            {
                AddDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        AddEventListeners();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_DEVICE_SCANNED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_DISCONNECTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);
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
        ClearDevices();

        // Some devices are already connected, display the ones that are.  Connected devices will not
        // show up during the scan so we must make sure they are already displayed.
        HashSet<String> connectedDevices = m_ConnectionManager.GetConnectedDevices();
        Iterator<String> it = connectedDevices.iterator();
        while(it.hasNext())
        {
            LinearLayout ll = findViewById(R.id.device_scroll);
            String deviceAddress = it.next();
            View device = ll.findViewWithTag(deviceAddress);
            if (device == null)
            {
                    AddDevice(deviceAddress);
            }
        }

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
        container.SetOnClick(new ViewInputHandler.OnClick()
        {
            @Override
            public void Do(View view)
            {
                String address = view.getTag().toString();
                Logger.Debug(TAG, "OnDeviceClick: " + address);

                ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                if (device == null) {
                    return;
                }

                Intent intent = new Intent(view.getContext(), DeviceActivity.class);
                intent.putExtra(ConnectionManager.ADDRESS, device.GetAddress());
                intent.putExtra(ConnectionManager.NAME, device.GetName());
                // TODO: listen for result
                m_Activity.startActivityForResult(intent, 2);
            }
        });

        insertPoint.addView(view);
    }

    private void ClearDevices()
    {
        LinearLayout ll = findViewById(R.id.device_scroll);
        if (ll != null)
        {
            ll.removeAllViews();
        }
    }
}
