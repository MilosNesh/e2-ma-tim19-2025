package com.example.habitgame.services;

import android.util.Log;

import com.example.habitgame.R;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.repositories.AccountRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountService {
    private AccountRepository accountRepository;
    private Account account;

    public AccountService(){
        accountRepository = new AccountRepository();
    }
    public String register(Account account, String confirmPassword, int selectedAvatar){

        Integer[] avatars = new Integer[] {
                R.drawable.avatar1, R.drawable.avatar2, R.drawable.avatar3,
                R.drawable.avatar4, R.drawable.avatar5
        };
        if (account.getEmail().isEmpty() || account.getUsername().isEmpty() || account.getPassword().isEmpty() || confirmPassword.isEmpty()) {
            return "Sva polja su obavezna!";
        }

        if (account.getPassword().length() < 6) {
            return "Lozinka mora sadrzati barem 6 karaktera!";
        }

        if (!account.getPassword().equals(confirmPassword)) {
            return "Lozinke se nepodudaraju!";
        }

        if (selectedAvatar == -1) {
            return "Izaberite avatara!";
        }

        account.setAvatar(avatars[selectedAvatar]);

        AccountRepository.insert(account);
        return "Uspjesna registracija";
    }

    public void login(String email, String password, StringCallback callback) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        AccountRepository accountRepository = new AccountRepository();

        if (user != null) {
            user.reload()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (!user.isEmailVerified()) {
                                accountRepository.selectByEmail(email)
                                        .addOnSuccessListener(account -> {
                                            long elapsed = System.currentTimeMillis() - account.getRegistrationTimestamp();
                                            if (elapsed > 24 * 60 * 60 * 1000) {
                                                AccountRepository.deleteByEmail(user.getEmail());
                                                FirebaseAuth.getInstance().signOut();
                                                callback.onResult("Link za verifikaciju je istekao. Registrujte se ponovo.");
                                            } else {
                                                callback.onResult("Molimo verifikujte email pre prijave.");
                                            }
                                        });
                            } else {
                                AccountRepository.updateIsVerified(user.getEmail(), true);

                                accountRepository.selectByEmail(email)
                                        .addOnSuccessListener(account -> {

                                            if (account != null && account.getPassword().equals(password)) {
                                                callback.onResult("");
                                            } else {
                                                callback.onResult("Prijava nije uspela. Pogrešan email ili lozinka");
                                            }
                                        });

                            }
                        } else {
                            Log.e("Firebase", "Greška pri osvežavanju korisničkih podataka", task.getException());
                        }
                    });
        }
    }

    public void changePassword(String email, String oldPassword, String newPassword, String confirmPassword, StringCallback callback) {
        accountRepository.selectByEmail(email)
                .addOnSuccessListener(account -> {
                    if (!oldPassword.equals(account.getPassword())) {
                        callback.onResult("Stara lozinka nije tačna");
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        callback.onResult("Nove lozinke se ne poklapaju");
                        return;
                    }

                    account.setPassword(newPassword);
                    AccountRepository.update(account);
                    callback.onResult("Lozinka uspjesno promjenjena");
                });
    }

    public void getAccountByEmail(String email, AccountCallback callback) {
        accountRepository.selectByEmail(email)
                .addOnSuccessListener(account -> {
                    callback.onResult(account);
                });
    }
}
