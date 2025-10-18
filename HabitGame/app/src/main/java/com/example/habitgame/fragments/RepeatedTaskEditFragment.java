package com.example.habitgame.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habitgame.R;
import com.example.habitgame.repositories.RepeatedTaskRepository;
import com.example.habitgame.services.RepeatedTaskService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RepeatedTaskEditFragment extends Fragment {

    public static final String ARG_SERIES_ID = "seriesId";
    public static final String ARG_NAME      = "name";
    public static final String ARG_DESC      = "desc";

    private TextInputEditText etName, etDesc;
    private MaterialButton btnSave, btnCancel;

    private String seriesId;

    private final RepeatedTaskService service = new RepeatedTaskService();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c,
                             @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_repeated_task_edit, c, false);

        etName  = v.findViewById(R.id.et_task_name);
        etDesc  = v.findViewById(R.id.et_task_description);
        btnSave = v.findViewById(R.id.btn_save);
        btnCancel = v.findViewById(R.id.btn_cancel);

        // args
        Bundle a = requireArguments();
        seriesId = a.getString(ARG_SERIES_ID, null);
        String nameArg = a.getString(ARG_NAME, "");
        String descArg = a.getString(ARG_DESC, "");

        // inicijalni prikaz (ako su stigli kroz args)
        etName.setText(nameArg == null ? "" : nameArg);
        etDesc.setText(descArg == null ? "" : descArg);

        // Ako opis NIJE prosleđen kroz args, dovuci ga iz baze (real-time iz serije)
        if (TextUtils.isEmpty(descArg) && !TextUtils.isEmpty(seriesId)) {
            RepeatedTaskRepository.getById(seriesId)
                    .addOnSuccessListener(rt -> {
                        if (rt != null) {
                            // postavi ime (ako je args ime prazno ili staro)
                            if (TextUtils.isEmpty(nameArg)) {
                                etName.setText(rt.getName() == null ? "" : rt.getName());
                            }
                            // postavi opis (ispravka tipfelera: rt.getDescription())
                            if (etDesc.getText() != null && TextUtils.isEmpty(etDesc.getText())) {
                                etDesc.setText(rt.getDescription() == null ? "" : rt.getDescription());
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        }

        btnSave.setOnClickListener(x -> onSave());
        btnCancel.setOnClickListener(x ->
                NavHostFragment.findNavController(this).popBackStack());

        return v;
    }

    private void onSave() {
        String name = safe(etName.getText());
        String desc = safe(etDesc.getText());

        if (TextUtils.isEmpty(seriesId)) {
            Toast.makeText(getContext(), "Nema ID serije.", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), R.string.err_name_required, Toast.LENGTH_LONG).show();
            return;
        }

        btnSave.setEnabled(false);
        service.updateSeriesNameDesc(seriesId, name, TextUtils.isEmpty(desc) ? null : desc)
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(), R.string.task_updated, Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
