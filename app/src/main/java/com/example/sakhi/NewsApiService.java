package com.example.sakhi;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {
    @GET("v2/everything")
    Call<NewsResponse> getArticles(
            @Query("q") String query,
            @Query("language") String language, // Filter by English
            @Query("sortBy") String sortBy,     // Sort by Newest
            @Query("apiKey") String apiKey
    );
}