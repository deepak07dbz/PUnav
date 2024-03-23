package com.example.accelerometer;

import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.accelerometer.data.Helper;
import com.example.accelerometer.data.RecordsModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSIONS_FINE_LOCATION = 100;

    //default delays
    public static int LOCATION_DELAY = 1000;        //in milliseconds
    public static int TIME_DELAY = 10;             //in records
    public static int SENSOR_DELAY = 100;          //in milliseconds

    TextView x, y, z, lat, lon, currentDelays;
    Button stop, del, start;
    FloatingActionButton settings;
    ListView records;
    private double xval = 0, yval = 0, zval = 0;
    private boolean shouldRecord;
    private boolean isShow;
    RecordsModel recordsModel;
    Helper helper;
    ArrayAdapter<RecordsModel> readingArrayAdapter;
    List<RecordsModel> everyone;
    private FusedLocationProviderClient fusedLocationClient;
    Location currentLocation;
    LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private List<RecordsModel> temp;
    long recordCounter;
    long lastUpdateSensor;
    long lastUpdateLocation;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        x = findViewById(R.id.txt1);
        y = findViewById(R.id.txt2);
        z = findViewById(R.id.txt3);
        lat = findViewById(R.id.txt4);
        lon = findViewById(R.id.txt5);
        stop = findViewById(R.id.button);
        del = findViewById(R.id.del);
        start = findViewById(R.id.start);
        records = findViewById(R.id.Lview);
        settings = findViewById(R.id.fab);
        currentDelays = findViewById(R.id.txtdelay);

        x.setText("0.00");
        y.setText("0.00");
        z.setText("0.00");

        temp = new ArrayList<>();
        shouldRecord = false;
        isShow = false;
        lastUpdateSensor = 0;
        lastUpdateLocation = 0;

        //shared preferences
        Context context = getApplicationContext();
        sharedPref = context.getSharedPreferences("Setting_values", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("sensor", SENSOR_DELAY);
        editor.putInt("location", LOCATION_DELAY);
        editor.putInt("timeStamp", TIME_DELAY);
        editor.putInt("defaultS", 1);
        editor.putInt("defaultL", 1);
        editor.putInt("defaultT", 1);
        editor.apply();

        //timestamp
        recordCounter = 0;

        //Initializing Location and Updating Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, sharedPref.getInt("location", 1000))
                .setIntervalMillis(sharedPref.getInt("location", 1000))
//                .setMaxUpdateDelayMillis(LOCATION_DELAY + 1000)
                .setMinUpdateIntervalMillis(sharedPref.getInt("location", 1000))
                .build();
        lastLocation();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
               // long current = System.currentTimeMillis();
                if (locationResult.getLastLocation() != null) {
//                    if(current - lastUpdateLocation >= LOCATION_DELAY) {
//                        lastUpdateLocation = current;

                        currentLocation = locationResult.getLastLocation();
                        updateLocation();
                    //}
                }else {
                    Toast.makeText(MainActivity.this, "last location was null!", Toast.LENGTH_SHORT).show();
                }
            }
        };

        //initialising Sensor services
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);


        //display current records in db
        helper = new Helper(MainActivity.this);
        listRecords(helper);

        //Button listeners
        start.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shouldRecord = true;
                isShow = true;
                Toast.makeText(MainActivity.this, "Recording...", Toast.LENGTH_SHORT).show();
                listRecords(helper);
            }
        });
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shouldRecord = false;
                isShow = false;
                helper = new Helper(MainActivity.this);
                helper.deleteAll();
                Toast.makeText(MainActivity.this, "Records deleted.", Toast.LENGTH_SHORT).show();
                listRecords(helper);

            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                shouldRecord = false;
                Toast.makeText(MainActivity.this, "Recording stopped.", Toast.LENGTH_SHORT).show();
                isShow = false;

            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    //database related
    private void listRecords(Helper helper) {
        readingArrayAdapter = new ArrayAdapter<RecordsModel>(MainActivity.this, android.R.layout.simple_list_item_1, helper.getEveyrone());
        records.setAdapter(readingArrayAdapter);
    }
    private void addData(RecordsModel recordsModel){
        if(temp.size() != 100){
            if(recordCounter % sharedPref.getInt("timeStamp", 10) == 0){
                temp.add(new RecordsModel(recordCounter));
                recordCounter++;
            }
            temp.add(recordsModel);
            recordCounter++;
        }else {
            int i = 0;
            while(i < temp.size()){
                helper = new Helper(MainActivity.this);
                helper.addOne(temp.get(i));
                i++;
            }
            temp.clear();
        }
    }

    //sensor updates
    private void processSensorData(SensorEvent sensorEvent) {
        if (shouldRecord) {
            addData(new RecordsModel(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
            listRecords(helper);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateSensor >= sharedPref.getInt("sensor", 100)) {
                lastUpdateSensor = currentTime;

                if (isShow) {
                    x.setText(String.valueOf(sensorEvent.values[0]));
                    y.setText(String.valueOf(sensorEvent.values[1]));
                    z.setText(String.valueOf(sensorEvent.values[2]));
                }
        //        xval = sensorEvent.values[0];
        //        yval = sensorEvent.values[1];
        //        zval = sensorEvent.values[2];
                processSensorData(sensorEvent);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lastLocation();
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Need precise location", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //location updates
    private void lastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
        }else {

            Task<Location> task = fusedLocationClient.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        currentLocation = location;
                        updateLocation();
                        startLocationUpdates();
                    } else {
                        Toast.makeText(MainActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    private void updateLocation() {
        lat.setText(String.valueOf(currentLocation.getLatitude()));
        lon.setText(String.valueOf(currentLocation.getLongitude()));
        if (shouldRecord) {
            addData(new RecordsModel(currentLocation.getLongitude(), currentLocation.getLatitude()));
        }
    }

    private void startLocationUpdates() {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            } else {
                Toast.makeText(this, "Check permissions", Toast.LENGTH_SHORT).show();
            }
    }

}
