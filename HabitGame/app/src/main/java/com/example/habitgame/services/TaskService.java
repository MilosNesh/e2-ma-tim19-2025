package com.example.habitgame.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.repositories.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
            return com.google.android.gms.tasks.Tasks.forException(new IllegalStateException("User not authenticated."));
        }

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
        task.setStatus(TaskStatus.AKTIVAN);
        return TaskRepository.insert(task);
    }

    public com.google.android.gms.tasks.Task<List<Task>> getTasksForCurrentUser() {
        return TaskRepository.getTasksForCurrentUser();
    }

    public com.google.android.gms.tasks.Task<Void> markDone(Task t){
        t.setStatus(TaskStatus.URADJEN);
        t.setIsCompleted(true);
        return TaskRepository.updateStatus(t.getId(), TaskStatus.URADJEN.name(), true);
    }

    public com.google.android.gms.tasks.Task<Void> markCanceled(Task t){
        t.setStatus(TaskStatus.OTKAZAN);
        t.setIsCompleted(false);
        return TaskRepository.updateStatus(t.getId(), TaskStatus.OTKAZAN.name(), false);
    }

    public com.google.android.gms.tasks.Task<Void> markPaused(Task t){
        t.setStatus(TaskStatus.PAUZIRAN);
        return TaskRepository.updateStatus(t.getId(), TaskStatus.PAUZIRAN.name(), false);
    }

    public com.google.android.gms.tasks.Task<Void> markActive(Task t){
        t.setStatus(TaskStatus.AKTIVAN);
        return TaskRepository.updateStatus(t.getId(), TaskStatus.AKTIVAN.name(), false);
    }

    public com.google.android.gms.tasks.Task<Void> deleteTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("taskId je prazan."));
        }
        return TaskRepository.delete(taskId);
    }

    /** ✅ Parcijalni update (preporučeno iz EditTaskFragment-a) */
    public com.google.android.gms.tasks.Task<Void> updateTaskPartial(String taskId, Map<String, Object> updates) {
        if (taskId == null || taskId.isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("taskId je prazan."));
        }
        if (updates == null || updates.isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forResult(null);
        }
        return TaskRepository.updateFields(taskId, updates);
    }

    public com.google.android.gms.tasks.Task<Void> deleteTaskOneTime(@NonNull com.example.habitgame.model.Task t) {
        if (t.getStatus() == com.example.habitgame.model.TaskStatus.URADJEN) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("Nije moguće obrisati završen zadatak."));
        }
        return com.example.habitgame.repositories.TaskRepository.delete(t.getId());
    }

    /**
     * Briše buduća pojavljivanja ponavljajućeg zadatka tako što setuje endDate na (cutOff - 1).
     * Ako je instanceTime == null, koristi "sada".
     */
    public com.google.android.gms.tasks.Task<Void> deleteTaskFutureOccurrences(
            @NonNull com.example.habitgame.model.Task t,
            @androidx.annotation.Nullable Long instanceTime
    ) {
        if (!t.getIsRepeating()) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("Zadatak nije ponavljajući."));
        }
        if (t.getStatus() == com.example.habitgame.model.TaskStatus.URADJEN) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("Nije moguće obrisati završen zadatak."));
        }

        long cutOff = (instanceTime != null) ? instanceTime : System.currentTimeMillis();
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("endDate", cutOff - 1); // prekini buduća pojavljivanja

        return com.example.habitgame.repositories.TaskRepository.updateFields(t.getId(), updates);
    }
}
