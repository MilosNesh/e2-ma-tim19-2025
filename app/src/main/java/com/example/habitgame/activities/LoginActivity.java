package com.example.habitgame.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habitgame.MainActivity;
import com.example.habitgame.databinding.ActivityLoginBinding;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.services.AccountService;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private AccountService accountService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        emailEditText = binding.email;
        passwordEditText = binding.password;
        loginButton = binding.loginButton;
        accountService = new AccountService();
    }

    @Override
    protected void onResume(){
        super.onResume();

        loginButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               accountService.login(emailEditText.getText().toString(), passwordEditText.getText().toString(), new StringCallback() {
                   @Override
                   public void onResult(String result) {
                       if (result.isEmpty()) {
                           Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                           startActivity(intent);
                       } else {
                           Toast.makeText(LoginActivity.this, result, Toast.LENGTH_SHORT).show();
                       }
                   }
               });
           }
       }
        );
    }

}