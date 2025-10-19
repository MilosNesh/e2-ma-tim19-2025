package com.example.habitgame.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.habitgame.R;
import com.example.habitgame.adapters.EquipmentAdapter;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.Equipment;
import com.example.habitgame.model.EquipmentListCallback;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.services.AccountService;
import com.example.habitgame.services.EquipmentService;
import com.example.habitgame.utils.LevelUtils;

import java.util.List;

public class ShopFragment extends Fragment {

    private ListView listView;
    private final AccountService accountService = new AccountService();
    private final EquipmentService equipmentService = new EquipmentService();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_shop, container, false);
        listView = rootView.findViewById(R.id.list_view);

        // Uzmemo samo email iz prefs (to je ključ)
        SharedPreferences sp = requireActivity()
                .getSharedPreferences("HabitGamePrefs", requireContext().MODE_PRIVATE);
        String email = sp.getString("email", null);
        if (email == null) {
            Toast.makeText(getContext(), "Niste prijavljeni.", Toast.LENGTH_SHORT).show();
            return rootView;
        }

        accountService.getAccountByEmail(email, new com.example.habitgame.model.AccountCallback() {
            @Override public void onResult(@Nullable Account acc) {
                if (acc == null) {
                    Toast.makeText(getContext(), "Nalog nije pronađen.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int storedLevel   = Math.max(1, acc.getLevel());
                int computedLevel = LevelUtils.levelFromXp(Math.max(0, acc.getExperiencePoints()));
                int effectiveLevel= Math.max(storedLevel, computedLevel);

                if (effectiveLevel != storedLevel) {
                    acc.setLevel(effectiveLevel);
                    acc.setTitle(LevelUtils.titleForLevel(effectiveLevel));
                    accountService.update(acc);
                }

                equipmentService.getAllForShop(effectiveLevel, new EquipmentListCallback() {
                    @Override public void onResult(List<Equipment> equipmentList) {
                        EquipmentAdapter adapter = new EquipmentAdapter(
                                getContext(),
                                equipmentList,
                                "Kupi",
                                equipment -> {
                                    if (effectiveLevel < 1) {
                                        Toast.makeText(getContext(),
                                                "Ne možete kupiti jer ste i dalje na nivou početnik.",
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    accountService.buyEquipment(email, equipment, new StringCallback() {
                                        @Override public void onResult(String result) {
                                            Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                        );
                        listView.setAdapter(adapter);
                    }
                });
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }
}
