package com.example.habitgame.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habitgame.R;
import com.example.habitgame.model.Category;
import com.example.habitgame.services.CategoryService;
import com.example.habitgame.services.RepeatedTaskService;
import com.example.habitgame.utils.XpCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kreiranje PONAVLJAJUĆE serije — bez 'seed', generiše sve pojave u [start..end]. */
public class RepeatedTaskCreateFragment extends Fragment {

    // UI
    private EditText etName, etDesc;
    private Spinner spCategory, spWeight, spImportance, spInterval, spUnit;
    private Button btnPickStart, btnPickEnd, btnCreate;

    // Data
    private final List<Category> categories = new ArrayList<>();
    private Long startMs = null, endMs = null;

    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());
    private final RepeatedTaskService service = new RepeatedTaskService();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_repeated_create, c, false);

        etName = v.findViewById(R.id.et_task_name);
        etDesc = v.findViewById(R.id.et_task_description);
        spCategory = v.findViewById(R.id.spinner_category);
        spWeight = v.findViewById(R.id.spinner_weight);
        spImportance = v.findViewById(R.id.spinner_importance);
        spInterval = v.findViewById(R.id.spinner_repeat_interval);
        spUnit = v.findViewById(R.id.spinner_repeat_unit);
        btnPickStart = v.findViewById(R.id.btn_pick_start);
        btnPickEnd   = v.findViewById(R.id.btn_pick_end);
        btnCreate    = v.findViewById(R.id.btn_create_task);

        setupStaticSpinners();
        loadCategories();

        btnPickStart.setOnClickListener(x -> pickDate(ms -> {
            startMs = ms;
            btnPickStart.setText(getString(R.string.date) + ": " + df.format(new java.util.Date(ms)));
            // ako je end < start, resetuj end
            if (endMs != null && endMs < startMs) {
                endMs = null;
                btnPickEnd.setText(getString(R.string.odaberi_datum_zavrsetka));
            }
        }));

        btnPickEnd.setOnClickListener(x -> pickDate(ms -> {
            endMs = ms;
            btnPickEnd.setText(getString(R.string.date) + ": " + df.format(new java.util.Date(ms)));
        }));

        btnCreate.setOnClickListener(x -> attemptCreate());
        return v;
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

    private void setupStaticSpinners() {
        // Težina
        ArrayAdapter<CharSequence> weightAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_weights, android.R.layout.simple_spinner_dropdown_item);
        spWeight.setAdapter(weightAdapter);

        // Bitnost
        ArrayAdapter<CharSequence> importanceAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_importance, android.R.layout.simple_spinner_dropdown_item);
        spImportance.setAdapter(importanceAdapter);

        // Interval (1..7 pretpostavljeno u resursu)
        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.repeat_intervals, android.R.layout.simple_spinner_dropdown_item);
        spInterval.setAdapter(intervalAdapter);

        // Jedinica ("dan" | "nedelja")
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.repeat_units, android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);
    }

    private void loadCategories() {
        CategoryService.getMyCategories()
                .addOnSuccessListener(list -> {
                    categories.clear();
                    if (list != null) categories.addAll(list);
                    ArrayList<String> names = new ArrayList<>();
                    for (Category c : categories) names.add(c.getName());
                    spCategory.setAdapter(new ArrayAdapter<>(
                            requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška pri učitavanju kategorija.", Toast.LENGTH_LONG).show());
    }

    private void attemptCreate() {
        String name = etName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), getString(R.string.err_name_required), Toast.LENGTH_SHORT).show();
            return;
        }
        if (categories.isEmpty() || spCategory.getSelectedItemPosition() == Spinner.INVALID_POSITION) {
            Toast.makeText(getContext(), getString(R.string.no_categories), Toast.LENGTH_SHORT).show();
            return;
        }
        if (startMs == null) {
            Toast.makeText(getContext(), getString(R.string.pick_date), Toast.LENGTH_SHORT).show();
            return;
        }
        if (endMs == null) {
            Toast.makeText(getContext(), getString(R.string.odaberi_datum_zavrsetka), Toast.LENGTH_SHORT).show();
            return;
        }
        if (endMs < startMs) {
            Toast.makeText(getContext(), "Krajnji datum mora biti posle početnog.", Toast.LENGTH_LONG).show();
            return;
        }

        Category cat = categories.get(spCategory.getSelectedItemPosition());
        String categoryId = cat.getId();

        String weight = (String) spWeight.getSelectedItem();          // srpski nazivi
        String importance = (String) spImportance.getSelectedItem();  // srpski nazivi

        String intervalStr = (String) spInterval.getSelectedItem();
        int interval;
        try { interval = Math.max(1, Math.min(7, Integer.parseInt(intervalStr))); }
        catch (NumberFormatException nfe) {
            Toast.makeText(getContext(), getString(R.string.repeating), Toast.LENGTH_SHORT).show();
            return;
        }

        String unit = (String) spUnit.getSelectedItem(); // "dan" | "nedelja"

        // XP se računa iz težina+bitnost; deli se po pojavi u servisu → šaljemo xpPerOccurrence = null
        int previewTotalXp = XpCalculator.calculateTotalXp(weight, importance);

        service.createSeriesAndGenerate(
                name,
                TextUtils.isEmpty(desc) ? null : desc,
                categoryId,
                weight,
                importance,
                startMs,
                endMs,
                interval,
                unit   // <— NEMA više dodatnog , null
        )
                .addOnSuccessListener(ref -> {
                    Toast.makeText(getContext(),
                            getString(R.string.create_task) + " · ukupno XP: " + previewTotalXp,
                            Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    btnCreate.setEnabled(true);
                    Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
