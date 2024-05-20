package com.example.accelerometer;

import static com.example.accelerometer.FileUtils.copyAssetsToStorage;
import static com.example.accelerometer.MainActivity.LOCATION_DELAY;
import static com.example.accelerometer.MainActivity.SENSOR_DELAY;
import static com.example.accelerometer.MainActivity.TIME_DELAY;
import static com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY;
import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;
import static com.google.android.gms.location.Priority.PRIORITY_LOW_POWER;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.accelerometer.data.Helper;
import com.example.accelerometer.data.RecordsModel;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.Constants;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;

import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.layers.vector.PathLayer;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import java.io.File;
import java.nio.file.Path;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class MapActivity extends AppCompatActivity implements SensorEventListener {

    public boolean markerAdded = false;
    private boolean permit = false;
    private int addOnce = 0;
    private static MapView mapView;
    private GraphHopper hopper;
    private GeoPoint start;
    private GeoPoint end;
    private static MapPosition tmpPos = new MapPosition();
    private volatile boolean prepareInProgress = false;
    private volatile boolean shortestPathRunning = false;
    private final String currentArea = "latest";
    private File mapsFolder;
    private ItemizedLayer itemizedLayer;
    private PathLayer pathLayer;
    public static final int REQUEST_PERMISSIONS = 100;
    MapActions mapActions;
    SharedPreferences sharedPreferences;
    long lastUpdateSensor = 0;
    long recordCounter = 0;
    private List<RecordsModel> temp;
    Helper helper;
    private FusedLocationProviderClient fusedLocationClient;
    private MarkerItem in, out, current;
    //location
    public static Location userLocation;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    LocationManager locationManager;
    //data dumping
    long recordsCounter = 0;
    //Sensor
    SensorManager sensorManager;
    private static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_map);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.custom_map_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        temp = new ArrayList<>();
        mapView = new MapView(this);


        final EditText input = new EditText(this);
        input.setText(currentArea);

        File baseDir;

        if (Build.VERSION.SDK_INT >= 29) { // ExternalStoragePublicDirectory Deprecated since android Q
            baseDir = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (!Environment.getExternalStorageState(baseDir).equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(this, "Not usable without an external storage!", Toast.LENGTH_SHORT).show();
                baseDir = null;
            }
        } else if (Build.VERSION.SDK_INT >= 19) { // greater or equal to Kitkat
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(this, "Not usable without an external storage!", Toast.LENGTH_SHORT).show();
                baseDir = null;
            }
            baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            baseDir = Environment.getExternalStorageDirectory();
        }

        if (baseDir != null) {
            mapsFolder = new File(String.valueOf(baseDir));
        }
        if (!mapsFolder.exists())
            mapsFolder.mkdirs();

        helper = new Helper(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Ensure location is tuned on", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!permit) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS);
        } else {
            copyAssetsToStorageIfNeeded(this, "maps/latest-gh", "latest-gh");
            chooseArea();
            customMapView();
            updateSharedPref();
            initSensor();
            initLocation();
            startDataService();
        }

    }
    private void startDataService() {
        Intent intent = new Intent(MapActivity.this, DataService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
            Log.d("service", "startDataService: service called");
        } else {
            startService(intent);
        }
    }

    private void updateSharedPref() {
        Context context = getApplicationContext();
        sharedPreferences = context.getSharedPreferences("Setting_values", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("sensor", SENSOR_DELAY);
        editor.putInt("location", LOCATION_DELAY);
        editor.putInt("timeStamp", TIME_DELAY);
//        editor.putInt("defaultS", 1);
//        editor.putInt("defaultL", 1);
//        editor.putInt("defaultT", 1);
        editor.apply();
    }

    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "location permissions needed", Toast.LENGTH_SHORT).show();
            finish();
        }
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    userLocation = location;
                    Log.d("LOCATION", "onSuccess: " + location.getLatitude() + " | " + location.getLongitude());
                    updateLocation();
                    startLocationUpdates();
                }else {
                    Toast.makeText(MapActivity.this, "Ensure location is turned on", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
        locationRequest = new LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, sharedPreferences.getInt("location", 1000)).setMinUpdateIntervalMillis(sharedPreferences.getInt("location", 1000)).build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    Log.d("LOCATION", "onLocationResult: " + location.getLatitude() + " | " + location.getLongitude());
                    userLocation = location;
                    updateLocation();
                }
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int res = 0; res < grantResults.length; res++) {
                if (grantResults[res] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                permit = true;
                copyAssetsToStorageIfNeeded(this, "maps/latest-gh", "latest-gh");
                chooseArea();
                customMapView();
                updateSharedPref();
                initSensor();
                initLocation();
                startDataService();
            } else {
                Toast.makeText(this, "Check permissions: WRITE, LOCATION", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    //to bring ui elements on top of map
    private void customMapView() {
        ViewGroup inclusionViewGroup = (ViewGroup) findViewById(R.id.custom_map_view);
        inclusionViewGroup.removeAllViews();
        View inflate = LayoutInflater.from(this).inflate(R.layout.activity_map, null);
        inclusionViewGroup.addView(inflate);

        inclusionViewGroup.getParent().bringChildToFront(inclusionViewGroup);
        mapActions = new MapActions(this, mapView);
    }

    public static void copyAssetsToStorageIfNeeded(Context context, String sourceFolder, String destinationFolder) {
        if (!shouldCopyFiles(context, destinationFolder)) {
            return;
        }

        boolean allCopied = copyAssetsToStorage(context, sourceFolder, destinationFolder);
        Log.d("COPY", String.valueOf(allCopied));

        if (allCopied) {
            SharedPreferences.Editor editor = context.getSharedPreferences("FileCopyPrefs", Context.MODE_PRIVATE).edit();
            editor.putBoolean(destinationFolder + "_copied", true);
            editor.apply();
        }
    }

    private static boolean shouldCopyFiles(Context context, String destinationFolder) {
        SharedPreferences prefs = context.getSharedPreferences("FileCopyPrefs", Context.MODE_PRIVATE);
        return !prefs.getBoolean(destinationFolder + "_copied", false);
    }


    private void chooseArea() {
        prepareInProgress = true;
        final File areaFolder = new File(mapsFolder, currentArea + "-gh");
        Log.d("OPEN", "chooseArea: " + areaFolder);
        Log.d("OPEN", "opening file");
        loadMap(areaFolder);

    }

    void loadMap(File areaFolder) {
        logUser("loading map");

        // Map events receiver
        mapView.map().layers().add(new MapEventsReceiver(mapView.map()));

        // Map file source
        MapFileTileSource tileSource = new MapFileTileSource();
        File mapFile = new File(areaFolder, currentArea + ".map");
        if (!mapFile.exists()) {
            Log.d("LOAD", "mapFile not found");
            return;
        }
        tileSource.setMapFile(mapFile.getAbsolutePath());
        //Log.d("LOAD", "loadMap: " + );
        Log.d("LOAD", "loadMap: " + tileSource);
        VectorTileLayer l = mapView.map().setBaseMap(tileSource);
        mapView.map().setTheme(VtmThemes.DEFAULT);
        mapView.map().layers().add(new BuildingLayer(mapView.map(), l));
        mapView.map().layers().add(new LabelLayer(mapView.map(), l));

        // Markers layer
        itemizedLayer = new ItemizedLayer(mapView.map(), (MarkerSymbol) null);
        mapView.map().layers().add(itemizedLayer);

        // Map position
        mapView.map().setMapPosition(18.551576, 73.831151, 1 << 17);

        //setContentView(mapView);
        ViewGroup.LayoutParams params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (mapView.getParent() != null) {
            ((ViewGroup) mapView.getParent()).removeView(mapView);
        }
        this.addContentView(mapView, params);
        loadGraphStorage();
    }

    void loadGraphStorage() {
        logUser("loading graph (" + Constants.VERSION + ") ... ");
        new GHAsyncTask<Void, Void, Path>() {
            protected Path saveDoInBackground(Void... v) {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                tmpHopp.setProfiles(new Profile("car").setVehicle("car").setWeighting("fastest"));
                tmpHopp.getCHPreparationHandler().setCHProfiles(new CHProfile("car"));
                tmpHopp.load(new File(mapsFolder, currentArea).getAbsolutePath() + "-gh");
                log("found graph " + tmpHopp.getGraphHopperStorage().toString() + ", nodes:" + tmpHopp.getGraphHopperStorage().getNodes());
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute(Path o) {
                if (hasError()) {
                    logUser("An error happened while creating graph:"
                            + getErrorMessage());
                } else {
                    logUser("Finished loading graph. Long press to define where to start and end the route.");
                }

                prepareInProgress = false;
            }
        }.execute();
    }

    private PathLayer createPathLayer(ResponsePath response) {
        Style style = Style.builder()
                .fixed(true)
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(0x9900cc33)
                .strokeWidth(4 * getResources().getDisplayMetrics().density)
                .build();
        PathLayer pathLayer = new PathLayer(mapView.map(), style);
        List<GeoPoint> geoPoints = new ArrayList<>();
        PointList pointList = response.getPoints();
        for (int i = 0; i < pointList.getSize(); i++)
            geoPoints.add(new GeoPoint(pointList.getLatitude(i), pointList.getLongitude(i)));
        pathLayer.setPoints(geoPoints);
        return pathLayer;
    }

    @SuppressWarnings("deprecation")
    private MarkerItem createMarkerItem(GeoPoint p, int resource) {
        Drawable drawable = getResources().getDrawable(resource);
        Bitmap bitmap = AndroidGraphics.drawableToBitmap(drawable);
        MarkerSymbol markerSymbol = new MarkerSymbol(bitmap, 0.5f, 1);
        MarkerItem markerItem = new MarkerItem("", "", p);
        markerItem.setMarker(markerSymbol);
        return markerItem;
    }

    public void calcPath(final double fromLat, final double fromLon,
                         final double toLat, final double toLon) {

        log("calculating path ...");
        new AsyncTask<Void, Void, ResponsePath>() {
            float time;

            protected ResponsePath doInBackground(Void... v) {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).setProfile("car");
                req.getHints().putObject(Parameters.Routing.INSTRUCTIONS, false);
                GHResponse resp = hopper.route(req);
                time = sw.stop().getSeconds();
                if (resp.getAll().isEmpty())
                    return null;
                return resp.getBest();
            }

            protected void onPostExecute(ResponsePath resp) {
                if (resp == null) {
                    logUser("Cannot find path");
                } else if (!resp.hasErrors()) {
                    log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                            + toLon + " found path with distance:" + resp.getDistance()
                            / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                            + time + " " + resp.getDebugInfo());
                    logUser("the route is " + (int) (resp.getDistance() / 100) / 10f
                            + "km long, time:" + resp.getTime() / 60000f + "min, debug:" + time);

                    pathLayer = createPathLayer(resp);
                    mapView.map().layers().add(pathLayer);
                    mapView.map().updateMap(true);
                } else {
                    logUser("Error:" + resp.getErrors());
                }
                shortestPathRunning = false;
            }
        }.execute();
    }

    private void log(String str) {
        Log.i("GH", str);
    }

    private void logUser(String str) {
        log(str);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    boolean isReady() {
        // only return true if already loaded
        if (hopper != null)
            return true;

        if (prepareInProgress) {
            logUser("Preparation still in progress");
            return false;
        }
        logUser("Prepare finished but GraphHopper not ready. This happens when there was an error while loading the files");
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateSensor >= sharedPreferences.getInt("sensor", 100)) {
                lastUpdateSensor = currentTime;

                mapActions.xAxis.setText(String.valueOf(sensorEvent.values[0]));
                mapActions.yAxis.setText(String.valueOf(sensorEvent.values[1]));
                mapActions.zAxis.setText(String.valueOf(sensorEvent.values[2]));

                if(addOnce == 0) {
                    RecordsModel recordsModel = new RecordsModel(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                    helper.addOne(recordsModel);
                    Log.d("OLD_INSERTION", "onSensorChanged: " + recordsModel);
                    addOnce++;
                }
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    class MapEventsReceiver extends Layer implements GestureListener {

        MapEventsReceiver(org.oscim.map.Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(Gesture g, MotionEvent e) {
            if (g instanceof Gesture.LongPress) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                return onLongPress(p);
            }
            return false;
        }
    }

    protected boolean onLongPress(GeoPoint p) {
        if (!isReady())
            return false;

        if (shortestPathRunning) {
            logUser("Calculation still in progress");
            return false;
        }

        if (start != null && end == null) {
            end = p;
            shortestPathRunning = true;
            out = createMarkerItem(p, R.drawable.marker_icon_red);
            itemizedLayer.addItem(out);
            mapView.map().updateMap(true);

            calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                    end.getLongitude());
        } else {
            start = p;
            end = null;
            // remove routing layers and markers
            mapView.map().layers().remove(pathLayer);
            itemizedLayer.removeAllItems();
            markerAdded = false;
            in = createMarkerItem(start, R.drawable.marker_icon_green);
            itemizedLayer.addItem(in);
            mapView.map().updateMap(true);
        }
        return true;
    }

    public int getMarkerItem(MarkerSymbol symbol) {
        List<MarkerInterface> mList = itemizedLayer.getItemList();
        int pos = -1;
        for (int i = 0; i < mList.size(); i++) {
            if (mList.get(i).getMarker() == symbol) {
                pos = i;
            }
        }
        return pos;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSharedPref();
       // startLocationUpdates();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //fusedLocationClient.removeLocationUpdates(locationCallback);
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hopper != null)
            hopper.close();

        hopper = null;
        // necessary?
        System.gc();

        // Cleanup VTM
        mapView.map().destroy();

        sensorManager.unregisterListener(this);
        fusedLocationClient.removeLocationUpdates(locationCallback);
        //dbHandlerThread.quitSafely();
    }


    private void updateLocation() {
        if (markerAdded)
            itemizedLayer.removeItem(getMarkerItem(current.getMarker()));

        mapActions.lat.setText(String.valueOf(userLocation.getLatitude()));
        mapActions.lon.setText(String.valueOf(userLocation.getLongitude()));
        GeoPoint p = new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude());
        current = createMarkerItem(p, R.drawable.location_marker);
        itemizedLayer.addItem(current);
        markerAdded = true;
        if (addOnce == 1) {
            RecordsModel recordsModel = new RecordsModel(userLocation.getLongitude(), userLocation.getLatitude());
            helper.addOne(recordsModel);
            Log.d("OLD_INSERT", "updateLocation: " + recordsModel);
            addOnce++;
        }
        //addData(new RecordsModel(userLocation.getLongitude(), userLocation.getLatitude()));
    }

    public static void getCenter(GeoPoint g, int zoom, float bearing, float tilt) {
        if (zoom == 0) {
            zoom = mapView.map().getMapPosition().zoomLevel;
        }
        double scale = 1 << zoom;
        tmpPos.setPosition(g);
        tmpPos.setScale(scale);
        tmpPos.setBearing(bearing);
        tmpPos.setTilt(tilt);
        mapView.map().animator().animateTo(300, tmpPos);
    }
}