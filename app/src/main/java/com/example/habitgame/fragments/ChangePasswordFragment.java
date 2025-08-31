package com.example.habitgame.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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
import com.example.habitgame.repositories.AccountRepository;


public class ChangePasswordFragment extends Fragment {

    private FragmentChangePasswordBinding binding;
    private EditText oldPasswordEditText, newPasswordEditText, confirmNewPasswordEditText;
    private Button changePasswordButton;

    private Account account;
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

        AccountRepository accountRepository = new AccountRepository();
        accountRepository.selectByEmail("neskovic.milos02@gmail.com")
                .addOnSuccessListener(account -> {
                    if (getContext() != null) {
                        this.account = account;
                    }
                });

        changePasswordButton.setOnClickListener(v -> changePassword());

        return view;
    }

    private void changePassword() {
        String oldPassword = oldPasswordEditText.getText().toString();
        String newPassword = newPasswordEditText.getText().toString();
        String confirmPassword = confirmNewPasswordEditText.getText().toString();

        if (!oldPassword.equals(account.getPassword())) {
            Toast.makeText(getContext(), "Stara lozinka nije tačna", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Nove lozinke se ne poklapaju", Toast.LENGTH_SHORT).show();
            return;
        }

        account.setPassword(newPassword);
        AccountRepository.update(account);

        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.mainContainer, new ProfileFragment());
        transaction.commit();

    }

}
