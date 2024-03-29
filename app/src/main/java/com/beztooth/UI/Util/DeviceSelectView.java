package com.beztooth.UI.Util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beztooth.R;

import java.util.HashMap;

public class DeviceSelectView
{
    private Context m_Context;
    private LinearLayout m_Root;
    private LayoutInflater m_LayoutInflater;
    private HashMap<String, View> m_DeviceSelectViews;
    private boolean m_IsAppend;
    private boolean m_ReplaceIfExists;

    public DeviceSelectView(Context context, LinearLayout root)
    {
        Initialize(context, root, true, false);
    }

    public DeviceSelectView(Context context, LinearLayout root, boolean isAppend)
    {
        Initialize(context, root, isAppend, false);
    }

    public DeviceSelectView(Context context, LinearLayout root, boolean isAppend, boolean replaceIfExists)
    {
        Initialize(context, root, isAppend, replaceIfExists);
    }

    private void Initialize(Context context, LinearLayout root, boolean isAppend, boolean replaceIfExists)
    {
        m_Context = context;
        m_Root = root;
        m_LayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        m_DeviceSelectViews = new HashMap<>();
        m_IsAppend = isAppend;
        m_ReplaceIfExists = replaceIfExists;
    }

    public View GetRoot()
    {
        return m_Root;
    }

    public void AddDevice(String name, String address, ViewInputHandler.OnClick onClick)
    {
        View view = m_Root.findViewWithTag(address);
        boolean deviceExists = view != null;

        // Check if device is already visible
        if (deviceExists && !m_ReplaceIfExists)
        {
            return;
        }

        if (!deviceExists)
        {
            view = m_LayoutInflater.inflate(R.layout.device_select, null);
        }

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
        if (onClick != null) container.SetOnClick(onClick);

        if (!deviceExists || !m_ReplaceIfExists) {
            m_DeviceSelectViews.put(address, container);
            if (m_IsAppend) {
                m_Root.addView(view);
            } else {
                m_Root.addView(view, 0);
            }
        }
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

    public void SetDeviceSelectState(String address, boolean isActive)
    {
        if (!m_DeviceSelectViews.containsKey(address)) return;

        View view = m_DeviceSelectViews.get(address);

        if (isActive)
        {
            view.setAlpha(1.f);
            view.setClickable(true);
        }
        else
        {
            view.setAlpha(.5f);
            view.setClickable(false);
        }
    }

    public void SetExtra(String address, View extraView)
    {
        if (!m_DeviceSelectViews.containsKey(address)) return;

        View view = m_DeviceSelectViews.get(address);
        LinearLayout insertPoint = view.findViewById(R.id.device_select_extra);
        insertPoint.removeAllViews();
        insertPoint.addView(extraView);
    }

    public void ClearDevices()
    {
        if (m_Root != null)
        {
            m_Root.removeAllViews();
            m_DeviceSelectViews.clear();
        }
    }
}
