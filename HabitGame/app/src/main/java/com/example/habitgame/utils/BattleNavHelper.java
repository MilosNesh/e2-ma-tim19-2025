package com.example.habitgame.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habitgame.MainActivity;
import com.example.habitgame.R;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.services.AccountService;

public final class BattleNavHelper {
    private BattleNavHelper(){}
    public static void checkAndLaunchFromActivity(@NonNull Activity a){
        String email = a.getSharedPreferences("HabitGamePrefs", Context.MODE_PRIVATE)
                .getString("email", null);
        if (email == null) return;

        new AccountService().getAccountByEmail(email, new AccountCallback() {
            @Override public void onResult(Account acc) {
                if (acc == null) return;
                Boolean pend = acc.getPendingBoss();
                if (pend != null && pend) {
                    acc.setPendingBoss(false);
                    new AccountService().update(acc);
                    try {
                        NavController nav = Navigation.findNavController(a, R.id.mainContainer);
                        nav.navigate(R.id.battleFragment);
                        if (a.getActionBar()!=null) a.getActionBar().setTitle(R.string.boss);
                    } catch (Exception ignore) {}
                }
            }
            @Override public void onFailure(Exception e) {}
        });
    }
}
