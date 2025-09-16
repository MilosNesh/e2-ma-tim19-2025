package com.example.habitgame.fragments;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.habitgame.R;
import com.example.habitgame.databinding.FragmentChangePasswordBinding;
import com.example.habitgame.databinding.FragmentProfileBinding;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.repositories.AccountRepository;
import com.example.habitgame.services.AccountService;
import com.google.android.material.navigation.NavigationView;


public class ChangePasswordFragment extends Fragment {
    private static final String ARG_EMAIL = "email";
    private FragmentChangePasswordBinding binding;
    private EditText oldPasswordEditText, newPasswordEditText, confirmNewPasswordEditText;
    private Button changePasswordButton;
    private String email;
    private AccountService accountService;

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
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        oldPasswordEditText = binding.editTextOldPassword;
        newPasswordEditText = binding.editTextNewPassword;
        confirmNewPasswordEditText = binding.editTextConfirmNewPassword;
        changePasswordButton = binding.buttonChangePassword;

        accountService = new AccountService();

        changePasswordButton.setOnClickListener(v -> {
            accountService.changePassword(email, oldPasswordEditText.getText().toString(), newPasswordEditText.getText().toString(), confirmNewPasswordEditText.getText().toString(), new StringCallback() {
                @Override
                public void onResult(String result) {
                    Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                    if(result.equals("Lozinka uspjesno promjenjena")){
                        Navigation.findNavController(view).popBackStack();
                    }
                }
            });

        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(view).popBackStack();
            }
        });

        return view;
    }

}
