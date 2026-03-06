package com.example.realproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private FirebaseAuth mAuth;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnRegister = view.findViewById(R.id.btnRegister);

        // ==========================================================
        // 1. SECURE LOGIN LOGIC (With Resend Verification Feature)
        // ==========================================================
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            if (user.isEmailVerified()) {
                                // SUCCESSFUL LOGIN
                                Toast.makeText(getContext(), "Login Successful!", Toast.LENGTH_SHORT).show();
                                NavOptions navOptions = new NavOptions.Builder()
                                        .setPopUpTo(R.id.loginFragment, true)
                                        .build();
                                Navigation.findNavController(view).navigate(R.id.homeFragment, null, navOptions);
                            } else {
                                // EMAIL NOT VERIFIED: Give them a way to fix it!
                                new android.app.AlertDialog.Builder(getContext())
                                        .setTitle("Verification Required")
                                        .setMessage("You need to verify your email before logging in. Check your inbox")
                                        .setPositiveButton("Resend Email", (dialog, which) -> {
                                            user.sendEmailVerification()
                                                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "New link sent! Please check your email.", Toast.LENGTH_LONG).show())
                                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to send link: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                            mAuth.signOut(); // Kick them out safely
                                        })
                                        .setNegativeButton("Cancel", (dialog, which) -> mAuth.signOut())
                                        .setCancelable(false)
                                        .show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // ==========================================================
        // 2. SECURE REGISTRATION LOGIC
        // ==========================================================
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // SEND THE EMAIL
                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "Account Created! Please check your email to verify.", Toast.LENGTH_LONG).show();
                                    // Log them out immediately so they have to verify to get back in
                                    mAuth.signOut();
                                } else {
                                    Toast.makeText(getContext(), "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Registration Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });


        // SKIP BUTTON LOGIC (Guest Mode)
        TextView tvSkip = view.findViewById(R.id.tvSkip);

        tvSkip.setOnClickListener(v -> {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build();
            Navigation.findNavController(view).navigate(R.id.homeFragment, null, navOptions);
            Toast.makeText(getContext(), "Welcome Guest!", Toast.LENGTH_SHORT).show();
        });
    }
}