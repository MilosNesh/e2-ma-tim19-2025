package com.example.habitgame.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Kreiranje zadatka:
 * - Jednokratni: bira se SAMO datum (bez vremena) -> executionDate = 00:00 tog dana.
 * - Ponavljajući: start/end su datumi (bez vremena).
 */
public class TaskCreationFragment extends Fragment {

    private static final String TAG = "TaskCreationFragment";

    // UI
    private EditText etName, etDescription;
    private Spinner spinnerCategory, spinnerWeight, spinnerImportance;
    private CheckBox cbIsRepeating;
    private Button btnCreateTask;

    // One-time UI
    private View oneTimeSection;
    private Button btnPickOneTimeDate;

    // Repeat UI
    private View repeatSection;
    private Spinner spinnerRepeatInterval, spinnerRepeatUnit;
    private Button btnPickStart, btnPickEnd;

    // Data
    private List<Category> categories = new ArrayList<>();
    private Long pickedOneTimeDateMs = null; // jednokratni datum (00:00)
    private Long pickedStartDateMs = null;   // ponavljajući start (00:00)
    private Long pickedEndDateMs = null;     // ponavljajući end (00:00)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_task_creation, container, false);

        initializeViews(view);
        hookUi();
        populateStaticSpinners();
        loadCategories();

        btnCreateTask.setOnClickListener(v -> attemptCreateTask());

        return view;
    }

    private void initializeViews(View v) {
        etName = v.findViewById(R.id.et_task_name);
        etDescription = v.findViewById(R.id.et_task_description);
        spinnerCategory = v.findViewById(R.id.spinner_category);
        spinnerWeight = v.findViewById(R.id.spinner_weight);
        spinnerImportance = v.findViewById(R.id.spinner_importance);
        cbIsRepeating = v.findViewById(R.id.cb_is_repeating);
        btnCreateTask = v.findViewById(R.id.btn_create_task);

        // One-time
        oneTimeSection = v.findViewById(R.id.one_time_section);
        btnPickOneTimeDate = v.findViewById(R.id.btn_pick_one_time_date);

        // Repeat
        repeatSection = v.findViewById(R.id.repeat_section);
        spinnerRepeatInterval = v.findViewById(R.id.spinner_repeat_interval);
        spinnerRepeatUnit = v.findViewById(R.id.spinner_repeat_unit);
        btnPickStart = v.findViewById(R.id.btn_pick_start);
        btnPickEnd = v.findViewById(R.id.btn_pick_end);
    }

    private void hookUi() {
        // Prikaz/sakrivanje sekcija u zavisnosti od "Ponavljajući"
        cbIsRepeating.setOnCheckedChangeListener((buttonView, isChecked) -> {
            repeatSection.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            oneTimeSection.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        // Default: jednokratni (sekcija vidljiva, ponavljanje sakriveno)
        repeatSection.setVisibility(View.GONE);
        oneTimeSection.setVisibility(View.VISIBLE);

        // Pickeri — SVI setuju 00:00 tog dana.
        btnPickOneTimeDate.setOnClickListener(v -> pickDate(ms -> {
            pickedOneTimeDateMs = ms;
            btnPickOneTimeDate.setText(getString(R.string.date) + ": " + formatDate(ms));
        }));

        btnPickStart.setOnClickListener(v -> pickDate(ms -> {
            pickedStartDateMs = ms;
            btnPickStart.setText(getString(R.string.date) + ": " + formatDate(ms));
            if (pickedEndDateMs != null && pickedEndDateMs < pickedStartDateMs) {
                pickedEndDateMs = null;
                btnPickEnd.setText(getString(R.string.odaberi_datum_zavrsetka));
            }
        }));

        btnPickEnd.setOnClickListener(v -> pickDate(ms -> {
            pickedEndDateMs = ms;
            btnPickEnd.setText(getString(R.string.date) + ": " + formatDate(ms));
        }));
    }

    private interface OnDatePicked { void onPicked(long midnightMs); }

    private void pickDate(OnDatePicked cb) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    cal.set(java.util.Calendar.YEAR, year);
                    cal.set(java.util.Calendar.MONTH, month);
                    cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
                    // NORMALIZUJ NA 00:00 (bez vremena)
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

    private void populateStaticSpinners() {
        // Težina
        ArrayAdapter<CharSequence> weightAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_weights, android.R.layout.simple_spinner_dropdown_item);
        spinnerWeight.setAdapter(weightAdapter);

        // Bitnost
        ArrayAdapter<CharSequence> importanceAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_importance, android.R.layout.simple_spinner_dropdown_item);
        spinnerImportance.setAdapter(importanceAdapter);

        // Interval ponavljanja
        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.repeat_intervals, android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeatInterval.setAdapter(intervalAdapter);

        // Jedinica ponavljanja (dan/nedelja)
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.repeat_units, android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeatUnit.setAdapter(unitAdapter);
    }

    private void loadCategories() {
        CategoryService.getMyCategories()
                .addOnSuccessListener(list -> {
                    categories = (list != null) ? list : new ArrayList<>();
                    setupCategorySpinner();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories", e);
                    Toast.makeText(getContext(), "Greška pri učitavanju kategorija.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupCategorySpinner() {
        if (categories.isEmpty()) {
            spinnerCategory.setAdapter(new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{}));
            Toast.makeText(getContext(), getString(R.string.no_categories), Toast.LENGTH_LONG).show();
            return;
        }
        List<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, names);
        spinnerCategory.setAdapter(adapter);
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

        int catIndex = spinnerCategory.getSelectedItemPosition();
        Category selectedCategory = categories.get(catIndex);
        if (selectedCategory.getId() == null) {
            Toast.makeText(getContext(), getString(R.string.categories), Toast.LENGTH_SHORT).show();
            return;
        }

        String weight = (String) spinnerWeight.getSelectedItem();
        String importance = (String) spinnerImportance.getSelectedItem();
        boolean isRepeating = cbIsRepeating.isChecked();

        int xpValue = XpCalculator.calculateTotalXp(weight, importance);

        // Datumi / ponavljanje — SVE bez vremena (00:00)
        Integer repeatInterval = null;
        String repeatUnit = null;
        Long startDate = null;
        Long endDate = null;
        Long executionDate = null;

        if (isRepeating) {
            if (pickedStartDateMs == null) {
                Toast.makeText(getContext(), getString(R.string.pick_date), Toast.LENGTH_SHORT).show();
                return;
            }
            String intervalStr = (String) spinnerRepeatInterval.getSelectedItem();
            String unitStr = (String) spinnerRepeatUnit.getSelectedItem();
            if (TextUtils.isEmpty(intervalStr) || TextUtils.isEmpty(unitStr)) {
                Toast.makeText(getContext(), getString(R.string.repeating), Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                repeatInterval = Math.max(1, Integer.parseInt(intervalStr));
            } catch (NumberFormatException nfe) {
                Toast.makeText(getContext(), getString(R.string.repeating), Toast.LENGTH_SHORT).show();
                return;
            }

            repeatUnit = unitStr.toLowerCase(Locale.ROOT).trim(); // "dan" ili "nedelja"
            startDate = pickedStartDateMs; // 00:00
            endDate = pickedEndDateMs;     // može biti null
            executionDate = startDate;     // referentni prvi termin

            if (endDate != null && endDate < startDate) {
                Toast.makeText(getContext(), getString(R.string.odaberi_datum_zavrsetka), Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (pickedOneTimeDateMs == null) {
                Toast.makeText(getContext(), getString(R.string.pick_date), Toast.LENGTH_SHORT).show();
                return;
            }
            executionDate = pickedOneTimeDateMs; // već 00:00
        }

        new TaskService()
                .createTask(
                        name, description, selectedCategory.getId(),
                        weight, importance, xpValue, executionDate,
                        isRepeating, startDate, endDate,
                        repeatInterval, repeatUnit
                )
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), getString(R.string.create_task), Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Task creation failed.", e);
                    Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ————————————— helpers —————————————

    private String formatDate(long ms) {
        return new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
                .format(new java.util.Date(ms));
    }
}
