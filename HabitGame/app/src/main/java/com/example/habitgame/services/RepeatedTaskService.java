package com.example.habitgame.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitgame.model.RepeatedTask;
import com.example.habitgame.model.RepeatedTaskOccurence;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.repositories.RepeatedTaskOccurrenceRepository;
import com.example.habitgame.repositories.RepeatedTaskRepository;
import com.example.habitgame.utils.XpCalculator;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RepeatedTaskService {

    // ====== PAUZA / AKTIVACIJA / EDIT ======

    public Task<Void> pauseSeries(@NonNull String seriesId){
        return RepeatedTaskRepository.setStatus(seriesId, TaskStatus.PAUZIRAN)
                .onSuccessTask(v -> RepeatedTaskOccurrenceRepository.updateFutureStatusForSeries(seriesId, TaskStatus.PAUZIRAN));
    }

    public Task<Void> activateSeries(@NonNull String seriesId){
        return RepeatedTaskRepository.setStatus(seriesId, TaskStatus.AKTIVAN)
                .onSuccessTask(v -> RepeatedTaskOccurrenceRepository.updateFutureStatusForSeries(seriesId, TaskStatus.AKTIVAN));
    }

    public Task<Void> updateSeriesNameDesc(@NonNull String seriesId,
                                           @NonNull String name,
                                           @Nullable String desc){
        return RepeatedTaskRepository.updateNameDesc(seriesId, name, desc)
                .onSuccessTask(v -> RepeatedTaskOccurrenceRepository.updateFutureSnapshotFields(seriesId, name, desc));
    }

    // ====== KREIRANJE SERIJE + GENERISANJE POJAVA ======

    public Task<DocumentReference> createSeriesAndGenerate(
            @NonNull String name,
            @Nullable String description,
            @NonNull String categoryId,
            @Nullable String weight,
            @Nullable String importance,
            long startDateMs,
            @Nullable Long endDateMs,
            int repeatInterval,
            @NonNull String repeatUnit // "day" | "week"
    ){
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "unknown_user";

        RepeatedTask rt = new RepeatedTask();
        rt.setUserId(uid);
        rt.setName(name);
        rt.setDescription(description);
        rt.setCategoryId(categoryId);
        rt.setWeight(weight);
        rt.setImportance(importance);
        rt.setStartDate(normalizeMidnight(startDateMs));
        rt.setEndDate(endDateMs == null ? null : normalizeMidnight(endDateMs));
        rt.setRepeatInterval(Math.max(1, repeatInterval));
        rt.setRepeatUnit(RepeatedTask.normUnit(repeatUnit));
        rt.setStatus(TaskStatus.AKTIVAN);
        rt.setCreatedAt(System.currentTimeMillis());

        return RepeatedTaskRepository.insert(rt)
                .onSuccessTask(docRef -> {
                    String seriesId = docRef.getId();
                    rt.setId(seriesId);

                    long from = rt.getStartDate();
                    long to = (rt.getEndDate() != null) ? rt.getEndDate() : rt.getStartDate();

                    List<Long> dates = generateDates(rt, from, to);
                    List<RepeatedTaskOccurence> occs = new ArrayList<>();

                    // XP po pojavi – SVIMA ISTA VREDNOST (težina + bitnost)
                    int perOccXp = Math.max(0, XpCalculator.calculateTotalXp(rt.getWeight(), rt.getImportance()));

                    for (long d : dates){
                        RepeatedTaskOccurence o = new RepeatedTaskOccurence();
                        o.setRepeatedTaskId(seriesId);
                        o.setUserId(uid);
                        o.setWhen(d);
                        o.setStatus(TaskStatus.AKTIVAN);
                        o.setCompleted(false);
                        o.setTaskName(rt.getName());
                        o.setTaskDescription(rt.getDescription());
                        o.setCategoryId(rt.getCategoryId());
                        o.setSeriesPaused(false);
                        o.setXp(perOccXp); // ← jednako za svaku pojavu
                        o.setCreatedAt(System.currentTimeMillis());
                        occs.add(o);
                    }

                    if (occs.isEmpty()) return Tasks.forResult(docRef);
                    return RepeatedTaskOccurrenceRepository.batchInsert(seriesId, occs)
                            .onSuccessTask(v -> Tasks.forResult(docRef));
                });
    }

    // ===== helpers =====

    private static long normalizeMidnight(long ms){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }

    private static List<Long> generateDates(RepeatedTask rt, long fromMs, long toMs){
        List<Long> out = new ArrayList<>();
        if (fromMs > toMs) return out;

        int step = Math.max(1, rt.getRepeatInterval());
        String unit = rt.getRepeatUnit() == null ? "day" : rt.getRepeatUnit().toLowerCase(Locale.ROOT);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(rt.getStartDate());

        while (cal.getTimeInMillis() < fromMs){
            addInterval(cal, step, unit);
        }
        while (cal.getTimeInMillis() <= toMs){
            out.add(normalizeMidnight(cal.getTimeInMillis()));
            addInterval(cal, step, unit);
        }
        return out;
    }

    private static void addInterval(Calendar cal, int step, String unit){
        switch (unit){
            case "week":  cal.add(Calendar.WEEK_OF_YEAR, step); break;
            default:      cal.add(Calendar.DAY_OF_YEAR, step);  break;
        }
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
    }
}
