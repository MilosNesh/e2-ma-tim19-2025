package com.example.habitgame.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.repositories.AccountRepository;
import com.example.habitgame.repositories.TaskRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskService {

    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;

    public TaskService() {}

    // --- pravilo 3 dana unazad ---
    public static boolean shouldBeMarkedMissed(@Nullable Long plannedTimeMs, long nowMs) {
        if (plannedTimeMs == null) return false;
        long limit = nowMs - (3 * ONE_DAY_MS);
        return plannedTimeMs < limit;
    }

    public com.google.android.gms.tasks.Task<Void> autoFlipOverdueToMissed(@NonNull Task t) {
        TaskStatus st = t.getStatus()==null? TaskStatus.AKTIVAN : t.getStatus();
        if (st == TaskStatus.URADJEN || st == TaskStatus.OTKAZAN || st == TaskStatus.NEURADJEN)
            return Tasks.forResult(null);

        Long refTime = t.getExecutionTime();
        if (shouldBeMarkedMissed(refTime, System.currentTimeMillis())) {
            Map<String, Object> up = new HashMap<>();
            up.put("status", TaskStatus.NEURADJEN.name());
            up.put("isCompleted", false);
            return TaskRepository.updateFields(t.getId(), up);
        }
        return Tasks.forResult(null);
    }

    // --- statusne akcije (one-time) ---
    public com.google.android.gms.tasks.Task<Void> markDone(@NonNull Task t) {
        TaskStatus cur = t.getStatus()==null? TaskStatus.AKTIVAN : t.getStatus();
        if (cur != TaskStatus.AKTIVAN) {
            return Tasks.forException(new IllegalStateException("Samo aktivan zadatak može biti označen kao urađen."));
        }
        Long refTime = t.getExecutionTime();
        if (shouldBeMarkedMissed(refTime, System.currentTimeMillis())) {
            Map<String, Object> upMiss = new HashMap<>();
            upMiss.put("status", TaskStatus.NEURADJEN.name());
            upMiss.put("isCompleted", false);
            return TaskRepository.updateFields(t.getId(), upMiss);
        }

        Map<String, Object> up = new HashMap<>();
        up.put("status", TaskStatus.URADJEN.name());
        up.put("isCompleted", true);
        up.put("lastCompletionTimestamp", System.currentTimeMillis());

        int xp = Math.max(0, t.getXpValue());
        return TaskRepository.updateFields(t.getId(), up)
                .onSuccessTask(a -> xp > 0
                        ? AccountRepository.incrementXpForCurrentUser(xp)
                        : Tasks.forResult(null));
    }

    public com.google.android.gms.tasks.Task<Void> markCanceled(@NonNull Task t) {
        TaskStatus cur = t.getStatus()==null? TaskStatus.AKTIVAN : t.getStatus();
        if (cur != TaskStatus.AKTIVAN) {
            return Tasks.forException(new IllegalStateException("Samo aktivan zadatak može biti označen kao otkazan."));
        }
        Map<String, Object> up = new HashMap<>();
        up.put("status", TaskStatus.OTKAZAN.name());
        up.put("isCompleted", false);
        return TaskRepository.updateFields(t.getId(), up);
    }

    // --- kreiranje (one-time) ---
    public com.google.android.gms.tasks.Task<DocumentReference> createTask(
            String name, String description, String categoryId,
            String weight, String importance, int xpValue, Long executionDate,
            boolean ignored_isRepeating, Long ignored_start, Long ignored_end,
            Integer ignored_interval, String ignored_unit) {

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "unknown_user";

        Task task = new Task(
                userId,
                name,
                description,
                categoryId,
                weight,
                importance,
                xpValue,
                executionDate,   // 00:00 tog dana
                false,           // uvijek one-time
                null, null, null, null,
                System.currentTimeMillis()
        );

        task.setStatus(TaskStatus.AKTIVAN);
        task.setIsCompleted(false);

        return TaskRepository.insert(task);
    }

    // --- fetch / delete ---
    public com.google.android.gms.tasks.Task<List<Task>> getTasksForCurrentUser() {
        return TaskRepository.getTasksForCurrentUser();
    }

    public com.google.android.gms.tasks.Task<Void> deleteTask(@NonNull String taskId) {
        return TaskRepository.delete(taskId);
    }
}
