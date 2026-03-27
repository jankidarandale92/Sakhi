package com.example.sakhi;

import android.Manifest;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddRemainderActivity extends AppCompatActivity {

    private EditText etTitle, etNotes;
    private Spinner repeatSpinner, ampmSpinner;
    private TextView tvHour, tvMinute;
    private SwitchCompat switchReminder;

    private int systemHour = -1;
    private int systemMinute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_remainder);

        etTitle = findViewById(R.id.etTitle);
        etNotes = findViewById(R.id.etNotes);
        repeatSpinner = findViewById(R.id.repeatSpinner);
        ampmSpinner = findViewById(R.id.ampmSpinner);
        tvHour = findViewById(R.id.tvHour);
        tvMinute = findViewById(R.id.tvMinute);
        switchReminder = findViewById(R.id.switchReminder);
        Button btnSet = findViewById(R.id.btnSet);
        Button btnCancel = findViewById(R.id.btnCancel);
        ImageView btnBack = findViewById(R.id.btnBack);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        setupSpinners();

        View.OnClickListener timeClickListener = v -> showTimePicker();
        tvHour.setOnClickListener(timeClickListener);
        tvMinute.setOnClickListener(timeClickListener);

        btnSet.setOnClickListener(v -> saveReminder());

        btnCancel.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        String[] repeatOptions = {"Daily", "Weekly", "Monthly", "Once"};
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, repeatOptions);
        repeatSpinner.setAdapter(repeatAdapter);

        String[] ampmOptions = {"AM", "PM"};
        ArrayAdapter<String> ampmAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ampmOptions);
        ampmSpinner.setAdapter(ampmAdapter);
    }

    private void showTimePicker() {
        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);

        TimePickerDialog mTimePicker = new TimePickerDialog(this, (timePicker, selectedHourOfDay, selectedMinuteOfHour) -> {
            systemHour = selectedHourOfDay;
            systemMinute = selectedMinuteOfHour;

            int hour12 = selectedHourOfDay;
            String ampm = "AM";
            if (hour12 >= 12) {
                ampm = "PM";
                if (hour12 > 12) hour12 -= 12;
            }
            if (hour12 == 0) hour12 = 12;

            tvHour.setText(String.format(Locale.getDefault(), "%02d", hour12));
            tvMinute.setText(String.format(Locale.getDefault(), "%02d", selectedMinuteOfHour));

            if (ampm.equals("AM")) ampmSpinner.setSelection(0);
            else ampmSpinner.setSelection(1);

        }, hour, minute, false);
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    private void saveReminder() {
        String title = etTitle.getText().toString().trim();
        String repeat = repeatSpinner.getSelectedItem().toString();
        boolean isActive = switchReminder.isChecked();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (systemHour == -1) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- PART A: FORMAT & GENERATE ID ---

        // Generate a unique ID for this specific reminder
        int uniqueId = (int) (System.currentTimeMillis() & 0xfffffff);

        int hour12 = systemHour;
        String ampm = "AM";
        if (hour12 >= 12) { ampm = "PM"; if (hour12 > 12) hour12 -= 12; }
        if (hour12 == 0) hour12 = 12;
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, systemMinute, ampm);

        // --- PART B: SAVE TO PREFERENCES ---

        SharedPreferences prefs = getSharedPreferences("SakhiData", MODE_PRIVATE);
        String json = prefs.getString("reminders", null);
        Gson gson = new Gson();
        List<RemainderModel> list;

        if (json == null) {
            list = new ArrayList<>();
        } else {
            Type type = new TypeToken<ArrayList<RemainderModel>>() {}.getType();
            list = gson.fromJson(json, type);
        }

        // 🔥 FIX: Passed the uniqueId as the first parameter
        RemainderModel newReminder = new RemainderModel(uniqueId, title, formattedTime, repeat, isActive);
        list.add(newReminder);

        prefs.edit().putString("reminders", gson.toJson(list)).apply();

        // --- PART C: SCHEDULE THE ALARM ---

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Required check for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Please allow Exact Alarms permission", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Use our Helper to trigger the "Ringing" logic
        if (isActive) {
            ReminderHelper.setAlarm(this, newReminder);
            Toast.makeText(this, "Reminder Set & Alarm Scheduled! 🌸", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Reminder Saved (Disabled)", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}