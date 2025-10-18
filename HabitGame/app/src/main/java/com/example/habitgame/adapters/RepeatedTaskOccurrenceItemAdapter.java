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
import com.example.habitgame.model.RepeatedTaskOccurence;
import com.example.habitgame.model.TaskStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class RepeatedTaskOccurrenceItemAdapter
        extends ListAdapter<RepeatedTaskOccurence, RepeatedTaskOccurrenceItemAdapter.VH> {

    public interface Listener {
        void onOpen(RepeatedTaskOccurence o);
        void onDone(RepeatedTaskOccurence o);
        void onCancel(RepeatedTaskOccurence o);

        void onMarkDone(RepeatedTaskOccurence occ);
        void onOpenDetails(RepeatedTaskOccurence occ);
    }

    private final Listener listener;
    private final SimpleDateFormat dateDf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    public RepeatedTaskOccurrenceItemAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(false);
    }

    private static final DiffUtil.ItemCallback<RepeatedTaskOccurence> DIFF =
            new DiffUtil.ItemCallback<RepeatedTaskOccurence>() {
                @Override
                public boolean areItemsTheSame(@NonNull RepeatedTaskOccurence a,
                                               @NonNull RepeatedTaskOccurence b) {
                    return a.getId() != null && a.getId().equals(b.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull RepeatedTaskOccurence a,
                                                  @NonNull RepeatedTaskOccurence b) {
                    return Objects.equals(a.getTaskName(), b.getTaskName())
                            && Objects.equals(a.getTaskDescription(), b.getTaskDescription())
                            && Objects.equals(a.getWhen(), b.getWhen())
                            && Objects.equals(a.getStatus(), b.getStatus())
                            && a.getXp() == b.getXp()
                            && a.isCompleted() == b.isCompleted()
                            && a.isSeriesPaused() == b.isSeriesPaused();
                }
            };

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RepeatedTaskOccurence o = getItem(position);

        h.tvTitle.setText(o.getTaskName() == null ? "" : o.getTaskName());
        String dsc = o.getTaskDescription();
        h.tvDesc.setText(dsc == null ? "" : dsc);
        h.tvDesc.setVisibility(dsc == null || dsc.trim().isEmpty() ? View.GONE : View.VISIBLE);

        h.tvXp.setText(h.itemView.getContext().getString(R.string.xp_s, o.getXp()));

        Long when = o.getWhen();
        h.tvWhen.setText(when != null ? dateDf.format(new Date(when)) : "");

        TaskStatus status = (o.getStatus() == null ? TaskStatus.AKTIVAN : o.getStatus());
        h.tvStatus.setText(getStatusLabel(h, status));
        h.tvStatus.setTextColor(getStatusColor(status));

        // Klik – otvori detalje (bottom sheet)
        h.itemView.setOnClickListener(v -> listener.onOpenDetails(o));
        h.btnMore.setOnClickListener(v -> listener.onOpenDetails(o));

        // Done/Cancel samo kad je AKTIVAN
        setEnabled(h.btnDone,   false);
        setEnabled(h.btnCancel, false);
        if (status == TaskStatus.AKTIVAN) {
            setEnabled(h.btnDone, true);
            setEnabled(h.btnCancel, true);
        }
        h.btnDone.setOnClickListener(v -> listener.onDone(o));
        h.btnCancel.setOnClickListener(v -> listener.onCancel(o));

        // Pauza/Aktiviraj se rade na nivou serije – sakrij
        h.btnPause.setVisibility(View.GONE);
        h.btnActive.setVisibility(View.GONE);
    }

    private void setEnabled(View v, boolean enabled) {
        v.setEnabled(enabled);
        v.setClickable(enabled);
        v.setAlpha(enabled ? 1f : 0.35f);
    }

    private String getStatusLabel(@NonNull VH h, TaskStatus st) {
        switch (st) {
            case AKTIVAN:   return h.itemView.getContext().getString(R.string.status_active);
            case OTKAZAN:   return h.itemView.getContext().getString(R.string.status_canceled);
            case URADJEN:   return h.itemView.getContext().getString(R.string.status_done);
            case NEURADJEN: return h.itemView.getContext().getString(R.string.status_missed);
            case PAUZIRAN:  return h.itemView.getContext().getString(R.string.status_paused);
        }
        return h.itemView.getContext().getString(R.string.status_active);
    }

    private int getStatusColor(TaskStatus st) {
        switch (st) {
            case AKTIVAN:   return Color.parseColor("#2E7D32");
            case OTKAZAN:   return Color.parseColor("#C62828");
            case URADJEN:   return Color.parseColor("#1565C0");
            case NEURADJEN: return Color.parseColor("#6D4C41");
            case PAUZIRAN:  return Color.parseColor("#EF6C00");
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
