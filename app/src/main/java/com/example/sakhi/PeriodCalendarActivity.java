package com.example.sakhi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

    RecyclerView rv;
    TextView tvNext, tvMonth;
    ImageView btnPrev, btnNext;

    Calendar currentMonth = Calendar.getInstance();

    Date lastPeriod;
    int cycleLength;
    int periodLength;

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

        // Standard 7-column grid. LayoutManager is set once.
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
                        if (!res.isSuccessful() || res.body() == null || res.body().size() == 0) {
                            tvNext.setText("No data. Tap 'Edit Cycle'.");
                            renderCalendar(); // Render empty calendar anyway
                            return;
                        }

                        try {
                            JsonObject data = res.body().get(0).getAsJsonObject();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                            lastPeriod = sdf.parse(data.get("last_period_date").getAsString());
                            cycleLength = data.get("cycle_length").getAsInt();
                            periodLength = data.get("period_length").getAsInt();

                            renderCalendar();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonArray> call, Throwable t) {
                        Toast.makeText(PeriodCalendarActivity.this, "Sync failed", Toast.LENGTH_SHORT).show();
                        renderCalendar();
                    }
                });
    }

    private void renderCalendar() {
        List<CalendarDay> cells = new ArrayList<>();

        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
        tvMonth.setText(monthYearFormat.format(currentMonth.getTime()));

        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // 1. Padding for the start of the month
        int offset = cal.get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < offset; i++) {
            cells.add(new CalendarDay());
        }

        // 2. Windows calculation (Only if data exists)
        Date ovulation = null;
        List<Date> fertile = new ArrayList<>();
        List<Date> period = new ArrayList<>();

        if (lastPeriod != null) {
            ovulation = CycleCalculator.getOvulation(lastPeriod, cycleLength);
            fertile = CycleCalculator.getFertileWindow(ovulation);
            period = CycleCalculator.getPeriodDays(lastPeriod, periodLength);

            Date next = CycleCalculator.getNextPeriod(lastPeriod, cycleLength);
            tvNext.setText("Next Period: " + new SimpleDateFormat("dd MMM", Locale.US).format(next));
        }

        // 3. Fill days
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Date todayDate = new Date();

        for (int i = 0; i < daysInMonth; i++) {
            CalendarDay d = new CalendarDay();
            d.date = cal.getTime();

            d.isToday = same(d.date, todayDate);
            if (lastPeriod != null) {
                d.isPeriod = contains(period, d.date);
                d.isOvulation = same(d.date, ovulation);
                d.isFertile = contains(fertile, d.date);
            }

            cells.add(d);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 4. Update Adapter
        rv.setAdapter(new CalendarAdapter(cells));
    }

    private boolean same(Date a, Date b) {
        if (a == null || b == null) return false;
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(a);
        c2.setTime(b);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean contains(List<Date> list, Date d) {
        if (list == null) return false;
        for (Date x : list) if (same(x, d)) return true;
        return false;
    }
}