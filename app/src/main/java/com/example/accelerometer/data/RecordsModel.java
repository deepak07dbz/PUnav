package com.example.accelerometer.data;

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

    public RecordsModel(int id, double x, double y, double z, long timeStamp) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timeStamp = timeStamp;
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

    public String serialize() {
        return id + "," + x + "," + y + "," + z + "," + lon + "," + lat + "," + timeStamp;
    }

    public static RecordsModel deserialize(String data) {
        String[] parts = data.split(",");
        int id = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
        double x = parts.length > 1 ? Double.parseDouble(parts[1]) : 0.0;
        double y = parts.length > 2 ? Double.parseDouble(parts[2]) : 0.0;
        double z = parts.length > 3 ? Double.parseDouble(parts[3]) : 0.0;
        double lon = parts.length > 4 ? Double.parseDouble(parts[4]) : 0.0;
        double lat = parts.length > 5 ? Double.parseDouble(parts[5]) : 0.0;
        long timeStamp = parts.length > 6 ? Long.parseLong(parts[6]) : 0;

        return new RecordsModel(id, x, y, z, lon, lat, timeStamp);
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
