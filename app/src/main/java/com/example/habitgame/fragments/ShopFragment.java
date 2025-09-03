package com.example.habitgame.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.example.habitgame.R;
import com.example.habitgame.adapters.EquipmentAdapter;
import com.example.habitgame.model.Equipment;
import com.example.habitgame.model.EquipmentListCallback;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.services.AccountService;
import com.example.habitgame.services.EquipmentService;

import java.util.List;

public class ShopFragment extends Fragment {

    private List<Equipment> equipmentList;
    private EquipmentAdapter equipmentAdapter;
    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflating layout za fragment
        View rootView = inflater.inflate(R.layout.fragment_shop, container, false);

        listView = rootView.findViewById(R.id.list_view);
        EquipmentService equipmentService = new EquipmentService();
        AccountService accountService = new AccountService();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("HabitGamePrefs", getContext().MODE_PRIVATE);
        String email = sharedPreferences.getString("email", null);
        int level = sharedPreferences.getInt("level", 1);
        equipmentService.getAllForShop(level, new EquipmentListCallback() {
            @Override
            public void onResult(List<Equipment> equipmentList) {
                equipmentAdapter = new EquipmentAdapter(getContext(), equipmentList, equipment -> {

                    accountService.buyEquipment(email, equipment, new StringCallback() {
                        @Override
                        public void onResult(String result) {
                            Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                listView.setAdapter(equipmentAdapter);
            }
        });



        return rootView;
    }
}
