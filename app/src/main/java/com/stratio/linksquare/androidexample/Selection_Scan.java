package com.stratio.linksquare.androidexample;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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
    Button button_viewScan;
    ListView listView_items;

    // DECLARE GLOBALS
    ArrayList<String> arrayList;
    ArrayAdapter arrayAdapter;
    DatabaseManager myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection__scan);

        // INIT DISPLAY OBJECTS
        button_moveBack = findViewById(R.id.button_moveBack);
        button_viewScan = findViewById(R.id.button_viewScan); // currently an empty object
        listView_items = findViewById(R.id.listView_items);

        // INIT GLOBALS
        arrayList = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
        myDb = new DatabaseManager(this);

        // CONFIGURE BUTTONS
        configure_button_moveBack();
        //configure_button_viewScan();
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

    private void configure_button_viewScan() {
        button_viewScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Selection_Scan.this, View_ScanGraph.class);
                startActivity(i);
            }
        });
    }
    private void configure_listView_items() {
        Cursor data = myDb.getData();
        ArrayList<String> listData = new ArrayList<>();
        while (data.moveToNext()) {
            listData.add(data.getString(6));
            arrayList.add(data.getString(6));
        }
        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listData);
        listView_items.setAdapter(adapter);

        listView_items.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(Selection_Scan.this,"clicked item: "+i+" "+arrayList.get(i),Toast.LENGTH_SHORT).show();
                // studyNameText.setText( "Study Name: " + arrayList.get(i).toString() );

                Intent intent = new Intent(getBaseContext(), View_ScanGraph.class);
                intent.putExtra("localScanID", arrayList.get(i));
                startActivity(intent);
            }
        });
    }
}
