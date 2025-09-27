package com.example.habitgame.repositories;

import android.util.Log;

import com.example.habitgame.model.Message;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static Task<Message> save(Message message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        TaskCompletionSource<Message> taskCompletionSource = new TaskCompletionSource<>();

        db.collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    String id = documentReference.getId();
                    message.setId(id);

                    db.collection("messages")
                            .document(id)
                            .set(message)
                            .addOnSuccessListener(runnable -> {
                                taskCompletionSource.setResult(message);
                            })
                            .addOnFailureListener(runnable -> {
                                taskCompletionSource.setResult(null);
                            });
                })
                .addOnFailureListener(e -> {
                    taskCompletionSource.setResult(null);
                });
        return taskCompletionSource.getTask();
    }

    public static Task<List<Message>> getAllByAlliance(String allianceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        TaskCompletionSource<List<Message>> taskCompletionSource = new TaskCompletionSource<>();

        db.collection("messages")
                .whereEqualTo("allianceId", allianceId)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Message> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Message message = document.toObject(Message.class);
                            messages.add(message);
                        }
                        taskCompletionSource.setResult(messages);
                    } else {
                        taskCompletionSource.setResult(null);
                    }
                });

        return taskCompletionSource.getTask();
    }

    public static void deleteByAlliance(String allianceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("messages")
                .whereEqualTo("allianceId", allianceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete();
                        }
                    }
                });
        }

}
