package com.example.habitgame.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.habitgame.R;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.services.AccountService;

public class ProgressFragment extends Fragment {

    private String myEmail;
    private AccountService accountService;

    private TextView titleText, ppText, xpText, maxXpText;
    private ImageView titleImage;
    private ProgressBar progressBar;

    public ProgressFragment() {
    }
    public static ProgressFragment newInstance(String param1, String param2) {
        ProgressFragment fragment = new ProgressFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);
        titleText = view.findViewById(R.id.titleText);
        ppText = view.findViewById(R.id.ppText);
        xpText = view.findViewById(R.id.xpText);
        maxXpText = view.findViewById(R.id.maxXpText);
        progressBar = view.findViewById(R.id.xpProgressBar);
        titleImage = view.findViewById(R.id.titleImage);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("HabitGamePrefs", getContext().MODE_PRIVATE);
        myEmail = sharedPreferences.getString("email", "");
        accountService = new AccountService();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        accountService.getAccountByEmail(myEmail, new AccountCallback() {
            @Override
            public void onResult(Account account) {
                int resID = getResources().getIdentifier("level"+account.getLevel(), "drawable", getActivity().getPackageName());
                titleImage.setImageResource(resID);
                account.newTitle();
                titleText.setText(account.getTitle());
                ppText.setText("Power points: " +account.getPowerPoints());
                xpText.setText("Osvojeni XP: " + account.getExperiencePoints());
                maxXpText.setText("Potrebno: "+account.countMaxXp());
                progressBar.setProgress(account.getExperiencePoints());
                progressBar.setMax(account.countMaxXp());
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("ProgressFragment", "Greška pri učitavanju Accounta za Progress: ", e);
                Toast.makeText(getContext(), "Greška pri učitavanju napretka: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}