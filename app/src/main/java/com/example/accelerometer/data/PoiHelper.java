package com.example.accelerometer.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class PoiHelper extends SQLiteOpenHelper {

    //table name
    public static final String TABLE_NAME = "POI";

    //columns
    public static final String ID = "ID";
    public static final String NAME = "NAME";
    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";

    public PoiHelper(Context context) {
        super(context, "pois.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ( " + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + NAME + " TEXT, " + LATITUDE + " REAL, " + LONGITUDE + " REAL)";
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

    public boolean isTableEmpty() {
        SQLiteDatabase db = this.getReadableDatabase();
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count == 0;
    }

    public PoiModel search(String name) {
        String query = "SELECT * FROM " + TABLE_NAME  + " WHERE " + NAME + " = '" + name + "'";
        Log.d("QUERY", "search: " + query);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        PoiModel poiModel = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(0);
                String poiName = cursor.getString(1);
                double lat = cursor.getDouble(2);
                double lon = cursor.getDouble(3);
                poiModel = new PoiModel(id, poiName, lat, lon);
            }
            cursor.close();
        }
        return poiModel;
    }

    public boolean addOne(PoiModel poiModel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(NAME, poiModel.getName());
        cv.put(LATITUDE, poiModel.getLat());
        cv.put(LONGITUDE, poiModel.getLon());

        long insert = db.insert(TABLE_NAME, null, cv);
        return insert != -1;
    }
}
