package com.beztooth;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class Beztooth extends AppCompatActivity
{
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beztooth);

        // determine whether BLE is supported on the device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!hasLocationPermission())
        {
            Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent intent = new Intent(this, ConnectionManager.class);
        startService(intent);
    }

    private boolean hasLocationPermission()
    {
        if (Build.VERSION.SDK_INT >= 23)
        {
            // Marshmallow+ Permission APIs
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // don't scan
                } else {
                    Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
