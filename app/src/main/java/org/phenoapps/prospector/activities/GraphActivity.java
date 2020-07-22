//package org.phenoapps.prospector.activities;
//
//import android.content.DialogInterface;
//import android.database.Cursor;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.text.InputType;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.MenuItem;
//import android.widget.EditText;
//
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.jjoe64.graphview.GraphView;
//import com.jjoe64.graphview.series.DataPoint;
//import com.jjoe64.graphview.series.LineGraphSeries;
//
//import org.phenoapps.prospector.DatabaseManager;
//import org.phenoapps.prospector.R;
//
//import java.util.Random;
//
//public class GraphActivity extends AppCompatActivity {
//    // DECLARE DISPLAY OBJECTS
//    GraphView graph;
//
//    // DECLARE GLOBAL VARIABLES
//    //DatabaseManager myDb;
//    String observationUnitName;
//    String[] items;
//    boolean[] itemsChecked;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_view__scangraph);
//
//        // INIT TOOLBAR
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle(null);
//            getSupportActionBar().getThemedContext();
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setHomeButtonEnabled(true);
//        }
//
//        // INIT DISPLAY OBJECTS
//        graph = findViewById(R.id.graph);
//
//        // INIT GLOBAL VARIABLES
//        myDb = new DatabaseManager(this);
//        observationUnitName = getIntent().getStringExtra("observationUnitName");
//        populate_items();
//
//        // OTHER FUNCTION CALLS
//        initGraph();
//    }
//
//    private void populate_items() {
//        Cursor observationUnitIDs = myDb.get_ID_byObservationUnitName(observationUnitName); // TODO: make this dependant on observationUnitID
//        items = new String[observationUnitIDs.getCount()];
//        itemsChecked = new boolean[observationUnitIDs.getCount()];
//        for (int i = 0; i < observationUnitIDs.getCount(); i++) {
//            observationUnitIDs.moveToNext();
//            items[i] = observationUnitIDs.getString(0);
//            itemsChecked[i] = true; // initally all true because Prospector will automatically display all plots
//        }
//    }
//
//    private void updateGraph() {
//        graph.removeAllSeries();
//
//        Cursor spectralValues;
//        int id;
//        for (int i = 0; i < items.length; i++) {
//            id = Integer.parseInt(items[i]);
//
//            if (itemsChecked[i]) {
//                spectralValues = myDb.get_spectralValues_byID(id);
//                while (spectralValues.moveToNext()) {
//                    if (!spectralValues.getString(0).isEmpty()) {
//                        // parse spectralValues
//                        String[] data = spectralValues.getString(0).split(" ");
//                        DataPoint[] dataPoints = new DataPoint[data.length];
//                        for (int j = 0; j < data.length; j++) {
//                            dataPoints[j] = new DataPoint(j, Math.round(Float.parseFloat(data[j])));
//                        }
//                        LineGraphSeries<DataPoint> plot = new LineGraphSeries<>(dataPoints);
//
//                        // set line color to a value that remains consistent for easier comparison
//                        plot.setColor(Color.rgb(
//                                Math.round(255 * ((float) i / items.length)),
//                                0,
//                                Math.round(255 - (255 * ((float) i / items.length)))));
//                        graph.addSeries(plot);
//                    }
//                }
//            }
//        }
//    }
//
//    private void initGraph() {
//        // TODO: get maxx / maxy from scan graph programatically
//        // TODO: make sure these values work with other spectrometers as well
//        // Set manual X bounds on the graph
//        graph.getViewport().setXAxisBoundsManual(true);
//        graph.getViewport().setMinX(0);
//        graph.getViewport().setMaxX(600);
//
//        // Set manual Y bounds on the graph
//        graph.getViewport().setYAxisBoundsManual(true);
//        graph.getViewport().setMinY(0);
//        graph.getViewport().setMaxY(25);
//
//        // Enable scaling
//        graph.getViewport().setScalable(true);
//        graph.getViewport().setScalableY(true);
//
//        // Add data to graph
//        Cursor spectralValues = myDb.get_spectralValues_byObservationUnitName(observationUnitName);
//        while (spectralValues.moveToNext()) {
//            if (!spectralValues.getString(0).isEmpty()) {
//                // parse spectralValues
//                String[] data = spectralValues.getString(0).split(" ");
//                DataPoint[] dataPoints = new DataPoint[data.length];
//                for (int i = 0; i < data.length; i++) {
//                    dataPoints[i] = new DataPoint(i, Math.round(Float.parseFloat(data[i])));
//                }
//                LineGraphSeries<DataPoint> plot = new LineGraphSeries<>(dataPoints);
//
//                // give line a unique color
//                plot.setColor(Color.rgb(new Random().nextInt(10) * 25, new Random().nextInt(10) * 25, new Random().nextInt(10) * 25));
//                graph.addSeries(plot);
//            }
//        }
//    }
//
//    private void renameScan() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(GraphActivity.this);
//        builder.setTitle(R.string.updated_sample_name);
//
//        final EditText input = new EditText(GraphActivity.this);
//        input.setInputType(InputType.TYPE_CLASS_TEXT);
//        builder.setView(input);
//
//        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                String newObservationUnitName = input.getText().toString();
//                myDb.update_observationUnitName(observationUnitName, newObservationUnitName);
//                dialog.cancel();
//            }
//        });
//
//        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                dialogInterface.cancel();
//            }
//        });
//
//        builder.show();
//    }
//
//    private void editScan() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(GraphActivity.this);
//        builder.setTitle(R.string.choose_scans_to_display);
//        builder.setMultiChoiceItems(items, itemsChecked, new DialogInterface.OnMultiChoiceClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
//                itemsChecked[which] = isChecked;
//            }
//        });
//        builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                updateGraph();
//                dialogInterface.dismiss();
//            }
//        });
//        builder.show();
//    }
//
//    private void deleteScan() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(GraphActivity.this);
//        builder.setTitle(R.string.ask_delete_scan);
//
//        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                myDb.delete_observationUnitName(observationUnitName);
//                finish();
//            }
//        });
//        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                dialogInterface.cancel();
//            }
//        });
//
//        builder.show();
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        new MenuInflater(GraphActivity.this).inflate(R.menu.scangraph, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                finish();
//                break;
//            case R.id.scan_rename:
//                renameScan();
//                break;
//            case R.id.scan_edit:
//                editScan();
//                break;
//            case R.id.scan_delete:
//                deleteScan();
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//}