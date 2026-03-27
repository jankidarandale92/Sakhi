package com.example.sakhi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class ReminderHelper {

    public static void setAlarm(Context context, RemainderModel model) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RemainderReceiver.class);
        intent.putExtra("title", "Sakhi Reminder: " + model.title);
        intent.putExtra("message", "It's time for your scheduled health task!");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, model.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Parse time (Expected format "10:30 AM")
        String[] parts = model.time.split("[: ]");
        int hour = Integer.parseInt(parts[0]);
        int min = Integer.parseInt(parts[1]);
        if (parts[2].equalsIgnoreCase("PM") && hour < 12) hour += 12;
        if (parts[2].equalsIgnoreCase("AM") && hour == 12) hour = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1); // Set for tomorrow if time already passed
        }

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(), pendingIntent);
        }
    }

    public static void cancelAlarm(Context context, int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RemainderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}