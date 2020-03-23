package com.stratio.linksquare.androidexample;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class Selection_Scan extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    ListView listView_items;

    // DECLARE GLOBALS
    DatabaseManager myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection__scan);

        // INIT DISPLAY OBJECTS
        listView_items = findViewById(R.id.listView_items);

        // INIT GLOBALS
        myDb = new DatabaseManager(this);

        // CONFIGURE BUTTONS
        configure_listView_items();
    }

    private void configure_listView_items() {
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

        listView_items.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getBaseContext(), View_ScanGraph.class);
                intent.putExtra("localScanID", listData.get(i));
                startActivity(intent);
            }
        });
    }
}
