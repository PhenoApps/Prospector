package org.phenoapps.prospector;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

public class Selection_Scan extends AppCompatActivity {
    // DECLARE DISPLAY OBJECTS
    ListView listView_items;
    Button button_deleteScanAll;

    // DECLARE GLOBALS
    DatabaseManager myDb;
    ArrayList<String> listData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection__scan);

        // INIT TOOLBAR
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Prospector");
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // INIT DISPLAY OBJECTS
        listView_items = findViewById(R.id.listView_items);

        // INIT GLOBALS
        myDb = new DatabaseManager(this);
        listData = listView_items_populate();

        // CONFIGURE BUTTONS
        configure_listView_items();

        // OTHER FUNCTION CALLS
        permissions_check();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listData = listView_items_populate(); // used to make sure that the list displayed is actually the current database data
        // NOTE: this function is used when "deleteScan" is called in View_ScanGraph activity
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);

        if (requestCode == 0 && dataIntent != null) { // Called when View_FileScan activity is closed
            if (resultCode == RESULT_OK) {
                Log.d("DEBUG", dataIntent.toString());

                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(dataIntent.getData());
                    myDb.export_toSimpleCSV_withOutputStream(outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }

//                File f = new File(dataIntent.getData().getPath());
//                Log.d("DEBUG", "Absolute path: " + f.getAbsoluteFile());

//                Uri uri = dataIntent.getData();
//                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
//                String path = getPath(this, docUri);
//                Log.d("DEBUG", path);

//                Uri uri = DocumentsContract.buildDocumentUriUsingTree(dataIntent.getData(), "Log.csv");
//                Log.d("DEBUG", uri.getPath());

//                File sdCard = Environment.getExternalStorageDirectory();
//                File dir = new File (sdCard.getAbsoluteFile() + "/Download");
//                dir.mkdirs();
//                File csv_file = new File(uri, "Log.csv");
//                csv_file.createNewFile();
//                Log.d("DEBUG", csv_file.getAbsolutePath());
//                FileOutputStream out = new FileOutputStream(csv_file);

//                String FilePath = dataIntent.getData().getPath();
//                Log.d("DEBUG", FilePath);

//                exportScans(f);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: move this set of commands to a separate file
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void configure_listView_items() {
        listView_items.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getBaseContext(), View_ScanGraph.class);
                intent.putExtra("observationUnitName", listData.get(i));
                // Log.d("DEBUG", listData.get(i) + ", Integer: " + i);
                startActivity(intent);
            }
        });
    }

    private void deleteAllScans() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Selection_Scan.this);
        builder.setTitle("Are you sure you want to delete all scans?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                myDb.deleteAll();
                listView_items_populate();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.show();
    }

    private void importSampleScans() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(Selection_Scan.this);
        builder.setTitle("Select Import Method");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(Selection_Scan.this, android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("Example Data");

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0: // user clicked "Example Data"
                        try {
                            if (!myDb.isUnique_observationUnitName("sample_1")) {
                                Toast.makeText(getApplicationContext(), "Data was not added because \"sample_1\" already exists in the database.", Toast.LENGTH_SHORT).show();
                            } else if (!myDb.isUnique_observationUnitName("sample_2")) {
                                Toast.makeText(getApplicationContext(), "Data was not added because \"sample_2\" already exists in the database.", Toast.LENGTH_SHORT).show();
                            } else if (!myDb.isUnique_observationUnitName("sample_3")) {
                                Toast.makeText(getApplicationContext(), "Data was not added because \"sample_3\" already exists in the database.", Toast.LENGTH_SHORT).show();
                            } else {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("ExampleData.csv")));
                                String line = reader.readLine(); // NOTE: this skips the first line of ExampleData which is column names
                                while ((line = reader.readLine()) != null) {
                                    myDb.insertData_fromSimpleCSV(line);
                                }
                                Toast.makeText(getApplicationContext(), "Example data added to database.", Toast.LENGTH_SHORT).show();
                                listData = listView_items_populate(); // NOTE: this should not move. I tried moving it to the end of the function, but the view does not appear to update if called then
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;

                    case 1: // user clicked "Simple CSV"
                        break;

                    case 2: // user clicked "SCiO Format"
                        break;

                    case 3: // user clicked "BrAPI Format"
                        break;

                    default:
                        break;
                }
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void exportScans(final File pathForExport) {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(Selection_Scan.this);
        builder.setTitle("Select Output Format");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(Selection_Scan.this, android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("Simple CSV");
        arrayAdapter.add("SCiO Format");
        arrayAdapter.add("BrAPI Format");

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0: // user clicked "Simple CSV"
                        File csv_file = myDb.export_toSimpleCSV();
                        // TODO: figure out why this sometimes doesn't show all of the scans
                        myDb.scanFile(Selection_Scan.this, csv_file); // TODO: figure out how to move this into export_toCSV()
                        Toast.makeText(getApplicationContext(), "Exported to CSV. FIle located at " + csv_file.getPath(), Toast.LENGTH_LONG).show();
                        break;

                    case 1: // user clicked "SCiO Format"
                        File scio_file = myDb.export_toSCiO();
                        myDb.scanFile(Selection_Scan.this, scio_file);
                        Toast.makeText(getApplicationContext(), "Exported to SCiO Format. FIle located at " + scio_file.getPath(), Toast.LENGTH_LONG).show();
                        break;

                    case 2: // user clicked "BrAPI Format"
                        File brapi_file = myDb.export_toBrAPI();
                        myDb.scanFile(Selection_Scan.this, brapi_file);
                        Toast.makeText(getApplicationContext(), "Exported to SCiO Format. FIle located at " + brapi_file.getPath(), Toast.LENGTH_LONG).show();
                        break;

                    default:
                        break;
                }
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void newScan() {
        Intent i = new Intent(getApplicationContext(), MainActivity_LinkSquare.class);
        startActivity(i);
    }

    private void permissions_get() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CAMERA}, 0);
    }

    private void permissions_check() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            permissions_get();
        }
    }

    private ArrayList<String> listView_items_populate() {
        Cursor data = myDb.getAll_observationUnitName();
        final ArrayList<String> listData = new ArrayList<>();
        String observationUnitName;
        while (data.moveToNext()) {
            observationUnitName = data.getString(0);
            if (!listData.contains(observationUnitName)) {
                listData.add(observationUnitName);
            }
        }

        final ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listData);
        listView_items.setAdapter(adapter);

        return listData;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(Selection_Scan.this).inflate(R.menu.selection_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_scnas:
                deleteAllScans();
                break;
            case R.id.import_scans:
                importSampleScans();
                break;
            case R.id.export_scans:
//                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//                i.addCategory(Intent.CATEGORY_DEFAULT);
//                startActivityForResult(Intent.createChooser(i, "Choose directory"), 0);

//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("file/*");
//                startActivityForResult(intent, 0);

                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, 0);

                break;
            case R.id.new_scan:
                newScan();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
