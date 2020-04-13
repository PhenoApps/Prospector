package org.phenoapps.prospector;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Selection_Scan extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    ListView listView_items;
    Button button_deleteScanAll;
    ImageButton toolbarImageButton_import;
    ImageButton toolbarImageButton_export;
    ImageButton toolbarImageButton_add;

    // DECLARE GLOBALS
    DatabaseManager myDb;
    ArrayList<String> listData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection__scan);

        // INIT DISPLAY OBJECTS
        listView_items = findViewById(R.id.listView_items);
        button_deleteScanAll = findViewById(R.id.button_deleteScanAll);
        toolbarImageButton_import = findViewById(R.id.toolbarImageButton_import);
        toolbarImageButton_export = findViewById(R.id.toolbarImageButton_export);
        toolbarImageButton_add = findViewById(R.id.toolbarImageButton_add);

        // INIT GLOBALS
        myDb = new DatabaseManager(this);
        listData = listView_items_populate();

        // CONFIGURE BUTTONS
        configure_listView_items();
        configure_button_deleteScanAll();
        configure_toolbarImageButton_import();
        configure_toolbarImageButton_export();
        configure_toolbarImageButton_add();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listData = listView_items_populate(); // used to make sure that the list displayed is actually the current database data
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

    private void configure_button_deleteScanAll() {
        button_deleteScanAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        });
    }

    private void configure_toolbarImageButton_import() {
        toolbarImageButton_import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                        switch(i) {
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
            }
        });

        listData = listView_items_populate();
    }

    private void configure_toolbarImageButton_export() {
        toolbarImageButton_export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                        switch(i) {
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
        });
    }

    private void configure_toolbarImageButton_add() {
        toolbarImageButton_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            }
        });
    }
}
