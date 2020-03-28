package com.stratio.linksquare.androidexample;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class DatabaseManager extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseManager";

    private static final String TABLE_NAME = "CassavaBase";
    private static final String COL0 = "ID";
    private static final String COL1 = "scanTime";
    private static final String COL2 = "deviceID";
    private static final String COL3 = "observationUnitID";
    private static final String COL4 = "observationUnitName";
    private static final String COL5 = "observationUnitBarcode";
    private static final String COL6 = "frameNumber";
    private static final String COL7 = "lightSource";
    private static final String COL8 = "spectralValuesCount";
    private static final String COL9 = "spectralValues";
    private static final String COL10 = "serverScanID";

    public DatabaseManager(Context context) {
        super(context, TABLE_NAME, null, 10);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // does this get called in each instance or only once?
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                COL1 + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "+
                COL2 + " TEXT, "+
                COL3 + " TEXT, "+
                COL4 + " TEXT, "+
                COL5 + " TEXT, "+
                COL6 + " TEXT, "+
                COL7 + " TEXT, "+
                COL8 + " TEXT, "+
                COL9 + " TEXT, "+
                COL10 + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String item) {
        /**
         * Data is passed to DB in the following format:
         *     COL2 = "deviceID";
         *     COL3 = "observationUnitID";
         *     COL4 = "observationUnitName";
         *     COL5 = "observationUnitBarcode";
         *     COL6 = "frameNumber";
         *     COL7 = "lightSource";
         *     COL8 = "spectralValuesCount";
         *     COL9 = "spectralValues";
         *
         *     NOTE: this format should NOT CHANGE!!
         *     The DatabaseManager parser is dependant on correctly formatted data!!
         */

        String[] parts = item.split("\\s+");

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        // insert "deviceID"
        contentValues.put(COL2, parts[0]);

        // insert "observationUnitID"
        contentValues.put(COL3, parts[1]);

        // insert "observationUnitName"
        contentValues.put(COL4, parts[2]);

        // insert "observationUnitBarcode"
        contentValues.put(COL5, parts[3]);

        // insert "frameNumber"
        contentValues.put(COL6, parts[4]);

        // insert "lightSource"
        contentValues.put(COL7, parts[5]);

        // insert "spectralValuesCount"
        contentValues.put(COL8, parts[6]);

        // insert "spectralValues"
        String spectralValues = "";
        for (int i = 0; i < Integer.parseInt(parts[6]); i++) {
            spectralValues += (parts[7 + i] + " ");
        }
        contentValues.put(COL9, spectralValues);

        Log.d(TAG, "addData: Adding " + item + " to " + TABLE_NAME);

        long result = db.insert(TABLE_NAME, null, contentValues);
        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public Cursor getAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public static void scanFile(Context ctx, File filepath) {
        MediaScannerConnection.scanFile(ctx, new  String[] {filepath.getAbsolutePath()}, null, null);
    }

    public File export_toCSV() {
        Cursor data = getAll();
        String data_string;
        String output;

        try{
            // Create the directory and file
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsoluteFile() + "/Download");
            dir.mkdirs();
            File csv_file = new File(dir, "Log.csv");
            csv_file.createNewFile();
            FileOutputStream out = new FileOutputStream(csv_file);

            // Store column names into output
            output = "";
            for (int i = 0; i < data.getColumnCount(); i++) {
                output += data.getColumnName(i);
                output += ",";
            }
            output += "\n";

            // Store column data into output
            while (data.moveToNext()) {
                for(int i = 0; i < data.getColumnCount(); i++) {
                    data_string = data.getString(i);
                    if (data_string != null) {
                        output += data_string;
                    } else {
                        output += " ";
                    }
                    output += ",";
                }
                output += "\n";
            }

            // Write data to output file
            out.write(output.getBytes()); // NOTE: this is most efficient if done as few times as possible

            // Close the file
            out.close();

            return csv_file;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Cursor get_spectralValues(String observationUnitName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL9 + " FROM " + TABLE_NAME +
                " WHERE " + COL4 + " = '" + observationUnitName + "'";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public Cursor getAll_observationUnitName() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL4 + " FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public void delete_observationUnitName (String observationUnitName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME + " WHERE " + COL4 + " = '" + observationUnitName + "'";
        Log.d(TAG, "deleteName: query: " + query);
        db.execSQL(query);
    }

    public void deleteAll () {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME;
        db.execSQL(query);
    }

    public void update_observationUnitName(String oldObservationUnitName, String newObservationUnitName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME + " SET " + COL4 + " = '" +
            newObservationUnitName + "' WHERE " + COL4 + " = '" + oldObservationUnitName +"'";
        db.execSQL(query);
    }

    /**************************************************************************************************************************************
     * OBSOLETE FUNCTIONS
     */
    public void updateName(String newName, int id, String oldName){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME + " SET " + COL2 +
                " = '" + newName + "' WHERE " + COL1 + " = '" + id + "'" +
                " AND " + COL2 + " = '" + oldName + "'";
        Log.d(TAG, "updateName: Setting name to " + newName);
        db.execSQL(query);
    }

    public void updateLocalScanID (String oldLocalScanID, String newLocalScanID) {
        // TODO: fix case where localScanID = "a"
        // TODO: do i actually need localScanID to be unqiue?
        SQLiteDatabase db = this.getWritableDatabase();

        String subquery = "SELECT " + COL6 + " FROM " + TABLE_NAME + " WHERE " + COL6 + " LIKE '" + oldLocalScanID + "_Frame%'";
        Cursor result = db.rawQuery(subquery, null);
        while (result.moveToNext()) {
            Log.d("DEBUG", result.getString(0));
        }

        String query = "UPDATE " + TABLE_NAME +
                " SET " + COL6 + " = REPLACE(" +
                "(SELECT SUBSTR(" + oldLocalScanID + ", 1, (SELECT INSTR('_Frame', " + oldLocalScanID + "))))" +
                ",'" + oldLocalScanID +
                "','" + newLocalScanID + "')" +
                " WHERE " + COL6 + " LIKE '" + oldLocalScanID + "_Frame%'";
        Log.d("DEBUG", "updateName: query: " + query);
        db.execSQL(query);
    }

    public boolean isValidLocalScanID(String localScanID) {
        // Check to see if new localScanID is unique
        Cursor allLocalScanID = getAll_observationUnitName();
        while (allLocalScanID.moveToNext()) {
            if (allLocalScanID.getString(0).contains(localScanID + "_Frame") && allLocalScanID.getString(0).startsWith(localScanID)) {
                return false;
            }
        }
        return true;
    }
}