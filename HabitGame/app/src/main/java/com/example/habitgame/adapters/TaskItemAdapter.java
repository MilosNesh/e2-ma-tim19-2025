package com.example.habitgame.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habitgame.R;
import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskItemAdapter extends ListAdapter<Task, TaskItemAdapter.VH> {

    public interface Listener {
        void onOpen(Task t);
        void onDone(Task t);
        void onCancel(Task t);
        void onPause(Task t);
        void onActive(Task t);
    }

    private final Listener listener;
    private final SimpleDateFormat dateDf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public TaskItemAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Task> DIFF =
            new DiffUtil.ItemCallback<Task>() {
                @Override public boolean areItemsTheSame(@NonNull Task o, @NonNull Task n) {
                    String oid = o.getId(), nid = n.getId();
                    return oid != null && oid.equals(nid);
                }
                @Override public boolean areContentsTheSame(@NonNull Task o, @NonNull Task n) {
                    return eq(o.getName(), n.getName())
                            && eq(o.getDescription(), n.getDescription())
                            && eq(o.getWeight(), n.getWeight())
                            && eq(o.getImportance(), n.getImportance())
                            && o.getXpValue() == n.getXpValue()
                            && eq(o.getStatus(), n.getStatus())
                            && eq(o.getExecutionTime(), n.getExecutionTime())
                            && eq(o.getStartDate(), n.getStartDate())
                            && eq(o.getEndDate(), n.getEndDate())
                            && o.getIsRepeating() == n.getIsRepeating();
                }
                private boolean eq(Object a, Object b){ return a==null? b==null : a.equals(b); }
            };

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Task t = getItem(position);

        h.tvTitle.setText(t.getName() == null ? "" : t.getName());
        h.tvDesc.setText(t.getDescription() == null ? "" : t.getDescription());
        h.tvXp.setText(h.itemView.getContext().getString(R.string.xp_s, t.getXpValue()));

        Long when = t.getExecutionTime() != null ? t.getExecutionTime() : t.getStartDate();
        h.tvWhen.setText(when != null ? dateDf.format(new Date(when)) : "");

        // STATUS BADGE
        String label = getStatusLabel(h, t.getStatus());
        int color = getStatusColor(t.getStatus());
        h.tvStatus.setText(label);
        h.tvStatus.setTextColor(color);

        // Klikovi
        h.itemView.setOnClickListener(v -> listener.onOpen(t));
        h.btnMore.setOnClickListener(v -> listener.onOpen(t));
        h.btnDone.setOnClickListener(v -> listener.onDone(t));
        h.btnCancel.setOnClickListener(v -> listener.onCancel(t));
        h.btnPause.setOnClickListener(v -> listener.onPause(t));
        h.btnActive.setOnClickListener(v -> listener.onActive(t));
    }

    private String getStatusLabel(@NonNull VH h, TaskStatus st) {
        if (st == null) return h.itemView.getContext().getString(R.string.status_active);
        switch (st) {
            case AKTIVAN:   return h.itemView.getContext().getString(R.string.status_active);
            case PAUZIRAN:  return h.itemView.getContext().getString(R.string.status_paused);
            case OTKAZAN:   return h.itemView.getContext().getString(R.string.status_canceled);
            case URADJEN:   return h.itemView.getContext().getString(R.string.status_done);
        }
        return h.itemView.getContext().getString(R.string.status_active);
    }

    private int getStatusColor(TaskStatus st) {
        if (st == null) return Color.parseColor("#2E7D32");       // default zeleno (aktivan)
        switch (st) {
            case AKTIVAN:   return Color.parseColor("#2E7D32");   // zelena
            case PAUZIRAN:  return Color.parseColor("#EF6C00");   // narandžasta
            case OTKAZAN:   return Color.parseColor("#C62828");   // crvena
            case URADJEN:   return Color.parseColor("#1565C0");   // plava
        }
        return Color.parseColor("#2E7D32");
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvDesc, tvWhen, tvXp, tvStatus;
        final ImageButton btnDone, btnCancel, btnPause, btnActive, btnMore;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle  = itemView.findViewById(R.id.tv_title);
            tvDesc   = itemView.findViewById(R.id.tv_desc);
            tvWhen   = itemView.findViewById(R.id.tv_when);
            tvXp     = itemView.findViewById(R.id.tv_xp);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnDone  = itemView.findViewById(R.id.btn_done);
            btnCancel= itemView.findViewById(R.id.btn_cancel);
            btnPause = itemView.findViewById(R.id.btn_pause);
            btnActive= itemView.findViewById(R.id.btn_active);
            btnMore  = itemView.findViewById(R.id.btn_more);
        }
    }
}
