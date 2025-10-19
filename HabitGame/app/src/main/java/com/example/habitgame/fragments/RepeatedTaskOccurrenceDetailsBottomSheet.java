package com.example.habitgame.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitgame.R;
import com.example.habitgame.model.RepeatedTaskOccurence;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.services.RepeatedTaskOccurrenceService;
import com.example.habitgame.services.RepeatedTaskService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RepeatedTaskOccurrenceDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_SERIES_ID = "seriesId";
    private static final String ARG_NAME = "name";
    private static final String ARG_XP = "xp";
    private static final String ARG_WHEN = "when";
    private static final String ARG_STATUS = "status";
    private static final String ARG_DESC = "desc";

    public static RepeatedTaskOccurrenceDetailsBottomSheet newInstance(RepeatedTaskOccurence oc){
        RepeatedTaskOccurrenceDetailsBottomSheet s = new RepeatedTaskOccurrenceDetailsBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_ID, oc.getId());
        b.putString(ARG_SERIES_ID, oc.getRepeatedTaskId());
        b.putString(ARG_NAME, oc.getTaskName());
        b.putInt(ARG_XP, Math.max(0, oc.getXp()));
        b.putString(ARG_DESC, oc.getTaskDescription());
        if (oc.getWhen()!=null) b.putLong(ARG_WHEN, oc.getWhen());
        b.putString(ARG_STATUS, oc.getStatus()==null? "AKTIVAN" : oc.getStatus().name());
        s.setArguments(b);
        return s;
    }

    public static RepeatedTaskOccurrenceDetailsBottomSheet newInstance(
            @Nullable String seriesId,
            @NonNull String occurrenceId,
            @Nullable String name,
            @Nullable String desc,
            int xp,
            @Nullable Long when
    ){
        RepeatedTaskOccurrenceDetailsBottomSheet s = new RepeatedTaskOccurrenceDetailsBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_ID, occurrenceId);
        b.putString(ARG_SERIES_ID, seriesId);
        b.putString(ARG_NAME, name);
        b.putInt(ARG_XP, Math.max(0, xp));
        b.putString(ARG_DESC, desc);
        if (when != null) b.putLong(ARG_WHEN, when);
        b.putString(ARG_STATUS, "AKTIVAN");
        s.setArguments(b);
        return s;
    }

    private final RepeatedTaskOccurrenceService occSvc = new RepeatedTaskOccurrenceService();
    private final RepeatedTaskService seriesSvc = new RepeatedTaskService();
    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());

    @Nullable @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inf,
                             @Nullable android.view.ViewGroup c,
                             @Nullable Bundle b) {
        View v = inf.inflate(R.layout.bottomsheet_task_details, c, false);

        String id    = requireArguments().getString(ARG_ID);
        String sid   = requireArguments().getString(ARG_SERIES_ID);
        String name  = requireArguments().getString(ARG_NAME);
        String desc  = requireArguments().getString(ARG_DESC, "");
        int xp       = requireArguments().getInt(ARG_XP, 0);
        String stStr = requireArguments().getString(ARG_STATUS, "AKTIVAN");
        Long when    = requireArguments().containsKey(ARG_WHEN)
                ? requireArguments().getLong(ARG_WHEN) : null;

        TaskStatus status;
        try { status = TaskStatus.valueOf(stStr); } catch (Exception e){ status = TaskStatus.AKTIVAN; }

        RepeatedTaskOccurence oc = new RepeatedTaskOccurence();
        oc.setId(id);
        oc.setRepeatedTaskId(sid);
        oc.setTaskName(name);
        oc.setTaskDescription(desc);
        oc.setXp(xp);
        oc.setWhen(when);
        oc.setStatus(status);

        TextView title = v.findViewById(R.id.tv_title);
        TextView tvDesc = v.findViewById(R.id.tv_desc);
        TextView meta = v.findViewById(R.id.tv_meta);

        title.setText(name == null ? getString(R.string.repeating) : name);
        tvDesc.setText(desc == null ? "" : desc);
        tvDesc.setVisibility((desc == null || desc.trim().isEmpty()) ? View.GONE : View.VISIBLE);

        StringBuilder mb = new StringBuilder();
        if (when != null) mb.append(df.format(new Date(when))).append(" · ");
        mb.append(getString(R.string.xp_s, xp)).append(" · ");
        switch (status) {
            case URADJEN:   mb.append(getString(R.string.status_done)); break;
            case OTKAZAN:   mb.append(getString(R.string.status_canceled)); break;
            case NEURADJEN: mb.append(getString(R.string.status_missed)); break;
            case PAUZIRAN:  mb.append(getString(R.string.status_paused)); break;
            default:        mb.append(getString(R.string.status_active)); break;
        }
        meta.setText(mb.toString());

        MaterialButton bDone    = v.findViewById(R.id.btn_done);
        MaterialButton bCancel  = v.findViewById(R.id.btn_cancel);
        MaterialButton bPause   = v.findViewById(R.id.btn_pause);
        MaterialButton bActive  = v.findViewById(R.id.btn_active);
        MaterialButton bEdit    = v.findViewById(R.id.btn_edit);
        MaterialButton bDelete  = v.findViewById(R.id.btn_delete);

        setEnabled(bDone,   status == TaskStatus.AKTIVAN);
        setEnabled(bCancel, status == TaskStatus.AKTIVAN);

        boolean canToggleSeries = (status == TaskStatus.AKTIVAN || status == TaskStatus.PAUZIRAN);
        bEdit.setVisibility(View.VISIBLE);
        bPause.setVisibility(View.VISIBLE);
        bActive.setVisibility(View.VISIBLE);
        setEnabled(bPause, canToggleSeries && status != TaskStatus.PAUZIRAN);
        setEnabled(bActive, canToggleSeries && status != TaskStatus.AKTIVAN);

        bDelete.setVisibility(View.VISIBLE);
        setEnabled(bDelete, true);

        bDone.setOnClickListener(x -> occSvc.markDone(oc)
                .addOnSuccessListener(a -> { Toast.makeText(getContext(), oc.getXp() + " XP", Toast.LENGTH_SHORT).show(); sendResult(); dismiss(); })
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show()));

        bCancel.setOnClickListener(x -> occSvc.markCanceled(oc)
                .addOnSuccessListener(a -> { Toast.makeText(getContext(), R.string.status_canceled, Toast.LENGTH_SHORT).show(); sendResult(); dismiss(); })
                .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show()));

        bPause.setOnClickListener(x -> {
            if (sid == null) { Toast.makeText(getContext(), "Nema ID serije.", Toast.LENGTH_LONG).show(); return; }
            seriesSvc.activateSeries(sid).addOnSuccessListener(a -> {});
            seriesSvc.pauseSeries(sid)
                    .addOnSuccessListener(a -> { Toast.makeText(getContext(), "⏸ Serija pauzirana.", Toast.LENGTH_SHORT).show(); sendResult(); dismiss(); })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        });

        bActive.setOnClickListener(x -> {
            if (sid == null) { Toast.makeText(getContext(), "Nema ID serije.", Toast.LENGTH_LONG).show(); return; }
            seriesSvc.activateSeries(sid)
                    .addOnSuccessListener(a -> { Toast.makeText(getContext(), "Serija aktivirana.", Toast.LENGTH_SHORT).show(); sendResult(); dismiss(); })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        });

        bEdit.setOnClickListener(x -> {
            if (sid == null) { Toast.makeText(getContext(), "Nema ID serije.", Toast.LENGTH_LONG).show(); return; }
            Bundle args = new Bundle();
            args.putString(RepeatedTaskEditFragment.ARG_SERIES_ID, sid);
            args.putString(RepeatedTaskEditFragment.ARG_NAME, name==null? "" : name);
            args.putString(RepeatedTaskEditFragment.ARG_DESC, desc==null? "" : desc);
            dismiss();
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.repeatedTaskEditFragment, args);
        });

        bDelete.setOnClickListener(x -> {
            occSvc.delete(oc)
                    .addOnSuccessListener(a -> { Toast.makeText(getContext(), "🗑️ Pojava obrisana.", Toast.LENGTH_SHORT).show(); sendResult(); dismiss(); })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        });

        return v;
    }

    private void sendResult() {
        Bundle res = new Bundle();
        res.putBoolean("changed", true);
        getParentFragmentManager().setFragmentResult("series_toggle", res);
    }

    private void setEnabled(@NonNull MaterialButton b, boolean enabled) {
        b.setEnabled(enabled);
        b.setClickable(enabled);
        b.setAlpha(enabled ? 1f : 0.35f);
    }
}
