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

        etMessage.setText("");
        progressBar.setVisibility(View.VISIBLE);
        tvTitle.setText("Thinking...");

        if (messages.isEmpty()) {
            messages.add(new Message("system", "You are Sakhi, a women's health triage assistant. If you suspect a condition (PCOS, Anemia, Thyroid, etc), reply ONLY with [POSSIBLE: ConditionName]. Otherwise ask a short, empathetic follow-up question."));
        }
        messages.add(new Message("user", userMsg));

        // --- UPDATED MODEL ID ---
        // OpenRouter often prefers the full path. Try "google/gemini-flash-1.5"
        // If 404 persists, try "google/gemini-flash-1.5-8b"
        // UPDATED MODEL ID FOR 2026
        OpenRouterRequest request = new OpenRouterRequest("google/gemini-2.0-flash-001", messages);

        OpenRouterClient.getInterface().getChatCompletion("Bearer " + BuildConfig.OPENROUTER_KEY, request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null && response.body().choices != null && !response.body().choices.isEmpty()) {
                            String reply = response.body().choices.get(0).message.content;
                            messages.add(new Message("assistant", reply));

                            if (reply.contains("[POSSIBLE:")) {
                                try {
                                    String condition = reply.substring(reply.indexOf(":") + 1, reply.indexOf("]")).trim();
                                    Intent i = new Intent(SymptomChatActivity.this, SymptomSurveyActivity.class);
                                    i.putExtra("CONDITION_NAME", condition);
                                    startActivity(i);
                                    finish();
                                } catch (Exception e) {
                                    tvAIQuestion.setText(reply); // Fallback to showing the text
                                }
                            } else {
                                tvTitle.setText("Sakhi");
                                tvAIQuestion.setText(reply);
                            }
                        } else {
                            // Detailed error logging
                            Log.e("SakhiError", "Code: " + response.code() + " Message: " + response.message());
                            tvAIQuestion.setText("AI Error: " + response.code() + ". Check Model ID or API Key.");
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e("SakhiError", "Failure: " + t.getMessage());
                        tvAIQuestion.setText("Connection failed. Check your internet.");
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}