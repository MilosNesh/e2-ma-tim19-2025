package com.example.habitgame.utils;

public class XpCalculator {

    public static int calculateTotalXp(String weight, String importance) {
        int weightXp = getWeightXp(weight);
        int importanceXp = getImportanceXp(importance);

        return weightXp + importanceXp;
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