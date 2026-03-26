package com.example.sakhi;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    RecyclerView rvChat;
    EditText etMessage;
    ImageButton btnSend;
    ImageView btnBack;

    ChatAdapter adapter;
    List<ChatMessage> chatList;

    // conversation history
    List<Message> apiMessages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        chatList = new ArrayList<>();
        // Professional Greeting
        chatList.add(new ChatMessage("Namaste! I am Sakhi. How can I help you with your health today?", false));

        adapter = new ChatAdapter(chatList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    private void sendMessage() {
        String userQuery = etMessage.getText().toString().trim();
        if (userQuery.isEmpty()) return;

        chatList.add(new ChatMessage(userQuery, true));
        adapter.notifyItemInserted(chatList.size() - 1);
        rvChat.scrollToPosition(chatList.size() - 1);
        etMessage.setText("");

        // 🔥 MULTILINGUAL SYSTEM PROMPT WITHOUT STARS
        if (apiMessages.isEmpty()) {
            String professionalPrompt = "You are Sakhi, a professional women's health triage assistant. " +
                    "1. Respond ONLY in the language used by the user (English, Hindi, or Marathi). " +
                    "2. NO FORMATTING: Do NOT use stars (*), hashtags (#), or bullet points (-). " +
                    "3. STRUCTURE: Use plain paragraphs and simple numbering (1, 2, 3) if needed. " +
                    "4. SCOPE: Only answer health, diet, and fitness queries. Otherwise, politely decline.";

            apiMessages.add(new Message("system", professionalPrompt));
        }
        apiMessages.add(new Message("user", userQuery));

        OpenRouterRequest request = new OpenRouterRequest("google/gemini-2.0-flash-001", apiMessages);

        OpenRouterClient.getInterface().getChatCompletion("Bearer " + BuildConfig.OPENROUTER_KEY, request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String aiReply = response.body().choices.get(0).message.content;

                            // 🔥 STRING CLEANUP: Remove any accidental Markdown stars
                            aiReply = aiReply.replace("*", "").trim();

                            apiMessages.add(new Message("assistant", aiReply));

                            String finalReply = aiReply;
                            runOnUiThread(() -> {
                                chatList.add(new ChatMessage(finalReply, false));
                                adapter.notifyItemInserted(chatList.size() - 1);
                                rvChat.scrollToPosition(chatList.size() - 1);
                            });
                        } else {
                            Toast.makeText(ChatActivity.this, "Connection Error", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                        Toast.makeText(ChatActivity.this, "Network Failure", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}