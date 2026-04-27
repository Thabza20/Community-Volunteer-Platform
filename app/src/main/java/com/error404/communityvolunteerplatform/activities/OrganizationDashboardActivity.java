package com.error404.communityvolunteerplatform.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.error404.communityvolunteerplatform.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class OrganizationDashboardActivity extends AppCompatActivity {

    private TextView tvActiveOpportunities, tvTotalVolunteers, tvTotalOpportunities,
            tvPendingApplications, tvApprovedApplications, tvCompletionRate;

    private Button btnCreateOpportunity;
    private CardView cvViewOpportunities;
    private ImageView ivProfileIcon;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String orgId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_org_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            orgId = currentUser.getUid();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        progressDialog = new ProgressDialog(this);

        initializeViews();
        setupClickListeners();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initializeViews() {
        tvActiveOpportunities = findViewById(R.id.tvActiveOpportunities);
        tvTotalVolunteers = findViewById(R.id.tvTotalVolunteers);
        tvTotalOpportunities = findViewById(R.id.tvTotalOpportunities);
        tvPendingApplications = findViewById(R.id.tvPendingApplications);
        tvApprovedApplications = findViewById(R.id.tvApprovedApplications);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);

        btnCreateOpportunity = findViewById(R.id.btnCreateOpportunity);
        cvViewOpportunities = findViewById(R.id.cvViewOpportunities);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);
    }

    private void setupClickListeners() {
        ivProfileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OrganizationDashboardActivity.this, OrganizationProfileActivity.class));
            }
        });

        btnCreateOpportunity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(OrganizationDashboardActivity.this,
                        "Create Opportunity - Coming Soon", Toast.LENGTH_SHORT).show();
            }
        });

        cvViewOpportunities.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OrganizationDashboardActivity.this, ManageOpportunitiesActivity.class));
            }
        });
    }

    private void loadDashboardData() {
        progressDialog.setMessage("Loading dashboard...");
        progressDialog.show();

        db.collection("opportunities")
                .whereEqualTo("organizationId", orgId)
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            tvActiveOpportunities.setText(String.valueOf(task.getResult().size()));
                        } else {
                            tvActiveOpportunities.setText("0");
                        }
                    }
                });

        db.collection("opportunities")
                .whereEqualTo("organizationId", orgId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            tvTotalOpportunities.setText(String.valueOf(task.getResult().size()));
                        } else {
                            tvTotalOpportunities.setText("0");
                        }
                        tvCompletionRate.setText("0%");
                    }
                });

        db.collection("opportunities")
                .whereEqualTo("organizationId", orgId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int totalVolunteers = 0;
                            for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult()) {
                                Long count = doc.getLong("approvedApplicants");
                                if (count != null) totalVolunteers += count;
                            }
                            tvTotalVolunteers.setText(String.valueOf(totalVolunteers));
                        } else {
                            tvTotalVolunteers.setText("0");
                        }
                    }
                });

        db.collection("applications")
                .whereEqualTo("organizationId", orgId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            tvPendingApplications.setText(String.valueOf(task.getResult().size()));
                        } else {
                            tvPendingApplications.setText("0");
                        }
                    }
                });

        db.collection("applications")
                .whereEqualTo("organizationId", orgId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            tvApprovedApplications.setText(String.valueOf(task.getResult().size()));
                        } else {
                            tvApprovedApplications.setText("0");
                        }
                        progressDialog.dismiss();
                    }
                });
    }
}