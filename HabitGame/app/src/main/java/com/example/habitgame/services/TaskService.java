package com.example.habitgame.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.repositories.AccountRepository;
import com.example.habitgame.repositories.TaskRepository;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servis za zadatke:
 * - pravilo "3 dana unazad" za jednokratne
 * - markiranje statusa
 * - XP:
 *      • jednokratni: puni XP
 *      • ponavljajući: share = (total XP grupe) / (broj AKTIVNIH instanci u grupi)
 *        grupa je po difoltu definisana po imenu zadatka
 */
public class TaskService {

    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;

    public TaskService() {}

    // ---------------- PRAVILO 3 DANA ----------------

    public static boolean shouldBeMarkedMissed(@Nullable Long plannedTimeMs, long nowMs) {
        if (plannedTimeMs == null) return false;
        long limit = nowMs - (3 * ONE_DAY_MS);
        return plannedTimeMs < limit;
    }

    public com.google.android.gms.tasks.Task<Void> autoFlipOverdueToMissed(@NonNull Task t) {
        TaskStatus st = (t.getStatus() == null) ? TaskStatus.AKTIVAN : t.getStatus();
        if (st == TaskStatus.URADJEN || st == TaskStatus.OTKAZAN || st == TaskStatus.NEURADJEN)
            return Tasks.forResult(null);

        // pravilo 3 dana primeni samo na jednokratne (instancirane ponavljajuće tretiramo posebno)
        if (isRepeatingLike(t)) return Tasks.forResult(null);

        Long refTime = (t.getExecutionTime() != null) ? t.getExecutionTime() : t.getStartDate();
        if (shouldBeMarkedMissed(refTime, System.currentTimeMillis())) {
            Map<String, Object> up = new HashMap<>();
            up.put("status", TaskStatus.NEURADJEN.name());
            up.put("isCompleted", false);
            return TaskRepository.updateFields(t.getId(), up);
        }
        return Tasks.forResult(null);
    }

    // ---------------- STATUSNE AKCIJE ----------------

    public com.google.android.gms.tasks.Task<Void> markDone(@NonNull Task t) {
        TaskStatus cur = (t.getStatus() == null) ? TaskStatus.AKTIVAN : t.getStatus();
        if (cur != TaskStatus.AKTIVAN) {
            return Tasks.forException(new IllegalStateException("Samo aktivan zadatak može biti označen kao urađen."));
        }

        final long now = System.currentTimeMillis();

        if (isRepeatingLike(t)) {
            // ===== PONAVLJAJUĆE INSTANSE =====
            // Grupisanje po imenu zadatka (po potrebi promeni na groupId kada ga dodaš u Task model)
            final String groupKey = nullSafe(t.getName()).trim();

            return TaskRepository.getTasksForCurrentUser()
                    .onSuccessTask(list -> {
                        List<Task> groupActive = new ArrayList<>();
                        int totalGroupXp = 0;

                        for (Task it : safeList(list)) {
                            if (it == null) continue;
                            if (!isRepeatingLike(it)) continue;
                            if (!nullSafe(it.getName()).trim().equals(groupKey)) continue;

                            TaskStatus st = (it.getStatus() == null) ? TaskStatus.AKTIVAN : it.getStatus();
                            if (st == TaskStatus.AKTIVAN) {
                                groupActive.add(it);
                                totalGroupXp += Math.max(0, it.getXpValue());
                            }
                        }

                        // Ako su sve instance imale "ukupan XP" kopiran u svaku,
                        // tada je totalGroupXp = N * TOTAL. Ako želiš da TOTAL bude upravo vrednost sa kliknute kartice,
                        // zameni totalGroupXp -> Math.max(0, t.getXpValue()).
                        int n = Math.max(1, groupActive.size());
                        int share = Math.max(0, totalGroupXp / n);

                        Map<String, Object> up = new HashMap<>();
                        up.put("status", TaskStatus.URADJEN.name()); // ova kartica ide u URADJEN
                        up.put("isCompleted", true);
                        up.put("lastCompletionTimestamp", now);

                        return TaskRepository.updateFields(t.getId(), up)
                                .onSuccessTask(a -> share > 0
                                        ? AccountRepository.incrementXpForCurrentUser(share)
                                        : Tasks.forResult(null));
                    });

        } else {
            // ===== JEDNOKRATNI =====
            Long refTime = (t.getExecutionTime() != null) ? t.getExecutionTime() : t.getStartDate();
            if (shouldBeMarkedMissed(refTime, now)) {
                Map<String, Object> upMiss = new HashMap<>();
                upMiss.put("status", TaskStatus.NEURADJEN.name());
                upMiss.put("isCompleted", false);
                return TaskRepository.updateFields(t.getId(), upMiss);
            }

            Map<String, Object> up = new HashMap<>();
            up.put("status", TaskStatus.URADJEN.name());
            up.put("isCompleted", true);
            up.put("lastCompletionTimestamp", now);

            int xp = Math.max(0, t.getXpValue());
            return TaskRepository.updateFields(t.getId(), up)
                    .onSuccessTask(a -> xp > 0
                            ? AccountRepository.incrementXpForCurrentUser(xp)
                            : Tasks.forResult(null));
        }
    }

    public com.google.android.gms.tasks.Task<Void> markCanceled(@NonNull Task t) {
        TaskStatus cur = (t.getStatus() == null) ? TaskStatus.AKTIVAN : t.getStatus();
        if (cur != TaskStatus.AKTIVAN) {
            return Tasks.forException(new IllegalStateException("Samo aktivan zadatak može biti označen kao otkazan."));
        }

        if (isRepeatingLike(t)) {
            // Za ponavljajuće “otkazivanje” pojedinačne kartice nema smisla – koristi URADJEN ili PAUZA za celu seriju
            return Tasks.forException(new IllegalStateException("Ponavljajući zadatak pauziraj (ili označi instancu kao urađenu)."));
        }

        Map<String, Object> up = new HashMap<>();
        up.put("status", TaskStatus.OTKAZAN.name());
        up.put("isCompleted", false);
        return TaskRepository.updateFields(t.getId(), up);
    }

    public com.google.android.gms.tasks.Task<Void> markPaused(@NonNull Task t) {
        TaskStatus cur = (t.getStatus() == null) ? TaskStatus.AKTIVAN : t.getStatus();
        if (cur != TaskStatus.AKTIVAN || !Boolean.TRUE.equals(t.getIsRepeating())) {
            return Tasks.forException(new IllegalStateException("Pauza je moguća samo za aktivne ponavljajuće zadatke."));
        }
        Map<String, Object> up = new HashMap<>();
        up.put("status", TaskStatus.PAUZIRAN.name());
        return TaskRepository.updateFields(t.getId(), up);
    }

    public com.google.android.gms.tasks.Task<Void> markActive(@NonNull Task t) {
        TaskStatus cur = (t.getStatus() == null) ? TaskStatus.PAUZIRAN : t.getStatus();
        if (cur != TaskStatus.PAUZIRAN) {
            return Tasks.forException(new IllegalStateException("Samo pauziran zadatak može da se aktivira."));
        }
        Map<String, Object> up = new HashMap<>();
        up.put("status", TaskStatus.AKTIVAN.name());
        return TaskRepository.updateFields(t.getId(), up);
    }

    // ---------------- KREIRANJE ----------------

    public com.google.android.gms.tasks.Task<DocumentReference> createTask(
            String name, String description, String categoryId,
            String weight, String importance, int xpValue, Long executionDate,
            boolean isRepeating, Long startDate, Long endDate,
            Integer repeatInterval, String repeatUnit) {

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
                executionDate,      // jednokratni: datum (bez vremena)
                isRepeating,
                startDate,          // ponavljajući: početak
                endDate,            // ponavljajući: kraj (opciono)
                repeatInterval,
                repeatUnit,
                System.currentTimeMillis() // creationTimestamp
        );

        task.setStatus(TaskStatus.AKTIVAN);
        task.setIsCompleted(false);

        return TaskRepository.insert(task);
    }

    // ---------------- PREUZIMANJE / BRISANJE ----------------

    public com.google.android.gms.tasks.Task<List<Task>> getTasksForCurrentUser() {
        return TaskRepository.getTasksForCurrentUser();
    }

    public com.google.android.gms.tasks.Task<Void> deleteTask(@NonNull String taskId) {
        return TaskRepository.delete(taskId);
    }

    public com.google.android.gms.tasks.Task<Void> deleteTaskFutureOccurrences(@NonNull Task t, @Nullable Long instanceTime) {
        if (t.getId() == null) return Tasks.forResult(null);
        return TaskRepository.delete(t.getId());
    }

    public com.google.android.gms.tasks.Task<Void> markIfOverdue(@NonNull Task t) {
        return autoFlipOverdueToMissed(t);
    }

    // ---------------- HELPERI ----------------

    private static boolean isRepeatingLike(@NonNull Task t) {
        if (Boolean.TRUE.equals(t.getIsRepeating())) return true;
        Integer ri = t.getRepeatInterval();
        String ru = t.getRepeatUnit();
        return ri != null && ri > 0 && ru != null && ru.trim().length() > 0;
    }

    private static <E> List<E> safeList(List<E> in) {
        return (in == null) ? new ArrayList<>() : in;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
