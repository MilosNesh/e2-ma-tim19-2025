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

public class TaskCreationFragment extends Fragment {

    private static final String TAG = "TaskCreationFragment";

    // UI
    private EditText etName, etDescription;
    private Spinner spinnerCategory, spinnerWeight, spinnerImportance;
    private CheckBox cbIsRepeating;
    private Button btnCreateTask;

    // Repeat UI
    private View repeatSection;
    private Spinner spinnerRepeatInterval, spinnerRepeatUnit;
    private Button btnPickStart, btnPickEnd;

    // Data
    private List<Category> categories = new ArrayList<>();
    private Long pickedStartDateMs = null;
    private Long pickedEndDateMs = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_task_creation, container, false);

        initializeViews(view);
        hookRepeatingUi();
        populateStaticSpinners();
        loadCategories();

        btnCreateTask.setOnClickListener(v -> attemptCreateTask());

        return view;
    }

    private void initializeViews(View view) {
        etName = view.findViewById(R.id.et_task_name);
        etDescription = view.findViewById(R.id.et_task_description);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerWeight = view.findViewById(R.id.spinner_weight);
        spinnerImportance = view.findViewById(R.id.spinner_importance);
        cbIsRepeating = view.findViewById(R.id.cb_is_repeating);
        btnCreateTask = view.findViewById(R.id.btn_create_task);

        repeatSection = view.findViewById(R.id.repeat_section);
        spinnerRepeatInterval = view.findViewById(R.id.spinner_repeat_interval);
        spinnerRepeatUnit = view.findViewById(R.id.spinner_repeat_unit);
        btnPickStart = view.findViewById(R.id.btn_pick_start);
        btnPickEnd = view.findViewById(R.id.btn_pick_end);
    }

    private void hookRepeatingUi() {
        // Prikaži/sakrij ponavljajuću sekciju
        cbIsRepeating.setOnCheckedChangeListener((buttonView, isChecked) ->
                repeatSection.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // Date pickeri
        btnPickStart.setOnClickListener(v -> pickDate(true));
        btnPickEnd.setOnClickListener(v -> pickDate(false));
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
            // Pokaži prazan adapter ali upozori korisnika
            spinnerCategory.setAdapter(new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{}));
            Toast.makeText(getContext(), "Kreirajte kategoriju pre dodavanja Taska.", Toast.LENGTH_LONG).show();
            return;
        }

        // Prikazujemo nazive istim redosledom kao lista kategorija → index mape na kategoriju
        List<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, names);
        spinnerCategory.setAdapter(adapter);
    }

    private void attemptCreateTask() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description)) {
            Toast.makeText(getContext(), "Popunite ime i opis zadatka.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (categories.isEmpty() || spinnerCategory.getSelectedItemPosition() == Spinner.INVALID_POSITION) {
            Toast.makeText(getContext(), "Odaberite kategoriju.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sigurno uzmi kategoriju po indexu (bez poređenja po imenu)
        int catIndex = spinnerCategory.getSelectedItemPosition();
        Category selectedCategory = categories.get(catIndex);
        if (selectedCategory.getId() == null) {
            Toast.makeText(getContext(), "Kategorija nema ID. Pokušajte ponovo.", Toast.LENGTH_SHORT).show();
            return;
        }

        String weight = (String) spinnerWeight.getSelectedItem();
        String importance = (String) spinnerImportance.getSelectedItem();
        boolean isRepeating = cbIsRepeating.isChecked();

        int xpValue = XpCalculator.calculateTotalXp(weight, importance);

        // Datumi / ponavljanje
        Integer repeatInterval = null;
        String repeatUnit = null;
        Long startDate = null;
        Long endDate = null;
        Long executionTime = null;

        if (isRepeating) {
            if (pickedStartDateMs == null) {
                Toast.makeText(getContext(), "Odaberite datum početka za ponavljajući task.", Toast.LENGTH_SHORT).show();
                return;
            }
            String intervalStr = (String) spinnerRepeatInterval.getSelectedItem();
            String unitStr = (String) spinnerRepeatUnit.getSelectedItem();
            if (TextUtils.isEmpty(intervalStr) || TextUtils.isEmpty(unitStr)) {
                Toast.makeText(getContext(), "Odaberite interval i jedinicu ponavljanja.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                repeatInterval = Math.max(1, Integer.parseInt(intervalStr));
            } catch (NumberFormatException nfe) {
                Toast.makeText(getContext(), "Neispravan interval.", Toast.LENGTH_SHORT).show();
                return;
            }

            repeatUnit = unitStr.toLowerCase(Locale.ROOT).trim(); // "dan" ili "nedelja"
            startDate = pickedStartDateMs;
            endDate = pickedEndDateMs; // može ostati null
            executionTime = startDate; // referentni prvi termin

            if (endDate != null && endDate < startDate) {
                Toast.makeText(getContext(), "Datum završetka ne može biti pre početka.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // Neponavljajući — ako nema posebnog pickera, postavi “sutra”
            executionTime = System.currentTimeMillis() + 24L * 60 * 60 * 1000;
        }

        // Kreiranje i upis
        TaskService taskService = new TaskService();
        taskService.createTask(
                        name, description, selectedCategory.getId(),
                        weight, importance, xpValue, executionTime,
                        isRepeating, startDate, endDate,
                        repeatInterval, repeatUnit)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Task \"" + name + "\" uspešno kreiran!", Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Task creation failed.", e);
                    Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ————————————— helpers —————————————

    private void pickDate(boolean isStart) {
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
                    long ms = cal.getTimeInMillis();

                    if (isStart) {
                        pickedStartDateMs = ms;
                        btnPickStart.setText(formatDate(ms));

                        // ako end postoji a pre je starta → resetuj end
                        if (pickedEndDateMs != null && pickedEndDateMs < pickedStartDateMs) {
                            pickedEndDateMs = null;
                        }
                    } else {
                        pickedEndDateMs = ms;
                        btnPickEnd.setText(formatDate(ms));
                    }
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private String formatDate(long ms) {
        return new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
                .format(new java.util.Date(ms));
    }
}
