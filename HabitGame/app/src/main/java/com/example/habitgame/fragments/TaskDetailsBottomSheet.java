package com.example.habitgame.fragments;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitgame.R;
import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.services.TaskService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class TaskDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ID="id", ARG_NAME="name", ARG_DESC="desc",
            ARG_WEIGHT="w", ARG_IMP="imp", ARG_XP="xp", ARG_STATUS="st";

    public static TaskDetailsBottomSheet newInstance(Task t){
        TaskDetailsBottomSheet s = new TaskDetailsBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_ID, t.getId());
        b.putString(ARG_NAME, t.getName());
        b.putString(ARG_DESC, t.getDescription());
        b.putString(ARG_WEIGHT, t.getWeight());
        b.putString(ARG_IMP, t.getImportance());
        b.putInt(ARG_XP, t.getXpValue());
        b.putString(ARG_STATUS, t.getStatus()==null? "AKTIVAN" : t.getStatus().name());
        s.setArguments(b);
        return s;
    }

    private final TaskService svc = new TaskService();

    @Nullable @Override
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inf,
                                          @Nullable android.view.ViewGroup c,
                                          @Nullable Bundle b) {
        android.view.View v = inf.inflate(R.layout.bottomsheet_task_details, c, false);

        String id   = requireArguments().getString(ARG_ID);
        String name = requireArguments().getString(ARG_NAME);
        String desc = requireArguments().getString(ARG_DESC);
        String w    = requireArguments().getString(ARG_WEIGHT);
        String imp  = requireArguments().getString(ARG_IMP);
        int xp      = requireArguments().getInt(ARG_XP, 0);
        String st   = requireArguments().getString(ARG_STATUS, "AKTIVAN");

        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setDescription(desc);
        t.setWeight(w);
        t.setImportance(imp);
        t.setXpValue(xp);
        try { t.setStatus(TaskStatus.valueOf(st)); } catch (Exception ignored){ t.setStatus(TaskStatus.AKTIVAN); }

        TextView title = v.findViewById(R.id.tv_title);
        TextView tvDesc = v.findViewById(R.id.tv_desc);
        TextView meta = v.findViewById(R.id.tv_meta);
        title.setText(name);
        tvDesc.setText(desc == null ? "" : desc);
        String metaTxt = getString(R.string.one_time)
                + " · " + getString(R.string.weight_s, (w==null?"-":w))
                + " · " + getString(R.string.importance_s, (imp==null?"-":imp))
                + " · " + getString(R.string.xp_s, xp);
        meta.setText(metaTxt);

        MaterialButton bDone = v.findViewById(R.id.btn_done);
        MaterialButton bCancel = v.findViewById(R.id.btn_cancel);
        MaterialButton bEdit  = v.findViewById(R.id.btn_edit);
        MaterialButton bDelete= v.findViewById(R.id.btn_delete);

        v.findViewById(R.id.btn_pause).setVisibility(android.view.View.GONE);
        v.findViewById(R.id.btn_active).setVisibility(android.view.View.GONE);

        TaskStatus status = (t.getStatus()==null? TaskStatus.AKTIVAN : t.getStatus());

        setEnabled(bDone,   status == TaskStatus.AKTIVAN);
        setEnabled(bCancel, status == TaskStatus.AKTIVAN);

        bDone.setOnClickListener(x -> svc.markDone(t)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), R.string.status_done, Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show())
        );

        bCancel.setOnClickListener(x -> svc.markCanceled(t)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), R.string.status_canceled, Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show())
        );

        bEdit.setOnClickListener(x -> {
            Bundle args = new Bundle();
            args.putString(EditTaskFragment.ARG_ID, id);
            args.putString(EditTaskFragment.ARG_NAME, name);
            args.putString(EditTaskFragment.ARG_DESC, desc);
            args.putString(EditTaskFragment.ARG_STATUS, status.name());
            args.putString(EditTaskFragment.ARG_WEIGHT, w);
            args.putString(EditTaskFragment.ARG_IMPORTANCE, imp);
            dismiss();
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.editTaskFragment, args);
        });

        bDelete.setOnClickListener(x -> new TaskService().deleteTask(id)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), R.string.delete, Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show())
        );

        return v;
    }

    private void setEnabled(@NonNull MaterialButton b, boolean enabled) {
        b.setEnabled(enabled);
        b.setClickable(enabled);
        b.setAlpha(enabled ? 1f : 0.35f);
    }
}
