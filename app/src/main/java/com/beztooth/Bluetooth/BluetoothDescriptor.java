package com.beztooth.Bluetooth;

public class BluetoothDescriptor
{
    private String m_UUID;
    private int m_Permissions;

    BluetoothDescriptor(String uuid, int permissions)
    {
        m_UUID = uuid;
        m_Permissions = permissions;
    }
}
