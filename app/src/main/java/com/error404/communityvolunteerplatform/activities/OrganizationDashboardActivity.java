package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.List;

public class OrganizationDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
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
            getSupportActionBar().setTitle("Community Volunteer Platform");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // 2. Setup Drawer and Sidebar (Hamburger Menu)
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
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
        getWindow().getDecorView().post(() -> {
            fetchOrganizationName();
        });
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(this::loadDashboardStats, 1000);

        // Handle back press to close drawer if open
        OnBackPressedCallback callback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                callback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                callback.setEnabled(false);
            }
        });
    }

    private void fetchOrganizationName() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Log.d("OrgDashboard", "User Doc: " + documentSnapshot.getData());
                            String orgName = documentSnapshot.getString("orgName");
                            if (orgName == null || orgName.isEmpty()) {
                                orgName = documentSnapshot.getString("name");
                            }
                            if (orgName == null || orgName.isEmpty()) {
                                orgName = documentSnapshot.getString("organizationName");
                            }

                            String logoUrl = documentSnapshot.getString("logoUrl");

                            if (orgName != null && !orgName.isEmpty()) {
                                updateOrgUI(orgName, logoUrl);
                            } else {
                                fetchFromOrganisationsCollection(uid);
                            }
                        } else {
                            fetchFromOrganisationsCollection(uid);
                        }
                    }).addOnFailureListener(e -> fetchFromOrganisationsCollection(uid));
        }
    }

    private void fetchFromOrganisationsCollection(String uid) {
        db.collection("organisations").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("OrgDashboard", "Org Doc: " + documentSnapshot.getData());
                        String orgName = documentSnapshot.getString("orgName");
                        if (orgName == null || orgName.isEmpty()) {
                            orgName = documentSnapshot.getString("name");
                        }
                        String logoUrl = documentSnapshot.getString("logoUrl");
                        updateOrgUI(orgName, logoUrl);
                    } else {
                        tvWelcomeMessage.setText("Welcome");
                    }
                })
                .addOnFailureListener(e -> tvWelcomeMessage.setText("Welcome"));
    }

    private void updateOrgUI(String orgName, String logoUrl) {
        if (orgName != null && !orgName.isEmpty()) {
            tvWelcomeMessage.setText("Welcome, " + orgName);
        } else {
            tvWelcomeMessage.setText("Welcome");
        }

        NavigationView navigationView = findViewById(R.id.navigationView);
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) {
            headerView = navigationView.inflateHeaderView(R.layout.nav_header);
        }

        TextView tvNavOrgName = headerView.findViewById(R.id.tvNavOrgName);
        ImageView ivNavProfilePic = headerView.findViewById(R.id.ivNavProfilePic);

        if (tvNavOrgName != null) {
            tvNavOrgName.setText(orgName != null ? orgName : "Organization");
        }

        if (ivNavProfilePic != null) {
            if (logoUrl != null && !logoUrl.isEmpty()) {
                Glide.with(this).load(logoUrl).placeholder(R.drawable.ic_default_avatar).into(ivNavProfilePic);
            } else {
                ivNavProfilePic.setImageResource(R.drawable.ic_default_avatar);
            }
        }

        // CONNECTED: Navigates to OrganizationProfileActivity when the header is clicked
        if (headerView != null) {
            headerView.setOnClickListener(v -> {
                startActivity(new Intent(OrganizationDashboardActivity.this, OrganizationProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
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

    private void handleGiveBadges() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("opportunities")
                .whereEqualTo("orgId", user.getUid())
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "You have no active events to scan for.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> titles = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        titles.add(doc.getString("title"));
                        ids.add(doc.getId());
                    }

                    if (ids.size() == 1) {
                        openScanner(ids.get(0));
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Select Event")
                                .setItems(titles.toArray(new String[0]), (dialog, which) -> {
                                    openScanner(ids.get(which));
                                })
                                .show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching events", Toast.LENGTH_SHORT).show());
    }

    private void openScanner(String opportunityId) {
        Intent intent = new Intent(this, GiveBadgesActivity.class);
        intent.putExtra("opportunityId", opportunityId);
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_create_opp) {
            startActivity(new Intent(this, CreateEventActivity.class));
        } else if (id == R.id.nav_manage_opps) {
            startActivity(new Intent(this, ManageOpportunitiesActivity.class));
        } else if (id == R.id.nav_give_badges) {
            handleGiveBadges();
        } else if (id == R.id.nav_chats) {
            startActivity(new Intent(this, RecentChatsActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, OrganizationProfileActivity.class));
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
}