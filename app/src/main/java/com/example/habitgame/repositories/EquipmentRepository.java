package com.example.habitgame.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Equipment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class EquipmentRepository {
    public static Task<List<Equipment>> selectForShop(int level) {
        TaskCompletionSource<List<Equipment>> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("equipments")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Equipment> equipmentList = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Equipment equipment = document.toObject(Equipment.class);
                                if(!equipment.getType().equals("oruzje")) {
                                    equipment.calculatePrice(level);
                                    equipmentList.add(equipment);
                                }
                                Log.d("REZ_DB", document.getId() + " => " + document.getData());
                            }
                            taskCompletionSource.setResult(equipmentList);

                            Log.d("REZ_DB", "Povučeni podaci: " + equipmentList.toString());
                        } else {
                            Log.w("REZ_DB", "Error getting documents.", task.getException());
                            taskCompletionSource.setResult(null);
                        }
                    }
                });
        return taskCompletionSource.getTask();
    }


}
