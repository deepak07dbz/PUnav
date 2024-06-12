package com.example.accelerometer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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

public class SettingsActivity extends AppCompatActivity {


    ArrayAdapter<String> options;
    ListView optionList;
    Button apply;
    FloatingActionButton fabExport;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        optionList = findViewById(R.id.optionsview);
        apply = findViewById(R.id.button6);
        fabExport = findViewById(R.id.settingsFAB);
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
                helper.deleteAll();
                Toast.makeText(SettingsActivity.this, "exported", Toast.LENGTH_SHORT).show();
            }
        });
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