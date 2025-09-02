package com.example.habitgame.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habitgame.R;
import com.example.habitgame.adapters.AvatarAdapter;
import com.example.habitgame.databinding.ActivityRegistrationBinding;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.Equipment;
import com.example.habitgame.repositories.AccountRepository;
import com.example.habitgame.services.AccountService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RegistrationActivity extends AppCompatActivity {
    private ActivityRegistrationBinding binding;
    private EditText emailEditText, usernameEditText, passwordEditText, confirmPasswordEditText;
    private GridView avatarGridView;
    private ImageView selectedAvatarImageView;
    private Button registerButton;

    private Button loginButton;
    private Integer[] avatars;
    private int selectedAvatar = -1; // Default: no avatar selected

    private AccountService accountService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        emailEditText = binding.email;
        usernameEditText = binding.username;
        passwordEditText = binding.password;
        confirmPasswordEditText = binding.confirmPassword;
        avatarGridView = binding.avatarGrid;
        registerButton = binding.registerButton;
        loginButton = binding.loginButton;

        this.avatars = new Integer[] {
                R.drawable.avatar1, R.drawable.avatar2, R.drawable.avatar3,
                R.drawable.avatar4, R.drawable.avatar5
        };

        AvatarAdapter avatarAdapter = new AvatarAdapter(this, avatars);
        avatarGridView.setAdapter(avatarAdapter);
        selectedAvatarImageView = binding.selectedAvatarImage;
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        accountService = new AccountService();
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
                Account account = new Account(usernameEditText.getText().toString().trim(), emailEditText.getText().toString().trim(), passwordEditText.getText().toString().trim(), selectedAvatar);
                String registration = accountService.register(account, confirmPasswordEditText.getText().toString().trim(), selectedAvatar);
                Toast.makeText(RegistrationActivity.this, registration, Toast.LENGTH_SHORT).show();

                if (registration.equals("Uspjesna registracija")) {
                    Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void addEquipments(){
        List<Equipment> equipmentList = new ArrayList<>();

        // Dodaj napitke
        equipmentList.add(new Equipment(
                "Napitak za jednokratnu snagu 20%",
                "napitak",
                "Povecava snagu za 20%",
                1.2,
                1,
                false,
                "potion1",
                0
        ));

        equipmentList.add(new Equipment(
                "Napitak za jednokratnu snagu 40%",
                "napitak",
                "Povećava snagu za 40%",
                1.4,
                1,
                false,
                "potion2",
                0
        ));

        equipmentList.add(new Equipment(
                "Napitak za trajno povećanje snage 5%",
                "napitak",
                "Povećava snagu za 5% trajno",
                1.05,
                -1,
                false,
                "potion3",
                0
        ));

        equipmentList.add(new Equipment(
                "Napitak za trajno povećanje snage 10%",
                "napitak",
                "Povećava snagu za 10% trajno",
                1.1,
                -1,
                false,
                "potion4",
                0
        ));

        // Dodaj odeću
        equipmentList.add(new Equipment(
                "Rukavice",
                "odeca",
                "Povećava snagu za 10%",
                1.1,
                2,
                false,
                "gloves",
                0
        ));

        equipmentList.add(new Equipment(
                "Stit",
                "odeca",
                "Povećava šansu za napad za 10%",
                1.1,
                2,
                false,
                "shield",
                0
        ));

        equipmentList.add(new Equipment(
                "Cizme",
                "odeca",
                "Povećava broj napada za 40%",
                1.4,
                2,
                false,
                "boots",
                0
        ));

        // Dodaj oružje
        equipmentList.add(new Equipment(
                "Mac",
                "oruzje",
                "Povećava snagu za 5% trajno",
                1.05,
                -1,
                false,
                "sword",
                0
        ));

        equipmentList.add(new Equipment(
                "Luk i strela",
                "oruzje",
                "Povećava procenat novca od nagrade za 5%",
                1.05,
                -1,
                false,
                "bow",
                0
        ));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for(Equipment e: equipmentList){
            db.collection("equipments")
                    .add(e)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d("REZ_DB", "DocumentSnapshot added with ID: " + documentReference.getId());
                        }
                    });
        }

    }
}
