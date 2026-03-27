package com.example.sakhi;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MythFactManager {

    private static final String PREF = "myth_fact_prefs";
    // 🔥 Dynamic key prefix to separate users
    private static final String KEY_PREFIX_DONE = "done_date_";

    public static List<MythFactQuestion> getQuestions() {
        List<MythFactQuestion> list = new ArrayList<>();
        list.add(new MythFactQuestion(
                "Periods should always be painful.",
                false,
                "Fact: Severe, debilitating pain is not normal and may be a sign of conditions like endometriosis."
        ));
        list.add(new MythFactQuestion(
                "Exercise during your period is harmful.",
                false,
                "Fact: Light exercise releases endorphins which can actually help reduce cramps and improve mood."
        ));
        list.add(new MythFactQuestion(
                "Irregular cycles are common in the first few years of menstruation.",
                true,
                "Fact: It often takes a few years for the body's hormonal system to establish a regular rhythm."
        ));
        list.add(new MythFactQuestion(
                "A woman cannot get pregnant during her period.",
                false,
                "Fact: While unlikely, it is possible, especially for women with very short or irregular cycles."
        ));
        list.add(new MythFactQuestion(
                "Stress can cause your period to be late or skipped.",
                true,
                "Fact: High stress affects the hypothalamus, which controls the hormones responsible for your period."
        ));
        return list;
    }

    /**
     * Selects a question based on the day of the year so everyone sees the same one daily.
     */
    public static MythFactQuestion getTodayQuestion() {
        List<MythFactQuestion> list = getQuestions();
        int dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
        int index = dayOfYear % list.size();
        return list.get(index);
    }

    /**
     * Checks if the SPECIFIC user has completed the myth/fact for TODAY.
     */
    public static boolean isCompleted(Context context) {
        String userId = SessionManager.getUserId(context);
        if (userId == null) return false;

        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String today = getTodayString();

        // Key format example: done_date_user123
        String lastDate = sp.getString(KEY_PREFIX_DONE + userId, "");
        return today.equals(lastDate);
    }

    /**
     * Marks the task as done for the current user and current date.
     */
    public static void markCompleted(Context context) {
        String userId = SessionManager.getUserId(context);
        if (userId == null) return;

        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_PREFIX_DONE + userId, getTodayString())
                .apply();
    }

    private static String getTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}