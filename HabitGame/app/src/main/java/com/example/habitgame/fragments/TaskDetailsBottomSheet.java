package com.example.habitgame.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.habitgame.R;
import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.services.TaskService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import android.widget.Toast;

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

    // Overload za kalendar – prosleđuješ konkretan instance timestamp (start te pojave)
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
        b.putString(ARG_STATUS, t.getStatus()==null? "KREIRAN" : t.getStatus().name());
        if (instanceTime != null) b.putLong(ARG_INSTANCE_TIME, instanceTime);
        s.setArguments(b);
        return s;
    }

    private final TaskService svc = new TaskService();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.bottomsheet_task_details, c, false);

        String id   = requireArguments().getString(ARG_ID);
        String name = requireArguments().getString(ARG_NAME);
        String desc = requireArguments().getString(ARG_DESC);
        boolean rep = requireArguments().getBoolean(ARG_REP, false);
        String w    = requireArguments().getString(ARG_WEIGHT);
        String imp  = requireArguments().getString(ARG_IMP);
        int xp      = requireArguments().getInt(ARG_XP, 0);
        String st   = requireArguments().getString(ARG_STATUS, "KREIRAN");
        Long instanceTime = requireArguments().containsKey(ARG_INSTANCE_TIME)
                ? requireArguments().getLong(ARG_INSTANCE_TIME) : null;

        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setDescription(desc);
        t.setIsRepeating(rep);
        t.setWeight(w);
        t.setImportance(imp);
        t.setXpValue(xp);
        try { t.setStatus(TaskStatus.valueOf(st)); } catch (Exception ignored){ t.setStatus(TaskStatus.AKTIVAN); }

        TextView title = v.findViewById(R.id.tv_title);
        TextView tvDesc = v.findViewById(R.id.tv_desc);
        TextView meta = v.findViewById(R.id.tv_meta);
        title.setText(name);
        tvDesc.setText(desc == null ? "" : desc);
        String metaTxt = (rep? getString(R.string.repeating) : getString(R.string.one_time))
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

        // Statusne akcije
        bDone.setOnClickListener(x -> svc.markDone(t).addOnSuccessListener(a->dismiss()));
        bCancel.setOnClickListener(x -> svc.markCanceled(t).addOnSuccessListener(a->dismiss()));
        bPause.setOnClickListener(x -> svc.markPaused(t).addOnSuccessListener(a->dismiss()));
        bActive.setOnClickListener(x -> svc.markActive(t).addOnSuccessListener(a->dismiss()));

        // Edit – otvara postojeći EditTaskFragment sa istim argumentima
        bEdit.setOnClickListener(x -> {
            Bundle args = new Bundle();
            args.putString(EditTaskFragment.ARG_ID, id);
            args.putString(EditTaskFragment.ARG_NAME, name);
            args.putString(EditTaskFragment.ARG_DESC, desc);
            args.putBoolean(EditTaskFragment.ARG_IS_REPEATING, rep);
            args.putString(EditTaskFragment.ARG_STATUS, st);
            args.putString(EditTaskFragment.ARG_WEIGHT, w);
            args.putString(EditTaskFragment.ARG_IMPORTANCE, imp);
            // prosleđivanje vremena – ako ti treba za jednokratne popunu datuma:
            // args.putLong(EditTaskFragment.ARG_EXECUTION_TIME, ... );
            // args.putLong(EditTaskFragment.ARG_START_DATE, ... );

            dismiss();
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.editTaskFragment, args);
        });

        // Delete
        bDelete.setOnClickListener(x -> {
            if (t.getStatus() == TaskStatus.URADJEN) {
                Toast.makeText(getContext(), R.string.err_cannot_delete_finished, Toast.LENGTH_LONG).show();
                return;
            }
            if (!t.getIsRepeating()) {
                svc.deleteTaskOneTime(t)
                        .addOnSuccessListener(a -> {
                            notifyChanged("DELETED", id);
                            dismiss();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                svc.deleteTaskFutureOccurrences(t, instanceTime)
                        .addOnSuccessListener(a -> {
                            notifyChanged("CUT_FUTURE", id);
                            dismiss();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });

        return v;
    }

    private void notifyChanged(String action, String taskId){
        Bundle res = new Bundle();
        res.putString(FR_RESULT_ACTION, action);
        res.putString(FR_RESULT_TASK_ID, taskId);
        getParentFragmentManager().setFragmentResult(FR_RESULT_KEY, res);
    }
}
