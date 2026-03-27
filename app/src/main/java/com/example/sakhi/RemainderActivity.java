package com.example.sakhi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RemainderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RemainderAdapter adapter;
    private List<RemainderModel> reminderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remainder);

        // 1. Initialize UI Components
        recyclerView = findViewById(R.id.rvReminders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button btnAdd = findViewById(R.id.btnAddReminder);
        ImageView btnBack = findViewById(R.id.btn_back);

        // 2. Navigation Listeners
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                );
            });
        }

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                startActivity(new Intent(RemainderActivity.this, AddRemainderActivity.class));
            });
        }

        // 3. Initial Load
        loadReminders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning from AddRemainderActivity
        loadReminders();
    }

    private void loadReminders() {
        SharedPreferences prefs = getSharedPreferences("SakhiData", MODE_PRIVATE);
        String json = prefs.getString("reminders", null);

        reminderList = new ArrayList<>();
        if (json != null && !json.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<RemainderModel>>() {}.getType();
            reminderList = gson.fromJson(json, type);
        }

        // NOTE: We removed the hardcoded dummy data here.
        // This ensures that if the user deletes all reminders, the screen stays empty
        // until they purposefully add a new one.

        // 4. Set up the Adapter with the retrieved list
        adapter = new RemainderAdapter(reminderList);
        recyclerView.setAdapter(adapter);
    }
}