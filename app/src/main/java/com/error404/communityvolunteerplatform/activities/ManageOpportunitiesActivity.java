package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.OpportunityManagementAdapter;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageOpportunitiesActivity extends AppCompatActivity {

    private RecyclerView rvOpportunities;
    private OpportunityManagementAdapter adapter;
    private List<Opportunity> allOpportunities = new ArrayList<>();
    private List<Opportunity> filteredOpportunities = new ArrayList<>();
    private Map<String, Integer> applicantCounts = new HashMap<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String orgId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_opportunities);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            orgId = mAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvOpportunities = findViewById(R.id.rvOpportunities);
        rvOpportunities.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OpportunityManagementAdapter(filteredOpportunities, applicantCounts, new OpportunityManagementAdapter.OnOpportunityClickListener() {
            @Override
            public void onOpportunityClick(Opportunity opportunity) {
                Intent intent = new Intent(ManageOpportunitiesActivity.this, EventApplicantsActivity.class);
                intent.putExtra("OPPORTUNITY_ID", opportunity.getOpportunityId());
                intent.putExtra("OPPORTUNITY_TITLE", opportunity.getTitle());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Opportunity opportunity) {
                Intent intent = new Intent(ManageOpportunitiesActivity.this, CreateEventActivity.class);
                intent.putExtra("EDIT_MODE", true);
                intent.putExtra("OPPORTUNITY_ID", opportunity.getOpportunityId());
                startActivity(intent);
            }

            @Override
            public void onCancelClick(Opportunity opportunity) {
                showCancelConfirmationDialog(opportunity);
            }
        });
        rvOpportunities.setAdapter(adapter);

        // FloatingActionButton code has been removed from here

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadOpportunities();
    }

    private void loadOpportunities() {
        db.collection("opportunities")
                .whereEqualTo("orgId", orgId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allOpportunities.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Opportunity opp = doc.toObject(Opportunity.class);
                        opp.setOpportunityId(doc.getId());
                        allOpportunities.add(opp);
                    }
                    loadApplicantCounts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load opportunities", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadApplicantCounts() {
        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    applicantCounts.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String oppId = doc.getString("opportunityId");
                        if (oppId != null) {
                            Integer currentCount = applicantCounts.get(oppId);
                            applicantCounts.put(oppId, (currentCount == null ? 0 : currentCount) + 1);
                        }
                    }
                    filter(""); // Update display
                });
    }

    private void showCancelConfirmationDialog(Opportunity opportunity) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.cancel_event)
                .setMessage("Are you sure you want to cancel '" + opportunity.getTitle() + "'? This will notify all applicants and mark the event as canceled.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelEvent(opportunity))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelEvent(Opportunity opportunity) {
        WriteBatch batch = db.batch();

        // 1. Update Opportunity Status
        batch.update(db.collection("opportunities").document(opportunity.getOpportunityId()),
                "status", Opportunity.STATUS_CANCELED,
                "updatedAt", com.google.firebase.Timestamp.now());

        // 2. Update Applications and Create Notifications
        db.collection("applications")
                .whereEqualTo("opportunityId", opportunity.getOpportunityId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "status", "canceled");

                        // Create notification for volunteer
                        String volunteerId = doc.getString("volunteerId");
                        if (volunteerId != null) {
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("userId", volunteerId);
                            notification.put("title", "Event Canceled");
                            notification.put("message", "The event '" + opportunity.getTitle() + "' has been canceled by the organizer.");
                            notification.put("timestamp", com.google.firebase.Timestamp.now());
                            notification.put("read", false);
                            notification.put("type", "event_cancellation");

                            batch.set(db.collection("notifications").document(), notification);
                        }
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, R.string.event_canceled_success, Toast.LENGTH_SHORT).show();
                        loadOpportunities(); // Refresh list
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to cancel event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching applications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filter(String text) {
        filteredOpportunities.clear();
        for (Opportunity opp : allOpportunities) {
            if (opp.getTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredOpportunities.add(opp);
            }
        }
        adapter.updateData(filteredOpportunities, applicantCounts);
    }
}