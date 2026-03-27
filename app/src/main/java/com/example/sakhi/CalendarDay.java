package com.example.sakhi;

import java.util.Date;

public class CalendarDay {
    public Date date;         // null for padding slots
    public boolean isToday;
    public boolean isPeriod;
    public boolean isOvulation;
    public boolean isFertile;

    // 🔥 NEW: Flag for the Luteal Phase (Preparation phase)
    public boolean isLuteal;

    public CalendarDay() {
        this.date = null;
        this.isToday = false;
        this.isPeriod = false;
        this.isOvulation = false;
        this.isFertile = false;
        this.isLuteal = false;
    }
}