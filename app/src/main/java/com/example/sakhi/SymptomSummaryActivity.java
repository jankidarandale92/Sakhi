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
            // 🔥 MULTILINGUAL FEEDBACK
            if (isDevanagari(condition)) {
                tvMessage.setText("तुमच्या लक्षणांवरून तुम्हाला " + condition + " असण्याची शक्यता आहे.");
            } else {
                tvMessage.setText("Your symptoms suggest a possibility of " + condition + ".");
            }
        }

        // 3. Handle the Severity Logic
        handleEmergencyLogic(condition);

        // 4. "Find Nearby Care" Button Logic
        Button btnFindCare = findViewById(R.id.btnFindCare);
        btnFindCare.setOnClickListener(v -> {
            String query = isDevanagari(condition) ? "जवळचे डॉक्टर " + condition : "Doctors for " + condition + " near me";
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Maps could not be opened.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void handleEmergencyLogic(String condition) {
        SharedPreferences prefs = getSharedPreferences("SakhiHealthHistory", MODE_PRIVATE);
        String key = "first_report_" + condition.toLowerCase().replace(" ", "_");

        long firstReportedTimestamp = prefs.getLong(key, System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();

        long diffInMs = currentTime - firstReportedTimestamp;
        long daysPassed = diffInMs / (1000 * 60); // Testing: 1 min = 1 day

        TextView tvNote = findViewById(R.id.tvNote);
        if (tvNote != null) {
            boolean isLocal = isDevanagari(condition);

            if (daysPassed >= 7) {
                String text = isLocal ? "🚨 गंभीर: " + daysPassed + " दिवसांपासून त्रास होत आहे. कृपया त्वरित डॉक्टरांचा सल्ला घ्या."
                        : "🚨 CRITICAL: Persistent for " + daysPassed + " days. Please seek urgent medical advice.";
                tvNote.setText(text);
                tvNote.setTextColor(Color.RED);
                tvNote.setTypeface(null, Typeface.BOLD);
                showEmergencyPopup(condition, daysPassed);

            } else if (daysPassed >= 3) {
                String text = isLocal ? "⚠️ चेतावणी: हा त्रास " + daysPassed + " दिवसांपासून आहे. डॉक्टरांची भेट घेण्याचा विचार करा."
                        : "⚠️ WARNING: Symptom persistent for " + daysPassed + " days. Consider booking an appointment.";
                tvNote.setText(text);
                tvNote.setTextColor(Color.parseColor("#E65100"));
                tvNote.setTypeface(null, Typeface.BOLD);
            } else {
                String text = isLocal ? "🌸 वेळेवर लक्ष दिल्यास पुढील त्रास टाळता येतो."
                        : "🌸 Early attention can help prevent long-term complications.";
                tvNote.setText(text);
                tvNote.setTextColor(Color.parseColor("#D81B60"));
            }
        }
    }

    private void showEmergencyPopup(String condition, long days) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) { e.printStackTrace(); }

        boolean isLocal = isDevanagari(condition);
        String title = isLocal ? "🚨 तातडीची आरोग्य सूचना" : "🚨 Urgent Health Alert";
        String msg = isLocal ? "तुम्ही " + days + " दिवसांपासून " + condition + " संबंधित लक्षणे नोंदवली आहेत. डॉक्टरांची गरज असू शकते."
                : "You have reported symptoms related to " + condition + " for over " + days + " days. Seek medical attention.";
        String btnPos = isLocal ? "डॉक्टर शोधा" : "Find Doctor";
        String btnNeg = isLocal ? "बंद करा" : "Dismiss";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(btnPos, (dialog, which) -> findViewById(R.id.btnFindCare).performClick())
                .setNegativeButton(btnNeg, (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // 🔥 Helper to detect Devanagari (Hindi/Marathi) Script
    private boolean isDevanagari(String text) {
        return text.matches(".*[\\u0900-\\u097F].*");
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