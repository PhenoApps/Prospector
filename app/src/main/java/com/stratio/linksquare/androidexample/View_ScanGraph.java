package com.stratio.linksquare.androidexample;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class View_ScanGraph extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    GraphView graph;
    Button button_deleteScan;
    Button button_renameScan;

    // DECLARE GLOBAL VARIABLES
    DatabaseManager myDb;
    String localScanID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view__scan);

        // INIT DISPLAY OBJECTS
        graph = findViewById(R.id.graph);
        button_deleteScan = findViewById(R.id.button_deleteScan);
        button_renameScan = findViewById(R.id.button_renameScan);

        // INIT GLOBAL VARIABLES
        myDb = new DatabaseManager(this);
        localScanID = getIntent().getStringExtra("localScanID");

        // CONFIGURE BUTTONS
        configure_button_deleteScan();
        configure_button_renameScan();

        // OTHER FUNCTION CALLS
        initGraph();
    }

    private void configure_button_deleteScan() {
        button_deleteScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDb.deleteLocalScanID(localScanID);
                finish();
            }
        });
    }

    private void configure_button_renameScan() {
        button_renameScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(View_ScanGraph.this);
                builder.setTitle("Updated Scan Name");

                final EditText input = new EditText(View_ScanGraph.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newLocalScanID = input.getText().toString();
                        myDb.updateLocalScanID(localScanID, newLocalScanID);
                        dialog.cancel();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                builder.show();
            }
        });
    }

    private void initGraph() {
        int frame = 1; // this var keeps track of what frame of each scan is about to be added to graph
        while (true) {
            Cursor spectralValues = myDb.getSpectralValues(localScanID + "_Frame" + frame);

            // Log.d("DEBUG", localScanID + "_Frame" + Integer.toString(frame));

            if (spectralValues.moveToFirst()) {
                String[] data = spectralValues.getString(0).split(" ");
                DataPoint[] dataPoints = new DataPoint[data.length];
                for (int i = 0; i < data.length; i++) {
                    dataPoints[i] = new DataPoint(i, Math.round(Float.parseFloat(data[i])));
                }
                LineGraphSeries<DataPoint> plot = new LineGraphSeries<>(dataPoints);
                // TODO: make each color more distinct
                // TODO: ensure that color values <= 255
                plot.setColor(Color.rgb(12*frame, 24*frame, 36*frame));
                graph.addSeries(plot);

                frame++;
            } else {
                break;
            }
        }
    }
}
