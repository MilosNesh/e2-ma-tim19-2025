package com.example.habitgame.utils;

public class XpCalculator {

    public static int calculateTotalXp(String weight, String importance, int currentLevel) {
        int base = getWeightXp(weight) + getImportanceXp(importance);
        double mult = Math.pow(1.5, Math.max(0, currentLevel - 1));
        return (int)Math.round(base * mult);
    }

    private static int getWeightXp(String weight) {
        if (weight == null) return 0;
        switch (weight.toLowerCase()) {
            case "veoma lak": return 1;
            case "lak": return 3;
            case "tezak": return 7;
            case "ekstremno tezak": return 20;
            default: return 0;
        }
    }

    private static int getImportanceXp(String importance) {
        if (importance == null) return 0;
        switch (importance.toLowerCase()) {
            case "normalan": return 1;
            case "vazan": return 3;
            case "ekstremno vazan": return 10;
            case "specijalan": return 100;
            default: return 0;
        }
    }
}