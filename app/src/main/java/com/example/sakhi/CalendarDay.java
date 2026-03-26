package com.example.sakhi;

import java.util.Date;

public class CalendarDay {
    public Date date;         // null for padding slots
    public boolean isToday;
    public boolean isPeriod;
    public boolean isOvulation;
    public boolean isFertile;

    public CalendarDay() {
        this.date = null;
    }
}