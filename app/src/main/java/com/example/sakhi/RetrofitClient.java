package com.example.sakhi;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // --- 1. SUPABASE CONFIGURATION (Existing) ---
    private static final String SUPABASE_URL = "https://sbspqnnmullezlpbdzhs.supabase.co/";
    private static Retrofit supabaseRetrofit;

    // Keeps your existing Supabase login/profile working
    public static Retrofit getClient() {
        if (supabaseRetrofit == null) {
            supabaseRetrofit = new Retrofit.Builder()
                    .baseUrl(SUPABASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return supabaseRetrofit;
    }

    // --- 2. NEWS API CONFIGURATION (New) ---
    private static final String NEWS_URL = "https://newsapi.org/";
    private static Retrofit newsRetrofit;

    // This is the method your HomeActivity is trying to call
    public static NewsApiService getService() {
        if (newsRetrofit == null) {
            newsRetrofit = new Retrofit.Builder()
                    .baseUrl(NEWS_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return newsRetrofit.create(NewsApiService.class);
    }
}