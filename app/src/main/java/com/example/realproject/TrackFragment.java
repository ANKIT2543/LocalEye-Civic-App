package com.example.realproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TrackFragment extends Fragment {

    public TrackFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_track, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // =================================================================
        // 1. THE BOUNCER CHECK
        // =================================================================
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // If user is NOT logged in (Guest), send them away immediately
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please Login to view profile", Toast.LENGTH_LONG).show();

            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.trackFragment, true)
                    .build();

            Navigation.findNavController(view).navigate(R.id.loginFragment, null, navOptions);
            return;
        }

        // =================================================================
        // 2. SETUP PROFILE DATA
        // =================================================================
        TextView tvUserName = view.findViewById(R.id.tvUserName);
        if (currentUser.getEmail() != null) {
            tvUserName.setText(currentUser.getEmail());
        }

        // =================================================================
        // 3. MENU BUTTONS
        // =================================================================
        view.findViewById(R.id.btnHistory).setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.historyFragment);
        });

        view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.settingsFragment);
        });

        // =================================================================
        // 4. SECURE LOGOUT LOGIC
        // =================================================================
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();

            // Navigate to Login AND completely wipe the backstack history
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();

            Navigation.findNavController(view).navigate(R.id.loginFragment, null, navOptions);
        });
    }
}