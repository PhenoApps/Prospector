//package consumerphysics.com.myscioapplication.activities;
package org.phenoapps.prospector.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
//import android.support.v4.app.ActivityCompat;
import androidx.core.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.consumerphysics.android.sdk.callback.cloud.ScioCloudAnalyzeManyCallback;
import com.consumerphysics.android.sdk.callback.cloud.ScioCloudSCiOVersionCallback;
import com.consumerphysics.android.sdk.callback.cloud.ScioCloudUserCallback;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceBatteryHandler;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceCalibrateHandler;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceCallbackHandler;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceScanHandler;
import com.consumerphysics.android.sdk.model.ScioBattery;
import com.consumerphysics.android.sdk.model.ScioModel;
import com.consumerphysics.android.sdk.model.ScioReading;
import com.consumerphysics.android.sdk.model.ScioUser;
import com.consumerphysics.android.sdk.sciosdk.ScioLoginActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import consumerphysics.com.myscioapplication.R;
import org.phenoapps.prospector.R;
import consumerphysics.com.myscioapplication.adapter.ScioModelAdapter;
import consumerphysics.com.myscioapplication.config.Constants;
import consumerphysics.com.myscioapplication.utils.StringUtils;

public final class MainActivity extends BaseScioActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int LOGIN_ACTIVITY_RESULT = 1000;

    // TODO: Put your redirect url here!
    private static final String REDIRECT_URL = "https://www.consumerphysics.com";

    // TODO: Put your app key here!
    private static final String APPLICATION_KEY = "4b5ac28b-28f9-4695-b784-b7665dfe3763";

    // UI
    private TextView nameTextView;
    private TextView addressTextView;
    private TextView statusTextView;
    private TextView usernameTextView;
    private TextView modelTextView;
    private TextView version;
    private TextView statusSensorTextView;
    private ProgressDialog progressDialog;

    // Members
    private String deviceName;
    private String deviceAddress;
    private String username;
    private String modelId;
    private String modelName;

    // SCiO
    private ScioReading scan;
    private ScioDeviceCalibrateHandler scioDeviceCalibrateHandler;
    private ScioDeviceScanHandler scioDeviceScanHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initHandlers();

        initUI();
        requestLocationPermission();
    }

    private void initHandlers() {
        scioDeviceScanHandler = new ScioDeviceScanHandler() {
            @Override
            public void onSuccess(final ScioReading reading) {
                scan = reading;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Scan saved. You can now analyze it.", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onNeedCalibrate() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Can not scan. Calibration is needed", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while scanning", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });

            }

            @Override
            public void onTimeout() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Timeout while scanning", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }
        };

        scioDeviceCalibrateHandler = new ScioDeviceCalibrateHandler() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "SCiO was calibrated successfully", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while calibrating", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onTimeout() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Timeout while calibrating", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getScioCloud().hasAccessToken() && username == null) {
            getScioUser();
        }

        updateDisplay();
    }

    @Override
    protected void onStop() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case LOGIN_ACTIVITY_RESULT:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "We are logged in.");
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final String description = data.getStringExtra(ScioLoginActivity.ERROR_DESCRIPTION);
                            final int errorCode = data.getIntExtra(ScioLoginActivity.ERROR_CODE, -1);

                            Toast.makeText(MainActivity.this, "An error has occurred.\nError code: " + errorCode + "\nDescription: " + description, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                break;
        }
    }

    @Override
    public void onScioButtonClicked() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDisplay();
                Toast.makeText(getApplicationContext(), "SCiO button was pressed", Toast.LENGTH_SHORT).show();

                scanAndAnalyze();
            }
        });
    }

    @Override
    public void onScioConnectionFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDisplay();
                Toast.makeText(getApplicationContext(), "Connection to SCiO failed", Toast.LENGTH_SHORT).show();

                dismissingProgress();
            }
        });
    }

    private void dismissingProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onScioConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDisplay();
                Log.d(TAG, "SCiO was connected successfully");
                dismissingProgress();
            }
        });
    }

    @Override
    public void onScioDisconnected() {
        super.onScioDisconnected();

        Log.d(TAG, "SCiO disconnected.");

        updateDisplay();

        dismissingProgress();
    }

    // Button Actions
    public void doLogout(final View view) {
        if (getScioCloud() != null) {
            getScioCloud().deleteAccessToken();

            storeUsername(null);
            updateDisplay();
        }
    }

    // Button Actions
    public void clearScans(final View view) {
        if (getScioCloud() != null) {
            getScioCloud().clearScans();
            Toast.makeText(getApplicationContext(), "Scan session cleared", Toast.LENGTH_SHORT).show();
        }
    }

    public void doLogin(final View view) {
        if (!isLoggedIn()) {
            final Intent intent = new Intent(this, ScioLoginActivity.class);
            intent.putExtra(ScioLoginActivity.INTENT_REDIRECT_URI, REDIRECT_URL);
            intent.putExtra(ScioLoginActivity.INTENT_APPLICATION_ID, APPLICATION_KEY);

            startActivityForResult(intent, LOGIN_ACTIVITY_RESULT);
        }
        else {
            Log.d(TAG, "Already have token");

            getScioUser();
        }
    }

    public void startCachedScanning(View view) {
        if (!isDeviceConnected() || !isLoggedIn()) {
            Toast.makeText(getApplicationContext(), "Please connect to SCiO and login.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (StringUtils.isEmpty(modelId)) {
            Toast.makeText(getApplicationContext(), "Please select models", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MultiScanningActivity.class);
        ArrayList<String> models = new ArrayList<>(Arrays.asList(modelId.split(",")));
        intent.putStringArrayListExtra("models", models);
        startActivity(intent);
    }

    public void doDiscover(View view) {
        Intent intent = new Intent(this, DiscoverActivity.class);
        startActivity(intent);
    }

    public void getScioVersion(final View view) {
        if (!isScioSensorAvailable()) {
            getScioDeviceVersion();
        }
        else {
            if (isDeviceConnected()) {
                // both available. select
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Device
                                getScioDeviceVersion();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //Sensor
                                getScioSensorVersion();
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Which SCiO?").setPositiveButton("SCiO Device", dialogClickListener)
                        .setNegativeButton("SCiO Sensor", dialogClickListener).show();
            }
            else {
                getScioSensorVersion();
            }
        }


    }

    private void getScioDeviceVersion() {
        if (!isDeviceConnected()) {
            Toast.makeText(getApplicationContext(), "Device is not connected. No valid SCiO id.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Getting SCiO Version...", false);

        getScioCloud().getScioVersion(getScioDevice().getId(), new ScioCloudSCiOVersionCallback() {
            @Override
            public void onSuccess(final String scioVersion) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("SCiO Version")
                                .setMessage(scioVersion)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .show();

                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onError(int i, String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while get SCiO version", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }
        });
    }

    private void getScioSensorVersion() {
        progressDialog = ProgressDialog.show(this, "Please Wait", "Getting SCiO Version...", false);

        getScioCloud().getScioVersion(getScioSensor().getId(), new ScioCloudSCiOVersionCallback() {
            @Override
            public void onSuccess(final String scioVersion) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("SCiO Version")
                                .setMessage(scioVersion)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .show();

                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onError(int i, String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while get SCiO version", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }
        });
    }

    public void doModels(final View view) {
        if (!isLoggedIn()) {
            Toast.makeText(getApplicationContext(), "Can not select collection. User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        final Intent intent = new Intent(this, ModelActivity.class);
        startActivity(intent);
    }

    public void doCPModels(final View view) {
        if (!isLoggedIn()) {
            Toast.makeText(getApplicationContext(), "Can not get Consumer Physics models. User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        final Intent intent = new Intent(this, CPModelActivity.class);
        startActivity(intent);
    }

    public void doConnect(final View view) {
        if (deviceAddress == null) {
            Toast.makeText(getApplicationContext(), "No SCiO is selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getScioDevice() != null) {
            if (isDeviceConnected()) {
                Toast.makeText(getApplicationContext(), "Already connected to a SCiO device", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Connecting...", false);

        connect(deviceAddress);
    }

    public void doDisconnect(final View view) {
        if (!isDeviceConnected()) {
            Toast.makeText(getApplicationContext(), "SCiO not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Disconnecting...", false);

        disconnect();
    }

    public void doCalibrate(final View view) {
        if (!isDeviceConnected()) {
            Toast.makeText(getApplicationContext(), "Can not calibrate. SCiO is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Calibrating...", false);

        getScioDevice().calibrate(scioDeviceCalibrateHandler);
    }

    public void readBattery(final View view) {
        if (!isDeviceConnected()) {
            Toast.makeText(getApplicationContext(), "Can not read battery. SCiO is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Reading battery status...", false);

        getScioDevice().readBattery(new ScioDeviceBatteryHandler() {
            @Override
            public void onSuccess(final ScioBattery scioBattery) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Battery Status: Charge Percentage: " + scioBattery.getChargePercentage() + ", Is charging? " + scioBattery.isCharging(), Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while reading battery", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onTimeout() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Timeout while reading battery", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }
        });
    }

    public void checkCalibration(final View view) {
        if (!isDeviceConnected()) {
            Toast.makeText(getApplicationContext(), "Can not check if calibration is needed. SCiO is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getApplicationContext(), "SCiO Requires calibration? " + getScioDevice().isCalibrationNeeded(), Toast.LENGTH_SHORT).show();
    }

    public void doRename(final View view) {
        if (!isDeviceConnected()) {
            Toast.makeText(getApplicationContext(), "Can not rename device. SCiO is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Rename SCiO");
        alertDialog.setMessage("Enter new name");

        final EditText input = new EditText(MainActivity.this);
        input.setHint(nameTextView.getText().toString());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                getScioDevice().renameDevice(input.getText().toString(), new ScioDeviceCallbackHandler() {
                    @Override
                    public void onSuccess() {
                        storeDeviceName(input);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Device renamed.", Toast.LENGTH_SHORT).show();
                                nameTextView.setText(input.getText().toString());
                            }
                        });
                    }

                    @Override
                    public void onError() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Rename device failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onTimeout() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Rename device failed due to SCiO timeout", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private void storeDeviceName(final EditText input) {
        getSharedPreferences().edit().putString(Constants.SCIO_NAME, input.getText().toString()).commit();
    }

    public void doScan(final View view) {
        if (!isDeviceConnected()) {
            Toast.makeText(getApplicationContext(), "Can not scan. SCiO is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (modelId == null) {
            Toast.makeText(getApplicationContext(), "Can not scan. Model was not selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isLoggedIn()) {
            Toast.makeText(getApplicationContext(), "Can not scan. User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Scanning...", false);

        getScioDevice().scan(scioDeviceScanHandler);
    }

    public void doScanSensor(View view) {
        if (isScioSensorAvailable()) {
            if (modelId == null) {
                Toast.makeText(getApplicationContext(), "Can not scan. Model was not selected.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isLoggedIn()) {
                Toast.makeText(getApplicationContext(), "Can not scan. User is not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog = ProgressDialog.show(this, "Please Wait", "Analyzing...", false);

            getScioSensor().scan(scioDeviceScanHandler);
        }
        else {
            Toast.makeText(getApplicationContext(), "SCiO Sensor not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanAndAnalyze() {
        if (!isDeviceConnected()) {
            Toast.makeText(getApplicationContext(), "Can not scan. SCiO is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (modelId == null) {
            Toast.makeText(getApplicationContext(), "Can not scan. Model was not selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isLoggedIn()) {
            Toast.makeText(getApplicationContext(), "Can not scan. User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Analyzing...", false);

        getScioDevice().scan(new ScioDeviceScanHandler() {
            @Override
            public void onSuccess(final ScioReading reading) {
                analyzeScioReading(reading);
            }

            @Override
            public void onNeedCalibrate() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Can not scan. Calibration is needed", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while scanning", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });

            }

            @Override
            public void onTimeout() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Timeout while scanning", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }
        });
    }

    // Private
    private void getScioUser() {
        progressDialog = ProgressDialog.show(this, "Please Wait", "Getting User Info...", true);

        getScioCloud().getScioUser(new ScioCloudUserCallback() {
            @Override
            public void onSuccess(final ScioUser user) {
                storeUsername(user.getUsername());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Welcome " + user.getFirstName() + " " + user.getLastName(), Toast.LENGTH_SHORT).show();
                        updateDisplay();
                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onError(final int code, final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while getting the user info.", Toast.LENGTH_SHORT).show();
                        dismissingProgress();
                    }
                });
            }
        });
    }

    private void showAnalyzeResults(final List<ScioModel> models) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Results");

        final LayoutInflater inflater = getLayoutInflater();
        final View convertView = inflater.inflate(R.layout.results_view, null);
        builder.setView(convertView);

        final ArrayList<ScioModel> arrayOfModels = new ArrayList<>();
        final ScioModelAdapter scioModelAdapter = new ScioModelAdapter(this, arrayOfModels);

        final ListView listView = (ListView) convertView.findViewById(R.id.results);
        listView.setAdapter(scioModelAdapter);

        scioModelAdapter.addAll(models);

        builder.setPositiveButton("OK", null);
        builder.setCancelable(true);

        builder.create().show();
    }

    private void initUI() {
        nameTextView = (TextView) findViewById(R.id.tv_scio_name);
        addressTextView = (TextView) findViewById(R.id.tv_scio_address);
        statusTextView = (TextView) findViewById(R.id.tv_scio_status);
        usernameTextView = (TextView) findViewById(R.id.tv_username);
        statusSensorTextView = (TextView) findViewById(R.id.tv_scio_status_sensor);
        modelTextView = (TextView) findViewById(R.id.tv_model);
        version = (TextView) findViewById(R.id.version);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version.setText("v" + pInfo.versionName);
        }
        catch (PackageManager.NameNotFoundException e) {
        }

        if (isScioSensorAvailable()) {
            statusSensorTextView.setText("Available");
        }
        else {
            statusSensorTextView.setText("Not Available");
        }
    }

    private void storeUsername(final String username) {
        this.username = username;
        getSharedPreferences().edit().putString(Constants.USER_NAME, username).commit();
    }

    private void updateDisplay() {
        final SharedPreferences pref = getSharedPreferences();

        deviceName = pref.getString(Constants.SCIO_NAME, null);
        deviceAddress = pref.getString(Constants.SCIO_ADDRESS, null);
        username = pref.getString(Constants.USER_NAME, null);
        modelName = pref.getString(Constants.MODEL_NAME, null);
        modelId = pref.getString(Constants.MODEL_ID, null);

        nameTextView.setText(deviceName);
        addressTextView.setText(deviceAddress);
        usernameTextView.setText(username);
        modelTextView.setText(modelName);

        if (!isDeviceConnected()) {
            statusTextView.setText("Disconnected");
        }
        else {
            statusTextView.setText("Connected");
        }
    }

    private void analyzeScioReading(ScioReading reading) {
        if (reading == null && scan == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Please perform a scan before analyzing.", Toast.LENGTH_SHORT).show();
                    dismissingProgress();
                }
            });

            return;
        }

        ScioReading scioReading = reading == null ? scan : reading;

        // ScioReading object is Serializable and can be saved to be used later for analyzing.
        List<String> modelsToAnalyze = new ArrayList<>();
        modelsToAnalyze.addAll(Arrays.asList(modelId.split(",")));

        progressDialog = ProgressDialog.show(this, "Please Wait", "Analyzing...", false);

        getScioCloud().analyze(scioReading, modelsToAnalyze, new ScioCloudAnalyzeManyCallback() {
            @Override
            public void onSuccess(final List<ScioModel> models) {
                Log.d(TAG, "analyze onSuccess");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAnalyzeResults(models);
                        dismissingProgress();
                    }
                });
            }

            @Override
            public void onError(final int code, final String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while analyzing: " + msg, Toast.LENGTH_LONG).show();
                        dismissingProgress();
                    }
                });
            }
        });
    }

    public void doAnalyze(View view) {
        analyzeScioReading(null);
    }

    public void checkCalibrationSensor(View view) {
        if (isScioSensorAvailable()) {
            Toast.makeText(getApplicationContext(), "SCiO Sensor Requires calibration? " + getScioSensor().isCalibrationNeeded(), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "SCiO Sensor not available", Toast.LENGTH_SHORT).show();
        }
    }

    public void doCalibrateSensor(View view) {
        if (isScioSensorAvailable()) {

            progressDialog = ProgressDialog.show(this, "Please Wait", "Calibrating...", false);

            getScioSensor().calibrate(new ScioDeviceCalibrateHandler() {
                @Override
                public void onSuccess() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "SCiO was calibrated successfully", Toast.LENGTH_SHORT).show();
                            dismissingProgress();
                        }
                    });
                }

                @Override
                public void onError() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Error while calibrating", Toast.LENGTH_SHORT).show();
                            dismissingProgress();
                        }
                    });
                }

                @Override
                public void onTimeout() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Timeout while calibrating", Toast.LENGTH_SHORT).show();
                            dismissingProgress();
                        }
                    });
                }
            });
        }
        else {
            Toast.makeText(getApplicationContext(), "SCiO Sensor not available", Toast.LENGTH_SHORT).show();
        }
    }

    public void doGetIdSensor(View view) {
        if (isScioSensorAvailable()) {
            Toast.makeText(getApplicationContext(), "SCiO Sensor ID: " + getScioSensor().getId(), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "SCiO Sensor not available", Toast.LENGTH_SHORT).show();
        }
    }

    public void requestLocationPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION}, 111);
        }
    }

}