package com.beztooth.UI.Activities;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.Util.Constants;
import com.beztooth.Util.Util;

// Marquee device has custom service UUID 5ef12a1f-5c32-4d60-b4d7-b312a37d6adc
// with 3 characteristics:
// - message R/W up to 1995 bytes UUID: 5ef12a1f-5c32-4d60-b4d7-b312a37d6add
// - brightness: R/W 1 byte range 0..5 UUID 5ef12a1f-5c32-4d60-b4d7-b312a37d6ade
// - speed: R/W 1 byte range 0..5 UUID 5ef12a1f-5c32-4d60-b4d7-b312a37d6adf
//
// GUI should have:
// - an input text window (restrict typing more that 1995 characters),
// - a button for sending entered text to the device
// - slider for brightness (once value is changed, it should be transmitted)
// - slider for speed (once value is changed, it should be transmitted).
public class MarqueeActivity extends BluetoothActivity
{
    private static final String TAG = "MarqueeActivity";

    private static final String MARQUEE_DEVICE_PREFIX = "Marquee";

    private static final String SERVICE_MARQUEE = "5ef12a1f-5c32-4d60-b4d7-b312a37d6adc";
    private static final String CHARACTERISTIC_MESSAGE = "5ef12a1f-5c32-4d60-b4d7-b312a37d6add";
    private static final String CHARACTERISTIC_BRIGHTNESS = "5ef12a1f-5c32-4d60-b4d7-b312a37d6ade";
    private static final String CHARACTERISTIC_SPEED = "5ef12a1f-5c32-4d60-b4d7-b312a37d6adf";

    private ConnectionManager.Device m_Device;

    private ProgressBar m_ScanProgress;
    private View m_BrightnessThumb;
    private View m_SpeedThumb;

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
        }
        else if (action.equals(ConnectionManager.ON_DEVICE_CONNECTED))
        {
        }
        else if (action.equals(ConnectionManager.ON_DEVICE_DISCONNECTED))
        {
        }
        else if (action.equals(ConnectionManager.ON_CHARACTERISTIC_READ))
        {
            if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(CHARACTERISTIC_MESSAGE))
            {

            }
            else if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(CHARACTERISTIC_BRIGHTNESS))
            {
                SeekBar seekBar = findViewById(R.id.brightness_slider);
                seekBar.setProgress(intent.getByteArrayExtra(ConnectionManager.DATA)[0]);
            }
            else if (intent.getStringExtra(ConnectionManager.CHARACTERISTIC).equalsIgnoreCase(CHARACTERISTIC_SPEED))
            {
                SeekBar seekBar = findViewById(R.id.speed_slider);
                seekBar.setProgress(intent.getByteArrayExtra(ConnectionManager.DATA)[0]);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marquee);

        m_BrightnessThumb = m_LayoutInflater.inflate(R.layout.seekbar_thumb, null, false);
        m_SpeedThumb = m_LayoutInflater.inflate(R.layout.seekbar_thumb, null, false);

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

    private void Scan()
    {
        m_ScanProgress.setVisibility(View.VISIBLE);
        m_ConnectionManager.Scan(false, 30000);
    }

    private void AddDevice(String address)
    {
        ConnectionManager.Device device = m_ConnectionManager.GetDevice(address);
        if (device == null) return;

        if (!device.GetName().startsWith(MARQUEE_DEVICE_PREFIX)) return;

        m_Device = device;
        m_Device.Connect();

        ((SeekBar)findViewById(R.id.brightness_slider)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                SetProgress(getResources(), seekBar, m_BrightnessThumb, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (m_Device == null) return;

                int value = seekBar.getProgress();
                m_Device.WriteCharacteristic(SERVICE_MARQUEE, CHARACTERISTIC_BRIGHTNESS, new byte[] { (byte)value });
            }
        });

        ((SeekBar)findViewById(R.id.speed_slider)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                SetProgress(getResources(), seekBar, m_SpeedThumb, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (m_Device == null) return;

                int value = seekBar.getProgress();
                m_Device.WriteCharacteristic(SERVICE_MARQUEE, CHARACTERISTIC_SPEED, new byte[] { (byte)value });
            }
        });

        Button button = findViewById(R.id.send_button);
        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if (m_Device == null) return;

                EditText editText = findViewById(R.id.message);
                m_Device.WriteCharacteristic(SERVICE_MARQUEE, CHARACTERISTIC_MESSAGE, editText.getText().toString().getBytes());
            }
        });

        m_ScanProgress.setVisibility(View.GONE);
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
