package com.beztooth.Bluetooth;

import java.util.LinkedList;

public class BluetoothService
{
    private String m_UUID;
    private LinkedList<BluetoothCharacteristic> m_Characteristics;

    public BluetoothService(String uuid)
    {
        m_Characteristics = new LinkedList<>();
        m_UUID = uuid;
    }

    public void AddCharacteristic(BluetoothCharacteristic characteristic)
    {
        m_Characteristics.add(characteristic);
    }

    public String GetUUID()
    {
        return m_UUID;
    }

    public LinkedList<BluetoothCharacteristic> GetCharacteristics()
    {
        return m_Characteristics;
    }

    public void Clear()
    {
        for (BluetoothCharacteristic c : m_Characteristics)
        {
            c.Clear();
        }

        m_Characteristics.clear();
    }

    public BluetoothService Clone()
    {
        BluetoothService result = new BluetoothService(m_UUID);
        for (BluetoothCharacteristic c : m_Characteristics)
        {
            result.AddCharacteristic(c.Clone());
        }

        return result;
    }
}
