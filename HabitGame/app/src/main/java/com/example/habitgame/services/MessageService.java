package com.example.habitgame.services;

import android.util.Log;

import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountListCallback;
import com.example.habitgame.model.Message;
import com.example.habitgame.model.MessageCallback;
import com.example.habitgame.model.MessageListCallback;
import com.example.habitgame.repositories.MessageRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MessageService {

    public void save(Message message, MessageCallback callback) {
        MessageRepository.save(message).addOnSuccessListener(message1 -> {
            AccountService accountService = new AccountService();
            accountService.getByAlliance(message.getAllianceId(), new AccountListCallback() {
                @Override
                public void onResult(List<Account> accountList) {
                    for (Account a : accountList) {
                        if(!a.getEmail().equals(message.getAuthorEmail()))
                            sendNotifications(a.getFcmToken(), message.getAuthorUsername());
                    }
                }
            });
            callback.onResult(message1);
        }).addOnFailureListener(e -> {
            callback.onResult(null);
        });
    }

    public void getAllByAlliance(String allianceId, MessageListCallback callback) {
        MessageRepository.getAllByAlliance(allianceId).addOnSuccessListener(messageList -> {
            callback.onResult(messageList);

        }).addOnFailureListener(e -> {
            callback.onResult(null);
        });
    }

    private void sendNotifications(String token, String senderUsername){
        try {
            if(token == null || token.isEmpty())
                Log.i("Notification token", "Token je prazan");
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("title", "Nova poruka");
            notificationObject.put("body", "Dobili ste novu poruku korisnika "+senderUsername+ ".");
            notificationObject.put("token", token);
            AllianceService.callApi(notificationObject, "send");
        } catch (JSONException e) {

        }
    }

}
