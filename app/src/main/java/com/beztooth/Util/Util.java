package com.beztooth.Util;

import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.graphics.Color.rgb;

public class Util
{
    public static final float MMHG_PER_PA = 0.00750062f;

    public static class Color
    {
        public int R;
        public int G;
        public int B;

        public Color(int r, int g, int b)
        {
            R = r;
            G = g;
            B = b;
        }

        public int GetColor()
        {
            return rgb(R, G, B);
        }

        public static int GetColorInSpectrum(Color start, Color end, float percent)
        {
            if (percent > 100) percent = 100;

            Color c = new Color(
                Math.round(start.R + (percent / 100.f * (end.R - start.R))),
                Math.round(start.G + (percent / 100.f * (end.G - start.G))),
                Math.round(start.B + (percent / 100.f * (end.B - start.B))));

            return c.GetColor();
        }
    }

    public static String GetCallerName()
    {
        String[] blacklistedMethods = new String[] { "log", "dalvik.system.vmstack", "thread.getstacktrace", "util.getcallername", "device.access", "gattaction.do", ".do(devicesactivity", "device.dequeuegattaction", "device.queuegattaction" };

        boolean thisMethodFound = false;
        for (StackTraceElement s : Thread.currentThread().getStackTrace())
        {
            String methodName = s.toString();
            boolean isBlacklistedMethod = false;

            // Ignore all other logging methods to find caller
            for (String blacklistedMethod : blacklistedMethods)
            {
                if (methodName.toLowerCase().contains(blacklistedMethod))
                {
                    isBlacklistedMethod = true;
                    break;
                };
            }

            if (!isBlacklistedMethod) return methodName;
        }

        return "";
    }

    public static boolean IsBufferZero(byte[] buffer)
    {
        for (byte b : buffer)
        {
            if (b != 0)
            {
                return false;
            }
        }

        return true;
    }

    private static byte GetDayCode(int day)
    {
        switch (day)
        {
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

    public static byte[] GetTimeInBytes(long timestamp)
    {
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
        else if (type == Constants.CharacteristicReadType.HEX || type == Constants.CharacteristicReadType.CUSTOM)
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
            String result = "" + ByteBuffer.allocate(4).put(data).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
            if (result.isEmpty()) return "0";

            return result;
        }
        else if (type == Constants.CharacteristicReadType.TIME)
        {
            if (data.length != 10) return "";

            int year = (data[0] & 0xFF) + ((data[1] & 0xFF) << 8);

            return String.format(Locale.getDefault(), "%02d/%02d/%d %02d:%02d:%02d", data[2], data[3], year, data[4], data[5], data[6]);
        }
        else if (type == Constants.CharacteristicReadType.TIME_HMS)
        {
            if (data.length != 3) return "";

            return String.format(Locale.getDefault(), "%02d:%02d:%02d", data[0], data[1], data[2]);
        }

        return "";
    }

    public static byte[] GetByteArrayFromInteger(int value, int bytes)
    {
        return ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    public static Date GetDateFromData(byte[] data)
    {
        try
        {
            SimpleDateFormat dateParser = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            return dateParser.parse(GetDataString(data, Constants.CharacteristicReadType.TIME));
        }
        catch (ParseException e)
        {
            return null;
        }
    }

    public static String GetTimeFromSeconds(int time) {
        int seconds = time % 60;
        int minutes = (time % (60 * 60)) / 60;
        int hours = (time / (60 * 60)) % 24;
        int days = time / (60 * 60 * 24);

        String result = String.format("%ds", seconds);
        if (minutes > 0) result = String.format("%dm:", minutes) + result;
        if (hours > 0) result = String.format("%dh:", hours) + result;
        if (days > 0) result = String.format("%dd:", days) + result;
        return result;
    }

    public static float ConvertPascalToMMHG(float pascal)
    {
        return pascal * MMHG_PER_PA;
    }

    public static int PixelToDP(WindowManager windowManager, int pixels)
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return (int) (pixels * displayMetrics.density);
    }

    public static void SetTextViewBoldMonospace(TextView textView) {
        textView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        textView.setLetterSpacing(-.1f);
    }
}
