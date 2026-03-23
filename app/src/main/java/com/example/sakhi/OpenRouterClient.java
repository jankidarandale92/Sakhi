package com.example.sakhi;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenRouterClient {

    @POST("chat/completions")
    Call<OpenRouterResponse> getChatCompletion(
            @Header("Authorization") String token,
            @Body OpenRouterRequest body
    );

    // Static helper to initialize Retrofit
    static OpenRouterClient getInterface() {
        return new Retrofit.Builder()
                .baseUrl("https://openrouter.ai/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenRouterClient.class);
    }
}

// --- DATA MODELS (POJOs) ---

class OpenRouterRequest {
    @SerializedName("model")
    String model;

    @SerializedName("messages")
    List<Message> messages;

    OpenRouterRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }
}

class Message {
    @SerializedName("role")
    String role;

    @SerializedName("content")
    String content;

    Message(String role, String content) {
        this.role = role;
        this.content = content;
    }
}

class OpenRouterResponse {
    @SerializedName("choices")
    List<Choice> choices;

    // Must be public static so the Activity can access Choice.message
    public static class Choice {
        @SerializedName("message")
        Message message;
    }
}