package com.example.sakhi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class HospitalDetailsActivity extends AppCompatActivity {

    private String extractedPhone = "Not Available";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_details);

        // 1. Initialize Views
        ImageView img = findViewById(R.id.detailImage);
        TextView title = findViewById(R.id.detailTitle);
        TextView loc = findViewById(R.id.detailLocation);
        TextView desc = findViewById(R.id.detailDescription);
        ImageView verified = findViewById(R.id.detailVerified);
        Button btnDirection = findViewById(R.id.btnDirection);
        Button btnCall = findViewById(R.id.btnCall);
        ImageView btnBack = findViewById(R.id.btnBack);

        // 2. Get Data
        String hospitalName = getIntent().getStringExtra("NAME");
        String distanceData = getIntent().getStringExtra("LOCATION");
        String description = getIntent().getStringExtra("DESC");
        int imageRes = getIntent().getIntExtra("IMAGE", R.drawable.map_image);
        boolean isVerified = getIntent().getBooleanExtra("VERIFIED", false);

        // 3. Set UI
        title.setText(hospitalName != null ? hospitalName : "Hospital");
        loc.setText(distanceData != null ? distanceData : "Location not available");
        img.setImageResource(imageRes);

        if (description != null && !description.isEmpty()) {
            desc.setText(description);
            extractedPhone = extractPhone(description); // 🔥 SAFE EXTRACTION
        } else {
            desc.setText("Verified healthcare facility. No additional details available.");
        }

        verified.setVisibility(isVerified ? View.VISIBLE : View.GONE);

        // ✅ 4. DIRECTIONS (MORE ACCURATE)
        btnDirection.setOnClickListener(v -> {
            if (hospitalName != null && distanceData != null) {

                // Better search query
                String query = hospitalName + " " + distanceData;

                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(query));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Opening in browser...", Toast.LENGTH_SHORT).show();

                    Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query));
                    startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                }
            } else {
                Toast.makeText(this, "Location data missing", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ 5. CALL BUTTON (SAFE + STRICT)
        btnCall.setOnClickListener(v -> {

            if (extractedPhone.equals("Not Available") || extractedPhone.length() < 8) {
                Toast.makeText(this, "No verified phone available for this hospital.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + extractedPhone));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open dialer.", Toast.LENGTH_SHORT).show();
            }
        });

        // 6. BACK
        btnBack.setOnClickListener(v -> finish());
    }

    // ✅ SAFE PHONE EXTRACTION METHOD (VERY IMPORTANT 🔥)
    private String extractPhone(String description) {
        try {
            if (!description.contains("Contact:")) return "Not Available";

            String phone = description.split("Contact:")[1].split("\n")[0].trim();

            // Clean unwanted characters
            phone = phone.replaceAll("[^0-9+]", "");

            // Validate
            if (phone.length() < 8) return "Not Available";

            return phone;

        } catch (Exception e) {
            return "Not Available";
        }
    }
}