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
    private boolean isVerified;
    private long registrationTimestamp;
    private List<String> friends;
    private String fcmToken;
    private String allianceId;
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
        this.friends = new ArrayList<>();
        this.allianceId = "";
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

    public boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(boolean verified) {
        isVerified = verified;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public void setRegistrationTimestamp(long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public int countMaxXp() {
        int maxXp = 200;
        for(int i = 0; i < level; i++){
            maxXp = maxXp * 2 + maxXp / 2;
            maxXp= ((maxXp + 99) / 100) * 100;
        }
        return maxXp;
    }
    public int countPP() {
        int pp = 40;
        for(int i = 0; i<level; i++){
            pp = pp + 3/4*pp;
        }
        return pp;
    }
    public void newTitle(){
        switch (level){
            case 0: title = "Pocetnik"; break;
            case 1: title = "Junior Vitez"; break;
            case 2: title = "Medior Vitez"; break;
            case 3: title = "Senior Vitez"; break;
            case 4: title = "Master Vitez"; break;
            default: title = "Noob";
        }
    }
    public void addExperiencePoints(int xp) {
        experiencePoints += xp;
        if(experiencePoints >= countMaxXp()) {
            level+=1;
            newTitle();
        }
    }
}
