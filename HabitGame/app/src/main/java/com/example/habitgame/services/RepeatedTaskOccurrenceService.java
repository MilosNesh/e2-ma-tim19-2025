package com.example.habitgame.services;

import androidx.annotation.NonNull;

import com.example.habitgame.model.RepeatedTaskOccurence;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.repositories.AccountRepository;
import com.example.habitgame.repositories.RepeatedTaskOccurrenceRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;
public class RepeatedTaskOccurrenceService {

    public com.google.android.gms.tasks.Task<Void> markDone(@NonNull RepeatedTaskOccurence oc) {
        if (oc.getId() == null || oc.getId().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Occurrence ID je prazan."));
        }

        TaskStatus cur = oc.getStatus() == null ? TaskStatus.AKTIVAN : oc.getStatus();
        if (cur == TaskStatus.URADJEN || cur == TaskStatus.OTKAZAN || cur == TaskStatus.NEURADJEN) {
            return Tasks.forResult(null);
        }
        if (cur != TaskStatus.AKTIVAN) {
            return Tasks.forException(new IllegalStateException("Samo aktivna pojava može biti označena kao urađena."));
        }

        Map<String, Object> up = new HashMap<>();
        up.put("status", TaskStatus.URADJEN.name());
        up.put("isCompleted", true);
        up.put("completedAt", System.currentTimeMillis());

        final int xp = Math.max(0, oc.getXp());

        return RepeatedTaskOccurrenceRepository.updateFields(oc.getId(), up)
                .onSuccessTask(a -> xp > 0
                        ? AccountRepository.addXpAndCheckLevelUp(xp)  // <<<< ovde
                        : Tasks.forResult(null));
    }

    public com.google.android.gms.tasks.Task<Void> markCanceled(@NonNull RepeatedTaskOccurence oc) {
        if (oc.getId() == null || oc.getId().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Occurrence ID je prazan."));
        }

        TaskStatus cur = oc.getStatus() == null ? TaskStatus.AKTIVAN : oc.getStatus();
        if (cur == TaskStatus.URADJEN || cur == TaskStatus.OTKAZAN || cur == TaskStatus.NEURADJEN) {
            return Tasks.forResult(null);
        }
        if (cur != TaskStatus.AKTIVAN) {
            return Tasks.forException(new IllegalStateException("Samo aktivna pojava može biti otkazana."));
        }

        Map<String, Object> up = new HashMap<>();
        up.put("status", TaskStatus.OTKAZAN.name());
        up.put("isCompleted", false);
        up.put("completedAt", FieldValue.delete());

        return RepeatedTaskOccurrenceRepository.updateFields(oc.getId(), up);
    }

    public com.google.android.gms.tasks.Task<Void> delete(@NonNull RepeatedTaskOccurence oc){
        if (oc.getId() == null || oc.getId().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Occurrence ID je prazan."));
        }
        return RepeatedTaskOccurrenceRepository.deleteById(oc.getId());
    }
}
