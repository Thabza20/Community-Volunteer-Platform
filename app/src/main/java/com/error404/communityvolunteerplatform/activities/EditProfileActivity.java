package com.error404.communityvolunteerplatform.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.BadgeAwardHelper;
import com.error404.communityvolunteerplatform.helpers.LocationHelper;
import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etPhoneNumber, etBio, etLocation, etSkillInput;
    private ChipGroup cgSkills;
    private MaterialButton btnDetectLocation, btnAddSkill, btnSaveProfile;
    private ProgressBar pbLocation, pbSave;

    private FirebaseFirestore db;
    private String currentUserId;
    private Volunteer currentVolunteer;
    private LocationHelper locationHelper;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocation != null && fineLocation || coarseLocation != null && coarseLocation) {
                    startLocationDetection();
                } else {
                    Toast.makeText(this, "Location permission denied. Please type manually.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        locationHelper = new LocationHelper(this);

        if (currentUserId == null) {
            finish();
            return;
        }

        bindViews();
        loadProfileData();

        btnDetectLocation.setOnClickListener(v -> checkLocationPermissions());
        btnAddSkill.setOnClickListener(v -> addSkillFromInput());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void bindViews() {
        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etBio = findViewById(R.id.etBio);
        etLocation = findViewById(R.id.etLocation);
        etSkillInput = findViewById(R.id.etSkillInput);
        cgSkills = findViewById(R.id.cgSkills);
        btnDetectLocation = findViewById(R.id.btnDetectLocation);
        btnAddSkill = findViewById(R.id.btnAddSkill);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        pbLocation = findViewById(R.id.pbLocation);
        pbSave = findViewById(R.id.pbSave);
    }

    private void loadProfileData() {
        db.collection("volunteers").document(currentUserId).get().addOnSuccessListener(doc -> {
            currentVolunteer = doc.toObject(Volunteer.class);
            if (currentVolunteer != null) {
                etFullName.setText(currentVolunteer.getFullName());
                etPhoneNumber.setText(currentVolunteer.getPhoneNumber());
                etBio.setText(currentVolunteer.getBio());
                etLocation.setText(currentVolunteer.getLocation());

                if (currentVolunteer.getSkills() != null) {
                    for (String skill : currentVolunteer.getSkills()) {
                        addSkillChip(skill);
                    }
                }
            }
        });
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationDetection();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationDetection() {
        pbLocation.setVisibility(View.VISIBLE);
        btnDetectLocation.setEnabled(false);
        locationHelper.detectLocation(new LocationHelper.OnLocationDetectedListener() {
            @Override
            public void onLocationDetected(String locationName) {
                runOnUiThread(() -> {
                    pbLocation.setVisibility(View.GONE);
                    btnDetectLocation.setEnabled(true);
                    etLocation.setText(locationName);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    pbLocation.setVisibility(View.GONE);
                    btnDetectLocation.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void addSkillFromInput() {
        String skill = etSkillInput.getText().toString().trim();
        if (!skill.isEmpty()) {
            addSkillChip(skill);
            etSkillInput.setText("");
        }
    }

    private void addSkillChip(String skill) {
        Chip chip = new Chip(this);
        chip.setText(skill);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> cgSkills.removeView(chip));
        cgSkills.addView(chip);
    }

    private void saveProfile() {
        pbSave.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        String name = etFullName.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String loc = etLocation.getText().toString().trim();

        List<String> skills = new ArrayList<>();
        for (int i = 0; i < cgSkills.getChildCount(); i++) {
            Chip chip = (Chip) cgSkills.getChildAt(i);
            skills.add(chip.getText().toString());
        }

        db.collection("volunteers").document(currentUserId)
                .update("fullName", name,
                        "phoneNumber", phone,
                        "bio", bio,
                        "location", loc,
                        "skills", skills)
                .addOnSuccessListener(aVoid -> {
                    BadgeAwardHelper.checkAndAward(currentUserId, this);
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pbSave.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) locationHelper.stopUpdates();
    }
}