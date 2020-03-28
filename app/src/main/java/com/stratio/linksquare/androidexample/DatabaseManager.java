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
    private static final String COL1 = "deviceID";
    private static final String COL2 = "scanTime";
    private static final String COL3 = "observationUnitID";
    private static final String COL4 = "observationUnitName";
    private static final String COL5 = "observationUnitBarcode";
    private static final String COL6 = "localScanID"; // in the form name_Frame#
    private static final String COL7 = "serverScanID";
    private static final String COL8 = "spectralValues";

    public DatabaseManager(Context context) {
        super(context, TABLE_NAME, null, 9);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // does this get called in each instance or only once?
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                COL1 + " TEXT, "+
                COL2 + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "+
                COL3 + " TEXT, "+
                COL4 + " TEXT, "+
                COL5 + " TEXT, "+
                COL6 + " TEXT UNIQUE, "+
                COL7 + " TEXT UNIQUE, "+
                COL8 + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String item) {
        // parse the incoming String item
        // NOTE: this parse method is dependant on data being passed in the correct order
        // TODO: implement a method that is not dependant
        String[] parts = item.split("\\s+");

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL1, item);
        contentValues.put(COL6, parts[0] + "_Frame" + parts[1]);

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
     */
    public Cursor getData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public Cursor getSpectralValues(String localScanID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL8 + " FROM " + TABLE_NAME +
                " WHERE " + COL6 + " = '" + localScanID + "'";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public Cursor getAllLocalScanID() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL6 + " FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public boolean isValidLocalScanID(String localScanID) {
        // Check to see if new localScanID is unique
        Cursor allLocalScanID = getAllLocalScanID();
        while (allLocalScanID.moveToNext()) {
            if (allLocalScanID.getString(0).contains(localScanID + "_Frame") && allLocalScanID.getString(0).startsWith(localScanID)) {
                return false;
            }
        }
        return true;
    }

    public void deleteLocalScanID (String localScanID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME + " WHERE "
                + COL6 + " LIKE '" + localScanID + "_Frame%'"; // removes all database entries whose name begins with *localScanID*_Frame
        Log.d(TAG, "deleteName: query: " + query);
        db.execSQL(query);
    }

    public void deleteScanAll () {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME;
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
        Log.d(TAG, "updateName: Setting name to " + newName);
        db.execSQL(query);
    }

    public File exportToCSV() {
        Cursor data = getData();
        String data_string;
        String output;

        // TODO: improve and comment this
        try{
            // Create the directory and file
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsoluteFile() + "/Download");
            dir.mkdirs();
            File csv_file = new File(dir, "Log.csv");
            csv_file.createNewFile();
            FileOutputStream out = new FileOutputStream(csv_file);

            // Write column names to output file
            output = "";
            for (int i = 0; i < data.getColumnCount(); i++) {
                String column_name = data.getColumnName(i);
                if (column_name.equals("spectralValues")) {
                    for (int j=  0; j < 600; j++) { // TODO: change 600 (bandwidth) to variable
                        output += "X" + Integer.toString(j + 400); // TODO: change 400 (starting wavelength) to a variable
                        output += ",";
                    }
                } else {
                    output += data.getColumnName(i);
                    output += ",";
                }
            }
            output += "\n";
            out.write(output.getBytes());

            // Write data to output file
            while (data.moveToNext()) {
                output = "";
                for(int i = 0; i < data.getColumnCount(); i++) {
                    data_string = data.getString(i);
                    if (data_string != null) {
                        if (data.getColumnName(i).equals("spectralValues")) {

                            // Break the spectralValues string into individual columns
                            String[] spectralValuesArray = data_string.split(" ");
                            for (int j = 0; j < spectralValuesArray.length; j++) {
                                output += spectralValuesArray[j] + ",";
                            }

                        } else {
                            output += data_string;
                        }
                    } else {
                        output += " ";
                    }
                    output += ",";
                }
                output += "\n";

                out.write(output.getBytes());
            }

            // Close the file
            out.close();

            return csv_file;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void scanFile(Context ctx, File filepath) {
        MediaScannerConnection.scanFile(ctx, new  String[] {filepath.getAbsolutePath()}, null, null);
    }


}