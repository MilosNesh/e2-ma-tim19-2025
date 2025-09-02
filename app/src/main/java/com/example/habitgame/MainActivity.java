package com.example.habitgame;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.android.material.navigation.NavigationView;

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

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            args.putString("email", "neskovic.milos02@gmail.com");
            navController.navigate(R.id.profileFragment, args);
        }

        // 2. Rukovanje klikovima u meniju (logout ručno)
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.logout) {
                drawer.closeDrawers();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return true;
            }
            else if (id == R.id.profileFragment) {
                Bundle args = new Bundle();
                args.putString("email", "neskovic.milos02@gmail.com");
                navController.navigate(R.id.profileFragment, args);
                drawer.closeDrawers();
                return true;
            }
            else if (id == R.id.changePasswordFragment) {
                Bundle args = new Bundle();
                args.putString("email", "neskovic.milos02@gmail.com");
                navController.navigate(R.id.changePasswordFragment, args);
                drawer.closeDrawers();
                return true;
            }

            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                drawer.closeDrawers();
            }
            return handled;
        });


//        navController.addOnDestinationChangedListener((navController, navDestination, bundle) -> {
//            int id = navDestination.getId();
//
//            if (id == R.id.profileFragment) {
//                getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.mainContainer, ProfileFragment.newInstance("neskovic.milos02@gmail.com"))
//                    .commit();
//
//            } else if (id == R.id.logout) {
//                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//                startActivity(intent);
//            }
//
//            drawer.closeDrawers();
//        });
    }
}