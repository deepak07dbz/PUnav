package com.example.accelerometer;


import static com.example.accelerometer.MapActivity.LOCATION_DELAY;
import static com.example.accelerometer.MapActivity.SENSOR_DELAY;
import static com.example.accelerometer.MapActivity.TIME_DELAY;
import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
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

import com.example.accelerometer.data.RecordsModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DataService extends Service implements SensorEventListener {
    public static Location curLocation;
    private SensorManager sensorManager;
    private long lastUpdateSensor = 0;
    private  long lastUpdateTime = 0;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FileWriter fileWriter;
    private SharedPreferences sharedPreferences;
    private static final String UNIQUE_WORK = "DataDumpWork";

    public static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
    public static final String ACTION_STOP_APP = "ACTION_STOP_APP";

    private final BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
                stopService();
            }
        }
    };

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
        initLocation();
        try {
            fileWriter = new FileWriter(new File(getApplicationContext().getFilesDir(), "sensor_location_data.txt"), true);
        } catch (IOException e) {
            Log.d("SERVICE_FILE", "onCreate: failed to open file for writing");
            e.printStackTrace();
        }
        IntentFilter filter = new IntentFilter(ACTION_STOP_SERVICE);
        registerReceiver(stopServiceReceiver, filter);
    }

    private void stopService() {
        stopForeground(true);
        stopSelf();
        WorkManager.getInstance(getApplicationContext()).cancelUniqueWork("DataInsertionWork");
        Intent stopApp = new Intent(ACTION_STOP_APP);
        sendBroadcast(stopApp);
    }

    private void initLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                curLocation = location;
                getLocationUpdates();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(stopServiceReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopService();
            return START_NOT_STICKY;
        }
        Log.d("SERVICE", "onStartCommand: service started");
        startForeground(1, getNotification());
        getLocationUpdates();
        scheduleDataWorker(this);
        return START_STICKY;
    }

    private Notification getNotification() {
        Intent stopSelfIntent = new Intent(this, DataService.class);
        stopSelfIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent pStopSelf = PendingIntent.getService(this, 0, stopSelfIntent, PendingIntent.FLAG_IMMUTABLE);
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
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(R.drawable.ic_launcher_foreground, "Stop", pStopSelf);
        return builder.build();
    }

    private void getLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, sharedPreferences.getInt("location", 1000)).setMinUpdateIntervalMillis(sharedPreferences.getInt("location", 1000)).build();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    Log.d("SERVICE", "onLocationResult: " + location.getLatitude() + location.getLongitude());
                    curLocation = location;
                    writeDataToFile(new RecordsModel(location.getLongitude(), location.getLatitude()));
                }
            }
        }, Looper.getMainLooper());
    }

    private void writeDataToFile(RecordsModel recordsModel) {
        try {
            fileWriter = new FileWriter(new File(getApplicationContext().getFilesDir(), "sensor_location_data.txt"), true);
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
            if (currentTime - lastUpdateSensor >= sharedPreferences.getInt("sensor", 50)) {
                lastUpdateSensor = currentTime;
                processSensorData(sensorEvent);
            }
            if (currentTime - lastUpdateTime >= sharedPreferences.getInt("timeStamp", 10000)) {
                lastUpdateTime = currentTime;
                writeDataToFile(new RecordsModel(sensorEvent.timestamp));
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
                new PeriodicWorkRequest.Builder(DataWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
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
