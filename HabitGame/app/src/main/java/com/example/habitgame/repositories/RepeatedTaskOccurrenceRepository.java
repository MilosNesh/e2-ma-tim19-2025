package com.example.habitgame.repositories;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitgame.model.RepeatedTaskOccurence;
import com.example.habitgame.model.TaskStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RepeatedTaskOccurrenceRepository {

    private static final String TAG  = "RepeatedTaskOccRepo";
    private static final String COLL = "repeated_task_occurrences";

    // ===== READ =====

    public static Task<List<RepeatedTaskOccurence>> getByUserInRange(
            @NonNull String uid, long fromInclusive, long toInclusive) {
        TaskCompletionSource<List<RepeatedTaskOccurence>> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(COLL)
                .whereEqualTo("userId", uid)
                .whereGreaterThanOrEqualTo("when", fromInclusive)
                .whereLessThanOrEqualTo("when", toInclusive)
                .orderBy("when", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<RepeatedTaskOccurence> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        RepeatedTaskOccurence o = d.toObject(RepeatedTaskOccurence.class);
                        o.setId(d.getId());
                        out.add(o);
                    }
                    tcs.setResult(out);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error getByUserInRange", e);
                    tcs.setException(e);
                });

        return tcs.getTask();
    }

    public static Task<List<RepeatedTaskOccurence>> getForCurrentUserBetween(long fromInclusive, long toInclusive){
        String uid = FirebaseAuth.getInstance().getCurrentUser()!=null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) return Tasks.forResult(new ArrayList<>());
        return getByUserInRange(uid, fromInclusive, toInclusive);
    }

    public static Task<List<RepeatedTaskOccurence>> getBySeriesInRange(
            @NonNull String seriesId, long fromInclusive, long toInclusive) {
        TaskCompletionSource<List<RepeatedTaskOccurence>> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(COLL)
                .whereEqualTo("repeatedTaskId", seriesId)
                .whereGreaterThanOrEqualTo("when", fromInclusive)
                .whereLessThanOrEqualTo("when", toInclusive)
                .orderBy("when", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<RepeatedTaskOccurence> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        RepeatedTaskOccurence o = d.toObject(RepeatedTaskOccurence.class);
                        o.setId(d.getId());
                        out.add(o);
                    }
                    tcs.setResult(out);
                })
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }

    // ===== WRITE =====

    public static Task<Void> batchInsert(@NonNull String seriesId,
                                         @NonNull List<RepeatedTaskOccurence> list){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Task<Void>> writes = new ArrayList<>();
        for (RepeatedTaskOccurence o : list){
            DocumentReference doc = db.collection(COLL).document();
            o.setId(doc.getId());
            writes.add(doc.set(o));
        }
        return Tasks.whenAll(writes);
    }

    public static Task<Void> updateFields(@NonNull String occurrenceId,
                                          @NonNull Map<String, Object> fields){
        if (fields.isEmpty()) return Tasks.forResult(null);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection(COLL).document(occurrenceId).update(fields);
    }

    /** NEW: obriši JEDNU pojavu (document) po ID-u. */
    public static Task<Void> deleteById(@NonNull String occurrenceId){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection(COLL).document(occurrenceId).delete();
    }

    // ===== BULK UPDATE =====

    public static Task<Void> markSeriesPausedForFuture(@NonNull String seriesId, boolean paused) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long todayMidnight = normalizeMidnight(System.currentTimeMillis());

        return db.collection(COLL)
                .whereEqualTo("repeatedTaskId", seriesId)
                .whereGreaterThanOrEqualTo("when", todayMidnight)
                .get()
                .onSuccessTask(snap -> {
                    WriteBatch wb = db.batch();
                    for (QueryDocumentSnapshot d : snap) {
                        wb.update(d.getReference(), "seriesPaused", paused);
                    }
                    return wb.commit();
                });
    }

    public static Task<Void> updateFutureNames(@NonNull String seriesId, @NonNull String newName){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long todayMidnight = normalizeMidnight(System.currentTimeMillis());

        return db.collection(COLL)
                .whereEqualTo("repeatedTaskId", seriesId)
                .whereGreaterThanOrEqualTo("when", todayMidnight)
                .get()
                .onSuccessTask(snap -> {
                    WriteBatch wb = db.batch();
                    for (QueryDocumentSnapshot d : snap) {
                        wb.update(d.getReference(), "taskName", newName);
                    }
                    return wb.commit();
                });
    }

    public static Task<Void> updateFutureDescriptions(@NonNull String seriesId, @Nullable String newDesc){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long todayMidnight = normalizeMidnight(System.currentTimeMillis());

        return db.collection(COLL)
                .whereEqualTo("repeatedTaskId", seriesId)
                .whereGreaterThanOrEqualTo("when", todayMidnight)
                .get()
                .onSuccessTask(snap -> {
                    WriteBatch wb = db.batch();
                    for (QueryDocumentSnapshot d : snap) {
                        if (newDesc == null || newDesc.trim().isEmpty()) {
                            wb.update(d.getReference(), "taskDescription", FieldValue.delete());
                        } else {
                            wb.update(d.getReference(), "taskDescription", newDesc);
                        }
                    }
                    return wb.commit();
                });
    }

    public static Task<Void> updateFutureStatusForSeries(@NonNull String seriesId,
                                                         @NonNull TaskStatus targetStatus){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long todayMidnight = normalizeMidnight(System.currentTimeMillis());

        final String fromStatus = (targetStatus == TaskStatus.PAUZIRAN)
                ? TaskStatus.AKTIVAN.name()
                : TaskStatus.PAUZIRAN.name();

        return db.collection(COLL)
                .whereEqualTo("repeatedTaskId", seriesId)
                .whereGreaterThanOrEqualTo("when", todayMidnight)
                .whereEqualTo("status", fromStatus)
                .get()
                .onSuccessTask(snap -> {
                    WriteBatch wb = db.batch();
                    boolean setPausedFlag = (targetStatus == TaskStatus.PAUZIRAN);
                    for (QueryDocumentSnapshot d : snap) {
                        DocumentReference ref = d.getReference();
                        wb.update(ref, "status", targetStatus.name());
                        wb.update(ref, "seriesPaused", setPausedFlag);
                    }
                    return wb.commit();
                });
    }

    private static long normalizeMidnight(long ms){
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(java.util.Calendar.HOUR_OF_DAY,0);
        c.set(java.util.Calendar.MINUTE,0);
        c.set(java.util.Calendar.SECOND,0);
        c.set(java.util.Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }

    public static Task<Void> updateFutureSnapshotFields(
            @NonNull String seriesId,
            @NonNull String newName,
            @Nullable String newDesc
    ){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long todayMidnight = normalizeMidnight(System.currentTimeMillis());

        return db.collection(COLL)
                .whereEqualTo("repeatedTaskId", seriesId)
                .whereGreaterThanOrEqualTo("when", todayMidnight)
                .get()
                .onSuccessTask(snap -> {
                    WriteBatch wb = db.batch();
                    for (QueryDocumentSnapshot d : snap) {
                        DocumentReference ref = d.getReference();
                        wb.update(ref, "taskName", newName);
                        if (newDesc == null || newDesc.trim().isEmpty()) {
                            wb.update(ref, "taskDescription", FieldValue.delete());
                        } else {
                            wb.update(ref, "taskDescription", newDesc);
                        }
                    }
                    return wb.commit();
                });
    }
}
