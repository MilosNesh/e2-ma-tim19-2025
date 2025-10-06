package com.example.habitgame.model;

public interface TaskCompletionCallback {
    void onSuccess(int xpEarned, String message);
    void onFailure(String errorMessage, Exception e);
}
