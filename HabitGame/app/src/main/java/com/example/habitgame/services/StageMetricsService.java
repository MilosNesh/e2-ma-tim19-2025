package com.example.habitgame.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class StageMetricsService {

    public interface Callback { void onReady(int successRate); void onError(Exception e); }

    public void computeStageSuccessSince(long sinceTimestamp, @Nullable Integer quota, @NonNull Callback cb) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { cb.onReady(0); return; }

        FirebaseFirestore.getInstance().collection("tasks")
                .whereEqualTo("userId", uid)
                .whereGreaterThan("createdAt", sinceTimestamp)
                .get()
                .addOnSuccessListener(snap -> {
                    int total = 0, done = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        String st = (String) d.get("status");
                        if ("PAUZIRAN".equals(st) || "OTKAZAN".equals(st)) continue;
                        total++;
                        if ("URADJEN".equals(st) || Boolean.TRUE.equals(d.getBoolean("isCompleted"))) done++;
                    }
                    if (quota != null && quota > 0 && total > quota) {
                        if (done > quota) done = quota;
                        total = quota;
                    }
                    int rate = (total <= 0) ? 0 : (int)Math.round(done * 100.0 / total);
                    cb.onReady(Math.max(0, Math.min(100, rate)));
                })
                .addOnFailureListener(cb::onError);
    }

    public void computeCurrentStageSuccess(@Nullable Integer quota, @NonNull Callback cb) {
        String uid = FirebaseAuth.getInstance().getCurrentUser()!=null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { cb.onReady(0); return; }

        FirebaseFirestore.getInstance().collection("tasks")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    int total = 0, done = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        String st = (String) d.get("status");
                        if ("PAUZIRAN".equals(st) || "OTKAZAN".equals(st)) continue;
                        total++;
                        if ("URADJEN".equals(st) || Boolean.TRUE.equals(d.getBoolean("isCompleted"))) done++;
                    }
                    if (quota != null && quota > 0 && total > quota) {
                        if (done > quota) done = quota;
                        total = quota;
                    }
                    int rate = (total <= 0) ? 0 : (int) Math.round(done * 100.0 / total);
                    cb.onReady(Math.max(0, Math.min(100, rate)));
                })
                .addOnFailureListener(cb::onError);
    }

    public interface IntResult { void onResult(int percent); }
    public interface Err { void onError(Exception e); }
    public void getLastStageSuccessPercent(long sinceTimestamp,
                                           @Nullable Integer quota,
                                           @NonNull IntResult ok,
                                           @Nullable Err errCb) {
        computeStageSuccessSince(sinceTimestamp, quota, new Callback() {
            @Override public void onReady(int successRate) { ok.onResult(successRate); }
            @Override public void onError(Exception e) {
                if (errCb != null) errCb.onError(e);
                ok.onResult(0);
            }
        });
    }
}
