package com.stratio.linksquare.androidexample;

import android.content.Context;
import android.content.DialogInterface;
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

    final Context context = this;

    Button btnConnect;
    Button btnScan;
    Button btnClose;

    TextView tvConnect;
    TextView tvScan;
    TextView tvClose;

    DatabaseManager myDb;

    // Get LinkSquareAPI Instance
    LinkSquareAPI linkSqaureAPI = LinkSquareAPI.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDb = new DatabaseManager(this);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Initialize
                linkSqaureAPI.Initialize();

                // Add Event Listener to Receive Device Button Event
                linkSqaureAPI.SetEventListener(MainActivity.this);


                // Connect to LinkSquare
                String IP = "192.168.1.1"; // Change IP


                int result = linkSqaureAPI.Connect(IP, 18630);
                if (result != LinkSquareAPI.RET_OK) {
                    tvConnect.setText("Result: " + linkSqaureAPI.GetLSError()); // Get Error Message
                } else {
                    LinkSquareAPI.LSDeviceInfo deviceInfo = linkSqaureAPI.GetDeviceInfo();

                    String strDesc = "Result: OK.\n";
                    strDesc += "Alias:" + deviceInfo.Alias + "\n";
                    strDesc += "SW Ver:" + deviceInfo.SWVersion + "\n";
                    strDesc += "HW Ver:" + deviceInfo.HWVersion + "\n";
                    strDesc += "DeviceID:" + deviceInfo.DeviceID + "\n";
                    strDesc += "OPMode:" + deviceInfo.OPMode + "\n";
                    tvConnect.setText(strDesc);
                }
            }
        });

        btnScan = (Button) findViewById(R.id.btnScan);
        btnScan.setOnClickListener(new View.OnClickListener() {
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

                        if (localScanID.contains(" ")) {
                            Toast.makeText(getApplicationContext(), "Name cannot contain spaces.", Toast.LENGTH_LONG).show();
                        } else {
                            List<LSFrame> frames = new ArrayList<LSFrame>();

                            // Scan
                            int result = linkSqaureAPI.Scan(3, 3, frames);
                            if (result != LinkSquareAPI.RET_OK) {
                                tvScan.setText("Result: " + linkSqaureAPI.GetLSError());
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

                                    str.append(localScanID + " " + frm.frameNo + " " + frm.lightSource + " " + frm.length + " ");
                                    for (int j = 0; j < frm.length; j++) {
                                        str.append(frm.raw_data[j] + " " + frm.data[i] + " ");
                                    }
                                    LinkSquareAPI.LSDeviceInfo deviceInfo = linkSqaureAPI.GetDeviceInfo();
                                    str.append(deviceInfo.DeviceID);

                                    String data = str.toString();
                                    boolean upload_success = myDb.insertData(data);
                                    if (upload_success) {
                                        Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_LONG).show();
                                        Log.d("Debug", data);
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Unsuccessful upload.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                tvScan.setText("Result: OK.");
                            }
                        }
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

        btnClose = (Button) findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Close
                linkSqaureAPI.Close();
                tvClose.setText("Result: OK.");
            }
        });

        tvConnect = (TextView) findViewById(R.id.tvConnect);
        tvScan = (TextView) findViewById(R.id.tvScan);
        tvClose = (TextView) findViewById(R.id.tvClose);
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