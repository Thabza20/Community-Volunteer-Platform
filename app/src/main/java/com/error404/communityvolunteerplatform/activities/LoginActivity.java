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

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private TextView tvLoginHeading, tvErrorMessage, tvGoToSignup;
    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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

        // Placeholder for authentication logic
        String userType = tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).getText().toString();
        Toast.makeText(this, "Logging in as " + userType, Toast.LENGTH_SHORT).show();
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