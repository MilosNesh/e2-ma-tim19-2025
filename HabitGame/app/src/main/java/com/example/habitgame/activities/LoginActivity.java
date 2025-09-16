package com.example.habitgame.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habitgame.MainActivity;
import com.example.habitgame.databinding.ActivityLoginBinding;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.services.AccountService;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private AccountService accountService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        emailEditText = binding.email;
        passwordEditText = binding.password;
        loginButton = binding.loginButton;
        registerButton = binding.registerButton;
        accountService = new AccountService();
    }

    @Override
    protected void onResume(){
        super.onResume();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               accountService.login(emailEditText.getText().toString(), passwordEditText.getText().toString(), new StringCallback() {
                   @Override
                   public void onResult(String result) {
                       if (result.isEmpty()) {
                           SharedPreferences sharedPreferences = getSharedPreferences("HabitGamePrefs", MODE_PRIVATE);
                           SharedPreferences.Editor editor = sharedPreferences.edit();
                           editor.putString("email", emailEditText.getText().toString());
                           editor.apply();
                           Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                           startActivity(intent);
                       } else {
                           Toast.makeText(LoginActivity.this, result, Toast.LENGTH_SHORT).show();
                       }
                   }
               }, new AccountCallback() {
                   @Override
                   public void onResult(Account account) {
                       if(account != null){
                           SharedPreferences sharedPreferences = getSharedPreferences("HabitGamePrefs", MODE_PRIVATE);
                           SharedPreferences.Editor editor = sharedPreferences.edit();
                           editor.putString("username", account.getUsername());
                           editor.putInt("avatar", account.getAvatar());
                           editor.putInt("level", account.getLevel());
                           editor.apply();
                       }
                   }
               });
           }
       }
        );
    }

}