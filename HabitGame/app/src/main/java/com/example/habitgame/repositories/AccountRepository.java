package com.example.habitgame.repositories;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitgame.model.Account;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountRepository {
    public static void insert(Account account){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        mAuth.createUserWithEmailAndPassword(account.getEmail(), account.getPassword())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("AUTH", "Greška prilikom kreiranja korisnika", task.getException());
                        return;
                    }
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        Log.e("AUTH", "User null posle createUserWithEmailAndPassword");
                        return;
                    }

                    user.sendEmailVerification()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("EMAIL", "Verifikacioni email poslat.");

                                account.setIsVerified(false);
                                account.setRegistrationTimestamp(System.currentTimeMillis());

                                String uid = user.getUid();
                                Map<String, Object> data = new HashMap<>();
                                data.put("username", account.getUsername());
                                data.put("email", account.getEmail());
                                data.put("avatar", account.getAvatar());
                                data.put("password", account.getPassword());
                                data.put("level", account.getLevel());
                                data.put("title", account.getTitle());
                                data.put("powerPoints", account.getPowerPoints());
                                data.put("experiencePoints", account.getExperiencePoints());
                                data.put("coins", account.getCoins());
                                data.put("badgeNumbers", account.getBadgeNumbers());
                                data.put("equipments", account.getEquipments());
                                data.put("isVerified", account.getIsVerified());
                                data.put("registrationTimestamp", account.getRegistrationTimestamp());
                                data.put("friends", account.getFriends());
                                data.put("fcmToken", account.getFcmToken());
                                data.put("allianceId", account.getAllianceId());
                                data.put("uid", uid);

                                // NOVO: default false
                                data.put("pendingBoss", account.getPendingBoss() != null ? account.getPendingBoss() : false);

                                db.collection("accounts").document(uid)
                                        .set(data, SetOptions.merge())
                                        .addOnSuccessListener(ref -> {
                                            FirebaseAuth.getInstance().signOut();
                                            Log.d("REZ_DB", "Account upisan na docId=uid: " + uid);
                                        })
                                        .addOnFailureListener(e -> Log.w("REZ_DB", "Error writing account", e));
                            })
                            .addOnFailureListener(e -> Log.e("EMAIL", "Greška prilikom slanja emaila.", e));
                });
    }

    public static Task<List<Account>> select(){
        TaskCompletionSource<List<Account>> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Account> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        Account a = d.toObject(Account.class);
                        out.add(a);
                        Log.d("REZ_DB", d.getId() + " => " + d.getData());
                    }
                    tcs.setResult(out);
                })
                .addOnFailureListener(e -> {
                    Log.w("REZ_DB", "Error getting documents.", e);
                    tcs.setResult(null);
                });
        return tcs.getTask();
    }

    public Task<Account> selectByEmail(String email){
        TaskCompletionSource<Account> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        tcs.setResult(q.getDocuments().get(0).toObject(Account.class));
                    } else {
                        tcs.trySetResult(null);
                    }
                })
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    public static void update(Account account){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("email", account.getEmail())
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot doc : q) {
                        DocumentReference ref = doc.getReference();
                        Map<String, Object> up = new HashMap<>();
                        up.put("password", account.getPassword());
                        up.put("equipments", account.getEquipments());
                        up.put("coins", account.getCoins());
                        up.put("friends", account.getFriends());
                        up.put("title", account.getTitle());
                        up.put("level", account.getLevel());
                        up.put("experiencePoints", account.getExperiencePoints());
                        up.put("powerPoints", account.getPowerPoints());
                        up.put("badgeNumbers", account.getBadgeNumbers());
                        up.put("uid", getUid()); // veži uid ako fali

                        if (account.getPendingBoss() != null) {
                            up.put("pendingBoss", account.getPendingBoss());
                        }

                        ref.update(up)
                                .addOnSuccessListener(a -> Log.d("REZ_DB","Updated user: "+account.getEmail()))
                                .addOnFailureListener(e -> Log.w("REZ_DB","Update error: "+account.getEmail(), e));
                    }
                })
                .addOnFailureListener(e -> Log.w("REZ_DB","Error querying email: "+account.getEmail(), e));
    }

    public static void updateIsVerified(String email, boolean verified) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot doc : q) {
                        doc.getReference().update("isVerified", verified);
                    }
                });
    }

    public static void deleteByEmail(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot doc : q) {
                        doc.getReference().delete();
                    }
                });
    }

    public Task<List<Account>> selectByUsernameContains(String usernameSubstring) {
        TaskCompletionSource<List<Account>> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .orderBy("username")
                .get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) { tcs.setResult(null); return; }
                    List<Account> out = new ArrayList<>();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        Account a = d.toObject(Account.class);
                        if (a != null && a.getUsername()!=null &&
                                a.getUsername().toLowerCase().contains(usernameSubstring.toLowerCase())) {
                            out.add(a);
                        }
                    }
                    tcs.setResult(out);
                })
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }

    public static Task<List<Account>> selectAllExpectMine(String email){
        TaskCompletionSource<List<Account>> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .get()
                .addOnSuccessListener(q -> {
                    List<Account> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : q) {
                        Account a = d.toObject(Account.class);
                        if (a != null && a.getEmail()!=null && !a.getEmail().equals(email) && a.getIsVerified())
                            out.add(a);
                    }
                    tcs.setResult(out);
                })
                .addOnFailureListener(e -> { Log.w("REZ_DB","Error getting documents.", e); tcs.setResult(null); });
        return tcs.getTask();
    }

    public static Task<List<Account>> selectAllFriends(String email) {
        TaskCompletionSource<List<Account>> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .get()
                .addOnSuccessListener(q -> {
                    List<Account> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : q) {
                        Account a = d.toObject(Account.class);
                        if (a!=null && a.getEmail()!=null && !a.getEmail().equals(email)
                                && a.getIsVerified() && a.getFriends()!=null && a.getFriends().contains(email)) {
                            out.add(a);
                        }
                    }
                    tcs.setResult(out);
                })
                .addOnFailureListener(e -> { Log.w("REZ_DB","Error getting documents.", e); tcs.setResult(null); });
        return tcs.getTask();
    }

    public static void updateFcmToken(String email, String token) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot doc : q) {
                        doc.getReference().update("fcmToken", token, "uid", getUid());
                    }
                });
    }

    public static Task<Account> updateAlliance(String email, String allianceId) {
        TaskCompletionSource<Account> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) { tcs.setResult(null); return; }
                    DocumentSnapshot document = q.getDocuments().get(0);
                    String docId = document.getId();
                    document.getReference().update("allianceId", allianceId, "uid", getUid())
                            .addOnSuccessListener(aVoid ->
                                    db.collection("accounts").document(docId).get()
                                            .addOnSuccessListener(updated -> tcs.setResult(updated.toObject(Account.class)))
                                            .addOnFailureListener(e -> tcs.setResult(null)))
                            .addOnFailureListener(e -> { Log.e("REZ_DB","Failed to update allianceId", e); tcs.setResult(null); });
                })
                .addOnFailureListener(e -> { Log.e("REZ_DB","Greška pri pretrazi naloga po emailu", e); tcs.setResult(null); });

        return tcs.getTask();
    }

    public static Task<List<Account>> getByAlliance(String allianceId) {
        TaskCompletionSource<List<Account>> tcs = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("accounts")
                .whereEqualTo("allianceId", allianceId)
                .get()
                .addOnSuccessListener(q -> {
                    List<Account> out = new ArrayList<>();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        Account a = d.toObject(Account.class);
                        if (a != null) out.add(a);
                    }
                    tcs.setResult(out);
                })
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }
    @Nullable
    private static String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    public static com.google.android.gms.tasks.Task<Void> addXpAndCheckLevelUp(int deltaXp) {
        if (deltaXp <= 0) return com.google.android.gms.tasks.Tasks.forResult(null);

        String email = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser()!=null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        if (email == null) return com.google.android.gms.tasks.Tasks.forException(new IllegalStateException("Nema ulogovanog korisnika."));

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        return db.collection("accounts").whereEqualTo("email", email).limit(1).get()
                .continueWithTask(q -> {
                    if (!q.isSuccessful() || q.getResult()==null || q.getResult().isEmpty())
                        return com.google.android.gms.tasks.Tasks.forException(new IllegalStateException("Nalog nije pronađen."));

                    com.google.firebase.firestore.DocumentSnapshot doc = q.getResult().getDocuments().get(0);

                    int curXp  = doc.getLong("experiencePoints")==null ? 0 : doc.getLong("experiencePoints").intValue();
                    int curLvl = doc.getLong("level")==null ? 1 : doc.getLong("level").intValue();
                    int curPP  = doc.getLong("powerPoints")==null ? 0 : doc.getLong("powerPoints").intValue();

                    int newXp = Math.max(0, curXp + deltaXp);

                    int newLevel = com.example.habitgame.utils.LevelUtils.levelFromXp(newXp);
                    boolean leveledUp = newLevel > curLvl;
                    int levelUps = Math.max(0, newLevel - curLvl);

                    java.util.Map<String,Object> up = new java.util.HashMap<>();
                    up.put("experiencePoints", newXp);

                    if (leveledUp) {
                        up.put("level", newLevel);
                        up.put("title", com.example.habitgame.utils.LevelUtils.titleForLevel(newLevel));
                        up.put("pendingBoss", true);

                        int newPP = curPP;

                        if (curLvl < 1 && newLevel >= 1) {
                            newPP = Math.max(curPP, 40);
                        }

                        int timesToGrow = 0;
                        if (newLevel >= 2) {
                            int from = Math.max(1, curLvl);
                            int to   = newLevel;
                            timesToGrow = Math.max(0, to - from);
                        }
                        if (timesToGrow > 0) {
                            newPP = com.example.habitgame.utils.LevelUtils.applyPpGrowth(newPP, timesToGrow);
                        }

                        up.put("powerPoints", newPP);
                    }


                    return doc.getReference().update(up);
                });
    }

    public static Task<Void> setPendingBossForEmail(@NonNull String email, boolean value) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        db.collection("accounts").whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) { tcs.setException(new IllegalStateException("Nalog nije nađen.")); return; }
                    q.getDocuments().get(0).getReference()
                            .update("pendingBoss", value)
                            .addOnSuccessListener(v -> tcs.setResult(null))
                            .addOnFailureListener(tcs::setException);
                })
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }
}
