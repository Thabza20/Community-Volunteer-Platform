package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.error404.communityvolunteerplatform.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OpportunityDetailsActivity extends AppCompatActivity {

    private TextView tvTitle, tvOrgName, tvLocation, tvCategory, tvOrgDesc, tvOppDesc, tvSlots;
    private Button btnApply;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String opportunityId;

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

        loadOpportunityDetails();

        btnApply.setOnClickListener(v -> {
            // TODO: Implement application logic
            Toast.makeText(this, "Applying...", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadOpportunityDetails() {
        db.collection("opportunities").document(opportunityId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvTitle.setText(doc.getString("title"));
                        tvOrgName.setText(doc.getString("orgName"));
                        tvLocation.setText(doc.getString("location"));
                        tvCategory.setText(doc.getString("category"));
                        tvOrgDesc.setText(doc.getString("orgDescription"));
                        tvOppDesc.setText(doc.getString("opportunityDescription"));

                        long filled = doc.getLong("slotsFilled") != null ? doc.getLong("slotsFilled") : 0;
                        long total = doc.getLong("slotsTotal") != null ? doc.getLong("slotsTotal") : 0;
                        tvSlots.setText(filled + " / " + total + " slots filled");

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
