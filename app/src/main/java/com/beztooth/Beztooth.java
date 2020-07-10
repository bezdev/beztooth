package com.beztooth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Beztooth extends BluetoothActivity
{
    public void Scan(View view)
    {
        Intent intent = new Intent(view.getContext(), DevicesActivity.class);
        view.getContext().startActivity(intent);
    }

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

        Intent intent = new Intent(this, ConnectionManager.class);
        startService(intent);
    }
}
