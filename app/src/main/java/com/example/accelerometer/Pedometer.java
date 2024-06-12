package com.example.accelerometer;

public class Pedometer {
    static {
        System.loadLibrary("accelerometer");
    }

    public native int countSteps(double[] x, double[] y, double[] z, int size);
}
