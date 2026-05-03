package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.Application;
import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

public class OpportunityDetailsActivity extends AppCompatActivity {

    private TextView tvTitle, tvOrgName, tvLocation, tvCategory, tvOrgDesc, tvOppDesc, tvSlots;
    private Button btnApply;
    private ProgressBar pbLoading;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String opportunityId;
    private String orgId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunity_details);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        opportunityId = getIntent().getStringExtra("OPPORTUNITY_ID");
        if (opportunityId == null) {
            Toast.makeText(this, "Error: Opportunity not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Opportunity Details");
        }

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvOrgName = findViewById(R.id.tvDetailOrgName);
        tvLocation = findViewById(R.id.tvDetailLocation);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvOrgDesc = findViewById(R.id.tvDetailOrgDesc);
        tvOppDesc = findViewById(R.id.tvDetailOppDesc);
        tvSlots = findViewById(R.id.tvDetailSlots);
        btnApply = findViewById(R.id.btnApply);
        pbLoading = findViewById(R.id.pbDetailsLoading);

        loadOpportunityDetails();
        checkIfAlreadyApplied();

        btnApply.setOnClickListener(v -> applyForOpportunity());
    }

    private void checkIfAlreadyApplied() {
        if (auth.getCurrentUser() == null) return;

        db.collection("applications")
                .whereEqualTo("opportunityId", opportunityId)
                .whereEqualTo("volunteerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        btnApply.setEnabled(false);
                        btnApply.setText("Already Applied");
                    }
                });
    }

    private void applyForOpportunity() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to apply", Toast.LENGTH_SHORT).show();
            return;
        }

        String volunteerId = auth.getCurrentUser().getUid();
        btnApply.setEnabled(false);
        btnApply.setText("Applying...");

        // Fetch volunteer data to create application
        db.collection("volunteers").document(volunteerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Volunteer volunteer = doc.toObject(Volunteer.class);
                        if (volunteer != null) {
                            submitApplication(volunteer);
                        }
                    } else {
                        // Maybe it's an organization trying to apply?
                        Toast.makeText(this, "Only volunteers can apply for opportunities", Toast.LENGTH_SHORT).show();
                        btnApply.setEnabled(true);
                        btnApply.setText("Apply Now");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnApply.setEnabled(true);
                    btnApply.setText("Apply Now");
                });
    }

    private void submitApplication(Volunteer volunteer) {
        String volunteerName = volunteer.getFirstName() + " " + volunteer.getSurname();
        Application application = new Application(
                opportunityId,
                orgId,
                volunteer.getUserId(),
                volunteerName,
                volunteer.getEmail()
        );
        application.setAppliedAt(Timestamp.now());
        application.setUpdatedAt(Timestamp.now());
        application.setStatus(Application.STATUS_PENDING);

        db.collection("applications").add(application)
                .addOnSuccessListener(documentReference -> {
                    String appId = documentReference.getId();
                    documentReference.update("applicationId", appId);
                    Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_LONG).show();
                    btnApply.setText("Already Applied");
                    btnApply.setEnabled(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnApply.setEnabled(true);
                    btnApply.setText("Apply Now");
                });
    }

    private void loadOpportunityDetails() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        db.collection("opportunities").document(opportunityId).get()
                .addOnSuccessListener(doc -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    if (doc.exists()) {
                        orgId = doc.getString("orgId");
                        tvTitle.setText(doc.getString("title"));
                        tvOrgName.setText(doc.getString("orgName"));
                        tvLocation.setText(doc.getString("location"));
                        tvCategory.setText(doc.getString("category"));
                        tvOrgDesc.setText(doc.getString("orgDescription"));
                        tvOppDesc.setText(doc.getString("opportunityDescription"));

                        Long filledVal = doc.getLong("slotsFilled");
                        Long totalVal = doc.getLong("slotsTotal");
                        long filled = filledVal != null ? filledVal : 0;
                        long total = totalVal != null ? totalVal : 0;
                        tvSlots.setText(getString(R.string.slots_filled_format, filled, total));

                        if (filled >= total) {
                            btnApply.setEnabled(false);
                            btnApply.setText("Full");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
