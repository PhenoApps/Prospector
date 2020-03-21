package com.stratio.linksquare.androidexample;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class View_ScanGraph extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    GraphView graph;

    // DECLARE GLOBAL VARIABLES
    DatabaseManager myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view__scan);

        // INIT DISPLAY OBJECTS
        graph = findViewById(R.id.graph);

        // INIT GLOBAL VARIABLES
        myDb = new DatabaseManager(this);

        String localScanID = getIntent().getStringExtra("localScanID");
        Log.d("DEBUG", localScanID);

        int frame = 1;
        while (true) {
            Cursor spectralValues = myDb.getSpectralValues(localScanID + "_Frame" + frame);

            Log.d("DEBUG", localScanID + "_Frame" + Integer.toString(frame));

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
