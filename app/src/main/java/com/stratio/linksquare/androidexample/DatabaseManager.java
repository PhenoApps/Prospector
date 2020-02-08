package com.stratio.linksquare.androidexample;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseManager extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseManager";

    private static final String TABLE_NAME = "CassavaBase";
    private static final String COL0 = "ID";
    private static final String COL1 = "deviceID";
    private static final String COL2 = "scanTime";
    private static final String COL3 = "observationUnitID";
    private static final String COL4 = "observationUnitName";
    private static final String COL5 = "observationUnitBarcode";
    private static final String COl6 = "localScanID"; // in the form name_Frame#
    private static final String COL7 = "serverScanID";
    private static final String COL8 = "spectralValues";

    public int currentScans; // I have no idea if this value is actually reliable

    public DatabaseManager(Context context) {
        super(context, TABLE_NAME, null, 9);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                COL1 + " TEXT, "+
                COL2 + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "+
                COL3 + " TEXT, "+
                COL4 + " TEXT, "+
                COL5 + " TEXT, "+
                COl6 + " TEXT UNIQUE, "+
                COL7 + " TEXT UNIQUE, "+
                COL8 + " TEXT)";
        db.execSQL(createTable);

        currentScans = 0;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String item) {
        // parse the incoming String item
        String[] parts = item.split("\\s+");

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL1, item);
        contentValues.put(COl6, parts[0] + "_Frame" + parts[1]);

        int length = Integer.parseInt(parts[3]);
        String spectralValues = "";
        for (int i = 0; i < length; i++ ) {
            spectralValues += (parts[4 + i * 2] + " ");
        }
        contentValues.put(COL8, spectralValues);
        contentValues.put(COL1, parts[4 + length*2]);

        Log.d(TAG, "addData: Adding " + item + " to " + TABLE_NAME);

        long result = db.insert(TABLE_NAME, null, contentValues);

        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns all the data from database
     * @return
     */
    public Cursor getData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    /**
     * Returns only the ID that matches the name passed in
     * @param localScanID
     * @return
     */
    public Cursor getSpectralValues(String localScanID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL8 + " FROM " + TABLE_NAME +
                " WHERE " + COl6 + " = '" + localScanID + "'";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    /**
     * Updates the name field
     * @param newName
     * @param id
     * @param oldName
     */
    public void updateName(String newName, int id, String oldName){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME + " SET " + COL2 +
                " = '" + newName + "' WHERE " + COL1 + " = '" + id + "'" +
                " AND " + COL2 + " = '" + oldName + "'";
        Log.d(TAG, "updateName: query: " + query);
        Log.d(TAG, "updateName: Setting name to " + newName);
        db.execSQL(query);
    }

    /**
     * Delete from database
     * @param id
     * @param name
     */
    public void deleteName(int id, String name){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME + " WHERE "
                + COL1 + " = '" + id + "'" +
                " AND " + COL2 + " = '" + name + "'";
        Log.d(TAG, "deleteName: query: " + query);
        Log.d(TAG, "deleteName: Deleting " + name + " from database.");
        db.execSQL(query);
    }
}
