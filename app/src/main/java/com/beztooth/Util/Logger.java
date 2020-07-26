package com.beztooth.Util;

import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;

public class Logger
{
    public static final boolean DEBUG = true;
    public static final boolean LOG_THREAD_ID = DEBUG && true;
    public static final boolean USE_STDIO = DEBUG && false;

    private static final String TAG = "bezlog";

    private static HashMap<String, LinkedList<Long>> s_MethodThreads = new HashMap<>();

    public static void Debug(String tag, String message)
    {
        if (Logger.DEBUG)
        {
            if (LOG_THREAD_ID)
            {
                long tid = Thread.currentThread().getId();
                message = String.format("[tid:%s] [%s]", tid, message);

                String methodName = Util.GetCallerName();
                if (!s_MethodThreads.containsKey(methodName)) s_MethodThreads.put(methodName, new LinkedList<Long>());

                LinkedList<Long> tids = s_MethodThreads.get(methodName);
                if (!tids.contains(tid)) tids.add(tid);
            }

            if (USE_STDIO)
            {
                System.out.println(String.format("[%s] [%s]: %s", TAG, tag, message));
            }
            else
            {
                Log.d(tag, message);
            }
        }
    }

    public static void Error(String message) {
        Log.e(TAG, message);
    }
}
