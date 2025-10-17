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
import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.services.TaskService;
import com.example.habitgame.utils.DateUtils;
import com.example.habitgame.utils.RecurrenceScheduler;

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

        // LISTENER za rezultate iz TaskDetailsBottomSheet (osvežavanje posle akcija)
        getParentFragmentManager().setFragmentResultListener(
                TaskDetailsBottomSheet.FR_RESULT_KEY, this, (key, res) -> load()
        );

        // (postojeći) listener ako već koristiš drugi key
        getParentFragmentManager().setFragmentResultListener("taskStatusChanged", this, (key, res) -> {
            String taskId = res.getString("taskId");
            String st = res.getString("newStatus", "AKTIVAN");
            TaskStatus ns;
            try { ns = TaskStatus.valueOf(st); } catch (Exception e){ ns = TaskStatus.AKTIVAN; }
            applyLocalStatus(taskId, ns, ns==TaskStatus.URADJEN);
            submitOrEmpty();
        });

        load();
        return v;
    }

    private void load() {
        taskService.getTasksForCurrentUser()
                .addOnSuccessListener(list -> {
                    if (list == null) list = new ArrayList<>();
                    long from = DateUtils.startOfToday();
                    long to   = addMonths(from, 6);

                    current.clear();

                    for (Task t : list) {
                        // u listi ne prikazujemo otkazane
                        if (t.getStatus() == TaskStatus.OTKAZAN) continue;

                        if (repeating) {
                            if (Boolean.TRUE.equals(t.getIsRepeating())) {
                                current.addAll(RecurrenceScheduler.expandTaskAsInstances(t, from, to));
                            }
                        } else {
                            if (!Boolean.TRUE.equals(t.getIsRepeating())) {
                                Long when = (t.getExecutionTime()!=null ? t.getExecutionTime() : t.getStartDate());
                                if (when != null && DateUtils.isTodayOrFuture(when)) {
                                    current.add(t);
                                }
                            }
                        }
                    }

                    sortByTime(current);
                    submitOrEmpty();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),"Greška: "+e.getMessage(),Toast.LENGTH_LONG).show());
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

    // ---- Listener impl ----

    @Override public void onOpen(Task t) {
        // OTVORI BOTTOM SHEET – ovo je nedostajalo
        TaskDetailsBottomSheet.newInstance(t)
                .show(getParentFragmentManager(), "taskDetails");
    }

    @Override public void onDone(Task t) {
        TaskStatus old = t.getStatus();
        Boolean oldCompleted = t.getIsCompleted();
        applyLocalStatus(t.getId(), TaskStatus.URADJEN, true);
        submitOrEmpty();

        taskService.markDone(t)
                .addOnFailureListener(e -> {
                    applyLocalStatus(t.getId(), old, oldCompleted!=null && oldCompleted);
                    submitOrEmpty();
                });
    }

    @Override public void onCancel(Task t) {
        TaskStatus old = t.getStatus();
        Boolean oldCompleted = t.getIsCompleted();
        applyLocalStatus(t.getId(), TaskStatus.OTKAZAN, false);
        submitOrEmpty();

        taskService.markCanceled(t)
                .addOnFailureListener(e -> {
                    applyLocalStatus(t.getId(), old, oldCompleted!=null && oldCompleted);
                    submitOrEmpty();
                });
    }

    @Override public void onPause(Task t) {
        TaskStatus old = t.getStatus();
        Boolean oldCompleted = t.getIsCompleted();
        applyLocalStatus(t.getId(), TaskStatus.PAUZIRAN, false);
        submitOrEmpty();

        taskService.markPaused(t)
                .addOnFailureListener(e -> {
                    applyLocalStatus(t.getId(), old, oldCompleted!=null && oldCompleted);
                    submitOrEmpty();
                });
    }

    @Override public void onActive(Task t) {
        TaskStatus old = t.getStatus();
        Boolean oldCompleted = t.getIsCompleted();
        applyLocalStatus(t.getId(), TaskStatus.AKTIVAN, false);
        submitOrEmpty();

        taskService.markActive(t)
                .addOnFailureListener(e -> {
                    applyLocalStatus(t.getId(), old, oldCompleted!=null && oldCompleted);
                    submitOrEmpty();
                });
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
