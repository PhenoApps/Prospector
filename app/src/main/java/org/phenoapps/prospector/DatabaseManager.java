package org.phenoapps.prospector;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

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

    private static final int CURRENT_LINKSQURE_LENGTH = 600; // TODO: remove dependance on this
    private static final int CURRENT_LINKSQURE_START = 400;

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

    public boolean insertData_fromLinkSquare(String item) {
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

    public boolean insertData_fromSimpleCSV(String data) {
        String[] parts = data.split(",");
        ContentValues contentValues = new ContentValues();

        /**
         * NOTE: data is already in db format
         * parts[0] = "ID"
         * parts[1] = "scanTime"
         * parts[2] = "deviceID"
         * parts[3] = "observationUnitID"
         * parts[4] = "observationUnitName"
         * parts[5] = "observationUnitBarcode"
         * parts[6] = "frameNumber"
         * parts[7] = "lightSource"
         * parts[8] = "spectralValuesCount"
         * parts[9] = "spectralValues"
         * parts[10] = "serverScanID"
         */
        contentValues.put(COL1, parts[1]);
        contentValues.put(COL2, parts[2]);
        contentValues.put(COL3, parts[3]);
        contentValues.put(COL4, parts[4]);
        contentValues.put(COL5, parts[5]);
        contentValues.put(COL6, parts[6]);
        contentValues.put(COL7, parts[7]);
        contentValues.put(COL8, parts[8]);
        contentValues.put(COL9, parts[9]);
        contentValues.put(COL10, parts[10]);

        SQLiteDatabase db = this.getWritableDatabase();
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

    public void deleteAll () {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME;
        db.execSQL(query);
    }

    public int getCount_id() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT COUNT(" + COL0 + ") FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        data.moveToNext();
        return data.getInt(0);
    }

    public int getCount_observationScanName() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT COUNT(DISTINCT " + COL4 + ") FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        data.moveToNext();
        return data.getInt(0);
    }

    public Cursor getAll_observationUnitName() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL4 + " FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public void update_observationUnitName(String oldObservationUnitName, String newObservationUnitName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME + " SET " + COL4 + " = '" +
                newObservationUnitName + "' WHERE " + COL4 + " = '" + oldObservationUnitName +"'";
        db.execSQL(query);
    }

    public void delete_observationUnitName (String observationUnitName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME + " WHERE " + COL4 + " = '" + observationUnitName + "'";
        Log.d(TAG, "deleteName: query: " + query);
        db.execSQL(query);
    }

    public boolean isUnique_observationUnitName (String observationUnitName) {
        Cursor observationUnitNames = getAll_observationUnitName();
        while (observationUnitNames.moveToNext()) {
            if (observationUnitNames.getString(0).equals(observationUnitName)) {
                return false;
            }
        }
        return true;
    }

    public Cursor get_spectralValues_byObservationUnitName(String observationUnitName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL9 + " FROM " + TABLE_NAME +
                " WHERE " + COL4 + " = '" + observationUnitName + "'";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public Cursor get_spectralValues_byID(int ID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL9 + " FROM " + TABLE_NAME +
                " WHERE " + COL0 + " = '" + ID + "'";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public Cursor get_ID_byObservationUnitName(String observationUnitName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL0 + " FROM " + TABLE_NAME +
                " WHERE " + COL4 + " = '" + observationUnitName + "'";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public File export_toSimpleCSV() {
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

    public File export_toSCiO() {
        // Based on: https://cassava-test.sgn.cornell.edu/breeders/nirs/
        Cursor data = getAll();
        StringBuilder output = new StringBuilder(); // TODO: figure out if storing all data in one string is okay procedure

        try{
            // Create the directory and file
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsoluteFile() + "/Download");
            dir.mkdirs();
            File csv_file = new File(dir, "Log_SCiO.csv");
            csv_file.createNewFile();
            FileOutputStream out = new FileOutputStream(csv_file);

            // add metadata
            output.append("name," + "testName" + "\n");
            output.append("user," + "testUser" + "\n");
            output.append("description," + "dummyData" + "\n");
            output.append("num_records," + getCount_id() + "\n");
            output.append("num_samples," + getCount_observationScanName() + "\n");
            output.append("num_auto_attributes," + "0" + "\n");
            output.append("num_custom_attributes," + "0" + "\n");
            output.append("num_wavelengths," + CURRENT_LINKSQURE_LENGTH + "\n");
            output.append("wavelengths_start," + CURRENT_LINKSQURE_START + "\n");
            output.append("wavelengths_resolution," + "1" + "\n");

            // add column names
            output.append("id,sample_id,sampling_time,User_input_id,device_id,comment,temperature,location,outlier,");
            for (int i = 0; i < CURRENT_LINKSQURE_LENGTH; i++) {
                output.append("spectrum_" + i + " + " + CURRENT_LINKSQURE_START + ",");
            }
            output.append("\n");

            // add column types
            output.append("int,unicode,datetime,unicode,unicode,NoneType,float,NoneType,str,");
            for (int i = 0; i < CURRENT_LINKSQURE_LENGTH; i++) {
                output.append("float,");
            }
            output.append("\n");

            // add data
            while (data.moveToNext()) {
                output.append(data.getString(0) + ","); // id
                output.append(data.getString(0) + ","); // sample_id
                output.append(data.getString(1) + ","); // sampling_time
                output.append(data.getString(4) + ","); // User_input_id
                output.append(data.getString(2) + ","); // device_id
                output.append("None,"); // comment
                output.append("0,"); // temperature
                output.append("None,"); // location
                output.append("no,"); // outlier

                // spectral values
                String[] spectralValues = data.getString(9).split(" ");
                for (int i = 0; i < spectralValues.length; i++) {
                    output.append(spectralValues[i] + ",");
                }

                output.append("\n");
            }

            // Write data to output file
            out.write(output.toString().getBytes()); // NOTE: this is most efficient if done as few times as possible

            // Close the file
            out.close();

            return csv_file;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public File export_toBrAPI() {
        // Based on: https://github.com/plantbreeding/API/issues/399
        Cursor data = getAll();
        StringBuilder output = new StringBuilder();

        try{
            // Create the directory and file
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsoluteFile() + "/Download");
            dir.mkdirs();
            File csv_file = new File(dir, "Log_BrAPI.json");
            csv_file.createNewFile();
            FileOutputStream out = new FileOutputStream(csv_file);

            // open Json object
            output.append("\"log\": [\n");

            // formatting looks bad here so that it looks nice after export
            while (data.moveToNext()) {
                output.append("{\n");
                output.append("\t\"deviceID\": \"" + data.getString(2) + "\",\n"); // deviceID
                output.append("\t\"timestamp\": \"" + data.getString(1) + "\",\n"); // timestamp
                output.append("\t\"observationUnitID\": \"" + data.getString(3) + "\",\n"); // observationUnitID
                output.append("\t\"observationUnitName\": \"" + data.getString(4) + "\",\n"); // observationUnitName
                output.append("\t\"observationUnitBarcode\": \"" + data.getString(5) + "\",\n"); // observationUnitBarcode
                output.append("\t\"scanDbID\": \"" + data.getString(10) + "\",\n"); // scanDbID
                output.append("\t\"scanPUI\": \"" + data.getString(0) + "\",\n"); // scanPUI

                // spectralValues
                output.append("\t\"spectralValues\": [\n\t\t");
                String[] spectralValues = data.getString(9).split(" ");
                for (int i = 0; i < 5; i++) {
                    output.append("{\n\t\t\t\"wavelength\": " + (CURRENT_LINKSQURE_START + i) + ",\n");
                    output.append("\t\t\t\"spectralValue\": " + spectralValues[i] + "\n\t\t},\n\t\t");
                }
                output.append("]\n\t},\n");
            }
            output.append("]");

            // Write data to output file
            out.write(output.toString().getBytes()); // NOTE: this is most efficient if done as few times as possible

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