package uk.co.turkltd.locationapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class LocationService extends Service {

    //Objects
    Intent nIntent;
    PendingIntent pendingIntent;
    Notification notification;
    NotificationChannel notificationChannel;
    NotificationManager notificationManager;

    //Variables
    String channel_ID = "Location Service Channel";
    String input = "";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    //Create a notification channel to run allowing the application to be run in the background
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        nIntent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this,
                0, nIntent, 0);
        notification = new NotificationCompat.Builder(this, channel_ID)
                .setContentTitle("Location Service")
                .setContentText(input)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        notificationChannel = new NotificationChannel(
                channel_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager = getSystemService(NotificationManager.class);
        assert notificationManager != null;
        notificationManager.createNotificationChannel(notificationChannel);
    }
}

