package com.paplo.silentio.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by grajp_000 on 13.10.2017.
 */

public class PlaceDbHelper extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "silentio.database";

    private static final int DATABASE_VERSION = 1;

    public PlaceDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }
    @Override
    public void onCreate(SQLiteDatabase db) {

        final String SQL_CREATE_PLACES_TABLE = "CREATE TABLE " + PlaceContract.PlaceEntry.TABLE_NAME + " (" +
                PlaceContract.PlaceEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_ID + " TEXT NOT NULL," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_RADIUS + " INTEGER," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_NAME + " TEXT," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_RINGER + " TEXT NOT NULL," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_NOTIFICATIONS + " BOOLEAN," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_TIME_CONSTRAINTS + " BOOLEAN," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_UNMUTE_AFTER_TIME + " BOOLEAN," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_DAYS + " TEXT," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_START_TIME + " INTEGER," +
                PlaceContract.PlaceEntry.COLUMN_PLACE_END_TIME + " INTEGER, " +
                "UNIQUE (" + PlaceContract.PlaceEntry.COLUMN_PLACE_ID + ") ON CONFLICT REPLACE" +
                "); ";

        db.execSQL(SQL_CREATE_PLACES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PlaceContract.PlaceEntry.TABLE_NAME);
        onCreate(db);
    }
}
