package com.example.habitgame.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.habitgame.MainActivity;
import com.example.habitgame.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            String text = remoteMessage.getData().get("body");
            String title = remoteMessage.getData().get("title");

            if (action == null || text == null || title == null) {
                Log.e("Notification Error", "Missing data in the notification.");
                return;
            }
            if(action.equals("none"))
                showNotification(title, text);
            else{
                String inviteId = remoteMessage.getData().get("inviteId");
                String allianceId = remoteMessage.getData().get("allianceId");
                String senderEmail = remoteMessage.getData().get("senderEmail");
                showInvitationNotification(action, inviteId, allianceId, text, title, senderEmail);
            }
        }

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            showNotification(title, body);
        }
    }

    private void showInvitationNotification(String action, String inviteId, String allianceId, String text, String title, String senderEmail) {
        PendingIntent acceptPendingIntent = createPendingIntent("accept", inviteId, allianceId, senderEmail);
        PendingIntent rejectPendingIntent = createPendingIntent("reject", inviteId, allianceId, senderEmail);

        // Kreiranje kanala ako je API 26+
        createNotificationChannel("channel_id", "Alliance Invites", "Notifications for Alliance invites");

        Notification notification = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .addAction(R.drawable.ic_accept, "Prihvati", acceptPendingIntent)
                .addAction(R.drawable.ic_reject, "Odbij", rejectPendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(inviteId.hashCode(), notification);
    }

    private PendingIntent createPendingIntent(String action, String inviteId, String allianceId, String senderEmail) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("action", action);
        intent.putExtra("inviteId", inviteId);
        intent.putExtra("allianceId", allianceId);
        intent.putExtra("senderEmail", senderEmail);
        return PendingIntent.getBroadcast(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void createNotificationChannel(String channelId, String channelName, String channelDescription) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(channelDescription);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String title, String message) {
        createNotificationChannel("channel_id2", "Alliance Invites", "Notifications for Alliance invites");

        Intent intent = new Intent(this, MainActivity.class);
        if(title.contains("poruka"))
            intent.putExtra("navigateTo", "messagesFragment");
        else
            intent.putExtra("navigateTo", "allianceFragment");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, "channel_id2")
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }
}
