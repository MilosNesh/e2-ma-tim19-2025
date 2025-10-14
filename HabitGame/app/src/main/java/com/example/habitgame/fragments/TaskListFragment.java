package com.example.habitgame.fragments;

import android.os.Bundle;
import android.view.*; import android.widget.TextView; import android.widget.Toast;
import androidx.annotation.*; import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import com.example.habitgame.R;
import com.example.habitgame.adapters.TaskItemAdapter;
import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.services.TaskService;
import java.util.*; import java.util.stream.Collectors;

public class TaskListFragment extends Fragment implements TaskItemAdapter.Listener {

    private static final String ARG_REPEATING = "repeating";
    public static TaskListFragment newInstance(boolean repeating){
        Bundle b = new Bundle(); b.putBoolean(ARG_REPEATING, repeating);
        TaskListFragment f = new TaskListFragment(); f.setArguments(b); return f;
    }

    private boolean repeating;
    private RecyclerView rv; private TextView tvEmpty;
    private TaskItemAdapter adapter;
    private final TaskService taskService = new TaskService();

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_task_list, c, false);
        repeating = getArguments()!=null && getArguments().getBoolean(ARG_REPEATING, false);

        tvEmpty = v.findViewById(R.id.tv_empty);
        rv = v.findViewById(R.id.recycler_tasks);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskItemAdapter(this);
        rv.setAdapter(adapter);

        load();
        return v;
    }

    private void load() {
        taskService.getTasksForCurrentUser()
                .addOnSuccessListener(list -> {
                    if (list == null) list = new ArrayList<>();
                    List<Task> filtered = new ArrayList<>();
                    for (Task t : list) {
                        boolean rep = t.getIsRepeating();
                        if (rep == repeating) filtered.add(t);
                    }
                    if (filtered.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE); rv.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE); rv.setVisibility(View.VISIBLE);
                        adapter.submitList(filtered);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),"Greška: "+e.getMessage(),Toast.LENGTH_LONG).show());
    }

    @Override public void onOpen(Task t) {
        TaskDetailsBottomSheet.newInstance(t).show(getParentFragmentManager(), "taskDetails");
    }
    @Override public void onDone(Task t) {
        taskService.markDone(t).addOnSuccessListener(a->load());
    }
    @Override public void onCancel(Task t) {
        taskService.markCanceled(t).addOnSuccessListener(a->load());
    }
    @Override public void onPause(Task t) {
        taskService.markPaused(t).addOnSuccessListener(a->load());
    }
    @Override public void onActive(Task t) {
        taskService.markActive(t).addOnSuccessListener(a->load());
    }
}
