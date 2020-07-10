package com.beztooth.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;
import com.beztooth.TestDeviceActivity;

public class Beztooth extends BluetoothActivity
{
    public void TestDevice(View view)
    {
        Intent intent = new Intent(view.getContext(), TestDeviceActivity.class);
        view.getContext().startActivity(intent);
    }

    // MAIN
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beztooth);

        AddEventListeners();

        Intent intent = new Intent(this, ConnectionManager.class);
        startService(intent);
    }

    private void AddEventListeners()
    {
        // Set event listeners
        BezButton button = (BezButton)findViewById(R.id.scanButton);
        button.SetOnClick(new BezButton.OnClick()
        {
            @Override
            public void Do(View view)
            {
                Intent intent = new Intent(view.getContext(), DevicesActivity.class);
                view.getContext().startActivity(intent);
            }
        });
    }
}
