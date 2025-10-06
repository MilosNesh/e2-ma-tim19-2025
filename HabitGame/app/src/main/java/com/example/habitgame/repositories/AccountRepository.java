package com.example.habitgame.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habitgame.model.Account;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountRepository {
    public static void initDB(){

    }

    public static void insert(Account account){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Kreiraj korisnika putem Firebase Authentication
        mAuth.createUserWithEmailAndPassword(account.getEmail(), account.getPassword())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser(); // Sada je korisnik autentifikovan

                        if (user != null && !user.isEmailVerified()) {
                            // Pošaljite verifikacioni email
                            user.sendEmailVerification()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("EMAIL", "Verifikacioni email poslat.");

                                        account.setIsVerified(false);
                                        account.setRegistrationTimestamp(System.currentTimeMillis());

                                        db.collection("accounts")
                                                .add(account)
                                                .addOnSuccessListener(documentReference -> {
                                                    FirebaseAuth.getInstance().signOut();
                                                    Log.d("REZ_DB", "DocumentSnapshot added with ID: " + documentReference.getId());
                                                })
                                                .addOnFailureListener(e -> Log.w("REZ_DB", "Error adding document", e));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("EMAIL", "Greška prilikom slanja emaila.", e);
                                    });
                        }
                    } else {
                        // Greška prilikom kreiranja korisnika
                        Log.e("AUTH", "Greška prilikom kreiranja korisnika", task.getException());
                    }
                });
    }

    public static Task<List<Account>> select(){
        TaskCompletionSource<List<Account>> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Account> accountList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Account account = document.toObject(Account.class);
                                accountList.add(account);
                                Log.d("REZ_DB", document.getId() + " => " + document.getData());
                            }
                            taskCompletionSource.setResult(accountList);
                        } else {
                            Log.w("REZ_DB", "Error getting documents.", task.getException());
                            taskCompletionSource.setResult(null);
                        }
                    }
                });
        return taskCompletionSource.getTask();
    }

    public Task<Account> selectByUsername(String username){
        TaskCompletionSource<Account> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                    Account account = document.toObject(Account.class);
                    taskCompletionSource.setResult(account);
                } else {
                    Log.d("NoUserFound", "Nema korisnika sa tim korisničkim imenom.");
                    taskCompletionSource.setException(new Exception("Nema korisnika sa tim korisničkim imenom."));
                }
            })
            .addOnFailureListener(e -> {
                Log.e("FirebaseError", "Greška prilikom pretrage korisnika: ", e);
                taskCompletionSource.setException(e);
            });
        return taskCompletionSource.getTask();
    }

    public static Task<Account> getAccountById(String userId) {
        TaskCompletionSource<Account> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Account account = documentSnapshot.toObject(Account.class);
                        taskCompletionSource.setResult(account);
                    } else {
                        Log.d("NoUserFound", "Nalog sa ID-om nije pronađen.");
                        taskCompletionSource.setResult(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Greška prilikom dohvata korisnika po ID-u: ", e);
                    taskCompletionSource.setException(e);
                });

        return taskCompletionSource.getTask();
    }

    public Task<Account> selectByEmail(String email){
        TaskCompletionSource<Account> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        Account account = document.toObject(Account.class);
                        taskCompletionSource.setResult(account);
                    } else {
                        Log.d("NoUserFound", "Nema korisnika sa tim emailom");
                        taskCompletionSource.trySetResult(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Greška prilikom pretrage korisnika: ", e);
                    taskCompletionSource.setException(e);
                });
        return taskCompletionSource.getTask();
    }

    public static void deleteAll(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d("REZ_DB", "Account " + document.getId() + " has been deleted."))
                                .addOnFailureListener(e -> Log.w("REZ_DB", "Error deleting account.", e));
                    }
                })
                .addOnFailureListener(e -> Log.w("REZ_DB", "Error fetching accounts.", e));

    }

    public static void update(Account account){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("email", account.getEmail())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DocumentReference docRef = doc.getReference();

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("password", account.getPassword());
                        updates.put("equipments", account.getEquipments());
                        updates.put("coins", account.getCoins());
                        updates.put("friends", account.getFriends());
                        updates.put("title", account.getTitle());
                        updates.put("level", account.getLevel());
                        updates.put("experiencePoints", account.getExperiencePoints());
                        updates.put("powerPoints", account.getPowerPoints());
                        updates.put("badgeNumbers", account.getBadgeNumbers());
                        docRef.update(updates)
                                .addOnSuccessListener(aVoid ->
                                        Log.d("REZ_DB", "Successfully updated user: " + account.getEmail()))
                                .addOnFailureListener(e ->
                                        Log.w("REZ_DB", "Error updating user: " + account.getEmail(), e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("REZ_DB", "Error querying username: " + account.getEmail(), e);
                });

    }

    public static void updateIsVerified(String email, boolean verified) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update("isVerified", verified)
                                .addOnSuccessListener(aVoid -> Log.d("REZ_DB", "isVerified updated"))
                                .addOnFailureListener(e -> Log.e("REZ_DB", "Failed to update isVerified", e));
                    }
                });
    }

    public static void deleteByEmail(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d("REZ_DB", "Account deleted due to expired link."))
                                .addOnFailureListener(e -> Log.e("REZ_DB", "Failed to delete expired account.", e));
                    }
                });
    }

    public Task<List<Account>> selectByUsernameContains(String usernameSubstring) {
        TaskCompletionSource<List<Account>> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .orderBy("username")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Account> accounts = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Account account = document.toObject(Account.class);
                            if(account.getUsername().toLowerCase().contains(usernameSubstring.toLowerCase()))
                                 accounts.add(account);
                        }
                        taskCompletionSource.setResult(accounts);
                    } else {
                        Log.d("NoUsersFound", "Nema korisnika koji sadrže zadati deo korisničkog imena.");
                        taskCompletionSource.setResult(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Greška prilikom pretrage korisnika: ", e);
                    taskCompletionSource.setException(e);
                });

        return taskCompletionSource.getTask();
    }

    public static Task<List<Account>> selectAllExpectMine(String email){
        TaskCompletionSource<List<Account>> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Account> accountList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Account account = document.toObject(Account.class);
                                if(!account.getEmail().equals(email) && account.getIsVerified())
                                    accountList.add(account);
                                Log.d("REZ_DB", document.getId() + " => " + document.getData());
                            }
                            taskCompletionSource.setResult(accountList);
                        } else {
                            Log.w("REZ_DB", "Error getting documents.", task.getException());
                            taskCompletionSource.setResult(null);
                        }
                    }
                });
        return taskCompletionSource.getTask();
    }

    public static Task<List<Account>> selectAllFriends(String email) {
        TaskCompletionSource<List<Account>> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Account> accountList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Account account = document.toObject(Account.class);
                                if(!account.getEmail().equals(email) && account.getIsVerified() && account.getFriends().contains(email))
                                    accountList.add(account);
                                Log.d("REZ_DB", document.getId() + " => " + document.getData());
                            }
                            taskCompletionSource.setResult(accountList);
                        } else {
                            Log.w("REZ_DB", "Error getting documents.", task.getException());
                            taskCompletionSource.setResult(null);
                        }
                    }
                });
        return taskCompletionSource.getTask();
    }

    public static void updateFcmToken(String email, String token) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d("REZ_DB", "isVerified updated"))
                                .addOnFailureListener(e -> Log.e("REZ_DB", "Failed to update isVerified", e));
                    }
                });
    }

    public static Task<Account> updateAlliance(String email, String allianceId) {
        TaskCompletionSource<Account> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        String accountId = document.getId();

                        // Ažuriraj allianceId
                        document.getReference().update("allianceId", allianceId)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("REZ_DB", "AllianceId updated");

                                    // Ponovo učitaj dokument i konvertuj u Account objekat
                                    db.collection("accounts")
                                            .document(accountId)
                                            .get()
                                            .addOnSuccessListener(updatedDoc -> {
                                                if (updatedDoc.exists()) {
                                                    Account updatedAccount = updatedDoc.toObject(Account.class);
                                                    taskCompletionSource.setResult(updatedAccount);
                                                } else {
                                                    taskCompletionSource.setResult(null);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                taskCompletionSource.setResult(null);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("REZ_DB", "Failed to update allianceId", e);
                                    taskCompletionSource.setResult(null);
                                });

                    } else {
                        Log.w("REZ_DB", "Nalog sa email-om nije pronađen");
                        taskCompletionSource.setResult(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("REZ_DB", "Greška pri pretrazi naloga po emailu", e);
                    taskCompletionSource.setResult(null);
                });

        return taskCompletionSource.getTask();
    }

    public static Task<Void> updateXp(String userId, int xpEarned) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        return getAccountById(userId)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Account account = task.getResult();

                        account.addExperiencePoints(xpEarned);

                        // 3. Ažuriraj ceo dokument direktno po ID-u
                        return db.collection("accounts").document(userId).set(account)
                                .addOnSuccessListener(aVoid -> Log.d("REZ_DB", "XP successfully updated for user: " + userId))
                                .addOnFailureListener(e -> {
                                    Log.w("REZ_DB", "Error updating XP for user: " + userId, e);
                                    throw new RuntimeException(e); // Propagiraj grešku dalje
                                });
                    } else {
                        // Ako dohvat nije uspeo ili je nalog null
                        Exception e = task.getException() != null ? task.getException() : new Exception("Nalog nije pronađen.");
                        Log.e("REZ_DB", "Greška pri dohvatanju naloga za XP update.", e);
                        return com.google.android.gms.tasks.Tasks.forException(e);
                    }
                });
    }

    public static Task<List<Account>> getByAlliance(String allianceId) {
        TaskCompletionSource<List<Account>> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("allianceId", allianceId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Account> accounts = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Account account = document.toObject(Account.class);
                            accounts.add(account);
                        }
                        taskCompletionSource.setResult(accounts);
                    } else {
                        Log.d("NoAccountsFound", "Nema korisnika sa tim allianceId");
                        taskCompletionSource.trySetResult(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Greška prilikom pretrage korisnika: ", e);
                    taskCompletionSource.setException(e);
                });

        return taskCompletionSource.getTask();
    }

}
