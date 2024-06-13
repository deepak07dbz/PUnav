package com.example.accelerometer;

import static com.example.accelerometer.FileUtils.copyAssetsToStorage;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.accelerometer.data.Helper;
import com.example.accelerometer.data.PoiHelper;
import com.example.accelerometer.data.PoiModel;
import com.example.accelerometer.data.RecordsModel;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity{

    public static int LOCATION_DELAY = 3000;        //in milliseconds
    public static int TIME_DELAY = 10000;             //in milliseconds
    public static int SENSOR_DELAY = 100;           //in milliseconds
    public static final String LAST_TIMESTAMP = "last_timestamp";
    private static MapActivity mapActivity;
    public boolean markerAdded = false;
    public boolean endAdded = false;
    private boolean permit = false;
    private static MapView mapView;
    private TextView counter;
    public static GraphHopper hopper;
    private static MapPosition tmpPos = new MapPosition();
    private final String currentArea = "latest";
    private File mapsFolder;
    public static ItemizedLayer itemizedLayer;
    private PathLayer pathLayer;
    public static final int REQUEST_PERMISSIONS = 100;
    MapActions mapActions;
    SharedPreferences sharedPreferences;
    Helper helper;
    PoiHelper poiHelper;
    private MarkerItem in, current;
    //location
    public static Location userLocation;
    LocationManager locationManager;
    //Sensor
    private static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};
    //retrieval
    Handler handler;
    Runnable runnable;
    //pedometer
    Pedometer pedometer;

    //broadcast
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && DataService.ACTION_STOP_APP.equals(intent.getAction())) {
                Log.d("BR", "onReceive: stopped app");
                stopHandler();
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_map);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.custom_map_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Log.d("MAIN", "onCreate: started");

        IntentFilter intentFilter = new IntentFilter(DataService.ACTION_STOP_APP);
        registerReceiver(broadcastReceiver, intentFilter);


        mapView = new MapView(this);
        //counter = findViewById(R.id.TVcounter);

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
        poiHelper = new PoiHelper(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Ensure location is tuned on", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!permit) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    public void startHandler() {
        handler = new Handler();
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this,sharedPreferences.getInt("location", 1000));
                if (DataService.curLocation != null) {
                    userLocation = DataService.curLocation;
                    Log.d("HANDLER", "run: " + userLocation.getLatitude() + " | " + userLocation.getLongitude());
                    updateLocation();
                }
            }
        }, sharedPreferences.getInt("location", 1000));
    }

    public static MapActivity getInstance() {
        if (mapActivity == null) {
            mapActivity = new MapActivity();
        }
        return mapActivity;
    }

    private void loadPoi() {
        try {
            Log.d("POI", "loadPoi: insertion started");
            InputStream inputStream = this.getAssets().open("latest.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                String name = parts[4].toLowerCase();
                double lat = Double.parseDouble(parts[2]);
                double lon = Double.parseDouble(parts[3]);
                poiHelper.addOne(new PoiModel(name, lat, lon));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "enter start and end points", Toast.LENGTH_SHORT).show();
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
        editor.apply();
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
                if (poiHelper.isTableEmpty()) {
                    loadPoi();
                }
                copyAssetsToStorageIfNeeded(this, "maps/latest-gh", "latest-gh");
                chooseArea();
                customMapView();
                updateSharedPref();
                startDataService();
                startHandler();
                pedometer = new Pedometer();
                new RetrieveStepsTask().execute();
            } else {
                Toast.makeText(this, "Check permissions: WRITE, LOCATION", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //to bring ui elements on top of map
    private void customMapView() {
        ViewGroup inclusionViewGroup = (ViewGroup) findViewById(R.id.custom_map_view);
        inclusionViewGroup.removeAllViews();
        View inflate = LayoutInflater.from(this).inflate(R.layout.activity_map, null);
        inclusionViewGroup.addView(inflate);

        inclusionViewGroup.getParent().bringChildToFront(inclusionViewGroup);
        mapActions = new MapActions(this);
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
        final File areaFolder = new File(mapsFolder, currentArea + "-gh");
        Log.d("OPEN", "chooseArea: " + areaFolder);
        Log.d("OPEN", "opening file");
        loadMap(areaFolder);

    }

    void loadMap(File areaFolder) {
        logUser("loading map", this);

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

        ViewGroup.LayoutParams params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (mapView.getParent() != null) {
            ((ViewGroup) mapView.getParent()).removeView(mapView);
        }
        this.addContentView(mapView, params);
        loadGraphStorage();
    }

    void loadGraphStorage() {
        logUser("loading graph (" + Constants.VERSION + ") ... ", this);
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
                    log("An error happened while creating graph:"
                            + getErrorMessage());
                } else {
                    log("Finished loading graph. Long press to define where to start and end the route.");
                }
            }
        }.execute();
    }

    private PathLayer createPathLayer(ResponsePath response, Context context) {
        Style style = Style.builder()
                .fixed(true)
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(0x9900cc33)
                .strokeWidth(4 * context.getResources().getDisplayMetrics().density)
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
    private MarkerItem createMarkerItem(GeoPoint p, int resource, Context context) {
        Drawable drawable = context.getResources().getDrawable(resource);
        Bitmap bitmap = AndroidGraphics.drawableToBitmap(drawable);
        MarkerSymbol markerSymbol = new MarkerSymbol(bitmap, 0.5f, 1);
        MarkerItem markerItem = new MarkerItem("", "", p);
        markerItem.setMarker(markerSymbol);
        return markerItem;
    }


    public void calcPath(final double fromLat, final double fromLon,
                         final double toLat, final double toLon, final Context context) {

        log("calculating path ...");
        new AsyncTask<Void, Void, ResponsePath>() {
            float time;

            protected ResponsePath doInBackground(Void... v) {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).setProfile("car");
                req.getHints().putObject(Parameters.Routing.INSTRUCTIONS, false);
                GHResponse resp = null;
                if (hopper != null) {
                    resp = hopper.route(req);
                } else {
                    Log.d("HOPPER", "doInBackground: hopper null");
                }
                if (resp == null || resp.hasErrors()) {
                    Log.d("RESP", "doInBackground: resp null");
                }
                time = sw.stop().getSeconds();
                if (resp.getAll().isEmpty())
                    return null;
                return resp.getBest();
            }

            protected void onPostExecute(ResponsePath resp) {
                if (resp == null) {
                    log("Cannot find path");
                } else if (!resp.hasErrors()) {

                    if (itemizedLayer != null) {
                        if (endAdded) {
                            itemizedLayer.removeItem(getMarkerItem(in.getMarker()));
                        }
                        in = createMarkerItem(new GeoPoint(toLat, toLon), R.drawable.marker_icon_green, context);
                        itemizedLayer.addItem(in);
                        endAdded = true;
                    }
                    logUser("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                            + toLon + " found path with distance:" + resp.getDistance()
                            / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                            + time + " " + resp.getDebugInfo(), context);
                    logUser("the route is " + (int) (resp.getDistance() / 100) / 10f
                            + "km long, time:" + resp.getTime() / 60000f + "min, debug:" + time, context);

                    if(getInstance() == null) {
                        log("null context");
                    }
                    if (mapView.map().layers().contains(pathLayer)) {
                        mapView.map().layers().remove(pathLayer);
                    }
                    pathLayer = createPathLayer(resp, context);
                    mapView.map().layers().add(pathLayer);
                    mapView.map().updateMap(true);
                } else {
                   log("Error:" + resp.getErrors());
                }
            }
        }.execute();
    }

    private void log(String str) {
        Log.i("GH", str);
    }

    private void logUser(String str, Context context) {
        log(str);
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
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
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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

        stopHandler();
        unregisterReceiver(broadcastReceiver);
    }

    private void stopHandler() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    public void updateLocation() {
        if (markerAdded)
            itemizedLayer.removeItem(getMarkerItem(current.getMarker()));

        GeoPoint p = new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude());
        current = createMarkerItem(p, R.drawable.location_marker, this);
        itemizedLayer.addItem(current);
        markerAdded = true;
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

    private class RetrieveStepsTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            long lastProcessedTimestamp = sharedPreferences.getLong(LAST_TIMESTAMP, 0);
            List<RecordsModel> data = helper.getDataSince(lastProcessedTimestamp);

            int size = data.size();
            if (size == 0) {
                return 0;
            }

            double[] x = new double[size];
            double[] y = new double[size];
            double[] z = new double[size];
            long[] timestamps = new long[size];

            for (int i = 0; i < size; i++) {
                RecordsModel datum = data.get(i);
                x[i] = datum.getX();
                y[i] = datum.getY();
                z[i] = datum.getZ();
                timestamps[i] = datum.getTimeStamp();
            }

            int stepCount = pedometer.countSteps(x, y, z, size);

            // Update the last processed timestamp
            long newTimeStamp = timestamps[size - 1];
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(LAST_TIMESTAMP, newTimeStamp);
            editor.apply();

            return stepCount;
        }

        @Override
        protected void onPostExecute(Integer stepCount) {
            counter.setText(String.valueOf(stepCount));
        }
    }

}