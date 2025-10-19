package com.example.habitgame.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.habitgame.R;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.Equipment;
import com.example.habitgame.repositories.AccountRepository;
import com.example.habitgame.services.AccountService;
import com.example.habitgame.services.StageMetricsService;
import com.example.habitgame.utils.LevelUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BattleFragment extends Fragment implements SensorEventListener {

    // UI
    private ImageView imgBoss;
    private LottieAnimationView lottieBossIdle, lottieBossHit, lottieChest;
    private TextView tvBossHp, tvUserPp, tvEquip, tvChance, tvAttempts, tvResult;
    private ProgressBar pbBoss, pbUser;
    private MaterialButton btnAttack;

    // State
    private int bossMaxHp = 200;
    private int bossHp = 200;
    private int userPP = 0;
    private int basePP = 0;
    private int attemptsLeft = 5;
    private int hitChancePercent = 67;
    private int currentUserLevel = 1;
    private String currentUserEmail;
    private Account currentAccount;

    // Shake
    private SensorManager sensorMgr;
    private Sensor accelerometer;
    private boolean shakeEnabled = true;
    private long lastShakeTs = 0L;

    // Audio
    private MediaPlayer mpHit, mpWin, mpLoss;

    private final Random rnd = new Random();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_battle, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);

        imgBoss       = v.findViewById(R.id.imgBoss);
        lottieBossIdle= v.findViewById(R.id.lottieBossIdle);
        lottieBossHit = v.findViewById(R.id.lottieBossHit);
        lottieChest   = v.findViewById(R.id.lottieChest);

        tvBossHp   = v.findViewById(R.id.tvBossHp);
        pbBoss     = v.findViewById(R.id.pbBossHp);
        tvUserPp   = v.findViewById(R.id.tvUserPp);
        pbUser     = v.findViewById(R.id.pbUserPp);
        tvEquip    = v.findViewById(R.id.tvEquip);
        tvChance   = v.findViewById(R.id.tvChance);
        tvAttempts = v.findViewById(R.id.tvAttempts);
        tvResult   = v.findViewById(R.id.tvResult);
        btnAttack  = v.findViewById(R.id.btnAttack);

        sensorMgr = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        SharedPreferences sp = requireContext().getSharedPreferences("HabitGamePrefs", Context.MODE_PRIVATE);
        currentUserEmail = sp.getString("email", null);

        try { mpHit  = MediaPlayer.create(getContext(), R.raw.boss_hit_sound); } catch (Exception ignore) {}
        try { mpWin  = MediaPlayer.create(getContext(), R.raw.boss_win); } catch (Exception ignore) {}
        try { mpLoss = MediaPlayer.create(getContext(), R.raw.boss_loss); } catch (Exception ignore) {}

        loadAccountAndInit();

        btnAttack.setOnClickListener(x -> doAttack());
    }

    private void loadAccountAndInit() {
        new AccountService().getAccountByEmail(currentUserEmail, new AccountCallback() {
            @Override public void onResult(Account acc) {
                if (acc == null) { toast("Nalog nije pronađen."); close(); return; }
                currentAccount = acc;

                currentUserLevel = Math.max(1, acc.getLevel());
                bossMaxHp = LevelUtils.bossHpForLevel(currentUserLevel);
                bossHp = bossMaxHp;

                basePP = Math.max(0, acc.getPowerPoints());

                final StageMetricsService sms = new StageMetricsService();
                final StageMetricsService.Callback cb = new StageMetricsService.Callback() {
                    @Override public void onReady(int successRate) {
                        hitChancePercent = Math.max(0, Math.min(100, successRate));
                        maybePromptEquipmentThenBindUI();
                    }
                    @Override public void onError(Exception e) {
                        hitChancePercent = 0;
                        maybePromptEquipmentThenBindUI();
                    }
                };

                long etapaStart = acc.getLastLevelUpTimestamp();

                if (etapaStart > 0) {
                    sms.computeStageSuccessSince(etapaStart, /*quota*/ null, cb);
                } else {
                    sms.computeCurrentStageSuccess(/*quota*/ null, cb);
                }
            }
            @Override public void onFailure(Exception e) { toast("Greška: "+e.getMessage()); close(); }
        });
    }



    private void maybePromptEquipmentThenBindUI() {
        List<Equipment> eq = currentAccount.getEquipments();
        if (eq == null || eq.isEmpty()) {
            recomputeUserPP();
            bindUi(currentAccount);
            startIdle();
            return;
        }

        // Multi-choice dijalog
        final String[] names = new String[eq.size()];
        final boolean[] checked = new boolean[eq.size()];
        for (int i = 0; i < eq.size(); i++) {
            Equipment e = eq.get(i);
            names[i] = safeName(e);
            checked[i] = e != null && e.isActivated();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Izaberi opremu za borbu")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setNegativeButton("Otkaži", (d, w) -> {
                    recomputeUserPP();
                    bindUi(currentAccount);
                    startIdle();
                })
                .setPositiveButton("Primeni", (dialog, which) -> {
                    List<Equipment> list = currentAccount.getEquipments();
                    for (int i = 0; i < list.size(); i++) {
                        Equipment e = list.get(i);
                        if (e != null) e.setActivated(checked[i]);
                    }
                    new AccountService().update(currentAccount);

                    recomputeUserPP();
                    bindUi(currentAccount);
                    startIdle();
                })
                .setCancelable(false)
                .show();
    }

    private void recomputeUserPP() {
        int bonus = 0;
        List<Equipment> list = currentAccount.getEquipments();
        if (list != null) {
            for (Equipment e : list) {
                if (e != null && e.isActivated()) {
                    bonus += computeBonusFor(e, basePP);
                }
            }
        }
        userPP = Math.max(0, basePP + bonus);
    }

    private int computeBonusFor(@NonNull Equipment e, int base) {
        String eff = e.getEffect() == null ? "" : e.getEffect().trim().toLowerCase(Locale.ROOT);
        double val = e.getEffectPercentage();
        if ("pp_flat".equals(eff)) {
            return (int)Math.round(val);
        } else if ("pp_percent".equals(eff)) {
            return (int)Math.round(base * (val / 100.0));
        }
        return 0;
    }

    private void bindUi(Account acc) {
        // Boss
        tvBossHp.setText(String.format(Locale.getDefault(),"Boss HP: %d/%d", bossHp, bossMaxHp));
        pbBoss.setMax(bossMaxHp);
        pbBoss.setProgress(bossHp);

        // PP
        tvUserPp.setText("PP: " + userPP + "  (osnovni " + basePP + ")");
        pbUser.setMax(Math.max(100, userPP));
        pbUser.setProgress(userPP);

        StringBuilder eqTxt = new StringBuilder();
        List<Equipment> list = acc.getEquipments();
        if (list != null) {
            for (Equipment e : list) {
                if (e != null && e.isActivated()) {
                    if (eqTxt.length() > 0) eqTxt.append(", ");
                    String add = e.getEffect() != null ? " [" + e.getEffect() + ":" + e.getEffectPercentage() + "]" : "";
                    eqTxt.append(safeName(e)).append(add);
                }
            }
        }
        tvEquip.setText(eqTxt.length()==0 ? "Aktivna oprema: -" : "Aktivna oprema: " + eqTxt);

        // Šansa & pokušaji
        tvChance.setText("Šansa: " + hitChancePercent + "%");
        tvAttempts.setText("Pokušaji: " + attemptsLeft + "/5");
    }

    private void doAttack() {
        if (attemptsLeft <= 0 || bossHp <= 0) return;

        boolean hit = rnd.nextInt(100) < hitChancePercent;
        attemptsLeft--;
        tvAttempts.setText("Pokušaji: " + attemptsLeft + "/5");

        if (hit) {
            playHit();
            bossHp = Math.max(0, bossHp - userPP);
            tvBossHp.setText(String.format(Locale.getDefault(),"Boss HP: %d/%d", bossHp, bossMaxHp));
            pbBoss.setProgress(bossHp);
        } else {
            toast("Promašaj!");
        }

        if (attemptsLeft == 0 || bossHp == 0) finishBattle();
    }

    private void startIdle() {
        if (lottieBossIdle != null) {
            lottieBossIdle.setVisibility(View.VISIBLE);
            if (!lottieBossIdle.isAnimating()) {
                lottieBossIdle.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                lottieBossIdle.playAnimation();
            } else {
                lottieBossIdle.resumeAnimation();
            }
        }

        if (lottieBossHit != null) {
            lottieBossHit.removeAllAnimatorListeners();
            lottieBossHit.setVisibility(View.GONE);
            lottieBossHit.setProgress(0f);  // reset na početak
        }
    }

    private void playHit() {
        if (mpHit != null) try { mpHit.start(); } catch (Exception ignore) {}

        if (lottieBossIdle != null) {
            lottieBossIdle.pauseAnimation();
        }

        if (lottieBossHit != null) {
            lottieBossHit.removeAllAnimatorListeners();
            lottieBossHit.setVisibility(View.VISIBLE);
            lottieBossHit.setRepeatCount(0);
            lottieBossHit.setProgress(0f);   // uvek od starta
            lottieBossHit.addAnimatorListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    if (lottieBossHit != null) {
                        lottieBossHit.post(() -> startIdle());
                    }
                }
            });
            lottieBossHit.playAnimation();
        }
    }

    private void finishBattle() {
        btnAttack.setEnabled(false);
        shakeEnabled = false;

        boolean defeated = (bossHp <= 0);

        int coins = LevelUtils.coinsForLevel(currentUserLevel);
        boolean dropsEquipment = false;

        if (defeated) {
            dropsEquipment = rnd.nextInt(100) < 20;
        } else {
            boolean halfHp = (bossHp <= bossMaxHp / 2);
            if (halfHp) {
                coins = coins / 2;
                dropsEquipment = rnd.nextInt(100) < 10;
            } else {
                coins = 0;
                dropsEquipment = false;
            }
        }

        final boolean defeatedFinal = defeated;
        final int rewardCoins = coins;
        final boolean rewardDropsEquipment = dropsEquipment;

        AccountService as = new AccountService();
        as.getAccountByEmail(currentUserEmail, new AccountCallback() {
            @Override public void onResult(Account acc) {
                if (acc != null) {
                    acc.setCoins(Math.max(0, acc.getCoins()) + Math.max(0, rewardCoins));
                    as.update(acc);
                }

                if (defeatedFinal) {
                    if (mpWin != null) try { mpWin.start(); } catch (Exception ignore) {}
                    tvResult.setText("Pobeda! +" + rewardCoins + " novčića" + (rewardDropsEquipment ? " + oprema" : ""));
                } else {
                    if (mpLoss != null) try { mpLoss.start(); } catch (Exception ignore) {}
                    if (rewardCoins > 0 || rewardDropsEquipment) {
                        tvResult.setText("Bos nije pao, ali nagrada: +" + rewardCoins + " novčića" + (rewardDropsEquipment ? " + oprema" : ""));
                    } else {
                        tvResult.setText("Bos je pobegao! Bez nagrade.");
                    }
                }

                if (lottieChest != null) {
                    lottieChest.setVisibility(View.VISIBLE);
                    lottieChest.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                    lottieChest.playAnimation();
                }

                AccountRepository.setPendingBossForEmail(currentUserEmail, false);
            }

            @Override public void onFailure(Exception e) {
                toast("Nagrada nije upisana: " + e.getMessage());
            }
        });
    }

    @Override public void onResume() {
        super.onResume();
        if (accelerometer != null) sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override public void onPause() {
        super.onPause();
        sensorMgr.unregisterListener(this);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        try { if (lottieBossHit != null) { lottieBossHit.removeAllAnimatorListeners(); lottieBossHit.cancelAnimation(); } } catch (Exception ignore) {}
        try { if (lottieBossIdle != null) { lottieBossIdle.cancelAnimation(); } } catch (Exception ignore) {}
        try { if (lottieChest != null) { lottieChest.cancelAnimation(); } } catch (Exception ignore) {}
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!shakeEnabled) return;
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0], y = event.values[1], z = event.values[2];
        double mag = Math.sqrt(x*x + y*y + z*z);
        long now = System.currentTimeMillis();
        if (mag > 18 && (now - lastShakeTs) > 600) {
            lastShakeTs = now;
            doAttack();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    private void toast(String s){ Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show(); }
    private void close(){ NavHostFragment.findNavController(this).popBackStack(); }
    private static String safeName(Equipment e){
        String n = (e.getName()==null || e.getName().trim().isEmpty()) ? "Oprema" : e.getName().trim();
        return n;
    }
}
