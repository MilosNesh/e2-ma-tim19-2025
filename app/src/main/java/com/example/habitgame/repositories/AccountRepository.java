package com.example.habitgame.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Account;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class AccountRepository {
    public static void initDB(){

    }

    public static void insert(Account account){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .add(account)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("REZ_DB", "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("REZ_DB", "Error adding document", e);
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
                        taskCompletionSource.setException(new Exception("Nema korisnika sa tim emailom."));
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
}
