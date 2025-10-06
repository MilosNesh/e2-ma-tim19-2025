package com.example.habitgame.model;

public interface AccountCallback {
    void onResult(Account account);
    void onFailure(Exception e);
}
