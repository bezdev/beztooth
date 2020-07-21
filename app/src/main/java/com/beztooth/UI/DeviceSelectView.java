package com.beztooth.UI;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beztooth.R;

public class DeviceSelectView
{
    private Context m_Context;
    private LinearLayout m_Root;
    private LayoutInflater m_LayoutInflater;

    public DeviceSelectView(Context context, LinearLayout root)
    {
        m_Context = context;
        m_Root = root;
        m_LayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void AddDevice(String name, String address, ViewInputHandler.OnClick onClick)
    {
        // If device is already visible, skip.
        if (m_Root.findViewWithTag(address) != null)
        {
            return;
        }

        View view = m_LayoutInflater.inflate(R.layout.device_select, null);

        // Set the device name and address, if there is no name, only display the address.
        TextView textView = view.findViewById(R.id.device_select_name);
        textView.setText(name);
        if (!name.equals(address))
        {
            textView = view.findViewById(R.id.device_select_mac);
            textView.setText(address);
        }

        // Make clickable and set onClick event handler.
        BezContainer container = view.findViewById(R.id.device_select_container);
        container.setTag(address);
        container.SetOnClick(onClick);

        m_Root.addView(view);
    }

    public void OnDeviceConnectionStatusChanged(String address, boolean isConnected, boolean isTimeout)
    {
        View device = m_Root.findViewWithTag(address);
        if (device == null)
        {
            return;
        }

        View isConnectedImage = device.findViewById(R.id.device_select_is_connected);
        View isTimeoutImage = device.findViewById(R.id.device_select_is_timeout);
        if (isConnected)
        {
            isConnectedImage.setVisibility(View.VISIBLE);
            isTimeoutImage.setVisibility(View.GONE);
        }
        else
        {
            isConnectedImage.setVisibility(View.GONE);
            if (isTimeout)
            {
                isTimeoutImage.setVisibility(View.VISIBLE);
            }
        }
    }

    public void SetDeviceStatusMessage(String address, String message)
    {
        View device = m_Root.findViewWithTag(address);
        if (device == null)
        {
            return;
        }

        TextView status = device.findViewById(R.id.device_select_status);

        if (message == null || message.isEmpty())
        {
            status.setVisibility(View.GONE);
        }
        else
        {
            status.setText(message);
            status.setVisibility(View.VISIBLE);
        }
    }

    public void ClearDevices()
    {
        if (m_Root != null)
        {
            m_Root.removeAllViews();
        }
    }
}
