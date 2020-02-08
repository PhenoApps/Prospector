package com.stratio.linksquare.androidexample;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class Selection_Main extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    Button button_newScan;
    Button button_viewScan;
    Button button_exportCSV;

    // DECLARE GLOBALS
    DatabaseManager myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection__main);

        // INIT DISPLAY OBJECTS
        button_newScan = findViewById(R.id.button_newScan);
        button_viewScan = findViewById(R.id.button_viewScan);
        button_exportCSV = findViewById(R.id.button_exportCSV);

        // INIT GLOBALS
        myDb = new DatabaseManager(this);

        // CONFIGURE BUTTONS
        configure_button_newScan();
        configure_button_viewScan();
        configure_button_exportCSV();
    }

    private void configure_button_newScan() {
        button_newScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Selection_Main.this, MainActivity.class);
                startActivity(i);
            }
        });
    }

    private void configure_button_viewScan () {
        button_viewScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Selection_Main.this, Selection_Scan.class);
                startActivity(i);
            }
        });
    }

    private void configure_button_exportCSV() {
        button_exportCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //generate data
                StringBuilder data_string = new StringBuilder();
                Cursor data = myDb.getData();
                String s;
                String total;
                ArrayList<String> listData = new ArrayList<>();

                // TODO: improve and comment this
                try{
                    //saving the file into device
                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File (sdCard.getAbsoluteFile() + "/Download");
                    dir.mkdirs();
                    File csv_file = new File(dir, "Log.csv");
                    csv_file.createNewFile();
                    FileOutputStream out = new FileOutputStream(csv_file);

                    // Write column names to output file
                    total = "";
                    for (int i = 0; i < data.getColumnCount(); i++) {
                        total += data.getColumnName(i);
                        total += ",";
                    }
                    total += "\n";
                    out.write(total.getBytes());

                    // Write data to output file
                    while (data.moveToNext()) {
                        total = "";
                        for(int i = 0; i < data.getColumnCount(); i++) {
                            s = data.getString(i);
                            if (s != null) {
                                if (data.getColumnName(i).equals("spectralValues")) {
                                    String[] spectralValuesArray = s.split(" ");
                                    for (int j = 0; j < spectralValuesArray.length; j++) {
                                        total += spectralValuesArray[j] + ", ";
                                    }
                                } else {
                                    total += s;
                                }
                            } else {
                                total += " ";
                            }
                            total += ",";
                        }
                        total += "\n";

                        out.write(total.getBytes());
                    }
                    out.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
