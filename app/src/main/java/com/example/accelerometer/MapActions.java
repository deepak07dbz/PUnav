package com.example.accelerometer;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.oscim.android.MapView;

public class MapActions {
    private Activity activity;
    private FloatingActionButton fabLocation, fabSettings;
    public TextView xAxis, yAxis, zAxis, lat, lon;
    public MapActions(Activity activity, MapView mapView) {
        this.activity = activity;
        this.fabLocation = activity.findViewById(R.id.locationFAB);
        this.fabSettings = activity.findViewById(R.id.settingsFAB);
        this.xAxis = activity.findViewById(R.id.xaxis);
        this.yAxis = activity.findViewById(R.id.yaxis);
        this.zAxis = activity.findViewById(R.id.zaxis);
        this.lat = activity.findViewById(R.id.lat);
        this.lon = activity.findViewById(R.id.lon);
        initSettingsButton();
        initLocationButton();
    }

    private void initLocationButton() {
        fabLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
            }
        });
    }
    private void initSettingsButton() {
        fabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, SettingsActivity.class);
                activity.startActivity(intent);
            }
        });
    }
}
