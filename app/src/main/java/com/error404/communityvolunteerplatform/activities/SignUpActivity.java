package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.error404.communityvolunteerplatform.R;
import com.google.android.material.tabs.TabLayout;

import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private LinearLayout layoutOrgForm, layoutVolForm;
    private CheckBox cbPopia;
    private TextView tvPopiaLink, tvErrorMessage, tvSignupHeading, tvGoToLogin;
    private Button btnSignup;

    // Organization Fields
    private EditText etOrgName, etOrgEmail, etOrgPassword, etOrgLocation, etOrgDetails, etOrgNumber, etOrgPrimaryPhone, etOrgSecondaryPhone;
    // Volunteer Fields
    private EditText etVolName, etVolSurname, etVolEmail, etVolPassword, etVolPhone, etVolBio, etVolSkills;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Views
        tabLayout = findViewById(R.id.tabLayout);
        layoutOrgForm = findViewById(R.id.layoutOrgForm);
        layoutVolForm = findViewById(R.id.layoutVolForm);
        cbPopia = findViewById(R.id.cbPopia);
        tvPopiaLink = findViewById(R.id.tvPopiaLink);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvSignupHeading = findViewById(R.id.tvSignupHeading);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        btnSignup = findViewById(R.id.btnSignup);

        // Org EditTexts
        etOrgName = findViewById(R.id.etOrgName);
        etOrgEmail = findViewById(R.id.etOrgEmail);
        etOrgPassword = findViewById(R.id.etOrgPassword);
        etOrgLocation = findViewById(R.id.etOrgLocation);
        etOrgDetails = findViewById(R.id.etOrgDetails);
        etOrgNumber = findViewById(R.id.etOrgNumber);
        etOrgPrimaryPhone = findViewById(R.id.etOrgPrimaryPhone);
        etOrgSecondaryPhone = findViewById(R.id.etOrgSecondaryPhone);

        // Vol EditTexts
        etVolName = findViewById(R.id.etVolName);
        etVolSurname = findViewById(R.id.etVolSurname);
        etVolEmail = findViewById(R.id.etVolEmail);
        etVolPassword = findViewById(R.id.etVolPassword);
        etVolPhone = findViewById(R.id.etVolPhone);
        etVolBio = findViewById(R.id.etVolBio);
        etVolSkills = findViewById(R.id.etVolSkills);

        // Setup Tab switching
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tvErrorMessage.setVisibility(View.GONE); // Hide error on switch
                if (tab.getPosition() == 0) {
                    // Organization Tab
                    layoutOrgForm.setVisibility(View.VISIBLE);
                    layoutVolForm.setVisibility(View.GONE);
                    tvSignupHeading.setText("Organization Signup");
                } else {
                    // Volunteer Tab
                    layoutOrgForm.setVisibility(View.GONE);
                    layoutVolForm.setVisibility(View.VISIBLE);
                    tvSignupHeading.setText("Volunteer Signup");
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Set initial state explicitly
        layoutOrgForm.setVisibility(View.VISIBLE);
        layoutVolForm.setVisibility(View.GONE);
        tvSignupHeading.setText("Organization Signup");

        // Setup POPIA link popup
        setupPopiaLink();

        // Setup Signup button
        btnSignup.setOnClickListener(v -> {
            validateAndSignup();
        });

        // Redirect to Login
        tvGoToLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void validateAndSignup() {
        boolean isOrg = tabLayout.getSelectedTabPosition() == 0;
        tvErrorMessage.setVisibility(View.GONE);

        if (isOrg) {
            // Secondary phone is now optional
            if (isEmpty(etOrgName) || isEmpty(etOrgEmail) || isEmpty(etOrgPassword) || isEmpty(etOrgLocation) || 
                isEmpty(etOrgDetails) || isEmpty(etOrgNumber) || isEmpty(etOrgPrimaryPhone)) {
                showError("Please fill in all fields");
                return;
            }
            // Validate Password
            if (!isValidPassword(etOrgPassword.getText().toString())) {
                showError("Password does not meet requirements");
                return;
            }
            // Validate Primary Phone
            if (!isValidSAPhone(etOrgPrimaryPhone.getText().toString())) {
                showError("invalid phone number");
                return;
            }
            // Validate Secondary Phone ONLY if it is not empty
            String secPhone = etOrgSecondaryPhone.getText().toString().trim();
            if (!secPhone.isEmpty() && !isValidSAPhone(secPhone)) {
                showError("invalid phone number");
                return;
            }
        } else {
            if (isEmpty(etVolName) || isEmpty(etVolSurname) || isEmpty(etVolEmail) || isEmpty(etVolPassword) || 
                isEmpty(etVolPhone) || isEmpty(etVolBio) || isEmpty(etVolSkills)) {
                showError("Please fill in all fields");
                return;
            }
            // Validate Password
            if (!isValidPassword(etVolPassword.getText().toString())) {
                showError("Password does not meet requirements");
                return;
            }
            if (!isValidSAPhone(etVolPhone.getText().toString())) {
                showError("invalid phone number");
                return;
            }
        }

        if (!cbPopia.isChecked()) {
            showError("Please consent to POPIA act");
            return;
        }

        String userType = isOrg ? "Organization" : "Volunteer";
        Toast.makeText(this, "Signup successful as " + userType, Toast.LENGTH_LONG).show();
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private boolean isEmpty(EditText et) {
        return et.getText().toString().trim().isEmpty();
    }

    private boolean isValidPassword(String password) {
        // Minimum 8 characters, at least one number, at least one uppercase, at least one special character
        String regex = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";
        return Pattern.compile(regex).matcher(password).matches();
    }

    private boolean isValidSAPhone(String phone) {
        // Regex for South African phone numbers:
        // Starts with +27 or 0, followed by 9 digits starting with 6, 7, or 8.
        String regex = "^(\\+27|0)[6-8][0-9]{8}$";
        return Pattern.compile(regex).matcher(phone).matches();
    }

    private void setupPopiaLink() {
        String text = "I consent according to the POPIA act";
        SpannableString ss = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showPopiaDialog();
            }
        };

        // Make "POPIA" clickable (Indices 25 to 30)
        ss.setSpan(clickableSpan, 25, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvPopiaLink.setText(ss);
        tvPopiaLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showPopiaDialog() {
        new AlertDialog.Builder(this)
                .setTitle("POPIA Details")
                .setMessage("to do add POPIA details")
                .setPositiveButton("OK", null)
                .show();
    }
}