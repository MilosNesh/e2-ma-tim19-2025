package com.example.habitgame.services;

import android.util.Log;

import com.example.habitgame.R;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.Equipment;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.repositories.AccountRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

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

    public void login(String email, String password, StringCallback callback, AccountCallback accountCallback) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        AccountRepository accountRepository = new AccountRepository();

        if (user != null) {
            user.reload()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (!user.isEmailVerified()) {
                                accountRepository.selectByEmail(email)
                                        .addOnSuccessListener(account -> {
                                            if (account == null){
                                                callback.onResult("Ne postoji korisnik sa tim email-om.");
                                                return;
                                            }
                                            long elapsed = System.currentTimeMillis() - account.getRegistrationTimestamp();
                                            if (elapsed > 2 * 60 * 1000) { // 2 minute je prošlo
                                                // Brisanje korisničkog naloga iz Firebase-a
                                                user.delete()
                                                        .addOnCompleteListener(deleteTask -> {
                                                            if (deleteTask.isSuccessful()) {
                                                                // Obriši podatke o korisniku iz tvoje baze
                                                                AccountRepository.deleteByEmail(user.getEmail());

                                                                // Odjavi korisnika
                                                                FirebaseAuth.getInstance().signOut();

                                                                callback.onResult("Link za verifikaciju je istekao. Registrujte se ponovo.");
                                                            } else {
                                                                // Greška prilikom brisanja korisničkog naloga
                                                                Log.e("Firebase", "Greška pri brisanju korisnika", deleteTask.getException());
                                                                callback.onResult("Greška pri brisanju korisničkog naloga.");
                                                            }
                                                        });
                                            } else {
                                                callback.onResult("Molimo verifikujte email pre prijave.");
                                            }
                                        }).addOnFailureListener(e-> callback.onResult("Ne postoji korisnik sa trazenim emailom!"));
                            } else {
                                AccountRepository.updateIsVerified(user.getEmail(), true);

                                accountRepository.selectByEmail(email)
                                        .addOnSuccessListener(account -> {
                                            if (account == null){
                                                callback.onResult("Ne postoji korisnik sa tim email-om.");
                                            }
                                            if (account != null && account.getPassword().equals(password)) {
                                                callback.onResult("");
                                                accountCallback.onResult(account);
                                            } else {
                                                callback.onResult("Prijava nije uspela. Pogrešan email ili lozinka");
                                            }
                                        });
                            }
                        } else {
                            Log.e("Firebase", "Greška pri osvežavanju korisničkih podataka", task.getException());
                        }
                    }).addOnFailureListener(e-> callback.onResult("Ne postoji korisnik sa trazenim email-om!"));
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
                    if(account != null)
                        callback.onResult(account);
                });
    }

    public void buyEquipment(String email, Equipment equipment, StringCallback callback) {
        accountRepository.selectByEmail(email)
                .addOnSuccessListener(account -> {
                    if(account != null) {
                        if(account.getCoins() < equipment.getPrice()){
                            callback.onResult("Nemate dovoljno coinsa na svom nalogu!");
                            return;
                        }
                        List<Equipment> equipmentList = account.getEquipments();
                        for(Equipment e : equipmentList) {
                            if (e.getName().equals(equipment.getName()) && equipment.getType().equals("odeca")) {
                                callback.onResult("Vec posjedujete ovu odjecu!");
                                return;
                            }
                        }
                        equipmentList.add(equipment);
                        account.setEquipments(equipmentList);
                        account.setCoins(account.getCoins() - (int) equipment.getPrice());
                        AccountRepository.update(account);
                        callback.onResult("Oprema je uspjesno kupljena");
                    }
                });
    }
}
