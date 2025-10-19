package com.example.habitgame.utils;

public final class LevelUtils {
    private LevelUtils(){}

    public static int xpThresholdForLevel(int level) {
        if (level <= 0) return 0;
        long prev = 200;
        if (level == 1) return (int) prev;
        for (int l = 2; l <= level; l++) {
            prev = prev * 2 + prev / 2;   // 2.5x
            if (prev > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) prev;
    }

    public static int levelFromXp(int totalXp) {
        int lvl = 0;
        while (true) {
            int next = xpThresholdForLevel(lvl + 1);
            if (next == Integer.MAX_VALUE || totalXp < next) break;
            lvl++;
        }
        return Math.max(0, lvl);
    }

    public static int bossHpForLevel(int level) {
        if (level <= 1) return 200;
        long hp = 200;
        for (int i = 2; i <= level; i++) {
            hp = hp * 2 + hp / 2;
            if (hp > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) hp;
    }

    public static int coinsForLevel(int level) {
        double base = 200.0;
        double val = base * Math.pow(1.2, Math.max(0, level - 1));
        return (int) Math.round(val);
    }

    public static int applyPpGrowth(int basePp, int times) {
        long val = Math.max(0, basePp);
        for (int i = 0; i < times; i++) {
            val = Math.round(val + 0.75 * val); // +75%
            if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) val;
    }

    public static String titleForLevel(int level) {
        switch (level) {
            case 0:  return "Početnik";
            case 1:  return "Junior Vitez";
            case 2:  return "Medior Vitez";
            case 3:  return "Senior Vitez";
            case 4:  return "Master Vitez";
            case 5:  return "Elitni Vitez";
            case 6:  return "Kralj Artur";
            default: return "Level " + level;
        }
    }
}
