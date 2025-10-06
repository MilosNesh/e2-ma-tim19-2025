package com.example.habitgame.services;

import android.util.Log;

import com.example.habitgame.R;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.AccountListCallback;
import com.example.habitgame.model.Equipment;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.repositories.AccountRepository;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class AccountService {
    private AccountRepository accountRepository;

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
        FirebaseAuth auth = FirebaseAuth.getInstance();
        AccountRepository accountRepository = new AccountRepository();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {
                            user.reload().addOnCompleteListener(reloadTask -> {
                                if (reloadTask.isSuccessful()) {
                                    if (!user.isEmailVerified()) {
                                        accountRepository.selectByEmail(email)
                                                .addOnSuccessListener(account -> {
                                                    if (account == null) {
                                                        callback.onResult("Ne postoji korisnik sa tim email-om.");
                                                        return;
                                                    }

                                                    long elapsed = System.currentTimeMillis() - account.getRegistrationTimestamp();
                                                    if (elapsed > 2 * 60 * 1000) { // više od 2 minute
                                                        user.delete().addOnCompleteListener(deleteTask -> {
                                                            if (deleteTask.isSuccessful()) {
                                                                AccountRepository.deleteByEmail(user.getEmail());
                                                                auth.signOut();
                                                                callback.onResult("Link za verifikaciju je istekao. Registrujte se ponovo.");
                                                            } else {
                                                                Log.e("Firebase", "Greška pri brisanju korisnika", deleteTask.getException());
                                                                callback.onResult("Greška pri brisanju korisničkog naloga.");
                                                            }
                                                        });
                                                    } else {
                                                        callback.onResult("Molimo verifikujte email pre prijave.");
                                                    }
                                                })
                                                .addOnFailureListener(e -> callback.onResult("Greška prilikom traženja korisnika."));
                                    } else {
                                        AccountRepository.updateIsVerified(user.getEmail(), true);

                                        accountRepository.selectByEmail(email)
                                                .addOnSuccessListener(account -> {
                                                    if (account == null) {
                                                        callback.onResult("Ne postoji korisnik sa tim email-om.");
                                                    } else if (account.getPassword().equals(password)) {
                                                        callback.onResult("");
                                                        accountCallback.onResult(account);
                                                    } else {
                                                        callback.onResult("Prijava nije uspela. Pogrešan email ili lozinka.");
                                                    }
                                                });
                                    }
                                } else {
                                    Log.e("Firebase", "Greška pri osvežavanju korisnika", reloadTask.getException());
                                    callback.onResult("Greška pri osvežavanju podataka.");
                                }
                            });
                        }
                    } else {
                        callback.onResult("Prijava nije uspela. Proverite email i lozinku.");
                    }
                });
    }


    public void changePassword(String email, String oldPassword, String newPassword, String confirmPassword, StringCallback callback) {
        accountRepository.selectByEmail(email)
                .addOnSuccessListener(account -> {
                    if (!oldPassword.equals(account.getPassword())) {
                        callback.onResult("Stara lozinka nije tačna");
                        return;
                    }
                    if(newPassword.length()<6) {
                        callback.onResult("Lozinka mora sadrzati barem 6 karaktera.");
                        return;
                    }
                    if (!newPassword.equals(confirmPassword)) {
                        callback.onResult("Nove lozinke se ne poklapaju");
                        return;
                    }

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    user.updatePassword(newPassword)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("PASSWORD", "Lozinka uspešno promenjena!");
                                    account.setPassword(newPassword);
                                    AccountRepository.update(account);
                                    callback.onResult("Lozinka uspjesno promjenjena");
                                } else {
                                    callback.onResult("Greska prilikom promjene lozinke");
                                    Log.e("PASSWORD", "Greška pri promeni lozinke", task.getException());
                                }
                            });
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

    public void getAllAccounts(AccountListCallback callback) {
        AccountRepository.select().addOnSuccessListener(accountList -> {
            callback.onResult(accountList);
        });
    }

    public void searchByUSername(String username, AccountListCallback callback) {
        accountRepository.selectByUsernameContains(username).addOnSuccessListener(accountList -> {
            callback.onResult(accountList);
        });
    }

    public void addFriend(Account account1, String email, StringCallback callback) {
        accountRepository.selectByEmail(email).addOnSuccessListener(account2 -> {
            List<String> friends1 = account1.getFriends();
            if(friends1.contains(email)){
                callback.onResult("Vec ste prijatelji!");
                return;
            }
            friends1.add(email);
            List<String> friends2 = account2.getFriends();
            friends2.add(account1.getEmail());
            account1.setFriends(friends1);
            account2.setFriends(friends2);

            AccountRepository.update(account1);
            AccountRepository.update(account2);
            callback.onResult("Dodali ste novog prijatelja.");
        }).addOnFailureListener(e -> {
            callback.onResult("Doslo je do greske prilikom dodavanja novog prijatelja.");
        });
    }

    public void getAllExpectMine(String email, AccountListCallback callback) {
        AccountRepository.selectAllExpectMine(email).addOnSuccessListener(accountList -> {
            callback.onResult(accountList);
        });
    }

    public void getAllFriends(String email, AccountListCallback callback) {
        AccountRepository.selectAllFriends(email).addOnSuccessListener(accountList -> {
            callback.onResult(accountList);
        });
    }

    public static void updateFcmTokne(String email, String token) {
        AccountRepository.updateFcmToken(email, token);
    }

    public static void updateAlliance(String email, String allianceId, AccountCallback callback) {
        AccountRepository.updateAlliance(email, allianceId).addOnSuccessListener(account -> {
            callback.onResult(account);
        });
    }

    public void getByAlliance(String allianceId, AccountListCallback callback) {
        AccountRepository.getByAlliance(allianceId).addOnSuccessListener(accountList -> {
            callback.onResult(accountList);
        });
    }

    public void leaveAlliance(String email, StringCallback callback) {
        updateAlliance(email, "", new AccountCallback() {
            @Override
            public void onResult(Account account) {
                if(account != null)
                    callback.onResult("Napustili ste savez!");
                else
                    callback.onResult("Greska prilikom napustanja saveza!");
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("Alliance", "Greška pri napuštanju saveza", e);
                callback.onResult("Došlo je do greške prilikom napuštanja saveza: " + e.getMessage());
            }
        });
    }
}
