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
    String userLang; // 🔥 Receives specific: "English", "Hindi", or "Marathi"
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

        // 1. Get the suspected condition and language from Intent
        conditionName = getIntent().getStringExtra("CONDITION_NAME");
        userLang = getIntent().getStringExtra("USER_LANG");

        if (conditionName == null) conditionName = "Unknown Issue";
        if (userLang == null) userLang = "English";

        // 🔥 Update UI Labels based on specific language
        updateUILabels();

        // 2. Trigger AI Question Generation
        generateQuestions(conditionName, userLang);

        // 3. Set Analyze Button Listener
        btnAnalyze.setOnClickListener(v -> analyzeResults());

        // Back Button Logic
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    private void updateUILabels() {
        if ("Marathi".equalsIgnoreCase(userLang)) {
            tvCondition.setText("तपासत आहे: " + conditionName);
            btnAnalyze.setText("तपासा");
        } else if ("Hindi".equalsIgnoreCase(userLang)) {
            tvCondition.setText("जाँच की जा रही है: " + conditionName);
            btnAnalyze.setText("जाँचें");
        } else {
            // 🔥 Explicitly English
            tvCondition.setText("Checking for: " + conditionName);
            btnAnalyze.setText("Analyze Results");
        }
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

    private void generateQuestions(String condition, String lang) {
        progressBar.setVisibility(View.VISIBLE);
        btnAnalyze.setVisibility(View.GONE);

        // 🔥 REINFORCED MULTILINGUAL PROMPT
        // Added a strict rule to prevent cross-language leakage
        String prompt = "The user is suspected to have " + condition + ". " +
                "Generate exactly 3 simple Yes/No diagnostic questions. " +
                "CRITICAL RULES: " +
                "1. The questions MUST be entirely in " + lang + ". " +
                "2. If the language is English, do NOT use Hindi or Marathi scripts or words. " +
                "3. If language is Hindi, use Hindi grammar. If Marathi, use Marathi grammar. " +
                "4. Return ONLY the questions separated by a pipe symbol (|). No stars or bold.";

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", prompt));

        OpenRouterRequest request = new OpenRouterRequest("google/gemini-2.0-flash-001", messages);

        OpenRouterClient.getInterface().getChatCompletion("Bearer " + BuildConfig.OPENROUTER_KEY, request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().choices != null) {
                            String rawText = response.body().choices.get(0).message.content.trim();

                            // Cleanup fallback
                            rawText = rawText.replace("*", "").replace("- ", "").trim();

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
                                        cb.setLineSpacing(1.2f, 1.2f);

                                        questionsContainer.addView(cb);
                                        checkBoxes.add(cb);
                                    }
                                }
                                if (!checkBoxes.isEmpty()) {
                                    btnAnalyze.setVisibility(View.VISIBLE);
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

        // 🔥 Language-Specific Fallback for results
        String finalCondition;
        if (yesCount >= 2) {
            finalCondition = conditionName;
        } else {
            if ("Marathi".equalsIgnoreCase(userLang)) finalCondition = "सामान्य आरोग्य समस्या";
            else if ("Hindi".equalsIgnoreCase(userLang)) finalCondition = "सामान्य स्वास्थ्य समस्या";
            else finalCondition = "General Health Issue";
        }

        SharedPreferences prefs = getSharedPreferences("SakhiHealthHistory", MODE_PRIVATE);
        String key = "first_report_" + finalCondition.toLowerCase().replace(" ", "_");

        // ✅ Persistence logic to prevent 4487 days bug
        long currentTime = System.currentTimeMillis();
        if (!prefs.contains(key) || prefs.getLong(key, 0) < 1704067200000L) {
            prefs.edit().putLong(key, currentTime).apply();
        }

        Intent intent = new Intent(SymptomSurveyActivity.this, SymptomSummaryActivity.class);
        intent.putExtra("CONDITION_NAME", finalCondition);
        intent.putExtra("USER_LANG", userLang);
        startActivity(intent);
        finish();
    }
}