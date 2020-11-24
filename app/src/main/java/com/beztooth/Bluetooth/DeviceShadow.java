package com.beztooth.Bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DeviceShadow
{
    private LinkedList<BluetoothService> m_Services;
    private Lock m_DeviceShadowLock;

    public DeviceShadow()
    {
        m_Services = new LinkedList<>();
        m_DeviceShadowLock = new ReentrantLock();
    }

    public void SetServices(List<BluetoothGattService> services)
    {
        m_DeviceShadowLock.lock();

        Clear();

        for (BluetoothGattService s : services)
        {
            BluetoothService service = new BluetoothService(s.getUuid().toString());

            for (BluetoothGattCharacteristic c : s.getCharacteristics())
            {
                BluetoothCharacteristic characteristic = new BluetoothCharacteristic(c.getUuid().toString(), c.getProperties());

                for (BluetoothGattDescriptor d : c.getDescriptors())
                {
                    characteristic.AddDescriptor(new BluetoothDescriptor(d.getUuid().toString(), d.getPermissions()));
                }

                service.AddCharacteristic(characteristic);
            }

            m_Services.add(service);
        }

        m_DeviceShadowLock.unlock();
    }

    public LinkedList<BluetoothService> GetServices()
    {
        LinkedList<BluetoothService> result = new LinkedList<>();

        m_DeviceShadowLock.lock();

        for (BluetoothService s : m_Services)
        {
            result.add(s.Clone());
        }

        m_DeviceShadowLock.unlock();

        return result;
    }

    public void UpdateCharacteristic(String serviceUUID, String characteristicUUID, byte[] data)
    {
        m_DeviceShadowLock.lock();

        try
        {
            BluetoothCharacteristic c = GetCharacteristic(serviceUUID, characteristicUUID);
            if (c == null) return;
            c.SetData(data);
        }
        finally
        {
            m_DeviceShadowLock.unlock();
        }
    }

    public BluetoothCharacteristic GetCharacteristic(String serviceUUID, String characteristicUUID)
    {
        for (BluetoothService s : m_Services)
        {
            if (s.GetUUID() == serviceUUID) {
                for (BluetoothCharacteristic c : s.GetCharacteristics())
                {
                    if (c.GetUUID() == characteristicUUID) {
                        return c;
                    }
                }

                return null;
            }
        }

        return null;
    }

    private void Clear()
    {
        for (BluetoothService s : m_Services)
        {
            s.Clear();
        }

        m_Services.clear();
    }
}
