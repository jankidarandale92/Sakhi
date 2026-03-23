package com.example.sakhi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SymptomSummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_summary);

        // 1. Get the suspected condition from Intent
        // Mark as final to be used safely inside the OnClickListener lambda
        final String condition = getIntent().getStringExtra("CONDITION_NAME") != null
                ? getIntent().getStringExtra("CONDITION_NAME")
                : "General Checkup";

        // 2. Update UI components
        TextView tvConditionTitle = findViewById(R.id.tvConditionName);
        if (tvConditionTitle != null) {
            tvConditionTitle.setText(condition);
        }

        TextView tvMessage = findViewById(R.id.tvMessage);
        if (tvMessage != null) {
            tvMessage.setText("Your symptoms suggest a possibility of " + condition + ".");
        }

        // 3. Handle the Severity Logic (Popup & Alarm trigger inside here)
        handleEmergencyLogic(condition);

        // 4. "Find Nearby Care" Button Logic
        Button btnFindCare = findViewById(R.id.btnFindCare);
        btnFindCare.setOnClickListener(v -> {
            String query = "Doctors for " + condition + " near me";
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            try {
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Uri webUri = Uri.parse("https://www.google.com/maps/search/" + Uri.encode(query));
                    startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                }
            } catch (Exception e) {
                Toast.makeText(this, "Could not open maps. Please check your browser.", Toast.LENGTH_SHORT).show();
            }
        });

        // 5. Back Button Logic
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void handleEmergencyLogic(String condition) {
        SharedPreferences prefs = getSharedPreferences("SakhiHealthHistory", MODE_PRIVATE);
        String key = "first_report_" + condition.toLowerCase().replace(" ", "_");

        long firstReportedTimestamp = prefs.getLong(key, System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();

        // Testing math: 1 minute = 1 day
        long diffInMs = currentTime - firstReportedTimestamp;
        long daysPassed = diffInMs / (1000 * 60);
        // long daysPassed = diffInMs / (1000 * 60 * 60 * 24); // Real days

        TextView tvNote = findViewById(R.id.tvNote);
        if (tvNote != null) {
            if (daysPassed >= 7) {
                // --- RED ALERT ---
                tvNote.setText("🚨 CRITICAL: Persistent for " + daysPassed + " days. Please seek urgent medical advice immediately.");
                tvNote.setTextColor(Color.RED);
                tvNote.setTypeface(null, Typeface.BOLD);

                // Show the Emergency Popup and Alarm Sound
                showEmergencyPopup(condition, daysPassed);

            } else if (daysPassed >= 3) {
                // --- ORANGE WARNING ---
                tvNote.setText("⚠️ WARNING: Symptom persistent for " + daysPassed + " days. Consider booking a doctor's appointment.");
                tvNote.setTextColor(Color.parseColor("#E65100")); // Deep Orange
                tvNote.setTypeface(null, Typeface.BOLD);
            } else {
                // --- PINK NORMAL ---
                tvNote.setText("🌸 Early attention can help prevent long-term complications.");
                tvNote.setTextColor(Color.parseColor("#D81B60"));
                tvNote.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    /**
     * Plays a notification sound and displays a modal dialog for Critical alerts.
     */
    private void showEmergencyPopup(String condition, long days) {
        // 1. Play Alarm/Notification Sound
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Build and Show the Alert Dialog
        new AlertDialog.Builder(this)
                .setTitle("🚨 Urgent Health Alert")
                .setMessage("You have reported symptoms related to " + condition + " for over " + days + " days. This may require medical attention. Would you like to find a doctor now?")
                .setCancelable(false) // Forces user to acknowledge
                .setPositiveButton("Find Doctor", (dialog, which) -> {
                    // Clicks the existing Maps button for the user
                    findViewById(R.id.btnFindCare).performClick();
                })
                .setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (!isTaskRoot()) {
            super.onBackPressed();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } else {
            startActivity(new Intent(this, SymptomChatActivity.class));
            finish();
        }
    }
}