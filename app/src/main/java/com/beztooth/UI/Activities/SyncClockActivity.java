package com.beztooth.UI.Activities;

import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.UI.Util.BezButton;
import com.beztooth.UI.Util.DeviceSelectView;
import com.beztooth.UI.Util.ViewInputHandler;
import com.beztooth.Util.Constants;
import com.beztooth.Util.Util;

import java.util.Calendar;
import java.util.Locale;

public class SyncClockActivity extends BluetoothActivity
{
    private static final String TAG = "SyncClockActivity";
    private static final String CLOCK_DEVICE_PREFIX = "Clock";

    private ProgressBar m_ScanProgress;
    private DeviceSelectView m_DeviceSelectView;

    private BroadcastReceiver m_BroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(ConnectionManager.ON_SCAN_STARTED))
            {
                m_ScanProgress.setVisibility(View.VISIBLE);
            }
            else if (action.equals(ConnectionManager.ON_SCAN_STOPPED))
            {
                m_ScanProgress.setVisibility(View.GONE);
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_SCANNED))
            {
                AddDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
            }
            else if (action.equals(ConnectionManager.ON_SERVICES_DISCOVERED))
            {
                final ConnectionManager.Device device = m_ConnectionManager.GetDevice(intent.getStringExtra(ConnectionManager.ADDRESS));
                if (device == null) return;

                boolean hasTimeCharacteristic = device.HasCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID());
                boolean hasAlarmCharacteristic = device.HasCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_REFERENCE_TIME.GetFullUUID());

                String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                m_DeviceSelectView.SetDeviceSelectState(address, true);

                if (!hasTimeCharacteristic && !hasAlarmCharacteristic) return;

                View extra = m_LayoutInflater.inflate(R.layout.clock_extra, null);

                if (hasTimeCharacteristic)
                {
                    BezButton syncButton = extra.findViewById(R.id.sync_clock_action);
                    syncButton.SetOnClick(new ViewInputHandler.OnClick()
                    {
                        @Override
                        public void Do(View view)
                        {
                            String address = ((View) (view.getParent().getParent().getParent())).getTag().toString();
                            ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                            device.WriteCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID(), Util.GetTimeInBytes(System.currentTimeMillis()));
                            //device.ReadCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID());
                        }
                    });
                    syncButton.setVisibility(View.VISIBLE);

                    TextView label = extra.findViewById(R.id.time_label);
                    label.setVisibility(View.VISIBLE);
                    device.ReadCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID());
                }

                if (hasAlarmCharacteristic)
                {
                    BezButton setAlarmButton = extra.findViewById(R.id.set_alarm_action);
                    setAlarmButton.SetOnClick(new ViewInputHandler.OnClick()
                    {
                        final TimePickerDialog.OnTimeSetListener OnTimeSelected = new TimePickerDialog.OnTimeSetListener()
                        {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hour, int minute)
                            {
                                String alarmTime = String.format(Locale.getDefault(), "%02d%02d", hour, minute);

                                byte[] data = new byte[4];
                                for (int i = 0; i < alarmTime.length(); i++) {
                                    data[i] = (byte)Integer.parseInt(String.valueOf(alarmTime.charAt(i)));
                                }

                                device.WriteCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_REFERENCE_TIME.GetFullUUID(), data);
                                device.ReadCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_REFERENCE_TIME.GetFullUUID());

                                /*
                                Calendar cal = Calendar.getInstance();
                                boolean isAlarmTomorrow = cal.get(Calendar.HOUR_OF_DAY) > hour && cal.get(Calendar.MINUTE) > minute;
                                Calendar alarmTime = new GregorianCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), hour, minute);
                                if (isAlarmTomorrow) alarmTime.add(Calendar.DATE, 1);
                                device.WriteCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID(), Util.GetTimeInBytes((alarmTime.getTimeInMillis())));
                                device.ReadCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID());
                                */
                            }
                        };

                        @Override
                        public void Do(View view)
                        {
                            Calendar cal = Calendar.getInstance();
                            TimePickerDialog tpd = new TimePickerDialog(
                                    m_Activity,
                                    R.style.AppTheme_TimePicker_Dialog,
                                    OnTimeSelected,
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false);
                            tpd.show();
                        }
                    });
                    setAlarmButton.setVisibility(View.VISIBLE);

                    TextView label = extra.findViewById(R.id.alarm_label);
                    label.setVisibility(View.VISIBLE);
                    device.ReadCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_REFERENCE_TIME.GetFullUUID());
                }

                m_DeviceSelectView.SetExtra(address, extra);
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_CONNECTED))
            {
                m_DeviceSelectView.OnDeviceConnectionStatusChanged(intent.getStringExtra(ConnectionManager.ADDRESS), true, false);
            }
            else if (action.equals(ConnectionManager.ON_DEVICE_DISCONNECTED))
            {
                String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                m_DeviceSelectView.OnDeviceConnectionStatusChanged(address, false, intent.getBooleanExtra(ConnectionManager.DATA, false));
                m_DeviceSelectView.SetDeviceSelectState(address, true);
            }
            else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_WRITE))
            {
                if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID()))
                {
                    String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                    TextView timeLabel = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.time_label);
                    timeLabel.setText(Util.GetDataString(intent.getByteArrayExtra(ConnectionManager.DATA), Constants.CharacteristicReadType.TIME));
                }
                /*
                if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(Constants.CHARACTERISTIC_REFERENCE_TIME.GetFullUUID()))
                {
                    String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                    TextView alarmLabel = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.alarm_label);
                    alarmLabel.setText(Util.GetDataString(intent.getByteArrayExtra(ConnectionManager.DATA), Constants.CharacteristicReadType.TIME));
                }
                */
            }
            else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_READ))
            {
                if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID()))
                {
                    String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                    TextView timeLabel = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.time_label);
                    timeLabel.setText(Util.GetDataString(intent.getByteArrayExtra(ConnectionManager.DATA), Constants.CharacteristicReadType.TIME));
                }
                if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(Constants.CHARACTERISTIC_REFERENCE_TIME.GetFullUUID()))
                {
                    String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                    TextView alarmLabel = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.alarm_label);
                    byte[] data = intent.getByteArrayExtra(ConnectionManager.DATA);
                    if (data.length != 4) return;

                    if (data[0] == 2 && data[1] == 4 && data[2] == 0 && data[3] == 0) {
                        alarmLabel.setText("NOT SET");
                    }
                    else
                    {
                        alarmLabel.setText(String.valueOf(data[0]) + String.valueOf(data[1]) + ":" + String.valueOf(data[2]) + String.valueOf(data[3]));
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_clock);

        m_DeviceSelectView = new DeviceSelectView(getApplicationContext(), (LinearLayout)findViewById(R.id.device_scroll));

        AddEventListeners();

        m_ScanProgress = findViewById(R.id.scanProgress);
        m_ScanProgress.setVisibility(View.GONE);
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
        if (m_ConnectionManager.IsScanning())
        {
            for (String mac : m_ConnectionManager.GetScannedDevices())
            {
                AddDevice(mac);
            }
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionManager.ON_SCAN_STARTED);
        intentFilter.addAction(ConnectionManager.ON_SCAN_STOPPED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_SCANNED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_CONNECTED);
        intentFilter.addAction(ConnectionManager.ON_DEVICE_DISCONNECTED);
        intentFilter.addAction(ConnectionManager.ON_SERVICES_DISCOVERED);
        intentFilter.addAction(ConnectionManager.ON_CHARACTERISTIC_WRITE);
        intentFilter.addAction(ConnectionManager.ON_CHARACTERISTIC_READ);
        LocalBroadcastManager.getInstance(this).registerReceiver(m_BroadcastReceiver, intentFilter);

        Scan();
    }

    private void AddEventListeners()
    {
        // Scan onClick
        Button button = findViewById(R.id.toolbarScanButton);
        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                Scan();
            }
        });
    }

    private void Scan()
    {
        if (!m_ConnectionManager.IsScanning())
        {
            // Fresh scan, clear devices
            m_DeviceSelectView.ClearDevices();
        }

        m_ScanProgress.setVisibility(View.VISIBLE);

        m_ConnectionManager.Scan(false);
    }

    private void AddDevice(String address)
    {
        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        if (!device.GetName().startsWith(CLOCK_DEVICE_PREFIX)) return;

        m_DeviceSelectView.AddDevice(device.GetName(), device.GetAddress(), new ViewInputHandler.OnClick()
        {
            @Override
            public void Do(View view)
            {
                String address = view.getTag().toString();

                m_DeviceSelectView.SetDeviceSelectState(address, false);

                ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
                if (device.IsConnected()) return;
                device.SetReadCharacteristicsWhenDiscovered(false);
                device.Connect();
            }
        });

        // If device is already connected, we can't sync.
        if (device.IsConnected())
        {
            m_DeviceSelectView.OnDeviceConnectionStatusChanged(address, true, false);
            m_DeviceSelectView.SetDeviceSelectState(address, false);
        }
    }
}
