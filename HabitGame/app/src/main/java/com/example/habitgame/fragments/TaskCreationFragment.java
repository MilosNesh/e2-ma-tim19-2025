package com.example.habitgame.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habitgame.R;
import com.example.habitgame.model.Category;
import com.example.habitgame.services.CategoryService;
import com.example.habitgame.services.TaskService;
import com.example.habitgame.utils.XpCalculator;

import java.text.SimpleDateFormat;
import java.util.*;

public class TaskCreationFragment extends Fragment {

    private static final String TAG = "TaskCreationFragment";

    private EditText etName, etDescription;
    private Spinner spinnerCategory, spinnerWeight, spinnerImportance;
    private Button btnPickDate, btnCreateTask;

    private List<Category> categories = new ArrayList<>();
    private Long pickedDateMs = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_creation, container, false);

        etName = v.findViewById(R.id.et_task_name);
        etDescription = v.findViewById(R.id.et_task_description);
        spinnerCategory = v.findViewById(R.id.spinner_category);
        spinnerWeight = v.findViewById(R.id.spinner_weight);
        spinnerImportance = v.findViewById(R.id.spinner_importance);

        // sakrij ponavljajuće elemente iz layouta (ako su ostali u XML-u)
        View repeatSection = v.findViewById(R.id.repeat_section);
        View oneTimeSection = v.findViewById(R.id.one_time_section);
        if (repeatSection != null) repeatSection.setVisibility(View.GONE);
        if (oneTimeSection != null) oneTimeSection.setVisibility(View.VISIBLE);

        btnPickDate = v.findViewById(R.id.btn_pick_one_time_date);
        btnCreateTask = v.findViewById(R.id.btn_create_task);

        populateStaticSpinners();
        loadCategories();

        btnPickDate.setOnClickListener(x -> pickDate(ms -> {
            pickedDateMs = ms;
            btnPickDate.setText(getString(R.string.date) + ": " + formatDate(ms));
        }));

        btnCreateTask.setOnClickListener(vw -> attemptCreateTask());

        return v;
    }

    private void populateStaticSpinners() {
        ArrayAdapter<CharSequence> weightAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_weights, android.R.layout.simple_spinner_dropdown_item);
        spinnerWeight.setAdapter(weightAdapter);

        ArrayAdapter<CharSequence> importanceAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_importance, android.R.layout.simple_spinner_dropdown_item);
        spinnerImportance.setAdapter(importanceAdapter);
    }

    private void loadCategories() {
        CategoryService.getMyCategories()
                .addOnSuccessListener(list -> {
                    categories = (list != null) ? list : new ArrayList<>();
                    if (categories.isEmpty()) {
                        spinnerCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_dropdown_item, new String[]{}));
                        Toast.makeText(getContext(), getString(R.string.no_categories), Toast.LENGTH_LONG).show();
                        return;
                    }
                    List<String> names = new ArrayList<>();
                    for (Category c : categories) names.add(c.getName());
                    ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_dropdown_item, names);
                    spinnerCategory.setAdapter(ad);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories", e);
                    Toast.makeText(getContext(), "Greška pri učitavanju kategorija.", Toast.LENGTH_SHORT).show();
                });
    }

    private void attemptCreateTask() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), getString(R.string.err_name_required), Toast.LENGTH_SHORT).show();
            return;
        }
        if (categories.isEmpty() || spinnerCategory.getSelectedItemPosition() == Spinner.INVALID_POSITION) {
            Toast.makeText(getContext(), getString(R.string.categories), Toast.LENGTH_SHORT).show();
            return;
        }
        if (pickedDateMs == null) {
            Toast.makeText(getContext(), getString(R.string.pick_date), Toast.LENGTH_SHORT).show();
            return;
        }

        Category cat = categories.get(spinnerCategory.getSelectedItemPosition());
        String weight = (String) spinnerWeight.getSelectedItem();
        String importance = (String) spinnerImportance.getSelectedItem();
        int xpValue = XpCalculator.calculateTotalXp(weight, importance);

        new TaskService()
                .createTask(
                        name, description, cat.getId(),
                        weight, importance, xpValue,
                        pickedDateMs, // executionDate
                        false, null, null, null, null // force one-time
                )
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), getString(R.string.create_task), Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ---------- helpers ----------

    private interface OnDatePicked { void onPicked(long midnightMs); }

    private void pickDate(OnDatePicked cb) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    cal.set(java.util.Calendar.YEAR, year);
                    cal.set(java.util.Calendar.MONTH, month);
                    cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    cal.set(java.util.Calendar.SECOND, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    cb.onPicked(cal.getTimeInMillis());
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
        );
        dp.setTitle(getString(R.string.pick_date));
        dp.show();
    }

    private String formatDate(long ms) {
        return new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
                .format(new java.util.Date(ms));
    }
}
