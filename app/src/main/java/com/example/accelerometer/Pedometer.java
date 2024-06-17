package com.example.accelerometer;

import android.util.Log;

public class Pedometer {
    static {
        try {
            System.loadLibrary("accelerometer");
            Log.d("LOAD", "static initializer: successful");
        }catch (UnsatisfiedLinkError e) {
            Log.d("LOAD", "static initializer: failure" + e);
        }
    }

    public static native int countSteps(float[] x, float[] y, float[] z, int size);
}
