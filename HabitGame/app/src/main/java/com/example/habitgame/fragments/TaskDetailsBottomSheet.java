package com.example.habitgame.fragments;

import android.os.Bundle; import android.view.*; import android.widget.TextView;
import androidx.annotation.*; import com.example.habitgame.R;
import com.example.habitgame.model.Task; import com.example.habitgame.services.TaskService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment; import com.google.android.material.button.MaterialButton;

public class TaskDetailsBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_ID="id", ARG_NAME="name", ARG_DESC="desc",
            ARG_REP="rep", ARG_WEIGHT="w", ARG_IMP="imp", ARG_XP="xp", ARG_STATUS="st";
    public static TaskDetailsBottomSheet newInstance(Task t){
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
        s.setArguments(b); return s;
    }

    private final TaskService svc = new TaskService();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.bottomsheet_task_details, c, false);

        String id = requireArguments().getString(ARG_ID);
        Task t = new Task(); t.setId(id);
        t.setName(requireArguments().getString(ARG_NAME));
        t.setDescription(requireArguments().getString(ARG_DESC));
        t.setIsRepeating(requireArguments().getBoolean(ARG_REP));
        t.setWeight(requireArguments().getString(ARG_WEIGHT));
        t.setImportance(requireArguments().getString(ARG_IMP));
        t.setXpValue(requireArguments().getInt(ARG_XP));

        TextView title = v.findViewById(R.id.tv_title);
        TextView desc = v.findViewById(R.id.tv_desc);
        TextView meta = v.findViewById(R.id.tv_meta);
        title.setText(t.getName());
        desc.setText(t.getDescription()==null? "" : t.getDescription());

        String metaTxt = (t.getIsRepeating()? getString(R.string.repeating) : getString(R.string.one_time))
                + " · " + getString(R.string.weight_s, t.getWeight())
                + " · " + getString(R.string.importance_s, t.getImportance())
                + " · " + getString(R.string.xp_s, t.getXpValue());
        meta.setText(metaTxt);

        MaterialButton bDone = v.findViewById(R.id.btn_done);
        MaterialButton bCancel = v.findViewById(R.id.btn_cancel);
        MaterialButton bPause = v.findViewById(R.id.btn_pause);
        MaterialButton bActive= v.findViewById(R.id.btn_active);

        bDone.setOnClickListener(x -> svc.markDone(t).addOnSuccessListener(a->dismiss()));
        bCancel.setOnClickListener(x -> svc.markCanceled(t).addOnSuccessListener(a->dismiss()));
        bPause.setOnClickListener(x -> svc.markPaused(t).addOnSuccessListener(a->dismiss()));
        bActive.setOnClickListener(x -> svc.markActive(t).addOnSuccessListener(a->dismiss()));

        return v;
    }
}
