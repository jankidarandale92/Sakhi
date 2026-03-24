package com.example.sakhi;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllArticlesActivity extends AppCompatActivity {

    private RecyclerView rvAllArticles;
    private EditText etSearch;
    private Button btnSearch, btnRetry;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout noInternetLayout;
    private ArticleAdapter adapter;
    private List<Article> articleList = new ArrayList<>();

    private static final String NEWS_API_KEY = "f09730fea1764ed1a55bfeb7d13a244b";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_articles);

        // 1. Setup UI Components
        btnSearch = findViewById(R.id.btnSearch);
        btnRetry = findViewById(R.id.btnRetry);
        etSearch = findViewById(R.id.etSearch);
        rvAllArticles = findViewById(R.id.rvAllArticles);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        noInternetLayout = findViewById(R.id.noInternetLayout);
        ImageView btnBack = findViewById(R.id.btnBack);

        // 2. Setup RecyclerView (Important: Initialize adapter once)
        rvAllArticles.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArticleAdapter(this, articleList, R.layout.item_article);
        rvAllArticles.setAdapter(adapter);

        // 3. Setup SwipeRefresh
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"));
            swipeRefresh.setOnRefreshListener(() -> fetchArticles(etSearch.getText().toString().trim()));
        }

        // 4. Search Logic
        btnSearch.setOnClickListener(v -> performSearch());

        // Keyboard "Search" button click
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        if (btnRetry != null) btnRetry.setOnClickListener(v -> performSearch());
        btnBack.setOnClickListener(v -> finish());

        // Initial Fetch
        fetchArticles("");
    }

    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        fetchArticles(keyword);
    }

    private void fetchArticles(String keyword) {
        if (swipeRefresh != null && !swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(true);
        }

        if (noInternetLayout != null) noInternetLayout.setVisibility(View.GONE);

        // 🔹 Refined Query Logic
        String finalQuery;
        if (keyword.isEmpty()) {
            // Default general women health query
            finalQuery = "women health PCOS thyroid diet";
        } else {
            // Search specifically for what the user typed within women's health context
            finalQuery = "women health " + keyword;
        }

        // Log the query to your Logcat to see if it's correct
        Log.d("NewsQuery", "Fetching: " + finalQuery);

        RetrofitClient.getService().getArticles(finalQuery, "en", "publishedAt", NEWS_API_KEY)
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Article> fetchedArticles = response.body().getArticles();

                            if (fetchedArticles != null && !fetchedArticles.isEmpty()) {
                                rvAllArticles.setVisibility(View.VISIBLE);
                                articleList.clear();

                                // Limit to 50 for performance
                                int limit = Math.min(fetchedArticles.size(), 50);
                                articleList.addAll(fetchedArticles.subList(0, limit));

                                adapter.notifyDataSetChanged();
                            } else {
                                articleList.clear();
                                adapter.notifyDataSetChanged();
                                Toast.makeText(AllArticlesActivity.this, "No results found for '" + keyword + "'", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            showError();
                        }
                    }

                    @Override
                    public void onFailure(Call<NewsResponse> call, Throwable t) {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        showError();
                        Log.e("NewsError", t.getMessage());
                    }
                });
    }

    private void showError() {
        if (noInternetLayout != null) noInternetLayout.setVisibility(View.VISIBLE);
        if (rvAllArticles != null) rvAllArticles.setVisibility(View.GONE);
    }
}