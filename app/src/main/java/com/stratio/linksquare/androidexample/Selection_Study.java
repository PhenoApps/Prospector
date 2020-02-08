package com.stratio.linksquare.androidexample;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class Selection_Study extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    Button button_moveSelectionStudy;
    Button button_moveBack;
    Button button_addStudy;
    TextView textView_deviceName;
    TextView textView_studyName;
    EditText editText_studyName;
    ListView listView_items;

    // DECLARE GLOBALS
    ArrayList<String> arrayList;
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection__study);

        // INIT DISPLAY OBJECTS
        button_moveSelectionStudy = findViewById(R.id.button_moveSelectionStudy);
        button_moveBack = findViewById(R.id.button_moveBack);
        button_addStudy = findViewById(R.id.button_addStudy);
        textView_deviceName = findViewById(R.id.textView_deviceName);
        textView_studyName = findViewById(R.id.textView_studyName);
        editText_studyName = findViewById(R.id.editText_newStudy);
        listView_items = findViewById(R.id.listView_items);

        // CONFIGURE BUTTONS
        configure_button_moveSelectionStudy();
        configure_button_moveBack();
        configure_listView_onClick();
        configure_button_addStudy();
    }

    private void configure_button_moveSelectionStudy() {
        button_moveSelectionStudy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Selection_Study.this, Selection_Group.class);
                Bundle b = getIntent().getExtras();

                // REMOVE UNNECESSARY TEXT BEFORE PASSING INTO BUNDLE
                String studyName = textView_studyName.getText().toString();
                studyName = studyName.substring("Study Name: ".length(), studyName.length());

                if (studyName.equals("")) {
                    Toast.makeText(Selection_Study.this, "You must select a Study first.", Toast.LENGTH_SHORT).show();
                } else {
                    // PASS STUDY NAME TO NEXT ACTIVITY VIA A BUNDLE
                    b.putString("study", studyName );
                    i.putExtras(b);
                    startActivity(i);
                }
            }
        });
    }

    private void configure_button_moveBack() {
        button_moveBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void configure_listView_onClick() {
        arrayList = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
        // TODO: get this ArrayList to reference the SQLite database

        arrayList.add("android");
        arrayList.add("is");
        arrayList.add("great");
        arrayList.add("and I love it");

        listView_items.setAdapter(arrayAdapter);

        listView_items.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(Selection_Study.this,"clicked item: "+i+" "+arrayList.get(i), Toast.LENGTH_SHORT).show();
                textView_studyName.setText( "Study Name: " + arrayList.get(i).toString() );
            }
        });
    }

    private void configure_button_addStudy() {
        button_addStudy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Selection_Study.this);
                builder.setCancelable(true);
                builder.setTitle("Confirmation Message");
                final String new_study_text = editText_studyName.getText().toString();
                builder.setMessage("Are you sure you want to add '" + new_study_text + "'?");
                builder.setPositiveButton("Confirm",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                arrayList.add(new_study_text);
                                listView_items.setAdapter(arrayAdapter);
                                arrayAdapter.notifyDataSetChanged();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                androidx.appcompat.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }
}
