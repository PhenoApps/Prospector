package com.stratio.linksquare.androidexample;

import android.database.Cursor;
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
        Log.d("Debug", localScanID);

        Cursor spectralValues = myDb.getSpectralValues(localScanID);
        spectralValues.moveToNext();
        Log.d("Debug", spectralValues.getString(0));
        String[] data = spectralValues.getString(0).split(" ");
        DataPoint[] dataPoints = new DataPoint[data.length];
        for (int i = 0; i < data.length; i++) {
            dataPoints[i] = new DataPoint(i, Math.round(Float.parseFloat(data[i])));
        }
        LineGraphSeries<DataPoint> plot = new LineGraphSeries<>(dataPoints);
        graph.addSeries(plot);
    }
}
