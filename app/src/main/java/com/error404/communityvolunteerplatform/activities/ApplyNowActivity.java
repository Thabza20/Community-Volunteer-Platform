package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.BadgeAwardHelper;
import com.error404.communityvolunteerplatform.helpers.CloudinaryManager;
import com.error404.communityvolunteerplatform.helpers.GroqRecommendationHelper;
import com.error404.communityvolunteerplatform.helpers.NotificationHelper;
import com.error404.communityvolunteerplatform.models.Application;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ApplyNowActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_DOCUMENT = 1002;

    private TextView tvTitle, tvLocation, tvDate, tvSlotsRemaining, tvBanner, tvUploadStatus;
    private ChipGroup cgApplyInfo;
    private TextInputEditText etMotivation, etRefName, etRefPhone, etRefEmail;
    private TextInputLayout tilMotivation;
    private Button btnApplyNow, btnUploadQualification, btnGenerateCoverLetter;
    private ProgressBar pbApply, pbQualUpload, pbAiLetter;
    private LinearLayout llExperienceSection, llQualificationSection;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String opportunityId;
    private Opportunity opportunity;
    private Volunteer volunteer;
    private boolean hasApplied = false;
    private Uri selectedDocumentUri = null;
    private String qualificationUploadUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_now);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        opportunityId = getIntent().getStringExtra("OPPORTUNITY_ID");
        if (opportunityId == null) {
            Toast.makeText(this, "Error: Opportunity not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadData();

        btnApplyNow.setOnClickListener(v -> handleApply());
        btnGenerateCoverLetter.setOnClickListener(v -> generateAiCoverLetter());
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvApplyTitle);
        tvLocation = findViewById(R.id.tvApplyLocation);
        tvDate = findViewById(R.id.tvApplyDate);
        tvSlotsRemaining = findViewById(R.id.tvApplySlotsRemaining);
        tvBanner = findViewById(R.id.tvApplyBanner);
        cgApplyInfo = findViewById(R.id.cgApplyInfo);
        etMotivation = findViewById(R.id.etMotivation);
        tilMotivation = findViewById(R.id.tilMotivation);
        btnApplyNow = findViewById(R.id.btnApplyNow);
        pbApply = findViewById(R.id.pbApply);
        btnGenerateCoverLetter = findViewById(R.id.btnGenerateCoverLetter);
        pbAiLetter = findViewById(R.id.pbAiLetter);

        // Conditional fields
        llExperienceSection = findViewById(R.id.llExperienceSection);
        etRefName = findViewById(R.id.etRefName);
        etRefPhone = findViewById(R.id.etRefPhone);
        etRefEmail = findViewById(R.id.etRefEmail);

        llQualificationSection = findViewById(R.id.llQualificationSection);
        btnUploadQualification = findViewById(R.id.btnUploadQualification);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);
        pbQualUpload = findViewById(R.id.pbQualUpload);

        btnApplyNow.setEnabled(false); // Enable after data load

        btnUploadQualification.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "image/*"});
            startActivityForResult(intent, REQUEST_PICK_DOCUMENT);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_DOCUMENT && resultCode == RESULT_OK && data != null) {
            selectedDocumentUri = data.getData();
            if (selectedDocumentUri != null) {
                String filename = DocumentFile.fromSingleUri(this, selectedDocumentUri).getName();
                tvUploadStatus.setText(filename != null ? filename : "Document selected");
            }
        }
    }

    private void loadData() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        // 1. Fetch Opportunity
        db.collection("opportunities").document(opportunityId).get()
                .addOnSuccessListener(oppDoc -> {
                    if (oppDoc.exists()) {
                        opportunity = oppDoc.toObject(Opportunity.class);
                        if (opportunity != null) {
                            opportunity.setOpportunityId(oppDoc.getId());
                        }

                        // 2. Fetch Volunteer
                        db.collection("volunteers").document(userId).get()
                                .addOnSuccessListener(volDoc -> {
                                    if (volDoc.exists()) {
                                        volunteer = volDoc.toObject(Volunteer.class);
                                    }

                                    // 3. Check for existing application
                                    db.collection("applications")
                                            .whereEqualTo("opportunityId", opportunityId)
                                            .whereEqualTo("volunteerId", userId)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                hasApplied = !querySnapshot.isEmpty();
                                                populateUI();
                                            });
                                });
                    }
                });
    }

    private void generateAiCoverLetter() {
        if (volunteer == null || opportunity == null) {
            Toast.makeText(this, "Still loading data...", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerateCoverLetter.setEnabled(false);
        pbAiLetter.setVisibility(View.VISIBLE);

        GroqRecommendationHelper.generateCoverLetter(
                volunteer.getUserId(),
                opportunity.getTitle(),
                opportunity.getOpportunityDescription(),
                opportunity.getCategory(),
                new GroqRecommendationHelper.OnCoverLetterListener() {
                    @Override
                    public void onSuccess(String coverLetter) {
                        etMotivation.setText(coverLetter);
                        btnGenerateCoverLetter.setEnabled(true);
                        pbAiLetter.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ApplyNowActivity.this, "Could not generate — write your own motivation", Toast.LENGTH_SHORT).show();
                        btnGenerateCoverLetter.setEnabled(true);
                        pbAiLetter.setVisibility(View.GONE);
                    }
                }
        );
    }

    private void populateUI() {
        if (opportunity == null || volunteer == null) return;

        tvTitle.setText(opportunity.getTitle());
        tvLocation.setText(opportunity.getLocation());

        // Handle eventDate
        Object dateObj = opportunity.getEventDate();
        if (dateObj instanceof Timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(((Timestamp) dateObj).toDate()));
        } else if (dateObj instanceof String) {
            tvDate.setText((String) dateObj);
        } else {
            tvDate.setText("Date TBC");
        }

        // Category Chip
        cgApplyInfo.removeAllViews();
        addChip(opportunity.getCategory(), ContextCompat.getColor(this, R.color.passport_card_accent));

        if (opportunity.isRequiresExperience()) {
            addChip("Requires Experience", android.graphics.Color.GRAY);
            llExperienceSection.setVisibility(View.VISIBLE);
        }
        if (opportunity.isRequiresQualification()) {
            addChip("Requires Qualification", android.graphics.Color.GRAY);
            llQualificationSection.setVisibility(View.VISIBLE);
        }

        // Slots Remaining
        int remaining = opportunity.getSlotsTotal() - opportunity.getSlotsFilled();
        tvSlotsRemaining.setText(remaining + " of " + opportunity.getSlotsTotal() + " spots remaining");
        if (remaining > 0) {
            tvSlotsRemaining.setTextColor(ContextCompat.getColor(this, R.color.passport_card_accent));
        } else {
            tvSlotsRemaining.setTextColor(android.graphics.Color.RED);
        }

        // Banner and Button State
        boolean canApply = true;
        if (hasApplied) {
            tvBanner.setText("You have already applied for this opportunity");
            tvBanner.setVisibility(View.VISIBLE);
            canApply = false;
        } else if (opportunity.getSlotsFilled() >= opportunity.getSlotsTotal()) {
            tvBanner.setText("This opportunity is full");
            tvBanner.setVisibility(View.VISIBLE);
            canApply = false;
        }

        btnApplyNow.setEnabled(canApply);
    }

    private void addChip(String text, int color) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
        chip.setTextColor(android.graphics.Color.WHITE);
        chip.setClickable(false);
        cgApplyInfo.addView(chip);
    }

    private void handleApply() {
        String motivation = etMotivation.getText().toString().trim();

        if (motivation.isEmpty()) {
            tilMotivation.setError("Please tell us why you want to volunteer");
            return;
        }

        if (motivation.length() < 20) {
            tilMotivation.setError("Please write at least 20 characters");
            return;
        }

        tilMotivation.setError(null);

        // Experience validation
        String refName = null, refPhone = null, refEmail = null;
        if (llExperienceSection.getVisibility() == View.VISIBLE) {
            refName = etRefName.getText().toString().trim();
            refPhone = etRefPhone.getText().toString().trim();
            refEmail = etRefEmail.getText().toString().trim();

            if (refName.isEmpty()) {
                etRefName.setError("Reference name is required");
                return;
            }
            if (refPhone.isEmpty() && refEmail.isEmpty()) {
                etRefPhone.setError("Please provide at least a phone number or email");
                return;
            }
        }

        // Qualification validation
        if (llQualificationSection.getVisibility() == View.VISIBLE && selectedDocumentUri == null) {
            Toast.makeText(this, "Please upload your qualification document", Toast.LENGTH_SHORT).show();
            return;
        }

        btnApplyNow.setEnabled(false);
        pbApply.setVisibility(View.VISIBLE);

        if (llQualificationSection.getVisibility() == View.VISIBLE && selectedDocumentUri != null) {
            uploadQualificationAndSave(motivation, refName, refPhone, refEmail);
        } else {
            saveApplication(motivation, refName, refPhone, refEmail);
        }
    }

    private void uploadQualificationAndSave(String motivation, String refName, String refPhone, String refEmail) {
        pbQualUpload.setVisibility(View.VISIBLE);
        String currentUserId = auth.getCurrentUser().getUid();
        String publicId = currentUserId + "_qualification_" + opportunityId;

        CloudinaryManager.uploadProfilePhoto(publicId, selectedDocumentUri, new CloudinaryManager.OnUploadListener() {
            @Override
            public void onProgress(int percent) {}

            @Override
            public void onSuccess(String secureUrl) {
                qualificationUploadUrl = secureUrl;
                runOnUiThread(() -> {
                    pbQualUpload.setVisibility(View.GONE);
                    saveApplication(motivation, refName, refPhone, refEmail);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    pbQualUpload.setVisibility(View.GONE);
                    btnApplyNow.setEnabled(true);
                    pbApply.setVisibility(View.GONE);
                    Toast.makeText(ApplyNowActivity.this, "Upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveApplication(String motivation, String refName, String refPhone, String refEmail) {
        String currentUserId = auth.getCurrentUser().getUid();

        Map<String, Object> appData = new HashMap<>();
        appData.put("opportunityId", opportunityId);
        appData.put("orgId", opportunity.getOrgId());
        appData.put("volunteerId", currentUserId);
        appData.put("volunteerName", volunteer.getFullName());
        appData.put("volunteerEmail", volunteer.getEmail());
        appData.put("motivation", motivation);
        appData.put("status", "pending");
        appData.put("withdrawnStatus", false);
        appData.put("appliedAt", Timestamp.now());
        appData.put("updatedAt", Timestamp.now());

        if (refName != null && !refName.isEmpty()) appData.put("refName", refName);
        if (refPhone != null && !refPhone.isEmpty()) appData.put("refPhone", refPhone);
        if (refEmail != null && !refEmail.isEmpty()) appData.put("refEmail", refEmail);
        if (qualificationUploadUrl != null) appData.put("cvFileUrl", qualificationUploadUrl);

        db.collection("applications").add(appData)
                .addOnSuccessListener(documentReference -> {
                    String appId = documentReference.getId();
                    documentReference.update("applicationId", appId);

                    NotificationHelper.createNotification(
                            currentUserId,
                            "Application Submitted",
                            "Your application for " + opportunity.getTitle() + " has been submitted and is under review.",
                            "application_pending",
                            appId
                    );

                    // Also notify the Organization
                    NotificationHelper.createNotification(
                            opportunity.getOrgId(),
                            "New Applicant",
                            volunteer.getFullName() + " applied for " + opportunity.getTitle(),
                            "new_application",
                            opportunityId
                    );

                    BadgeAwardHelper.checkAndAward(currentUserId, this);
                    Toast.makeText(this, "Application submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnApplyNow.setEnabled(true);
                    pbApply.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
