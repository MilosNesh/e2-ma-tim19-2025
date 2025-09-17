package com.example.habitgame.services;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Account;
import com.example.habitgame.model.Alliance;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.repositories.AllianceRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AllianceService {
    public void save(Alliance alliance, StringCallback callback) {
        AllianceRepository.insert(alliance).addOnSuccessListener(res -> {
                callback.onResult("Savez uspjesno dodat!");
        }).addOnFailureListener(ex -> {
            callback.onResult("Doslo je do greske prilikom dodavalja saveza!");
        });
    }

    public void sendAllianceInvite(String allianceName, List<Account> accountList, String leaderEmail) throws JSONException {
        Log.i("AllianceInvite", "Veličina liste: " + accountList.size());

        for (Account account : accountList) {
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("title", leaderEmail);
            notificationObject.put("body", "Pozvani ste u savez "+allianceName);
            notificationObject.put("token", account.getFcmToken());
            callApi(notificationObject);
        }
    }

    public void callApi(JSONObject jsonObject){
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm-server-965j.onrender.com/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.i("Notifikacija","Nije poslato");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.i("Notifikacija","Poslato valjda");
                response.close();
            }
        });
     }
}
