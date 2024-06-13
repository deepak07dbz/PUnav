package com.example.accelerometer;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.accelerometer.data.PoiHelper;
import com.example.accelerometer.data.PoiModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.oscim.android.MapView;
import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapActions {
    private Activity activity;
    private FloatingActionButton fabLocation, fabSettings, fabSearch;
    public TextView xAxis, yAxis, zAxis, lat, lon;
    private PoiHelper poiHelper;
    private AutoCompleteTextView autoStart, autoEnd;
    String[] locationNames;

    public MapActions(Activity activity) {
        this.activity = activity;
        poiHelper = new PoiHelper(activity);
        this.fabLocation = activity.findViewById(R.id.locationFAB);
        this.fabSettings = activity.findViewById(R.id.settingsFAB);
        this.fabSearch = activity.findViewById(R.id.searchFAB);
        this.xAxis = activity.findViewById(R.id.xaxis);
        this.yAxis = activity.findViewById(R.id.yaxis);
        this.zAxis = activity.findViewById(R.id.zaxis);
        this.lat = activity.findViewById(R.id.lat);
        this.lon = activity.findViewById(R.id.lon);
        this.autoStart = activity.findViewById(R.id.autoStart);
        this.autoStart.setThreshold(1);
        this.autoEnd = activity.findViewById(R.id.autoEnd);
        this.autoEnd.setThreshold(1);
        locationNames = new String[2];
        initSettingsButton();
        initLocationButton();
        onSearch();
        initSearchButton();
    }

    private void setupAutoComplete(AutoCompleteTextView textView, String[] locationNames, int position) {
        textView.setThreshold(1);
        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                List<String> suggestions = searchLocation(charSequence.toString());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(textView.getContext(), android.R.layout.simple_dropdown_item_1line, suggestions);
                textView.setAdapter(adapter);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String poiName = (String) adapterView.getItemAtPosition(0);
                locationNames[position] = poiName;
            }
        });
    }

    private List<String> searchLocation(String name) {
        return poiHelper.searchLocations(name);
    }

    private void onSearch() {
        setupAutoComplete(autoStart, locationNames, 0);
        setupAutoComplete(autoEnd, locationNames, 1);
    }

    private void initSearchButton() {
        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (locationNames[1] == null) {
                    Toast.makeText(activity, "enter destination", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (locationNames[0] == null) {
                    Location from = MapActivity.userLocation;
                    PoiModel to = poiHelper.search(locationNames[1].toLowerCase());
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
                    PoiModel from = poiHelper.search(locationNames[0].toLowerCase());
                    PoiModel to = poiHelper.search(locationNames[1].toLowerCase());
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
