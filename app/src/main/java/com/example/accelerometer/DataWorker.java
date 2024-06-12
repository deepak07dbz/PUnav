package com.example.accelerometer;

import android.content.Context;
import android.speech.RecognitionListener;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.accelerometer.data.Helper;
import com.example.accelerometer.data.RecordsModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DataWorker extends Worker {
    private final Helper helper;
    public DataWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        helper = new Helper(getApplicationContext());
    }

    @NonNull
    @Override
    public Result doWork() {
        File file = new File(getApplicationContext().getFilesDir(), "sensor_location_data.txt");
        if (!file.exists()) {
            return Result.success();
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Log.i("WORKER", "doWork: file opened for reading");
            String line;
            while ((line = reader.readLine()) != null) {
                RecordsModel recordsModel = RecordsModel.deserialize(line);
                insertIntoDB(recordsModel);
            }
            reader.close();
            file.delete();
            Log.i("WORKER", "doWork: file deleted");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Result.success();
    }

    private void insertIntoDB(RecordsModel recordsModel) {
            helper.addOne(recordsModel);
            Log.i("NEW_INSERT", "insertIntoDB: " + recordsModel);
        }
    }
