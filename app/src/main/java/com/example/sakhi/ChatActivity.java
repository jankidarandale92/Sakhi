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

    // We maintain a list of messages for OpenRouter to remember the conversation
    List<Message> apiMessages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize UI
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        chatList = new ArrayList<>();
        chatList.add(new ChatMessage("Hi! I am Sakhi. Ask me anything about women's health, diet, or fitness.", false));

        adapter = new ChatAdapter(chatList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        // Send Button Action
        btnSend.setOnClickListener(v -> sendMessage());

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    private void sendMessage() {
        String userQuery = etMessage.getText().toString().trim();
        if (userQuery.isEmpty()) return;

        // 1. Add User Message to RecyclerView UI
        chatList.add(new ChatMessage(userQuery, true));
        adapter.notifyItemInserted(chatList.size() - 1);
        rvChat.scrollToPosition(chatList.size() - 1);
        etMessage.setText("");

        // 2. Prepare the OpenRouter API Request
        if (apiMessages.isEmpty()) {
            // Set the "Persona" for the chatbot as the first system message
            apiMessages.add(new Message("system", "You are Sakhi, a medical assistant. " +
                    "ONLY answer questions related to medical health, women's wellness, fitness, diet, PCOD/PCOS, and mental well-being. " +
                    "If asked about coding, movies, politics, or off-topic things, " +
                    "reply: 'I can only assist with health-related queries.'"));
        }
        apiMessages.add(new Message("user", userQuery));

        // 3. Call OpenRouter using Retrofit
        OpenRouterRequest request = new OpenRouterRequest("google/gemini-2.0-flash-001", apiMessages);

        OpenRouterClient.getInterface().getChatCompletion("Bearer " + BuildConfig.OPENROUTER_KEY, request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().choices != null) {
                            String aiReply = response.body().choices.get(0).message.content;

                            // Save AI response to history for next turn
                            apiMessages.add(new Message("assistant", aiReply));

                            runOnUiThread(() -> {
                                chatList.add(new ChatMessage(aiReply, false));
                                adapter.notifyItemInserted(chatList.size() - 1);
                                rvChat.scrollToPosition(chatList.size() - 1);
                            });
                        } else {
                            Log.e("ChatAPI", "Error: " + response.code());
                            Toast.makeText(ChatActivity.this, "AI Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                        Log.e("ChatAPI", "Failure: " + t.getMessage());
                        Toast.makeText(ChatActivity.this, "Connection failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}