package com.example.habitgame.services;

import com.example.habitgame.model.Task;
import com.example.habitgame.repositories.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import android.util.Log;

import java.util.Date;

public class TaskService {

    private static final String TAG = "TaskService";
    public com.google.android.gms.tasks.Task<DocumentReference> createTask(
            String name, String description, String categoryId,
            String weight, String importance, int xpValue, Long executionTime,
            boolean isRepeating, Long startDate, Long endDate,
            Integer repeatInterval, String repeatUnit) {

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e(TAG, "User not authenticated. Cannot create task.");
            // Vrati neuspešan Task
            return com.google.android.gms.tasks.Tasks.forException(new IllegalStateException("User not authenticated."));
        }

        // Kreiranje Task objekta
        Task task = new Task(
                userId,
                name,
                description,
                categoryId,
                weight,
                importance,
                xpValue,
                executionTime,
                isRepeating,
                startDate,
                endDate,
                repeatInterval,
                repeatUnit,
                new Date().getTime()
        );
        return TaskRepository.insert(task);
    }

}