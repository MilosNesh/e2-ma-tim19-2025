package com.example.habitgame.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.habitgame.MainActivity;
import com.example.habitgame.R;
import com.example.habitgame.databinding.ActivitySplashScreenBinding;

import java.util.Timer;
import java.util.TimerTask;

public class SplashScreenActivity extends AppCompatActivity {
    private ActivitySplashScreenBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int SPLASH_TIME_OUT = 1000;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences = getSharedPreferences("HabitGamePrefs", MODE_PRIVATE);
                String email = sharedPreferences.getString("email", null);

                if(email == null) {
                    Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    startActivity(intent);
                }

                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}