package com.beztooth.UI.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.UI.Util.BezButton;
import com.beztooth.UI.Util.ViewInputHandler;
import com.beztooth.Util.Util;

public class Beztooth extends BluetoothActivity
{
    private class Activity
    {
        public int Name;
        public Class Class;

        public Activity(int name, Class className)
        {
            Name = name;
            Class = className;
        }
    }

    Activity[] ACTIVITIES = new Activity[]
    {
        new Activity(R.string.scan, DevicesActivity.class),
        new Activity(R.string.sync_clock, SyncClockActivity.class),
        new Activity(R.string.thermometer, ThermometerActivity.class),
        new Activity(R.string.garage_door, GarageDoorActivity.class),
        new Activity(R.string.kimchi, KimchiActivity.class),
        new Activity(R.string.counters, CounterActivity.class)
    };

    // MAIN
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beztooth);

        AddButtons();

        Intent intent = new Intent(this, ConnectionManager.class);
        startService(intent);
    }

    @Override
    protected void OnConnectionManagerConnected()
    {

    }

    private void AddButtons()
    {
        LinearLayout parent = (LinearLayout) findViewById(R.id.activity_scroll);

        for (Activity activity : ACTIVITIES)
        {
            LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            BezButton view = (BezButton) inflater.inflate(R.layout.activity_select, null);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = Util.PixelToDP(getWindowManager(), 20);
            layoutParams.topMargin = margin;
            layoutParams.leftMargin = margin;
            layoutParams.rightMargin = margin;
            view.setLayoutParams(layoutParams);

            final Class finalClass = activity.Class;
            view.setText(activity.Name);
            view.SetOnClick(new ViewInputHandler.OnClick()
            {
                @Override
                public void Do(View view)
                {
                    Intent intent = new Intent(view.getContext(), finalClass);
                    view.getContext().startActivity(intent);
                }
            });

            parent.addView(view);
        }
    }
}
