package com.databottle;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service that draws a floating overlay bottle on top of all apps.
 * The bottle's water level syncs with real mobile data usage every 30 seconds.
 */
public class OverlayService extends Service {

    private static final String CHANNEL_ID   = "data_bottle_channel";
    private static final int    NOTIF_ID     = 42;
    private static final long   REFRESH_MS   = 30_000L;  // 30 seconds

    private WindowManager        windowManager;
    private View                 overlayRoot;
    private BottleView           bottleView;
    private TextView             tvPercent;
    private TextView             tvGB;
    private View                 expandedPanel;
    private TextView             tvUsedExp;
    private TextView             tvRemainingExp;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            refreshData();
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    private boolean isExpanded = false;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        showOverlay();
    }

    // ── Build overlay ─────────────────────────────────────────────────

    private void showOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        overlayRoot = LayoutInflater.from(this).inflate(R.layout.overlay_bottle, null);
        bottleView       = overlayRoot.findViewById(R.id.bottle_view);
        tvPercent        = overlayRoot.findViewById(R.id.tv_percent);
        tvGB             = overlayRoot.findViewById(R.id.tv_gb);
        expandedPanel    = overlayRoot.findViewById(R.id.expanded_panel);
        tvUsedExp        = overlayRoot.findViewById(R.id.tv_used_exp);
        tvRemainingExp   = overlayRoot.findViewById(R.id.tv_remaining_exp);

        // Position: top-right
        params = new WindowManager.LayoutParams(
                dpToPx(72),
                dpToPx(130),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dpToPx(8);
        params.y = dpToPx(60);

        setupTouchListener();
        windowManager.addView(overlayRoot, params);

        // First data pull
        handler.post(refreshRunnable);
    }

    // ── Drag + tap ───────────────────────────────────────────────────

    private void setupTouchListener() {
        overlayRoot.setOnTouchListener(new View.OnTouchListener() {
            long downTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downTime = System.currentTimeMillis();
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = e.getRawX();
                        initialTouchY = e.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX - (int)(e.getRawX() - initialTouchX);
                        params.y = initialY + (int)(e.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayRoot, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        long elapsed = System.currentTimeMillis() - downTime;
                        float dx = Math.abs(e.getRawX() - initialTouchX);
                        float dy = Math.abs(e.getRawY() - initialTouchY);
                        if (elapsed < 300 && dx < 10 && dy < 10) {
                            toggleExpand();
                        }
                        return true;
                }
                return false;
            }
        });

        // Close expanded on outside tap (via a close button inside expanded panel)
        View closeBtn = overlayRoot.findViewById(R.id.btn_close_expand);
        if (closeBtn != null) closeBtn.setOnClickListener(v -> toggleExpand());
    }

    private void toggleExpand() {
        isExpanded = !isExpanded;
        if (isExpanded) {
            // Grow overlay to show detail panel
            params.width  = dpToPx(170);
            params.height = dpToPx(240);
            params.flags  = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                          | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            // Re-add flags correctly
            params.flags  = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                          | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            expandedPanel.setVisibility(View.VISIBLE);
            expandedPanel.setAlpha(0f);
            expandedPanel.animate().alpha(1f).setDuration(200).start();
        } else {
            params.width  = dpToPx(72);
            params.height = dpToPx(130);
            expandedPanel.animate().alpha(0f).setDuration(150)
                    .withEndAction(() -> expandedPanel.setVisibility(View.GONE)).start();
        }
        windowManager.updateViewLayout(overlayRoot, params);
    }

    // ── Data refresh ──────────────────────────────────────────────────

    private void refreshData() {
        float planGB = getSharedPreferences("databottle_prefs", MODE_PRIVATE)
                .getFloat("plan_gb", 5f);

        // Also try from MainActivity prefs
        SharedPreferences mainPrefs = getSharedPreferences("com.databottle_preferences", MODE_PRIVATE);
        if (mainPrefs.contains("plan_gb")) planGB = mainPrefs.getFloat("plan_gb", planGB);

        float usedPct = DataUsageHelper.getUsedPercent(this, planGB);
        long  usedBytes = DataUsageHelper.getMobileDataUsedBytes(this);
        long  planBytes = (long)(planGB * 1024L * 1024L * 1024L);
        long  remainBytes = Math.max(0, planBytes - usedBytes);

        float remainFraction;
        String pctText, gbText, usedText, remText;

        if (usedPct < 0) {
            // Permission not granted yet — show placeholder
            remainFraction = 0.7f;
            pctText = "??%";
            gbText  = "No perm";
            usedText = "Usage access\nnot granted";
            remText  = "Open app\nto fix";
        } else {
            remainFraction = 1f - (usedPct / 100f);
            pctText = (int)(100 - usedPct) + "%";
            gbText  = DataUsageHelper.formatBytes(usedBytes) + "\n/ " + planGB + " GB";
            usedText = "Used: " + DataUsageHelper.formatBytes(usedBytes);
            remText  = "Left: " + DataUsageHelper.formatBytes(remainBytes);
        }

        final float finalFraction = remainFraction;
        final String fPct = pctText, fGb = gbText, fUsed = usedText, fRem = remText;

        handler.post(() -> {
            bottleView.setFillFraction(finalFraction);
            tvPercent.setText(fPct);
            tvGB.setText(fGb);
            if (tvUsedExp    != null) tvUsedExp.setText(fUsed);
            if (tvRemainingExp != null) tvRemainingExp.setText(fRem);
        });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(refreshRunnable);
        if (overlayRoot != null && windowManager != null) {
            windowManager.removeView(overlayRoot);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ── Helpers ───────────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Data Bottle Overlay",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Keeps the floating data bottle active");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("Data Bottle Active")
                .setContentText("Tap to open settings")
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private int dpToPx(int dp) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return (int)(dp * dm.density);
    }
}
