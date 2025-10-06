package com.example.habitgame.services;

import android.text.TextUtils;
import android.util.Log;

import com.example.habitgame.model.Category;
import com.example.habitgame.repositories.CategoryRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.util.List;
import java.util.Locale;

public class CategoryService {

    private static final String TAG = "CategoryService";

    public static Task<DocumentReference> createCategory(String name, String colorHex) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) {
            Log.e(TAG, "User not authenticated. Cannot create category.");
            return Tasks.forException(new IllegalStateException("User not authenticated."));
        }

        if (TextUtils.isEmpty(name)) {
            return Tasks.forException(new IllegalArgumentException("Naziv kategorije je obavezan."));
        }

        String normHex = normalizeHex(colorHex);
        if (!isValidHex(normHex)) {
            return Tasks.forException(new IllegalArgumentException("Neispravan format boje. Koristi #RRGGBB."));
        }

        Category category = new Category();
        category.setUserId(uid);
        category.setName(name.trim());
        category.setColorHex(normHex);

        return CategoryRepository.colorExistsForUser(normHex)
                .onSuccessTask(exists -> {
                    if (exists) {
                        return Tasks.forException(new IllegalStateException("Boja je već zauzeta."));
                    }
                    return CategoryRepository.insert(category);
                });
    }

    public static Task<List<Category>> getMyCategories() {
        return CategoryRepository.getForCurrentUser();
    }

    public static Task<Void> renameCategory(String categoryId, String newName) {
        if (TextUtils.isEmpty(categoryId)) {
            return Tasks.forException(new IllegalArgumentException("Nedostaje categoryId."));
        }
        if (TextUtils.isEmpty(newName)) {
            return Tasks.forException(new IllegalArgumentException("Naziv je obavezan."));
        }
        return CategoryRepository.updateName(categoryId, newName.trim());
    }

    public static Task<Void> changeCategoryColor(String categoryId, String newColorHex) {
        if (TextUtils.isEmpty(categoryId)) {
            return Tasks.forException(new IllegalArgumentException("Nedostaje categoryId."));
        }

        String normHex = normalizeHex(newColorHex);
        if (!isValidHex(normHex)) {
            return Tasks.forException(new IllegalArgumentException("Neispravan format boje. Koristi #RRGGBB."));
        }

        // jedinstvena boja (osim ove kategorije) – uradimo klijentsku proveru preko liste
        return CategoryRepository.getForCurrentUser()
                .onSuccessTask(list -> {
                    for (Category c : list) {
                        if (c.getId() != null
                                && !c.getId().equals(categoryId)
                                && normHex.equalsIgnoreCase(c.getColorHex())) {
                            return Tasks.forException(new IllegalStateException("Boja je već zauzeta."));
                        }
                    }
                    return CategoryRepository.updateColor(categoryId, normHex);
                });
    }

    public static Task<Void> deleteCategory(String categoryId) {
        if (TextUtils.isEmpty(categoryId)) {
            return Tasks.forException(new IllegalArgumentException("Nedostaje categoryId."));
        }
        return CategoryRepository.delete(categoryId);
    }

    // ——— helpers ———

    private static String normalizeHex(String hex) {
        if (hex == null) return "";
        hex = hex.trim();
        if (!hex.startsWith("#") && hex.length() == 6) hex = "#" + hex;
        return hex.toUpperCase(Locale.ROOT);
    }

    private static boolean isValidHex(String hex) {
        return hex != null && hex.matches("^#[0-9A-Fa-f]{6}$");
    }
}
