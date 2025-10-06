package com.example.habitgame.fragments;

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

public class CategoryCreationFragment extends Fragment {

    private EditText etName, etColor;
    private Button btnSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_creation, container, false);

        etName = view.findViewById(R.id.et_category_name);
        etColor = view.findViewById(R.id.et_category_color);
        btnSave = view.findViewById(R.id.btn_save_category);

        btnSave.setOnClickListener(v -> saveCategory());

        return view;
    }

    private void saveCategory() {
        String name = etName.getText().toString().trim();
        String color = etColor.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(color)) {
            Toast.makeText(getContext(), "Popuni oba polja.", Toast.LENGTH_SHORT).show();
            return;
        }

        CategoryService.createCategory(name, color)
                .addOnSuccessListener((DocumentReference doc) -> {
                    Toast.makeText(getContext(), "Kategorija dodata!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
