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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    public class CharacteristicActionOnClick implements View.OnClickListener
    {
        private String m_ServiceUUID;
        private String m_CharacteristicUUID;
        public CharacteristicActionOnClick(String serviceUUID, String characteristicUUID) {
            m_ServiceUUID = serviceUUID;
            m_CharacteristicUUID = characteristicUUID;
        }

        @Override
        public void onClick(View v)
        {
            int action = Integer.parseInt(v.getTag().toString());
            if (action == BluetoothGattCharacteristic.PROPERTY_READ)
            {
                m_Device.ReadCharacteristic(m_Device.GetCharacteristic(m_ServiceUUID, m_CharacteristicUUID));
            }
            else if (action == BluetoothGattCharacteristic.PROPERTY_WRITE)
            {
                // TODO: only sync time is supported for now - add ability to write custom data
                if (m_ServiceUUID.contains("1805") && m_CharacteristicUUID.contains("2a2b")) {
                    BluetoothGattCharacteristic c = m_Device.GetCharacteristic(m_ServiceUUID, m_CharacteristicUUID);
                    c.setValue(ConnectionManager.GetTimeInBytes(System.currentTimeMillis()));
                    m_Device.WriteCharacteristic(c);
                }
            }
            else if (action == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
            {
                // TODO
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

    private void AddServiceSelect(String serviceUUID, List<BluetoothGattCharacteristic> characteristics)
    {
        String serviceName = Constants.Services.Get(m_Address, serviceUUID, false);

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Service layout shows text for both service name and uuid.  If there is no service name, only display the uuid.
        View serviceView = vi.inflate(R.layout.service_select, null);
        serviceView.setTag(serviceUUID);

        TextView serviceNameView = serviceView.findViewById(R.id.service_name);
        serviceNameView.setText(serviceName == null ? serviceUUID : serviceName);
        TextView serviceUUIDView = serviceView.findViewById(R.id.service_uuid);
        // Show uuid only if there is a service name, otherwise the name will be the uuid.
        if (serviceName != null)
        {
            serviceUUIDView.setText(serviceUUID);
        }
        else
        {
            serviceUUIDView.setVisibility(View.GONE);
        }
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
            AddCharacteristicView(serviceUUID, c.getUuid().toString(), serviceView);
        }

        LinearLayout insertPoint = findViewById(R.id.service_scroll);
        insertPoint.addView(serviceView);
    }

    private void AddCharacteristicView(String serviceUUID, String characteristicUUID, View serviceView)
    {
        Constants.Characteristic characteristicConstant = Constants.Characteristics.Get(m_Address, serviceUUID, characteristicUUID);
        String characteristicName = characteristicConstant == null ? null : characteristicConstant.Name;

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Characteristic layout shows text for both characteristic name and uuid.  If there is no characteristic name, only display the uuid.
        View characteristicView = vi.inflate(R.layout.characteristic_select, null);
        characteristicView.setTag(characteristicUUID);
        TextView characteristicNameView = characteristicView.findViewById(R.id.characteristic_name);
        characteristicNameView.setText(characteristicName == null ? characteristicUUID : characteristicName);
        TextView characteristicUUIDView = characteristicView.findViewById(R.id.characteristic_uuid);
        // Characteristic uuid only if there is a characteristic name, otherwise the name will be the uuid.
        if (characteristicName != null)
        {
            characteristicUUIDView.setText(characteristicUUID);
        }
        else
        {
            characteristicUUIDView.setVisibility(View.GONE);
        }

        // Display permissions of this characteristic
        int properties = m_Device.GetCharacteristic(serviceUUID, characteristicUUID).getProperties();
        TextView characteristicPermissions = characteristicView.findViewById(R.id.characteristic_permissions);
        characteristicPermissions.setText("Properties: " + ConnectionManager.GetProperties(properties, " | "));
        // Show the supported buttons
        ShowCharacteristicActions(serviceUUID, characteristicView, properties);

        LinearLayout insertPoint = serviceView.findViewById(R.id.characteristic_scroll);
        insertPoint.addView(characteristicView);
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
            characteristicValue = ConnectionManager.GetDataString(data, Constants.CharacteristicReadType.HEX);
        }
        else
        {
            characteristicValue = ConnectionManager.GetDataString(data, characteristicConstant.ReadType);
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

    private void ShowCharacteristicActions(String serviceUUID, View characteristicView, int properties)
    {
        int[] c_SupportedActions = new int[] {
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY
        };

        for (int action : c_SupportedActions) {
            Button button = null;
            if (action == BluetoothGattCharacteristic.PROPERTY_READ) {
                button = characteristicView.findViewById(R.id.characteristic_read);
            } else if (action == BluetoothGattCharacteristic.PROPERTY_WRITE) {
                button = characteristicView.findViewById(R.id.characteristic_write);
            } else if (action == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                button = characteristicView.findViewById(R.id.characteristic_notify);
            }

            button.setTag(action);

            if ((properties & action) == action)
            {
                button.setVisibility(View.VISIBLE);
                View.OnClickListener onClick = new CharacteristicActionOnClick(serviceUUID, characteristicView.getTag().toString());
                button.setOnClickListener(onClick);
            }
            else
            {
                button.setVisibility(View.GONE);
            }
        }
    }
}
