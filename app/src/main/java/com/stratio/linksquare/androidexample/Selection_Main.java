package com.stratio.linksquare.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

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
                myDb.exportToCSV();
            }
        });
    }
}
