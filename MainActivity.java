package com.databottle;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY    = 1001;
    private static final int REQ_USAGE      = 1002;

    private Button btnOverlay, btnUsage, btnStart, btnStop;
    private EditText etPlan;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOverlay = findViewById(R.id.btn_overlay_perm);
        btnUsage   = findViewById(R.id.btn_usage_perm);
        btnStart   = findViewById(R.id.btn_start);
        btnStop    = findViewById(R.id.btn_stop);
        etPlan     = findViewById(R.id.et_plan_gb);
        tvStatus   = findViewById(R.id.tv_status);

        btnOverlay.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
        });

        btnUsage.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(i);
        });

        btnStart.setOnClickListener(v -> {
            if (!hasOverlayPerm()) {
                Toast.makeText(this, "Please grant Draw Over Apps permission first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!hasUsagePerm()) {
                Toast.makeText(this, "Please grant Usage Access permission first", Toast.LENGTH_SHORT).show();
                return;
            }
            savePlan();
            Intent svc = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
            tvStatus.setText("✅ Bottle is active — check top-right of your screen!");
            tvStatus.setTextColor(0xFF2ecc71);
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            tvStatus.setText("⛔ Bottle stopped.");
            tvStatus.setTextColor(0xFFe74c3c);
        });

        // Load saved plan
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        float saved = prefs.getFloat("plan_gb", 5f);
        etPlan.setText(String.valueOf(saved));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermButtons();
    }

    private void updatePermButtons() {
        btnOverlay.setText(hasOverlayPerm() ? "✅ Overlay Granted" : "1. Grant Draw Over Apps");
        btnOverlay.setEnabled(!hasOverlayPerm());
        btnUsage.setText(hasUsagePerm()   ? "✅ Usage Access Granted" : "2. Grant Usage Access");
        btnUsage.setEnabled(!hasUsagePerm());
    }

    private boolean hasOverlayPerm() {
        return Settings.canDrawOverlays(this);
    }

    private boolean hasUsagePerm() {
        AppOpsManager aom = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void savePlan() {
        String txt = etPlan.getText().toString().trim();
        float gb = 5f;
        try { gb = Float.parseFloat(txt); } catch (Exception ignored) {}
        getPreferences(MODE_PRIVATE).edit().putFloat("plan_gb", gb).apply();
    }
}
