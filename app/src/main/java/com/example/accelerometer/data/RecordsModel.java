package com.example.accelerometer.data;

import java.sql.Time;
import java.time.LocalTime;

public class RecordsModel {

    private int id;
    private double x;
    private double y;
    private double z;
    private double lon;
    private double lat;
    private long timeStamp;

    public RecordsModel(int id, double x, double y, double z, double lon, double lat, long timeStamp) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.lon = lon;
        this.lat = lat;
        this.timeStamp = timeStamp;
    }
    public RecordsModel(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public RecordsModel(double lon, double lat){
        this.lon = lon;
        this.lat = lat;
    }
    public RecordsModel(long timeStamp){
        this.timeStamp = timeStamp;
    }

    @Override
    public String toString() {
        return "RecordsModel{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", lon=" + lon +
                ", lat=" + lat +
                ", timeStamp=" + timeStamp +
                '}';
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
