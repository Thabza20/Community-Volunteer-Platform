package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.Organisation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrganizationProfileActivity extends AppCompatActivity {

    private ImageView imgOrgLogo;
    private Button btnChangeLogo, btnSaveProfile, btnChangePassword, btnDeleteAccount;
    private EditText etOrgName, etEmail, etOrgNumber, etLocation, etOrgDetails, etPrimaryPhone;

    private FirebaseFirestore db;
    private String currentUserId;
    private Organisation currentOrg;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    imgOrgLogo.setImageURI(imageUri);
                    // In a real app, we would upload to Cloudinary or Firebase Storage here
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_org_profile);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        initializeViews();
        loadProfile();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        btnChangeLogo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        btnChangePassword.setOnClickListener(v ->
                Toast.makeText(this, "Change Password feature coming soon", Toast.LENGTH_SHORT).show()
        );

        btnDeleteAccount.setOnClickListener(v -> {
             // Basic implementation for now, would typically involve Auth deletion too
             Toast.makeText(this, "Account deletion requires support contact", Toast.LENGTH_LONG).show();
        });
    }

    private void initializeViews() {
        imgOrgLogo = findViewById(R.id.imgOrgLogo);
        btnChangeLogo = findViewById(R.id.btnChangeLogo);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        etOrgName = findViewById(R.id.etOrgName);
        etEmail = findViewById(R.id.etEmail);
        etOrgNumber = findViewById(R.id.etOrgNumber);
        etLocation = findViewById(R.id.etLocation);
        etOrgDetails = findViewById(R.id.etOrgDetails);
        etPrimaryPhone = findViewById(R.id.etPrimaryPhone);
    }


    private void loadProfile() {
        db.collection("organisations").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentOrg = documentSnapshot.toObject(Organisation.class);
                        if (currentOrg != null) {
                            currentOrg.setUserId(documentSnapshot.getId());
                            etOrgName.setText(currentOrg.getOrgName());
                            etEmail.setText(currentOrg.getEmail());
                            etOrgNumber.setText(currentOrg.getOrgNumber());
                            etLocation.setText(currentOrg.getLocation());
                            etOrgDetails.setText(currentOrg.getOrgDetails());
                            etPrimaryPhone.setText(currentOrg.getPrimaryPhoneNumber());
                            
                            // Load logo if exists
                            // if (currentOrg.getLogoUrl() != null) { Glide.with(this).load(currentOrg.getLogoUrl()).into(imgOrgLogo); }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        String name = etOrgName.getText().toString().trim();
        String number = etOrgNumber.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String details = etOrgDetails.getText().toString().trim();
        String phone = etPrimaryPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveProfile.setEnabled(false);
        Toast.makeText(this, "Saving profile...", Toast.LENGTH_SHORT).show();

        db.collection("organisations").document(currentUserId)
                .update("orgName", name,
                        "orgNumber", number,
                        "location", location,
                        "orgDetails", details,
                        "primaryPhoneNumber", phone)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile successfully saved!", Toast.LENGTH_SHORT).show();
                    finish(); // Return to Organization Dashboard
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Save unsuccessful: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}