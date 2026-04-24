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
import com.error404.communityvolunteerplatform.models.Organisation;
import com.error404.communityvolunteerplatform.models.User;
import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private LinearLayout layoutOrgForm, layoutVolForm;
    private CheckBox cbPopia;
    private TextView tvErrorMessage, tvSignupHeading;
    private Button btnSignup;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Organization Fields
    private EditText etOrgName, etOrgEmail, etOrgPassword, etOrgLocation, etOrgDetails, etOrgNumber, etOrgPrimaryPhone, etOrgSecondaryPhone, etOrgOtp;
    // Volunteer Fields
    private EditText etVolName, etVolSurname, etVolEmail, etVolPassword, etVolPhone, etVolBio, etVolSkills, etVolOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        tabLayout = findViewById(R.id.tabLayout);
        layoutOrgForm = findViewById(R.id.layoutOrgForm);
        layoutVolForm = findViewById(R.id.layoutVolForm);
        cbPopia = findViewById(R.id.cbPopia);
        TextView tvPopiaLink = findViewById(R.id.tvPopiaLink);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvSignupHeading = findViewById(R.id.tvSignupHeading);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);
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
        etOrgOtp = findViewById(R.id.etOrgOtp);
        Button btnOrgRequestOtp = findViewById(R.id.btnOrgRequestOtp);

        // Vol EditTexts
        etVolName = findViewById(R.id.etVolName);
        etVolSurname = findViewById(R.id.etVolSurname);
        etVolEmail = findViewById(R.id.etVolEmail);
        etVolPassword = findViewById(R.id.etVolPassword);
        etVolPhone = findViewById(R.id.etVolPhone);
        etVolBio = findViewById(R.id.etVolBio);
        etVolSkills = findViewById(R.id.etVolSkills);
        etVolOtp = findViewById(R.id.etVolOtp);
        Button btnVolRequestOtp = findViewById(R.id.btnVolRequestOtp);

        // Setup Tab switching
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tvErrorMessage.setVisibility(View.GONE); // Hide error on switch
                if (tab.getPosition() == 0) {
                    // Organization Tab
                    layoutOrgForm.setVisibility(View.VISIBLE);
                    layoutVolForm.setVisibility(View.GONE);
                    tvSignupHeading.setText(R.string.organization_signup);
                } else {
                    // Volunteer Tab
                    layoutOrgForm.setVisibility(View.GONE);
                    layoutVolForm.setVisibility(View.VISIBLE);
                    tvSignupHeading.setText(R.string.volunteer_signup);
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
        tvSignupHeading.setText(R.string.organization_signup);

        // Setup POPIA link popup
        setupPopiaLink(tvPopiaLink);

        // Setup Signup button
        btnSignup.setOnClickListener(v -> validateAndSignup());

        // Setup Request OTP buttons
        View.OnClickListener requestOtpListener = v -> Toast.makeText(this, R.string.otp_requested, Toast.LENGTH_SHORT).show();
        btnOrgRequestOtp.setOnClickListener(requestOtpListener);
        btnVolRequestOtp.setOnClickListener(requestOtpListener);

        // Redirect to Login
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void validateAndSignup() {
        boolean isOrg = tabLayout.getSelectedTabPosition() == 0;
        tvErrorMessage.setVisibility(View.GONE);

        if (isOrg) {
            // Secondary phone is now optional
            if (isEmpty(etOrgName) || isEmpty(etOrgEmail) || isEmpty(etOrgPassword) || isEmpty(etOrgLocation) || 
                isEmpty(etOrgDetails) || isEmpty(etOrgNumber) || isEmpty(etOrgPrimaryPhone) || isEmpty(etOrgOtp)) {
                showError(getString(R.string.fill_all_fields));
                return;
            }
            // Validate Password
            if (!isValidPassword(etOrgPassword.getText().toString())) {
                showError(getString(R.string.password_requirements));
                return;
            }
            // Validate Primary Phone
            if (!isValidSAPhone(etOrgPrimaryPhone.getText().toString())) {
                showError(getString(R.string.invalid_phone));
                return;
            }
            // Validate Secondary Phone ONLY if it is not empty
            String secPhone = etOrgSecondaryPhone.getText().toString().trim();
            if (!secPhone.isEmpty() && !isValidSAPhone(secPhone)) {
                showError(getString(R.string.invalid_phone));
                return;
            }
        } else {
            if (isEmpty(etVolName) || isEmpty(etVolSurname) || isEmpty(etVolEmail) || isEmpty(etVolPassword) || 
                isEmpty(etVolPhone) || isEmpty(etVolBio) || isEmpty(etVolSkills) || isEmpty(etVolOtp)) {
                showError(getString(R.string.fill_all_fields));
                return;
            }
            // Validate Password
            if (!isValidPassword(etVolPassword.getText().toString())) {
                showError(getString(R.string.password_requirements));
                return;
            }
            if (!isValidSAPhone(etVolPhone.getText().toString())) {
                showError(getString(R.string.invalid_phone));
                return;
            }
        }

        if (!cbPopia.isChecked()) {
            showError(getString(R.string.consent_popia));
            return;
        }

        String email = isOrg ? etOrgEmail.getText().toString().trim() : etVolEmail.getText().toString().trim();
        String password = isOrg ? etOrgPassword.getText().toString().trim() : etVolPassword.getText().toString().trim();

        btnSignup.setEnabled(false);
        showError(getString(R.string.creating_account));

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserDataToFirestore(uid, isOrg);
                    } else {
                        btnSignup.setEnabled(true);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        showError(getString(R.string.signup_failed, errorMsg));
                    }
                });
    }

    private void saveUserDataToFirestore(String uid, boolean isOrg) {
        String email = isOrg ? etOrgEmail.getText().toString().trim() : etVolEmail.getText().toString().trim();
        String role = isOrg ? "organisation" : "volunteer";

        User user = new User(uid, role, email);
        user.setPopiaAccepted(true);

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    if (isOrg) {
                        saveOrganisationProfile(uid);
                    } else {
                        saveVolunteerProfile(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    btnSignup.setEnabled(true);
                    showError(getString(R.string.error_saving_user, e.getMessage()));
                });
    }

    private void saveOrganisationProfile(String uid) {
        Organisation org = new Organisation(
                uid,
                etOrgName.getText().toString().trim(),
                etOrgEmail.getText().toString().trim(),
                etOrgLocation.getText().toString().trim(),
                etOrgPrimaryPhone.getText().toString().trim(),
                etOrgNumber.getText().toString().trim()
        );
        org.setOrgDetails(etOrgDetails.getText().toString().trim());
        String secPhone = etOrgSecondaryPhone.getText().toString().trim();
        if (!secPhone.isEmpty()) org.setSecondaryPhoneNumber(secPhone);

        db.collection("organisations").document(uid).set(org)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.org_reg_success, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSignup.setEnabled(true);
                    showError(getString(R.string.error_saving_profile, e.getMessage()));
                });
    }

    private void saveVolunteerProfile(String uid) {
        Volunteer vol = new Volunteer(
                uid,
                etVolName.getText().toString().trim(),
                etVolSurname.getText().toString().trim(),
                etVolEmail.getText().toString().trim(),
                ""
        );
        vol.setPhoneNumber(etVolPhone.getText().toString().trim());
        vol.setBio(etVolBio.getText().toString().trim());

        String skillsStr = etVolSkills.getText().toString().trim();
        if (!skillsStr.isEmpty()) {
            List<String> skillsList = new ArrayList<>(Arrays.asList(skillsStr.split("\\s*,\\s*")));
            vol.setSkills(skillsList);
        }

        db.collection("volunteers").document(uid).set(vol)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.vol_reg_success, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSignup.setEnabled(true);
                    showError(getString(R.string.error_saving_profile, e.getMessage()));
                });
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
        String regex = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$";
        return Pattern.compile(regex).matcher(password).matches();
    }

    private boolean isValidSAPhone(String phone) {
        // Regex for South African phone numbers:
        // Starts with +27 or 0, followed by 9 digits starting with 6, 7, or 8.
        String regex = "^(\\+27|0)[6-8][0-9]{8}$";
        return Pattern.compile(regex).matcher(phone).matches();
    }

    private void setupPopiaLink(TextView tvPopiaLink) {
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
                .setTitle(R.string.popia_details_title)
                .setMessage(R.string.popia_details_message)
                .setPositiveButton("OK", null)
                .show();
    }
}