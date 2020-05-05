package com.beztooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

public class DeviceActivity extends AppCompatActivity
{
    public final static String TAG = "DeviceActivity";

    private ConnectionManager m_ConnectionManager;
    private boolean m_IsConnectionManagerBound;
    private boolean m_AreServicesDiscovered;

    private ConnectionManager.Device m_Device;
    private String m_Address;
    private String m_Name;

    private ServiceConnection m_ConnectionManagerConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            ConnectionManager.LocalBinder binder = (ConnectionManager.LocalBinder) service;
            m_ConnectionManager = binder.getService();

            m_Device = m_ConnectionManager.GetDevice(m_Address);

            if (m_Device == null || !m_Device.IsConnected())
            {
                m_ConnectionManager.ConnectDevice(m_Address);
            }
            else if (m_Device.IsConnected())
            {
                m_Device.DiscoverServices();
            }

            m_AreServicesDiscovered = false;
            m_IsConnectionManagerBound = true;
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
            if (!m_IsConnectionManagerBound) return;

            String address = intent.getStringExtra(ConnectionManager.ADDRESS);
            if (address == null || !address.equals(m_Address)) return;

            if (intent.getAction().equals(ConnectionManager.ON_DEVICE_CONNECTED))
            {
                m_Device = m_ConnectionManager.GetDevice(address);
            }
            else if (intent.getAction().equals(ConnectionManager.ON_SERVICES_DISCOVERED))
            {
                ShowServices();
                m_AreServicesDiscovered = true;
            }
            else if (intent.getAction().equals(ConnectionManager.ON_CHARACTERISTIC_READ))
            {
                if (!m_AreServicesDiscovered) return;

                String service = intent.getStringExtra(ConnectionManager.SERVICE);
                String characteristic = intent.getStringExtra(ConnectionManager.CHARACTERISTIC);
                byte[] data = intent.getByteArrayExtra(ConnectionManager.DATA);
                UpdateCharacteristicData(service, characteristic, data);
            }
            else if (intent.getAction().equals(ConnectionManager.ON_DESCRIPTOR_READ))
            {
                if (!m_AreServicesDiscovered) return;

                String service = intent.getStringExtra(ConnectionManager.SERVICE);
                String characteristic = intent.getStringExtra(ConnectionManager.CHARACTERISTIC);
                byte[] data = intent.getByteArrayExtra(ConnectionManager.DATA);
                //UpdateDescriptorData(service, characteristic, data);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        m_IsConnectionManagerBound = false;
        m_AreServicesDiscovered = false;

        Intent intent = getIntent();
        m_Name = intent.getStringExtra(ConnectionManager.NAME);
        m_Address = intent.getStringExtra(ConnectionManager.ADDRESS);

        Toolbar toolbar = findViewById(R.id.toolbarDevice);
        toolbar.setTitle(m_Name);

        Logger.Debug(TAG, "DeviceActivity::OnCreate");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_SERVICES_DISCOVERED);
        intentFilter.addAction(ConnectionManager.ON_CHARACTERISTIC_READ);
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

    private void ShowServices()
    {
        List<BluetoothGattService> services = m_Device.GetServices();

        LinkedList<BluetoothGattService> sortedServices = new LinkedList<>();
        for (BluetoothGattService bgs : services)
        {
            String knownService = Constants.Services.Get(bgs.getUuid().toString());
            if (knownService != null) {
                sortedServices.add(bgs);
            }
        }
        for (BluetoothGattService bgs : services)
        {
            Constants.Device d = Constants.Devices.GetDevice(m_Address);
            if (d != null)
            {
                String knownService = Constants.Services.Get(m_Address, bgs.getUuid().toString(), true);
                if (knownService != null)
                {
                    sortedServices.addFirst(bgs);
                }
            }
            else
            {
                String knownService = Constants.Services.Get(bgs.getUuid().toString());
                if (knownService == null)
                {
                    sortedServices.add(bgs);
                }
            }
        }

        for (BluetoothGattService s : sortedServices)
        {
            AddServiceSelect(s.getUuid().toString(), s.getCharacteristics());
        }
    }

    private void AddServiceSelect(String serviceUuid, List<BluetoothGattCharacteristic> characteristics)
    {
        String serviceName = Constants.Services.Get(m_Address, serviceUuid, false);

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View service = vi.inflate(R.layout.service_select, null);
        TextView textView = service.findViewById(R.id.service_name);
        textView.setText(serviceName == null ? serviceUuid : serviceName);
        textView = service.findViewById(R.id.service_uuid);
        if (serviceName != null)
        {
            textView.setText(serviceUuid);
        } else
        {
            textView.setVisibility(View.GONE);
        }

        service.setTag(serviceUuid);
        //v.setClickable(true);
        //v.setOnClickListener();

        LinkedList<BluetoothGattCharacteristic> sortedCharacteristics = new LinkedList<>();
        for (BluetoothGattCharacteristic bgc : characteristics)
        {
            Constants.Characteristic knownCharacteristic = Constants.Characteristics.Get(m_Address, bgc.getService().getUuid().toString(), bgc.getUuid().toString());
            if (knownCharacteristic != null)
            {
                sortedCharacteristics.addFirst(bgc);
            }
            else
            {
                sortedCharacteristics.add(bgc);
            }
        }

        for (BluetoothGattCharacteristic c : sortedCharacteristics)
        {
            AddCharacteristicView(serviceUuid, c.getUuid().toString(), service);
        }

        LinearLayout insertPoint = findViewById(R.id.service_scroll);
        insertPoint.addView(service);
    }

    private void AddCharacteristicView(String serviceUuid, String characteristicUuid, View service)
    {
        Constants.Characteristic characteristicConstant = Constants.Characteristics.Get(m_Address, serviceUuid, characteristicUuid);
        String characteristicName = characteristicConstant == null ? null : characteristicConstant.Name;

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View characteristic = vi.inflate(R.layout.characteristic_select, null);
        TextView textView = characteristic.findViewById(R.id.characteristic_name);
        textView.setText(characteristicName == null ? characteristicUuid : characteristicName);
        textView = characteristic.findViewById(R.id.characteristic_uuid);
        if (characteristicName != null)
        {
            textView.setText(serviceUuid);
        }
        else
        {
            textView.setVisibility(View.GONE);
        }
        int properties = m_Device.GetCharacteristic(serviceUuid, characteristicUuid).getProperties();
        textView = characteristic.findViewById(R.id.characteristic_permissions);
        textView.setText("Properties: " + ConnectionManager.GetProperties(properties, " | "));

        characteristic.setTag(characteristicUuid);

        LinearLayout insertPoint = service.findViewById(R.id.characteristic_scroll);
        insertPoint.addView(characteristic);
    }

    private void UpdateCharacteristicData(String serviceUuid, String characteristicUuid, byte[] data)
    {
        LinearLayout services = findViewById(R.id.service_scroll);
        TextView textView = services.findViewWithTag(serviceUuid).findViewWithTag(characteristicUuid).findViewById(R.id.characteristic_data_label);
        textView.setVisibility(View.VISIBLE);
        textView.setText("Data: " + data.length + " byte" + ((data.length == 1) ? "" :  "s"));
        textView = services.findViewWithTag(serviceUuid).findViewWithTag(characteristicUuid).findViewById(R.id.characteristic_data);
        textView.setVisibility(View.VISIBLE);

        String characteristicValue;
        Constants.Characteristic characteristicConstant = Constants.Characteristics.Get(m_Address, serviceUuid, characteristicUuid);
        if (characteristicConstant == null)
        {
            characteristicValue = GetDataString(data, Constants.CharacteristicReadType.HEX);
        }
        else
        {
            characteristicValue = GetDataString(data, characteristicConstant.ReadType);
        }

        textView.setText(characteristicValue);
    }

    private void UpdateDescriptorData(String service, String characteristic, byte[] data)
    {
        LinearLayout services = findViewById(R.id.service_scroll);
        TextView textView = services.findViewWithTag(service).findViewWithTag(characteristic).findViewById(R.id.characteristic_descriptors);
        textView.setVisibility(View.VISIBLE);
        String descriptorText = new String(data);
        if (!textView.getText().toString().contains(descriptorText + "\n"))
        {
            textView.append(descriptorText + "\n");
        }
    }

    private static String GetDataString(byte[] data, Constants.CharacteristicReadType type)
    {
        if (type == Constants.CharacteristicReadType.STRING)
        {
            return new String(data);
        }
        else if (type == Constants.CharacteristicReadType.HEX)
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++)
            {
                sb.append(String.format("%02X", data[i]));
                if (i <= data.length - 1) sb.append(" ");
            }
            return sb.toString();
        }
        else if (type == Constants.CharacteristicReadType.INTEGER)
        {
            return "" + ByteBuffer.allocate(4).put(data).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
        }
        else if (type == Constants.CharacteristicReadType.TIME)
        {
            if (data.length != 10) return "";

            return "" + data[2] + "-" + data[3] + ": " + data[4] + ":" + data[5] + ":" + data[6];
        }

        return "";
    }
}
