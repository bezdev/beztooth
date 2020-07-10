package com.beztooth.Util;

import android.util.Log;

public class Logger
{
    public static final boolean DEBUG = true;
    public static final boolean LOG_THREAD_ID = true;
    public static final boolean USE_STDIO = false;

    private static final String TAG = "bezlog";

    public static void Debug(String tag, String message)
    {
        if (Logger.DEBUG)
        {
            if (LOG_THREAD_ID)
            {
                message = String.format("[tid:%s] [%s]", Thread.currentThread().getId(), message);
            }

            if (USE_STDIO)
            {
                System.out.println(String.format("[%s] [%s]: %s", TAG, tag, message));
            }
            else
            {
                Log.d("TAG", String.format("[%s]: %s", tag, message));
            }
        }
    }

    public static void Error(String message) {
        Log.e(TAG, message);
    }
}
