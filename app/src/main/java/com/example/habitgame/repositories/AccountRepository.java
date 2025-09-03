package com.example.habitgame.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Account;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class AccountRepository {
    public static void initDB(){

    }

    public static void insert(Account account){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Kreiraj korisnika putem Firebase Authentication
        mAuth.createUserWithEmailAndPassword(account.getEmail(), account.getPassword())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser(); // Sada je korisnik autentifikovan

                        if (user != null && !user.isEmailVerified()) {
                            // Pošaljite verifikacioni email
                            user.sendEmailVerification()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("EMAIL", "Verifikacioni email poslat.");

                                        // Sada dodaj korisnika u Firestore nakon uspešnog slanja emaila
                                        account.setIsVerified(false); // inicijalno nije verifikovan
                                        account.setRegistrationTimestamp(System.currentTimeMillis()); // trenutno vreme

                                        db.collection("accounts")
                                                .add(account)
                                                .addOnSuccessListener(documentReference -> {
                                                    Log.d("REZ_DB", "DocumentSnapshot added with ID: " + documentReference.getId());
                                                })
                                                .addOnFailureListener(e -> Log.w("REZ_DB", "Error adding document", e));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("EMAIL", "Greška prilikom slanja emaila.", e);
                                    });
                        }
                    } else {
                        // Greška prilikom kreiranja korisnika
                        Log.e("AUTH", "Greška prilikom kreiranja korisnika", task.getException());
                    }
                });
    }

    public static void select(){
        Log.i("DB", "select");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("REZ_DB", document.getId() + " => " + document.getData());
                            }
                        } else {
                            Log.w("REZ_DB", "Error getting documents.", task.getException());
                        }
                    }
                });

    }

    public Task<Account> selectByUsername(String username){
        TaskCompletionSource<Account> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                    Account account = document.toObject(Account.class);
                    taskCompletionSource.setResult(account);
                } else {
                    Log.d("NoUserFound", "Nema korisnika sa tim korisničkim imenom.");
                    taskCompletionSource.setException(new Exception("Nema korisnika sa tim korisničkim imenom."));
                }
            })
            .addOnFailureListener(e -> {
                Log.e("FirebaseError", "Greška prilikom pretrage korisnika: ", e);
                taskCompletionSource.setException(e);
            });
        return taskCompletionSource.getTask();
    }

    public Task<Account> selectByEmail(String email){
        TaskCompletionSource<Account> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        Account account = document.toObject(Account.class);
                        taskCompletionSource.setResult(account);
                    } else {
                        Log.d("NoUserFound", "Nema korisnika sa tim emailom");
                        taskCompletionSource.trySetResult(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Greška prilikom pretrage korisnika: ", e);
                    taskCompletionSource.setException(e);
                });
        return taskCompletionSource.getTask();
    }

    public static void deleteAll(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d("REZ_DB", "Account " + document.getId() + " has been deleted."))
                                .addOnFailureListener(e -> Log.w("REZ_DB", "Error deleting account.", e));
                    }
                })
                .addOnFailureListener(e -> Log.w("REZ_DB", "Error fetching accounts.", e));

    }

    public static void update(Account account){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("email", account.getEmail())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DocumentReference docRef = doc.getReference();

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("password", account.getPassword());
                        updates.put("equipments", account.getEquipments());
                        updates.put("coins", account.getCoins());
                        docRef.update(updates)
                                .addOnSuccessListener(aVoid ->
                                        Log.d("REZ_DB", "Successfully updated user: " + account.getEmail()))
                                .addOnFailureListener(e ->
                                        Log.w("REZ_DB", "Error updating user: " + account.getEmail(), e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("REZ_DB", "Error querying username: " + account.getEmail(), e);
                });

    }

    public static void updateIsVerified(String email, boolean verified) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update("isVerified", verified)
                                .addOnSuccessListener(aVoid -> Log.d("REZ_DB", "isVerified updated"))
                                .addOnFailureListener(e -> Log.e("REZ_DB", "Failed to update isVerified", e));
                    }
                });
    }

    public static void deleteByEmail(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d("REZ_DB", "Account deleted due to expired link."))
                                .addOnFailureListener(e -> Log.e("REZ_DB", "Failed to delete expired account.", e));
                    }
                });
    }

}
