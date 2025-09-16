package com.example.habitgame.repositories;

import com.example.habitgame.model.Alliance;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.FirebaseFirestore;

public class AllianceRepository {
    public static Task<String> insert(Alliance alliance) {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("alliances")
                .add(alliance)
                .addOnSuccessListener(documentReference -> {
                    String documentId = documentReference.getId();

                    alliance.setId(documentId);

                    db.collection("alliances")
                            .document(documentId)
                            .set(alliance)
                            .addOnSuccessListener(aVoid -> {
                                taskCompletionSource.setResult("Savez uspjesno dodat!");
                            })
                            .addOnFailureListener(e -> {
                                taskCompletionSource.setResult("Doslo je do greske prilikom dodavanja saveza!");
                            });

                })
                .addOnFailureListener(e -> {
                    taskCompletionSource.setResult("Doslo je do greske prilikom dodavanja saveza!");
                });
        return taskCompletionSource.getTask();
    }
}
