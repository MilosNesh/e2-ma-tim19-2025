package com.example.habitgame.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.habitgame.R;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.repositories.TaskRepository;
import com.example.habitgame.utils.DateUtils;
import com.example.habitgame.utils.XpCalculator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditTaskFragment extends Fragment {

    // args
    public static final String ARG_ID            = "id";
    public static final String ARG_NAME          = "name";
    public static final String ARG_DESC          = "desc";
    public static final String ARG_IS_REPEATING  = "isRep";
    public static final String ARG_EXECUTION_TIME= "execTime";   // jednokratni
    public static final String ARG_START_DATE    = "startDate";  // ponavljajući
    public static final String ARG_STATUS        = "status";
    public static final String ARG_WEIGHT        = "weight";
    public static final String ARG_IMPORTANCE    = "importance";

    private TextInputEditText etName, etDesc, etDate;
    private Spinner spWeight, spImportance;
    private MaterialButton btnPickDate, btnSave, btnCancel;

    private String taskId;
    private boolean isRepeating;
    private TaskStatus status;
    private Long baseWhenMs;       // prikazani datum (bez menjanja vremena)

    private final Calendar chosenCal = Calendar.getInstance(); // držimo i vreme, ali vreme ne menjamo kroz UI

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_edit_task, c, false);

        etName = v.findViewById(R.id.et_task_name);
        etDesc = v.findViewById(R.id.et_task_desc);
        etDate = v.findViewById(R.id.et_task_date);

        spWeight = v.findViewById(R.id.sp_weight);
        spImportance = v.findViewById(R.id.sp_importance);

        btnPickDate = v.findViewById(R.id.btn_pick_date);
        btnSave = v.findViewById(R.id.btn_save);
        btnCancel = v.findViewById(R.id.btn_cancel);

        // Spinners (za jednokratne)
        ArrayAdapter<CharSequence> wAd = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_weights, android.R.layout.simple_spinner_dropdown_item);
        spWeight.setAdapter(wAd);
        ArrayAdapter<CharSequence> iAd = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_importance, android.R.layout.simple_spinner_dropdown_item);
        spImportance.setAdapter(iAd);

        readArgsAndFill();

        // Ako je ponavljajući: sakrij sve što nije dozvoljeno (datum i spinnere)
        if (isRepeating) {
            v.findViewById(R.id.group_date_row).setVisibility(View.GONE);
            v.findViewById(R.id.group_weight_row).setVisibility(View.GONE);
            v.findViewById(R.id.group_importance_row).setVisibility(View.GONE);
        }

        // Ako je završeni/otkazani: zabrani save
        if (status == TaskStatus.URADJEN || status == TaskStatus.OTKAZAN) {
            btnSave.setEnabled(false);
            Toast.makeText(getContext(), R.string.err_cannot_edit_finished, Toast.LENGTH_LONG).show();
        }

        btnPickDate.setOnClickListener(v1 -> showDatePicker());
        btnSave.setOnClickListener(v12 -> onSave());
        btnCancel.setOnClickListener(v13 -> requireActivity().onBackPressed());

        return v;
    }

    private void readArgsAndFill() {
        Bundle a = requireArguments();

        taskId      = a.getString(ARG_ID);
        isRepeating = a.getBoolean(ARG_IS_REPEATING, false);

        // status – NEDOSTAJALO ranije
        String st = a.getString(ARG_STATUS, TaskStatus.AKTIVAN.name());
        try { status = TaskStatus.valueOf(st); } catch (Exception e) { status = TaskStatus.AKTIVAN; }

        etName.setText(a.getString(ARG_NAME, ""));
        etDesc.setText(a.getString(ARG_DESC, ""));

        // Odredi bazni timestamp (zadržavamo HH:mm postojeće)
        Long exec = a.containsKey(ARG_EXECUTION_TIME) ? a.getLong(ARG_EXECUTION_TIME) : null;
        Long start= a.containsKey(ARG_START_DATE) ? a.getLong(ARG_START_DATE) : null;
        baseWhenMs = (exec != null ? exec : start);
        if (baseWhenMs == null) baseWhenMs = System.currentTimeMillis();

        chosenCal.setTimeInMillis(DateUtils.ensureMillis(baseWhenMs));
        refreshDateField();

        // postavi spinnere samo za jednokratne
        if (!isRepeating) {
            String w = a.getString(ARG_WEIGHT, null);
            if (w != null) selectSpinner(spWeight, w);
            String imp = a.getString(ARG_IMPORTANCE, null);
            if (imp != null) selectSpinner(spImportance, imp);
        }
    }

    private void refreshDateField() {
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        etDate.setText(df.format(chosenCal.getTime()));
    }

    private void showDatePicker() {
        // menja se samo datum (NE vreme)
        MaterialDatePicker<Long> dp = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.pick_date)
                .setSelection(chosenCal.getTimeInMillis())
                .build();
        dp.addOnPositiveButtonClickListener(sel -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(sel);
            // zadrži HH:mm iz chosenCal
            int hh = chosenCal.get(Calendar.HOUR_OF_DAY);
            int mm = chosenCal.get(Calendar.MINUTE);
            c.set(Calendar.HOUR_OF_DAY, hh);
            c.set(Calendar.MINUTE, mm);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            chosenCal.setTimeInMillis(c.getTimeInMillis());
            refreshDateField();
        });
        dp.show(getParentFragmentManager(), "datePicker");
    }

    private void onSave() {
        if (status == TaskStatus.URADJEN || status == TaskStatus.OTKAZAN) {
            Toast.makeText(getContext(), R.string.err_cannot_edit_finished, Toast.LENGTH_LONG).show();
            return;
        }

        String name = safe(etName.getText());
        String desc = safe(etDesc.getText());
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), R.string.err_name_required, Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", desc);

        if (!isRepeating) {
            // jednokratni: dozvoljeno menjati datum (NE vreme), weight & importance
            String weight = (String) spWeight.getSelectedItem();
            String importance = (String) spImportance.getSelectedItem();

            // upiši weight/importance
            updates.put("weight", weight);
            updates.put("importance", importance);
            // datum (sa zadržanim vremenom)
            updates.put("executionTime", chosenCal.getTimeInMillis());

            // >>> PRERAČUNAJ I UPIŠI XP <<<
            int newXp = XpCalculator.calculateTotalXp(weight, importance);
            updates.put("xpValue", newXp);
        } else {
            // ponavljajući: menja se samo name/description (sve pojave)
            // Nema weight/importance ni datuma
        }

        TaskRepository.updateFields(taskId, updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), R.string.task_updated, Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private static void selectSpinner(Spinner sp, String text) {
        if (text == null) return;
        ArrayAdapter<?> ad = (ArrayAdapter<?>) sp.getAdapter();
        if (ad == null) return;
        for (int i = 0; i < ad.getCount(); i++) {
            if (text.equalsIgnoreCase(String.valueOf(ad.getItem(i)))) {
                sp.setSelection(i);
                return;
            }
        }
    }

    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
