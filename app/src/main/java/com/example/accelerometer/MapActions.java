package com.example.accelerometer;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.accelerometer.data.PoiHelper;
import com.example.accelerometer.data.PoiModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.oscim.android.MapView;
import org.oscim.core.GeoPoint;

import java.util.Map;

public class MapActions {
    private Activity activity;
    private FloatingActionButton fabLocation, fabSettings, fabSearch;
    private EditText start, end;
    public TextView xAxis, yAxis, zAxis, lat, lon;
    private PoiHelper poiHelper;

    public MapActions(Activity activity) {
        this.activity = activity;
        poiHelper = new PoiHelper(activity);
        this.fabLocation = activity.findViewById(R.id.locationFAB);
        this.fabSettings = activity.findViewById(R.id.settingsFAB);
        this.fabSearch = activity.findViewById(R.id.searchFAB);
        this.start = activity.findViewById(R.id.source);
        this.end = activity.findViewById(R.id.dest);
        this.xAxis = activity.findViewById(R.id.xaxis);
        this.yAxis = activity.findViewById(R.id.yaxis);
        this.zAxis = activity.findViewById(R.id.zaxis);
        this.lat = activity.findViewById(R.id.lat);
        this.lon = activity.findViewById(R.id.lon);
        initSettingsButton();
        initLocationButton();
        initSearchButton();
    }

    private void initSearchButton() {
        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (end.getText().toString().isEmpty()) {
                    Toast.makeText(activity, "enter destination", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (start.getText().toString().isEmpty()) {
                    Location from = MapActivity.userLocation;
                    Log.d("SEARCH", "onClick: " + end.getText().toString());
                    Log.d("SEARCH", "result: " + poiHelper.search(end.getText().toString()));
                    PoiModel to = poiHelper.search(end.getText().toString().toLowerCase());
                    if (from != null && to != null) {
                        MapActivity.getInstance().calcPath(from.getLatitude(), from.getLongitude(), to.getLat(), to.getLon(), activity);
                    }else {
                        if (from == null)
                            Toast.makeText(activity, "source not found", Toast.LENGTH_SHORT).show();
                        if (to == null)
                            Toast.makeText(activity, "destination not found", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    PoiModel from = poiHelper.search(start.getText().toString().toLowerCase());
                    PoiModel to = poiHelper.search(end.getText().toString().toLowerCase());
                    if (from != null && to != null) {
                        MapActivity.getInstance().calcPath(from.getLat(), from.getLon(), to.getLat(), to.getLon(), activity);
                    }else {
                        if (from == null)
                            Toast.makeText(activity, "source not found", Toast.LENGTH_SHORT).show();
                        if (to == null)
                            Toast.makeText(activity, "destination not found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void initLocationButton() {
        fabLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(MapActivity.userLocation != null) {
                    MapActivity.getCenter(new GeoPoint(MapActivity.userLocation.getLatitude(), MapActivity.userLocation.getLongitude()), 0, 0, 0);
                }
                else {
                    Toast.makeText(activity, "Location not found", Toast.LENGTH_SHORT).show();
                }
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
