package com.databottle;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Calendar;

/**
 * Reads real mobile (cellular) data usage for the current billing cycle
 * using NetworkStatsManager (requires PACKAGE_USAGE_STATS permission).
 */
public class DataUsageHelper {

    private static final String TAG = "DataUsageHelper";

    /**
     * Returns bytes used on mobile data since the start of the current month.
     */
    public static long getMobileDataUsedBytes(Context ctx) {
        try {
            NetworkStatsManager nsm = (NetworkStatsManager)
                    ctx.getSystemService(Context.NETWORK_STATS_SERVICE);

            // Billing cycle: start of current month → now
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startTime = cal.getTimeInMillis();
            long endTime   = System.currentTimeMillis();

            // Get subscriber ID for cellular stats
            String subscriberId = getSubscriberId(ctx);

            NetworkStats.Bucket bucket = nsm.querySummaryForDevice(
                    ConnectivityManager.TYPE_MOBILE,
                    subscriberId,
                    startTime,
                    endTime
            );

            long total = bucket.getRxBytes() + bucket.getTxBytes();
            Log.d(TAG, "Mobile data used this month: " + total + " bytes");
            return total;

        } catch (Exception e) {
            Log.e(TAG, "Error reading network stats: " + e.getMessage());
            return -1L;
        }
    }

    /**
     * Returns percentage of plan used (0–100).
     * planGB is the user's monthly data plan in gigabytes.
     */
    public static float getUsedPercent(Context ctx, float planGB) {
        long usedBytes = getMobileDataUsedBytes(ctx);
        if (usedBytes < 0) return -1f; // error / no permission
        long planBytes = (long)(planGB * 1024L * 1024L * 1024L);
        float pct = (usedBytes / (float) planBytes) * 100f;
        return Math.min(pct, 100f);
    }

    /**
     * Formats bytes into human-readable string: MB or GB.
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.2f GB", mb / 1024.0);
    }

    private static String getSubscriberId(Context ctx) {
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            return tm != null ? tm.getSubscriberId() : null;
        } catch (SecurityException e) {
            return null;
        }
    }
}
