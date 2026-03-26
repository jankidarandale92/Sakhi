package com.example.sakhi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class SymptomChatActivity extends AppCompatActivity {
    TextView tvAIQuestion, tvTitle;
    EditText etMessage;
    ProgressBar progressBar;
    List<Message> messages = new ArrayList<>();

    // 🔥 Track the specific detected language
    private String userLang = "English";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_chat);

        tvTitle = findViewById(R.id.tvTitle);
        tvAIQuestion = findViewById(R.id.tvAIQuestion);
        etMessage = findViewById(R.id.etMessage);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void sendMessage() {
        String userMsg = etMessage.getText().toString().trim();
        if (userMsg.isEmpty()) return;

        // 🔥 Reset and detect language on the VERY first message of the session
        if (messages.isEmpty()) {
            userLang = detectSpecificLanguage(userMsg);
        }

        etMessage.setText("");
        progressBar.setVisibility(View.VISIBLE);
        tvTitle.setText("Thinking...");

        if (messages.isEmpty()) {
            // 🔥 REINFORCED SYSTEM PROMPT: Forcing total adherence to the detected language
            String systemPrompt = "You are Sakhi, a professional women's health triage assistant. " +
                    "Gather a holistic view (2-3 questions) before identifying a condition. " +
                    "RULES: " +
                    "1. When ready, reply ONLY with [POSSIBLE: ConditionName]. " +
                    "2. FORMATTING: Use plain text only. NO stars (*), bullets, or bold. " +
                    "3. LANGUAGE: You MUST respond strictly in " + userLang + ". " +
                    "If the language is English, do NOT use any Hindi or Marathi words. " +
                    "If Hindi, use Hindi grammar. If Marathi, use Marathi grammar.";

            messages.add(new Message("system", systemPrompt));
        }
        messages.add(new Message("user", userMsg));

        OpenRouterRequest request = new OpenRouterRequest("google/gemini-2.0-flash-001", messages);

        OpenRouterClient.getInterface().getChatCompletion("Bearer " + BuildConfig.OPENROUTER_KEY, request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null && !response.body().choices.isEmpty()) {
                            String reply = response.body().choices.get(0).message.content;
                            reply = reply.replace("*", "").replace("- ", "").trim();

                            messages.add(new Message("assistant", reply));

                            if (reply.contains("[POSSIBLE:")) {
                                try {
                                    String condition = reply.substring(reply.indexOf(":") + 1, reply.indexOf("]")).trim();
                                    Intent i = new Intent(SymptomChatActivity.this, SymptomSurveyActivity.class);
                                    i.putExtra("CONDITION_NAME", condition);

                                    // 🔥 PASS THE SPECIFIC DETECTED LANGUAGE (English, Hindi, or Marathi)
                                    i.putExtra("USER_LANG", userLang);

                                    startActivity(i);
                                    finish();
                                } catch (Exception e) {
                                    tvAIQuestion.setText(reply);
                                }
                            } else {
                                tvTitle.setText("Sakhi");
                                tvAIQuestion.setText(reply);
                            }
                        } else {
                            Log.e("SakhiError", "Code: " + response.code());
                            tvAIQuestion.setText("AI Error: Check connection.");
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        tvAIQuestion.setText("Connection failed. Check your internet.");
                    }
                });
    }

    // 🔥 REFINED LANGUAGE DETECTION: Strictly separates English from Devanagari scripts
    private String detectSpecificLanguage(String text) {
        // Step 1: Check if there are ANY Devanagari characters.
        // If not, it is 100% English.
        if (!text.matches(".*[\\u0900-\\u097F].*")) {
            return "English";
        }

        // Step 2: If Devanagari is found, distinguish between Marathi and Hindi
        String lower = text.toLowerCase();

        // Marathi-specific common keywords/markers
        if (lower.contains("आहे") || lower.contains("होतंय") || lower.contains("नाही") ||
                lower.contains("मुलगी") || lower.contains("काय") || lower.contains("हवंय") ||
                lower.contains("दुखतंय") || lower.contains("सांगा")) {
            return "Marathi";
        }

        // Hindi-specific common keywords/markers
        if (lower.contains("है") || lower.contains("रहा") || lower.contains("रही") ||
                lower.contains("हूँ") || lower.contains("लड़की") || lower.contains("क्या") ||
                lower.contains("दर्द") || lower.contains("बताओ")) {
            return "Hindi";
        }

        // Default fallback for Devanagari script (defaults to Hindi as it's more common)
        return "Hindi";
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}