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
            // 🔥 BALANCED & CLEAN PROMPT
            String systemPrompt = "You are Sakhi, a professional women's health triage assistant. " +
                    "Your goal is to gather a holistic view of the user's health before identifying a condition. " +
                    "RULES: " +
                    "1. When a user mentions a symptom, ask exactly 2-3 broad follow-up questions to understand the overall context (e.g., location, duration, fever, or related discomfort). " +
                    "2. Do not limit yourself to one possibility; consider digestion, infection, or lifestyle alongside hormonal health. " +
                    "3. Once you have enough context, reply ONLY with [POSSIBLE: ConditionName]. " +
                    "4. FORMATTING: Use plain text only. DO NOT use stars (*), bullets, or bold formatting. " +
                    "5. LANGUAGE: Always respond in the SAME language the user is using (English, Hindi, or Marathi). " +
                    "6. Be empathetic but keep your questions concise and easy to read.";

            messages.add(new Message("system", systemPrompt));
        }
        messages.add(new Message("user", userMsg));

        OpenRouterRequest request = new OpenRouterRequest("google/gemini-2.0-flash-001", messages);

        OpenRouterClient.getInterface().getChatCompletion("Bearer " + BuildConfig.OPENROUTER_KEY, request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null && response.body().choices != null && !response.body().choices.isEmpty()) {
                            String reply = response.body().choices.get(0).message.content;

                            // Remove any accidental markdown stars or bullets just in case
                            reply = reply.replace("*", "").replace("- ", "").trim();

                            messages.add(new Message("assistant", reply));

                            if (reply.contains("[POSSIBLE:")) {
                                try {
                                    String condition = reply.substring(reply.indexOf(":") + 1, reply.indexOf("]")).trim();
                                    Intent i = new Intent(SymptomChatActivity.this, SymptomSurveyActivity.class);
                                    i.putExtra("CONDITION_NAME", condition);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}