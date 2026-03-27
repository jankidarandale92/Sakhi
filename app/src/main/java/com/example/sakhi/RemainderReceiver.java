package com.example.sakhi;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class RemainderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. Retrieve the data passed from ReminderHelper
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        // We use the unique ID to ensure multiple notifications can show at once
        int notificationId = intent.getIntExtra("id", (int) System.currentTimeMillis());

        // 2. Setup the Notification Channel (For Android 8.0+)
        String channelId = "sakhi_reminders";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Sakhi Wellness Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for medicine, water, and health checks");
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // 3. Define the sound to play
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // 4. Build the Notification with "Ringing" properties
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notifications) // Ensure this is a flat, white icon
                .setContentTitle(title != null ? title : "Sakhi Reminder")
                .setContentText(message != null ? message : "It's time for your health task! 🌸")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(alarmSound) // 🔥 Makes it ring
                .setDefaults(NotificationCompat.DEFAULT_ALL) // 🔥 Enables default vibration/lights
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true);

        // 5. Trigger the Notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Check for POST_NOTIFICATIONS permission (Required for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted; usually handled in the Activity, so we exit here
                return;
            }
        }

        notificationManager.notify(notificationId, builder.build());
    }
}