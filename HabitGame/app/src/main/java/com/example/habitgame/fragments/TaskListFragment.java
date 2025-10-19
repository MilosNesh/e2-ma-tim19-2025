package com.example.habitgame.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habitgame.R;
import com.example.habitgame.adapters.TaskItemAdapter;
import com.example.habitgame.model.RepeatedTaskOccurence;
import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.repositories.RepeatedTaskOccurrenceRepository;
import com.example.habitgame.repositories.TaskRepository;
import com.example.habitgame.services.RepeatedTaskOccurrenceService;
import com.example.habitgame.services.TaskService;
import com.example.habitgame.utils.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskListFragment extends Fragment implements TaskItemAdapter.Listener {

    private static final String ARG_REPEATING = "repeating";
    public static TaskListFragment newInstance(boolean repeating){
        Bundle b = new Bundle(); b.putBoolean(ARG_REPEATING, repeating);
        TaskListFragment f = new TaskListFragment(); f.setArguments(b); return f;
    }

    private boolean repeating;
    private RecyclerView rv;
    private TextView tvEmpty;
    private TaskItemAdapter adapter;

    private final TaskService taskService = new TaskService();
    private final RepeatedTaskOccurrenceService occService = new RepeatedTaskOccurrenceService();

    private final List<Task> current = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_task_list, c, false);
        repeating = getArguments()!=null && getArguments().getBoolean(ARG_REPEATING, false);

        tvEmpty = v.findViewById(R.id.tv_empty);
        rv = v.findViewById(R.id.recycler_tasks);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskItemAdapter(this);
        rv.setAdapter(adapter);

        load();

        getParentFragmentManager().setFragmentResultListener("taskStatusChanged", this, (key, res) -> {
            String taskId = res.getString("taskId");
            String st = res.getString("newStatus", "AKTIVAN");
            TaskStatus ns;
            try { ns = TaskStatus.valueOf(st); } catch (Exception e){ ns = TaskStatus.AKTIVAN; }
            applyLocalStatus(taskId, ns, ns==TaskStatus.URADJEN);
            submitOrEmpty();
        });

        return v;
    }

    private void load() {
        current.clear();

        final long from = DateUtils.startOfToday();
        final long to   = addMonths(from, 6);

        if (!repeating) {
            TaskRepository.getTasksForCurrentUser()
                    .addOnSuccessListener(list -> {
                        if (list == null) list = new ArrayList<>();
                        for (Task t : list) {
                            taskService.autoFlipOverdueToMissed(t);

                            if (!Boolean.TRUE.equals(t.getIsRepeating())) {
                                Long when = (t.getExecutionTime()!=null ? t.getExecutionTime() : t.getStartDate());
                                if (when != null && DateUtils.isTodayOrFuture(when)) {
                                    current.add(t);
                                }
                            }
                        }
                        sortByTime(current);
                        submitOrEmpty();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),"Greška: "+e.getMessage(),Toast.LENGTH_LONG).show());

        } else {
            RepeatedTaskOccurrenceRepository.getForCurrentUserBetween(from, to)
                    .addOnSuccessListener(occs -> {
                        if (occs == null) occs = Collections.emptyList();
                        for (RepeatedTaskOccurence oc : occs) {
                            current.add(mapOccurrenceToDisplayTask(oc));
                        }
                        sortByTime(current);
                        submitOrEmpty();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),"Greška: "+e.getMessage(),Toast.LENGTH_LONG).show());
        }
    }

    private static long addMonths(long from, int months){
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(from);
        c.add(java.util.Calendar.MONTH, months);
        return c.getTimeInMillis();
    }

    private void sortByTime(List<Task> list){
        Collections.sort(list, (a,b)-> {
            long ax = a.getExecutionTime()!=null? a.getExecutionTime() : (a.getStartDate()!=null? a.getStartDate():0);
            long bx = b.getExecutionTime()!=null? b.getExecutionTime() : (b.getStartDate()!=null? b.getStartDate():0);
            ax = ax==0? Long.MAX_VALUE: ax;
            bx = bx==0? Long.MAX_VALUE: bx;
            return Long.compare(ax, bx);
        });
    }

    private void forceSubmit() {
        adapter.submitList(null);
        adapter.submitList(new ArrayList<>(current));
    }

    private void submitOrEmpty() {
        if (current.isEmpty()) { tvEmpty.setVisibility(View.VISIBLE); rv.setVisibility(View.GONE); }
        else { tvEmpty.setVisibility(View.GONE); rv.setVisibility(View.VISIBLE); }
        forceSubmit();
    }


    @Override public void onOpen(Task t) {
        if (isOccurrenceDisplay(t)) {
            RepeatedTaskOccurrenceDetailsBottomSheet.newInstance(mapDisplayTaskIdToOccurrence(t))
                    .show(getParentFragmentManager(), "occDetails");
        } else {
            TaskDetailsBottomSheet.newInstance(t).show(getParentFragmentManager(), "taskDetails");
        }
    }

    @Override public void onDone(Task t) {
        if (isOccurrenceDisplay(t)) {
            RepeatedTaskOccurence oc = mapDisplayTaskIdToOccurrence(t);
            occService.markDone(oc)
                    .addOnSuccessListener(x -> {
                        Toast.makeText(getContext(), oc.getXp() + " XP", Toast.LENGTH_SHORT).show();
                        load();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            new TaskService().markDone(t)
                    .addOnSuccessListener(x -> {
                        Toast.makeText(getContext(), "Označeno kao urađeno.", Toast.LENGTH_SHORT).show();
                        load();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override public void onCancel(Task t) {
        if (isOccurrenceDisplay(t)) {
            RepeatedTaskOccurence oc = mapDisplayTaskIdToOccurrence(t);
            occService.markCanceled(oc)
                    .addOnSuccessListener(x -> {
                        Toast.makeText(getContext(), "✅ Otkazano.", Toast.LENGTH_SHORT).show();
                        load();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            new TaskService().markCanceled(t)
                    .addOnSuccessListener(x -> {
                        Toast.makeText(getContext(), "✅ Otkazano.", Toast.LENGTH_SHORT).show();
                        load();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private boolean isOccurrenceDisplay(@NonNull Task t) {
        String id = t.getId();
        return id != null && id.startsWith("occ:");
    }

    private RepeatedTaskOccurence mapDisplayTaskIdToOccurrence(@NonNull Task t) {
        RepeatedTaskOccurence oc = new RepeatedTaskOccurence();
        oc.setId(t.getId().substring("occ:".length()));
        oc.setUserId(t.getUserId());
        oc.setTaskName(t.getName());
        oc.setWhen(t.getExecutionTime());
        oc.setXp(t.getXpValue());
        oc.setCompleted(t.getIsCompleted());
        oc.setCompletedAt(t.getLastCompletionTimestamp());
        oc.setStatus(t.getStatus());
        return oc;
    }

    private Task mapOccurrenceToDisplayTask(@NonNull RepeatedTaskOccurence oc) {
        Task t = new Task();
        t.setId("occ:" + oc.getId());
        t.setUserId(oc.getUserId());
        t.setName(oc.getTaskName() != null ? oc.getTaskName() : "Ponavljajući");
        t.setDescription(null);
        t.setCategoryId(null);
        t.setWeight(null);
        t.setImportance(null);
        t.setXpValue(Math.max(0, oc.getXp()));
        t.setExecutionTime(oc.getWhen());
        t.setIsRepeating(false);
        t.setStartDate(oc.getWhen());
        t.setEndDate(null);
        t.setRepeatInterval(null);
        t.setRepeatUnit(null);
        t.setIsCompleted(oc.isCompleted());
        t.setCreationTimestamp(oc.getCreatedAt());
        t.setLastCompletionTimestamp(oc.getCompletedAt());
        t.setStatus(oc.getStatus() != null ? oc.getStatus() : TaskStatus.AKTIVAN);
        return t;
    }

    private void applyLocalStatus(String taskId, TaskStatus st, boolean completed) {
        boolean remove = (st == TaskStatus.OTKAZAN || st == TaskStatus.URADJEN);

        for (int i = current.size()-1; i>=0; i--){
            Task it = current.get(i);
            if (taskId.equals(it.getId())) {
                if (remove) {
                    current.remove(i);
                } else {
                    Task copy = cloneTask(it);
                    copy.setStatus(st);
                    copy.setIsCompleted(completed);
                    current.set(i, copy);
                }
            }
        }
    }

    private Task cloneTask(Task s){
        Task t = new Task();
        t.setId(s.getId());
        t.setUserId(s.getUserId());
        t.setName(s.getName());
        t.setDescription(s.getDescription());
        t.setCategoryId(s.getCategoryId());
        t.setWeight(s.getWeight());
        t.setImportance(s.getImportance());
        t.setXpValue(s.getXpValue());
        t.setExecutionTime(s.getExecutionTime());
        t.setIsRepeating(s.getIsRepeating());
        t.setStartDate(s.getStartDate());
        t.setEndDate(s.getEndDate());
        t.setRepeatInterval(s.getRepeatInterval());
        t.setRepeatUnit(s.getRepeatUnit());
        t.setIsCompleted(s.getIsCompleted());
        t.setCreationTimestamp(s.getCreationTimestamp());
        t.setLastCompletionTimestamp(s.getLastCompletionTimestamp());
        t.setCompletionsTodayCount(s.getCompletionsTodayCount());
        t.setStatus(s.getStatus());
        return t;
    }
}
