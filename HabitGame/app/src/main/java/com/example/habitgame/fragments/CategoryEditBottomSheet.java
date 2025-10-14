package com.example.habitgame.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitgame.R;
import com.example.habitgame.model.Category;
import com.example.habitgame.services.CategoryService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import yuku.ambilwarna.AmbilWarnaDialog;

public class CategoryEditBottomSheet extends BottomSheetDialogFragment {

    public interface OnSaved { void onSaved(); }

    private static final String ARG_ID   = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_HEX  = "hex";

    private String categoryId;
    private String initialName;
    private String initialHex;

    private TextInputEditText etName;
    private View colorPreview;
    private int selectedColor;
    private OnSaved onSaved;

    public static CategoryEditBottomSheet newInstance(Category c, OnSaved cb) {
        CategoryEditBottomSheet s = new CategoryEditBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_ID, c.getId());
        b.putString(ARG_NAME, c.getName());
        b.putString(ARG_HEX, c.getColorHex());
        s.setArguments(b);
        s.onSaved = cb;
        return s;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.bottomsheet_edit_category, c, false);

        categoryId  = requireArguments().getString(ARG_ID);
        initialName = requireArguments().getString(ARG_NAME);
        initialHex  = requireArguments().getString(ARG_HEX);

        etName = v.findViewById(R.id.et_name);
        colorPreview = v.findViewById(R.id.view_color_preview);
        MaterialButton btnPick = v.findViewById(R.id.btn_pick_color);
        MaterialButton btnSave = v.findViewById(R.id.btn_save);

        etName.setText(initialName);
        try {
            selectedColor = Color.parseColor(initialHex);
        } catch (Exception e) {
            selectedColor = Color.parseColor("#2196F3");
        }
        colorPreview.setBackgroundColor(selectedColor);

        btnPick.setOnClickListener(view -> {
            new AmbilWarnaDialog(requireContext(), selectedColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override public void onCancel(AmbilWarnaDialog dialog) {}
                @Override public void onOk(AmbilWarnaDialog dialog, int color) {
                    selectedColor = color;
                    colorPreview.setBackgroundColor(color);
                }
            }).show();
        });

        btnSave.setOnClickListener(view -> doSave());

        return v;
    }

    private void doSave() {
        String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(newName)) {
            etName.setError(getString(R.string.required));
            return;
        }
        String newHex = String.format("#%06X", (0xFFFFFF & selectedColor));

        // 1) naziv (ako je promenjen)
        if (!newName.equals(initialName)) {
            CategoryService.renameCategory(categoryId, newName)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Greška (naziv): " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
        // 2) boja (ako je promenjena)
        if (!newHex.equalsIgnoreCase(initialHex)) {
            CategoryService.changeCategoryColor(categoryId, newHex)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Greška (boja): " + e.getMessage(), Toast.LENGTH_LONG).show());
        }

        if (onSaved != null) onSaved.onSaved();
        dismiss();
    }
}
