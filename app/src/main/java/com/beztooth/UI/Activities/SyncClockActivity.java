package com.beztooth.UI.Activities;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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

    private static final String CHARACTERISTIC_BRIGHTNESS = "7dfe97ac-69d9-4dd4-8d19-7bfe753f1fee";
    private static final String CHARACTERISTIC_BEEP = "7dfe97ac-69d9-4dd4-8d19-7bfe753f1fed";
    private static final String CHARACTERISTIC_INTERVAL_TIMER = "7dfe97ac-69d9-4dd4-8d19-7bfe753f1fef";

    private static final int BEEP_COUNT_MIN = 1;
    private static final int BEEP_COUNT_MAX = 10;
    private static final int BRIGHTNESS_MIN = 0;
    private static final int BRIGHTNESS_MAX = 20;

    private ProgressBar m_ScanProgress;
    private DeviceSelectView m_DeviceSelectView;
    private View m_BeepsThumb;
    private View m_BrightnessThumb;

    protected void OnBroadcastEvent(Intent intent) {
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

            AddExtra(device);
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
        else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_READ))
        {
            if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID()))
            {
                String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                TextView dateLabel = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.date_label);
                TextView timeLabel = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.time_label);
                String[] timeSplit = Util.GetDataString(intent.getByteArrayExtra(ConnectionManager.DATA), Constants.CharacteristicReadType.TIME).split(" ");
                dateLabel.setText(timeSplit[0]);
                timeLabel.setText(timeSplit[1]);
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
            if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(CHARACTERISTIC_INTERVAL_TIMER))
            {
                String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                TextView intervalTimerLabel = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.interval_timer_label);
                intervalTimerLabel.setText(Util.GetDataString(intent.getByteArrayExtra(ConnectionManager.DATA), Constants.CharacteristicReadType.INTEGER));
                intervalTimerLabel.setVisibility(View.VISIBLE);
            }
            if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(CHARACTERISTIC_BEEP))
            {
                String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                SeekBar seekBar = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.beeps_slider);
                seekBar.setProgress(intent.getByteArrayExtra(ConnectionManager.DATA)[0]);

            }
            if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(CHARACTERISTIC_BRIGHTNESS))
            {
                String address = intent.getStringExtra(ConnectionManager.ADDRESS);
                SeekBar seekBar = m_DeviceSelectView.GetRoot().findViewWithTag(address).findViewById(R.id.brightness_slider);
                seekBar.setProgress(intent.getByteArrayExtra(ConnectionManager.DATA)[0]);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_clock);

        // Initialize views
        m_DeviceSelectView = new DeviceSelectView(getApplicationContext(), (LinearLayout)findViewById(R.id.device_scroll));
        m_BeepsThumb = m_LayoutInflater.inflate(R.layout.seekbar_thumb, null, false);
        m_BrightnessThumb = m_LayoutInflater.inflate(R.layout.seekbar_thumb, null, false);

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
        super.onDestroy();
    }

    @Override
    protected void OnConnectionManagerConnected()
    {
        super.OnConnectionManagerConnected();

        if (m_ConnectionManager.IsScanning())
        {
            for (String mac : m_ConnectionManager.GetScannedDevices())
            {
                AddDevice(mac);
            }
        }

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

        m_ConnectionManager.Scan(false, 30000);
    }

    private void AddDevice(String address)
    {
        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        if (!device.GetName().startsWith(CLOCK_DEVICE_PREFIX)) return;

        m_DeviceSelectView.AddDevice(device.GetName(), device.GetAddress(), null);

        device.Connect();
    }

    private void AddExtra(final ConnectionManager.Device device)
    {
        View extra = m_LayoutInflater.inflate(R.layout.clock_extra, null);
        if (device.HasCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID()))
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
                    device.ReadCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID());
                }
            });
            syncButton.setVisibility(View.VISIBLE);

            LinearLayout label = extra.findViewById(R.id.datetime_layout);
            label.setVisibility(View.VISIBLE);
            device.ReadCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_CURRENT_TIME.GetFullUUID());
        }
        if (device.HasCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), Constants.CHARACTERISTIC_REFERENCE_TIME.GetFullUUID()))
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
        if (device.HasCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), CHARACTERISTIC_BEEP)) {
            SeekBar seekBar = extra.findViewById(R.id.beeps_slider);
            seekBar.setMin(BEEP_COUNT_MIN);
            seekBar.setMax(BEEP_COUNT_MAX);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    SetProgress(getResources(), seekBar, m_BeepsThumb, progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int value = seekBar.getProgress();
                    device.WriteCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), CHARACTERISTIC_BEEP, new byte[] { (byte)value });
                }
            });
            extra.findViewById(R.id.beeps_label).setVisibility(View.VISIBLE);
            extra.findViewById(R.id.beeps_slider).setVisibility(View.VISIBLE);
        }
        if (device.HasCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), CHARACTERISTIC_BRIGHTNESS))
        {
            SeekBar seekBar = extra.findViewById(R.id.brightness_slider);
            seekBar.setMin(BRIGHTNESS_MIN);
            seekBar.setMax(BRIGHTNESS_MAX);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    SetProgress(getResources(), seekBar, m_BrightnessThumb, progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int value = seekBar.getProgress();
                    device.WriteCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), CHARACTERISTIC_BRIGHTNESS, new byte[] { (byte)value });
                }
            });
            extra.findViewById(R.id.brightness_label).setVisibility(View.VISIBLE);
            extra.findViewById(R.id.brightness_slider).setVisibility(View.VISIBLE);
        }
        if (device.HasCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), CHARACTERISTIC_INTERVAL_TIMER))
        {
            // Initialize Interval Timer dialog
            final AlertDialog.Builder intervalTimerDialogBuilder = new AlertDialog.Builder(SyncClockActivity.this);
            intervalTimerDialogBuilder.setTitle("Interval Timer");
            View viewInflated = m_LayoutInflater.inflate(R.layout.interval_timer_dialog, (ViewGroup) findViewById(android.R.id.content), false);
            final EditText intervalSecondsInput = (EditText) viewInflated.findViewById(R.id.seconds);
            intervalTimerDialogBuilder.setView(viewInflated);

            intervalTimerDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    String text = String.valueOf(intervalSecondsInput.getText());
                    if (text.isEmpty()) return;

                    int seconds = Integer.parseInt(text);
                    device.WriteCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), CHARACTERISTIC_INTERVAL_TIMER, Util.GetByteArrayFromInteger(seconds, 4));
                    device.ReadCharacteristic(Constants.SERVICE_CURRENT_TIME.GetFullUUID(), CHARACTERISTIC_INTERVAL_TIMER);
                }
            });
            intervalTimerDialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            final AlertDialog intervalTimerDialog = intervalTimerDialogBuilder.create();

            BezButton button = extra.findViewById(R.id.interval_timer_action);
            button.SetOnClick(new ViewInputHandler.OnClick()
            {
                @Override
                public void Do(View view)
                {
                    intervalSecondsInput.setText("");
                    intervalTimerDialog.show();
                }
            });
            button.setVisibility(View.VISIBLE);
        }

        m_DeviceSelectView.SetExtra(device.GetAddress(), extra);
    }

    private static void SetProgress(Resources resources, SeekBar seekBar, View thumb, int progress) {
        ((TextView)thumb.findViewById(R.id.progress)).setText(String.valueOf(progress));
        thumb.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        Bitmap bitmap = Bitmap.createBitmap(thumb.getMeasuredWidth(), thumb.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        thumb.layout(0, 0, thumb.getMeasuredWidth(), thumb.getMeasuredHeight());
        thumb.draw(canvas);
        seekBar.setThumb(new BitmapDrawable(resources, bitmap));
    }
}
