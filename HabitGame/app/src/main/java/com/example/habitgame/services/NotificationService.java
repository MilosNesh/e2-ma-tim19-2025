package com.example.habitgame.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.habitgame.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Proveravamo da li poruka sadrži podatke
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            String inviteId = remoteMessage.getData().get("inviteId");
            String allianceId = remoteMessage.getData().get("allianceId");
            String text = remoteMessage.getData().get("body");
            String title = remoteMessage.getData().get("title");
            String senderEmail = remoteMessage.getData().get("senderEmail");

            showInvitationNotification(action, inviteId, allianceId, text, title, senderEmail);
        }

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            // Prikaz sistema notifikacije
            showNotification(title, body);
        }
    }

    private void showInvitationNotification(String action, String inviteId, String allianceId, String text, String title, String senderEmail) {
        Intent acceptIntent = new Intent(this, NotificationReceiver.class);
        acceptIntent.putExtra("action", "accept");
        acceptIntent.putExtra("inviteId", inviteId);
        acceptIntent.putExtra("allianceId", allianceId);
        acceptIntent.putExtra("senderEmail", senderEmail);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, 0, acceptIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent rejectIntent = new Intent(this, NotificationReceiver.class);
        rejectIntent.putExtra("action", "reject");
        rejectIntent.putExtra("inviteId", inviteId);
        rejectIntent.putExtra("allianceId", allianceId);
        rejectIntent.putExtra("senderEmail", senderEmail);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(this, 1, rejectIntent, PendingIntent.FLAG_IMMUTABLE);

        // Kreiranje kanala ako je API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "channel_id";
            CharSequence channelName = "Alliance Invites";
            String channelDescription = "Notifications for Alliance invites";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .addAction(R.drawable.ic_accept, "Prihvati", acceptPendingIntent)
                .addAction(R.drawable.ic_reject, "Odbij", rejectPendingIntent)
                .setAutoCancel(false)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    private void showNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "channel_id2";
            CharSequence channelName = "Alliance Invite accepted";
            String channelDescription = "Notifications for Alliance invite accepted";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id2")
//                .setContentTitle(title)
//                .setContentText(message)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setPriority(NotificationCompat.PRIORITY_HIGH);
//
//        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
//        manager.notify(1, builder.build());


        Notification notification = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }
}
