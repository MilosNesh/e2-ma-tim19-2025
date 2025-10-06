package com.example.habitgame.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Category;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryRepository {

    private static final String TAG = "CategoryRepository";

    public static com.google.android.gms.tasks.Task<DocumentReference> insert(Category category) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        TaskCompletionSource<DocumentReference> tcs = new TaskCompletionSource<>();

        db.collection("categories")
                .add(category)
                .addOnSuccessListener(ref -> {
                    category.setId(ref.getId());
                    Log.d(TAG, "Category added with ID: " + ref.getId());
                    tcs.setResult(ref);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding category", e);
                    tcs.setException(e);
                });

        return tcs.getTask();
    }

    public static com.google.android.gms.tasks.Task<List<Category>> getForCurrentUser() {
        TaskCompletionSource<List<Category>> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) {
            Log.e(TAG, "User is not logged in. Cannot fetch categories.");
            tcs.setResult(new ArrayList<>());
            return tcs.getTask();
        }

        db.collection("categories")
                .whereEqualTo("userId", uid)
                .get()
                .addOnCompleteListener((OnCompleteListener<QuerySnapshot>) task -> {
                    if (task.isSuccessful()) {
                        List<Category> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Category c = doc.toObject(Category.class);
                            c.setId(doc.getId());
                            list.add(c);
                        }
                        tcs.setResult(list);
                    } else {
                        Log.w(TAG, "Error getting categories.", task.getException());
                        tcs.setResult(null);
                    }
                });

        return tcs.getTask();
    }

    public static com.google.android.gms.tasks.Task<Boolean> colorExistsForUser(@NonNull String hexColor) {
        TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) {
            tcs.setResult(false);
            return tcs.getTask();
        }

        db.collection("categories")
                .whereEqualTo("userId", uid)
                .whereEqualTo("colorHex", hexColor)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> tcs.setResult(!snap.isEmpty()))
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }

    public static com.google.android.gms.tasks.Task<Void> update(Category category) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (category.getId() == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("Category ID cannot be null for update."));
        }

        return db.collection("categories").document(category.getId())
                .set(category)
                .addOnSuccessListener(a -> Log.d(TAG, "Category updated: " + category.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating category: " + category.getId(), e);
                    throw new RuntimeException("DB update failed.", e);
                });
    }

    public static com.google.android.gms.tasks.Task<Void> updateColor(@NonNull String categoryId, @NonNull String hexColor) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("colorHex", hexColor);
        return db.collection("categories").document(categoryId).update(updates);
    }

    public static com.google.android.gms.tasks.Task<Void> updateName(@NonNull String categoryId, @NonNull String newName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        return db.collection("categories").document(categoryId).update(updates);
    }

    public static com.google.android.gms.tasks.Task<Void> delete(@NonNull String categoryId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("categories").document(categoryId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Category deleted: " + categoryId))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting category: " + categoryId, e));
    }
}
