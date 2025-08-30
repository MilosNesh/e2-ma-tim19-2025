package com.example.habitgame.model;

public class Account {
    private String username;
    private String email;
    private int avatar;
    private String password;
    public Account(){
    }

    public Account(String username, String email, String password, int avatar) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.avatar = avatar;
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
}
