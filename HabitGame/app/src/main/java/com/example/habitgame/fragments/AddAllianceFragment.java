package com.example.habitgame.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.habitgame.R;
import com.example.habitgame.adapters.ProfileAdapter;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.AccountListCallback;
import com.example.habitgame.model.Alliance;
import com.example.habitgame.model.AllianceCallback;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.services.AccountService;
import com.example.habitgame.services.AllianceService;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AddAllianceFragment extends Fragment {

    private EditText allianceName;
    private Button addAlliance;
    private ListView listView;
    private AccountService accountService;
    private String myEmail;
    private ProfileAdapter profileAdapter;
    private AllianceService allianceService;
    private List<Account> accountList;
    private  SharedPreferences sharedPreferences;
    public AddAllianceFragment() {
        // Required empty public constructor
    }

    public static AddAllianceFragment newInstance(String param1, String param2) {
        AddAllianceFragment fragment = new AddAllianceFragment();
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
        View view = inflater.inflate(R.layout.fragment_add_alliance, container, false);
        listView = view.findViewById(R.id.friends_list);
        allianceName = view.findViewById(R.id.alliance_name);
        addAlliance = view.findViewById(R.id.add_aliance);

        accountService = new AccountService();
        allianceService = new AllianceService();
        accountList = new ArrayList<>();
        sharedPreferences = getActivity().getSharedPreferences("HabitGamePrefs", getContext().MODE_PRIVATE);
        myEmail = sharedPreferences.getString("email", null);

        // Get all accounts initially
        accountService.getAllFriends(myEmail, new AccountListCallback() {
            @Override
            public void onResult(List<Account> result) {
                profileAdapter = new ProfileAdapter(getContext(), result, myEmail, getString(R.string.add_to_alliance), account -> {
                    Toast.makeText(getContext(), account.getUsername(), Toast.LENGTH_SHORT).show();
                    accountList.add(account);
                });
                listView.setAdapter(profileAdapter);
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addAlliance.setOnClickListener(v -> {
            if(allianceName.getText().toString().equals("")) {
                Toast.makeText(getContext(), "Ime saveza je obavezno polje", Toast.LENGTH_SHORT).show();
                return;
            }
            Alliance alliance = new Alliance(allianceName.getText().toString(), myEmail);
            allianceService.save(alliance, new AllianceCallback() {
                @Override
                public void onResult(Alliance result) {
                    if (result == null) {
                        Toast.makeText(getContext(), "Greska prilikom dodavanja savrza!", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("allianceId", result.getId());
                        editor.apply();

                        allianceService.sendAllianceInvite(result.getId(), result.getName(), accountList, myEmail);

                        AccountService.updateAlliance(myEmail, result.getId(), new AccountCallback() {
                            @Override
                            public void onResult(Account account) {
                                Toast.makeText(getContext(), "Savez dodat", Toast.LENGTH_SHORT).show();
                                NavController navController = Navigation.findNavController(requireActivity(), R.id.mainContainer);
                                navController.navigate(R.id.allianceFragment);
                            }
                        });

                    }
                }
            });

        });
    }
}