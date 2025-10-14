package com.example.habitgame.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habitgame.R;
import com.example.habitgame.services.CategoryService;
import com.google.firebase.firestore.DocumentReference;

import yuku.ambilwarna.AmbilWarnaDialog;

public class CategoryCreationFragment extends Fragment {

    private EditText etName;
    private View colorPreview;
    private Button btnPickColor, btnSave;

    private int selectedColor = Color.parseColor("#2196F3"); // default plava

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_creation, container, false);

        etName = view.findViewById(R.id.et_category_name);
        colorPreview = view.findViewById(R.id.view_color_preview);
        btnPickColor = view.findViewById(R.id.btn_pick_color);
        btnSave = view.findViewById(R.id.btn_save_category);

        colorPreview.setBackgroundColor(selectedColor);

        btnPickColor.setOnClickListener(v -> openColorPicker());
        btnSave.setOnClickListener(v -> saveCategory());

        return view;
    }

    private void openColorPicker() {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(requireContext(), selectedColor,
                new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override public void onCancel(AmbilWarnaDialog dialog) {}
                    @Override public void onOk(AmbilWarnaDialog dialog, int color) {
                        selectedColor = color;
                        colorPreview.setBackgroundColor(color);
                    }
                });
        dialog.show();
    }

    private void saveCategory() {
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "Unesi naziv kategorije.", Toast.LENGTH_SHORT).show();
            return;
        }

        String colorHex = String.format("#%06X", (0xFFFFFF & selectedColor));

        CategoryService.createCategory(name, colorHex)
                .addOnSuccessListener((DocumentReference doc) -> {
                    Toast.makeText(getContext(), "Kategorija dodata!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
