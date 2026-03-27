package com.example.sakhi;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChallengeManager {

    private static final String PREFS = "daily_challenge_prefs";

    // 🔥 Keys are now dynamic to include User ID
    private static final String KEY_PREFIX_DATE = "last_date_";
    private static final String KEY_PREFIX_POINTS = "points_";

    /**
     * Checks if the specific user has completed the challenge today.
     */
    public static boolean isCompleted(Context context, String date, String userId) {
        if (userId == null) return false;
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Check for a unique key: e.g., "last_date_user123"
        String savedDate = sp.getString(KEY_PREFIX_DATE + userId, "");
        return date.equals(savedDate);
    }

    /**
     * Marks the challenge as done for the specific user and adds points.
     */
    public static void markAsDone(Context context, String date, String userId) {
        if (userId == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        int currentPoints = sp.getInt(KEY_PREFIX_POINTS + userId, 0);

        sp.edit()
                .putString(KEY_PREFIX_DATE + userId, date)
                .putInt(KEY_PREFIX_POINTS + userId, currentPoints + 10)
                .apply();
    }

    /**
     * Retrieves total points for the specific user.
     */
    public static int getPoints(Context context, String userId) {
        if (userId == null) return 0;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_PREFIX_POINTS + userId, 0);
    }

    // Helper to get today's date string consistently
    public static String getTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}