package com.beztooth.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.beztooth.Bluetooth.ConnectionManager;
import com.beztooth.R;

public class Beztooth extends BluetoothActivity
{
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

    @Override
    protected void OnConnectionManagerConnected()
    {

    }

    private void AddEventListeners()
    {
        // Set event listeners
        BezButton button = findViewById(R.id.scanButton);
        button.SetOnClick(new ViewInputHandler.OnClick()
        {
            @Override
            public void Do(View view)
            {
                Intent intent = new Intent(view.getContext(), DevicesActivity.class);
                view.getContext().startActivity(intent);
            }
        });

        button = findViewById(R.id.clockSyncButton);
        button.SetOnClick(new ViewInputHandler.OnClick()
        {
            @Override
            public void Do(View view)
            {
                Intent intent = new Intent(view.getContext(), SyncClockActivity.class);
                view.getContext().startActivity(intent);
            }
        });
    }
}
