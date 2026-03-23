package com.example.sakhi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SymptomSurveyActivity extends AppCompatActivity {

    TextView tvCondition;
    LinearLayout questionsContainer;
    Button btnAnalyze;
    ProgressBar progressBar;
    String conditionName;
    List<CheckBox> checkBoxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_survey);

        // Initialize Views
        tvCondition = findViewById(R.id.tvSuspectedCondition);
        questionsContainer = findViewById(R.id.questionsContainer);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        progressBar = findViewById(R.id.progressBar);

        // 1. Get the suspected condition from Intent
        conditionName = getIntent().getStringExtra("CONDITION_NAME");
        if (conditionName == null) conditionName = "Unknown Issue";

        tvCondition.setText("Checking for: " + conditionName);

        // 2. Trigger AI Question Generation
        generateQuestions(conditionName);

        // 3. Set Analyze Button Listener
        btnAnalyze.setOnClickListener(v -> analyzeResults());

        // Back Button Logic
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    @Override
    public void onBackPressed() {
        if (!isTaskRoot()) {
            super.onBackPressed();
        } else {
            // Fallback to Chat if there's no activity stack
            startActivity(new Intent(this, SymptomChatActivity.class));
            finish();
        }
    }

    private void generateQuestions(String condition) {
        progressBar.setVisibility(View.VISIBLE);
        btnAnalyze.setVisibility(View.GONE); // Hide until questions are ready

        // Prepare the messages for the OpenRouter API
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "Generate exactly 3 simple Yes/No diagnostic questions to confirm if a patient has " + condition + ". Return ONLY the questions separated by a pipe symbol (|). Example: Question 1|Question 2|Question 3"));

        // Create Request (Model name must match OpenRouter format)
        // UPDATED MODEL ID FOR 2026
        OpenRouterRequest request = new OpenRouterRequest("google/gemini-2.0-flash-001", messages);

        // API Call using Retrofit
        OpenRouterClient.getInterface().getChatCompletion("Bearer " + BuildConfig.OPENROUTER_KEY, request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().choices != null) {
                            String rawText = response.body().choices.get(0).message.content.trim();

                            // Split questions by pipe character
                            final String[] questions = rawText.split("\\|");

                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                questionsContainer.removeAllViews(); // Clear existing views
                                checkBoxes.clear();

                                for (String q : questions) {
                                    if (!q.trim().isEmpty()) {
                                        CheckBox cb = new CheckBox(SymptomSurveyActivity.this);
                                        cb.setText(q.trim());
                                        cb.setTextSize(16f);
                                        cb.setPadding(0, 30, 0, 30); // Better spacing
                                        cb.setTextColor(Color.BLACK);

                                        questionsContainer.addView(cb);
                                        checkBoxes.add(cb);
                                    }
                                }
                                if (!checkBoxes.isEmpty()) {
                                    btnAnalyze.setVisibility(View.VISIBLE);
                                } else {
                                    Toast.makeText(SymptomSurveyActivity.this, "AI returned invalid format. Try again.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(SymptomSurveyActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(SymptomSurveyActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void analyzeResults() {
        int yesCount = 0;
        for (CheckBox cb : checkBoxes) {
            if (cb.isChecked()) yesCount++;
        }

        // Logic: If user says 'Yes' to at least 2 questions, confirm the condition
        String finalCondition = (yesCount >= 2) ? conditionName : "General Health Issue";

        // --- PERSISTENCE LOGIC (The "Sakhi" Special) ---
        // Store the date of first report to track 3-day/7-day severity
        SharedPreferences prefs = getSharedPreferences("SakhiHealthHistory", MODE_PRIVATE);
        String key = "first_report_" + finalCondition.toLowerCase().replace(" ", "_");

        if (!prefs.contains(key)) {
            // Save current time as the "First Report" timestamp
            prefs.edit().putLong(key, System.currentTimeMillis()).apply();
        }

        // Navigate to Summary screen
        Intent intent = new Intent(SymptomSurveyActivity.this, SymptomSummaryActivity.class);
        intent.putExtra("CONDITION_NAME", finalCondition);
        startActivity(intent);
        finish();
    }
}