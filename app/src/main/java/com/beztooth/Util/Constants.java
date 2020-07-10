package com.beztooth.Util;

import java.util.ArrayList;
import java.util.HashMap;

public class Constants {
    public enum CharacteristicReadType
    {
        STRING,
        HEX,
        INTEGER,
        TIME
    }

    private static HashMap<String, String> services = new HashMap<>();
    private static HashMap<String, Characteristic> characteristics = new HashMap<>();
    private static HashMap<String, String> descriptors = new HashMap<>();
    private static ArrayList<Device> devices = new ArrayList<>();

    public static class Device
    {
        public String MAC;
        public String Name;

        public ArrayList<Service> Services;

        public Device(String mac, String name, ArrayList<Service> services)
        {
            this.MAC = mac;
            this.Name = name;
            this.Services = services;
        }

        public Service GetService(String uuid)
        {
            for (Service s : Services) {
                if (s.UUID.equals(uuid)) return s;
            }

            return null;
        }
    }

    public static class Service
    {
        public String UUID;
        public String Name;
        public ArrayList<Characteristic> Characteristics;

        public Service(String uuid, String name, ArrayList<Characteristic> characteristics)
        {
            this.UUID = uuid;
            this.Name = name;
            this.Characteristics = characteristics;
        }

        public Characteristic GetCharacteristic(String uuid)
        {
            for (Characteristic c : Characteristics)
            {
                if (c.UUID.equals(uuid)) return c;
            }

            return null;
        }
    }

    public static class Characteristic
    {
        public String UUID;
        public String Name;
        public CharacteristicReadType ReadType;

        public Characteristic(String uuid, String name, CharacteristicReadType readType)
        {
            this.UUID = uuid;
            this.Name = name;
            this.ReadType = readType;
        }
    }

    public static class Devices
    {
        public static Device GetDevice(String mac)
        {
            for (Device d : devices)
            {
                if (d.MAC.equals(mac)) return d;
            }

            return null;
        }
    }

    public static class Services
    {
        public static String Get(String serviceUUID)
        {
            if ( serviceUUID == null) return null;
            serviceUUID = serviceUUID.toUpperCase();

            String serviceUUIDSubstring = GetUUIDSubstring(serviceUUID);
            String s = services.get(serviceUUIDSubstring);

            // Try retrieving the service by full uuid (less common)
            if (s == null)
            {
                s = services.get(serviceUUID);
            }

            return s;
        }

        public static String Get(String mac, String serviceUUID, boolean deviceOnly)
        {
            if (mac != null)
            {
                Device d = Devices.GetDevice(mac);
                if (d != null)
                {
                    Service s = d.GetService(serviceUUID);
                    if (s != null) return s.Name;
                }
            }

            // If deviceOnly is not specified, check the constants.
            return deviceOnly ? null : Get(serviceUUID);
        }
    }

    public static class Characteristics
    {
        public static Characteristic Get(String characteristicUUID)
        {
            if ( characteristicUUID == null) return null;
            characteristicUUID = characteristicUUID.toUpperCase();

            String uuidSubstring = GetUUIDSubstring(characteristicUUID);
            Characteristic c = characteristics.get(uuidSubstring);

            // Try retrieving the characteristic by full uuid (less common)
            if (c == null)
            {
                c = characteristics.get(characteristicUUID);
            }

            return c;
        }

        public static Characteristic Get(String device, String serviceUUID, String characteristicUUID)
        {
            if (device != null) {
                Device d = Devices.GetDevice(device);
                if (d != null) {
                    Service s = d.GetService(serviceUUID);
                    if (s != null) {
                        Characteristic c = s.GetCharacteristic(characteristicUUID);
                        if (c != null) return c;
                    }
                }
            }

            return Get(characteristicUUID);
        }
    }

    // Returns a substring of the UUID, specifically these (x):
    // 0000xxxx-0000-0000-0000-0000000000
    // This is because the gatt specifications only list the assigned number as these 4 digits of the UUID:
    // https://www.bluetooth.com/specifications/gatt
    private static String GetUUIDSubstring(String uuid)
    {
        if (uuid == null) return null;

        // If UUID is already a substring, just return it.
        if (uuid.length() == 4)
        {
            return uuid;
        }

        // Ensure UUID is of proper length (slashes included).
        if (uuid.length() != 36) return null;

        return uuid.substring(4, 8);
    }

    static {
        services.put("1800", "Generic Access");
        services.put("1811", "Alert Notification Service");
        services.put("1815", "Automation IO");
        services.put("180F", "Battery Service");
        services.put("183B", "Binary Sensor");
        services.put("1810", "Blood Pressure");
        services.put("181B", "Body Composition");
        services.put("181E", "Bond Management Service");
        services.put("181F", "Continuous Glucose Monitoring");
        services.put("1805", "Current Time Service");
        services.put("1818", "Cycling Power");
        services.put("1816", "Cycling Speed and Cadence");
        services.put("180A", "Device Information");
        services.put("183C", "Emergency Configuration");
        services.put("181A", "Environmental Sensing");
        services.put("1826", "Fitness Machine");
        services.put("1801", "Generic Attribute");
        services.put("1808", "Glucose");
        services.put("1809", "Health Thermometer");
        services.put("180D", "Heart Rate");
        services.put("1823", "HTTP Proxy");
        services.put("1812", "Human Interface Device");
        services.put("1802", "Immediate Alert");
        services.put("1821", "Indoor Positioning");
        services.put("183A", "Insulin Delivery");
        services.put("1820", "Internet Protocol Support Service");
        services.put("1803", "Link Loss");
        services.put("1819", "Location and Navigation");
        services.put("1827", "Mesh Provisioning Service");
        services.put("1828", "Mesh Proxy Service");
        services.put("1807", "Next DST Change Service");
        services.put("1825", "Object Transfer Service");
        services.put("180E", "Phone Alert Status Service");
        services.put("1822", "Pulse Oximeter Service");
        services.put("1829", "Reconnection Configuration");
        services.put("1806", "Reference Time Update Service");
        services.put("1814", "Running Speed and Cadence");
        services.put("1813", "Scan Parameters");
        services.put("1824", "Transport Discovery");
        services.put("1804", "Tx Power");
        services.put("181C", "User Data");
        services.put("181D", "Weight Scale");
        services.put("181D", "Weight Scale");
        services.put("1D14D6EE-FD63-4FA1-BFA4-8F47B42119F0", "Silicon Labs OTA");
    }

    static {
        characteristics.put("2A7E", new Characteristic("2A7E", "Aerobic Heart Rate Lower Limit", CharacteristicReadType.STRING));
        characteristics.put("2A84", new Characteristic("2A84", "Aerobic Heart Rate Upper Limit", CharacteristicReadType.STRING));
        characteristics.put("2A7F", new Characteristic("2A7F", "Aerobic Threshold", CharacteristicReadType.STRING));
        characteristics.put("2A80", new Characteristic("2A80", "Age", CharacteristicReadType.STRING));
        characteristics.put("2A5A", new Characteristic("2A5A", "Aggregate", CharacteristicReadType.STRING));
        characteristics.put("2A43", new Characteristic("2A43", "Alert Category ID", CharacteristicReadType.STRING));
        characteristics.put("2A42", new Characteristic("2A42", "Alert Category ID Bit Mask", CharacteristicReadType.STRING));
        characteristics.put("2A06", new Characteristic("2A06", "Alert Level", CharacteristicReadType.STRING));
        characteristics.put("2A44", new Characteristic("2A44", "Alert Notification Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A3F", new Characteristic("2A3F", "Alert Status", CharacteristicReadType.STRING));
        characteristics.put("2AB3", new Characteristic("2AB3", "Altitude", CharacteristicReadType.STRING));
        characteristics.put("2A81", new Characteristic("2A81", "Anaerobic Heart Rate Lower Limit", CharacteristicReadType.STRING));
        characteristics.put("2A82", new Characteristic("2A82", "Anaerobic Heart Rate Upper Limit", CharacteristicReadType.STRING));
        characteristics.put("2A83", new Characteristic("2A83", "Anaerobic Threshold", CharacteristicReadType.STRING));
        characteristics.put("2A58", new Characteristic("2A58", "Analog", CharacteristicReadType.STRING));
        characteristics.put("2A59", new Characteristic("2A59", "Analog Output", CharacteristicReadType.STRING));
        characteristics.put("2A73", new Characteristic("2A73", "Apparent Wind Direction", CharacteristicReadType.STRING));
        characteristics.put("2A72", new Characteristic("2A72", "Apparent Wind Speed", CharacteristicReadType.STRING));
        characteristics.put("2A01", new Characteristic("2A01", "Appearance", CharacteristicReadType.STRING));
        characteristics.put("2AA3", new Characteristic("2AA3", "Barometric Pressure Trend", CharacteristicReadType.STRING));
        characteristics.put("2A19", new Characteristic("2A19", "Battery Level", CharacteristicReadType.STRING));
        characteristics.put("2A1B", new Characteristic("2A1B", "Battery Level State", CharacteristicReadType.STRING));
        characteristics.put("2A1A", new Characteristic("2A1A", "Battery Power State", CharacteristicReadType.STRING));
        characteristics.put("2A49", new Characteristic("2A49", "Blood Pressure Feature", CharacteristicReadType.STRING));
        characteristics.put("2A35", new Characteristic("2A35", "Blood Pressure Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A9B", new Characteristic("2A9B", "Body Composition Feature", CharacteristicReadType.STRING));
        characteristics.put("2A9C", new Characteristic("2A9C", "Body Composition Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A38", new Characteristic("2A38", "Body Sensor Location", CharacteristicReadType.STRING));
        characteristics.put("2AA4", new Characteristic("2AA4", "Bond Management Control Point", CharacteristicReadType.STRING));
        characteristics.put("2AA5", new Characteristic("2AA5", "Bond Management Features", CharacteristicReadType.STRING));
        characteristics.put("2A22", new Characteristic("2A22", "Boot Keyboard Input Report", CharacteristicReadType.STRING));
        characteristics.put("2A32", new Characteristic("2A32", "Boot Keyboard Output Report", CharacteristicReadType.STRING));
        characteristics.put("2A33", new Characteristic("2A33", "Boot Mouse Input Report", CharacteristicReadType.STRING));
        characteristics.put("2B2B", new Characteristic("2B2B", "BSS Control Point", CharacteristicReadType.STRING));
        characteristics.put("2B2C", new Characteristic("2B2C", "BSS Response", CharacteristicReadType.STRING));
        characteristics.put("2AA8", new Characteristic("2AA8", "CGM Feature", CharacteristicReadType.STRING));
        characteristics.put("2AA7", new Characteristic("2AA7", "CGM Measurement", CharacteristicReadType.STRING));
        characteristics.put("2AAB", new Characteristic("2AAB", "CGM Session Run Time", CharacteristicReadType.STRING));
        characteristics.put("2AAA", new Characteristic("2AAA", "CGM Session Start Time", CharacteristicReadType.STRING));
        characteristics.put("2AAC", new Characteristic("2AAC", "CGM Specific Ops Control Point", CharacteristicReadType.STRING));
        characteristics.put("2AA9", new Characteristic("2AA9", "CGM Status", CharacteristicReadType.STRING));
        characteristics.put("2B29", new Characteristic("2B29", "Client Supported Features", CharacteristicReadType.STRING));
        characteristics.put("2ACE", new Characteristic("2ACE", "Cross Trainer Data", CharacteristicReadType.STRING));
        characteristics.put("2A5C", new Characteristic("2A5C", "CSC Feature", CharacteristicReadType.STRING));
        characteristics.put("2A5B", new Characteristic("2A5B", "CSC Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A2B", new Characteristic("2A2B", "Current Time", CharacteristicReadType.TIME));
        characteristics.put("2A66", new Characteristic("2A66", "Cycling Power Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A65", new Characteristic("2A65", "Cycling Power Feature", CharacteristicReadType.STRING));
        characteristics.put("2A63", new Characteristic("2A63", "Cycling Power Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A64", new Characteristic("2A64", "Cycling Power Vector", CharacteristicReadType.STRING));
        characteristics.put("2A99", new Characteristic("2A99", "Database Change Increment", CharacteristicReadType.STRING));
        characteristics.put("2B2A", new Characteristic("2B2A", "Database Hash", CharacteristicReadType.STRING));
        characteristics.put("2A85", new Characteristic("2A85", "Date of Birth", CharacteristicReadType.STRING));
        characteristics.put("2A86", new Characteristic("2A86", "Date of Threshold Assessment", CharacteristicReadType.STRING));
        characteristics.put("2A08", new Characteristic("2A08", "Date Time", CharacteristicReadType.STRING));
        characteristics.put("2AED", new Characteristic("2AED", "Date UTC", CharacteristicReadType.STRING));
        characteristics.put("2A0A", new Characteristic("2A0A", "Day Date Time", CharacteristicReadType.STRING));
        characteristics.put("2A09", new Characteristic("2A09", "Day of Week", CharacteristicReadType.STRING));
        characteristics.put("2A7D", new Characteristic("2A7D", "Descriptor Value Changed", CharacteristicReadType.STRING));
        characteristics.put("2A7B", new Characteristic("2A7B", "Dew Point", CharacteristicReadType.STRING));
        characteristics.put("2A56", new Characteristic("2A56", "Digital", CharacteristicReadType.STRING));
        characteristics.put("2A57", new Characteristic("2A57", "Digital Output", CharacteristicReadType.STRING));
        characteristics.put("2A0D", new Characteristic("2A0D", "DST Offset", CharacteristicReadType.STRING));
        characteristics.put("2A6C", new Characteristic("2A6C", "Elevation", CharacteristicReadType.STRING));
        characteristics.put("2A87", new Characteristic("2A87", "Email Address", CharacteristicReadType.STRING));
        characteristics.put("2B2D", new Characteristic("2B2D", "Emergency ID", CharacteristicReadType.STRING));
        characteristics.put("2B2E", new Characteristic("2B2E", "Emergency Text", CharacteristicReadType.STRING));
        characteristics.put("2A0B", new Characteristic("2A0B", "Exact Time 100", CharacteristicReadType.STRING));
        characteristics.put("2A0C", new Characteristic("2A0C", "Exact Time 256", CharacteristicReadType.STRING));
        characteristics.put("2A88", new Characteristic("2A88", "Fat Burn Heart Rate Lower Limit", CharacteristicReadType.STRING));
        characteristics.put("2A89", new Characteristic("2A89", "Fat Burn Heart Rate Upper Limit", CharacteristicReadType.STRING));
        characteristics.put("2A26", new Characteristic("2A26", "Firmware Revision String", CharacteristicReadType.STRING));
        characteristics.put("2A8A", new Characteristic("2A8A", "First Name", CharacteristicReadType.STRING));
        characteristics.put("2AD9", new Characteristic("2AD9", "Fitness Machine Control Point", CharacteristicReadType.STRING));
        characteristics.put("2ACC", new Characteristic("2ACC", "Fitness Machine Feature", CharacteristicReadType.STRING));
        characteristics.put("2ADA", new Characteristic("2ADA", "Fitness Machine Status", CharacteristicReadType.STRING));
        characteristics.put("2A8B", new Characteristic("2A8B", "Five Zone Heart Rate Limits", CharacteristicReadType.STRING));
        characteristics.put("2AB2", new Characteristic("2AB2", "Floor Number", CharacteristicReadType.STRING));
        characteristics.put("2AA6", new Characteristic("2AA6", "Central Address Resolution", CharacteristicReadType.STRING));
        characteristics.put("2A00", new Characteristic("2A00", "Device Name", CharacteristicReadType.STRING));
        characteristics.put("2A04", new Characteristic("2A04", "Peripheral Preferred Connection Parameters", CharacteristicReadType.STRING));
        characteristics.put("2A02", new Characteristic("2A02", "Peripheral Privacy Flag", CharacteristicReadType.STRING));
        characteristics.put("2A03", new Characteristic("2A03", "Reconnection Address", CharacteristicReadType.STRING));
        characteristics.put("2A05", new Characteristic("2A05", "Service Changed", CharacteristicReadType.STRING));
        characteristics.put("2A8C", new Characteristic("2A8C", "Gender", CharacteristicReadType.STRING));
        characteristics.put("2A51", new Characteristic("2A51", "Glucose Feature", CharacteristicReadType.STRING));
        characteristics.put("2A18", new Characteristic("2A18", "Glucose Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A34", new Characteristic("2A34", "Glucose Measurement Context", CharacteristicReadType.STRING));
        characteristics.put("2A74", new Characteristic("2A74", "Gust Factor", CharacteristicReadType.STRING));
        characteristics.put("2A27", new Characteristic("2A27", "Hardware Revision String", CharacteristicReadType.STRING));
        characteristics.put("2A39", new Characteristic("2A39", "Heart Rate Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A8D", new Characteristic("2A8D", "Heart Rate Max", CharacteristicReadType.STRING));
        characteristics.put("2A37", new Characteristic("2A37", "Heart Rate Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A7A", new Characteristic("2A7A", "Heat Index", CharacteristicReadType.STRING));
        characteristics.put("2A8E", new Characteristic("2A8E", "Height", CharacteristicReadType.STRING));
        characteristics.put("2A4C", new Characteristic("2A4C", "HID Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A4A", new Characteristic("2A4A", "HID Information", CharacteristicReadType.STRING));
        characteristics.put("2A8F", new Characteristic("2A8F", "Hip Circumference", CharacteristicReadType.STRING));
        characteristics.put("2ABA", new Characteristic("2ABA", "HTTP Control Point", CharacteristicReadType.STRING));
        characteristics.put("2AB9", new Characteristic("2AB9", "HTTP Entity Body", CharacteristicReadType.STRING));
        characteristics.put("2AB7", new Characteristic("2AB7", "HTTP Headers", CharacteristicReadType.STRING));
        characteristics.put("2AB8", new Characteristic("2AB8", "HTTP Status Code", CharacteristicReadType.STRING));
        characteristics.put("2ABB", new Characteristic("2ABB", "HTTPS Security", CharacteristicReadType.STRING));
        characteristics.put("2A6F", new Characteristic("2A6F", "Humidity", CharacteristicReadType.STRING));
        characteristics.put("2B22", new Characteristic("2B22", "IDD Annunciation Status", CharacteristicReadType.STRING));
        characteristics.put("2B25", new Characteristic("2B25", "IDD Command Control Point", CharacteristicReadType.STRING));
        characteristics.put("2B26", new Characteristic("2B26", "IDD Command Data", CharacteristicReadType.STRING));
        characteristics.put("2B23", new Characteristic("2B23", "IDD Features", CharacteristicReadType.STRING));
        characteristics.put("2B28", new Characteristic("2B28", "IDD History Data", CharacteristicReadType.STRING));
        characteristics.put("2B27", new Characteristic("2B27", "IDD Record Access Control Point", CharacteristicReadType.STRING));
        characteristics.put("2B21", new Characteristic("2B21", "IDD Status", CharacteristicReadType.STRING));
        characteristics.put("2B20", new Characteristic("2B20", "IDD Status Changed", CharacteristicReadType.STRING));
        characteristics.put("2B24", new Characteristic("2B24", "IDD Status Reader Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A2A", new Characteristic("2A2A", "IEEE 11073-20601 Regulatory Certification Data List", CharacteristicReadType.STRING));
        characteristics.put("2AD2", new Characteristic("2AD2", "Indoor Bike Data", CharacteristicReadType.STRING));
        characteristics.put("2AAD", new Characteristic("2AAD", "Indoor Positioning Configuration", CharacteristicReadType.STRING));
        characteristics.put("2A36", new Characteristic("2A36", "Intermediate Cuff Pressure", CharacteristicReadType.STRING));
        characteristics.put("2A1E", new Characteristic("2A1E", "Intermediate Temperature", CharacteristicReadType.STRING));
        characteristics.put("2A77", new Characteristic("2A77", "Irradiance", CharacteristicReadType.STRING));
        characteristics.put("2AA2", new Characteristic("2AA2", "Language", CharacteristicReadType.STRING));
        characteristics.put("2A90", new Characteristic("2A90", "Last Name", CharacteristicReadType.STRING));
        characteristics.put("2AAE", new Characteristic("2AAE", "Latitude", CharacteristicReadType.STRING));
        characteristics.put("2A6B", new Characteristic("2A6B", "LN Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A6A", new Characteristic("2A6A", "LN Feature", CharacteristicReadType.STRING));
        characteristics.put("2AB1", new Characteristic("2AB1", "Local East Coordinate", CharacteristicReadType.STRING));
        characteristics.put("2AB0", new Characteristic("2AB0", "Local North Coordinate", CharacteristicReadType.STRING));
        characteristics.put("2A0F", new Characteristic("2A0F", "Local Time Information", CharacteristicReadType.STRING));
        characteristics.put("2A67", new Characteristic("2A67", "Location and Speed Characteristic", CharacteristicReadType.STRING));
        characteristics.put("2AB5", new Characteristic("2AB5", "Location Name", CharacteristicReadType.STRING));
        characteristics.put("2AAF", new Characteristic("2AAF", "Longitude", CharacteristicReadType.STRING));
        characteristics.put("2A2C", new Characteristic("2A2C", "Magnetic Declination", CharacteristicReadType.STRING));
        characteristics.put("2AA0", new Characteristic("2AA0", "Magnetic Flux Density – 2D", CharacteristicReadType.STRING));
        characteristics.put("2AA1", new Characteristic("2AA1", "Magnetic Flux Density – 3D", CharacteristicReadType.STRING));
        characteristics.put("2A29", new Characteristic("2A29", "Manufacturer Name String", CharacteristicReadType.STRING));
        characteristics.put("2A91", new Characteristic("2A91", "Maximum Recommended Heart Rate", CharacteristicReadType.STRING));
        characteristics.put("2A21", new Characteristic("2A21", "Measurement Interval", CharacteristicReadType.STRING));
        characteristics.put("2A24", new Characteristic("2A24", "Model Number String", CharacteristicReadType.STRING));
        characteristics.put("2A68", new Characteristic("2A68", "Navigation", CharacteristicReadType.STRING));
        characteristics.put("2A3E", new Characteristic("2A3E", "Network Availability", CharacteristicReadType.STRING));
        characteristics.put("2A46", new Characteristic("2A46", "New Alert", CharacteristicReadType.STRING));
        characteristics.put("2AC5", new Characteristic("2AC5", "Object Action Control Point", CharacteristicReadType.STRING));
        characteristics.put("2AC8", new Characteristic("2AC8", "Object Changed", CharacteristicReadType.STRING));
        characteristics.put("2AC1", new Characteristic("2AC1", "Object First-Created", CharacteristicReadType.STRING));
        characteristics.put("2AC3", new Characteristic("2AC3", "Object ID", CharacteristicReadType.STRING));
        characteristics.put("2AC2", new Characteristic("2AC2", "Object Last-Modified", CharacteristicReadType.STRING));
        characteristics.put("2AC6", new Characteristic("2AC6", "Object List Control Point", CharacteristicReadType.STRING));
        characteristics.put("2AC7", new Characteristic("2AC7", "Object List Filter", CharacteristicReadType.STRING));
        characteristics.put("2ABE", new Characteristic("2ABE", "Object Name", CharacteristicReadType.STRING));
        characteristics.put("2AC4", new Characteristic("2AC4", "Object Properties", CharacteristicReadType.STRING));
        characteristics.put("2AC0", new Characteristic("2AC0", "Object Size", CharacteristicReadType.STRING));
        characteristics.put("2ABF", new Characteristic("2ABF", "Object Type", CharacteristicReadType.STRING));
        characteristics.put("2ABD", new Characteristic("2ABD", "OTS Feature", CharacteristicReadType.STRING));
        characteristics.put("2A5F", new Characteristic("2A5F", "PLX Continuous Measurement Characteristic", CharacteristicReadType.STRING));
        characteristics.put("2A60", new Characteristic("2A60", "PLX Features", CharacteristicReadType.STRING));
        characteristics.put("2A5E", new Characteristic("2A5E", "PLX Spot-Check Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A50", new Characteristic("2A50", "PnP ID", CharacteristicReadType.STRING));
        characteristics.put("2A75", new Characteristic("2A75", "Pollen Concentration", CharacteristicReadType.STRING));
        characteristics.put("2A2F", new Characteristic("2A2F", "Position 2D", CharacteristicReadType.STRING));
        characteristics.put("2A30", new Characteristic("2A30", "Position 3D", CharacteristicReadType.STRING));
        characteristics.put("2A69", new Characteristic("2A69", "Position Quality", CharacteristicReadType.STRING));
        characteristics.put("2A6D", new Characteristic("2A6D", "Pressure", CharacteristicReadType.STRING));
        characteristics.put("2A4E", new Characteristic("2A4E", "Protocol Mode", CharacteristicReadType.STRING));
        characteristics.put("2A62", new Characteristic("2A62", "Pulse Oximetry Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A78", new Characteristic("2A78", "Rainfall", CharacteristicReadType.STRING));
        characteristics.put("2B1D", new Characteristic("2B1D", "RC Feature", CharacteristicReadType.STRING));
        characteristics.put("2B1E", new Characteristic("2B1E", "RC Settings", CharacteristicReadType.STRING));
        characteristics.put("2B1F", new Characteristic("2B1F", "Reconnection Configuration Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A52", new Characteristic("2A52", "Record Access Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A14", new Characteristic("2A14", "Reference Time Information", CharacteristicReadType.STRING));
        characteristics.put("2B37", new Characteristic("2B37", "Registered User Characteristic", CharacteristicReadType.STRING));
        characteristics.put("2A3A", new Characteristic("2A3A", "Removable", CharacteristicReadType.STRING));
        characteristics.put("2A4D", new Characteristic("2A4D", "Report", CharacteristicReadType.STRING));
        characteristics.put("2A4B", new Characteristic("2A4B", "Report Map", CharacteristicReadType.STRING));
        characteristics.put("2AC9", new Characteristic("2AC9", "Resolvable Private Address Only", CharacteristicReadType.STRING));
        characteristics.put("2A92", new Characteristic("2A92", "Resting Heart Rate", CharacteristicReadType.STRING));
        characteristics.put("2A40", new Characteristic("2A40", "Ringer Control point", CharacteristicReadType.STRING));
        characteristics.put("2A41", new Characteristic("2A41", "Ringer Setting", CharacteristicReadType.STRING));
        characteristics.put("2AD1", new Characteristic("2AD1", "Rower Data", CharacteristicReadType.STRING));
        characteristics.put("2A54", new Characteristic("2A54", "RSC Feature", CharacteristicReadType.STRING));
        characteristics.put("2A53", new Characteristic("2A53", "RSC Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A55", new Characteristic("2A55", "SC Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A4F", new Characteristic("2A4F", "Scan Interval Window", CharacteristicReadType.STRING));
        characteristics.put("2A31", new Characteristic("2A31", "Scan Refresh", CharacteristicReadType.STRING));
        characteristics.put("2A3C", new Characteristic("2A3C", "Scientific Temperature Celsius", CharacteristicReadType.STRING));
        characteristics.put("2A10", new Characteristic("2A10", "Secondary Time Zone", CharacteristicReadType.STRING));
        characteristics.put("2A5D", new Characteristic("2A5D", "Sensor Location", CharacteristicReadType.STRING));
        characteristics.put("2A25", new Characteristic("2A25", "Serial Number String", CharacteristicReadType.STRING));
        characteristics.put("2B3A", new Characteristic("2B3A", "Server Supported Features", CharacteristicReadType.STRING));
        characteristics.put("2A3B", new Characteristic("2A3B", "Service Required", CharacteristicReadType.STRING));
        characteristics.put("2A28", new Characteristic("2A28", "Software Revision String", CharacteristicReadType.STRING));
        characteristics.put("2A93", new Characteristic("2A93", "Sport Type for Aerobic and Anaerobic Thresholds", CharacteristicReadType.STRING));
        characteristics.put("2AD0", new Characteristic("2AD0", "Stair Climber Data", CharacteristicReadType.STRING));
        characteristics.put("2ACF", new Characteristic("2ACF", "Step Climber Data", CharacteristicReadType.STRING));
        characteristics.put("2A3D", new Characteristic("2A3D", "String", CharacteristicReadType.STRING));
        characteristics.put("2AD7", new Characteristic("2AD7", "Supported Heart Rate Range", CharacteristicReadType.STRING));
        characteristics.put("2AD5", new Characteristic("2AD5", "Supported Inclination Range", CharacteristicReadType.STRING));
        characteristics.put("2A47", new Characteristic("2A47", "Supported New Alert Category", CharacteristicReadType.STRING));
        characteristics.put("2AD8", new Characteristic("2AD8", "Supported Power Range", CharacteristicReadType.STRING));
        characteristics.put("2AD6", new Characteristic("2AD6", "Supported Resistance Level Range", CharacteristicReadType.STRING));
        characteristics.put("2AD4", new Characteristic("2AD4", "Supported Speed Range", CharacteristicReadType.STRING));
        characteristics.put("2A48", new Characteristic("2A48", "Supported Unread Alert Category", CharacteristicReadType.STRING));
        characteristics.put("2A23", new Characteristic("2A23", "System ID", CharacteristicReadType.STRING));
        characteristics.put("2ABC", new Characteristic("2ABC", "TDS Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A6E", new Characteristic("2A6E", "Temperature", CharacteristicReadType.STRING));
        characteristics.put("2A1F", new Characteristic("2A1F", "Temperature Celsius", CharacteristicReadType.STRING));
        characteristics.put("2A20", new Characteristic("2A20", "Temperature Fahrenheit", CharacteristicReadType.STRING));
        characteristics.put("2A1C", new Characteristic("2A1C", "Temperature Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A1D", new Characteristic("2A1D", "Temperature Type", CharacteristicReadType.STRING));
        characteristics.put("2A94", new Characteristic("2A94", "Three Zone Heart Rate Limits", CharacteristicReadType.STRING));
        characteristics.put("2A12", new Characteristic("2A12", "Time Accuracy", CharacteristicReadType.STRING));
        characteristics.put("2A15", new Characteristic("2A15", "Time Broadcast", CharacteristicReadType.STRING));
        characteristics.put("2A13", new Characteristic("2A13", "Time Source", CharacteristicReadType.STRING));
        characteristics.put("2A16", new Characteristic("2A16", "Time Update Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A17", new Characteristic("2A17", "Time Update State", CharacteristicReadType.STRING));
        characteristics.put("2A11", new Characteristic("2A11", "Time with DST", CharacteristicReadType.STRING));
        characteristics.put("2A0E", new Characteristic("2A0E", "Time Zone", CharacteristicReadType.STRING));
        characteristics.put("2AD3", new Characteristic("2AD3", "Training Status", CharacteristicReadType.STRING));
        characteristics.put("2ACD", new Characteristic("2ACD", "Treadmill Data", CharacteristicReadType.STRING));
        characteristics.put("2A71", new Characteristic("2A71", "True Wind Direction", CharacteristicReadType.STRING));
        characteristics.put("2A70", new Characteristic("2A70", "True Wind Speed", CharacteristicReadType.STRING));
        characteristics.put("2A95", new Characteristic("2A95", "Two Zone Heart Rate Limit", CharacteristicReadType.STRING));
        characteristics.put("2A07", new Characteristic("2A07", "Tx Power Level", CharacteristicReadType.STRING));
        characteristics.put("2AB4", new Characteristic("2AB4", "Uncertainty", CharacteristicReadType.STRING));
        characteristics.put("2A45", new Characteristic("2A45", "Unread Alert Status", CharacteristicReadType.STRING));
        characteristics.put("2AB6", new Characteristic("2AB6", "URI", CharacteristicReadType.STRING));
        characteristics.put("2A9F", new Characteristic("2A9F", "User Control Point", CharacteristicReadType.STRING));
        characteristics.put("2A9A", new Characteristic("2A9A", "User Index", CharacteristicReadType.STRING));
        characteristics.put("2A76", new Characteristic("2A76", "UV Index", CharacteristicReadType.STRING));
        characteristics.put("2A96", new Characteristic("2A96", "VO2 Max", CharacteristicReadType.STRING));
        characteristics.put("2A97", new Characteristic("2A97", "Waist Circumference", CharacteristicReadType.STRING));
        characteristics.put("2A98", new Characteristic("2A98", "Weight", CharacteristicReadType.STRING));
        characteristics.put("2A9D", new Characteristic("2A9D", "Weight Measurement", CharacteristicReadType.STRING));
        characteristics.put("2A9E", new Characteristic("2A9E", "Weight Scale Feature", CharacteristicReadType.STRING));
        characteristics.put("2A79", new Characteristic("2A79", "Wind Chill", CharacteristicReadType.STRING));
        characteristics.put("F7BF3564-FB6D-4E53-88A4-5E37E0326063", new Characteristic("F7BF3564-FB6D-4E53-88A4-5E37E0326063", "Silicon Labs OTA Control", CharacteristicReadType.STRING));
    }

    static {
        descriptors.put("2905", "Characteristic Aggregate Format");
        descriptors.put("2900", "Characteristic Extended Properties");
        descriptors.put("2904", "Characteristic Presentation Format");
        descriptors.put("2901", "Characteristic User Description");
        descriptors.put("2902", "Client Characteristic Configuration");
        descriptors.put("290B", "Environmental Sensing Configuration");
        descriptors.put("290C", "Environmental Sensing Measurement");
        descriptors.put("290D", "Environmental Sensing Trigger Setting");
        descriptors.put("2907", "External Report Reference");
        descriptors.put("2909", "Number of Digitals");
        descriptors.put("2908", "Report Reference");
        descriptors.put("2903", "Server Characteristic Configuration");
        descriptors.put("290E", "Time Trigger Setting");
        descriptors.put("2906", "Valid Range");
        descriptors.put("290A", "Value Trigger Setting");
    }

    static {
        devices.add(new Device("00:0B:57:1A:88:EF", "Test Device", new ArrayList<Service>() {{
            add(new Service("28ccba6e-b38e-472a-a26b-a1c1ed9320da", "Test Sensor", new ArrayList<Characteristic>() {{
                add(new Characteristic("934daf64-bbff-47d2-8df4-864e2589f019", "Temperature", CharacteristicReadType.INTEGER));
                add(new Characteristic("fbe1a7f9-f24f-4ae0-9967-41464757eeb9", "Humidity", CharacteristicReadType.INTEGER));
            }}));
        }}));
    }
}
