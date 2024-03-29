package com.beztooth.UI.Activities;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;

public abstract class BluetoothActivity extends AppCompatActivity
{
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 1;

    private static boolean s_WaitingOnEnableBluetoothRequest = false;

    protected Activity m_Activity;
    protected ConnectionManager m_ConnectionManager;
    protected LayoutInflater m_LayoutInflater;

    // Whether or not the Activity is currently visible.
    private boolean m_IsActive = false;
    private boolean m_IsConnectionManagerBound;

    private final ServiceConnection m_ConnectionManagerConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            ConnectionManager.LocalBinder binder = (ConnectionManager.LocalBinder) service;
            m_ConnectionManager = binder.getService();
            m_IsConnectionManagerBound = true;

            OnConnectionManagerConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName className)
        {
            m_IsConnectionManagerBound = false;
        }
    };

    private final BroadcastReceiver m_BluetoothReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(ConnectionManager.ON_BLUETOOTH_DISABLED))
            {
                if (m_IsActive && !s_WaitingOnEnableBluetoothRequest)
                {
                    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
                    s_WaitingOnEnableBluetoothRequest = true;
                }
            }
        }
    };

    private final BroadcastReceiver m_BroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            OnBroadcastEvent(intent);
        }
    };

    protected void OnConnectionManagerConnected()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_SCAN_STARTED);
        intentFilter.addAction(ConnectionManager.ON_SCAN_STOPPED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_SCANNED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_DISCONNECTED);
        intentFilter.addAction(ConnectionManager.ON_SERVICES_DISCOVERED);
        intentFilter.addAction(ConnectionManager.ON_CHARACTERISTIC_READ);
        intentFilter.addAction(ConnectionManager.ON_CHARACTERISTIC_WRITE);
        intentFilter.addAction(ConnectionManager.ON_DESCRIPTOR_READ);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);
    }

    protected void OnBroadcastEvent(Intent intent) { }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_Activity = this;
        m_IsConnectionManagerBound = false;
        m_LayoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        CheckPermissions();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_BLUETOOTH_DISABLED);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BluetoothReceiver, intentFilter);

        Intent intent = new Intent(this, ConnectionManager.class);
        bindService(intent, m_ConnectionManagerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        m_IsActive = true;
    }

    @Override
    protected void onPause()
    {
        m_IsActive = false;

        super.onPause();
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

        // TODO: disconnect connected devices

        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_BluetoothReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_BroadcastReceiver);
        unbindService(m_ConnectionManagerConnection);
        m_IsConnectionManagerBound = false;
    }

    private void CheckPermissions()
    {
        // Determine whether BLE is supported on the device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!HasLocationPermission())
        {
            RequestLocationPermission();
        }

    }

    private boolean HasLocationPermission()
    {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestLocationPermission()
    {
        requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_ENABLE_BT)
        {
            s_WaitingOnEnableBluetoothRequest = false;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
