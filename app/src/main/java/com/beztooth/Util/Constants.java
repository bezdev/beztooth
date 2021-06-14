package com.beztooth.Util;

import java.util.ArrayList;
import java.util.HashMap;

public class Constants {
    public static final String BASE_UUID = "0000xxxx-0000-1000-8000-00805F9B34FB";
    public static final CharacteristicReadType DEFAULT_CHARACTERISTIC_TYPE = CharacteristicReadType.HEX;

    public enum CharacteristicReadType
    {
        STRING,
        HEX,
        INTEGER,
        TIME,
        CUSTOM
    }

    private static HashMap<String, String> services = new HashMap<>();
    private static HashMap<String, Characteristic> characteristics = new HashMap<>();
    private static HashMap<String, String> descriptors = new HashMap<>();
    private static ArrayList<Device> devices = new ArrayList<>();

    // Commonly used Services/Characteristics/Descriptors
    public static final UUIDNamePair SERVICE_CURRENT_TIME = new UUIDNamePair("1805", "Current Time Service");
    public static final UUIDNamePair SERVICE_ENVIRONMENTAL_SENSING = new UUIDNamePair("181A", "Environmental Sensing");
    public static final UUIDNamePair CHARACTERISTIC_CURRENT_TIME = new UUIDNamePair("2A2B", "Current Time");
    public static final UUIDNamePair CHARACTERISTIC_REFERENCE_TIME = new UUIDNamePair("2A14", "Reference Time Information");
    public static final UUIDNamePair CHARACTERISTIC_TEMPERATURE = new UUIDNamePair("2A6E", "Temperature");
    public static final UUIDNamePair CHARACTERISTIC_HUMIDITY = new UUIDNamePair("2A6F", "Humidity");
    public static final UUIDNamePair CHARACTERISTIC_PRESSURE = new UUIDNamePair("2A6D", "Pressure");
    public static final UUIDNamePair DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION = new UUIDNamePair("2902", "Client Characteristic Configuration");

    static
    {
        services.put("1800", "Generic Access");
        services.put("1811", "Alert Notification Service");
        services.put("1815", "Automation IO");
        services.put("180F", "Battery Service");
        services.put("183B", "Binary Sensor");
        services.put("1810", "Blood Pressure");
        services.put("181B", "Body Composition");
        services.put("181E", "Bond Management Service");
        services.put("181F", "Continuous Glucose Monitoring");
        services.put(SERVICE_CURRENT_TIME.UUID, SERVICE_CURRENT_TIME.Name);
        services.put("1818", "Cycling Power");
        services.put("1816", "Cycling Speed and Cadence");
        services.put("180A", "Device Information");
        services.put("183C", "Emergency Configuration");
        services.put(SERVICE_ENVIRONMENTAL_SENSING.UUID, SERVICE_ENVIRONMENTAL_SENSING.Name);
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
        services.put("1D14D6EE-FD63-4FA1-BFA4-8F47B42119F0", "Silicon Labs OTA");

        characteristics.put("2A7E", new Characteristic("2A7E", "Aerobic Heart Rate Lower Limit"));
        characteristics.put("2A84", new Characteristic("2A84", "Aerobic Heart Rate Upper Limit"));
        characteristics.put("2A7F", new Characteristic("2A7F", "Aerobic Threshold"));
        characteristics.put("2A80", new Characteristic("2A80", "Age"));
        characteristics.put("2A5A", new Characteristic("2A5A", "Aggregate"));
        characteristics.put("2A43", new Characteristic("2A43", "Alert Category ID", CharacteristicReadType.STRING));
        characteristics.put("2A42", new Characteristic("2A42", "Alert Category ID Bit Mask"));
        characteristics.put("2A06", new Characteristic("2A06", "Alert Level"));
        characteristics.put("2A44", new Characteristic("2A44", "Alert Notification Control Point"));
        characteristics.put("2A3F", new Characteristic("2A3F", "Alert Status"));
        characteristics.put("2AB3", new Characteristic("2AB3", "Altitude"));
        characteristics.put("2A81", new Characteristic("2A81", "Anaerobic Heart Rate Lower Limit"));
        characteristics.put("2A82", new Characteristic("2A82", "Anaerobic Heart Rate Upper Limit"));
        characteristics.put("2A83", new Characteristic("2A83", "Anaerobic Threshold"));
        characteristics.put("2A58", new Characteristic("2A58", "Analog"));
        characteristics.put("2A59", new Characteristic("2A59", "Analog Output"));
        characteristics.put("2A73", new Characteristic("2A73", "Apparent Wind Direction"));
        characteristics.put("2A72", new Characteristic("2A72", "Apparent Wind Speed"));
        characteristics.put("2A01", new Characteristic("2A01", "Appearance"));
        characteristics.put("2AA3", new Characteristic("2AA3", "Barometric Pressure Trend"));
        characteristics.put("2A19", new Characteristic("2A19", "Battery Level"));
        characteristics.put("2A1B", new Characteristic("2A1B", "Battery Level State"));
        characteristics.put("2A1A", new Characteristic("2A1A", "Battery Power State"));
        characteristics.put("2A49", new Characteristic("2A49", "Blood Pressure Feature"));
        characteristics.put("2A35", new Characteristic("2A35", "Blood Pressure Measurement"));
        characteristics.put("2A9B", new Characteristic("2A9B", "Body Composition Feature"));
        characteristics.put("2A9C", new Characteristic("2A9C", "Body Composition Measurement"));
        characteristics.put("2A38", new Characteristic("2A38", "Body Sensor Location"));
        characteristics.put("2AA4", new Characteristic("2AA4", "Bond Management Control Point"));
        characteristics.put("2AA5", new Characteristic("2AA5", "Bond Management Features"));
        characteristics.put("2A22", new Characteristic("2A22", "Boot Keyboard Input Report"));
        characteristics.put("2A32", new Characteristic("2A32", "Boot Keyboard Output Report"));
        characteristics.put("2A33", new Characteristic("2A33", "Boot Mouse Input Report"));
        characteristics.put("2B2B", new Characteristic("2B2B", "BSS Control Point"));
        characteristics.put("2B2C", new Characteristic("2B2C", "BSS Response"));
        characteristics.put("2AA8", new Characteristic("2AA8", "CGM Feature"));
        characteristics.put("2AA7", new Characteristic("2AA7", "CGM Measurement"));
        characteristics.put("2AAB", new Characteristic("2AAB", "CGM Session Run Time"));
        characteristics.put("2AAA", new Characteristic("2AAA", "CGM Session Start Time"));
        characteristics.put("2AAC", new Characteristic("2AAC", "CGM Specific Ops Control Point"));
        characteristics.put("2AA9", new Characteristic("2AA9", "CGM Status"));
        characteristics.put("2B29", new Characteristic("2B29", "Client Supported Features"));
        characteristics.put("2ACE", new Characteristic("2ACE", "Cross Trainer Data"));
        characteristics.put("2A5C", new Characteristic("2A5C", "CSC Feature"));
        characteristics.put("2A5B", new Characteristic("2A5B", "CSC Measurement"));
        characteristics.put(CHARACTERISTIC_CURRENT_TIME.UUID, new Characteristic(CHARACTERISTIC_CURRENT_TIME, CharacteristicReadType.TIME));
        characteristics.put("2A66", new Characteristic("2A66", "Cycling Power Control Point"));
        characteristics.put("2A65", new Characteristic("2A65", "Cycling Power Feature"));
        characteristics.put("2A63", new Characteristic("2A63", "Cycling Power Measurement"));
        characteristics.put("2A64", new Characteristic("2A64", "Cycling Power Vector"));
        characteristics.put("2A99", new Characteristic("2A99", "Database Change Increment"));
        characteristics.put("2B2A", new Characteristic("2B2A", "Database Hash"));
        characteristics.put("2A85", new Characteristic("2A85", "Date of Birth"));
        characteristics.put("2A86", new Characteristic("2A86", "Date of Threshold Assessment"));
        characteristics.put("2A08", new Characteristic("2A08", "Date Time"));
        characteristics.put("2AED", new Characteristic("2AED", "Date UTC"));
        characteristics.put("2A0A", new Characteristic("2A0A", "Day Date Time"));
        characteristics.put("2A09", new Characteristic("2A09", "Day of Week"));
        characteristics.put("2A7D", new Characteristic("2A7D", "Descriptor Value Changed"));
        characteristics.put("2A7B", new Characteristic("2A7B", "Dew Point"));
        characteristics.put("2A56", new Characteristic("2A56", "Digital"));
        characteristics.put("2A57", new Characteristic("2A57", "Digital Output"));
        characteristics.put("2A0D", new Characteristic("2A0D", "DST Offset"));
        characteristics.put("2A6C", new Characteristic("2A6C", "Elevation"));
        characteristics.put("2A87", new Characteristic("2A87", "Email Address"));
        characteristics.put("2B2D", new Characteristic("2B2D", "Emergency ID"));
        characteristics.put("2B2E", new Characteristic("2B2E", "Emergency Text"));
        characteristics.put("2A0B", new Characteristic("2A0B", "Exact Time 100"));
        characteristics.put("2A0C", new Characteristic("2A0C", "Exact Time 256"));
        characteristics.put("2A88", new Characteristic("2A88", "Fat Burn Heart Rate Lower Limit"));
        characteristics.put("2A89", new Characteristic("2A89", "Fat Burn Heart Rate Upper Limit"));
        characteristics.put("2A26", new Characteristic("2A26", "Firmware Revision String", CharacteristicReadType.STRING));
        characteristics.put("2A8A", new Characteristic("2A8A", "First Name", CharacteristicReadType.STRING));
        characteristics.put("2AD9", new Characteristic("2AD9", "Fitness Machine Control Point"));
        characteristics.put("2ACC", new Characteristic("2ACC", "Fitness Machine Feature"));
        characteristics.put("2ADA", new Characteristic("2ADA", "Fitness Machine Status"));
        characteristics.put("2A8B", new Characteristic("2A8B", "Five Zone Heart Rate Limits"));
        characteristics.put("2AB2", new Characteristic("2AB2", "Floor Number"));
        characteristics.put("2AA6", new Characteristic("2AA6", "Central Address Resolution"));
        characteristics.put("2A00", new Characteristic("2A00", "Device Name", CharacteristicReadType.STRING));
        characteristics.put("2A04", new Characteristic("2A04", "Peripheral Preferred Connection Parameters"));
        characteristics.put("2A02", new Characteristic("2A02", "Peripheral Privacy Flag"));
        characteristics.put("2A03", new Characteristic("2A03", "Reconnection Address"));
        characteristics.put("2A05", new Characteristic("2A05", "Service Changed"));
        characteristics.put("2A8C", new Characteristic("2A8C", "Gender"));
        characteristics.put("2A51", new Characteristic("2A51", "Glucose Feature"));
        characteristics.put("2A18", new Characteristic("2A18", "Glucose Measurement"));
        characteristics.put("2A34", new Characteristic("2A34", "Glucose Measurement Context"));
        characteristics.put("2A74", new Characteristic("2A74", "Gust Factor"));
        characteristics.put("2A27", new Characteristic("2A27", "Hardware Revision String"));
        characteristics.put("2A39", new Characteristic("2A39", "Heart Rate Control Point"));
        characteristics.put("2A8D", new Characteristic("2A8D", "Heart Rate Max"));
        characteristics.put("2A37", new Characteristic("2A37", "Heart Rate Measurement"));
        characteristics.put("2A7A", new Characteristic("2A7A", "Heat Index"));
        characteristics.put("2A8E", new Characteristic("2A8E", "Height"));
        characteristics.put("2A4C", new Characteristic("2A4C", "HID Control Point"));
        characteristics.put("2A4A", new Characteristic("2A4A", "HID Information"));
        characteristics.put("2A8F", new Characteristic("2A8F", "Hip Circumference"));
        characteristics.put("2ABA", new Characteristic("2ABA", "HTTP Control Point"));
        characteristics.put("2AB9", new Characteristic("2AB9", "HTTP Entity Body"));
        characteristics.put("2AB7", new Characteristic("2AB7", "HTTP Headers"));
        characteristics.put("2AB8", new Characteristic("2AB8", "HTTP Status Code"));
        characteristics.put("2ABB", new Characteristic("2ABB", "HTTPS Security"));
        characteristics.put(CHARACTERISTIC_HUMIDITY.UUID, new Characteristic(CHARACTERISTIC_HUMIDITY, CharacteristicReadType.INTEGER));
        characteristics.put("2B22", new Characteristic("2B22", "IDD Annunciation Status"));
        characteristics.put("2B25", new Characteristic("2B25", "IDD Command Control Point"));
        characteristics.put("2B26", new Characteristic("2B26", "IDD Command Data"));
        characteristics.put("2B23", new Characteristic("2B23", "IDD Features"));
        characteristics.put("2B28", new Characteristic("2B28", "IDD History Data"));
        characteristics.put("2B27", new Characteristic("2B27", "IDD Record Access Control Point"));
        characteristics.put("2B21", new Characteristic("2B21", "IDD Status"));
        characteristics.put("2B20", new Characteristic("2B20", "IDD Status Changed"));
        characteristics.put("2B24", new Characteristic("2B24", "IDD Status Reader Control Point"));
        characteristics.put("2A2A", new Characteristic("2A2A", "IEEE 11073-20601 Regulatory Certification Data List"));
        characteristics.put("2AD2", new Characteristic("2AD2", "Indoor Bike Data"));
        characteristics.put("2AAD", new Characteristic("2AAD", "Indoor Positioning Configuration"));
        characteristics.put("2A36", new Characteristic("2A36", "Intermediate Cuff Pressure"));
        characteristics.put("2A1E", new Characteristic("2A1E", "Intermediate Temperature"));
        characteristics.put("2A77", new Characteristic("2A77", "Irradiance"));
        characteristics.put("2AA2", new Characteristic("2AA2", "Language"));
        characteristics.put("2A90", new Characteristic("2A90", "Last Name", CharacteristicReadType.STRING));
        characteristics.put("2AAE", new Characteristic("2AAE", "Latitude"));
        characteristics.put("2A6B", new Characteristic("2A6B", "LN Control Point"));
        characteristics.put("2A6A", new Characteristic("2A6A", "LN Feature"));
        characteristics.put("2AB1", new Characteristic("2AB1", "Local East Coordinate"));
        characteristics.put("2AB0", new Characteristic("2AB0", "Local North Coordinate"));
        characteristics.put("2A0F", new Characteristic("2A0F", "Local Time Information"));
        characteristics.put("2A67", new Characteristic("2A67", "Location and Speed Characteristic"));
        characteristics.put("2AB5", new Characteristic("2AB5", "Location Name", CharacteristicReadType.STRING));
        characteristics.put("2AAF", new Characteristic("2AAF", "Longitude"));
        characteristics.put("2A2C", new Characteristic("2A2C", "Magnetic Declination"));
        characteristics.put("2AA0", new Characteristic("2AA0", "Magnetic Flux Density – 2D"));
        characteristics.put("2AA1", new Characteristic("2AA1", "Magnetic Flux Density – 3D"));
        characteristics.put("2A29", new Characteristic("2A29", "Manufacturer Name String", CharacteristicReadType.STRING));
        characteristics.put("2A91", new Characteristic("2A91", "Maximum Recommended Heart Rate"));
        characteristics.put("2A21", new Characteristic("2A21", "Measurement Interval"));
        characteristics.put("2A24", new Characteristic("2A24", "Model Number String", CharacteristicReadType.STRING));
        characteristics.put("2A68", new Characteristic("2A68", "Navigation"));
        characteristics.put("2A3E", new Characteristic("2A3E", "Network Availability"));
        characteristics.put("2A46", new Characteristic("2A46", "New Alert"));
        characteristics.put("2AC5", new Characteristic("2AC5", "Object Action Control Point"));
        characteristics.put("2AC8", new Characteristic("2AC8", "Object Changed"));
        characteristics.put("2AC1", new Characteristic("2AC1", "Object First-Created"));
        characteristics.put("2AC3", new Characteristic("2AC3", "Object ID"));
        characteristics.put("2AC2", new Characteristic("2AC2", "Object Last-Modified"));
        characteristics.put("2AC6", new Characteristic("2AC6", "Object List Control Point"));
        characteristics.put("2AC7", new Characteristic("2AC7", "Object List Filter"));
        characteristics.put("2ABE", new Characteristic("2ABE", "Object Name", CharacteristicReadType.STRING));
        characteristics.put("2AC4", new Characteristic("2AC4", "Object Properties"));
        characteristics.put("2AC0", new Characteristic("2AC0", "Object Size"));
        characteristics.put("2ABF", new Characteristic("2ABF", "Object Type"));
        characteristics.put("2ABD", new Characteristic("2ABD", "OTS Feature"));
        characteristics.put("2A5F", new Characteristic("2A5F", "PLX Continuous Measurement Characteristic"));
        characteristics.put("2A60", new Characteristic("2A60", "PLX Features"));
        characteristics.put("2A5E", new Characteristic("2A5E", "PLX Spot-Check Measurement"));
        characteristics.put("2A50", new Characteristic("2A50", "PnP ID"));
        characteristics.put("2A75", new Characteristic("2A75", "Pollen Concentration"));
        characteristics.put("2A2F", new Characteristic("2A2F", "Position 2D"));
        characteristics.put("2A30", new Characteristic("2A30", "Position 3D"));
        characteristics.put("2A69", new Characteristic("2A69", "Position Quality"));
        characteristics.put(CHARACTERISTIC_PRESSURE.UUID, new Characteristic(CHARACTERISTIC_PRESSURE, CharacteristicReadType.INTEGER));
        characteristics.put("2A4E", new Characteristic("2A4E", "Protocol Mode"));
        characteristics.put("2A62", new Characteristic("2A62", "Pulse Oximetry Control Point"));
        characteristics.put("2A78", new Characteristic("2A78", "Rainfall"));
        characteristics.put("2B1D", new Characteristic("2B1D", "RC Feature"));
        characteristics.put("2B1E", new Characteristic("2B1E", "RC Settings"));
        characteristics.put("2B1F", new Characteristic("2B1F", "Reconnection Configuration Control Point"));
        characteristics.put("2A52", new Characteristic("2A52", "Record Access Control Point"));
        characteristics.put(CHARACTERISTIC_REFERENCE_TIME.UUID, new Characteristic(CHARACTERISTIC_REFERENCE_TIME, CharacteristicReadType.TIME));
        characteristics.put("2B37", new Characteristic("2B37", "Registered User Characteristic"));
        characteristics.put("2A3A", new Characteristic("2A3A", "Removable"));
        characteristics.put("2A4D", new Characteristic("2A4D", "Report"));
        characteristics.put("2A4B", new Characteristic("2A4B", "Report Map"));
        characteristics.put("2AC9", new Characteristic("2AC9", "Resolvable Private Address Only"));
        characteristics.put("2A92", new Characteristic("2A92", "Resting Heart Rate"));
        characteristics.put("2A40", new Characteristic("2A40", "Ringer Control point"));
        characteristics.put("2A41", new Characteristic("2A41", "Ringer Setting"));
        characteristics.put("2AD1", new Characteristic("2AD1", "Rower Data"));
        characteristics.put("2A54", new Characteristic("2A54", "RSC Feature"));
        characteristics.put("2A53", new Characteristic("2A53", "RSC Measurement"));
        characteristics.put("2A55", new Characteristic("2A55", "SC Control Point"));
        characteristics.put("2A4F", new Characteristic("2A4F", "Scan Interval Window"));
        characteristics.put("2A31", new Characteristic("2A31", "Scan Refresh"));
        characteristics.put("2A3C", new Characteristic("2A3C", "Scientific Temperature Celsius"));
        characteristics.put("2A10", new Characteristic("2A10", "Secondary Time Zone"));
        characteristics.put("2A5D", new Characteristic("2A5D", "Sensor Location"));
        characteristics.put("2A25", new Characteristic("2A25", "Serial Number String", CharacteristicReadType.STRING));
        characteristics.put("2B3A", new Characteristic("2B3A", "Server Supported Features"));
        characteristics.put("2A3B", new Characteristic("2A3B", "Service Required"));
        characteristics.put("2A28", new Characteristic("2A28", "Software Revision String", CharacteristicReadType.STRING));
        characteristics.put("2A93", new Characteristic("2A93", "Sport Type for Aerobic and Anaerobic Thresholds"));
        characteristics.put("2AD0", new Characteristic("2AD0", "Stair Climber Data"));
        characteristics.put("2ACF", new Characteristic("2ACF", "Step Climber Data"));
        characteristics.put("2A3D", new Characteristic("2A3D", "String", CharacteristicReadType.STRING));
        characteristics.put("2AD7", new Characteristic("2AD7", "Supported Heart Rate Range"));
        characteristics.put("2AD5", new Characteristic("2AD5", "Supported Inclination Range"));
        characteristics.put("2A47", new Characteristic("2A47", "Supported New Alert Category"));
        characteristics.put("2AD8", new Characteristic("2AD8", "Supported Power Range"));
        characteristics.put("2AD6", new Characteristic("2AD6", "Supported Resistance Level Range"));
        characteristics.put("2AD4", new Characteristic("2AD4", "Supported Speed Range"));
        characteristics.put("2A48", new Characteristic("2A48", "Supported Unread Alert Category"));
        characteristics.put("2A23", new Characteristic("2A23", "System ID"));
        characteristics.put("2ABC", new Characteristic("2ABC", "TDS Control Point"));
        characteristics.put(CHARACTERISTIC_TEMPERATURE.UUID, new Characteristic(CHARACTERISTIC_TEMPERATURE, CharacteristicReadType.INTEGER));
        characteristics.put("2A1F", new Characteristic("2A1F", "Temperature Celsius", CharacteristicReadType.INTEGER));
        characteristics.put("2A20", new Characteristic("2A20", "Temperature Fahrenheit", CharacteristicReadType.INTEGER));
        characteristics.put("2A1C", new Characteristic("2A1C", "Temperature Measurement", CharacteristicReadType.INTEGER));
        characteristics.put("2A1D", new Characteristic("2A1D", "Temperature Type"));
        characteristics.put("2A94", new Characteristic("2A94", "Three Zone Heart Rate Limits"));
        characteristics.put("2A12", new Characteristic("2A12", "Time Accuracy"));
        characteristics.put("2A15", new Characteristic("2A15", "Time Broadcast"));
        characteristics.put("2A13", new Characteristic("2A13", "Time Source"));
        characteristics.put("2A16", new Characteristic("2A16", "Time Update Control Point"));
        characteristics.put("2A17", new Characteristic("2A17", "Time Update State"));
        characteristics.put("2A11", new Characteristic("2A11", "Time with DST"));
        characteristics.put("2A0E", new Characteristic("2A0E", "Time Zone"));
        characteristics.put("2AD3", new Characteristic("2AD3", "Training Status"));
        characteristics.put("2ACD", new Characteristic("2ACD", "Treadmill Data"));
        characteristics.put("2A71", new Characteristic("2A71", "True Wind Direction"));
        characteristics.put("2A70", new Characteristic("2A70", "True Wind Speed"));
        characteristics.put("2A95", new Characteristic("2A95", "Two Zone Heart Rate Limit"));
        characteristics.put("2A07", new Characteristic("2A07", "Tx Power Level"));
        characteristics.put("2AB4", new Characteristic("2AB4", "Uncertainty"));
        characteristics.put("2A45", new Characteristic("2A45", "Unread Alert Status"));
        characteristics.put("2AB6", new Characteristic("2AB6", "URI"));
        characteristics.put("2A9F", new Characteristic("2A9F", "User Control Point"));
        characteristics.put("2A9A", new Characteristic("2A9A", "User Index"));
        characteristics.put("2A76", new Characteristic("2A76", "UV Index"));
        characteristics.put("2A96", new Characteristic("2A96", "VO2 Max"));
        characteristics.put("2A97", new Characteristic("2A97", "Waist Circumference"));
        characteristics.put("2A98", new Characteristic("2A98", "Weight"));
        characteristics.put("2A9D", new Characteristic("2A9D", "Weight Measurement"));
        characteristics.put("2A9E", new Characteristic("2A9E", "Weight Scale Feature"));
        characteristics.put("2A79", new Characteristic("2A79", "Wind Chill"));
        characteristics.put("F7BF3564-FB6D-4E53-88A4-5E37E0326063", new Characteristic("F7BF3564-FB6D-4E53-88A4-5E37E0326063", "Silicon Labs OTA Control"));

        descriptors.put("2905", "Characteristic Aggregate Format");
        descriptors.put("2900", "Characteristic Extended Properties");
        descriptors.put("2904", "Characteristic Presentation Format");
        descriptors.put("2901", "Characteristic User Description");
        descriptors.put(DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION.UUID, DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION.Name);
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

    public static final UUIDNamePair LEO_SERVER_V2_ALL_SENSOR_DATA = new UUIDNamePair("DBA51660-5986-44F4-B5A1-55d82EC69890", "All Sensor Data");
    public static final UUIDNamePair KIMCHI_V1_SENSOR_SERVICE = new UUIDNamePair("29a79111-cc0d-42b2-b545-dac14f3da422", "Sensor Data Service");
    public static final UUIDNamePair KIMCHI_V1_SENSOR_DATA = new UUIDNamePair("25353214-b080-48b7-bb96-2a49af082dbc", "Sensor Data");
    public static final UUIDNamePair KIMCHI_V1_ALL_SENSOR_DATA = new UUIDNamePair("8e8b58b3-7e23-42d9-805f-2d5e6b7bec64", "All Sensor Data");

    public static final Device LEO_SERVER_V1 =
        new Device("00:0B:57:1A:88:EF", "LEO SERVER V1", new ArrayList<Service>() {{
            add(new Service("28CCBA6E-B38E-472A-A26B-A1C1ED9320DA", "Test Sensor", new ArrayList<Characteristic>() {{
                add(new Characteristic("934DAF64-BBFF-47D2-8DF4-864E2589F019", "Temperature", CharacteristicReadType.INTEGER));
                add(new Characteristic("FBE1A7F9-F24F-4AE0-9967-41464757EEB9", "Humidity", CharacteristicReadType.INTEGER));
            }}));
        }});

    public static final Device LEO_SERVER_V2 =
        new Device("EC:1B:BD:1C:50:99", "LEO SERVER V2", new ArrayList<Service>() {{
            add(new Service(SERVICE_ENVIRONMENTAL_SENSING, new ArrayList<Characteristic>() {{
                add(new Characteristic(LEO_SERVER_V2_ALL_SENSOR_DATA, CharacteristicReadType.CUSTOM));
            }}));
        }});

    public static final Device KIMCHI_V1 =
        new Device("00:A0:50:00:00:00", "KIMCHI V1", new ArrayList<Service>() {{
            add(new Service(KIMCHI_V1_SENSOR_SERVICE, new ArrayList<Characteristic>() {{
                add(new Characteristic(KIMCHI_V1_SENSOR_DATA, CharacteristicReadType.CUSTOM));
                add(new Characteristic(KIMCHI_V1_ALL_SENSOR_DATA, CharacteristicReadType.CUSTOM));
            }}));
        }});

    public static final Device KIMCHI_V2 =
        new Device("84:2E:14:9D:6D:F4", "KIMCHI V2", new ArrayList<Service>() {{
            add(new Service(KIMCHI_V1_SENSOR_SERVICE, new ArrayList<Characteristic>() {{
                add(new Characteristic(KIMCHI_V1_SENSOR_DATA, CharacteristicReadType.CUSTOM));
                add(new Characteristic(KIMCHI_V1_ALL_SENSOR_DATA, CharacteristicReadType.CUSTOM));
            }}));
        }});

    static
    {
        devices.add(LEO_SERVER_V1);
        devices.add(LEO_SERVER_V2);
        devices.add(KIMCHI_V1);
        devices.add(KIMCHI_V2);
    }

    public static class UUIDNamePair
    {
        public String UUID;
        public String Name;

        public UUIDNamePair(String uuid, String name)
        {
            UUID = uuid;
            Name = name;
        }

        public String GetFullUUID()
        {
            return AddBaseUUID(UUID);
        }
    }

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
            for (Service s : Services)
            {
                if (s.UUID.equalsIgnoreCase(uuid)) return s;
            }

            return null;
        }
    }

    public static class Service
    {
        public String UUID;
        public String Name;
        public ArrayList<Characteristic> Characteristics;

        public Service(UUIDNamePair anp, ArrayList<Characteristic> characteristics)
        {
            this(AddBaseUUID(anp.UUID), anp.Name, characteristics);
        }

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
                if (c.UUID.equalsIgnoreCase(uuid)) return c;
            }

            return null;
        }
    }

    public static class Characteristic
    {
        public String UUID;
        public String Name;
        public CharacteristicReadType ReadType;

        public Characteristic(String uuid, String name)
        {
            this(uuid, name, DEFAULT_CHARACTERISTIC_TYPE);
        }

        public Characteristic(UUIDNamePair anp, CharacteristicReadType readType)
        {
            this(AddBaseUUID(anp.UUID), anp.Name, readType);
        }

        public Characteristic(String uuid, String name, CharacteristicReadType readType)
        {
            this.UUID = uuid;
            this.Name = name;
            this.ReadType = readType;
        }
    }

    public static class Devices
    {
        public static Device Get(String mac)
        {
            for (Device d : devices)
            {
                if (d.MAC.equalsIgnoreCase(mac)) return d;
            }

            return null;
        }
    }

    public static class Services
    {
        public static String Get(String serviceUUID)
        {
            if (serviceUUID == null) return null;
            serviceUUID = serviceUUID.toUpperCase();

            String uuidSubstring = GetUUIDSubstring(serviceUUID);
            String s = services.get(uuidSubstring);

            if (s != null) return s;

            // Try retrieving the service by full uuid (less common)
            return services.get(serviceUUID);
        }

        public static String Get(String mac, String serviceUUID)
        {
            if (mac != null)
            {
                Device d = Devices.Get(mac);
                if (d != null)
                {
                    Service s = d.GetService(serviceUUID);
                    if (s != null) return s.Name;
                }
            }

            return Get(serviceUUID);
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

            if (c != null) return c;

            // Try retrieving the characteristic by full uuid (less common)
            return characteristics.get(characteristicUUID);
        }

        public static Characteristic Get(String device, String serviceUUID, String characteristicUUID)
        {
            if (device != null)
            {
                Device d = Devices.Get(device);
                if (d != null)
                {
                    Service s = d.GetService(serviceUUID);
                    if (s != null)
                    {
                        Characteristic c = s.GetCharacteristic(characteristicUUID);
                        if (c != null) return c;
                    }
                }
            }

            return Get(characteristicUUID);
        }
    }

    // Returns a substring of the UUID, specifically these (x):
    // 0000xxxx-0000-1000-8000-00805F9B34FB
    // This is because the gatt specifications only list the assigned number as these 4 digits of the UUID:
    // https://www.bluetooth.com/specifications/gatt
    private static String GetUUIDSubstring(String uuid)
    {
        if (uuid == null) return "";

        // If UUID is already a substring, just return it.
        if (uuid.length() == 4)
        {
            return uuid;
        }

        if (!HasBaseUUID(uuid)) return "";

        return uuid.substring(4, 8);
    }

    public static String AddBaseUUID(String uuid)
    {
        if (uuid.length() != 4)
        {
            return uuid;
        }

        return BASE_UUID.substring(0, 4) + uuid.toUpperCase() + BASE_UUID.substring(8);
    }

    private static boolean HasBaseUUID(String uuid)
    {
        // Ensure UUID is of proper length (slashes included).
        if (uuid.length() != 36) return false;
        uuid = uuid.toUpperCase();

        // Ensure UUID is formed with proper base address.
        for (int i = 0; i < BASE_UUID.length(); i++)
        {
            if (BASE_UUID.charAt(i) == 'x') continue;
            if (uuid.charAt(i) != BASE_UUID.charAt(i)) return false;
        }

        return true;
    }
}
