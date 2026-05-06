package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.error404.communityvolunteerplatform.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class OrganizationDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView tvWelcomeMessage;
    private ImageView ivNotifications;

    // Stat Tile TextViews
    private TextView tvActiveOpportunities, tvTotalVolunteers, tvTotalOpportunities;
    private TextView tvPendingApplications, tvApprovedApplications, tvCompletionRate;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_org_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // 2. Setup Drawer and Sidebar (Hamburger Menu)
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));

        // 3. Initialize Header Views
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        ivNotifications = findViewById(R.id.ivNotifications);

        ivNotifications.setOnClickListener(v -> {
            startActivity(new Intent(OrganizationDashboardActivity.this, NotificationsActivity.class));
        });

        // 4. Initialize Stat Tile Views & Buttons
        tvActiveOpportunities = findViewById(R.id.tvActiveOpportunities);
        tvTotalVolunteers = findViewById(R.id.tvTotalVolunteers);
        tvTotalOpportunities = findViewById(R.id.tvTotalOpportunities);
        tvPendingApplications = findViewById(R.id.tvPendingApplications);
        tvApprovedApplications = findViewById(R.id.tvApprovedApplications);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);

        // LINK THE ANALYTICS BUTTON
        Button btnViewAnalytics = findViewById(R.id.btnViewAnalytics);
        btnViewAnalytics.setOnClickListener(v -> {
            startActivity(new Intent(OrganizationDashboardActivity.this, OrgAnalyticsActivity.class));
        });

        // 5. Fetch Data from Firebase
        fetchOrganizationName();
        loadDashboardStats();
    }

    private void fetchOrganizationName() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String orgName = documentSnapshot.getString("name");
                            if (orgName != null && !orgName.isEmpty()) {
                                tvWelcomeMessage.setText("Welcome, " + orgName);
                            } else {
                                tvWelcomeMessage.setText("Welcome");
                            }
                        }
                    }).addOnFailureListener(e -> tvWelcomeMessage.setText("Welcome"));
        }
    }

    private void loadDashboardStats() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String orgId = currentUser.getUid();

        // 1. Fetch Opportunities Stats
        db.collection("opportunities")
                .whereEqualTo("orgId", orgId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalOpps = queryDocumentSnapshots.size();

                    tvTotalOpportunities.setText(String.valueOf(totalOpps));
                    tvActiveOpportunities.setText(String.valueOf(totalOpps));
                });

        // 2. Fetch Applications Stats
        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int pending = 0;
                    int approved = 0;
                    int totalApps = queryDocumentSnapshots.size();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if (status != null) {
                            if (status.equalsIgnoreCase("pending")) {
                                pending++;
                            } else if (status.equalsIgnoreCase("approved") || status.equalsIgnoreCase("accepted")) {
                                approved++;
                            }
                        }
                    }

                    tvPendingApplications.setText(String.valueOf(pending));
                    tvApprovedApplications.setText(String.valueOf(approved));

                    tvTotalVolunteers.setText(String.valueOf(approved));

                    // Calculate completion rate
                    if (totalApps > 0) {
                        int completion = (approved * 100) / totalApps;
                        tvCompletionRate.setText(completion + "%");
                    } else {
                        tvCompletionRate.setText("0%");
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_create_opp) {
            startActivity(new Intent(this, CreateEventActivity.class));
        } else if (id == R.id.nav_manage_opps) {
            startActivity(new Intent(this, ManageOpportunitiesActivity.class));
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}