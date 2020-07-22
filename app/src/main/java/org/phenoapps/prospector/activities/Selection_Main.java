//package org.phenoapps.prospector.activities;
//
//import android.Manifest;
//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import org.phenoapps.prospector.DatabaseManager;
//import org.phenoapps.prospector.R;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//
//public class Selection_Main extends AppCompatActivity {
//    // DECLARE DISPLAY OBJECTS
//    Button button_newScan;
//    Button button_viewScan;
//    Button button_export;
//    Button button_import;
//
//    // DECLARE GLOBALS
//    DatabaseManager myDb;
//
//    private int experiment;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_selection__main);
//
//        // INIT DISPLAY OBJECTS
//        button_newScan = findViewById(R.id.button_newScan);
//        button_viewScan = findViewById(R.id.button_viewScan);
//        button_export = findViewById(R.id.button_export);
//        button_import = findViewById(R.id.button_import);
//
//        // INIT GLOBALS
//        myDb = new DatabaseManager(this);
//
//        int eid = getIntent().getIntExtra("experiment", -1);
//
//        if (eid != -1) {
//
//            experiment = eid;
//
//        }
//
//        // CONFIGURE BUTTONS
//        configure_button_newScan();
//        configure_button_viewScan();
//        configure_button_export();
//        configure_button_import();
//
//        // GET AND CHECK REQUIRED PERMISSIONS
//        permissions_get();
//        permissions_check();
//    }
//
//    private void configure_button_newScan() {
//        button_newScan.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent i = new Intent(Selection_Main.this, MainActivity_LinkSquare.class);
//                startActivity(i);
//            }
//        });
//    }
//
//    private void configure_button_viewScan () {
//        button_viewScan.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent i = new Intent(Selection_Main.this, ScanActivity.class);
//                startActivity(i);
//            }
//        });
//    }
//
//    private void configure_button_export() {
//        button_export.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                final AlertDialog.Builder builder = new AlertDialog.Builder(Selection_Main.this);
//                builder.setTitle(R.string.select_output_format);
//
//                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(Selection_Main.this, android.R.layout.select_dialog_singlechoice);
//                arrayAdapter.add(getString(R.string.database_csv));
//                arrayAdapter.add(getString(R.string.scio_format));
//                arrayAdapter.add(getString(R.string.brapi_format));
//
//                builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        dialogInterface.dismiss();
//                    }
//                });
//                builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        switch(i) {
//                            case 0: // user clicked "Database CSV"
//                                File csv_file = myDb.export_toDatabaseCSV();
//                                // TODO: figure out why this sometimes doesn't show all of the scans
//                                myDb.scanFile(Selection_Main.this, csv_file); // TODO: figure out how to move this into export_toCSV()
//                                Toast.makeText(getApplicationContext(), getString(R.string.export_file_message) + csv_file.getPath(), Toast.LENGTH_LONG).show();
//                                break;
//
//                            case 1: // user clicked "SCiO Format"
//                                File scio_file = myDb.export_toSCiO();
//                                myDb.scanFile(Selection_Main.this, scio_file);
//                                Toast.makeText(getApplicationContext(), getString(R.string.export_to_scio_message) + scio_file.getPath(), Toast.LENGTH_LONG).show();
//                                break;
//
//                            case 2: // user clicked "BrAPI Format"
//                                File brapi_file = myDb.export_toBrAPI();
//                                myDb.scanFile(Selection_Main.this, brapi_file);
//                                Toast.makeText(getApplicationContext(), getString(R.string.export_to_scio_message) + brapi_file.getPath(), Toast.LENGTH_LONG).show();
//                                break;
//
//                            default:
//                                break;
//                        }
//                        dialogInterface.dismiss();
//                    }
//                });
//                builder.show();
//            }
//        });
//    }
//
//    private void configure_button_import() {
//        button_import.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                final AlertDialog.Builder builder = new AlertDialog.Builder(Selection_Main.this);
//                builder.setTitle(R.string.select_import_method);
//
//                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(Selection_Main.this, android.R.layout.select_dialog_singlechoice);
//                arrayAdapter.add(getString(R.string.example_data));
//
//                builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        dialogInterface.dismiss();
//                    }
//                });
//                builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        switch(i) {
//                            case 0: // user clicked "Example Data"
//                                try {
//                                    if (myDb.isUnique_observationUnitName("sample_1", experiment) == false) {
//                                        Toast.makeText(getApplicationContext(), R.string.sample1_already_exists, Toast.LENGTH_SHORT).show();
//                                    } else if (myDb.isUnique_observationUnitName("samle_2", experiment) == false) {
//                                        Toast.makeText(getApplicationContext(), R.string.sample2_already_exists, Toast.LENGTH_SHORT).show();
//                                    } else if (myDb.isUnique_observationUnitName("sample_3", experiment) == false) {
//                                        Toast.makeText(getApplicationContext(), R.string.sample3_already_exists, Toast.LENGTH_SHORT).show();
//                                    } else {
//                                        BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("ExampleData.csv")));
//                                        String line = reader.readLine(); // NOTE: this skips the first line of ExampleData which is column names
//                                        while ((line = reader.readLine()) != null) {
//                                            myDb.insertData_fromDatabaseCSV(line);
//                                        }
//                                        Toast.makeText(getApplicationContext(), R.string.after_example_data_added, Toast.LENGTH_SHORT).show();
//                                    }
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                break;
//
//                            case 1: // user clicked "Database CSV"
//                                break;
//
//                            case 2: // user clicked "SCiO Format"
//                                break;
//
//                            case 3: // user clicked "BrAPI Format"
//                                break;
//
//                            default:
//                                break;
//                        }
//                        dialogInterface.dismiss();
//                    }
//                });
//                builder.show();
//            }
//        });
//    }
//
//    private void permissions_get() {
//        ActivityCompat.requestPermissions(this, new String[] {
//                Manifest.permission.INTERNET,
//                Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.ACCESS_NETWORK_STATE,
//                Manifest.permission.ACCESS_WIFI_STATE,
//                Manifest.permission.CAMERA}, 0);
//    }
//
//    private void permissions_check() {
//        // TODO: figure out why this closes the app even if the permissions are granted
//        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) ||
//                (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
//                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
//                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) ||
//                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) ||
//                (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
//            finish();
//        }
//    }
//}
