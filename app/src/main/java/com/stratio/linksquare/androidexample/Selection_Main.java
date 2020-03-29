package com.stratio.linksquare.androidexample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.sip.SipSession;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
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
        button_exportCSV = findViewById(R.id.button_export);

        // INIT GLOBALS
        myDb = new DatabaseManager(this);

        // CONFIGURE BUTTONS
        configure_button_newScan();
        configure_button_viewScan();
        configure_button_export();

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

    private void configure_button_export() {
        button_exportCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(Selection_Main.this);
                builder.setTitle("Select Output Format");

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(Selection_Main.this, android.R.layout.select_dialog_singlechoice);
                arrayAdapter.add("Simple CSV");
                arrayAdapter.add("SCiO Format");
                arrayAdapter.add("BrAPI Format");

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch(i) {
                            case 0: // user clicked "Simple CSV"
                                File csv_file = myDb.export_toCSV();
                                // TODO: figure out why this sometimes doesn't show all of the scans
                                myDb.scanFile(Selection_Main.this, csv_file); // TODO: figure out how to move this into export_toCSV()
                                Toast.makeText(getApplicationContext(), "Exported to CSV. FIle located at " + csv_file.getPath(), Toast.LENGTH_LONG).show();
                                break;

                            case 1: // user clicked "SCiO Format"
                                File scio_file = myDb.export_toSCiO();
                                myDb.scanFile(Selection_Main.this, scio_file);
                                Toast.makeText(getApplicationContext(), "Exported to SCiO Format. FIle located at " + scio_file.getPath(), Toast.LENGTH_LONG).show();
                                break;

                            case 2: // user clicked "BrAPI Format"
                                File brapi_file = myDb.export_toBrAPI();
                                myDb.scanFile(Selection_Main.this, brapi_file);
                                Toast.makeText(getApplicationContext(), "Exported to SCiO Format. FIle located at " + brapi_file.getPath(), Toast.LENGTH_LONG).show();
                                break;

                            default:
                                break;
                        }
                        dialogInterface.dismiss();
                    }
                });
                builder.show();
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
