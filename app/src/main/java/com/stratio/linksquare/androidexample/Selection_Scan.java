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
    Button button_moveBack;
    ListView listView_items;

    // DECLARE GLOBALS
    DatabaseManager myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection__scan);

        // INIT DISPLAY OBJECTS
        button_moveBack = findViewById(R.id.button_moveBack);
        listView_items = findViewById(R.id.listView_items);

        // INIT GLOBALS
        myDb = new DatabaseManager(this);

        // CONFIGURE BUTTONS
        configure_button_moveBack();
        configure_listView_items();
    }

    private void configure_button_moveBack() {
        button_moveBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void configure_listView_items() {
        Cursor data = myDb.getData();
        final ArrayList<String> listData = new ArrayList<>();

        StringBuilder s = new StringBuilder("testy1_asdfisf_asdfoijs_Frame6");
        Log.d("DEBUG", Integer.toString(s.lastIndexOf("_")));
        Log.d("DEBUG", s.substring(0,s.lastIndexOf("_")));

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
