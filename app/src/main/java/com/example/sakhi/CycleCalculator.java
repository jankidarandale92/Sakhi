package com.example.sakhi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CycleCalculator {

    public static List<Date> getPeriodDays(Date start, int length) {
        List<Date> days = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTime(start);
        for (int i = 0; i < length; i++) {
            days.add(c.getTime());
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    public static Date getNextPeriod(Date last, int cycleLength) {
        Calendar c = Calendar.getInstance();
        c.setTime(last);
        c.add(Calendar.DAY_OF_MONTH, cycleLength);
        return c.getTime();
    }

    public static Date getOvulation(Date last, int cycleLength) {
        Calendar c = Calendar.getInstance();
        c.setTime(last);
        c.add(Calendar.DAY_OF_MONTH, cycleLength - 14);
        return c.getTime();
    }

    public static List<Date> getFertileWindow(Date ovulation) {
        List<Date> list = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTime(ovulation);
        c.add(Calendar.DAY_OF_MONTH, -4);
        for (int i = 0; i < 6; i++) {
            list.add(c.getTime());
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return list;
    }
}