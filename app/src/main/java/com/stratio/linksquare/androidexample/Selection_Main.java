package com.stratio.linksquare.androidexample;

import android.Manifest;
import android.content.Intent;
import android.net.sip.SipSession;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.List;

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

        get_permissions();
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
                myDb.scanFile(Selection_Main.this, myDb.exportToCSV());
            }
        });
    }

    private void get_permissions() {

        Dexter.withActivity(this).withPermissions(
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CAMERA
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                Log.d("DEBUG","yay!");
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                finish();
            }
        }).check();
    }
}
