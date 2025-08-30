package com.example.habitgame.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.habitgame.MainActivity;
import com.example.habitgame.R;
import com.example.habitgame.databinding.ActivityLoginBinding;
import com.example.habitgame.repositories.AccountRepository;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        emailEditText = binding.email;
        passwordEditText = binding.password;
        loginButton = binding.loginButton;

    }

    @Override
    protected void onResume(){
        super.onResume();

        loginButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
                handleLogin();
           }
       }
        );
    }

    private  void handleLogin(){
        AccountRepository accountRepository = new AccountRepository();
        accountRepository.selectByEmail(String.valueOf(emailEditText.getText()))
                .addOnSuccessListener(account -> {
                    if (account != null && account.getPassword().equals(String.valueOf(passwordEditText.getText()))) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(LoginActivity.this, "Prijava nije uspjela. Pogresan email ili lozinka", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Error", "Neuspješno dohvaćanje accounta", e);
                });
    }
}