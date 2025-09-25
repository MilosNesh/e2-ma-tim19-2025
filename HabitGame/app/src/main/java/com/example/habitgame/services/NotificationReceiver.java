package com.example.habitgame.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.habitgame.R;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        String inviteId = intent.getStringExtra("inviteId");
        String allianceId = intent.getStringExtra("allianceId");
        String senderEmail = intent.getStringExtra("senderEmail");
        Log.i("akcija", action);
        handleAction(inviteId, allianceId, action, senderEmail, context);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }

    private void handleAction(String inviteId, String allianceId, String action, String senderEmail, Context context) {
        if(action.equals("reject")) {
            Log.i("savez notifikacija", "odbijena");
            return;
        }
        if(action.equals("accept")) {
            AccountService accountService = new AccountService();
            AllianceService allianceService = new AllianceService();
            Log.i("savez notifikacija", "prihvacena");
            Log.i("Id saveza", allianceId);
            accountService.getAccountByEmail(senderEmail, new AccountCallback() {
                @Override
                public void onResult(Account sender) {

                    AccountService.updateAlliance(inviteId, sender.getAllianceId(), new AccountCallback() {
                        @Override
                        public void onResult(Account account) {
                            Log.i("email i token", senderEmail+ "   "+ sender.getFcmToken());
                            allianceService.sendAnswer(sender.getFcmToken(), account.getUsername());
                            saveAllianceIdToSharedPreferences(context, inviteId);
                        }
                    });
                }
            });
        }

    }

    private void saveAllianceIdToSharedPreferences(Context context, String allianceId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("HabitGamePrefs", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("allianceId", allianceId);

        editor.apply();

        Log.i("SharedPreferences", "Alliance ID saved: " + allianceId);
    }

}

