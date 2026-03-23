package com.example.sakhi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NearbyCareActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    HospitalAdapter adapter;
    List<HospitalModel> fullList;
    EditText searchBar;
    TextView chipAll, chipGynac, chipGeneral, chip247;
    FusedLocationProviderClient fusedLocationClient;
    Location userCurrentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_care);

        recyclerView = findViewById(R.id.recyclerViewHospitals);
        searchBar = findViewById(R.id.etSearch);
        chipAll = findViewById(R.id.chipAll);
        chipGynac = findViewById(R.id.chipGynac);
        chipGeneral = findViewById(R.id.chipGeneral);
        chip247 = findViewById(R.id.chip247);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fullList = new ArrayList<>();
        adapter = new HospitalAdapter(this, fullList);
        recyclerView.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentLocation();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipAll.setOnClickListener(v -> {
            updateChipUI(chipAll);
            adapter.setFilteredList(fullList);
        });

        chipGynac.setOnClickListener(v -> {
            updateChipUI(chipGynac);
            filterByCategory("Gynecologist");
        });

        chipGeneral.setOnClickListener(v -> {
            updateChipUI(chipGeneral);
            filterByCategory("General");
        });

        chip247.setOnClickListener(v -> {
            updateChipUI(chip247);
            filterBy247();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ✅ LOCATION
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        CancellationTokenSource cts = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        userCurrentLocation = location;
                        fetchRealHospitals(location);
                    } else {
                        Toast.makeText(this, "Enable GPS for accurate results", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ✅ FETCH FROM OSM
    private void fetchRealHospitals(Location location) {

        String query = "[out:json];(" +
                "node[\"amenity\"~\"hospital|clinic\"](around:30000," + location.getLatitude() + "," + location.getLongitude() + ");" +
                "way[\"amenity\"~\"hospital|clinic\"](around:30000," + location.getLatitude() + "," + location.getLongitude() + ");" +
                ");out center tags;";

        try {
            String url = "https://overpass-api.de/api/interpreter?data=" +
                    java.net.URLEncoder.encode(query, "UTF-8");

            new FetchPlacesTask().execute(url);

        } catch (Exception e) {
            Log.e("NearbyCare", "Encoding Error", e);
        }
    }

    // ✅ API CALL
    private class FetchPlacesTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                return sb.toString();

            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                parseOSMJson(result);
            }
        }
    }

    // ✅ FIXED PHONE EXTRACTION
    private String extractValidPhone(String rawPhone) {

        if (rawPhone == null || rawPhone.isEmpty())
            return "Not Available";

        String[] parts = rawPhone.split("[,;/]");

        for (String part : parts) {

            part = part.trim();
            String cleaned = part.replaceAll("[^0-9+]", "");

            if (cleaned.length() >= 8 && cleaned.length() <= 15) {
                return cleaned;
            }
        }

        return "Not Available";
    }

    // ✅ PARSER
    private void parseOSMJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray elements = jsonObject.getJSONArray("elements");

            fullList.clear();

            for (int i = 0; i < elements.length(); i++) {

                JSONObject place = elements.getJSONObject(i);
                JSONObject tags = place.optJSONObject("tags");

                if (tags == null) continue;

                String name = tags.optString("name", "").trim();
                if (name.isEmpty()) continue;

                String amenity = tags.optString("amenity", "");
                if (!(amenity.equals("hospital") || amenity.equals("clinic"))) continue;

                String lowerName = name.toLowerCase();
                if (lowerName.contains("dental") || lowerName.contains("eye") || lowerName.contains("veterinary"))
                    continue;

                // ✅ MULTI SOURCE PHONE
                String rawPhone = tags.optString("phone");
                if (rawPhone.isEmpty()) rawPhone = tags.optString("contact:phone");
                if (rawPhone.isEmpty()) rawPhone = tags.optString("contact:mobile");

                String phone = extractValidPhone(rawPhone);

                // ✅ SERVICES
                String speciality = tags.optString("healthcare:speciality", "General medical support");
                speciality = speciality.replace(";", ", ").replace("_", " ");

                String type = "General";
                if (lowerName.contains("women") || lowerName.contains("maternity") ||
                        lowerName.contains("gynaec") || speciality.toLowerCase().contains("gynaec")) {
                    type = "Gynecologist";
                }

                // ✅ COORDINATES
                double lat = place.has("lat") ? place.getDouble("lat") :
                        place.getJSONObject("center").getDouble("lat");

                double lon = place.has("lon") ? place.getDouble("lon") :
                        place.getJSONObject("center").getDouble("lon");

                // ✅ DISTANCE
                float[] results = new float[1];
                Location.distanceBetween(
                        userCurrentLocation.getLatitude(),
                        userCurrentLocation.getLongitude(),
                        lat, lon,
                        results
                );

                String distanceStr = String.format("%.1f km", results[0] / 1000);

                String desc = "Services: " + speciality +
                        "\n\nContact: " + phone +
                        "\n\nSource: OpenStreetMap";

                boolean isVerified = !phone.equals("Not Available");

                // ✅ FINAL OBJECT
                fullList.add(new HospitalModel(
                        name,
                        distanceStr,
                        isVerified,
                        type,
                        desc,
                        R.drawable.map_image,
                        lat,
                        lon,
                        phone
                ));
            }

            runOnUiThread(() -> adapter.setFilteredList(fullList));

        } catch (Exception e) {
            Log.e("NearbyCare", "Parsing error", e);
        }
    }

    // ✅ FILTERS
    private void filterByCategory(String category) {
        List<HospitalModel> filteredList = new ArrayList<>();
        for (HospitalModel item : fullList)
            if (item.getType().equalsIgnoreCase(category))
                filteredList.add(item);

        adapter.setFilteredList(filteredList);
    }

    private void filterBy247() {
        List<HospitalModel> filteredList = new ArrayList<>();
        for (HospitalModel item : fullList)
            if (item.getName().toLowerCase().contains("hospital"))
                filteredList.add(item);

        adapter.setFilteredList(filteredList);
    }

    private void filterList(String text) {
        List<HospitalModel> filteredList = new ArrayList<>();
        for (HospitalModel item : fullList)
            if (item.getName().toLowerCase().contains(text.toLowerCase()))
                filteredList.add(item);

        adapter.setFilteredList(filteredList);
    }

    private void updateChipUI(TextView selectedChip) {
        chipAll.setBackgroundResource(R.drawable.bg_chip_inactive);
        chipGynac.setBackgroundResource(R.drawable.bg_chip_inactive);
        chipGeneral.setBackgroundResource(R.drawable.bg_chip_inactive);
        chip247.setBackgroundResource(R.drawable.bg_chip_inactive);

        selectedChip.setBackgroundResource(R.drawable.btn_gradient_signup);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}