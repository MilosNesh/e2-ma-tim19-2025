package com.example.habitgame.events;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public final class TaskEvents {
    private TaskEvents() {}

    // Svaki put kad se nešto promeni na zadacima (status, edit, delete), pošaljemo timestamp.
    private static final MutableLiveData<Long> REFRESH = new MutableLiveData<>();

    public static LiveData<Long> refresh() { return REFRESH; }

    public static void signalRefresh() {
        // postValue zbog poziva i sa background thread-a.
        REFRESH.postValue(System.currentTimeMillis());
    }
}
