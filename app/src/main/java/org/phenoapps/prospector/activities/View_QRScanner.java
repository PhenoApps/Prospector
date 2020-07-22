package org.phenoapps.prospector.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;

import org.phenoapps.prospector.R;

public class View_QRScanner extends AppCompatActivity {

    // DECLARE DISPLAY OBJECTS
    CodeScanner codeScanner;
    CodeScannerView scannerView;
    TextView resultData;

    // DECLARE GLOBAL VARIABLES

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view__qrscanner);

        // INIT TOOLBAR
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        scannerView = findViewById(R.id.codeScannerView_qrScanner);
        codeScanner = new CodeScanner(this, scannerView);
        resultData = findViewById(R.id.textView_qrResult);

        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultData.setText(result.getText());
                        getIntent().putExtra("qr_result", result.getText());
                        setResult(RESULT_OK, getIntent());
                        finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        codeScanner.startPreview();
    }
}