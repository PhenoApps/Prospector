//package consumerphysics.com.myscioapplication.activities;
package org.phenoapps.prospector.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.consumerphysics.android.sdk.callback.cloud.ScioCloudAnalyzeManyCallback;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceScanHandler;
import com.consumerphysics.android.sdk.model.ScioModel;
import com.consumerphysics.android.sdk.model.ScioReading;

import org.phenoapps.prospector.R;
import org.phenoapps.prospector.config.Constants;
import org.phenoapps.prospector.storage.ScanStorage;

import java.util.Date;
import java.util.List;

//import consumerphysics.com.myscioapplication.R;

/**
 * Created by nadavg on 19/07/2016.
 */
public class MultiScanningActivity extends BaseScioActivity {

    // UI
    private ListView listView;
    private MyAdapter adapter;
    private ProgressDialog progressDialog;
    private EditText numOfScans;
    private Handler handler;

    // Data
    private ScanStorage scanStorage;
    private int scans = 0;
    private int maxScans = 1;

    // Intent Params
    private List<String> modelIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        scanStorage = new ScanStorage();
        scanStorage.initScansStorage(this);
        getModels();

        super.onCreate(savedInstanceState);

        handler = new Handler();

        setContentView(R.layout.activity_cached_multi_scanning);

        initUI();
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
    public void onScioButtonClicked() {
        super.onScioButtonClicked();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isWorking()) {
                    return;
                }

                maxScans = Integer.valueOf(numOfScans.getText().toString());
                scans = 0;

                progressDialog = ProgressDialog.show(MultiScanningActivity.this, "Please wait", "Scanning", true);

                scan();
            }
        });
    }

    private boolean isWorking() {
        return progressDialog != null && progressDialog.isShowing();
    }

    @Override
    public void onScioConnectionFailed() {
        super.onScioConnectionFailed();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MultiScanningActivity.this, "Failed to connect to SCiO device.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void initUI() {
        initAnalyzedModels();

        numOfScans = (EditText) findViewById(R.id.numOfScans);

        listView = (ListView) findViewById(R.id.list);
        adapter = new MyAdapter(this, scanStorage.getFileNames());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                handleScanClicked(position);
            }
        });
    }

    private void initAnalyzedModels() {
        TextView explain = (TextView) findViewById(R.id.explain);

        String listString = "";
        for (String s : modelIds) {
            listString += s + "\n";
        }

        explain.setText(explain.getText().toString() + "\n\n" + getString(R.string.will_analyze_models) + listString);
    }

    private void handleScanClicked(final int position) {
        if (progressDialog != null && progressDialog.isShowing()) {
            return;
        }

        progressDialog = ProgressDialog.show(MultiScanningActivity.this, getString(R.string.please_wait), getString(R.string.analyzing), true);

        ScioReading scioReading = scanStorage.getScioReadings().get(position);
        getScioCloud().analyze(scioReading, modelIds, new ScioCloudAnalyzeManyCallback() {
            @Override
            public void onSuccess(List<ScioModel> list) {
                scanStorage.deleteScan(position);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        Toast.makeText(MultiScanningActivity.this, R.string.scan_analyze_success, Toast.LENGTH_SHORT).show();
                        dismissProgress();

                    }
                });
            }

            @Override
            public void onError(int i, final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MultiScanningActivity.this, getString(R.string.scan_analyze_fail) + errorMessage, Toast.LENGTH_SHORT).show();
                        dismissProgress();

                    }
                });
            }
        });
    }

    private void getModels() {
        modelIds = getIntent().getStringArrayListExtra("models");
    }

    private void scan() {
        getScioDevice().scan(new ScioDeviceScanHandler() {
            @Override
            public void onSuccess(ScioReading scioReading) {
                handleSuccessScans(scioReading);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (++scans < maxScans) {
                            scan();
                        }
                        else {
                            resetScan();
                        }
                    }
                });
            }

            @Override
            public void onNeedCalibrate() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MultiScanningActivity.this, R.string.ask_calibrate_device, Toast.LENGTH_SHORT).show();
                        resetScan();
                    }
                });
            }

            @Override
            public void onError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MultiScanningActivity.this, R.string.scio_scan_failed, Toast.LENGTH_SHORT).show();
                        resetScan();
                    }
                });
            }

            @Override
            public void onTimeout() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MultiScanningActivity.this, R.string.scio_timed_out, Toast.LENGTH_SHORT).show();
                        resetScan();
                    }
                });
            }
        });
    }

    private void resetScan() {
        scans = 0;
        dismissProgress();
    }

    private void handleSuccessScans(ScioReading scioReading) {
        scanStorage.saveScanToStorage(scioReading);
        scanStorage.loadScansFromStorage();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    /***************
     * Adapter
     **********************/

    private class MyAdapter extends ArrayAdapter<String> {
        public MyAdapter(Context context, List<String> users) {
            super(context, 0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String filename = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.simple_item, parent, false);
            }

            TextView tvName = (TextView) convertView.findViewById(R.id.itemTitle);
            tvName.setText(Constants.DATE_FORMAT.format(new Date(Long.valueOf(filename))));

            return convertView;
        }
    }
}
