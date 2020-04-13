package org.phenoapps.prospector;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Selection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Selection_Scan extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    ListView listView_items;
    Button button_deleteScanAll;

    // DECLARE GLOBALS
    DatabaseManager myDb;
    ArrayList<String> listData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection__scan);

        // INIT TOOLBAR
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Prospector");
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // INIT DISPLAY OBJECTS
        listView_items = findViewById(R.id.listView_items);

        // INIT GLOBALS
        myDb = new DatabaseManager(this);
        listData = listView_items_populate();

        // CONFIGURE BUTTONS
        configure_listView_items();

        // OTHER FUNCTION CALLS
        permissions_check();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listData = listView_items_populate(); // used to make sure that the list displayed is actually the current database data
    }

    private void configure_listView_items() {
        listView_items.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getBaseContext(), View_ScanGraph.class);
                intent.putExtra("observationUnitName", listData.get(i));
                // Log.d("DEBUG", listData.get(i) + ", Integer: " + i);
                startActivity(intent);
            }
        });
    }

    private void deleteAllScans() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Selection_Scan.this);
        builder.setTitle("Are you sure you want to delete all scans?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                myDb.deleteAll();
                listView_items_populate();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.show();
    }

    private void importSampleScans() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(Selection_Scan.this);
        builder.setTitle("Select Import Method");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(Selection_Scan.this, android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("Example Data");

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0: // user clicked "Example Data"
                        try {
                            if (!myDb.isUnique_observationUnitName("sample_1")) {
                                Toast.makeText(getApplicationContext(), "Data was not added because \"sample_1\" already exists in the database.", Toast.LENGTH_SHORT).show();
                            } else if (!myDb.isUnique_observationUnitName("sample_2")) {
                                Toast.makeText(getApplicationContext(), "Data was not added because \"sample_2\" already exists in the database.", Toast.LENGTH_SHORT).show();
                            } else if (!myDb.isUnique_observationUnitName("sample_3")) {
                                Toast.makeText(getApplicationContext(), "Data was not added because \"sample_3\" already exists in the database.", Toast.LENGTH_SHORT).show();
                            } else {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("ExampleData.csv")));
                                String line = reader.readLine(); // NOTE: this skips the first line of ExampleData which is column names
                                while ((line = reader.readLine()) != null) {
                                    myDb.insertData_fromSimpleCSV(line);
                                }
                                Toast.makeText(getApplicationContext(), "Example data added to database.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;

                    case 1: // user clicked "Simple CSV"
                        break;

                    case 2: // user clicked "SCiO Format"
                        break;

                    case 3: // user clicked "BrAPI Format"
                        break;

                    default:
                        break;
                }
                dialogInterface.dismiss();
            }
        });
        builder.show();

        listData = listView_items_populate();
    }

    private void exportScans() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(Selection_Scan.this);
        builder.setTitle("Select Output Format");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(Selection_Scan.this, android.R.layout.select_dialog_singlechoice);
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
                switch (i) {
                    case 0: // user clicked "Simple CSV"
                        File csv_file = myDb.export_toSimpleCSV();
                        // TODO: figure out why this sometimes doesn't show all of the scans
                        myDb.scanFile(Selection_Scan.this, csv_file); // TODO: figure out how to move this into export_toCSV()
                        Toast.makeText(getApplicationContext(), "Exported to CSV. FIle located at " + csv_file.getPath(), Toast.LENGTH_LONG).show();
                        break;

                    case 1: // user clicked "SCiO Format"
                        File scio_file = myDb.export_toSCiO();
                        myDb.scanFile(Selection_Scan.this, scio_file);
                        Toast.makeText(getApplicationContext(), "Exported to SCiO Format. FIle located at " + scio_file.getPath(), Toast.LENGTH_LONG).show();
                        break;

                    case 2: // user clicked "BrAPI Format"
                        File brapi_file = myDb.export_toBrAPI();
                        myDb.scanFile(Selection_Scan.this, brapi_file);
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

    private void newScan() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
    }

    private void permissions_get() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CAMERA}, 0);
    }

    private void permissions_check() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            permissions_get();
        }
    }

    private ArrayList<String> listView_items_populate() {
        Cursor data = myDb.getAll_observationUnitName();
        final ArrayList<String> listData = new ArrayList<>();
        String observationUnitName;
        while (data.moveToNext()) {
            observationUnitName = data.getString(0);
            if (!listData.contains(observationUnitName)) {
                listData.add(observationUnitName);
            }
        }

        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listData);
        listView_items.setAdapter(adapter);

        return listData;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(Selection_Scan.this).inflate(R.menu.selection_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_scnas:
                deleteAllScans();
                break;
            case R.id.import_scans:
                importSampleScans();
                break;
            case R.id.export_scans:
                exportScans();
                break;
            case R.id.new_scan:
                newScan();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
