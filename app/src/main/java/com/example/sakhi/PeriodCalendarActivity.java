package com.example.sakhi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PeriodCalendarActivity extends AppCompatActivity {

    private static final String SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNic3Bxbm5tdWxsZXpscGJkemhzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njg4MTc4NDYsImV4cCI6MjA4NDM5Mzg0Nn0.H9p0LoBRWEgjKBRSfKg1DdwnCN7qV2dQCo2gVEL7DiU";

    private RecyclerView rv;
    private TextView tvNext, tvMonth;
    private ImageView btnPrev, btnNext;

    private Calendar currentMonth = Calendar.getInstance();

    private Date lastPeriod;
    private int cycleLength;
    private int periodLength;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_period_calendar);

        rv = findViewById(R.id.calendarRecycler);
        tvNext = findViewById(R.id.tvNextPeriod);
        tvMonth = findViewById(R.id.tvMonth);
        btnPrev = findViewById(R.id.btnPrevMonth);
        btnNext = findViewById(R.id.btnNextMonth);

        BottomNavHelper.setupBottomNav(this, R.id.navPeriod);
        rv.setLayoutManager(new GridLayoutManager(this, 7));

        findViewById(R.id.btnEditCycle).setOnClickListener(v ->
                startActivity(new Intent(this, EditCycleActivity.class)));

        btnPrev.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            renderCalendar();
        });

        btnNext.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            renderCalendar();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCycleFromSupabase();
    }

    private void loadCycleFromSupabase() {
        String userId = SessionManager.getUserId(this);
        String token = SessionManager.getAccessToken(this);
        if (userId == null || token == null) return;

        SupabasePeriodApi api = RetrofitClient.getClient().create(SupabasePeriodApi.class);
        api.getCycle(SUPABASE_KEY, "Bearer " + token, "eq." + userId)
                .enqueue(new Callback<JsonArray>() {
                    @Override
                    public void onResponse(Call<JsonArray> call, Response<JsonArray> res) {
                        if (res.isSuccessful() && res.body() != null && res.body().size() > 0) {
                            try {
                                JsonObject data = res.body().get(0).getAsJsonObject();
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                lastPeriod = sdf.parse(data.get("last_period_date").getAsString());
                                cycleLength = data.get("cycle_length").getAsInt();
                                periodLength = data.get("period_length").getAsInt();

                                Date nextDate = CycleCalculator.getNextPeriod(lastPeriod, cycleLength);

                                // 🔥 NEW: Schedule notifications and sync with Reminder list
                                schedulePeriodNotifications(nextDate);

                                renderCalendar();
                            } catch (Exception e) { e.printStackTrace(); }
                        } else {
                            tvNext.setText("No data. Tap 'Edit Cycle'.");
                            renderCalendar();
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonArray> call, Throwable t) {
                        renderCalendar();
                    }
                });
    }

    private void schedulePeriodNotifications(Date nextPeriodDate) {
        if (nextPeriodDate == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.US);
        String dateStr = sdf.format(nextPeriodDate);

        String title = "Menstrual Cycle Alert";
        String message = "Your period is expected tomorrow (" + dateStr + "). Stay prepared! 🌸";

        // 1. Save this to the shared reminder list (SharedPreferences) at 1:40 PM
        saveToReminderList(title, dateStr);

        // 2. Schedule the actual alarm for 1:40 PM (13:40) one day before the period
        scheduleSingleAlarm(nextPeriodDate, -1, title, message, 999);
    }

    private void saveToReminderList(String title, String dateStr) {
        SharedPreferences prefs = getSharedPreferences("SakhiData", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("reminders", null);
        List<RemainderModel> list;

        if (json == null) {
            list = new ArrayList<>();
        } else {
            Type type = new TypeToken<ArrayList<RemainderModel>>() {}.getType();
            list = gson.fromJson(json, type);
        }

        // Check if item 999 (Fixed ID for Period) exists; if so, update it
        boolean updated = false;
        for (RemainderModel m : list) {
            if (m.id == 999) {
                m.title = title;
                m.time = "01:40 PM";
                m.repeat = "Once on " + dateStr;
                m.isActive = true;
                updated = true;
                break;
            }
        }

        if (!updated) {
            // Add as a new item with ID 999 if it doesn't exist
            list.add(new RemainderModel(999, title, "01:40 PM", "Once on " + dateStr, true));
        }

        prefs.edit().putString("reminders", gson.toJson(list)).apply();
    }

    private void scheduleSingleAlarm(Date baseDate, int daysOffset, String title, String message, int requestCode) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(baseDate);
        calendar.add(Calendar.DAY_OF_YEAR, daysOffset);

        // 🔥 Set for 1:40 PM (13:40 in 24-hour format)
        calendar.set(Calendar.HOUR_OF_DAY, 13);
        calendar.set(Calendar.MINUTE, 40);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) return;

        Intent intent = new Intent(this, RemainderReceiver.class);
        intent.putExtra("type", "PERIOD_ALERT");
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("id", requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    private void renderCalendar() {
        List<CalendarDay> cells = new ArrayList<>();
        tvMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.US).format(currentMonth.getTime()));

        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int offset = cal.get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < offset; i++) cells.add(new CalendarDay());

        Date ovulationDay = null;
        Date nextPeriodStart = null;
        List<Date> periodDays = new ArrayList<>();
        List<Date> fertileWindow = new ArrayList<>();

        if (lastPeriod != null) {
            ovulationDay = CycleCalculator.getOvulation(lastPeriod, cycleLength);
            fertileWindow = CycleCalculator.getFertileWindow(ovulationDay);
            periodDays = CycleCalculator.getPeriodDays(lastPeriod, periodLength);
            nextPeriodStart = CycleCalculator.getNextPeriod(lastPeriod, cycleLength);
            tvNext.setText("Next Period: " + new SimpleDateFormat("dd MMM", Locale.US).format(nextPeriodStart));
        }

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Date todayDate = new Date();

        for (int i = 0; i < daysInMonth; i++) {
            CalendarDay d = new CalendarDay();
            Date cellDate = cal.getTime();
            d.date = cellDate;
            d.isToday = same(cellDate, todayDate);

            if (lastPeriod != null) {
                d.isPeriod = contains(periodDays, cellDate);
                d.isOvulation = same(cellDate, ovulationDay);
                d.isFertile = contains(fertileWindow, cellDate);

                if (cellDate.after(ovulationDay) && cellDate.before(nextPeriodStart) && !d.isOvulation) {
                    d.isLuteal = true;
                }
            }
            cells.add(d);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        rv.setAdapter(new CalendarAdapter(cells));
    }

    private boolean same(Date a, Date b) {
        if (a == null || b == null) return false;
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(a);
        c2.setTime(b);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean contains(List<Date> list, Date d) {
        if (list == null) return false;
        for (Date x : list) if (same(x, d)) return true;
        return false;
    }
}