package com.example.habitgame.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    public static com.google.android.gms.tasks.Task<DocumentReference> insert(Task task) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        TaskCompletionSource<DocumentReference> tcs = new TaskCompletionSource<>();

        DocumentReference docRef = db.collection("tasks").document();
        task.setId(docRef.getId());

        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    task.setId(documentReference.getId());
                    Log.d("TaskRepository", "Task added with ID: " + documentReference.getId());
                    tcs.setResult(documentReference);
                })
                .addOnFailureListener(e -> {
                    Log.w("TaskRepository", "Error adding task", e);
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
            Log.e("TaskRepository", "User is not logged in. Cannot fetch tasks.");
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
                            Log.w("TaskRepository", "Error getting documents.", task.getException());
                            tcs.setResult(null);
                        }
                    }
                });
        return tcs.getTask();
    }

    public static com.google.android.gms.tasks.Task<Void> update(Task task) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (task.getId() == null) {
            Log.e("TaskRepository", "Cannot update Task: ID is null.");
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("Task ID cannot be null for update."));
        }

        return db.collection("tasks").document(task.getId())
                .set(task)
                .addOnSuccessListener(aVoid -> Log.d("TaskRepository", "Task successfully updated: " + task.getId()))
                .addOnFailureListener(e -> {
                    Log.e("TaskRepository", "Error updating Task: " + task.getId(), e);
                    // Baca se izuzetak koji se može uhvatiti u TaskCompletionService
                    throw new RuntimeException("DB update failed.", e);
                });
    }

    public static com.google.android.gms.tasks.Task<Void> updateCompletionStatus(String taskId, boolean isCompleted) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection("tasks").document(taskId)
                .update("isCompleted", isCompleted)
                .addOnSuccessListener(aVoid -> Log.d("TaskRepository", "Completion status updated for: " + taskId))
                .addOnFailureListener(e -> Log.e("TaskRepository", "Error updating completion status: " + taskId, e));
    }
}