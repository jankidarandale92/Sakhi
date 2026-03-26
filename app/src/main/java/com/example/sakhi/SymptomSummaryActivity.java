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

        // 1. Get data from Intent
        final String condition = getIntent().getStringExtra("CONDITION_NAME") != null
                ? getIntent().getStringExtra("CONDITION_NAME")
                : "General Checkup";

        // 🔥 RECEIVE THE SPECIFIC LANGUAGE (English, Hindi, or Marathi)
        final String userLang = getIntent().getStringExtra("USER_LANG") != null
                ? getIntent().getStringExtra("USER_LANG")
                : "English";

        // 2. Update UI components
        TextView tvConditionTitle = findViewById(R.id.tvConditionName);
        if (tvConditionTitle != null) {
            tvConditionTitle.setText(condition);
        }

        TextView tvMessage = findViewById(R.id.tvMessage);
        if (tvMessage != null) {
            // 🔥 MULTILINGUAL FEEDBACK BASED ON SPECIFIC USER_LANG
            if ("Marathi".equalsIgnoreCase(userLang)) {
                tvMessage.setText("तुमच्या लक्षणांवरून तुम्हाला " + condition + " असण्याची शक्यता आहे.");
            } else if ("Hindi".equalsIgnoreCase(userLang)) {
                tvMessage.setText("आपके लक्षणों के आधार पर आपको " + condition + " होने की संभावना है।");
            } else {
                tvMessage.setText("Your symptoms suggest a possibility of " + condition + ".");
            }
        }

        // 3. Handle the Severity Logic
        handleEmergencyLogic(condition, userLang);

        // 4. "Find Nearby Care" Button Logic
        Button btnFindCare = findViewById(R.id.btnFindCare);
        btnFindCare.setOnClickListener(v -> {
            String query;
            if ("Marathi".equalsIgnoreCase(userLang)) {
                query = "जवळचे डॉक्टर " + condition;
            } else if ("Hindi".equalsIgnoreCase(userLang)) {
                query = "नज़दीकी डॉक्टर " + condition;
            } else {
                query = "Doctors for " + condition + " near me";
            }

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

    private void handleEmergencyLogic(String condition, String userLang) {
        SharedPreferences prefs = getSharedPreferences("SakhiHealthHistory", MODE_PRIVATE);
        String key = "first_report_" + condition.toLowerCase().replace(" ", "_");

        long firstReportedTimestamp = prefs.getLong(key, System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();

        long diffInMs = currentTime - firstReportedTimestamp;

        // --- FORMULA SECTION ---
        // Real Formula: long daysPassed = diffInMs / (1000 * 60 * 60 * 24);

        // Testing Formula: 1 minute = 1 day
        long daysPassed = diffInMs / (1000 * 60);
        // -----------------------

        TextView tvNote = findViewById(R.id.tvNote);
        if (tvNote != null) {
            if (daysPassed >= 7) {
                // --- RED ALERT ---
                tvNote.setText(getLocalizedSeverity("Critical", userLang, daysPassed));
                tvNote.setTextColor(Color.RED);
                tvNote.setTypeface(null, Typeface.BOLD);
                showEmergencyPopup(condition, daysPassed, userLang);

            } else if (daysPassed >= 3) {
                // --- ORANGE WARNING ---
                tvNote.setText(getLocalizedSeverity("Warning", userLang, daysPassed));
                tvNote.setTextColor(Color.parseColor("#E65100"));
                tvNote.setTypeface(null, Typeface.BOLD);
            } else {
                // --- PINK NORMAL ---
                tvNote.setText(getLocalizedSeverity("Normal", userLang, daysPassed));
                tvNote.setTextColor(Color.parseColor("#D81B60"));
            }
        }
    }

    // 🔥 Helper to get the correct text for Hindi, Marathi, or English
    private String getLocalizedSeverity(String level, String lang, long days) {
        if ("Marathi".equalsIgnoreCase(lang)) {
            if ("Critical".equals(level)) return "🚨 गंभीर: " + days + " दिवसांपासून त्रास होत आहे. कृपया त्वरित डॉक्टरांचा सल्ला घ्या.";
            if ("Warning".equals(level)) return "⚠️ चेतावणी: हा त्रास " + days + " दिवसांपासून आहे. डॉक्टरांना भेटण्याचा विचार करा.";
            return "🌸 वेळेवर लक्ष दिल्यास पुढील त्रास टाळता येतो.";
        } else if ("Hindi".equalsIgnoreCase(lang)) {
            if ("Critical".equals(level)) return "🚨 गंभीर: " + days + " दिनों से समस्या बनी हुई है। कृपया तुरंत डॉक्टर से मिलें।";
            if ("Warning".equals(level)) return "⚠️ चेतावनी: यह समस्या " + days + " दिनों से है। डॉक्टर से सलाह लेने पर विचार करें।";
            return "🌸 समय पर ध्यान देने से भविष्य की जटिलताओं को रोका जा सकता है।";
        } else {
            if ("Critical".equals(level)) return "🚨 CRITICAL: Persistent for " + days + " days. Please seek urgent medical advice.";
            if ("Warning".equals(level)) return "⚠️ WARNING: Symptom persistent for " + days + " days. Consider booking an appointment.";
            return "🌸 Early attention can help prevent long-term complications.";
        }
    }

    private void showEmergencyPopup(String condition, long days, String userLang) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) { e.printStackTrace(); }

        String title, msg, btnPos, btnNeg;

        if ("Marathi".equalsIgnoreCase(userLang)) {
            title = "🚨 तातडीची आरोग्य सूचना";
            msg = "तुम्ही " + days + " दिवसांपासून " + condition + " संबंधित लक्षणे नोंदवली आहेत. डॉक्टरांची गरज असू शकते.";
            btnPos = "डॉक्टर शोधा"; btnNeg = "बंद करा";
        } else if ("Hindi".equalsIgnoreCase(userLang)) {
            title = "🚨 तत्काल स्वास्थ्य चेतावनी";
            msg = "आपने " + days + " दिनों से " + condition + " से संबंधित लक्षणों की सूचना दी है। आपको डॉक्टर की आवश्यकता हो सकती है।";
            btnPos = "डॉक्टर खोजें"; btnNeg = "खारिज करें";
        } else {
            title = "🚨 Urgent Health Alert";
            msg = "You have reported symptoms related to " + condition + " for over " + days + " days. Seek medical attention.";
            btnPos = "Find Doctor"; btnNeg = "Dismiss";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(btnPos, (dialog, which) -> findViewById(R.id.btnFindCare).performClick())
                .setNegativeButton(btnNeg, (dialog, which) -> dialog.dismiss())
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