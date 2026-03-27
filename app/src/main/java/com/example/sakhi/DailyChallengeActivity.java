package com.example.sakhi;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DailyChallengeActivity extends AppCompatActivity {

    TextView tvTitle, tvDesc, tvTimer;
    Button btnAction1, btnAction2;
    private boolean isGlass1Done = false;
    private boolean isGlass2Done = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_challenge);

        // Initialize Views
        tvTitle = findViewById(R.id.tvTitle);
        tvDesc = findViewById(R.id.tvDesc);
        tvTimer = findViewById(R.id.tvTimer);
        btnAction1 = findViewById(R.id.btnAction1);
        btnAction2 = findViewById(R.id.btnAction2);

        // 1. Check if already done today for THIS user
        if (isAlreadyCompletedToday()) {
            showCompletedState();
            return;
        }

        // 2. Rotate challenges based on date
        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int type = day % 3;

        if (type == 0) loadTapChallenge();
        else if (type == 1) loadTimerChallenge();
        else loadQuizChallenge();
    }

    private boolean isAlreadyCompletedToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String userId = SessionManager.getUserId(this);
        return ChallengeManager.isCompleted(this, today, userId);
    }

    private void showCompletedState() {
        tvTitle.setText("All Done for Today! 🌟");
        tvDesc.setText("You've already earned your +10 points. Come back tomorrow!");
        tvTimer.setVisibility(View.GONE);
        btnAction1.setVisibility(View.GONE);
        btnAction2.setVisibility(View.GONE);

        new Handler().postDelayed(this::finish, 3000);
    }

    private void loadTapChallenge() {
        tvTitle.setText("💧 Hydration Challenge");
        tvDesc.setText("Drink 2 glasses of water to complete.");
        tvTimer.setVisibility(View.GONE);

        btnAction1.setText("Glass 1 🥛");
        btnAction2.setText("Glass 2 🥛");

        btnAction1.setOnClickListener(v -> {
            isGlass1Done = true;
            btnAction1.setEnabled(false);
            btnAction1.setAlpha(0.5f);
            checkTapComplete();
        });

        btnAction2.setOnClickListener(v -> {
            isGlass2Done = true;
            btnAction2.setEnabled(false);
            btnAction2.setAlpha(0.5f);
            checkTapComplete();
        });
    }

    private void checkTapComplete() {
        if (isGlass1Done && isGlass2Done) {
            finishChallenge("Hydration Goal Reached! 💧");
        }
    }

    private void loadTimerChallenge() {
        tvTitle.setText("🧘‍♀️ Mindfulness Stretch");
        tvDesc.setText("Hold a stretch for 30 seconds.");
        btnAction2.setVisibility(View.GONE);
        btnAction1.setText("Start Stretching");
        tvTimer.setVisibility(View.VISIBLE);
        tvTimer.setText("30s");

        btnAction1.setOnClickListener(v -> {
            btnAction1.setEnabled(false);
            new CountDownTimer(30000, 1000) {
                public void onTick(long millis) {
                    tvTimer.setText((millis / 1000) + "s");
                    btnAction1.setText("Stay Still...");
                }
                public void onFinish() {
                    tvTimer.setText("Done! 🎉");
                    finishChallenge("Feeling Relaxed? Great job! 🌸");
                }
            }.start();
        });
    }

    private void loadQuizChallenge() {
        tvTitle.setText("🩸 Health Trivia");
        tvDesc.setText("Iron absorption is improved by Vitamin C. True or False?");
        tvTimer.setVisibility(View.GONE);

        btnAction1.setText("True");
        btnAction2.setText("False");

        btnAction1.setOnClickListener(v -> finishChallenge("Correct! Vitamin C + Iron = Power! 🍋"));
        btnAction2.setOnClickListener(v -> {
            Toast.makeText(this, "Vitamin C actually helps iron absorption!", Toast.LENGTH_LONG).show();
            finishChallenge("Thanks for participating! 🌟");
        });
    }

    private void finishChallenge(String message) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String userId = SessionManager.getUserId(this);

        // Mark as done in SharedPreferences
        ChallengeManager.markAsDone(this, today, userId);

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "+10 Wellness Points Earned! 🌸", Toast.LENGTH_LONG).show();

        new Handler().postDelayed(this::finish, 2000);
    }
}