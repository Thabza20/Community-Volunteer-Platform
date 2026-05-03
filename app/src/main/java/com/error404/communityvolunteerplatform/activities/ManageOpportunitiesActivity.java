package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.OpportunityManagementAdapter;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        
        adapter = new OpportunityManagementAdapter(filteredOpportunities, applicantCounts, opportunity -> {
            Intent intent = new Intent(ManageOpportunitiesActivity.this, EventApplicantsActivity.class);
            intent.putExtra("OPPORTUNITY_ID", opportunity.getOpportunityId());
            intent.putExtra("OPPORTUNITY_TITLE", opportunity.getTitle());
            startActivity(intent);
        });
        rvOpportunities.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddOpportunity);
        fab.setOnClickListener(v -> {
            // TODO: Open Create Opportunity Activity
            Toast.makeText(this, "Create Opportunity", Toast.LENGTH_SHORT).show();
        });

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