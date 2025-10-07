package com.example.habitgame.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.example.habitgame.R;
import com.example.habitgame.adapters.EquipmentAdapter;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.Equipment;
import com.example.habitgame.services.AccountService;

import java.util.ArrayList;
import java.util.List;


public class EquipmentFragment extends Fragment {
    private EquipmentAdapter equipmentAdapter, activeEquipmentAdapter;
    private ListView listView;

    public EquipmentFragment() {
        // Required empty public constructor
    }

    public static EquipmentFragment newInstance(String param1, String param2) {
        EquipmentFragment fragment = new EquipmentFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equipment, container, false);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        ListView recyclerView = view.findViewById(R.id.recycler_view);
        ListView activeEquipment = view.findViewById(R.id.active_equipments);

        AccountService accountService = new AccountService();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("HabitGamePrefs", getContext().MODE_PRIVATE);
        String email = sharedPreferences.getString("email", null);
        accountService.getAccountByEmail(email, new AccountCallback() {
            @Override
            public void onResult(Account account) {
                List<Equipment> equipmentList = new ArrayList<>();
                for(Equipment e: account.getEquipments()){
                    if(e.isActivated())
                        equipmentList.add(e);
                }
                activeEquipmentAdapter = new EquipmentAdapter(getContext(), equipmentList, "", new EquipmentAdapter.OnBuyClickListener() {
                    @Override
                    public void onBuyClick(Equipment equipment) {
                    }
                });

                equipmentAdapter = new EquipmentAdapter(getContext(), account.getEquipments(), "Aktiviraj", new EquipmentAdapter.OnBuyClickListener() {
                    @Override
                    public void onBuyClick(Equipment equipment) {
                        for (Equipment e : account.getEquipments()) {
                            if (e.equals(equipment)) {
                                e.setActivated(true);
                                accountService.update(account);
                                break;
                            }
                        }
                    }
                });
                recyclerView.setAdapter(equipmentAdapter);
                activeEquipment.setAdapter(activeEquipmentAdapter);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Doslo je do greske prilikom ucitavanja", Toast.LENGTH_SHORT).show();
            }
        });

    }
}