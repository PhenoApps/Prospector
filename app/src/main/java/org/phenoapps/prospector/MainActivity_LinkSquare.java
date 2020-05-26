package org.phenoapps.prospector;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.stratiotechnology.linksquareapi.LSFrame;
import com.stratiotechnology.linksquareapi.LinkSquareAPI;

import java.util.ArrayList;
import java.util.List;

public class MainActivity_LinkSquare extends AppCompatActivity implements LinkSquareAPI.LinkSquareAPIListener {

    // Dynamic Loading LinkSqaureAPI Library
    static {
        System.loadLibrary("LinkSquareAPI");
    }

    // Get LinkSquareAPI Instance
    LinkSquareAPI linkSqaureAPI = LinkSquareAPI.getInstance();

    // DECLARE DISPLAY OBJECTS
    Button button_connect;
    Button button_scan;
    Button button_close;
    TextView textView_connect;
    TextView textView_scan;
    TextView textView_close;

    // DECLARE GLOBALS
    final Context context = this;
    DatabaseManager myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_linksquare);

        // INIT DISPLAY OBJECTS
        button_connect = (Button) findViewById(R.id.button_connect);
        button_scan = (Button) findViewById(R.id.button_scan);
        button_close = (Button) findViewById(R.id.button_close);
        textView_connect = (TextView) findViewById(R.id.textView_connect);
        textView_scan = (TextView) findViewById(R.id.textView_scan);
        textView_close = (TextView) findViewById(R.id.textView_close);

        // INIT GLOBALS
        myDb = new DatabaseManager(this);

        // CONFIGURE BUTTONS
        configure_button_connect();
        configure_button_scan();
        configure_button_close();
    }

    public void configure_button_connect() {
        button_connect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Check for the correct Wifi Network
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (!wifiInfo.getSSID().contains("LS1-")) {
                    Log.e("ERROR", "You may be connected to the wrong WiFi network.\n You are connected to " + wifiInfo.getSSID());
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "You may be connected to the wrong WiFi network. You need to join LS1-0102315.",
                            Toast.LENGTH_SHORT);

                    toast.show();
                }

                // Initialize
                linkSqaureAPI.Initialize();


                // Add Event Listener to Receive Device Button Event
                linkSqaureAPI.SetEventListener(MainActivity_LinkSquare.this);


                // Connect to LinkSquare
                String IP = "192.168.1.1"; // Change IP


                int result = linkSqaureAPI.Connect(IP, 18630);
                if (result != LinkSquareAPI.RET_OK) {
                    textView_connect.setText("Result: " + linkSqaureAPI.GetLSError()); // Get Error Message
                } else {
                    LinkSquareAPI.LSDeviceInfo deviceInfo = linkSqaureAPI.GetDeviceInfo();

                    String strDesc = "Result: OK.\n";
                    strDesc += "Alias:" + deviceInfo.Alias + "\n";
                    strDesc += "SW Ver:" + deviceInfo.SWVersion + "\n";
                    strDesc += "HW Ver:" + deviceInfo.HWVersion + "\n";
                    strDesc += "DeviceID:" + deviceInfo.DeviceID + "\n";
                    strDesc += "OPMode:" + deviceInfo.OPMode + "\n";
                    textView_connect.setText(strDesc);
                }
            }
        });
    }

    public void configure_button_scan() {
        button_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LinearLayout layout_newScan = new LinearLayout(MainActivity_LinkSquare.this);
                layout_newScan.setOrientation(LinearLayout.VERTICAL);
                final EditText input_sampleName = new EditText(MainActivity_LinkSquare.this);
                layout_newScan.addView(input_sampleName);
                final TextView text1 = new TextView(MainActivity_LinkSquare.this);
                text1.setText("Additional Scan Notes");
                layout_newScan.addView(text1);
                final EditText input_sampleNote = new EditText(MainActivity_LinkSquare.this);
                layout_newScan.addView(input_sampleNote);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity_LinkSquare.this);
                builder.setTitle("Sample Name");
                builder.setView(layout_newScan);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String observationUnitName = input_sampleName.getText().toString();
                        final String scanNote = input_sampleNote.getText().toString();
                        if (myDb.isUnique_observationUnitName(observationUnitName)) {
                            saveScan(observationUnitName, scanNote);
                        } else {
                            AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity_LinkSquare.this);
                            builder2.setTitle("Duplicate Sample Name");
                            builder2.setMessage("Add this scan to the existing sample?");
                            builder2.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    saveScan(observationUnitName, scanNote);
                                }
                            });
                            builder2.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                            builder2.show();
                        }
                        dialog.cancel();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.setNeutralButton("Scan QR Code", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivityForResult(new Intent(getApplicationContext(), View_QRScanner.class), 0);
                    }
                });

                builder.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);

        if (requestCode == 0) { // Called when View_QRScanner activity is closed
            if (resultCode == RESULT_OK) {
                saveScan(dataIntent.getStringExtra("qr_result"), ""); // TODO: figure out a better option for scanNotes
            }
        }
    }

    public void saveScan(String localScanID, String scanNote) {
        // check to see if new localScanID contains any invalid characters
        // commas and tildas are used as delimiters in the database
        if (localScanID.contains(",") || localScanID.contains("~")) {
            Toast.makeText(getApplicationContext(), "Scan name cannot contain spaces.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (scanNote.equals("")) {
            scanNote = " ";
        }

        List<LSFrame> frames = new ArrayList<LSFrame>();

        // Scan
        int result = linkSqaureAPI.Scan(3, 3, frames); // (number of frames using light source 1, number of frames using light source 2, List to store frames in)
        if (result != LinkSquareAPI.RET_OK) {
            textView_scan.setText("Result: " + linkSqaureAPI.GetLSError());
        } else {
            for (int i = 0; i < frames.size(); i++) {
                LSFrame frm = frames.get(i);
                StringBuilder str = new StringBuilder();

                /**
                 * Data is passed to DB in the following format:
                 *     COL2 = "deviceID";
                 *     COL3 = "observationUnitID";
                 *     COL4 = "observationUnitName";
                 *     COL5 = "observationUnitBarcode";
                 *     COL6 = "frameNumber";
                 *     COL7 = "lightSource";
                 *     COL8 = "spectralValuesCount";
                 *     COL9 = "spectralValues";
                 *
                 *     NOTE: this format should NOT CHANGE!!
                 *     The DatabaseManager parser is dependant on correctly formatted data!!
                 */

                // This StringBuilder uses a "~" as the delimiter

                // Append "deviceID"
                LinkSquareAPI.LSDeviceInfo deviceInfo = linkSqaureAPI.GetDeviceInfo();
                str.append(deviceInfo.DeviceID + "~");

                // Append "observationUnitID"
                str.append(0 + "~");

                // Append "observationUnitName"
                str.append(localScanID + "~");

                // Append "observationUnitBarcode"
                str.append(0 + "~");

                // Append "frameNumber"
                str.append(frm.frameNo + "~");

                // Append "lightSource"
                str.append(frm.lightSource + "~");

                // Append "spectralValuesCount"
                str.append(frm.length + "~");

                // Append "spectralValues"
                for (int j = 0; j < frm.length; j++) {
                    str.append(frm.raw_data[j] + " ");
                    // str.append(frm.data[i] + " ");
                }
                str.append("~");

                // Append "scanNote"
                str.append(scanNote);

                String data = str.toString();
                Log.d("DEBUG", data);

                boolean upload_success = myDb.insertData_fromLinkSquare(data);
                if (upload_success) {
                    // TODO: figure out why this message lasts so long
                    Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Unsuccessful upload.", Toast.LENGTH_SHORT).show();
                }
            }
            textView_scan.setText("Result: OK.");
        }
    }

    public void configure_button_close() {
        button_close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Close
                linkSqaureAPI.Close();
                textView_close.setText("Result: OK.");
            }
        });
    }

    // Implements LinkSquareAPIListener Interface.
    @Override
    public void LinkSquareEventCallback(LinkSquareAPI.EventType eventType, int value) {
        if (eventType == LinkSquareAPI.EventType.Button) {

            List<LSFrame> frames = new ArrayList<LSFrame>();

            // Scan
            int result = linkSqaureAPI.Scan(3, 3, frames);
            if (result == LinkSquareAPI.RET_OK) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity_LinkSquare.this).create();
                        alertDialog.setTitle("Alert");
                        alertDialog.setMessage("LinkSquare Device Button!");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
                });
            }
        }
        else if (eventType == LinkSquareAPI.EventType.Timeout)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity_LinkSquare.this).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("LinkSquare Network Timeout!");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            });
        }
        else if (eventType == LinkSquareAPI.EventType.Disconnected)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity_LinkSquare.this).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("LinkSquare Network Closed!");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            });
        }
    }
}