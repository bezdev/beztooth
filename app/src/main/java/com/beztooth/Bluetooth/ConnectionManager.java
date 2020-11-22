package com.beztooth.Bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.beztooth.Util.*;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

public class ConnectionManager extends Service
{
    // Broadcast Events
    public final static String ON_BLUETOOTH_DISABLED = "beztooth.Device.ON_BLUETOOTH_DISABLED";
    public final static String ON_SCAN_STARTED = "beztooth.Device.ON_SCAN_STARTED";
    public final static String ON_SCAN_STOPPED = "beztooth.Device.ON_SCAN_STOPPED";
    public final static String ON_DEVICE_SCANNED = "beztooth.ON_DEVICE_SCANNED";
    public final static String ON_DEVICE_CONNECTED = "beztooth.Device.ON_DEVICE_CONNECTED";
    public final static String ON_DEVICE_DISCONNECTED = "beztooth.Device.ON_DEVICE_DISCONNECTED";
    public final static String ON_SERVICES_DISCOVERED = "beztooth.Device.ON_SERVICES_DISCOVERED";
    public final static String ON_CHARACTERISTIC_READ = "beztooth.Device.ON_CHARACTERISTIC_READ";
    public final static String ON_CHARACTERISTIC_WRITE = "beztooth.Device.ON_CHARACTERISTIC_WRITE";
    public final static String ON_DESCRIPTOR_READ = "beztooth.Device.ON_DESCRIPTOR_READ";

    // Broadcast Data
    public final static String ADDRESS = "beztooth.Device.ADDRESS";
    public final static String NAME = "beztooth.Device.NAME";
    public final static String SERVICE = "beztooth.Device.SERVICE";
    public final static String CHARACTERISTIC = "beztooth.Device.CHARACTERISTIC";
    public final static String DATA = "beztooth.Device.DATA";
    public final static String TIME = "beztooth.Device.TIME";

    private static final String TAG = "ConnectionManager";

    private static final int SCAN_TIMEOUT = 10000;
    private static final int CONNECT_DEVICE_TIMEOUT = 10000;
    private static final int GATT_TIMEOUT = 5000;
    private static final boolean AUTO_READ_DESCRIPTOR = false;

    private Handler m_ScanHandler;
    private BluetoothAdapter m_BluetoothAdapter;
    private BluetoothLeScanner m_BluetoothLeScanner;

    private HashMap<String, Device> m_Devices = new HashMap<>();
    private LinkedList<String> m_ScannedDevices = new LinkedList<>();

    private boolean m_IsInitialized;
    private boolean m_IsScanning;

    private Context m_Context;

    private enum GattActionType
    {
        None,
        Connect,
        Disconnect,
        Close,
        DiscoverServices,
        ReadCharacteristic,
        WriteCharacteristic,
        ReadDescriptor,
        WriteDescriptor
    };

    // Used to queue Gatt actions since a Device will only handle one request at a time.
    private interface IGattAction
    {
        void Do();
        void Done();
        void OnTimeout();
    }

    // Bluetooth Device.  Contains metadata about the device, connection state, and underlying
    // BluetoothDevice and BluetoothGatt references.  Handles BluetoothGattCallback callbacks:
    // https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback
    public class Device
    {
        private static final String TAG = "Device";

        private String m_Name;
        private String m_Address;
        private BluetoothDevice m_Device;
        private BluetoothGatt m_Gatt;
        private int m_ConnectionState;
        private boolean m_IsConnecting;
        private boolean m_DiscoverServicesWhenConnected;
        private boolean m_ReadCharacteristicsWhenDiscovered;
        private Handler m_GattTimeoutHandler;

        private long m_ConnectTime;
        private long m_OnConnectedTime;

        // Queue of GattActions.  A device can only handle one request at a time, but from the UI
        // we might fire off several actions at once.  Need to dequeue after performing every action.
        private Lock m_GattActionQueueLock;
        private LinkedList<GattAction> m_GattActionQueue;

        private class GattAction implements IGattAction
        {
            protected int m_Timeout;
            protected GattActionType m_Type;
            private final Runnable m_RunDequeueGattAction;

            GattAction(GattActionType type)
            {
                m_Type = type;
                m_Timeout = GATT_TIMEOUT;

                m_RunDequeueGattAction= new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log("Timeout: " + Util.GetCallerName() + " " + m_Type);
                        GattAction removed = DequeueGattAction(m_Type);
                        if (removed != null) Log("Gatt timeout: " + GetGattAction(removed.getClass().toString()));
                        OnTimeout();
                    }
                };
            }

            @Override
            public void Do()
            {
                Log("GattAction.Do: " + Util.GetCallerName());
                m_GattTimeoutHandler.postDelayed(m_RunDequeueGattAction, m_Timeout);
            }

            @Override
            public void Done()
            {
                m_GattTimeoutHandler.removeCallbacks(m_RunDequeueGattAction);
            }

            @Override
            public void OnTimeout()
            {

            }

            public GattActionType GetType()
            {
                return m_Type;
            }
        }

        private class GattDiscoverServices extends GattAction
        {
            public GattDiscoverServices()
            {
                super(GattActionType.DiscoverServices);
            }

            @Override
            public void Do()
            {
                super.Do();
                m_Gatt.discoverServices();
            }
        }

        private class GattReadCharacteristic extends GattAction
        {
            private BluetoothGattCharacteristic m_Characteristic;

            public GattReadCharacteristic(BluetoothGattCharacteristic characteristic)
            {
                super(GattActionType.ReadCharacteristic);
                m_Characteristic = characteristic;
            }

            @Override
            public void Do()
            {
                super.Do();
                m_Gatt.readCharacteristic(m_Characteristic);
            }
        }

        private class GattWriteCharacteristic extends GattAction
        {
            private BluetoothGattCharacteristic m_Characteristic;

            public GattWriteCharacteristic(BluetoothGattCharacteristic characteristic)
            {
                super(GattActionType.WriteCharacteristic);
                m_Characteristic = characteristic;
            }

            @Override
            public void Do()
            {
                super.Do();
                m_Gatt.writeCharacteristic(m_Characteristic);
            }
        }

        private class GattConnect extends GattAction
        {
            public GattConnect()
            {
                super(GattActionType.Connect);
                m_Timeout = CONNECT_DEVICE_TIMEOUT;
            }

            @Override
            public void Do()
            {
                super.Do();

                if (IsConnected() || IsConnecting()) return;

                m_ConnectTime = System.currentTimeMillis();
                m_Gatt = m_Device.connectGatt(m_Context, true, c_BluetoothGattCallback);
                m_IsConnecting = true;
            }

            @Override
            public void OnTimeout()
            {
                // If connect timeout, broadcast that we disconnected.
                BroadcastOnDeviceDisconnected(m_Address, true);
            }
        }

        private class GattDisconnect extends GattAction
        {
            public GattDisconnect()
            {
                super(GattActionType.Disconnect);
            }

            @Override
            public void Do()
            {
                super.Do();
                m_Gatt.disconnect();
                QueueGattAction(new GattClose());
            }
        }

        private class GattClose extends GattAction
        {
            public GattClose()
            {
                super(GattActionType.Close);
            }

            @Override
            public void Do()
            {
                super.Do();
                m_Gatt.close();
            }
        }

        private class GattReadDescriptor extends GattAction
        {
            private BluetoothGattDescriptor m_Descriptor;

            public GattReadDescriptor(BluetoothGattDescriptor descriptor)
            {
                super(GattActionType.ReadDescriptor);
                m_Descriptor = descriptor;
            }

            @Override
            public void Do()
            {
                super.Do();
                m_Gatt.readDescriptor(m_Descriptor);
            }
        }

        private class GattWriteDescriptor extends GattAction
        {
            private BluetoothGattDescriptor m_Descriptor;

            public GattWriteDescriptor(BluetoothGattDescriptor descriptor)
            {
                super(GattActionType.WriteDescriptor);
                m_Descriptor = descriptor;
            }

            @Override
            public void Do()
            {
                super.Do();
                m_Gatt.writeDescriptor(m_Descriptor);
            }
        }

        // Device callbacks.
        private final BluetoothGattCallback c_BluetoothGattCallback = new BluetoothGattCallback()
        {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                m_IsConnecting = false;

                if (newState == STATE_CONNECTED)
                {
                    m_OnConnectedTime = System.currentTimeMillis();
                    m_ConnectionState = STATE_CONNECTED;
                    long connectTime = m_OnConnectedTime - m_ConnectTime;
                    Log("Connected in " + connectTime + "ms");
                    BroadcastOnDeviceConnected(m_Address);

                    if (m_DiscoverServicesWhenConnected)
                    {
                        DiscoverServices();
                    }

                    DequeueGattAction(GattActionType.Connect);
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    Log("Disconnected");
                    m_ConnectionState = STATE_DISCONNECTED;
                    BroadcastOnDeviceDisconnected(m_Address, false);

                    ClearGattActionQueue();

                    DequeueGattAction(GattActionType.Disconnect);
                }
                else if (newState == BluetoothProfile.STATE_CONNECTING)
                {
                    m_IsConnecting = true;
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTING)
                {

                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                m_Gatt = gatt;

                Log("OnServicesDiscovered (" + gatt.getServices().size() + "): " + m_Address);

                for (BluetoothGattService s : gatt.getServices())
                {
                    Log("Service: " + s.getUuid());
                    for (BluetoothGattCharacteristic c : s.getCharacteristics())
                    {
                        Log(" Characteristic (properties: " + c.getProperties() + "): " + c.getUuid());

                        if (m_ReadCharacteristicsWhenDiscovered)
                        {
                            if (AUTO_READ_DESCRIPTOR)
                            {
                                for (BluetoothGattDescriptor d : c.getDescriptors())
                                {
                                    if ((d.getPermissions() & BluetoothGattDescriptor.PERMISSION_READ) == BluetoothGattDescriptor.PERMISSION_READ)
                                    {
                                        ReadDescriptor(d);
                                    }
                                }
                            }

                            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ)
                            {
                                ReadCharacteristic(c);
                            }
                        }
                    }
                }

                BroadcastOnServicesDiscovered();

                DequeueGattAction(GattActionType.DiscoverServices);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                m_Gatt = gatt;

                Log("onCharacteristicRead: " + characteristic.getUuid() + " - " + (characteristic.getValue() == null ? "null" : (characteristic.getValue().length + " byte(s)")));

                BroadcastOnCharacteristicRead(
                    characteristic.getService().getUuid().toString(),
                    characteristic.getUuid().toString(),
                    characteristic.getValue());

                DequeueGattAction(GattActionType.ReadCharacteristic);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                m_Gatt = gatt;

                Log("onCharacteristicWrite");

                BroadcastOnCharacteristicWrite(
                        characteristic.getService().getUuid().toString(),
                        characteristic.getUuid().toString(),
                        characteristic.getValue());

                DequeueGattAction(GattActionType.WriteCharacteristic);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
            {
                m_Gatt = gatt;

                Log("onCharacteristicChanged");

                BroadcastOnCharacteristicRead(
                        characteristic.getService().getUuid().toString(),
                        characteristic.getUuid().toString(),
                        characteristic.getValue());

                DequeueGattAction(GattActionType.None);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
            {
                m_Gatt = gatt;

                Log("onDescriptorRead: (c: " + descriptor.getCharacteristic().getUuid().toString() + "): " + descriptor.getValue().length + " byte(s): " + new String(descriptor.getValue()));

                BroadcastOnDescriptorRead(
                        descriptor.getCharacteristic().getService().getUuid().toString(),
                        descriptor.getCharacteristic().getUuid().toString(),
                        descriptor.getValue());

                DequeueGattAction(GattActionType.ReadDescriptor);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
            {
                Log("onDescriptorWrite");

                m_Gatt = gatt;

                DequeueGattAction(GattActionType.WriteDescriptor);
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status)
            {
                Log("onMtuChanged");

                m_Gatt = gatt;

                DequeueGattAction(GattActionType.None);
            }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status)
            {
                Log("onPhyRead");

                m_Gatt = gatt;

                DequeueGattAction(GattActionType.None);
            }

            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status)
            {
                Log("onPhyUpdate");

                m_Gatt = gatt;

                DequeueGattAction(GattActionType.None);
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
            {
                Log("onReadRemoteRssi");

                m_Gatt = gatt;

                DequeueGattAction(GattActionType.None);
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status)
            {
                Log("onReliableWriteCompleted");

                m_Gatt = gatt;

                DequeueGattAction(GattActionType.None);
            }
        };

        private Device() { }

        public Device(BluetoothDevice bluetoothDevice)
        {
            m_ConnectionState = STATE_DISCONNECTED;
            m_IsConnecting = false;

            m_Device = bluetoothDevice;
            m_Name = (bluetoothDevice.getName() == null) ? bluetoothDevice.getAddress() : bluetoothDevice.getName();
            m_Address = bluetoothDevice.getAddress();
            m_GattTimeoutHandler = new Handler();

            m_DiscoverServicesWhenConnected = true;
            m_ReadCharacteristicsWhenDiscovered = true;

            m_GattActionQueueLock = new ReentrantLock();
            m_GattActionQueue = new LinkedList<>();
        }

        public void Connect()
        {
            if (IsConnected() || IsConnecting()) return;

            QueueGattAction(new GattConnect());
        }

        public void Disconnect()
        {
            if (!IsConnected()) return;

            QueueGattAction(new GattDisconnect());
        }

        public void DiscoverServices()
        {
            if (!IsConnected()) return;

            QueueGattAction(new GattDiscoverServices());
        }

        public void ReadCharacteristic(BluetoothGattCharacteristic characteristic)
        {
            if (!IsConnected()) return;

            if (characteristic == null) return;

            QueueGattAction(new GattReadCharacteristic(characteristic));
        }

        public void WriteCharacteristic(BluetoothGattCharacteristic characteristic)
        {
            if (!IsConnected()) return;

            if (characteristic == null) return;

            QueueGattAction(new GattWriteCharacteristic(characteristic));
        }

        public void ReadDescriptor(BluetoothGattDescriptor descriptor)
        {
            if (!IsConnected()) return;

            QueueGattAction(new GattReadDescriptor(descriptor));
        }

        public void WriteDescriptor(BluetoothGattDescriptor descriptor)
        {
            if (!IsConnected()) return;

            QueueGattAction(new GattWriteDescriptor(descriptor));
        }

        private BluetoothGattService GetService(String service)
        {
            if (!IsConnected()) return null;

            return m_Gatt.getService(UUID.fromString(service));
        }

        public BluetoothGattCharacteristic GetCharacteristic(String serviceUUID, String characteristicUUID)
        {
            if (!IsConnected()) return null;

            BluetoothGattService s = GetService(serviceUUID);
            if (s == null) return null;
            return s.getCharacteristic(UUID.fromString(characteristicUUID));
        }

        public int GetCharacteristicProperties(String serviceUUID, String characteristicUUID)
        {
            BluetoothGattCharacteristic c = GetCharacteristic(serviceUUID, characteristicUUID);
            if (c == null) return 0;

            return c.getProperties();
        }

        public void SetCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable)
        {
            if (!IsConnected()) return;
            if (characteristic == null) return;
            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != BluetoothGattCharacteristic.PROPERTY_NOTIFY) return;

            // Get Client Characteristic Configuration descriptor
            BluetoothGattDescriptor d = characteristic.getDescriptor(UUID.fromString(Constants.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION.GetFullUUID()));
            if (d == null) return;

            // Enable notify on client
            m_Gatt.setCharacteristicNotification(characteristic, enable);

            // Enable notify on server
            d.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[] { 0x00, 0x00 });
            WriteDescriptor(d);
        }

        public void SetCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enable)
        {
            if (!IsConnected()) return;
            if (characteristic == null) return;
            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != BluetoothGattCharacteristic.PROPERTY_INDICATE) return;

            // Get Client Characteristic Configuration descriptor
            BluetoothGattDescriptor d = characteristic.getDescriptor(UUID.fromString(Constants.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION.GetFullUUID()));
            if (d == null) return;

            // Enable notify on client
            m_Gatt.setCharacteristicNotification(characteristic, enable);

            // Enable notify on server
            d.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : new byte[] { 0x00, 0x00 });
            WriteDescriptor(d);
        }

        public void SetReadCharacteristicsWhenDiscovered(boolean readCharacteristicsWhenDiscovered)
        {
            m_ReadCharacteristicsWhenDiscovered = readCharacteristicsWhenDiscovered;
        }

        public boolean IsConnected()
        {
            return m_ConnectionState == STATE_CONNECTED;
        }

        public boolean IsConnecting()
        {
            return m_IsConnecting;
        }

        public String GetName()
        {
            return m_Name;
        }

        public String GetAddress()
        {
            return m_Address;
        }

        public BluetoothDevice GetDevice()
        {
            return m_Device;
        }

        public List<BluetoothGattService> GetServices()
        {
            return m_Gatt.getServices();
        }

        public void ClearGattActionQueue()
        {
            m_GattActionQueueLock.lock();

            for (GattAction ga : m_GattActionQueue)
            {
                ga.Done();
            }

            m_GattActionQueue.clear();

            m_GattActionQueueLock.unlock();
        }

        private void QueueGattAction(GattAction action)
        {
            m_GattActionQueueLock.lock();

            Log("QueueGattAction " + Util.GetCallerName() + " (" + m_GattActionQueue.size() + " items in queue): " + GetGattAction(action.getClass().toString()));

            m_GattActionQueue.add(action);

            // If this is the only queued action, dequeue it immediately.
            if (m_GattActionQueue.size() == 1) {
                m_GattActionQueue.peek().Do();
            }

            m_GattActionQueueLock.unlock();
        }

        private GattAction DequeueGattAction(GattActionType type)
        {
            m_GattActionQueueLock.lock();

            GattAction completed;

            try
            {
                GattAction peek = m_GattActionQueue.peek();

                Log("DequeueGattAction (" + type + ") " + Util.GetCallerName() + " (" + m_GattActionQueue.size() + " items in queue) - completed: " + (m_GattActionQueue.size() == 0 ? "null" : GetGattAction(peek.getClass().toString())));

                // If the queue is empty or the action doesn't match the queue, then a timeout likely happened.
                if (m_GattActionQueue.size() == 0 || peek.GetType() != type)
                {
                    return null;
                }

                completed = m_GattActionQueue.remove();
                completed.Done();

                if (m_GattActionQueue.size() > 0)
                {
                    m_GattActionQueue.peek().Do();
                }
            }
            finally
            {
                m_GattActionQueueLock.unlock();
            }

            return completed;
        }

        private String GetGattAction(String className) {
            return className.substring(className.lastIndexOf('$') + 1);
        }

        private void BroadcastOnServicesDiscovered()
        {
            Intent intent = new Intent(ON_SERVICES_DISCOVERED);
            intent.putExtra(ADDRESS, m_Address);
            LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
        }

        private void BroadcastOnCharacteristicRead(String service, String uuid, byte[] value)
        {
            if (value == null || value.length == 0) return;

            Intent intent = new Intent(ON_CHARACTERISTIC_READ);
            intent.putExtra(ADDRESS, m_Address);
            intent.putExtra(SERVICE, service);
            intent.putExtra(CHARACTERISTIC, uuid);
            intent.putExtra(DATA, value);
            LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
        }

        private void BroadcastOnCharacteristicWrite(String service, String uuid, byte[] value)
        {
            if (value.length == 0) return;

            Intent intent = new Intent(ON_CHARACTERISTIC_WRITE);
            intent.putExtra(ADDRESS, m_Address);
            intent.putExtra(SERVICE, service);
            intent.putExtra(CHARACTERISTIC, uuid);
            intent.putExtra(DATA, value);
            LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
        }

        private void BroadcastOnDescriptorRead(String service, String characteristic, byte[] value)
        {
            if (value.length == 0) return;

            Intent intent = new Intent(ON_DESCRIPTOR_READ);
            intent.putExtra(ADDRESS, m_Address);
            intent.putExtra(SERVICE, service);
            intent.putExtra(CHARACTERISTIC, characteristic);
            intent.putExtra(DATA, value);
            LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
        }

        private void BroadcastOnDeviceConnected(String address)
        {
            Intent intent = new Intent(ON_DEVICE_CONNECTED);
            intent.putExtra(ADDRESS, address);
            LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
        }

        private void BroadcastOnDeviceDisconnected(String address, boolean isTimeout)
        {
            Intent intent = new Intent(ON_DEVICE_DISCONNECTED);
            intent.putExtra(ADDRESS, address);
            intent.putExtra(DATA, isTimeout);
            LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
        }

        private void Log(String message)
        {
            Logger.Debug(TAG + " " + m_Address, message);
        }
    }

    // Get string representation of the properties of a characteristic.
    public static String GetProperties(int properties, String delimiter)
    {
        LinkedList<String> propertiesList = new LinkedList<>();

        if ((properties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) == BluetoothGattCharacteristic.PROPERTY_BROADCAST)
        {
            propertiesList.add("BROADCAST");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ)
        {
            propertiesList.add("READ");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
        {
            propertiesList.add("WRITE NO RESPONSE");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE)
        {
            propertiesList.add("WRITE");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
        {
            propertiesList.add("NOTIFY");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE)
        {
            propertiesList.add("INDICATE");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)
        {
            propertiesList.add("SIGNED WRITE");
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) == BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS)
        {
            propertiesList.add("EXTENDED_PROPS");
        }

        if (propertiesList.size() == 0) return "None";

        return String.join(delimiter, propertiesList);
    }

    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder
    {
        public ConnectionManager getService()
        {
            return ConnectionManager.this;
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        Initialize();

        registerReceiver(m_Receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        unregisterReceiver(m_Receiver);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    // Scan callback.  Called when a device is discovered.
    private ScanCallback m_ScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            if (result.isConnectable())
            {
                BluetoothDevice bluetoothDevice = result.getDevice();

                // Device already found, skip.
                if (m_Devices.containsKey(bluetoothDevice.getAddress())) return;

                Device device = new Device(bluetoothDevice);
                String deviceName = device.GetName();
                String deviceAddress = device.GetAddress();
                m_Devices.put(deviceAddress, device);
                m_ScannedDevices.add(deviceAddress);

                BroadcastOnDeviceScanned(deviceName, deviceAddress);

                Logger.Debug(TAG, "Connectable Device Found: " + deviceAddress + (deviceName != deviceAddress ? " - " + deviceName : ""));
            }
        }
    };

    public boolean Initialize()
    {
        if (!m_IsInitialized)
        {
            m_Context = getApplicationContext();

            final BluetoothManager bluetoothManager = (BluetoothManager) m_Context.getSystemService(Context.BLUETOOTH_SERVICE);
            m_BluetoothAdapter = bluetoothManager.getAdapter();

            if (m_ScanHandler != null)
            {
                // Remove all queued messages
                m_ScanHandler.removeCallbacksAndMessages(null);
            }

            m_ScanHandler = new Handler();
            m_IsScanning = false;
        }

        // Checks if Bluetooth is supported on the device.
        if (m_BluetoothAdapter == null || !m_BluetoothAdapter.isEnabled())
        {
            BroadcastOnBluetoothDisabled();
            m_IsInitialized = false;
            return false;
        }

        if (m_BluetoothLeScanner == null)
        {
            m_BluetoothLeScanner = m_BluetoothAdapter.getBluetoothLeScanner();
            if (m_BluetoothLeScanner == null)
            {
                BroadcastOnBluetoothDisabled();
                m_IsInitialized = false;
                return false;
            }
        }

        m_IsInitialized = true;
        return true;
    }

    public Device GetDevice(String address)
    {
        if (!m_Devices.containsKey(address)) return null;

        return m_Devices.get(address);
    }

    // Scan for Bluetooth LE devices.
    public void Scan(boolean stopScan)
    {
        if (!Initialize()) return;

        if (stopScan) StopScan();

        // Fresh scan, need to remove all devices that aren't connected since a new scan will not
        // show devices that are already connected.
        if (!m_IsScanning)
        {
            LinkedList<String> connectedDevices = new LinkedList<>();

            Iterator<String> it = m_ScannedDevices.iterator();
            while (it.hasNext())
            {
                String address = it.next();
                Device device = m_Devices.get(address);
                if (!device.IsConnected())
                {
                    m_Devices.remove(address);
                    it.remove();
                }
                else
                {
                    // Broadcast already connected devices so UI knows to display them.
                    BroadcastOnDeviceScanned(device.GetName(), device.GetAddress());

                    connectedDevices.add(device.GetAddress());
                }
            }

            m_ScannedDevices = connectedDevices;

            // Start scan
            LinkedList<ScanFilter> filters = new LinkedList<>();
            ScanSettings settings = (new ScanSettings.Builder().setCallbackType(CALLBACK_TYPE_ALL_MATCHES).setScanMode(SCAN_MODE_LOW_LATENCY).build());
            m_BluetoothLeScanner.startScan(filters, settings, m_ScanCallback);

            BroadcastScanStarted();
        }

        // Remove all queued StopScan handlers
        m_ScanHandler.removeCallbacksAndMessages(null);

        // Add new StopScan handler
        m_ScanHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (Initialize() && m_IsScanning)
                {
                    m_BluetoothLeScanner.stopScan(m_ScanCallback);
                }

                m_IsScanning = false;

                BroadcastScanStopped();
            }
        }, SCAN_TIMEOUT);

        m_IsScanning = true;
    }

    private void StopScan()
    {
        if (!Initialize() || !m_IsScanning) return;

        // Remove all queued StopScan handlers
        m_ScanHandler.removeCallbacksAndMessages(null);

        m_BluetoothLeScanner.stopScan(m_ScanCallback);

        m_IsScanning = false;

        BroadcastScanStopped();
    }

    public boolean IsScanning()
    {
        return m_IsScanning;
    }

    public LinkedList<String> GetScannedDevices()
    {
        return m_ScannedDevices;
    }

    // TODO: remove connect/disconnect
    public void ConnectDevice(Device device)
    {
        if (!Initialize()) return;
        if (device == null) return;
        if (device.IsConnected() || device.IsConnecting()) return;

        Logger.Debug(TAG, "ConnectDevice");
        // Clear gatt queue in case there are some unprocessed events
        device.ClearGattActionQueue();
        device.Connect();
    }

    public void DisconnectDevice(Device device)
    {
        if (!Initialize()) return;
        if (device == null) return;
        if (!device.IsConnected()) return;

        device.Disconnect();
    }

    private final BroadcastReceiver m_Receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                if (!Initialize()) return;

                if (m_BluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF)
                {
                    m_IsInitialized = false;
                    BroadcastOnBluetoothDisabled();
                }
                else if (m_BluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF)
                {
                    m_IsInitialized = false;
                    BroadcastOnBluetoothDisabled();
                }
            }
        }
    };

    private void BroadcastOnBluetoothDisabled()
    {
        Intent intent = new Intent(ON_BLUETOOTH_DISABLED);
        LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
    }

    private void BroadcastScanStarted()
    {
        Intent intent = new Intent(ON_SCAN_STARTED);
        LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
    }

    private void BroadcastScanStopped()
    {
        Intent intent = new Intent(ON_SCAN_STOPPED);
        LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
    }

    private void BroadcastOnDeviceScanned(String name, String address)
    {
        Intent intent = new Intent(ON_DEVICE_SCANNED);
        intent.putExtra(NAME, name);
        intent.putExtra(ADDRESS, address);
        LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
    }
}
