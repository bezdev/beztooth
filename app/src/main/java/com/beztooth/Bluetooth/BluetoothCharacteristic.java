package com.beztooth.Bluetooth;

import java.util.LinkedList;
import java.util.List;

public class BluetoothCharacteristic
{
    private List<BluetoothDescriptor> m_Descriptors;

    private String m_UUID;
    private int m_Properties;
    private byte[] m_Data;

    public BluetoothCharacteristic(String uuid, int properties)
    {
        m_Descriptors = new LinkedList<>();

        m_UUID = uuid;
        m_Properties = properties;
        m_Data = new byte[0];
    }

    public void AddDescriptor(BluetoothDescriptor descriptor)
    {
        m_Descriptors.add(descriptor);
    }

    public void SetData(byte[] data)
    {
        m_Data = data;
    }

    public String GetUUID()
    {
        return m_UUID;
    }

    public int GetProperties()
    {
        return m_Properties;
    }

    public void Clear()
    {
        m_UUID = "";
        m_Properties = 0;
        m_Data = new byte[0];
    }

    public BluetoothCharacteristic Clone()
    {
        BluetoothCharacteristic result = new BluetoothCharacteristic(m_UUID, m_Properties);
        result.SetData(m_Data);
        return result;
    }
}
