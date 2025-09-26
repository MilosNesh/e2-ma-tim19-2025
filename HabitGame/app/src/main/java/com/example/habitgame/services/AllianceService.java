package com.example.habitgame.services;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Account;
import com.example.habitgame.model.Alliance;
import com.example.habitgame.model.AllianceCallback;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.repositories.AccountRepository;
import com.example.habitgame.repositories.AllianceRepository;
import com.google.android.gms.tasks.OnSuccessListener;
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
    public void save(Alliance alliance, AllianceCallback callback) {
        AllianceRepository.insert(alliance).addOnSuccessListener(res -> {
                callback.onResult(alliance);
        }).addOnFailureListener(ex -> {
            callback.onResult(alliance);
        });
    }

    public void sendAllianceInvite(String allianceId, String allianceName, List<Account> accountList, String leaderEmail) {
        Log.i("AllianceInvite", "Veličina liste: " + accountList.size());

        for (Account account : accountList) {
            try{
                JSONObject notificationObject = new JSONObject();
                notificationObject.put("title", "Savez-"+allianceName);
                notificationObject.put("body", "Korisnik "+leaderEmail+" Vas poziva u savez "+allianceName+".");
                notificationObject.put("token", account.getFcmToken());
                notificationObject.put("allianceId", allianceId);
                notificationObject.put("senderEmail", leaderEmail);
                notificationObject.put("inviteId", account.getEmail());
                callApi(notificationObject, "send-invite");

            }catch (JSONException e){}

        }
    }

    public void sendAnswer(String token, String usename) {
        try {
            if(token == null || token.isEmpty())
                Log.i("Notification token", "Token je prazan");
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("title", "Prihvacen zahtjev");
            notificationObject.put("body", "Korisnik "+usename+" je prihvaio vas poziv u savez.");
            notificationObject.put("token", token);
            callApi(notificationObject, "send");
        } catch (JSONException e) {

        }
    }

    public static void callApi(JSONObject jsonObject, String endpoint){
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm-server-965j.onrender.com/"+endpoint;
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

     public void getById(String id, AllianceCallback callback) {
        AllianceRepository.getById(id).addOnSuccessListener(alliance -> {
            callback.onResult(alliance);
        });
     }

     public void deleteAlliance(String id, StringCallback callback) {
        AllianceRepository.delete(id).addOnSuccessListener(res -> {
            AccountRepository.getByAlliance(id).addOnSuccessListener(accountList -> {
                for(Account a: accountList){
                    AccountRepository.updateAlliance(a.getEmail(), "");
                }
                callback.onResult("Savez uspjesno obrisan.");
            });
        }).addOnFailureListener(res -> {
            callback.onResult("Dodlo je do greske prilikom brisanja saveza.");
        });
     }

}
