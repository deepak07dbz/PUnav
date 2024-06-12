package com.example.accelerometer.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class Helper extends SQLiteOpenHelper{

    static final int db_version = 1;

    //TABLE_NAME
    public static final String TABLE_NAME = "Accel_Records";

    //COLUMN_NAMES
    public static final String ID = "ID";
    public static final String XAXIS = "XAXIS";
    public static final String YAXIS = "YAXIS";
    public static final String ZAXIS = "ZAXIS";
    public static final String LAT = "LAT";
    public static final String LON = "LON";
    public static final String TIMESTAMP = "TIMESTAMP";

    public Helper(Context context){
        super(context, "Records.db", null, db_version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + XAXIS + " REAL, " + YAXIS + " REAL, " + ZAXIS + " REAL, " + LON + " REAL, " + LAT + " REAL, " + TIMESTAMP + " INTEGER)";
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

    public boolean addOne(RecordsModel recordsModel){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(XAXIS, recordsModel.getX());
        cv.put(YAXIS, recordsModel.getY());
        cv.put(ZAXIS, recordsModel.getZ());
        cv.put(LON, recordsModel.getLon());
        cv.put(LAT, recordsModel.getLat());
        cv.put(TIMESTAMP, recordsModel.getTimeStamp());

        long insert = db.insert(TABLE_NAME, null, cv);
        if(insert == -1){
            return false;
        }else {
            return true;
        }
    }
    public void deleteAll(){
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }

    public List<RecordsModel> getDataSince(long sinceTimestamp) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<RecordsModel> dataList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + TIMESTAMP + " > " + sinceTimestamp;

        Cursor cursor = db.rawQuery(selectQuery, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            double x = cursor.getDouble(1);
            double y = cursor.getDouble(2);
            double z = cursor.getDouble(3);
            long timestamp = cursor.getLong(6);

            dataList.add(new RecordsModel(id, x, y, z, timestamp));
        }

        cursor.close();
        return dataList;
    }

    public List<RecordsModel> getEveyrone(){
        List<RecordsModel> readings = new ArrayList<>();
        String selectQuery = "SELECT * " + "FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(selectQuery, null);
        if(cursor.moveToFirst()){
            do {
                int ID = cursor.getInt(0);
                double xaxis = cursor.getDouble(1);
                double yaxis = cursor.getDouble(2);
                double zaxis = cursor.getDouble(3);
                double lon = cursor.getDouble(4);
                double lat = cursor.getDouble(5);
                long timeStamp = cursor.getInt(6);

                RecordsModel recordsModel = new RecordsModel(ID, xaxis, yaxis, zaxis, lon, lat, timeStamp);
                readings.add(recordsModel);

            }while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return readings;
    }
}
