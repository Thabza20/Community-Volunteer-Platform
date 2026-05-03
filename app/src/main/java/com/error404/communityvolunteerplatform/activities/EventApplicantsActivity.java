package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.OrgApplicationAdapter;
import com.error404.communityvolunteerplatform.models.Application;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventApplicantsActivity extends AppCompatActivity {

    private RecyclerView rvApplicants;
    private OrgApplicationAdapter adapter;
    private List<Application> applicationList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvNoApplicants;

    private FirebaseFirestore db;
    private String opportunityId;
    private String opportunityTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_applicants);

        db = FirebaseFirestore.getInstance();
        opportunityId = getIntent().getStringExtra("OPPORTUNITY_ID");
        opportunityTitle = getIntent().getStringExtra("OPPORTUNITY_TITLE");

        if (opportunityId == null) {
            finish();
            return;
        }

        initializeViews();
        loadApplicants();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(opportunityTitle != null ? opportunityTitle : "Applicants");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvApplicants = findViewById(R.id.rvApplicants);
        progressBar = findViewById(R.id.progressBar);
        tvNoApplicants = findViewById(R.id.tvNoApplicants);

        rvApplicants.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrgApplicationAdapter(applicationList, new OrgApplicationAdapter.OnApplicationActionListener() {
            @Override
            public void onApprove(Application application) {
                updateApplicationStatus(application, "approved");
            }

            @Override
            public void onReject(Application application) {
                updateApplicationStatus(application, "rejected");
            }
        });
        rvApplicants.setAdapter(adapter);
    }

    private void loadApplicants() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("applications")
                .whereEqualTo("opportunityId", opportunityId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    applicationList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Application app = doc.toObject(Application.class);
                        app.setApplicationId(doc.getId());
                        applicationList.add(app);
                    }
                    adapter.notifyDataSetChanged();
                    tvNoApplicants.setVisibility(applicationList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading applicants", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateApplicationStatus(Application application, String newStatus) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("applications").document(application.getApplicationId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    if ("approved".equals(newStatus)) {
                        // Increment slotsFilled in the opportunity document
                        db.collection("opportunities").document(opportunityId)
                                .update("slotsFilled", FieldValue.increment(1))
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(this, "Application Approved", Toast.LENGTH_SHORT).show();
                                    loadApplicants(); // Refresh list
                                });
                    } else {
                        Toast.makeText(this, "Application Rejected", Toast.LENGTH_SHORT).show();
                        loadApplicants(); // Refresh list
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }
}