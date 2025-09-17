package com.example.habitgame.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

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
}
