package com.example.habitgame.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.habitgame.MainActivity;
import com.example.habitgame.R;
import com.example.habitgame.activities.LoginActivity;
import com.example.habitgame.databinding.FragmentProfileBinding;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.Equipment;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.repositories.AccountRepository;
import com.example.habitgame.services.AccountService;
import com.example.habitgame.services.QRCodeService;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private static final String ARG_EMAIL = "email";
    private String email;

    private ImageView avatar, qrCode;
    private TextView username, levelAndTitle, pp, xp, coins, badgeLabel;
    private ImageButton addFriend;
    private LinearLayout statsSection;
    private AccountService accountService;
    private Account showedAccount;
    public ProfileFragment() {

    }

    public static ProfileFragment newInstance(String param1) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, param1);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            email = getArguments().getString(ARG_EMAIL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatar = binding.avatarImage;
        username = binding.username;
        levelAndTitle = binding.levelTitle;
        pp = binding.pp;
        xp = binding.xp;
        coins = binding.coins;
        badgeLabel = binding.badgesLabel;
        statsSection = binding.statsSection;
        addFriend = binding.addFriend;
        qrCode = binding.qrcode;

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("HabitGamePrefs", getContext().MODE_PRIVATE);
        String myEmail = sharedPreferences.getString("email", null);

        addFriend.setVisibility(view.GONE);
        if(!myEmail.equals(email)){
            statsSection.setVisibility(view.GONE);
        }
        else{
            statsSection.setVisibility(view.VISIBLE);
        }

        QRCodeService qrCodeService = new QRCodeService();
        accountService = new AccountService();
        accountService.getAccountByEmail(email, new AccountCallback() {
            @Override
            public void onResult(Account account) {
                showedAccount = account;
                avatar.setImageResource(account.getAvatar());
                username.setText(account.getUsername());
                levelAndTitle.setText("Lvl " + account.getLevel() + " ~ " + account.getTitle());
                pp.setText(String.valueOf(account.getPowerPoints()));
                xp.setText(String.valueOf(account.getExperiencePoints()));
                coins.setText(String.valueOf(account.getCoins()));

                String bl = "Bedzevi: " + account.getBadgeNumbers();
                badgeLabel.setText(bl);
                setBudges(account);
                setEquipments(account);
                if(!account.getFriends().contains(myEmail) && !account.getEmail().equals(myEmail)){
                    addFriend.setVisibility(view.VISIBLE);
                }
                qrCode.setImageBitmap(qrCodeService.generateQRCode(account.getEmail()));
            }
        });

        addFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accountService.addFriend(showedAccount, myEmail, new StringCallback() {
                    @Override
                    public void onResult(String result) {
                        Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                        addFriend.setVisibility(view.GONE);
                    }
                });
            }
        });
    }

    private void setBudges(Account account){
        LinearLayout badgesLayout = binding.badgesLayout;
        badgesLayout.removeAllViews();

        for (int i = 0; i < account.getBadgeNumbers(); i++) {
            ImageView badgeImage = new ImageView(getActivity());
            badgeImage.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            badgeImage.setImageResource(R.drawable.badge);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    100, 100);
            params.setMargins(4, 4, 4, 4);
            badgeImage.setLayoutParams(params);

            badgesLayout.addView(badgeImage);
        }
    }

    private void setEquipments(Account account){
        LinearLayout equipmentLayout = binding.equipmentLayout;
        equipmentLayout.removeAllViews();

        for (Equipment equipment: account.getEquipments()) {
            ImageView equipmentImage = new ImageView(getActivity());
            equipmentImage.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            int resID = getResources().getIdentifier(equipment.getImage(), "drawable", getActivity().getPackageName());
            equipmentImage.setImageResource(resID);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    100, 100);
            params.setMargins(4, 4, 4, 4);
            equipmentImage.setLayoutParams(params);

            equipmentLayout.addView(equipmentImage);
        }
    }

}