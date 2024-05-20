package com.example.accelerometer;

import static com.example.accelerometer.MainActivity.LOCATION_DELAY;
import static com.example.accelerometer.MainActivity.SENSOR_DELAY;
import static com.example.accelerometer.MainActivity.TIME_DELAY;
import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.impl.utils.ForceStopRunnable;

import com.example.accelerometer.data.RecordsModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DataService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private long lastUpdateSensor = 0;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FileWriter fileWriter;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SERVICE", "onCreate: service created");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //listener for shared preferences changes
        sharedPreferences = this.getSharedPreferences("Setting_values", Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                updateSharedPref();
            }
        });
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            fileWriter = new FileWriter(new File(getApplicationContext().getFilesDir(), "sensor_location_data.txt"), true);
        } catch (IOException e) {
            Log.d("SERVICE_FILE", "onCreate: failed to open file for writing");
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("SERVICE", "onStartCommand: service started");
        startForeground(1, getNotification());
        getLocationUpdates();
        scheduleDataWorker(this);
        return START_STICKY;
    }

    private Notification getNotification() {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("channel_id", "Foreground Service", NotificationManager.IMPORTANCE_DEFAULT);
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("accelerometer")
                .setContentText("Collecting sensor and location data")
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        return builder.build();
    }

    private void getLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, sharedPreferences.getInt("location", 1000)).setMinUpdateIntervalMillis(sharedPreferences.getInt("location", 1000)).build();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    Log.d("SERVICE", "onLocationResult: " + location.getLatitude() + location.getLongitude());
                    writeDataToFile(new RecordsModel(location.getLongitude(), location.getLatitude()));
                }
            }
        }, Looper.getMainLooper());
    }

    private void writeDataToFile(RecordsModel recordsModel) {
        try {
            fileWriter.write(recordsModel.serialize() + "\n");
            fileWriter.flush();
        } catch (IOException e) {
            Log.d("SERVICEFILE", "writeDataToFile: unable to write");
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateSensor >= sharedPreferences.getInt("sensor", 100)) {
                lastUpdateSensor = currentTime;
                processSensorData(sensorEvent);
            }
        }
    }

    private void processSensorData(SensorEvent sensorEvent) {
        writeDataToFile(new RecordsModel(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void scheduleDataWorker(Context context) {
        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(DataWorker.class, 1, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DataDumpWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    private void updateSharedPref() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("sensor", SENSOR_DELAY);
        editor.putInt("location", LOCATION_DELAY);
        editor.putInt("timeStamp", TIME_DELAY);
        editor.apply();
    }

}