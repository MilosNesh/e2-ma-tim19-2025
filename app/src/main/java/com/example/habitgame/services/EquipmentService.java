package com.example.habitgame.services;

import com.example.habitgame.model.EquipmentListCallback;
import com.example.habitgame.repositories.EquipmentRepository;

public class EquipmentService {
    public void getAllForShop(int level, EquipmentListCallback callback){
        EquipmentRepository.selectForShop(level).addOnSuccessListener(list ->{
            callback.onResult(list);
        });

    }
}
