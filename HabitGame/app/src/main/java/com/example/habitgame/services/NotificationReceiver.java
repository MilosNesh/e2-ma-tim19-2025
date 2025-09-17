package com.example.habitgame.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
        handleAction(inviteId, allianceId, action, senderEmail);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }

    private void handleAction(String inviteId, String allianceId, String action, String senderEmail) {
        if(action.equals("reject")) {
            Log.i("savez notifikacija", "odbijena");
            return;
        }
        AccountService accountService = new AccountService();
        if(action.equals("accept")) {
            Log.i("savez notifikacija", "prihvacena");
            AccountService.updateAlliance(inviteId, allianceId, new AccountCallback() {
                @Override
                public void onResult(Account account) {
                    accountService.getAccountByEmail(senderEmail, new AccountCallback() {
                        @Override
                        public void onResult(Account sender) {
                            AllianceService allianceService = new AllianceService();
                            allianceService.sendAnswer(sender.getFcmToken(), account.getUsername());
//                            NavController navController = Navigation.findNavController(context, R.id.mainContainer);
//                            navController.navigate(R.id.allianceFragment);
                        }
                    });
                }
            });
        }

    }

}

