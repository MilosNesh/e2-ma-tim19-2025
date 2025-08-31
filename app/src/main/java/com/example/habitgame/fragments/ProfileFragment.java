package com.example.habitgame.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.habitgame.MainActivity;
import com.example.habitgame.R;
import com.example.habitgame.activities.LoginActivity;
import com.example.habitgame.databinding.FragmentProfileBinding;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.Equipment;
import com.example.habitgame.repositories.AccountRepository;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private static final String ARG_EMAIL = "email";
    private String email;

    private ImageView avatar;
    private TextView username, levelAndTitle, pp, xp, coins, badgeLabel;
    private Button changePassword;

    public ProfileFragment() {
        // Required empty public constructor
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
//        changePassword = binding.changePassword;

        if (email != null && !email.isEmpty()) {
            AccountRepository accountRepository = new AccountRepository();
            accountRepository.selectByEmail(email)
                    .addOnSuccessListener(account -> {
                        if (getContext() != null) {
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
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Error", "Neuspješno dohvaćanje accounta", e);
                        Toast.makeText(getContext(), "Greška prilikom učitavanja korisničkog naloga.", Toast.LENGTH_SHORT).show();
                    });
        }

//        changePassword.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
//                transaction.replace(R.id.mainContainer, new ChangePasswordFragment());
//                transaction.addToBackStack(null);
//                transaction.commit();
//            }
//        });
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
                    58, 58);
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
                    58, 58);
            params.setMargins(4, 4, 4, 4);
            equipmentImage.setLayoutParams(params);

            equipmentLayout.addView(equipmentImage);
        }
    }

}