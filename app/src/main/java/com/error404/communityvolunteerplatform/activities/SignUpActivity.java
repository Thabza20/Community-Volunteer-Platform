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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.GmailOtpHelper;
import com.error404.communityvolunteerplatform.helpers.LocationHelper;
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

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ── Brevo OTP helper (one instance for the session) ──────────

    private final GmailOtpHelper emailHelper = new GmailOtpHelper();
    private LocationHelper locationHelper;
    private ProgressBar pbOrgLocation;
    private Button btnOrgDetectLocation;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocation = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocation = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocation != null && fineLocation) || (coarseLocation != null && coarseLocation)) {
                    startLocationDetection();
                } else {
                    Toast.makeText(this, "Location permission denied. Please type manually.", Toast.LENGTH_LONG).show();
                }
            });

    // Organisation fields
    private EditText etOrgName, etOrgEmail, etOrgPassword, etOrgLocation, etOrgDetails,
            etOrgNumber, etOrgPrimaryPhone, etOrgSecondaryPhone, etOrgOtp;
    // Volunteer fields
    private EditText etVolName, etVolSurname, etVolEmail, etVolPassword, etVolPhone,
            etVolBio, etVolSkills, etVolOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        locationHelper = new LocationHelper(this);

        initViews();
        setupTabLayout();
        setupPopiaLink(findViewById(R.id.tvPopiaLink));
        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> finish());

        btnOrgDetectLocation.setOnClickListener(v -> checkLocationPermissions());

        btnSignup.setOnClickListener(v -> verifyOtpAndCreateAccount());

        // "Request OTP" buttons — now send to email
        Button btnOrgRequestOtp = findViewById(R.id.btnOrgRequestOtp);
        Button btnVolRequestOtp = findViewById(R.id.btnVolRequestOtp);
        btnOrgRequestOtp.setOnClickListener(v -> requestEmailOtp(true));
        btnVolRequestOtp.setOnClickListener(v -> requestEmailOtp(false));
    }

    // ─────────────────────────────────────────────────────────────
    //  OTP via Brevo email
    // ─────────────────────────────────────────────────────────────

    /**
     * Generates an OTP and emails it to the address the user typed in.
     * @param isOrg true = Organisation tab, false = Volunteer tab
     */
    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startLocationDetection();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationDetection() {
        pbOrgLocation.setVisibility(View.VISIBLE);
        btnOrgDetectLocation.setEnabled(false);
        locationHelper.detectLocation(new LocationHelper.OnLocationDetectedListener() {
            @Override
            public void onLocationDetected(String locationName) {
                runOnUiThread(() -> {
                    pbOrgLocation.setVisibility(View.GONE);
                    btnOrgDetectLocation.setEnabled(true);
                    etOrgLocation.setText(locationName);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    pbOrgLocation.setVisibility(View.GONE);
                    btnOrgDetectLocation.setEnabled(true);
                    Toast.makeText(SignUpActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void requestEmailOtp(boolean isOrg) {
        String email = isOrg
                ? etOrgEmail.getText().toString().trim()
                : etVolEmail.getText().toString().trim();

        String name = isOrg
                ? etOrgName.getText().toString().trim()
                : (etVolName.getText().toString().trim() + " " +
                etVolSurname.getText().toString().trim()).trim();

        if (email.isEmpty()) {
            showError("Enter your email address first");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Invalid email address");
            return;
        }

        showError("Sending OTP to " + email + "…");

        // Generate OTP then fire off the email
        emailHelper.generateOtp();
        emailHelper.sendOtp(email, name, new GmailOtpHelper.EmailCallback() {
            @Override
            public void onSuccess() {
                showError("OTP sent! Check your inbox (and spam folder).");
                Toast.makeText(SignUpActivity.this, "OTP sent to " + email, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                showError("Failed to send OTP: " + error);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  Sign-up button handler
    // ─────────────────────────────────────────────────────────────

    private void verifyOtpAndCreateAccount() {
        boolean isOrg = tabLayout.getSelectedTabPosition() == 0;

        // 1. Validate all fields
        if (isOrg) {
            if (isEmpty(etOrgName) || isEmpty(etOrgEmail) || isEmpty(etOrgPassword) ||
                    isEmpty(etOrgLocation) || isEmpty(etOrgDetails) || isEmpty(etOrgNumber) ||
                    isEmpty(etOrgPrimaryPhone) || isEmpty(etOrgOtp)) {
                showError("Please fill in all required fields");
                return;
            }
            if (!isValidPassword(etOrgPassword.getText().toString())) {
                showError("Password must be 8+ chars, 1 uppercase, 1 number, 1 special char");
                return;
            }
            if (!isValidSAPhone(etOrgPrimaryPhone.getText().toString())) {
                showError("Invalid primary phone number");
                return;
            }
            String secPhone = etOrgSecondaryPhone.getText().toString().trim();
            if (!secPhone.isEmpty() && !isValidSAPhone(secPhone)) {
                showError("Invalid secondary phone number");
                return;
            }
        } else {
            if (isEmpty(etVolName) || isEmpty(etVolSurname) || isEmpty(etVolEmail) ||
                    isEmpty(etVolPassword) || isEmpty(etVolPhone) || isEmpty(etVolBio) ||
                    isEmpty(etVolSkills) || isEmpty(etVolOtp)) {
                showError("Please fill in all required fields");
                return;
            }
            if (!isValidPassword(etVolPassword.getText().toString())) {
                showError("Password requirements not met");
                return;
            }
            if (!isValidSAPhone(etVolPhone.getText().toString())) {
                showError("Invalid phone number");
                return;
            }
        }

        if (!cbPopia.isChecked()) {
            showError("You must consent to the POPIA act");
            return;
        }

        // 2. Verify OTP locally
        String enteredOtp = isOrg
                ? etOrgOtp.getText().toString().trim()
                : etVolOtp.getText().toString().trim();

        if (emailHelper.isExpired()) {
            showError("OTP has expired — please request a new one");
            return;
        }
        if (!emailHelper.verifyOtp(enteredOtp)) {
            showError("Incorrect OTP. Please try again.");
            return;
        }

        // 3. OTP is valid — create the Firebase account
        emailHelper.invalidate(); // prevent re-use
        createEmailPasswordAccount(isOrg);
    }

    // ─────────────────────────────────────────────────────────────
    //  Firebase account creation (unchanged logic, cleaned up)
    // ─────────────────────────────────────────────────────────────

    private void createEmailPasswordAccount(boolean isOrg) {
        String email    = isOrg ? etOrgEmail.getText().toString().trim()    : etVolEmail.getText().toString().trim();
        String password = isOrg ? etOrgPassword.getText().toString().trim() : etVolPassword.getText().toString().trim();

        btnSignup.setEnabled(false);
        showError("Creating account…");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserDataToFirestore(uid, isOrg);
                    } else {
                        btnSignup.setEnabled(true);
                        showError("Signup failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    //  Firestore save methods (unchanged)
    // ─────────────────────────────────────────────────────────────

    private void saveUserDataToFirestore(String uid, boolean isOrg) {
        String email = isOrg
                ? etOrgEmail.getText().toString().trim()
                : etVolEmail.getText().toString().trim();
        String role = isOrg ? "organisation" : "volunteer";

        User user = new User(uid, role, email);
        user.setPopiaAccepted(true);
        user.setEmailVerified(true); // they verified via OTP

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    if (isOrg) saveOrganisationProfile(uid);
                    else       saveVolunteerProfile(uid);
                })
                .addOnFailureListener(e -> {
                    btnSignup.setEnabled(true);
                    showError("Error saving user: " + e.getMessage());
                });
    }

    private void saveOrganisationProfile(String uid) {
        Organisation org = new Organisation(
                uid,
                etOrgName.getText().toString().trim(),
                etOrgEmail.getText().toString().trim(),
                etOrgLocation.getText().toString().trim(),
                etOrgPrimaryPhone.getText().toString().trim(),
                etOrgNumber.getText().toString().trim());

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
                    showError("Error saving profile: " + e.getMessage());
                });
    }

    private void saveVolunteerProfile(String uid) {
        Volunteer vol = new Volunteer(
                uid,
                etVolName.getText().toString().trim(),
                etVolSurname.getText().toString().trim(),
                etVolEmail.getText().toString().trim(),
                "");

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
                    showError("Error saving profile: " + e.getMessage());
                });
    }

    // ─────────────────────────────────────────────────────────────
    //  View init & helpers (unchanged)
    // ─────────────────────────────────────────────────────────────

    private void initViews() {
        tabLayout       = findViewById(R.id.tabLayout);
        layoutOrgForm   = findViewById(R.id.layoutOrgForm);
        layoutVolForm   = findViewById(R.id.layoutVolForm);
        cbPopia         = findViewById(R.id.cbPopia);
        tvErrorMessage  = findViewById(R.id.tvErrorMessage);
        tvSignupHeading = findViewById(R.id.tvSignupHeading);
        btnSignup       = findViewById(R.id.btnSignup);

        // Organisation
        etOrgName           = findViewById(R.id.etOrgName);
        etOrgEmail          = findViewById(R.id.etOrgEmail);
        etOrgPassword       = findViewById(R.id.etOrgPassword);
        etOrgLocation       = findViewById(R.id.etOrgLocation);
        etOrgDetails        = findViewById(R.id.etOrgDetails);
        etOrgNumber         = findViewById(R.id.etOrgNumber);
        etOrgPrimaryPhone   = findViewById(R.id.etOrgPrimaryPhone);
        etOrgSecondaryPhone = findViewById(R.id.etOrgSecondaryPhone);
        etOrgOtp            = findViewById(R.id.etOrgOtp);
        btnOrgDetectLocation = findViewById(R.id.btnOrgDetectLocation);
        pbOrgLocation        = findViewById(R.id.pbOrgLocation);

        // Volunteer
        etVolName    = findViewById(R.id.etVolName);
        etVolSurname = findViewById(R.id.etVolSurname);
        etVolEmail   = findViewById(R.id.etVolEmail);
        etVolPassword= findViewById(R.id.etVolPassword);
        etVolPhone   = findViewById(R.id.etVolPhone);
        etVolBio     = findViewById(R.id.etVolBio);
        etVolSkills  = findViewById(R.id.etVolSkills);
        etVolOtp     = findViewById(R.id.etVolOtp);
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tvErrorMessage.setVisibility(View.GONE);
                if (tab.getPosition() == 0) {
                    layoutOrgForm.setVisibility(View.VISIBLE);
                    layoutVolForm.setVisibility(View.GONE);
                    tvSignupHeading.setText(R.string.organization_signup);
                } else {
                    layoutOrgForm.setVisibility(View.GONE);
                    layoutVolForm.setVisibility(View.VISIBLE);
                    tvSignupHeading.setText(R.string.volunteer_signup);
                }
                // Reset OTP state when switching tabs
                emailHelper.invalidate();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        layoutOrgForm.setVisibility(View.VISIBLE);
        layoutVolForm.setVisibility(View.GONE);
        tvSignupHeading.setText(R.string.organization_signup);
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private boolean isEmpty(EditText et) {
        return et.getText().toString().trim().isEmpty();
    }

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$";
        return Pattern.compile(regex).matcher(password).matches();
    }

    private boolean isValidSAPhone(String phone) {
        String regex = "^(\\+27|0)[6-8][0-9]{8}$";
        return Pattern.compile(regex).matcher(phone).matches();
    }

    private void setupPopiaLink(TextView tvPopiaLink) {
        String text = "I consent according to the POPIA act";
        SpannableString ss = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) { showPopiaDialog(); }
        };
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