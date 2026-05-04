package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.error404.communityvolunteerplatform.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.concurrent.atomic.AtomicInteger;

public class OrganizationDashboardActivity extends AppCompatActivity {

    private TextView tvActiveOpportunities, tvTotalVolunteers, tvTotalOpportunities,
            tvPendingApplications, tvApprovedApplications, tvCompletionRate, tvNotificationBadge;

    private Button btnCreateOpportunity;
    private CardView cvViewOpportunities;
    private ImageView ivProfileIcon, ivChatIcon, ivNotificationBell;
    private ProgressBar pbDashboard;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String orgId;
    private com.google.firebase.firestore.ListenerRegistration notificationListener;

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

        initializeViews();
        setupClickListeners();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
        listenToNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    private void initializeViews() {
        tvActiveOpportunities = findViewById(R.id.tvActiveOpportunities);
        tvTotalVolunteers = findViewById(R.id.tvTotalVolunteers);
        tvTotalOpportunities = findViewById(R.id.tvTotalOpportunities);
        tvPendingApplications = findViewById(R.id.tvPendingApplications);
        tvApprovedApplications = findViewById(R.id.tvApprovedApplications);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        btnCreateOpportunity = findViewById(R.id.btnCreateOpportunity);
        cvViewOpportunities = findViewById(R.id.cvViewOpportunities);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);
        ivChatIcon = findViewById(R.id.ivChatIcon);
        ivNotificationBell = findViewById(R.id.ivNotificationBell);
        pbDashboard = findViewById(R.id.pbDashboard);
    }

    private void listenToNotifications() {
        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", orgId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        int unreadCount = 0;
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            Boolean isRead = doc.getBoolean("read");
                            if (isRead != null && !isRead) {
                                unreadCount++;
                            }
                        }

                        if (unreadCount > 0) {
                            tvNotificationBadge.setText(String.valueOf(unreadCount));
                            tvNotificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            tvNotificationBadge.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void setupClickListeners() {
        ivProfileIcon.setOnClickListener(v ->
                startActivity(new Intent(OrganizationDashboardActivity.this, OrganizationProfileActivity.class))
        );

        ivChatIcon.setOnClickListener(v ->
                startActivity(new Intent(OrganizationDashboardActivity.this, RecentChatsActivity.class))
        );

        ivNotificationBell.setOnClickListener(v ->
                startActivity(new Intent(OrganizationDashboardActivity.this, NotificationsActivity.class))
        );

        btnCreateOpportunity.setOnClickListener(v ->
                startActivity(new Intent(OrganizationDashboardActivity.this, CreateEventActivity.class))
        );


        cvViewOpportunities.setOnClickListener(v ->
                startActivity(new Intent(OrganizationDashboardActivity.this, ManageOpportunitiesActivity.class))
        );
    }

    private void loadDashboardData() {
        pbDashboard.setVisibility(View.VISIBLE);
        AtomicInteger queriesRemaining = new AtomicInteger(4);

        Runnable checkComplete = () -> {
            if (queriesRemaining.decrementAndGet() == 0) {
                pbDashboard.setVisibility(View.GONE);
            }
        };

        // Query 1: active opportunities → tvActiveOpportunities
        db.collection("opportunities")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        tvActiveOpportunities.setText(String.valueOf(task.getResult().size()));
                    } else {
                        tvActiveOpportunities.setText(getString(R.string.default_zero));
                    }
                    checkComplete.run();
                });

        // Query 2: all opportunities → tvTotalOpportunities + tvTotalVolunteers (sum slotsFilled) + tvCompletionRate (count status=="completed")
        db.collection("opportunities")
                .whereEqualTo("orgId", orgId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int total = task.getResult().size();
                        int volunteers = 0;
                        int completed = 0;
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Long filled = doc.getLong("slotsFilled");
                            if (filled != null) volunteers += filled;

                            if ("completed".equals(doc.getString("status"))) {
                                completed++;
                            }
                        }
                        tvTotalOpportunities.setText(String.valueOf(total));
                        tvTotalVolunteers.setText(String.valueOf(volunteers));
                        if (total > 0) {
                            int rate = (completed * 100) / total;
                            tvCompletionRate.setText(getString(R.string.percent_format, rate));
                        } else {
                            tvCompletionRate.setText(getString(R.string.default_zero_percent));
                        }
                    } else {
                        tvTotalOpportunities.setText(getString(R.string.default_zero));
                        tvTotalVolunteers.setText(getString(R.string.default_zero));
                        tvCompletionRate.setText(getString(R.string.default_zero_percent));
                    }
                    checkComplete.run();
                });

        // Query 3: pending applications → tvPendingApplications
        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        tvPendingApplications.setText(String.valueOf(task.getResult().size()));
                    } else {
                        tvPendingApplications.setText(getString(R.string.default_zero));
                    }
                    checkComplete.run();
                });

        // Query 4: approved applications → tvApprovedApplications
        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        tvApprovedApplications.setText(String.valueOf(task.getResult().size()));
                    } else {
                        tvApprovedApplications.setText(getString(R.string.default_zero));
                    }
                    checkComplete.run();
                });
    }
}
