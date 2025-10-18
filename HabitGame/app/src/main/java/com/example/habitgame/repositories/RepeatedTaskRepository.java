package com.example.habitgame.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitgame.model.RepeatedTask;
import com.example.habitgame.model.TaskStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RepeatedTaskRepository {

    private static final String COLL = "repeatedTasks";

    // ===== CREATE =====
    public static Task<DocumentReference> insert(@NonNull RepeatedTask rt){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (rt.getStatus() == null) rt.setStatus(TaskStatus.AKTIVAN);
        if (rt.getCreatedAt() == null) rt.setCreatedAt(System.currentTimeMillis());
        if (rt.getUserId() == null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : "unknown_user";
            rt.setUserId(uid);
        }

        TaskCompletionSource<DocumentReference> tcs = new TaskCompletionSource<>();
        db.collection(COLL).add(rt)
                .addOnSuccessListener(ref -> {
                    rt.setId(ref.getId());
                    tcs.setResult(ref);
                })
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    // ===== READ =====
    public static Task<RepeatedTask> getById(@NonNull String seriesId){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        TaskCompletionSource<RepeatedTask> tcs = new TaskCompletionSource<>();
        db.collection(COLL).document(seriesId).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) { tcs.setResult(null); return; }
                    RepeatedTask rt = snap.toObject(RepeatedTask.class);
                    if (rt != null) rt.setId(snap.getId());
                    tcs.setResult(rt);
                })
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    // ===== UPDATE (single-field helpers) =====
    /** Postavi status serije (AKTIVAN/PAUZIRAN/...). */
    public static Task<Void> setStatus(@NonNull String seriesId, @NonNull TaskStatus status){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String,Object> up = new HashMap<>();
        up.put("status", status.name());
        return db.collection(COLL).document(seriesId).update(up);
    }

    /** Pauziraj/aktiviraj seriju: upisuje paused + status; ako doc ne postoji → fallback na pojave. */
    public static Task<Void> setPaused(@NonNull String seriesId, boolean paused){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String,Object> up = new HashMap<>();
        up.put("paused", paused);
        up.put("status", paused ? TaskStatus.PAUZIRAN.name() : TaskStatus.AKTIVAN.name());

        return db.collection(COLL).document(seriesId)
                .update(up)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) return Tasks.forResult(null);
                    // Fallback kad doc ne postoji: označi buduće pojave flagom seriesPaused
                    return RepeatedTaskOccurrenceRepository.markSeriesPausedForFuture(seriesId, paused);
                });
    }

    /** Izmena imena/opisa na seriji. */
    public static Task<Void> updateNameDesc(@NonNull String seriesId,
                                            @NonNull String name,
                                            @Nullable String desc){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String,Object> up = new HashMap<>();
        up.put("name", name);
        // čuvamo null kao null (ako želiš brisanje polja može FieldValue.delete())
        up.put("description", (desc == null || desc.trim().isEmpty()) ? null : desc.trim());
        return db.collection(COLL).document(seriesId).update(up);
    }

    /** Generičan partial update nad serijom. */
    public static Task<Void> updateFields(@NonNull String seriesId,
                                          @NonNull Map<String, Object> fields){
        if (fields.isEmpty()) return Tasks.forResult(null);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection(COLL).document(seriesId).update(fields);
    }
}
