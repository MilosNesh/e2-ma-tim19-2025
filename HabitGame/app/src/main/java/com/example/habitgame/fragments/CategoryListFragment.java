package com.example.habitgame.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habitgame.R;
import com.example.habitgame.adapters.CategoryAdapter;
import com.example.habitgame.model.Category;
import com.example.habitgame.repositories.CategoryRepository;
import com.example.habitgame.services.CategoryService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;

public class CategoryListFragment extends Fragment implements CategoryAdapter.Listener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private CategoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_list, container, false);

        tvEmpty = view.findViewById(R.id.tv_empty);
        recyclerView = view.findViewById(R.id.recycler_categories);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CategoryAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_category);
        fab.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.categoryCreationFragment)
        );

        loadCategoriesOnce();

        return view;
    }

    private void loadCategoriesOnce() {
        CategoryService.getMyCategories()
                .addOnSuccessListener(list -> render(list))
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška pri učitavanju: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void render(List<Category> list) {
        if (list == null || list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(list);
        }
    }

    @Override public void onEdit(Category c) {
        CategoryEditBottomSheet.newInstance(c, () -> {
            CategoryService.getMyCategories()
                    .addOnSuccessListener(this::render);
        }).show(getParentFragmentManager(), "editCategory");
    }

    @Override public void onDelete(Category c) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Obriši kategoriju?")
                .setMessage("Ova akcija se ne može poništiti.")
                .setPositiveButton("Obriši", (d, w) ->
                        CategoryService.deleteCategory(c.getId())
                                .addOnFailureListener(e -> Toast.makeText(getContext(),"Greška: "+e.getMessage(),Toast.LENGTH_LONG).show()))
                .setNegativeButton("Otkaži", null)
                .show();
    }


    private ListenerRegistration reg;
    @Override public void onStart() {
        super.onStart();
        reg = CategoryRepository.listenForCurrentUser(
                this::render,
                e -> Toast.makeText(getContext(), "Greška pri čitanju: "+e.getMessage(), Toast.LENGTH_LONG).show());
    }
    @Override public void onStop() { super.onStop(); if (reg != null) reg.remove(); }

}
