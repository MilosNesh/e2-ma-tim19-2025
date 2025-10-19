package com.example.habitgame.fragments;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.habitgame.model.Account;
import com.example.habitgame.model.Category;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.services.AccountService;
import com.example.habitgame.services.CategoryService;
import com.example.habitgame.services.RepeatedTaskService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RepeatedTaskCreateFragment extends Fragment {

    private EditText etName, etDesc;
    private Spinner spCategory, spWeight, spImportance, spInterval, spUnit;
    private Button btnPickStart, btnPickEnd, btnCreate;

    private final List<Category> categories = new ArrayList<>();
    private Long startMs = null, endMs = null;

    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault());
    private final RepeatedTaskService service = new RepeatedTaskService();

    private int currentUserLevel = 1;

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
        loadCurrentUserLevel(); // <<< učitaj level

        btnPickStart.setOnClickListener(x -> pickDate(ms -> {
            startMs = ms;
            btnPickStart.setText(getString(R.string.date) + ": " + df.format(new java.util.Date(ms)));
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


    private void loadCurrentUserLevel() {
        try {
            SharedPreferences sp = requireContext().getSharedPreferences("HabitGamePrefs", Context.MODE_PRIVATE);
            String email = sp.getString("email", null);
            if (email == null || email.trim().isEmpty()) {
                currentUserLevel = 1;
                return;
            }
            new AccountService().getAccountByEmail(email, new AccountCallback() {
                @Override public void onResult(Account acc) {
                    if (acc != null) currentUserLevel = Math.max(1, acc.getLevel());
                }
                @Override public void onFailure(Exception e) {
                    currentUserLevel = 1; // fallback
                }
            });
        } catch (Exception ignore) {
            currentUserLevel = 1;
        }
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
        ArrayAdapter<CharSequence> weightAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_weights, android.R.layout.simple_spinner_dropdown_item);
        spWeight.setAdapter(weightAdapter);

        ArrayAdapter<CharSequence> importanceAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.xp_importance, android.R.layout.simple_spinner_dropdown_item);
        spImportance.setAdapter(importanceAdapter);

        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.repeat_intervals, android.R.layout.simple_spinner_dropdown_item);
        spInterval.setAdapter(intervalAdapter);

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

        String weight = (String) spWeight.getSelectedItem();
        String importance = (String) spImportance.getSelectedItem();
        String intervalStr = (String) spInterval.getSelectedItem();
        int interval;
        try { interval = Math.max(1, Math.min(7, Integer.parseInt(intervalStr))); }
        catch (NumberFormatException nfe) {
            Toast.makeText(getContext(), getString(R.string.repeating), Toast.LENGTH_SHORT).show();
            return;
        }
        String unit = (String) spUnit.getSelectedItem(); // "dan" | "nedelja"

        service.createSeriesAndGenerate(
                        name,
                        TextUtils.isEmpty(desc) ? null : desc,
                        categoryId,
                        weight,
                        importance,
                        startMs,
                        endMs,
                        interval,
                        unit,
                        currentUserLevel
                )
                .addOnSuccessListener(ref -> {
                    Toast.makeText(getContext(), getString(R.string.create_task), Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    btnCreate.setEnabled(true);
                    Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
