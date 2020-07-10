package com.beztooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.beztooth.Bluetooth.ConnectionManager;

public class TestDeviceActivity extends AppCompatActivity
{
    private ConnectionManager m_ConnectionManager;
    private boolean m_IsConnectionManagerBound;

    private static final String ADDRESS = "00:0B:57:1A:88:EF";

    private ServiceConnection m_ConnectionManagerConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            ConnectionManager.LocalBinder binder = (ConnectionManager.LocalBinder) service;
            m_ConnectionManager = binder.getService();
            m_IsConnectionManagerBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className)
        {
            m_IsConnectionManagerBound = false;
        }
    };

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
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_device);
    }

    public void synctime(View v)
    {
        if (!m_IsConnectionManagerBound) return;

        ConnectionManager.Device d = m_ConnectionManager.GetDevice(ADDRESS);
        if (d == null) return;

        BluetoothGattCharacteristic c = d.GetCharacteristic("00001805-0000-1000-8000-00805f9b34fb", "00002a2b-0000-1000-8000-00805f9b34fb");
        if (c == null) return;

        c.setValue(ConnectionManager.GetTimeInBytes(System.currentTimeMillis()));
        d.WriteCharacteristic(c);
    }
}
