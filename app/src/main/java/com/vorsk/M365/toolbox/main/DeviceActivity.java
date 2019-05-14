package com.vorsk.M365.toolbox.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class DeviceActivity extends Activity {

    private final static String TAG = DeviceActivity.class.getSimpleName();

    private Scooter scooter;

    private TextView conn_status;
    private TextView pwdata;
    private TextView sndata;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device);

        conn_status = findViewById(R.id.conn_status);
        pwdata = findViewById(R.id.pwdata);
        sndata = findViewById(R.id.sndata);

        final Intent intent = getIntent();
        String mDeviceName = intent.getStringExtra(Scooter.EXTRAS_DEVICE_NAME);
        String mDeviceAddress = intent.getStringExtra(Scooter.EXTRAS_DEVICE_ADDRESS);

        scooter = new Scooter(this, mDeviceName, mDeviceAddress);
    }

    /*@Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(scooter);
    }*/

    public void getPW(View view) {
        scooter.requestPW();
    }

    public void getSN(View view) {
        scooter.requestSN();
    }

    public void unlock(View view) {
        scooter.unlock();
    }

    public void lock(View view) {
        scooter.lock();
    }

    public void connect(View view) {
        Log.d(TAG, "press connect");
        scooter.connect();
    }

    public void setConnStatus(String str) {
        //conn_status.setText(str);
        //Thread t = new Thread() {
        //    public void run() {
                runOnUiThread(() -> conn_status.setText(str));
         //   }
        //};
        //t.start();
    }

    public void setPWView(String str) {
        Thread t = new Thread() {
            public void run() {
                runOnUiThread(() -> pwdata.setText(str));
            }
        };
        t.start();
    }

    public void setSNView(String str) {
        Thread t = new Thread() {
            public void run() {
                runOnUiThread(() -> sndata.setText(str));
            }
        };
        t.start();
    }

    public void setPW(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New PW");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String m_Text = input.getText().toString();
            if (m_Text.length() != 6) {
                toast("Must be 6 long");
                return;
            }
            scooter.setPW(m_Text);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    public void toast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    public void selectPW(View view) {
        this.toast("Copied to clipboard");
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("simple text", pwdata.getText());
        assert clipboard != null;
        clipboard.setPrimaryClip(clip);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scooter.disconnect();
    }

}