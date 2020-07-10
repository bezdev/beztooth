package com.beztooth;

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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.support.v4.app.ActivityCompat.startActivityForResult;

public class ConnectionManager extends Service
{
    // Broadcast Events
    public final static String ON_BLUETOOTH_DISABLED = "beztooth.Device.ON_BLUETOOTH_DISABLED";
    public final static String ON_DEVICE_SCANNED = "beztooth.ON_DEVICE_SCANNED";
    public final static String ON_DEVICE_CONNECTED = "beztooth.Device.ON_DEVICE_CONNECTED";
    public final static String ON_SERVICES_DISCOVERED = "beztooth.Device.ON_SERVICES_DISCOVERED";
    public final static String ON_CHARACTERISTIC_READ = "beztooth.Device.ON_CHARACTERISTIC_READ";
    public final static String ON_DESCRIPTOR_READ = "beztooth.Device.ON_DESCRIPTOR_READ";

    // Broadcast Data
    public final static String ADDRESS = "beztooth.Device.ADDRESS";
    public final static String NAME = "beztooth.Device.NAME";
    public final static String SERVICE = "beztooth.Device.SERVICE";
    public final static String CHARACTERISTIC = "beztooth.Device.CHARACTERISTIC";
    public final static String DATA = "beztooth.Device.DATA";

    private static final String TAG = "ConnectionManager";

    private static final int SCAN_TIME = 10000;

    private BluetoothAdapter m_BluetoothAdapter;
    private BluetoothLeScanner m_BluetoothLeScanner;

    private HashMap<String, Device> m_Devices = new HashMap<>();
    private HashSet<String> m_ConnectedDevices = new HashSet<>();

    private boolean m_IsInitialized;
    private boolean m_IsScanning;

    private Context m_Context;

    // Used to queue Gatt actions since a Device will only handle one request at a time.
    private interface GattAction
    {
        void Do();
    }

    // Bluetooth Device.  Contains metadata about the device, connection state, and underlying
    // BluetoothDevice and BluetoothGatt references.  Handles BluetoothGattCallback callbacks:
    // https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback
    public class Device
    {
        private static final String TAG = "Device";

        private static final boolean DISCOVER_WHEN_CONNECTED = true;
        private static final boolean READ_WHEN_DISCOVERED = true;

        private String m_Name;
        private String m_Address;
        private BluetoothDevice m_Device;
        private BluetoothGatt m_Gatt;
        private int m_ConnectionState;

        // Queue of GattActions.  A device can only handle one request at a time, but from the UI
        // we might fire off several actions at once.  Need to dequeue after performing every action.
        private LinkedList<GattAction> m_GattActionQueue;

        private class GattDiscoverServices implements GattAction
        {
            public GattDiscoverServices() { }

            @Override
            public void Do()
            {
                m_Gatt.discoverServices();
            }
        }

        private class GattReadCharacteristic implements GattAction
        {
            private BluetoothGattCharacteristic m_Characteristic;

            public GattReadCharacteristic(BluetoothGattCharacteristic characteristic)
            {
                m_Characteristic = characteristic;
            }

            @Override
            public void Do()
            {
                m_Gatt.readCharacteristic(m_Characteristic);
            }
        }

        private class GattWriteCharacteristic implements GattAction
        {
            private BluetoothGattCharacteristic m_Characteristic;

            public GattWriteCharacteristic(BluetoothGattCharacteristic characteristic)
            {
                m_Characteristic = characteristic;
            }

            @Override
            public void Do()
            {
                m_Gatt.writeCharacteristic(m_Characteristic);
            }
        }

        private class GattReadDescriptor implements GattAction
        {
            private BluetoothGattDescriptor m_Descriptor;

            public GattReadDescriptor(BluetoothGattDescriptor descriptor)
            {
                m_Descriptor = descriptor;
            }

            @Override
            public void Do()
            {
                m_Gatt.readDescriptor(m_Descriptor);
            }
        }

        // Device callbacks.
        private final BluetoothGattCallback c_BluetoothGattCallback = new BluetoothGattCallback()
        {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                if (newState == STATE_CONNECTED)
                {
                    m_ConnectionState = STATE_CONNECTED;
                    m_ConnectedDevices.add(m_Address);
                    BroadcastOnDeviceConnected(m_Address);

                    if (DISCOVER_WHEN_CONNECTED)
                    {
                        DiscoverServices();
                    }
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    m_ConnectionState = STATE_DISCONNECTED;
                    m_ConnectedDevices.remove(m_Address);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                m_Gatt = gatt;

                Logger.Debug(TAG, "OnServicesDiscovered: " + m_Address);
                Logger.Debug(TAG, "number of services: " + gatt.getServices().size());

                for (BluetoothGattService s : gatt.getServices())
                {
                    Logger.Debug(TAG, "service uuid: " + s.getUuid());
                    for (BluetoothGattCharacteristic c : s.getCharacteristics())
                    {
                        Logger.Debug(TAG, " characteristic uuid: " + c.getUuid());
                        Logger.Debug(TAG, " properties: " + c.getProperties());

                        if (READ_WHEN_DISCOVERED)
                        {
                            for (BluetoothGattDescriptor d : c.getDescriptors())
                            {
                                if ((d.getPermissions() & BluetoothGattDescriptor.PERMISSION_READ) == BluetoothGattDescriptor.PERMISSION_READ)
                                {
                                    ReadDescriptor(d);
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

                DequeueGattAction();
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                m_Gatt = gatt;

                Logger.Debug(TAG, "onCharacteristicRead: " + characteristic.getUuid());
                Logger.Debug(TAG, characteristic.getValue().length + " byte(s)");

                BroadcastOnCharacteristicRead(
                    characteristic.getService().getUuid().toString(),
                    characteristic.getUuid().toString(),
                    characteristic.getValue());

                DequeueGattAction();
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                m_Gatt = gatt;

                DequeueGattAction();
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                m_Gatt = gatt;

                Logger.Debug(TAG, "onDescriptorRead: (c: " + descriptor.getCharacteristic().getUuid().toString() + "): " + descriptor.getValue().length + " byte(s): " + new String(descriptor.getValue()));

                BroadcastOnDescriptorRead(
                        descriptor.getCharacteristic().getService().getUuid().toString(),
                        descriptor.getCharacteristic().getUuid().toString(),
                        descriptor.getValue());

                DequeueGattAction();
            }
        };

        private Device() { }

        public Device(BluetoothDevice bluetoothDevice)
        {
            m_ConnectionState = STATE_DISCONNECTED;

            m_Device = bluetoothDevice;
            m_Name = (bluetoothDevice.getName() == null) ? bluetoothDevice.getAddress() : bluetoothDevice.getName();
            m_Address = bluetoothDevice.getAddress();

            m_GattActionQueue = new LinkedList<>();
        }

        public void DiscoverServices()
        {
            if (!IsConnected()) return;

            QueueGattAction(new GattDiscoverServices());
        }

        public void ReadCharacteristic(BluetoothGattCharacteristic characteristic)
        {
            if (!IsConnected()) return;

            QueueGattAction(new GattReadCharacteristic(characteristic));
        }

        public void WriteCharacteristic(BluetoothGattCharacteristic characteristic)
        {
            if (!IsConnected()) return;

            QueueGattAction(new GattWriteCharacteristic(characteristic));
        }

        public void ReadDescriptor(BluetoothGattDescriptor descriptor)
        {
            if (!IsConnected()) return;

            QueueGattAction(new GattReadDescriptor(descriptor));
        }

        public BluetoothGattService GetService(String service)
        {
            return m_Gatt.getService(UUID.fromString(service));
        }

        public BluetoothGattCharacteristic GetCharacteristic(String service, String characteristic)
        {
            BluetoothGattService s = GetService(service);
            if (s == null) return null;
            return s.getCharacteristic(UUID.fromString(characteristic));
        }

        public boolean IsConnected()
        {
            return m_ConnectionState == STATE_CONNECTED;
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

        public BluetoothGattCallback GetBluetoothGattCallback()
        {
            return c_BluetoothGattCallback;
        }

        public void SetGatt(BluetoothGatt gatt)
        {
            m_Gatt = gatt;
        }

        private void QueueGattAction(GattAction action)
        {
            m_GattActionQueue.add(action);

            // If this is the only queued action, dequeue it immediately.
            if (m_GattActionQueue.size() == 1) {
                m_GattActionQueue.peek().Do();
            }
        }

        private void DequeueGattAction()
        {
            if (m_GattActionQueue.size() == 0) return;
            m_GattActionQueue.remove();
            if (m_GattActionQueue.size() == 0) return;

            m_GattActionQueue.peek().Do();
        }

        private void BroadcastOnServicesDiscovered()
        {
            Intent intent = new Intent(ON_SERVICES_DISCOVERED);
            intent.putExtra(ADDRESS, m_Address);
            LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
        }

        private void BroadcastOnCharacteristicRead(String service, String uuid, byte[] value)
        {
            if (value.length == 0) return;

            Intent intent = new Intent(ON_CHARACTERISTIC_READ);
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
        ConnectionManager getService()
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
                m_Devices.put(device.GetAddress(), device);

                BroadcastOnDeviceScanned(device.GetAddress());

                Logger.Debug(TAG, "Connectable Device Found: " + device.GetAddress());
                Logger.Debug(TAG, "Device name: " + device.GetName());
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

            m_IsScanning = false;
        }

        // Checks if Bluetooth is supported on the device.
        if (m_BluetoothAdapter == null || !m_BluetoothAdapter.isEnabled())
        {
            BroadcastOnBluetoothDisabled();
            m_IsInitialized = false;
            return false;
        }

        m_BluetoothLeScanner = m_BluetoothAdapter.getBluetoothLeScanner();
        if (m_BluetoothLeScanner == null)
        {
            BroadcastOnBluetoothDisabled();
            m_IsInitialized = false;
            return false;
        }

        m_IsInitialized = true;
        return true;
    }

    public Device GetDevice(String address)
    {
        return m_Devices.get(address);
    }

    public void ScanDevices()
    {
        if (!Initialize()) return;
        if (m_IsScanning) return;

        // Remove all devices that aren't connected.
        HashSet<String> connectedDevices = GetConnectedDevices();
        Iterator it = m_Devices.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry)it.next();
            if (!connectedDevices.contains(pair.getKey().toString()))
            {
                it.remove();
            }
        }

        LinkedList<ScanFilter> filters = new LinkedList<>();
        ScanSettings settings = (new ScanSettings.Builder().setCallbackType(CALLBACK_TYPE_ALL_MATCHES).build());

        m_IsScanning = true;
        m_BluetoothLeScanner.startScan(filters, settings, m_ScanCallback);

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (Initialize())
                {
                    m_BluetoothLeScanner.stopScan(m_ScanCallback);
                }

                m_IsScanning = false;
            }
        }, SCAN_TIME);
    }

    public HashSet<String> GetConnectedDevices()
    {
        return m_ConnectedDevices;
    }

    public HashMap<String, Device> GetDevices()
    {
        return m_Devices;
    }

    public void ConnectDevice(String address)
    {
        ConnectDevice(m_Devices.get(address));
    }

    public void ConnectDevice(Device device)
    {
        if (!Initialize()) return;
        if (device == null) return;
        if (device.IsConnected()) return;

        Logger.Debug(TAG, "ConnectDevice: " + device.GetAddress());
        device.SetGatt(device.GetDevice().connectGatt(m_Context, true, device.GetBluetoothGattCallback()));
    }

    private static byte GetDayCode(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            case Calendar.SATURDAY:
                return 6;
            case Calendar.SUNDAY:
                return 7;
            default:
                return 0;
        }
    }

    public static byte[] GetTimeInBytes(long timestamp) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timestamp);

        byte[] field = new byte[10];

        // Year
        int year = time.get(Calendar.YEAR);
        field[0] = (byte) (year & 0xFF);
        field[1] = (byte) ((year >> 8) & 0xFF);
        // Month
        field[2] = (byte) (time.get(Calendar.MONTH) + 1);
        // Day
        field[3] = (byte) time.get(Calendar.DATE);
        // Hours
        field[4] = (byte) time.get(Calendar.HOUR_OF_DAY);
        // Minutes
        field[5] = (byte) time.get(Calendar.MINUTE);
        // Seconds
        field[6] = (byte) time.get(Calendar.SECOND);
        // Day of Week (1-7)
        field[7] = GetDayCode(time.get(Calendar.DAY_OF_WEEK));
        // Fractions256
        field[8] = (byte) (time.get(Calendar.MILLISECOND) / 256);

        field[9] = 0;

        return field;
    }

    // Get string representation of the byte data, formatted depending on the data type.
    public static String GetDataString(byte[] data, Constants.CharacteristicReadType type)
    {
        if (type == Constants.CharacteristicReadType.STRING)
        {
            return new String(data);
        }
        else if (type == Constants.CharacteristicReadType.HEX)
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++)
            {
                sb.append(String.format("%02X", data[i]));
                if (i <= data.length - 1) sb.append(" ");
            }
            return sb.toString();
        }
        else if (type == Constants.CharacteristicReadType.INTEGER)
        {
            return "" + ByteBuffer.allocate(4).put(data).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
        }
        else if (type == Constants.CharacteristicReadType.TIME)
        {
            if (data.length != 10) return "";

            int year = (data[0] & 0xFF) + ((data[1] & 0xFF) << 8);

            return String.format(Locale.getDefault(), "%d/%02d/%02d %02d:%02d:%02d", year, data[2], data[3], data[4], data[5], data[6]);
        }

        return "";
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

    private void BroadcastOnDeviceScanned(String address)
    {
        Intent intent = new Intent(ON_DEVICE_SCANNED);
        intent.putExtra(ADDRESS, address);
        LocalBroadcastManager.getInstance(m_Context).sendBroadcast(intent);
    }
}
