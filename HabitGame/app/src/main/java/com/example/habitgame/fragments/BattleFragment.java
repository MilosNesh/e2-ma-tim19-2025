package com.example.habitgame.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
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

import java.util.*;

public class BattleFragment extends Fragment implements SensorEventListener {

    private LottieAnimationView lottieBossIdle, lottieBossHit;
    private LottieAnimationView lottieChestAnim, lottieConfetti;
    private TextView tvBossHp, tvUserPp, tvEquip, tvChance, tvAttempts, tvResult, tvRewardSummary;
    private ProgressBar pbBoss, pbUser;
    private MaterialButton btnAttack;
    private ImageView imgRewardEquip;
    private LinearLayout rewardIcons;

    private int bossMaxHp = 200;
    private int bossHp = 200;
    private int userPP = 0;
    private int basePP = 0;
    private int attemptsLeft = 5;
    private int hitChancePercent = 67;
    private int currentUserLevel = 1;
    private String currentUserEmail;
    private Account currentAccount;

    private boolean battleOver = false;
    private boolean chestOpened = false;
    private int rewardCoins = 0;
    private @Nullable String rewardEquipType = null;

    private SensorManager sensorMgr;
    private Sensor accelerometer;
    private boolean shakeEnabled = true;
    private long lastShakeTs = 0L;

    // Audio
    private MediaPlayer mpHit, mpWin, mpLoss, mpChestOpen, mpConfetti;

    private final Random rnd = new Random();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_battle, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);

        lottieBossIdle   = v.findViewById(R.id.lottieBossIdle);
        lottieBossHit    = v.findViewById(R.id.lottieBossHit);

        tvBossHp         = v.findViewById(R.id.tvBossHp);
        pbBoss           = v.findViewById(R.id.pbBossHp);
        tvUserPp         = v.findViewById(R.id.tvUserPp);
        pbUser           = v.findViewById(R.id.pbUserPp);
        tvEquip          = v.findViewById(R.id.tvEquip);
        tvChance         = v.findViewById(R.id.tvChance);
        tvAttempts       = v.findViewById(R.id.tvAttempts);
        tvResult         = v.findViewById(R.id.tvResult);
        btnAttack        = v.findViewById(R.id.btnAttack);

        lottieChestAnim  = v.findViewById(R.id.lottieChestAnim);
        lottieConfetti   = v.findViewById(R.id.lottieConfetti);
        tvRewardSummary  = v.findViewById(R.id.tvRewardSummary);
        rewardIcons      = v.findViewById(R.id.rewardIcons);
        imgRewardEquip   = v.findViewById(R.id.imgRewardEquip);

        sensorMgr = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        SharedPreferences sp = requireContext().getSharedPreferences("HabitGamePrefs", Context.MODE_PRIVATE);
        currentUserEmail = sp.getString("email", null);

        try { mpHit       = MediaPlayer.create(getContext(), R.raw.boss_hit_sound); } catch (Exception ignore) {}
        try { mpWin       = MediaPlayer.create(getContext(), R.raw.boss_win); } catch (Exception ignore) {}
        try { mpLoss      = MediaPlayer.create(getContext(), R.raw.boss_loss); } catch (Exception ignore) {}
        try { mpChestOpen = MediaPlayer.create(getContext(), R.raw.chest_open_sound); } catch (Exception ignore) {}
        try { mpConfetti = MediaPlayer.create(getContext(), R.raw.confetti); } catch (Exception ignore) {}

        loadAccountAndInit();

        btnAttack.setOnClickListener(x -> doAttack());

        if (lottieChestAnim != null) {
            lottieChestAnim.setOnClickListener(v1 -> {
                if (battleOver && !chestOpened) openChestAndShowRewards();
            });
        }
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

                long etapaStart = acc.getLastLevelUpTimestamp();

                final StageMetricsService sms = new StageMetricsService();
                StageMetricsService.Callback cb = new StageMetricsService.Callback() {
                    @Override public void onReady(int successRate) {
                        hitChancePercent = Math.max(0, Math.min(100, successRate));
                        maybePromptEquipmentThenBindUI();
                    }
                    @Override public void onError(Exception e) {
                        hitChancePercent = 67;
                        maybePromptEquipmentThenBindUI();
                    }
                };

                if (etapaStart > 0) {
                    sms.computeStageSuccessSince(etapaStart, null, cb);
                } else {
                    sms.computeCurrentStageSuccess(null, cb);
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

        final String[] names = new String[eq.size()];
        final boolean[] checked = new boolean[eq.size()];
        for (int i = 0; i < eq.size(); i++) {
            Equipment e = eq.get(i);
            names[i] = safeName(e);
            checked[i] = e != null && e.isActivated();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Izaberi opremu za borbu")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
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
                if (e != null && e.isActivated()) bonus += computeBonusFor(e, basePP);
            }
        }
        userPP = Math.max(0, basePP + bonus);
    }

    private int computeBonusFor(@NonNull Equipment e, int base) {
        String eff = e.getEffect() == null ? "" : e.getEffect().trim().toLowerCase(Locale.ROOT);
        double val = e.getEffectPercentage();
        if ("pp_flat".equals(eff))    return (int)Math.round(val);
        if ("pp_percent".equals(eff)) return (int)Math.round(base * (val / 100.0));
        return 0;
    }

    private void bindUi(Account acc) {
        tvBossHp.setText(String.format(Locale.getDefault(),"Boss HP: %d/%d", bossHp, bossMaxHp));
        pbBoss.setMax(bossMaxHp);
        pbBoss.setProgress(bossHp);

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

        tvChance.setText("Šansa: " + hitChancePercent + "%");
        tvAttempts.setText("Pokušaji: " + attemptsLeft + "/5");
    }
    private void doAttack() {
        if (attemptsLeft <= 0 || bossHp <= 0 || battleOver) return;

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
            lottieBossHit.setProgress(0f);
        }
    }

    private void playHit() {
        if (mpHit != null) try { mpHit.start(); } catch (Exception ignore) {}
        if (lottieBossIdle != null) lottieBossIdle.pauseAnimation();

        if (lottieBossHit != null) {
            lottieBossHit.removeAllAnimatorListeners();
            lottieBossHit.setVisibility(View.VISIBLE);
            lottieBossHit.setRepeatCount(0);
            lottieBossHit.setProgress(0f);
            lottieBossHit.addAnimatorListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    if (lottieBossHit != null) lottieBossHit.post(() -> startIdle());
                }
            });
            lottieBossHit.playAnimation();
        }
    }

    private void finishBattle() {
        btnAttack.setEnabled(false);
        battleOver = true;

        boolean defeated = (bossHp <= 0);

        try {
            if (defeated) {
                if (mpWin != null) mpWin.start();
            } else {
                if (mpLoss != null) mpLoss.start();
            }
        } catch (Exception ignore) {}

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

        boolean hasAnyReward = (coins > 0) || dropsEquipment;
        rewardCoins = Math.max(0, coins);
        rewardEquipType = dropsEquipment ? ((rnd.nextInt(100) < 95) ? "odeca" : "oruzje") : null;

        if (!hasAnyReward) {
            chestOpened = true;
            shakeEnabled = false;
            tvResult.setText(defeated ? "Bos je poražen, ali bez nagrade." : "Bos je pobegao! Bez nagrade.");
            tvRewardSummary.setText("Bez nagrade.");
            if (lottieChestAnim != null) lottieChestAnim.setVisibility(View.GONE);
            if (lottieConfetti  != null) lottieConfetti.setVisibility(View.GONE);
            if (rewardIcons     != null) rewardIcons.setVisibility(View.GONE);
            if (imgRewardEquip  != null) imgRewardEquip.setVisibility(View.GONE);
            AccountRepository.setPendingBossForEmail(currentUserEmail, false);
            return;
        }

        chestOpened = false;
        shakeEnabled = true;
        if (lottieChestAnim != null) {
            lottieChestAnim.setVisibility(View.VISIBLE);
            lottieChestAnim.setRepeatCount(0);
            lottieChestAnim.setProgress(0f);
        }

        tvResult.setText(defeated
                ? "Pobeda! Protresi ili tapni kovčeg da preuzmeš nagradu."
                : "Borba završena. Protresi ili tapni kovčeg da preuzmeš utešnu nagradu.");
    }



    private void openChestAndShowRewards() {
        if (!battleOver || chestOpened) return;
        chestOpened = true;

        // Zvukovi pri otvaranju
        try { if (mpChestOpen != null) mpChestOpen.start(); } catch (Exception ignore) {}
        try { if (mpConfetti  != null) mpConfetti.start();  } catch (Exception ignore) {}

        // Animacije
        if (lottieChestAnim != null) {
            lottieChestAnim.setVisibility(View.VISIBLE);
            lottieChestAnim.setRepeatCount(0);
            lottieChestAnim.setProgress(0f);
            lottieChestAnim.playAnimation();
        }
        if (lottieConfetti != null) {
            lottieConfetti.setVisibility(View.VISIBLE);
            lottieConfetti.setRepeatCount(0);
            lottieConfetti.setProgress(0f);
            lottieConfetti.playAnimation();
        }

        new AccountService().getAccountByEmail(currentUserEmail, new AccountCallback() {
            @Override public void onResult(Account acc) {
                if (acc != null) {
                    acc.setCoins(Math.max(0, acc.getCoins()) + Math.max(0, rewardCoins));
                    if (rewardEquipType != null) {
                        List<Equipment> list = acc.getEquipments();
                        if (list == null) list = new ArrayList<>();
                        Equipment e = new Equipment();
                        e.setName(rewardEquipType.equals("oruzje") ? "Nagrada: Oružje" : "Nagrada: Odeća");
                        e.setType(rewardEquipType);
                        e.setActivated(false);
                        e.setPrice(0);
                        list.add(e);
                        acc.setEquipments(list);
                    }
                    new AccountService().update(acc);
                }
                StringBuilder sb = new StringBuilder("Nagrada: +" + rewardCoins + " novčića");
                if (rewardEquipType != null) sb.append(" + oprema (").append(rewardEquipType).append(")");
                tvRewardSummary.setText(sb.toString());

                rewardIcons.setVisibility(View.VISIBLE);
                if (rewardEquipType != null) {
                    imgRewardEquip.setVisibility(View.VISIBLE);
                    imgRewardEquip.setImageResource(
                            "oruzje".equals(rewardEquipType)
                                    ? R.drawable.gloves
                                    : R.drawable.badge
                    );
                } else {
                    imgRewardEquip.setVisibility(View.GONE);
                }

                AccountRepository.setPendingBossForEmail(currentUserEmail, false);
            }
            @Override public void onFailure(Exception e) {
                toast("Greška nagrade: " + e.getMessage());
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
        try { if (lottieChestAnim != null) { lottieChestAnim.cancelAnimation(); } } catch (Exception ignore) {}
        try { if (lottieConfetti  != null) { lottieConfetti.cancelAnimation(); } } catch (Exception ignore) {}

        safeRelease(mpHit);        mpHit = null;
        safeRelease(mpWin);        mpWin = null;
        safeRelease(mpLoss);       mpLoss = null;
        safeRelease(mpChestOpen);  mpChestOpen = null;
        safeRelease(mpConfetti);   mpConfetti = null;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0], y = event.values[1], z = event.values[2];
        double mag = Math.sqrt(x*x + y*y + z*z);
        long now = System.currentTimeMillis();
        if (mag > 18 && (now - lastShakeTs) > 600) {
            lastShakeTs = now;

            if (!battleOver) {
                doAttack();
            } else if (!chestOpened && lottieChestAnim.getVisibility() == View.VISIBLE) {
                openChestAndShowRewards();
            }
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void toast(String s){ Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show(); }
    private void close(){ NavHostFragment.findNavController(this).popBackStack(); }
    private static String safeName(Equipment e){
        String n = (e.getName()==null || e.getName().trim().isEmpty()) ? "Oprema" : e.getName().trim();
        return n;
    }

    private void safeRelease(MediaPlayer mp) {
        try { if (mp != null) { mp.stop(); mp.release(); } } catch (Exception ignore) {}
    }
}
