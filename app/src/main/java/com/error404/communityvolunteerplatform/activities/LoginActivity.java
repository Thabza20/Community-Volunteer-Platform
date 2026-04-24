package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.error404.communityvolunteerplatform.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private TextView tvLoginHeading, tvErrorMessage, tvGoToSignup;
    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        tabLayout = findViewById(R.id.tabLayout);
        tvLoginHeading = findViewById(R.id.tvLoginHeading);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvGoToSignup = findViewById(R.id.tvGoToSignup);
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Setup Tab switching
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tvErrorMessage.setVisibility(View.GONE);
                String userType = tab.getText().toString();
                tvLoginHeading.setText(userType + " Login");
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Set initial state
        tvLoginHeading.setText("Admin Login");

        // Login button listener
        btnLogin.setOnClickListener(v -> validateAndLogin());

        // Redirect to Signup
        tvGoToSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void validateAndLogin() {
        tvErrorMessage.setVisibility(View.GONE);
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (!isValidPassword(password)) {
            showError("Password does not meet requirements");
            return;
        }

        btnLogin.setEnabled(false);
        showError("Authenticating...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        checkUserRoleAndNavigate(uid);
                    } else {
                        btnLogin.setEnabled(true);
                        showError("Login failed: " + task.getException().getMessage());
                    }
                });
    }

    private void checkUserRoleAndNavigate(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        String selectedTab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).getText().toString().toLowerCase();

                        // Basic security: check if user is logging into the correct tab
                        if (role != null && role.equals(selectedTab)) {
                            if (role.equals("volunteer")) {
                                startActivity(new Intent(LoginActivity.this, VolunteerDashboardActivity.class));
                                finish();
                            } else if (role.equals("organisation")) {
                                // TODO: Create OrganizationDashboardActivity
                                Toast.makeText(this, "Organization Dashboard coming soon", Toast.LENGTH_SHORT).show();
                                btnLogin.setEnabled(true);
                            } else if (role.equals("admin")) {
                                // TODO: Create AdminDashboardActivity
                                Toast.makeText(this, "Admin Dashboard coming soon", Toast.LENGTH_SHORT).show();
                                btnLogin.setEnabled(true);
                            }
                        } else {
                            mAuth.signOut();
                            btnLogin.setEnabled(true);
                            showError("Access Denied: You are not registered as a " + selectedTab);
                        }
                    } else {
                        mAuth.signOut();
                        btnLogin.setEnabled(true);
                        showError("User data not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    showError("Error checking role: " + e.getMessage());
                });
    }

    private boolean isValidPassword(String password) {
        // Minimum 8 characters, at least one number, at least one uppercase, at least one special character
        String regex = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";
        return Pattern.compile(regex).matcher(password).matches();
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }
}