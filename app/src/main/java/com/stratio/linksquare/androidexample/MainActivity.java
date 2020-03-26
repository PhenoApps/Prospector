package com.stratio.linksquare.androidexample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.stratiotechnology.linksquareapi.LSFrame;
import com.stratiotechnology.linksquareapi.LinkSquareAPI;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LinkSquareAPI.LinkSquareAPIListener {

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
        setContentView(R.layout.activity_main);

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
                linkSqaureAPI.SetEventListener(MainActivity.this);


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
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Scan Name");

                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String localScanID = input.getText().toString();
                        dialog.cancel();
                        saveScan(localScanID);
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

        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                saveScan(dataIntent.getStringExtra("qr_result"));
            }
        }
    }

    public void saveScan(String localScanID) {
        // check to see if new localScanID contains any invalid characters
        if (localScanID.contains(" ")) {
            Toast.makeText(getApplicationContext(), "Scan name cannot contain spaces.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check to see if new localScanID is unique
        Cursor allLocalScanID = myDb.getAllLocalScanID();
        boolean isUnique = true;
        while (allLocalScanID.moveToNext()) {
            if (allLocalScanID.getString(0).contains(localScanID + "_Frame")) {
                isUnique = false;
                break;
            }
        }
        if (isUnique == false) {
            Toast.makeText(getApplicationContext(), "Scan name is not unique. All scan names must be unique.", Toast.LENGTH_SHORT).show();
            return;
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

                /*
                str.append(String.format("Frame #%d, lightsource = %d\n", frm.frameNo, frm.lightSource));
                str.append(String.format("  Length = %d\n", frm.length));
                str.append(String.format("  raw_data = %f, %f, %f ...\n", frm.raw_data[0], frm.raw_data[1], frm.raw_data[2]));
                str.append(String.format("  data = %.3f, %.3f, %.3f, ...\n", frm.data[0], frm.data[1], frm.data[2]));
                */

                // Data is passed to DB in the following format
                // NOTE: this format should NOT CHANGE!!
                // The DatabaseManager parser is dependant on correctly formatted data!!
                str.append(localScanID + " " + frm.frameNo + " " + frm.lightSource + " " + frm.length + " ");
                for (int j = 0; j < frm.length; j++) {
                    str.append(frm.raw_data[j] + " " + frm.data[i] + " ");
                }
                LinkSquareAPI.LSDeviceInfo deviceInfo = linkSqaureAPI.GetDeviceInfo();
                str.append(deviceInfo.DeviceID);

                String data = str.toString();
                //Log.d("Debug", data);

                boolean upload_success = myDb.insertData(data);
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
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
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
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
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
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
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