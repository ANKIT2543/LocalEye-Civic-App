package com.example.realproject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPrefs;
    private FirebaseAuth mAuth;
    private static final String PREF_DARK_MODE = "dark_mode_enabled";
    private static final String PREF_NOTIFICATIONS = "notifications_enabled";

    public SettingsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPrefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // ==========================================================
        // STATUS & SYSTEM NAV BAR HACK: Runs every time screen loads
        // ==========================================================
        int nightModeFlags = getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // Dark Mode is ON: Force Dark background and WHITE icons for both top and bottom bars
            requireActivity().getWindow().setStatusBarColor(android.graphics.Color.parseColor("#121212"));
            requireActivity().getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#121212")); // NEW: Dark Nav Bar
            View decorView = requireActivity().getWindow().getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            // Light Mode is ON: Force White background and DARK icons for both top and bottom bars
            requireActivity().getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));
            requireActivity().getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#FFFFFF")); // NEW: White Nav Bar
            View decorView = requireActivity().getWindow().getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        // 1. Back Button
        view.findViewById(R.id.btnBackFromSettings).setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        // 2. Account Actions
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            if (currentUser == null) return;
            final EditText input = new EditText(getContext());
            input.setText(currentUser.getDisplayName());
            LinearLayout layout = new LinearLayout(getContext());
            layout.setPadding(50, 20, 50, 20);
            layout.addView(input);

            new AlertDialog.Builder(getContext())
                    .setTitle("Edit Profile Name")
                    .setView(layout)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            currentUser.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(newName).build())
                                    .addOnCompleteListener(task -> Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show());
                        }
                    }).setNegativeButton("Cancel", null).show();
        });

        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            if (currentUser != null && currentUser.getEmail() != null) {
                mAuth.sendPasswordResetEmail(currentUser.getEmail())
                        .addOnCompleteListener(task -> Toast.makeText(getContext(), "Password reset link sent to your email!", Toast.LENGTH_LONG).show());
            } else {
                Toast.makeText(getContext(), "Available for registered users only.", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Preferences
        view.findViewById(R.id.btnLanguage).setOnClickListener(v ->
                Toast.makeText(getContext(), "Language localization coming in V2", Toast.LENGTH_SHORT).show());

        Switch switchNotifications = view.findViewById(R.id.switchNotifications);
        switchNotifications.setChecked(sharedPrefs.getBoolean(PREF_NOTIFICATIONS, true));
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            sharedPrefs.edit().putBoolean(PREF_NOTIFICATIONS, isChecked).apply();
            Toast.makeText(getContext(), isChecked ? "Push Notifications Enabled" : "Push Notifications Disabled", Toast.LENGTH_SHORT).show();
        });

        // ==========================================================
        // 4. THE PROPER DARK MODE ENGINE (Simplified)
        // ==========================================================
        Switch switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchDarkMode.setChecked(sharedPrefs.getBoolean(PREF_DARK_MODE, false));

        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            // Save the preference
            sharedPrefs.edit().putBoolean(PREF_DARK_MODE, isChecked).apply();

            // Just trigger the theme change. The screen will restart and hit our Status Bar hack above!
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // 5. Data & Support
        view.findViewById(R.id.btnClearCache).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Clearing cache...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                Glide.get(requireContext()).clearDiskCache();
                requireActivity().runOnUiThread(() -> {
                    Glide.get(requireContext()).clearMemory();
                    Toast.makeText(getContext(), "App cache cleared successfully", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        // Real Web Intents
        view.findViewById(R.id.btnHelpCenter).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.google.com"));
            startActivity(browserIntent);
        });

        view.findViewById(R.id.btnPrivacy).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy"));
            startActivity(browserIntent);
        });

        // 6. Delete Account
        view.findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> {
            if (currentUser == null) return;
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Account")
                    .setMessage("Are you sure? This will permanently delete your account and data.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        currentUser.delete().addOnSuccessListener(aVoid -> {
                            NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build();
                            Navigation.findNavController(view).navigate(R.id.loginFragment, null, navOptions);
                        }).addOnFailureListener(e -> Toast.makeText(getContext(), "Please re-authenticate to delete.", Toast.LENGTH_LONG).show());
                    }).setNegativeButton("Cancel", null).show();
        });
    }
}