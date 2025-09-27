package com.example.habitgame;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.habitgame.activities.LoginActivity;
import com.example.habitgame.activities.RegistrationActivity;
import com.example.habitgame.databinding.ActivityMainBinding;
import com.example.habitgame.fragments.ProfileFragment;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.StringCallback;
import com.example.habitgame.services.AccountService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private NavController navController;
    private Toolbar toolbar;
    private ActionBar actionBar;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawer = binding.drawerLayout;
        navigationView = binding.navView;
        toolbar = binding.activityHomeBase.toolbar;

        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_hamburger);
            actionBar.setHomeButtonEnabled(true);
        }

        actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        navController = Navigation.findNavController(this, R.id.mainContainer);
        NavigationUI.setupWithNavController(navigationView, navController);

        SharedPreferences sharedPreferences = getSharedPreferences("HabitGamePrefs", MODE_PRIVATE);
        String email = sharedPreferences.getString("email", null);
        String username = sharedPreferences.getString("username", "");
        int avatar = sharedPreferences.getInt("avatar", R.drawable.avatar1);


        // Postavljanje nav header elementa
        View headerView = binding.navView.getHeaderView(0);
        ImageView userImage = headerView.findViewById(R.id.user_image);
        TextView userName = headerView.findViewById(R.id.user_name);

        userImage.setImageResource(avatar);
        userName.setText(username);

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            args.putString("email", email);
            navController.navigate(R.id.profileFragment, args);
            actionBar.setTitle(R.string.profile);
        }

        Intent intentQR = getIntent();
        if (intentQR.getData() != null) {
            String qrEmail = intentQR.getData().getQueryParameter("email");

            if (qrEmail != null && email != null) {
                AccountService accountService = new AccountService();
                accountService.getAccountByEmail(qrEmail, new AccountCallback() {
                    @Override
                    public void onResult(Account account) {
                        accountService.addFriend(account, email, new StringCallback() {
                            @Override
                            public void onResult(String result) {
                                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                                Bundle args = new Bundle();
                                args.putString("email", qrEmail);
                                navController.navigate(R.id.profileFragment, args);
                                actionBar.setTitle(R.string.friend);
                            }
                        });
                    }
                });
            }
        }


        getFMCToken(email);

        // Dobijamo informaciju iz Intent-a
        String navigateTo = getIntent().getStringExtra("navigateTo");

        // Ako je "navigateTo" setovano, navigiraj na odgovarajući fragment
        if ("allianceFragment".equals(navigateTo)) {
            NavController navController = Navigation.findNavController(this, R.id.mainContainer);
            navController.navigate(R.id.allianceFragment);
            actionBar.setTitle(R.string.alliance);
        } else if ("messagesFragment".equals(navigateTo)) {
            NavController navController = Navigation.findNavController(this, R.id.mainContainer);
            navController.navigate(R.id.messagesFragment);
            actionBar.setTitle(R.string.messages);
        }

        // 2. Rukovanje klikovima u meniju (logout ručno)
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.logout) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                drawer.closeDrawers();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            else if (id == R.id.profileFragment) {
                Bundle args = new Bundle();
                args.putString("email", email);
                navController.navigate(R.id.profileFragment, args);
                actionBar.setTitle(R.string.profile);
                drawer.closeDrawers();
                return true;
            }
            else if (id == R.id.changePasswordFragment) {
                Bundle args = new Bundle();
                args.putString("email", email);
                navController.navigate(R.id.changePasswordFragment, args);
                actionBar.setTitle(R.string.chage_password);
                drawer.closeDrawers();
                return true;
            }
            else if (id == R.id.profileListFragment) {
                navController.navigate(R.id.profileListFragment);
                actionBar.setTitle(R.string.friends);
                drawer.closeDrawers();
                return true;
            }
            else if (id == R.id.shopFragment) {
                navController.navigate(R.id.shopFragment);
                actionBar.setTitle(R.string.shop);
                drawer.closeDrawers();
                return true;
            }
            else if (id == R.id.allianceFragment) {
                navController.navigate(R.id.allianceFragment);
                actionBar.setTitle(R.string.alliance);
                drawer.closeDrawers();
                return true;
            }
            else if (id == R.id.messagesFragment) {
                navController.navigate(R.id.messagesFragment);
                actionBar.setTitle(R.string.messages);
                drawer.closeDrawers();
                return true;
            }

            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                drawer.closeDrawers();
            }
            return handled;
        });

//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
    }

    private void getFMCToken(String email) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
           if (task.isSuccessful()) {
               String token = task.getResult();
               Log.i("Moj token ", token);
               AccountService.updateFcmTokne(email, token);
           }
        });
    }
}