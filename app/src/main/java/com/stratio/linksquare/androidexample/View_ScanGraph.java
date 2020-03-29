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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;

public class View_ScanGraph extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    GraphView graph;
    Button button_deleteScan;
    Button button_renameScan;

    // DECLARE GLOBAL VARIABLES
    DatabaseManager myDb;
    String observationUnitName;

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
        observationUnitName = getIntent().getStringExtra("observationUnitName");

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
                myDb.delete_observationUnitName(observationUnitName);
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
                        String newObservationUnitName = input.getText().toString();
                        myDb.update_observationUnitName(observationUnitName, newObservationUnitName);
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
        Cursor spectralValues = myDb.get_spectralValues(observationUnitName);

        // TODO: make sure these values work with other spectrometers as well
        // Set manual X bounds on the graph
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(600);

        // Set manual Y bounds on the graph
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(25);

        // Enable scaling
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);

        while (spectralValues.moveToNext()) {
            if (!spectralValues.getString(0).isEmpty()) {
                // parse spectralValues
                String[] data = spectralValues.getString(0).split(" ");
                DataPoint[] dataPoints = new DataPoint[data.length];
                for (int i = 0; i < data.length; i++) {
                    dataPoints[i] = new DataPoint(i, Math.round(Float.parseFloat(data[i])));
                }
                LineGraphSeries<DataPoint> plot = new LineGraphSeries<>(dataPoints);

                // give line a unique color
                plot.setColor(Color.rgb(new Random().nextInt(10) * 25, new Random().nextInt(10) * 25, new Random().nextInt(10) * 25));
                graph.addSeries(plot);
            }
        }
    }
}
