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
    public static final String ACCEL_RECORDS = "Accel_Records";

    //COLUMN_NAMES
    public static final String ID = "ID";
    public static final String XAXIS = "XAXIS";
    public static final String YAXIS = "YAXIS";
    public static final String ZAXIS = "ZAXIS";
    public static final String LAT = "LAT";
    public static final String LON = "LON";
    public static final String TIMESTAMP = "TIMESTAMP";

    public Helper(Context context){
        super(context, "Records", null, db_version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_TABLE = "CREATE TABLE " + ACCEL_RECORDS + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + XAXIS + " REAL, " + YAXIS + " REAL, " + ZAXIS + " REAL, " + LON + " REAL, " + LAT + " REAL, " + TIMESTAMP + " INTEGER)";
        sqLiteDatabase.execSQL(CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
       // sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ACCEL_RECORDS);
       // onCreate(sqLiteDatabase);
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

        long insert = db.insert(ACCEL_RECORDS, null, cv);
        if(insert == -1){
            return false;
        }else {
            return true;
        }
    }
    public void deleteAll(){
        SQLiteDatabase db = getWritableDatabase();
        db.delete(ACCEL_RECORDS, null, null);
    }
    public List<RecordsModel> getEveyrone(){
        List<RecordsModel> readings = new ArrayList<>();
        String selectQuery = "SELECT * " +
//                "CASE\n" +
//                "WHEN TIMESTAMP % 5 = 0 THEN TIMESTAMP\n" +
//                "ELSE 1\n" +
//                "END as TIMESTMAP\n" +
                "FROM " + ACCEL_RECORDS; //" WHERE TIMESTAMP % 2 = 0";
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
