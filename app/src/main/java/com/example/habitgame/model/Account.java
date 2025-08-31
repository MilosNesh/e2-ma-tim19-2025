package com.example.habitgame.model;

import java.util.ArrayList;
import java.util.List;

public class Account {
    private String username;
    private String email;
    private int avatar;
    private String password;
    private int level;
    private String title;
    private int powerPoints;
    private int experiencePoints;
    private int coins;
    private int badgeNumbers;
    private List<Equipment> equipments;
    public Account(){
    }

    public Account(String username, String email, String password, int avatar) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.avatar = avatar;
        this.level = 1;
        this.powerPoints = 0;
        this.experiencePoints = 0;
        this.coins = 0;
        this.badgeNumbers = 0;
        this.title = "Baby Knight";
        this.equipments = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAvatar() {
        return avatar;
    }

    public void setAvatar(int avatar) {
        this.avatar = avatar;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPowerPoints() {
        return powerPoints;
    }

    public void setPowerPoints(int powerPoints) {
        this.powerPoints = powerPoints;
    }

    public int getExperiencePoints() {
        return experiencePoints;
    }

    public void setExperiencePoints(int experiencePoints) {
        this.experiencePoints = experiencePoints;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getBadgeNumbers() {
        return badgeNumbers;
    }

    public void setBadgeNumbers(int badgeNumbers) {
        this.badgeNumbers = badgeNumbers;
    }

    public List<Equipment> getEquipments() {
        return equipments;
    }

    public void setEquipments(List<Equipment> equipments) {
        this.equipments = equipments;
    }
}
