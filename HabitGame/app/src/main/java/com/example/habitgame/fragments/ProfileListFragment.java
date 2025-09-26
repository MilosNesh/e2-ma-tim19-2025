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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.habitgame.R;
import com.example.habitgame.adapters.ProfileAdapter;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountListCallback;
import com.example.habitgame.services.AccountService;

import java.util.List;

public class ProfileListFragment extends Fragment {
    AccountService accountService;
    ProfileAdapter profileAdapter;
    ListView listView;
    EditText searchText;
    ImageButton searchButton;
    String myEmail, allianceId;
    Button addAlianceButton;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_list, container, false);
        listView = view.findViewById(R.id.profile_list);
        searchText = view.findViewById(R.id.search_profile_text);
        searchButton = view.findViewById(R.id.search_profile_button);
        addAlianceButton = view.findViewById(R.id.add_aliance_button);
        accountService = new AccountService();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("HabitGamePrefs", getContext().MODE_PRIVATE);
        myEmail = sharedPreferences.getString("email", null);
        allianceId = sharedPreferences.getString("allianceId", "");

        if(!allianceId.equals("")) {
            addAlianceButton.setVisibility(view.GONE);
        }
        // Get all accounts initially
        accountService.getAllExpectMine(myEmail, new AccountListCallback() {
            @Override
            public void onResult(List<Account> accountList) {
                profileAdapter = new ProfileAdapter(getContext(), accountList, myEmail,  getString(R.string.show_profile), account -> {
                    Bundle args = new Bundle();
                    args.putString("email", account.getEmail());
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.mainContainer);
                    navController.navigate(R.id.profileFragment, args);
                });
                listView.setAdapter(profileAdapter);
            }
        });

        addAlianceButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.mainContainer);
            navController.navigate(R.id.addAllianceFragment);
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchButton.setOnClickListener(v -> {
            String query = searchText.getText().toString().trim();
            if (!query.isEmpty()) {
                accountService.searchByUSername(query, new AccountListCallback() {
                    @Override
                    public void onResult(List<Account> accountList) {
                        // Update the adapter with search results
                        if (profileAdapter != null) {
                            if(accountList == null) {
                                profileAdapter.clearList();
                                Toast.makeText(getContext(), "Nema korisnika sa unijetim kotisnickim imenom!", Toast.LENGTH_SHORT).show();
                            } else {
                                profileAdapter.updateList(accountList);
                            }
                            profileAdapter.notifyDataSetChanged();
                        }
                    }
                });
            } else {
                // If search query is empty, show all accounts again
                accountService.getAllExpectMine(myEmail, new AccountListCallback() {
                    @Override
                    public void onResult(List<Account> accountList) {
                        if (profileAdapter != null) {
                            profileAdapter.updateList(accountList);
                            profileAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
    }
}
