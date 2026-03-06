package com.example.realproject;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;

import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// --- CLOUDINARY IMPORTS ---
import com.cloudinary.android.MediaManager;

// --- FIREBASE IMPORT ---
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        boolean isDarkMode = getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean("dark_mode_enabled", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);

        // =================================================================
        // PART 0: CLOUDINARY SETUP
        // =================================================================
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dc3iwnbpv"); // <--- CLOUD NAME HERE
            MediaManager.init(this, config);
        } catch (Exception e) {
            // prevents a crash if the app tries to initialize it twice
        }

        // =================================================================
        // PART 1: SYSTEM BAR FIX
        // =================================================================
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#E3F2FD"));
        window.setNavigationBarColor(Color.WHITE);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        // =================================================================
        // PART 2: NAVIGATION LOGIC (BOTTOM MENU)
        // =================================================================
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);

            // --- NEW: PERSISTENT LOGIN CHECK ---
            // Grab the navigation graph
            if (savedInstanceState == null) {
                NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    navGraph.setStartDestination(R.id.homeFragment);
                } else {
                    navGraph.setStartDestination(R.id.loginFragment);
                }
                navController.setGraph(navGraph);
            }
            // -----------------------------------

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.loginFragment) {
                    bottomNav.setVisibility(View.GONE);
                } else {
                    bottomNav.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}