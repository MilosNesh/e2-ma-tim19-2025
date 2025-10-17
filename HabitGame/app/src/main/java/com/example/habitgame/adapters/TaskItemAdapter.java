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
    private final SimpleDateFormat dateDf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

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
                            && o.getIsRepeating() == n.getIsRepeating()
                            && eq(o.getRepeatInterval(), n.getRepeatInterval())
                            && eq(o.getRepeatUnit(), n.getRepeatUnit())
                            && eq(o.getCategoryId(), n.getCategoryId());
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

        TaskStatus status = (t.getStatus() == null ? TaskStatus.AKTIVAN : t.getStatus());
        String label = getStatusLabel(h, status);
        int color = getStatusColor(status);
        h.tvStatus.setText(label);
        h.tvStatus.setTextColor(color);

        h.itemView.setOnClickListener(v -> listener.onOpen(t));
        h.btnMore.setOnClickListener(v -> listener.onOpen(t));
        h.btnDone.setOnClickListener(v -> listener.onDone(t));
        h.btnCancel.setOnClickListener(v -> listener.onCancel(t));
        h.btnPause.setOnClickListener(v -> listener.onPause(t));
        h.btnActive.setOnClickListener(v -> listener.onActive(t));

        applyButtonsState(h, t, status);
    }

    private void applyButtonsState(@NonNull VH h, @NonNull Task t, @NonNull TaskStatus st) {
        boolean repeating = isRepeatingLike(t);

        // uvek prikaz za ponavljajuće; sakrij na one-time
        h.btnPause.setVisibility(repeating ? View.VISIBLE : View.GONE);
        h.btnActive.setVisibility(repeating ? View.VISIBLE : View.GONE);

        setEnabled(h.btnDone,   false);
        setEnabled(h.btnCancel, false);
        setEnabled(h.btnPause,  false);
        setEnabled(h.btnActive, false);

        // završeni/otkazani/neurađeni -> lock
        if (st == TaskStatus.URADJEN || st == TaskStatus.OTKAZAN || st == TaskStatus.NEURADJEN) return;

        if (st == TaskStatus.AKTIVAN) {
            setEnabled(h.btnDone, true);
            setEnabled(h.btnCancel, true);
            if (repeating) setEnabled(h.btnPause, true);
            return;
        }

        if (st == TaskStatus.PAUZIRAN && repeating) {
            setEnabled(h.btnActive, true);
            return;
        }
    }

    private boolean isRepeatingLike(@NonNull Task t) {
        if (Boolean.TRUE.equals(t.getIsRepeating())) return true;
        Integer ri = t.getRepeatInterval();
        String ru = t.getRepeatUnit();
        return ri != null && ri > 0 && ru != null && ru.trim().length() > 0;
    }

    private void setEnabled(View v, boolean enabled) {
        v.setEnabled(enabled);
        v.setClickable(enabled);
        v.setAlpha(enabled ? 1f : 0.35f);
        v.bringToFront();
    }

    private String getStatusLabel(@NonNull VH h, TaskStatus st) {
        switch (st) {
            case AKTIVAN:   return h.itemView.getContext().getString(R.string.status_active);
            case PAUZIRAN:  return h.itemView.getContext().getString(R.string.status_paused);
            case OTKAZAN:   return h.itemView.getContext().getString(R.string.status_canceled);
            case URADJEN:   return h.itemView.getContext().getString(R.string.status_done);
            case NEURADJEN: return h.itemView.getContext().getString(R.string.status_missed);
        }
        return h.itemView.getContext().getString(R.string.status_active);
    }

    private int getStatusColor(TaskStatus st) {
        switch (st) {
            case AKTIVAN:   return Color.parseColor("#2E7D32");
            case PAUZIRAN:  return Color.parseColor("#EF6C00");
            case OTKAZAN:   return Color.parseColor("#C62828");
            case URADJEN:   return Color.parseColor("#1565C0");
            case NEURADJEN: return Color.parseColor("#6D4C41");
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
