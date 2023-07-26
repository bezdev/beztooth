package com.beztooth.UI.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.UI.Util.BezButton;
import com.beztooth.UI.Util.BezContainer;
import com.beztooth.UI.Util.ViewInputHandler;
import com.beztooth.Util.Util;

public class Beztooth extends BluetoothActivity
{
    private class SelectActivity
    {
        public int Name;
        public Class Class;

        public SelectActivity(int name, Class className)
        {
            Name = name;
            Class = className;
        }
    }

    private class ImageActivity extends SelectActivity {
        public int Image;

        public ImageActivity(int name, Class className, int image)
        {
            super(name, className);
            Image = image;
        }
    }

    SelectActivity[] SELECT_ACTIVITIES = new SelectActivity[]
    {
        new SelectActivity(R.string.scan, DevicesActivity.class),
    };

    ImageActivity[] IMAGE_ACTIVITIES = new ImageActivity[]
    {
        new ImageActivity(R.string.temperature, ThermometerActivity.class, R.drawable.ic_launcher_thermometer),
        new ImageActivity(R.string.sync_clock, SyncClockActivity.class, R.drawable.ic_launcher_sync_clock),
        new ImageActivity(R.string.garage_door, GarageDoorActivity.class, R.mipmap.ic_launcher_garage_door),
        new ImageActivity(R.string.kimchi, KimchiActivity.class, R.drawable.ic_launcher_kimchi),
        new ImageActivity(R.string.counter, CounterActivity.class, R.drawable.ic_launcher_counter),
        new ImageActivity(R.string.marquee, MarqueeActivity.class, R.drawable.ic_launcher_marquee),
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
        for (SelectActivity activity : SELECT_ACTIVITIES)
        {
            LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            BezButton view = (BezButton) inflater.inflate(R.layout.activity_select, null);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = Util.PixelToDP(getWindowManager(), 20);
            layoutParams.topMargin = margin;
            layoutParams.leftMargin = margin;
            layoutParams.rightMargin = margin;
            layoutParams.bottomMargin = margin;
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

        GridLayout grid = findViewById(R.id.activity_grid);
        for (ImageActivity activity : IMAGE_ACTIVITIES)
        {
            BezContainer activitySelect = (BezContainer) m_LayoutInflater.inflate(R.layout.activity_select_image, null);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = Util.PixelToDP(getWindowManager(), 5);
            int size = Util.PixelToDP(getWindowManager(), 100);
            layoutParams.topMargin = margin;
            layoutParams.leftMargin = margin;
            layoutParams.rightMargin = margin;
            layoutParams.bottomMargin = margin;
            layoutParams.height = size;
            layoutParams.width = size;
            activitySelect.setLayoutParams(layoutParams);
            ImageView image = activitySelect.findViewById(R.id.select_image);
            image.setImageResource(activity.Image);
            TextView text = activitySelect.findViewById(R.id.select_text);
            text.setText(activity.Name);

            final Class finalClass = activity.Class;
            activitySelect.SetOnClick(new ViewInputHandler.OnClick()
            {
                @Override
                public void Do(View view)
                {
                    Intent intent = new Intent(view.getContext(), finalClass);
                    view.getContext().startActivity(intent);
                }
            });

            grid.addView(activitySelect);
        }
    }
}
