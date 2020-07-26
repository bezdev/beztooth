package com.beztooth.Util;

public class Util
{

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
}
