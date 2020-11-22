package com.beztooth.Bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.LinkedList;
import java.util.List;

public class DeviceShadow
{
    private String Address;
    private LinkedList<Service> m_Services = new LinkedList<>();
    private LinkedList<Service.Characteristic> m_Characteristics = new LinkedList<>();

    public DeviceShadow(String mac)
    {
        Address = mac;
    }

    public void Clear()
    {
        for (Service s : m_Services)
        {
            s.Clear();
        }

        m_Services.clear();
        m_Characteristics.clear();
    }

    public static class Service
    {
        public String UUID;

        private LinkedList<Service.Characteristic> m_Characteristics;

        public Service(String uuid)
        {
            UUID = uuid;
        }

        public void AddCharacteristic(Characteristic characteristic)
        {
            m_Characteristics.add(characteristic);
        }

        public void Clear()
        {
            for (Service.Characteristic c : m_Characteristics)
            {
                c.Clear();
            }

            m_Characteristics.clear();
        }

        public static class Characteristic
        {
            public String UUID;
            public int Properties;
            public byte[] Value;

            public Characteristic(String uuid, int properties)
            {
                UUID = uuid;
                Properties = properties;
                Value = new byte[0];
            }

            public void Clear()
            {
                UUID = "";
                Properties = 0;
                Value = new byte[0];
            }
        }
    }

    public void Set(List<BluetoothGattService> services)
    {
        Clear();

        for (BluetoothGattService s : services)
        {
            Service service = new Service(s.getUuid().toString());

            for (BluetoothGattCharacteristic c : s.getCharacteristics())
            {
                Service.Characteristic characteristic = new Service.Characteristic(c.getUuid().toString(), c.getProperties());
                service.AddCharacteristic(characteristic);
                m_Characteristics.add(characteristic);
            }

            m_Services.add(service);
        }
    }
}
