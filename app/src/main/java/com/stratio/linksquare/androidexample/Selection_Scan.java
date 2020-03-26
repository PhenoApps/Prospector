package com.stratio.linksquare.androidexample;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class Selection_Scan extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    ListView listView_items;
    Button button_deleteScanAll;

    // DECLARE GLOBALS
    DatabaseManager myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection__scan);

        // INIT DISPLAY OBJECTS
        listView_items = findViewById(R.id.listView_items);
        button_deleteScanAll = findViewById(R.id.button_deleteScanAll);

        // INIT GLOBALS
        myDb = new DatabaseManager(this);

        // CONFIGURE BUTTONS
        configure_listView_items();
        configure_button_DeleteScanAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listView_items_populate(); // used to make sure that the list displayed is actually the current database data
    }

    private ArrayList<String> listView_items_populate() {
        Cursor data = myDb.getData();
        final ArrayList<String> listData = new ArrayList<>();

        String lastScanName = "";
        String nextScanName;

        while (data.moveToNext()) {
            nextScanName = data.getString(6).substring(0, data.getString(6).lastIndexOf("_"));

            if (! nextScanName.equals(lastScanName)) {
                listData.add(nextScanName);
                lastScanName = nextScanName;
            }
        }

        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listData);
        listView_items.setAdapter(adapter);

        return listData;
    }

    private void configure_listView_items() {
        final ArrayList<String> listData = listView_items_populate();

        listView_items.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getBaseContext(), View_ScanGraph.class);
                intent.putExtra("localScanID", listData.get(i));
                startActivity(intent);
            }
        });
    }

    private void configure_button_DeleteScanAll() {
        button_deleteScanAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Selection_Scan.this);
                builder.setTitle("Are you sure you want to delete all scans?");

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        myDb.deleteScanAll();
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
}
