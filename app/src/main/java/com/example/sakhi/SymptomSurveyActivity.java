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

        // 🔥 Multilingual Header Support
        if (isDevanagari(conditionName)) {
            tvCondition.setText("तपासत आहे: " + conditionName);
        } else {
            tvCondition.setText("Checking for: " + conditionName);
        }

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
            startActivity(new Intent(this, SymptomChatActivity.class));
            finish();
        }
    }

    private void generateQuestions(String condition) {
        progressBar.setVisibility(View.VISIBLE);
        btnAnalyze.setVisibility(View.GONE);

        // 🔥 MULTILINGUAL GENERATION PROMPT
        String prompt = "Generate exactly 3 simple Yes/No diagnostic questions to confirm if a patient has " + condition + ". " +
                "IMPORTANT: You must detect the language of '" + condition + "' and generate the questions in that SAME language (English, Hindi, or Marathi). " +
                "Return ONLY the questions separated by a pipe symbol (|). Example: Question 1|Question 2|Question 3";

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", prompt));

        // Using Gemini 2.0 Flash
        OpenRouterRequest request = new OpenRouterRequest("google/gemini-2.0-flash-001", messages);

        OpenRouterClient.getInterface().getChatCompletion("Bearer " + BuildConfig.OPENROUTER_KEY, request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().choices != null) {
                            String rawText = response.body().choices.get(0).message.content.trim();

                            final String[] questions = rawText.split("\\|");

                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                questionsContainer.removeAllViews();
                                checkBoxes.clear();

                                for (String q : questions) {
                                    if (!q.trim().isEmpty()) {
                                        CheckBox cb = new CheckBox(SymptomSurveyActivity.this);
                                        cb.setText(q.trim());
                                        cb.setTextSize(16f);
                                        cb.setPadding(0, 30, 0, 30);
                                        cb.setTextColor(Color.BLACK);
                                        // Ensure local scripts have enough space
                                        cb.setLineSpacing(1.2f, 1.2f);

                                        questionsContainer.addView(cb);
                                        checkBoxes.add(cb);
                                    }
                                }
                                if (!checkBoxes.isEmpty()) {
                                    btnAnalyze.setVisibility(View.VISIBLE);
                                    // Update button text for better UX
                                    if (isDevanagari(condition)) btnAnalyze.setText("तपासा");
                                }
                            });
                        } else {
                            runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    }
                });
    }

    private void analyzeResults() {
        int yesCount = 0;
        for (CheckBox cb : checkBoxes) {
            if (cb.isChecked()) yesCount++;
        }

        String finalCondition = (yesCount >= 2) ? conditionName : (isDevanagari(conditionName) ? "सामान्य आरोग्य समस्या" : "General Health Issue");

        SharedPreferences prefs = getSharedPreferences("SakhiHealthHistory", MODE_PRIVATE);
        String key = "first_report_" + finalCondition.toLowerCase().replace(" ", "_");

        if (!prefs.contains(key)) {
            prefs.edit().putLong(key, System.currentTimeMillis()).apply();
        }

        Intent intent = new Intent(SymptomSurveyActivity.this, SymptomSummaryActivity.class);
        intent.putExtra("CONDITION_NAME", finalCondition);
        startActivity(intent);
        finish();
    }

    // 🔥 Helper to detect Devanagari Script (Hindi/Marathi)
    private boolean isDevanagari(String text) {
        return text.matches(".*[\\u0900-\\u097F].*");
    }
}