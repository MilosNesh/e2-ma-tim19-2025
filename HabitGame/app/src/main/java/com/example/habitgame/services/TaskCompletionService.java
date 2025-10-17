package com.example.habitgame.services;

import android.util.Log;
import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskCompletionCallback;
import com.example.habitgame.repositories.TaskRepository;
import com.example.habitgame.repositories.AccountRepository; // Potreban za ažuriranje XP-a
import com.example.habitgame.utils.DateUtils; // Kreiraćemo ovu klasu za jednostavnu manipulaciju datumima

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.TimeUnit;

public class TaskCompletionService {

    private static final String TAG = "TaskCompletionService";

    public void completeTask(Task task, TaskCompletionCallback callback) {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (task.getIsCompleted() && !task.getIsRepeating()) {
            callback.onSuccess(0, "Zadatak je već završen.");
            return;
        }

        if (isQuotaExceeded(task)) {
            updateTaskStatus(task, true, callback, 0);
            callback.onSuccess(0, "Zadatak završen, ali je kvota za bodovanje danas/ovog meseca ispunjena.");
            return;
        }

        int xpEarned = task.getXpValue();

        AccountRepository.updateXp(userId, xpEarned)
                .addOnSuccessListener(aVoid -> {
                    // 4. Ažuriraj status Taska (isCompleted, lastCompletionTimestamp, completionsTodayCount)
                    updateTaskStatus(task, true, callback, xpEarned);

                    // 5. Vrati uspeh
                    callback.onSuccess(xpEarned, "Uspešno završen zadatak. Osvojeno: " + xpEarned + " XP.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Account XP.", e);
                    callback.onFailure("Greška pri dodeljivanju XP-a.", e);
                });
    }

    private boolean isQuotaExceeded(Task task) {

        long todayStartTimestamp = DateUtils.startOfToday();
        if (task.getLastCompletionTimestamp() == null || task.getLastCompletionTimestamp() < todayStartTimestamp) {
            task.setCompletionsTodayCount(0);
        }

        String weight = task.getWeight();
        String importance = task.getImportance();

        int dailyLimit = 0;
        long periodMillis = 0;

        // Logika Specifikacije:
        if (("Veoma lak".equals(weight) || "Lak".equals(weight)) && "Normalan".equals(importance)) {
            dailyLimit = 5;
        } else if ("Lak".equals(weight) && "Vazan".equals(importance)) {
            dailyLimit = 5;
        } else if ("Tezak".equals(weight) && "Ekstremno vazan".equals(importance)) {
            dailyLimit = 2;
        } else if ("Ekstremno tezak".equals(weight)) {
            periodMillis = TimeUnit.DAYS.toMillis(7);
            dailyLimit = 1;
        } else if ("Specijalan".equals(importance)) {
            periodMillis = TimeUnit.DAYS.toMillis(30);
            dailyLimit = 1;
        }

        if (dailyLimit > 0 && periodMillis == 0) {
            return task.getCompletionsTodayCount() >= dailyLimit;
        }

        if (periodMillis > 0 && task.getLastCompletionTimestamp() != null) {
            long timeSinceLastCompletion = System.currentTimeMillis() - task.getLastCompletionTimestamp();
            // Ako je kvota 1, i prošlo je manje od PERIODA (npr. 7 dana), kvota je prekoračena.
            return timeSinceLastCompletion < periodMillis;
        }

        return false;
    }

    private void updateTaskStatus(Task task, boolean isCompleted, TaskCompletionCallback callback, int xpEarned) {

        // Ažuriranje polja za sledeće bodovanje
        long currentTimestamp = System.currentTimeMillis();
        task.setIsCompleted(isCompleted);

        // Ažuriraj polja KVOTA ako je XP dodeljen
        if (xpEarned > 0) {
            task.setLastCompletionTimestamp(currentTimestamp);
            task.setCompletionsTodayCount(task.getCompletionsTodayCount() + 1);
        }

        TaskRepository.update(task)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Task status in DB.", e);
                    callback.onFailure("Greška pri ažuriranju statusa zadatka.", e);
                });
    }
}