package com.example.sakhi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private ShapeableImageView imgProfile;
    private TextView tvGreeting, tvWelcome;

    private RecyclerView rvPopular;
    private TextView tvSeeAll;
    private SwipeRefreshLayout swipeRefreshHome;
    private LinearLayout noInternetLayout;
    private Button btnRetry;

    private static final String NEWS_API_KEY = "f09730fea1764ed1a55bfeb7d13a244b";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNic3Bxbm5tdWxsZXpscGJkemhzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njg4MTc4NDYsImV4cCI6MjA4NDM5Mzg0Nn0.H9p0LoBRWEgjKBRSfKg1DdwnCN7qV2dQCo2gVEL7DiU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // UI Initialization
        imgProfile = findViewById(R.id.imgProfile);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvWelcome = findViewById(R.id.tvWelcome);
        rvPopular = findViewById(R.id.rvPopularArticles);
        tvSeeAll = findViewById(R.id.tvSeeAll);
        swipeRefreshHome = findViewById(R.id.swipeRefreshHome);
        noInternetLayout = findViewById(R.id.noInternetLayout);
        btnRetry = findViewById(R.id.btnRetry);

        // Navigation and Data Update
        updateDailyChallengeCard();
        BottomNavHelper.setupBottomNav(this, R.id.navHome);

        // Setup Listeners
        if (imgProfile != null) imgProfile.setOnClickListener(v -> showProfileMenu());
        if (btnRetry != null) btnRetry.setOnClickListener(v -> fetchArticles());
        if (tvSeeAll != null) tvSeeAll.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AllArticlesActivity.class)));

        setupFeatureButtons();

        if (rvPopular != null) {
            rvPopular.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvPopular.setHasFixedSize(true);
        }

        if (swipeRefreshHome != null) {
            swipeRefreshHome.setColorSchemeColors(Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"));
            swipeRefreshHome.setOnRefreshListener(this::fetchArticles);
        }

        loadUserName();
        fetchArticles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDailyChallengeCard();
        loadProfileImage();
    }

    private void setupFeatureButtons() {
        findViewById(R.id.btnPlayMythFact).setOnClickListener(v -> {
            // 🔥 FIXED: User-specific completion check
            if (MythFactManager.isCompleted(this)) {
                Toast.makeText(this, "You've learned your wellness fact for today! 🌸", Toast.LENGTH_SHORT).show();
            } else {
                showMythFactGame();
            }
        });

        View included = findViewById(R.id.trackSymptomsInclude);
        if (included != null) {
            View btnTrack = included.findViewById(R.id.btnTrack);
            if (btnTrack != null) {
                btnTrack.setOnClickListener(v -> startActivity(new Intent(this, SymptomChatActivity.class)));
            }
        }
    }

    private void fetchArticles() {
        if (swipeRefreshHome != null && !swipeRefreshHome.isRefreshing()) swipeRefreshHome.setRefreshing(true);

        String strictQuery = "\"women health\" AND (PCOS OR PCOD OR thyroid OR menstruation OR stress OR sleep OR diet)";

        RetrofitClient.getService().getArticles(strictQuery, "en", "publishedAt", NEWS_API_KEY)
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                        if (swipeRefreshHome != null) swipeRefreshHome.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            List<Article> articleList = response.body().getArticles();
                            if (articleList != null && !articleList.isEmpty()) {
                                if (noInternetLayout != null) noInternetLayout.setVisibility(View.GONE);
                                if (rvPopular != null) rvPopular.setVisibility(View.VISIBLE);
                                int limit = Math.min(articleList.size(), 5);
                                ArticleAdapter adapter = new ArticleAdapter(HomeActivity.this, articleList.subList(0, limit), R.layout.item_article);
                                rvPopular.setAdapter(adapter);
                            } else { showErrorLayout(); }
                        } else { showErrorLayout(); }
                    }
                    @Override
                    public void onFailure(Call<NewsResponse> call, Throwable t) {
                        if (swipeRefreshHome != null) swipeRefreshHome.setRefreshing(false);
                        showErrorLayout();
                    }
                });
    }

    private void showErrorLayout() {
        if (noInternetLayout != null) noInternetLayout.setVisibility(View.VISIBLE);
        if (rvPopular != null) rvPopular.setVisibility(View.GONE);
    }

    private void showProfileMenu() {
        View menuView = getLayoutInflater().inflate(R.layout.layout_profile_menu, null);
        PopupWindow popupWindow = new PopupWindow(menuView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setElevation(20f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAsDropDown(imgProfile, -90, 20);

        menuView.findViewById(R.id.menuProfile).setOnClickListener(v -> { popupWindow.dismiss(); startActivity(new Intent(this, ProfileActivity.class)); });
        menuView.findViewById(R.id.menuReminders).setOnClickListener(v -> { popupWindow.dismiss(); startActivity(new Intent(this, RemainderActivity.class)); });
        menuView.findViewById(R.id.feedback).setOnClickListener(v -> { popupWindow.dismiss(); startActivity(new Intent(this, FeedbackListActivity.class)); });
        menuView.findViewById(R.id.menuLogout).setOnClickListener(v -> { popupWindow.dismiss(); logoutUser(); });
    }

    private void showMythFactGame() {
        // 🔥 FIXED: No longer passing 'this' as it's a static daily rotation
        MythFactQuestion q = MythFactManager.getTodayQuestion();

        View view = getLayoutInflater().inflate(R.layout.bottomsheet_myth_fact, null);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        dialog.setContentView(view);

        TextView tvQ = view.findViewById(R.id.tvQuestion);
        TextView tvRes = view.findViewById(R.id.tvResult);
        tvQ.setText(q.question);

        View.OnClickListener listener = v -> {
            boolean userAnswer = (v.getId() == R.id.btnFact);

            if (userAnswer == q.isFact) {
                tvRes.setText("Correct! 🎉\n" + q.explanation + "\n\n+10 points 🌸");
                tvRes.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else {
                tvRes.setText("Not quite!\n" + q.explanation + "\n\nGood try! ✨");
                tvRes.setTextColor(Color.parseColor("#F44336")); // Red
            }

            tvRes.setVisibility(View.VISIBLE);
            view.findViewById(R.id.btnMyth).setEnabled(false);
            view.findViewById(R.id.btnFact).setEnabled(false);

            // 🔥 FIXED: Marks completed for this user specifically
            MythFactManager.markCompleted(this);
        };

        view.findViewById(R.id.btnMyth).setOnClickListener(listener);
        view.findViewById(R.id.btnFact).setOnClickListener(listener);
        dialog.show();
    }

    private void loadProfileImage() {
        String userId = SessionManager.getUserId(this);
        String accessToken = SessionManager.getAccessToken(this);
        if (userId == null || accessToken == null) return;

        SupabaseProfileApi api = RetrofitClient.getClient().create(SupabaseProfileApi.class);
        api.getProfile(SUPABASE_KEY, "Bearer " + accessToken, "eq." + userId, "*").enqueue(new retrofit2.Callback<com.google.gson.JsonArray>() {
            @Override
            public void onResponse(retrofit2.Call<com.google.gson.JsonArray> call, retrofit2.Response<com.google.gson.JsonArray> response) {
                if (response.isSuccessful() && response.body() != null && response.body().size() > 0) {
                    JsonObject profile = response.body().get(0).getAsJsonObject();
                    String imageUrl = profile.has("image_url") && !profile.get("image_url").isJsonNull() ? profile.get("image_url").getAsString() : "Not set";
                    if (!imageUrl.equals("Not set")) {
                        Glide.with(HomeActivity.this).load(imageUrl + "?t=" + System.currentTimeMillis()).placeholder(R.drawable.profile_image).skipMemoryCache(true).diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE).into(imgProfile);
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.google.gson.JsonArray> call, Throwable t) {}
        });
    }

    private void loadUserName() {
        String userId = SessionManager.getUserId(this);
        String token = SessionManager.getAccessToken(this);
        if (userId == null || token == null) return;

        SupabaseProfileApi api = RetrofitClient.getClient().create(SupabaseProfileApi.class);
        api.getProfile(SUPABASE_KEY, "Bearer " + token, "eq." + userId, "*").enqueue(new retrofit2.Callback<com.google.gson.JsonArray>() {
            @Override
            public void onResponse(retrofit2.Call<com.google.gson.JsonArray> call, retrofit2.Response<com.google.gson.JsonArray> response) {
                if (response.isSuccessful() && response.body() != null && response.body().size() > 0) {
                    JsonObject profile = response.body().get(0).getAsJsonObject();
                    String fullName = profile.has("full_name") && !profile.get("full_name").isJsonNull() ? profile.get("full_name").getAsString() : profile.get("username").getAsString();
                    tvGreeting.setText("Hi, " + fullName);
                    tvWelcome.setText("Welcome, " + fullName + " 👋 Here are some health tasks for you!");
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.google.gson.JsonArray> call, Throwable t) { t.printStackTrace(); }
        });
    }

    private void updateDailyChallengeCard() {
        Button btnStart = findViewById(R.id.btnStartChallenge);
        if (btnStart == null) return;

        View cardParent = (View) btnStart.getParent();
        TextView tvStatus = cardParent.findViewById(R.id.tvChallengeStatus);

        String userId = SessionManager.getUserId(this);
        String today = ChallengeManager.getTodayString();

        if (ChallengeManager.isCompleted(this, today, userId)) {
            btnStart.setText("Completed ✓");
            btnStart.setEnabled(false);
            btnStart.setAlpha(0.6f);
            if (tvStatus != null) tvStatus.setText("You earned +10 points today 🌸");
        } else {
            btnStart.setText("Start");
            btnStart.setEnabled(true);
            btnStart.setAlpha(1f);
            if (tvStatus != null) tvStatus.setText("Earn +10 points today 🌸");
            btnStart.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DailyChallengeActivity.class)));
        }
    }

    private void logoutUser() {
        SessionManager.clearSession(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}