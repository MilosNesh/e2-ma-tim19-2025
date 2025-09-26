package com.example.habitgame.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.habitgame.R;
import com.example.habitgame.adapters.ProfileAdapter;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountListCallback;
import com.example.habitgame.model.Alliance;
import com.example.habitgame.model.AllianceCallback;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.services.AccountService;
import com.example.habitgame.services.AllianceService;

import java.util.ArrayList;
import java.util.List;

public class AllianceFragment extends Fragment {

    private TextView allianceName, leaderLabel, memebersLabel;
    private ListView memebers, leader;
    private String allainceId, myEmail;
    private AllianceService allianceService;
    private AccountService accountService;
    private ProfileAdapter profileAdapter, leaderAdapter;
    private Button deleteAlliance, leaveAlliance;
    private SharedPreferences sharedPreferences;
    public AllianceFragment() {
    }

    public static AllianceFragment newInstance(String param1, String param2) {
        AllianceFragment fragment = new AllianceFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alliance, container, false);
        allianceName = view.findViewById(R.id.alliance_name_);
        memebers = view.findViewById(R.id.members);
        leader = view.findViewById(R.id.leader);
        deleteAlliance = view.findViewById(R.id.delete_alliance);
        leaveAlliance = view.findViewById(R.id.leave_alliance);
        leaderLabel = view.findViewById(R.id.leader_label);
        memebersLabel = view.findViewById(R.id.members_label);

        leaveAlliance.setVisibility(view.GONE);
        deleteAlliance.setVisibility(view.GONE);
        sharedPreferences = getActivity().getSharedPreferences("HabitGamePrefs", getContext().MODE_PRIVATE);
        allainceId = sharedPreferences.getString("allianceId", "");
        myEmail = sharedPreferences.getString("email", null);

        allianceService = new AllianceService();
        accountService = new AccountService();

        if(allainceId.equals("")){
            allianceName.setText("Niste clan nijednog saveza");
            leaderLabel.setVisibility(view.GONE);
            memebersLabel.setVisibility(view.GONE);
            return view;
        }
        loadAlliance(view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        deleteAlliance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allianceService.deleteAlliance(allainceId, new StringCallback() {
                    @Override
                    public void onResult(String result) {
                        Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove("allianceId");
                        editor.putString("allianceId", "");
                        editor.apply();
                        NavController navController = Navigation.findNavController(requireActivity(), R.id.mainContainer);
                        navController.navigate(R.id.allianceFragment);
                    }
                });
            }
        });

        leaveAlliance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accountService.leaveAlliance(myEmail, new StringCallback() {
                    @Override
                    public void onResult(String result) {
                        Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove("allianceId");
                        editor.putString("allianceId", "");
                        editor.apply();
                        NavController navController = Navigation.findNavController(requireActivity(), R.id.mainContainer);
                        navController.navigate(R.id.allianceFragment);
                    }
                });
            }
        });
    }

    private void loadAlliance(View view) {
        allianceService.getById(allainceId, new AllianceCallback() {
            @Override
            public void onResult(Alliance alliance) {
                if(alliance == null){
                    allianceName.setText("Niste clan nijednog saveza");
                    leaderLabel.setVisibility(view.GONE);
                    memebersLabel.setVisibility(view.GONE);
                    return;
                }

                allianceName.setText(alliance.getName());
                accountService.getByAlliance(allainceId, new AccountListCallback() {
                    @Override
                    public void onResult(List<Account> accountList) {
                        List<Account> leaderList = new ArrayList<>();
                        List<Account> membersList = new ArrayList<>();
                        for(Account acc : accountList) {
                            if(acc.getEmail().equals(alliance.getLeader())){
                                leaderList.add(acc);
                                continue;
                            }
                            membersList.add(acc);
                        }
                        leaderAdapter = new ProfileAdapter(getContext(), leaderList, myEmail, getString(R.string.show_profile), account -> {
                            Toast.makeText(getContext(), account.getUsername(), Toast.LENGTH_SHORT).show();
                        });
                        profileAdapter = new ProfileAdapter(getContext(), membersList, myEmail,  getString(R.string.show_profile), account -> {
                            Toast.makeText(getContext(), account.getUsername(), Toast.LENGTH_SHORT).show();
                        });

                        leader.setAdapter(leaderAdapter);
                        memebers.setAdapter(profileAdapter);
                        if(myEmail.equals(alliance.getLeader()))
                            deleteAlliance.setVisibility(view.VISIBLE);
                        else
                            leaveAlliance.setVisibility(view.VISIBLE);
                    }
                });
            }
        });
    }
}