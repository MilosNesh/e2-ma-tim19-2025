package com.example.habitgame.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Task;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRepository {

    private static final String TAG = "TaskRepository";

    public static com.google.android.gms.tasks.Task<DocumentReference> insert(Task task) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        TaskCompletionSource<DocumentReference> tcs = new TaskCompletionSource<>();

        // Kreiraj unapred ID da bi task.setId imao smisla
        DocumentReference docRef = db.collection("tasks").document();
        task.setId(docRef.getId());

        docRef.set(task)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task added with ID: " + docRef.getId());
                    tcs.setResult(docRef);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding task", e);
                    tcs.setException(e);
                });

        return tcs.getTask();
    }

    public static com.google.android.gms.tasks.Task<List<Task>> getTasksForCurrentUser() {
        TaskCompletionSource<List<Task>> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e(TAG, "User is not logged in. Cannot fetch tasks.");
            tcs.setResult(new ArrayList<>());
            return tcs.getTask();
        }

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Task> taskList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Task t = document.toObject(Task.class);
                                t.setId(document.getId());
                                taskList.add(t);
                            }
                            tcs.setResult(taskList);
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                            tcs.setException(task.getException() != null
                                    ? task.getException()
                                    : new RuntimeException("Unknown Firestore error"));
                        }
                    }
                });
        return tcs.getTask();
    }

    /**
     * ⚠️ Ne koristi za formu za izmenu jer prepisuje ceo dokument.
     * Zadržavam zbog kompatibilnosti, ali preferiraj updateFields().
     */
    public static com.google.android.gms.tasks.Task<Void> update(Task task) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (task.getId() == null) {
            Log.e(TAG, "Cannot update Task: ID is null.");
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("Task ID cannot be null for update."));
        }

        return db.collection("tasks").document(task.getId())
                .set(task)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task successfully overwritten: " + task.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating Task: " + task.getId(), e);
                    throw new RuntimeException("DB update failed.", e);
                });
    }

    /**
     * ✅ Preporučeno za edit: parcijalni update samo promenjenih polja.
     */
    public static com.google.android.gms.tasks.Task<Void> updateFields(
            @NonNull String taskId,
            @NonNull Map<String, Object> updates
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("tasks").document(taskId).update(updates);
    }

    public static com.google.android.gms.tasks.Task<Void> updateStatus(
            @NonNull String taskId,
            @NonNull String status,
            boolean isCompleted
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("isCompleted", isCompleted);
        return db.collection("tasks").document(taskId).update(updates);
    }

    public static com.google.android.gms.tasks.Task<Void> updateCompletionStatus(String taskId, boolean isCompleted) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection("tasks").document(taskId)
                .update("isCompleted", isCompleted)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Completion status updated for: " + taskId))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating completion status: " + taskId, e));
    }

    public static com.google.android.gms.tasks.Task<Void> delete(String taskId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("tasks").document(taskId).delete();
    }
}
