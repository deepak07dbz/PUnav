package com.example.accelerometer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.example.accelerometer.data.Helper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SettingsActivity extends AppCompatActivity {

    public static final int CREATE_FILE_REQUEST = 1;
    public static final int PICK_FILE_REQUEST = 2;
    ArrayAdapter<String> options;
    ListView optionList;
    Button apply;
    FloatingActionButton fabExport, fabDelete;
    final String[] locationChoices = {"1", "3", "5"};           //in seconds
    final String[] sensorChoices = {"0.1", "0.05", "0.3"};      //in seconds
    final String[] timestamps = {"5", "10", "20"};              //in seconds
    int checkedItemL;
    int checkedItemT;
    int checkedItemS;
    int newLocation;
    int newTimestamp;
    double newSensor;
    SharedPreferences.Editor editor;
    private File zipFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        optionList = findViewById(R.id.optionsview);
        apply = findViewById(R.id.button6);
        fabExport = findViewById(R.id.settingsFAB);
        fabDelete = findViewById(R.id.deleteFAB);
        showOptions();

         checkedItemL = getSharedPreferences("Setting_values", MODE_PRIVATE).getInt("defaultL", 1);
         checkedItemT = getSharedPreferences("Setting_values", MODE_PRIVATE).getInt("defaultT", 1);
         checkedItemS = getSharedPreferences("Setting_values", MODE_PRIVATE).getInt("defaultS", 1);
         newLocation = getSharedPreferences("Setting_values", MODE_PRIVATE).getInt("location", 1000);
         newTimestamp = getSharedPreferences("Setting_values", MODE_PRIVATE).getInt("timeStamp", 10000);
         newSensor = getSharedPreferences("Setting_values", MODE_PRIVATE).getInt("sensor", 100);

        editor = getSharedPreferences("Setting_values", MODE_PRIVATE).edit();

        optionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(i == 0){
                    locationOptionsDialog(locationChoices);
                }
                else if(i == 1){
                    timeStampOptionsDialog(timestamps);
                }else {
                    sensorOptionsDialog(sensorChoices);
                }
            }
        });

        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmation();
            }
        });

        fabExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Helper helper = new Helper(SettingsActivity.this);
                helper.backupDb();
                pickFile();
            }
        });

        fabDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Helper helper = new Helper(SettingsActivity.this);
                helper.deleteAll();
                Toast.makeText(SettingsActivity.this, "database has been cleared!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    createZip(uri);
                }
            }
        }
        if (requestCode == CREATE_FILE_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null && zipFile != null) {
                    writeToUri(zipFile, uri);
                }
            }
        }
    }

    private void writeToUri(File zipFile, Uri uri) {
        try {
            InputStream is = new FileInputStream(zipFile);
            OutputStream os = getContentResolver().openOutputStream(uri);

            FileUtils.copyFile(is, os);
            Toast.makeText(this, "export success! ", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            Log.e("EXPORT", "writeToUri: export failed! : ", e);
        }
    }

    private void createZip(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                Toast.makeText(this, "failed to open file!", Toast.LENGTH_SHORT).show();
                return;
            }

            File backupDir = new File(getExternalFilesDir(null), Helper.BACKUPDIR);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(new Date());
            zipFile = new File(backupDir, "backup_" + timeStamp + ".zip");

            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
            BufferedInputStream bis = new BufferedInputStream(is);

            zos.putNextEntry(new ZipEntry("records.db"));

            FileUtils.copyFile(bis, zos);

            zos.closeEntry();
            Toast.makeText(this, "compression success!", Toast.LENGTH_SHORT).show();
            saveZipFile(zipFile);

        }
        catch (Exception e) {
            Log.e("EXPORT", "createZip: error creating zip!: ", e);
        }
    }

    private void saveZipFile(File zipFile) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, zipFile.getName());
        startActivityForResult(intent, CREATE_FILE_REQUEST);
    }

    private void showConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle("Confirm these settings?");
        builder.setMessage("Location delay will be set to " + newLocation + "\nTimestamp delay will be set to " + newTimestamp + "\nSensor delay will be set to " + newSensor);
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Toast.makeText(SettingsActivity.this, "Changes have been saved", Toast.LENGTH_SHORT).show();
                MapActivity.TIME_DELAY = newTimestamp * 1000;
                MapActivity.LOCATION_DELAY = newLocation * 1000;
                MapActivity.SENSOR_DELAY = (int) (newSensor * 1000);
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void sensorOptionsDialog(String[] sensorChoices) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);

        builder.setTitle("Change delay");
        builder.setSingleChoiceItems(sensorChoices, checkedItemS, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i == 0){
                    newSensor = Double.parseDouble(sensorChoices[i]);
                    checkedItemS = i;
                    dialogInterface.dismiss();
                } else if (i == 1) {
                    newSensor = Double.parseDouble((sensorChoices[i]));
                    checkedItemS = i;
                    dialogInterface.dismiss();
                } else {
                    newSensor = Double.parseDouble(sensorChoices[i]);
                    checkedItemS = i;
                    dialogInterface.dismiss();
                }
            }
        });
        builder.show();
    }


    private void timeStampOptionsDialog(String[] timestamps) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle("Change delay");
        builder.setSingleChoiceItems(timestamps, 1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i == 0){
                    newTimestamp = Integer.parseInt(timestamps[i]);
                    checkedItemT = i;
                    dialogInterface.dismiss();
                } else if (i == 1) {
                    newTimestamp = Integer.parseInt(timestamps[i]);
                    checkedItemT = i;
                    dialogInterface.dismiss();
                } else {
                    newTimestamp = Integer.parseInt(timestamps[i]);
                    checkedItemT = i;
                    dialogInterface.dismiss();
                }
            }
        });
        builder.show();
    }

    private void locationOptionsDialog(String[] choices) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle("Change delay");
        builder.setSingleChoiceItems(choices, checkedItemL, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i == 0){
                    newLocation = Integer.parseInt(choices[i]);
                    checkedItemL = i;
                    dialogInterface.dismiss();
                } else if (i == 1) {
                    newLocation = Integer.parseInt(choices[i]);
                    checkedItemL = i;
                    dialogInterface.dismiss();
                } else {
                    newLocation = Integer.parseInt(choices[i]);
                    checkedItemL = i;
                    dialogInterface.dismiss();
                }
            }
        });
        builder.show();
    }

    public void showOptions(){
        String[] optionsArray = {"Location", "Timestamp", "Accelerometer"};
        options = new ArrayAdapter<>(SettingsActivity.this, android.R.layout.simple_list_item_1, optionsArray);
        optionList.setAdapter(options);
    }
}