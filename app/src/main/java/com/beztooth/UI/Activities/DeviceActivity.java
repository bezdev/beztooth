package com.beztooth.UI.Activities;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beztooth.Bluetooth.BluetoothCharacteristic;
import com.beztooth.Bluetooth.BluetoothService;
import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.UI.Util.BezButton;
import com.beztooth.UI.Util.ViewInputHandler;
import com.beztooth.Util.*;

import java.util.LinkedList;
import java.util.List;

public class DeviceActivity extends BluetoothActivity
{
    public final static String TAG = "DeviceActivity";

    private ProgressBar m_ConnectDiscoverProgress;
    private LayoutInflater m_LayoutInflater;

    private ConnectionManager.Device m_Device;
    private boolean m_AreServicesDiscovered;
    private String m_Address;
    private String m_Name;

    private BroadcastReceiver m_BroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String address = intent.getStringExtra(ConnectionManager.ADDRESS);
            String action = intent.getAction();

            // Only process broadcasts for this device.
            if (address == null || !address.equals(m_Address)) return;

            Logger.Debug(TAG, "onReceive: " + address + " - " + action);

            if (action.equals(ConnectionManager.ON_DEVICE_CONNECTED))
            {
                m_Device = m_ConnectionManager.GetDevice(address);
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_DISCONNECTED))
            {
                finish();
            }
            else if (action.equals(ConnectionManager.ON_SERVICES_DISCOVERED))
            {
                m_ConnectDiscoverProgress.setVisibility(View.GONE);
                ShowServices();
                m_AreServicesDiscovered = true;
            }
            else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_READ))
            {
                if (!m_AreServicesDiscovered) return;

                String service = intent.getStringExtra(ConnectionManager.SERVICE);
                String characteristic = intent.getStringExtra(ConnectionManager.CHARACTERISTIC);
                byte[] data = intent.getByteArrayExtra(ConnectionManager.DATA);
                UpdateCharacteristicData(service, characteristic, data);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        m_LayoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        m_ConnectDiscoverProgress = findViewById(R.id.connectDiscoverProgress);
        m_ConnectDiscoverProgress.setVisibility(View.GONE);

        Intent intent = getIntent();
        m_Name = intent.getStringExtra(ConnectionManager.NAME);
        m_Address = intent.getStringExtra(ConnectionManager.ADDRESS);

        Toolbar toolbar = findViewById(R.id.toolbarDevice);
        toolbar.setTitle(m_Name);

        AddEventListeners();
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
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_DISCONNECTED);
        intentFilter.addAction(ConnectionManager.ON_SERVICES_DISCOVERED);
        intentFilter.addAction(ConnectionManager.ON_CHARACTERISTIC_READ);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);

        m_Device = m_ConnectionManager.GetDevice(m_Address);
        if (m_Device == null) finish();

        if (!m_Device.IsConnected())
        {
            m_Device.Connect();
        }
        else
        {
            // TODO: Show existing
            m_AreServicesDiscovered = false;
            m_Device.DiscoverServices();
        }

        m_ConnectDiscoverProgress.setVisibility(View.VISIBLE);
    }

    private void AddEventListeners()
    {
        // Disconnect onClick
        Button button = findViewById(R.id.toolbarDisconnectButton);
        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                m_Device.Disconnect();
                finish();
            }
        });
    }

    private void ShowServices()
    {
        for (BluetoothService s : m_Device.GetServices())
        {
            AddServiceSelect(s.GetUUID(), s.GetCharacteristics());
        }
    }

    private void AddServiceSelect(String serviceUUID, List<BluetoothCharacteristic> characteristics)
    {
        String serviceName = Constants.Services.Get(m_Address, serviceUUID);

        // Service layout shows text for both service name and uuid.  If there is no service name, only display the uuid.
        View serviceView = m_LayoutInflater.inflate(R.layout.service_select, null);
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

        for (BluetoothCharacteristic c : characteristics)
        {
            AddCharacteristicView(serviceUUID, c.GetUUID(), serviceView);
        }

        LinearLayout insertPoint = findViewById(R.id.service_scroll);
        insertPoint.addView(serviceView);
    }

    private void AddCharacteristicView(String serviceUUID, String characteristicUUID, View serviceView)
    {
        Constants.Characteristic characteristicConstant = Constants.Characteristics.Get(m_Address, serviceUUID, characteristicUUID);
        String characteristicName = characteristicConstant == null ? null : characteristicConstant.Name;

        // Characteristic layout shows text for both characteristic name and uuid.  If there is no characteristic name, only display the uuid.
        View characteristicView = m_LayoutInflater.inflate(R.layout.characteristic_select, null);
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

        // Add the supported buttons
        int properties = m_Device.GetCharacteristicProperties(serviceUUID, characteristicUUID);
        AddCharacteristicActions(serviceUUID, characteristicView, properties);

        LinearLayout insertPoint = serviceView.findViewById(R.id.characteristic_scroll);
        insertPoint.addView(characteristicView);
    }

    private void AddCharacteristicActions(String serviceUUID, View characteristicView, int properties)
    {
        int[] c_SupportedActions = new int[] {
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
            BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
        };

        LinkedList<View> actions = new LinkedList<>();
        for (int action : c_SupportedActions)
        {
            // Action not supported
            if ((properties & action) != action) continue;

            actions.add(CreateCharacteristicActionButton(serviceUUID, characteristicView.getTag().toString(), action));
        }

        if (actions.size() == 0) return;

        LinearLayout ll = characteristicView.findViewById(R.id.characteristic_actions);

        // There is no way to automatically overflow these buttons to the next line using a LinearLayout so must compute the
        // width of all the buttons, and add new rows when we reach the max width (taking into account margins and padding).
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float maxWidth  = (displayMetrics.widthPixels / displayMetrics.density - 55);  // 55: margins + padding

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);

        final int MARGIN = 10;

        float totalButtonWidth = 0;
        for (View action : actions)
        {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.rightMargin = (int) (MARGIN * displayMetrics.density);
            layoutParams.bottomMargin = (int) (MARGIN * displayMetrics.density);
            action.setLayoutParams(layoutParams);

            action.measure(0, 0);
            float buttonWidth = (action.getMeasuredWidth() / displayMetrics.density) + MARGIN;
            totalButtonWidth += buttonWidth;

            if (totalButtonWidth > maxWidth)
            {
                // Add previous row.
                ll.addView(row);

                // Need to add an additional row because we ran out of space.
                row = new LinearLayout(this);
                row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                row.setOrientation(LinearLayout.HORIZONTAL);

                // This will be the first element on a new row.
                totalButtonWidth = buttonWidth;
            }

            row.addView(action);
        }

        // Add final row
        ll.addView(row);
    }

    private void UpdateCharacteristicData(String serviceUuid, String characteristicUuid, byte[] data)
    {
        Constants.Characteristic characteristicConstant = Constants.Characteristics.Get(m_Address, serviceUuid, characteristicUuid);
        Constants.CharacteristicReadType type = (characteristicConstant == null) ? Constants.DEFAULT_CHARACTERISTIC_TYPE : characteristicConstant.ReadType;

        LinearLayout services = findViewById(R.id.service_scroll);
        TextView textView = services.findViewWithTag(serviceUuid).findViewWithTag(characteristicUuid).findViewById(R.id.characteristic_data_label);
        textView.setVisibility(View.VISIBLE);
        textView.setText(data.length + " byte" + (((data.length == 1) ? "" :  "s") + " (" + type + "):"));
        textView = services.findViewWithTag(serviceUuid).findViewWithTag(characteristicUuid).findViewById(R.id.characteristic_data);
        textView.setVisibility(View.VISIBLE);

        String characteristicValue;
        characteristicValue = Util.GetDataString(data, type);

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

    private class CharacteristicActionOnClick implements ViewInputHandler.OnClick
    {
        private String m_ServiceUUID;
        private String m_CharacteristicUUID;
        private boolean m_IsNotifyEnabled;
        private boolean m_IsIndicateEnabled;

        public CharacteristicActionOnClick(String serviceUUID, String characteristicUUID) {
            m_ServiceUUID = serviceUUID;
            m_CharacteristicUUID = characteristicUUID;
            m_IsNotifyEnabled = false;
            m_IsIndicateEnabled = false;
        }

        @Override
        public void Do(View view)
        {
            int action = Integer.parseInt(view.getTag().toString());
            if (action == BluetoothGattCharacteristic.PROPERTY_READ)
            {
                m_Device.ReadCharacteristic(m_ServiceUUID, m_CharacteristicUUID);
            }
            else if (action == BluetoothGattCharacteristic.PROPERTY_WRITE)
            {
                // TODO: only sync time is supported for now - add ability to write custom data
                if (m_ServiceUUID.contains("1805") && m_CharacteristicUUID.contains("2a2b")) {
                    m_Device.WriteCharacteristic(m_ServiceUUID, m_CharacteristicUUID, Util.GetTimeInBytes(System.currentTimeMillis()));
                    m_Device.ReadCharacteristic(m_ServiceUUID, m_CharacteristicUUID);
                }
            }
            else if (action == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
            {
                m_IsNotifyEnabled = !m_IsNotifyEnabled;

                if (m_IsNotifyEnabled)
                {
                    view.setBackgroundResource(R.drawable.select_border_medium_active);
                }
                else
                {
                    view.setBackgroundResource(R.drawable.select_border_medium);
                }

                m_Device.SetCharacteristicNotification(m_ServiceUUID, m_CharacteristicUUID, m_IsNotifyEnabled || m_IsNotifyEnabled);
            }
            else if (action == BluetoothGattCharacteristic.PROPERTY_INDICATE)
            {
                m_IsIndicateEnabled = !m_IsIndicateEnabled;

                if (m_IsIndicateEnabled)
                {
                    view.setBackgroundResource(R.drawable.select_border_medium_active);
                }
                else
                {
                    view.setBackgroundResource(R.drawable.select_border_medium);
                }

                m_Device.SetCharacteristicIndication(m_ServiceUUID, m_CharacteristicUUID, m_IsNotifyEnabled || m_IsIndicateEnabled);
            }
        }
    };

    private View CreateCharacteristicActionButton(String serviceUUID, String characteristicUUID, int action)
    {
        BezButton actionButton = (BezButton) m_LayoutInflater.inflate(R.layout.characteristic_action, null);

        if (action == BluetoothGattCharacteristic.PROPERTY_READ)
        {
            actionButton.setText(R.string.read);
        }
        else if (action == BluetoothGattCharacteristic.PROPERTY_WRITE)
        {
            actionButton.setText(R.string.write);
        }
        else if (action == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
        {
            actionButton.setText(R.string.write_no_response);
        }
        else if (action == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
        {
            actionButton.setText(R.string.notify);
        }
        else if (action == BluetoothGattCharacteristic.PROPERTY_INDICATE)
        {
            actionButton.setText(R.string.indicate);
        }
        else if (action == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)
        {
            actionButton.setText(R.string.signed_write);
        }
        else if (action == BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS)
        {
            actionButton.setText(R.string.extended_props);
        }

        actionButton.setTag(action);
        actionButton.SetOnClick(new CharacteristicActionOnClick(serviceUUID, characteristicUUID));

        return actionButton;
    }
}
