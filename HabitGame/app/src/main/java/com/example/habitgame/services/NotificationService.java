package com.example.habitgame.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.habitgame.MainActivity;
import com.example.habitgame.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Ako je poruka sa podacima
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String actionId = remoteMessage.getData().get("actionId");  // Moguće da šalješ ID poziva za koji treba da prihvatiš/odbiješ

            // Kreiraj i prikaži notifikaciju
            sendNotification(title, body, actionId);
        }
    }

    private void sendNotification(String title, String body, String actionId) {
        // Kreiraj Intent za prihvatanje
        Intent acceptIntent = new Intent(this, MainActivity.class);  // Aktivnost koja će obraditi odgovor
        acceptIntent.putExtra("action", "accept");
        acceptIntent.putExtra("actionId", actionId);  // Dodaj ID poziva za koji korisnik treba da odgovori
        PendingIntent acceptPendingIntent = PendingIntent.getActivity(this, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Kreiraj Intent za odbijanje
        Intent rejectIntent = new Intent(this, MainActivity.class);
        rejectIntent.putExtra("action", "reject");
        rejectIntent.putExtra("actionId", actionId);  // Dodaj ID poziva za koji korisnik treba da odgovori
        PendingIntent rejectPendingIntent = PendingIntent.getActivity(this, 0, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Kreiraj notifikaciju sa akcijama
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "default")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_notification)  // Postavi odgovarajući resurs ikone
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_add_person, "Prihvati", acceptPendingIntent)
                .addAction(R.drawable.gloves, "Odbij", rejectPendingIntent);

        // Prikazivanje notifikacije
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }
}
