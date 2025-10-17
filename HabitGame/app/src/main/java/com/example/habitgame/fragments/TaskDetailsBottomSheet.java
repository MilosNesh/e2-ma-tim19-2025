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
            ARG_REP="rep", ARG_WEIGHT="w", ARG_IMP="imp", ARG_XP="xp", ARG_STATUS="st",
            ARG_INSTANCE_TIME="instTime";

    public static final String FR_RESULT_KEY = "task_sheet_result";
    public static final String FR_RESULT_ACTION = "action";
    public static final String FR_RESULT_TASK_ID = "taskId";

    public static TaskDetailsBottomSheet newInstance(Task t){
        return newInstance(t, null);
    }

    public static TaskDetailsBottomSheet newInstance(Task t, @Nullable Long instanceTime){
        TaskDetailsBottomSheet s = new TaskDetailsBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_ID, t.getId());
        b.putString(ARG_NAME, t.getName());
        b.putString(ARG_DESC, t.getDescription());
        b.putBoolean(ARG_REP, t.getIsRepeating());
        b.putString(ARG_WEIGHT, t.getWeight());
        b.putString(ARG_IMP, t.getImportance());
        b.putInt(ARG_XP, t.getXpValue());
        b.putString(ARG_STATUS, t.getStatus()==null? "AKTIVAN" : t.getStatus().name());
        if (instanceTime != null) b.putLong(ARG_INSTANCE_TIME, instanceTime);
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
        boolean repFlag = requireArguments().getBoolean(ARG_REP, false);
        String w    = requireArguments().getString(ARG_WEIGHT);
        String imp  = requireArguments().getString(ARG_IMP);
        int xp      = requireArguments().getInt(ARG_XP, 0);
        String st   = requireArguments().getString(ARG_STATUS, "AKTIVAN");
        Long instanceTime = requireArguments().containsKey(ARG_INSTANCE_TIME)
                ? requireArguments().getLong(ARG_INSTANCE_TIME) : null;

        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setDescription(desc);
        t.setIsRepeating(repFlag);
        t.setWeight(w);
        t.setImportance(imp);
        t.setXpValue(xp);
        try { t.setStatus(TaskStatus.valueOf(st)); } catch (Exception ignored){ t.setStatus(TaskStatus.AKTIVAN); }

        TextView title = v.findViewById(R.id.tv_title);
        TextView tvDesc = v.findViewById(R.id.tv_desc);
        TextView meta = v.findViewById(R.id.tv_meta);
        title.setText(name);
        tvDesc.setText(desc == null ? "" : desc);
        String metaTxt = (repFlag? getString(R.string.repeating) : getString(R.string.one_time))
                + " · " + getString(R.string.weight_s, (w==null?"-":w))
                + " · " + getString(R.string.importance_s, (imp==null?"-":imp))
                + " · " + getString(R.string.xp_s, xp);
        meta.setText(metaTxt);

        MaterialButton bDone = v.findViewById(R.id.btn_done);
        MaterialButton bCancel = v.findViewById(R.id.btn_cancel);
        MaterialButton bPause = v.findViewById(R.id.btn_pause);
        MaterialButton bActive= v.findViewById(R.id.btn_active);
        MaterialButton bEdit  = v.findViewById(R.id.btn_edit);
        MaterialButton bDelete= v.findViewById(R.id.btn_delete);

        TaskStatus status = (t.getStatus()==null? TaskStatus.AKTIVAN : t.getStatus());
        boolean repeating = isRepeatingLike(t) || repFlag;

        // prikaži/ sakrij dugmad specifična za ponavljajuće
        bPause.setVisibility(repeating ? android.view.View.VISIBLE : android.view.View.GONE);
        bActive.setVisibility(repeating ? android.view.View.VISIBLE : android.view.View.GONE);

        // default disable
        setEnabled(bDone,   false);
        setEnabled(bCancel, false);
        setEnabled(bPause,  false);
        setEnabled(bActive, false);

        // enable po pravilima
        if (status == TaskStatus.AKTIVAN) {
            setEnabled(bDone, true);
            setEnabled(bCancel, true);
            if (repeating) setEnabled(bPause, true);
        } else if (status == TaskStatus.PAUZIRAN && repeating) {
            setEnabled(bActive, true);
        }

        // klikovi sa feedbackom + rezultat ka parentu
        bDone.setOnClickListener(x -> svc.markDone(t)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), R.string.status_done, Toast.LENGTH_SHORT).show();
                    notifyChanged("DONE", id);
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show())
        );

        bCancel.setOnClickListener(x -> svc.markCanceled(t)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), R.string.status_canceled, Toast.LENGTH_SHORT).show();
                    notifyChanged("CANCELED", id);
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show())
        );

        bPause.setOnClickListener(x -> svc.markPaused(t)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), R.string.status_paused, Toast.LENGTH_SHORT).show();
                    notifyChanged("PAUSED", id);
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show())
        );

        bActive.setOnClickListener(x -> svc.markActive(t)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), R.string.status_active, Toast.LENGTH_SHORT).show();
                    notifyChanged("ACTIVE", id);
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
            args.putBoolean(EditTaskFragment.ARG_IS_REPEATING, repeating);
            args.putString(EditTaskFragment.ARG_STATUS, status.name());
            args.putString(EditTaskFragment.ARG_WEIGHT, w);
            args.putString(EditTaskFragment.ARG_IMPORTANCE, imp);
            dismiss();
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.editTaskFragment, args);
        });

        bDelete.setOnClickListener(x -> {
            if (status == TaskStatus.URADJEN || status == TaskStatus.OTKAZAN || status == TaskStatus.NEURADJEN) {
                Toast.makeText(getContext(), R.string.err_cannot_delete_finished, Toast.LENGTH_LONG).show();
                return;
            }
            new TaskService().deleteTask(id)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(getContext(), R.string.delete, Toast.LENGTH_SHORT).show();
                        notifyChanged("DELETED", id);
                        dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        });

        return v;
    }

    private boolean isRepeatingLike(@NonNull Task t) {
        if (Boolean.TRUE.equals(t.getIsRepeating())) return true;
        Integer ri = t.getRepeatInterval();
        String ru = t.getRepeatUnit();
        return ri != null && ri > 0 && ru != null && ru.trim().length() > 0;
    }

    private void setEnabled(@NonNull MaterialButton b, boolean enabled) {
        b.setEnabled(enabled);
        b.setClickable(enabled);
        b.setAlpha(enabled ? 1f : 0.35f);
    }

    private void notifyChanged(String action, String taskId){
        Bundle res = new Bundle();
        res.putString(FR_RESULT_ACTION, action);
        res.putString(FR_RESULT_TASK_ID, taskId);
        getParentFragmentManager().setFragmentResult(FR_RESULT_KEY, res);
    }
}
