package com.example.habitgame.adapters;

import android.view.*; import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.ListAdapter;

import com.example.habitgame.R;
import com.example.habitgame.model.Task;

public class TaskItemAdapter extends ListAdapter<Task, TaskItemAdapter.VH> {

    public interface Listener {
        void onOpen(Task t);
        void onDone(Task t);
        void onCancel(Task t);
        void onPause(Task t);
        void onActive(Task t);
    }

    private final Listener listener;

    public TaskItemAdapter(Listener l) { super(DIFF); this.listener = l; }

    private static final DiffUtil.ItemCallback<Task> DIFF = new DiffUtil.ItemCallback<Task>() {
        @Override public boolean areItemsTheSame(@NonNull Task a, @NonNull Task b) { return eq(a.getId(), b.getId()); }
        @Override public boolean areContentsTheSame(@NonNull Task a, @NonNull Task b) {
            return eq(a.getName(), b.getName()) && eq(a.getDescription(), b.getDescription())
                    && eq(a.getStatus(), b.getStatus()) && a.getIsCompleted()==b.getIsCompleted();
        }
        private boolean eq(Object x, Object y){ return x==null? y==null : x.equals(y); }
    };

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_task, p, false);
        return new VH(view);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Task t = getItem(pos);
        h.name.setText(t.getName());
        h.desc.setText(t.getDescription()==null? "" : t.getDescription());
        h.itemView.setOnClickListener(v -> listener.onOpen(t));
        h.done.setOnClickListener(v -> listener.onDone(t));
        h.cancel.setOnClickListener(v -> listener.onCancel(t));
        h.pause.setOnClickListener(v -> listener.onPause(t));
        h.active.setOnClickListener(v -> listener.onActive(t));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, desc; ImageButton done, cancel, pause, active;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_name);
            desc = itemView.findViewById(R.id.tv_desc);
            done = itemView.findViewById(R.id.btn_done);
            cancel = itemView.findViewById(R.id.btn_cancel);
            pause = itemView.findViewById(R.id.btn_pause);
            active= itemView.findViewById(R.id.btn_active);
        }
    }
}
