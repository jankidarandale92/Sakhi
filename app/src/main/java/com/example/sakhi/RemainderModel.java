package com.example.sakhi;

public class RemainderModel {
    // 🔥 Added ID to uniquely identify each alarm in the system
    public int id;
    public String title;
    public String time;
    public String repeat;
    public boolean isActive;

    public RemainderModel(int id, String title, String time, String repeat, boolean isActive) {
        this.id = id;
        this.title = title;
        this.time = time;
        this.repeat = repeat;
        this.isActive = isActive;
    }
}