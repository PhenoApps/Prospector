package com.stratio.linksquare.androidexample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.sip.SipSession;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

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

        // GET AND CHECK REQUIRED PERMISSIONS
        permissions_get();
        permissions_check();
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
        // TODO: figure out why this sometimes doesn't show all of the scans
        button_exportCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File csv_file = myDb.export_toCSV();
                myDb.scanFile(Selection_Main.this, csv_file); // TODO: figure out how to move this into export_toCSV()
                Toast.makeText(getApplicationContext(), "Exported to CSV. FIle located at " + csv_file.getPath(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void permissions_get() {
        ActivityCompat.requestPermissions(this, new String[] {
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CAMERA}, 0);
    }

    private void permissions_check() {
        // TODO: figure out why this closes the app even if the permissions are granted
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            finish();
        }
    }
}
