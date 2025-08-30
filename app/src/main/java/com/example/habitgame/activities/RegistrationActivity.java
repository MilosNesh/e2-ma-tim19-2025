package com.example.habitgame.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.AdapterView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habitgame.R;
import com.example.habitgame.adapters.AvatarAdapter;
import com.example.habitgame.model.Account;
import com.example.habitgame.repositories.AccountRepository;
import com.google.firebase.FirebaseApp;

public class RegistrationActivity extends AppCompatActivity {

    private EditText emailEditText, usernameEditText, passwordEditText, confirmPasswordEditText;
    private GridView avatarGridView;
    private ImageView selectedAvatarImageView;
    private Button registerButton;

    private Button loginButton;
    private Integer[] avatars;
    private int selectedAvatar = -1; // Default: no avatar selected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        emailEditText = findViewById(R.id.email);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirm_password);
        avatarGridView = findViewById(R.id.avatar_grid);
        registerButton = findViewById(R.id.register_button);
        loginButton = findViewById(R.id.login_button);

        this.avatars = new Integer[] {
                R.drawable.avatar1, R.drawable.avatar2, R.drawable.avatar3,
                R.drawable.avatar4, R.drawable.avatar5
        };

        AvatarAdapter avatarAdapter = new AvatarAdapter(this, avatars);
        avatarGridView.setAdapter(avatarAdapter);
        selectedAvatarImageView = findViewById(R.id.selected_avatar_image);
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        avatarGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    selectedAvatar = position;
                    selectedAvatarImageView.setImageResource(avatars[position]);
                } catch (Exception e) {
                    e.printStackTrace(); // Ispis greške u logcat
                    Toast.makeText(RegistrationActivity.this, "Error selecting avatar", Toast.LENGTH_SHORT).show();
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegistration();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccountRepository.select();
            }
        });
    }
    private void handleRegistration() {
        String email = emailEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Sva polja su obavezna!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Lozinke se ne podudaraju!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAvatar == -1) {
            Toast.makeText(this, "Izaberite avatara!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Proceed with registration (save to database, etc.)
        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

        Account account = new Account(username, email, password, selectedAvatar);

        AccountRepository.insert(account);
    }
}
