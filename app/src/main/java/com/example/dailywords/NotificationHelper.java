package com.example.dailywords;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    private static final String CHANNEL_ID = "daily_word_channel";
    private static final int NOTIF_ID = 1001;

    public static void showWordRefreshedNotification(Context context) {
        // Create channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Daily Word Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifies when a new word of the day is available");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Intent to open MainActivity when clicked
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your own icon
                .setContentTitle("Word of the Day Refreshed")
                .setContentText("Your word of the day has refreshed, check it out")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.notify(NOTIF_ID, builder.build());
    }
}
