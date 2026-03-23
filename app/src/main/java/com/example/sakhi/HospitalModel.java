package com.example.sakhi;

public class HospitalModel {

    private String name;
    private String distance;
    private boolean isVerified;
    private String type;
    private String description;
    private int imageResId;

    // ✅ NEW FIELDS
    private double latitude;
    private double longitude;
    private String phone;

    // ✅ UPDATED CONSTRUCTOR (MATCHES YOUR ACTIVITY)
    public HospitalModel(String name, String distance, boolean isVerified,
                         String type, String description, int imageResId,
                         double latitude, double longitude, String phone) {

        this.name = name;
        this.distance = distance;
        this.isVerified = isVerified;
        this.type = type;
        this.description = description;
        this.imageResId = imageResId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
    }

    public String getName() { return name; }
    public String getDistance() { return distance; }
    public boolean isVerified() { return isVerified; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public int getImageResId() { return imageResId; }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getPhone() { return phone; }
}