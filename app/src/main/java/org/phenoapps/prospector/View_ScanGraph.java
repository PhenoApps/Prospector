package org.phenoapps.prospector;

import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;

public class View_ScanGraph extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    GraphView graph;
    ImageButton toolbarImageButton_rename;
    ImageButton toolbarImageButton_edit;
    ImageButton toolbarImageButton_delete;

    // DECLARE GLOBAL VARIABLES
    DatabaseManager myDb;
    String observationUnitName;
    String[] items;
    boolean[] itemsChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view__scan);

        // INIT DISPLAY OBJECTS
        graph = findViewById(R.id.graph);
        toolbarImageButton_rename = findViewById(R.id.toolbarImageButton_rename);
        toolbarImageButton_edit = findViewById(R.id.toolbarImageButton_edit);
        toolbarImageButton_delete = findViewById(R.id.toolbarImageButton_delete);

        // INIT GLOBAL VARIABLES
        myDb = new DatabaseManager(this);
        observationUnitName = getIntent().getStringExtra("observationUnitName");
        populate_items();

        // CONFIGURE BUTTONS
        configure_toolbarImageButton_rename();
        configure_toolbarImageButton_edit();
        configure_toolbarImageButton_delete();

        // OTHER FUNCTION CALLS
        initGraph();
    }

    private void populate_items() {
        Cursor observationUnitIDs = myDb.get_ID_byObservationUnitName(observationUnitName); // TODO: make this dependant on observationUnitID
        items = new String[observationUnitIDs.getCount()];
        itemsChecked = new boolean[observationUnitIDs.getCount()];
        for (int i = 0; i < observationUnitIDs.getCount(); i++) {
            observationUnitIDs.moveToNext();
            items[i] = observationUnitIDs.getString(0);
            itemsChecked[i] = true; // initally all true because Prospector will automatically display all plots
        }
    }

    private void updateGraph() {
        graph.removeAllSeries();

        Cursor spectralValues;
        int id;
        for (int i = 0; i < items.length; i++) {
            id = Integer.parseInt(items[i]);

            if (itemsChecked[i]) {
                spectralValues = myDb.get_spectralValues_byID(id);
                while (spectralValues.moveToNext()) {
                    if (!spectralValues.getString(0).isEmpty()) {
                        // parse spectralValues
                        String[] data = spectralValues.getString(0).split(" ");
                        DataPoint[] dataPoints = new DataPoint[data.length];
                        for (int j = 0; j < data.length; j++) {
                            dataPoints[j] = new DataPoint(j, Math.round(Float.parseFloat(data[j])));
                        }
                        LineGraphSeries<DataPoint> plot = new LineGraphSeries<>(dataPoints);

                        // set line color to a value that remains consistent for easier comparison
                        plot.setColor(Color.rgb(
                                Math.round(255 * ((float)i / items.length)),
                                0,
                                Math.round(255 - (255 * ((float)i / items.length)))));
                        graph.addSeries(plot);
                    }
                }
            }
        }

    }

    private void initGraph() {
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

        // Add data to graph
        Cursor spectralValues = myDb.get_spectralValues_byObservationUnitName(observationUnitName);
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

    private void configure_toolbarImageButton_rename() {
        toolbarImageButton_rename.setOnClickListener(new View.OnClickListener() {
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

    private void configure_toolbarImageButton_edit() {
        toolbarImageButton_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(View_ScanGraph.this);
                builder.setTitle("Choose Scans to Display");
                builder.setMultiChoiceItems(items, itemsChecked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        itemsChecked[which] = isChecked;
                    }
                });
                builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        updateGraph();
                        dialogInterface.dismiss();
                    }
                });
                builder.show();
            }
        });
    }

    private void configure_toolbarImageButton_delete() {
        toolbarImageButton_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDb.delete_observationUnitName(observationUnitName);
                finish();
            }
        });
    }
}
