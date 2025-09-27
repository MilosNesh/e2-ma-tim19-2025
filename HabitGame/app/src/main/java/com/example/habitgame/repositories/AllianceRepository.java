package com.example.habitgame.repositories;

import com.example.habitgame.model.Alliance;
import com.example.habitgame.model.AllianceCallback;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class AllianceRepository {
    public static Task<Alliance> insert(Alliance alliance) {
        TaskCompletionSource<Alliance> taskCompletionSource = new TaskCompletionSource<>();
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
                                taskCompletionSource.setResult(alliance);
                            })
                            .addOnFailureListener(e -> {
                                taskCompletionSource.setResult(null);
                            });

                })
                .addOnFailureListener(e -> {
                    taskCompletionSource.setResult(null);
                });

        return taskCompletionSource.getTask();
    }


    public static Task<Alliance> getById(String id) {
        TaskCompletionSource<Alliance> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        db.collection("alliances")
                .document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Alliance alliance = documentSnapshot.toObject(Alliance.class);
                        taskCompletionSource.setResult(alliance);
                    } else {
                        taskCompletionSource.setResult(null);
                    }
                })
                .addOnFailureListener(e -> {
                    taskCompletionSource.setResult(null);
                });

        return taskCompletionSource.getTask();
    }

    public static Task<String> delete(String id) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

        db.collection("alliances")
                .document(id)
                .delete()
                .addOnSuccessListener(runnable -> {
                    taskCompletionSource.setResult("");
                }).addOnFailureListener(runnable -> {
                    taskCompletionSource.setResult("Error");
                });
        return taskCompletionSource.getTask();
    }

    public static Task<Alliance> getByLeader(String leaderEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        TaskCompletionSource<Alliance> taskCompletionSource = new TaskCompletionSource<>();

        db.collection("alliances")
                .whereEqualTo("leader", leaderEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if(!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        Alliance alliance = document.toObject(Alliance.class);
                        taskCompletionSource.setResult(alliance);
                    }
                    else{
                        taskCompletionSource.setResult(null);
                    }
                }).addOnFailureListener(runnable -> {
                    taskCompletionSource.setResult(null);
                });
       return taskCompletionSource.getTask();
    }
}
