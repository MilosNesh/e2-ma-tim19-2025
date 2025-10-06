package com.example.habitgame.model;

public interface TaskCallback {
    void onResult(Task task);
    void onFailure(Exception e);
}
