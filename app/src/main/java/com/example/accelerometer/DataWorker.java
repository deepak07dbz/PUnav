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
    private Helper helper;
    private long recordsCounter;
    private int addTen = 0;
    public DataWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        helper = new Helper(getApplicationContext());
        recordsCounter = 0;
    }

    @NonNull
    @Override
    public Result doWork() {
        File file = new File(getApplicationContext().getFilesDir(), "sensor_location_data.txt");
        if (!file.exists()) {
            Result.success();
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                RecordsModel recordsModel = RecordsModel.deserialize(line);
                insertIntoDB(recordsModel);
            }
            reader.close();
            if (file.delete()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Result.success();
    }

    private void insertIntoDB(RecordsModel recordsModel) {
        if (addTen < 10) {
            if (recordsCounter % 10 == 0) {
                helper.addOne(new RecordsModel(recordsCounter));
                recordsCounter++;
            }
            helper.addOne(recordsModel);
            Log.d("NEW_INSERT", "insertIntoDB: " + recordsModel);
            recordsCounter++;
            addTen++;
        }
    }
}
