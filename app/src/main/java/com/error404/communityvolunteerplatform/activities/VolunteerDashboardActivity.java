package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.GroqRecommendationHelper;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class VolunteerDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String volunteerId;

    private TextView tvWelcome, tvHoursVolunteered, tvProjectsCompleted,
            tvBadgesEarned, tvActiveApplications, tvPendingApplications;

    private android.view.View cardAiRecommendation;
    private android.view.View pbAiRec;
    private TextView tvAiRecTitle, tvAiRecDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_dashboard);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // ── Safety check: redirect to login if user is not signed in ──
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        volunteerId = auth.getCurrentUser().getUid();

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Views
        tvWelcome            = findViewById(R.id.tvWelcome);
        tvHoursVolunteered   = findViewById(R.id.tvHoursVolunteered);
        tvProjectsCompleted  = findViewById(R.id.tvProjectsCompleted);
        tvBadgesEarned       = findViewById(R.id.tvBadgesEarned);
        tvActiveApplications = findViewById(R.id.tvActiveApplications);
        tvPendingApplications = findViewById(R.id.tvPendingApplications);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });

        cardAiRecommendation = findViewById(R.id.cardAiRecommendation);
        pbAiRec              = findViewById(R.id.pbAiRec);
        tvAiRecTitle         = findViewById(R.id.tvAiRecTitle);
        tvAiRecDescription   = findViewById(R.id.tvAiRecDescription);

        findViewById(R.id.btnSeeAllRecommendations).setOnClickListener(v -> 
                startActivity(new Intent(this, AiRecommendationsActivity.class)));

        loadVolunteerData();
        loadAiRecommendation();
    }

    private void loadAiRecommendation() {
        cardAiRecommendation.setVisibility(android.view.View.VISIBLE);
        pbAiRec.setVisibility(android.view.View.VISIBLE);
        tvAiRecTitle.setText("Loading...");
        tvAiRecDescription.setText("");

        GroqRecommendationHelper.getRecommendations(volunteerId, new GroqRecommendationHelper.OnRecommendationsListener() {
            @Override
            public void onSuccess(List<Opportunity> opportunities) {
                pbAiRec.setVisibility(android.view.View.GONE);
                if (!opportunities.isEmpty()) {
                    Opportunity topMatch = opportunities.get(0);
                    tvAiRecTitle.setText(topMatch.getTitle());
                    tvAiRecDescription.setText(topMatch.getOpportunityDescription());
                    cardAiRecommendation.setOnClickListener(v -> {
                        Intent intent = new Intent(VolunteerDashboardActivity.this, OpportunityDetailsActivity.class);
                        intent.putExtra("opportunityId", topMatch.getOpportunityId());
                        startActivity(intent);
                    });
                } else {
                    cardAiRecommendation.setVisibility(android.view.View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                cardAiRecommendation.setVisibility(android.view.View.GONE);
            }
        });
    }

    private void loadVolunteerData() {
        // Load volunteer profile
        db.collection("volunteers").document(volunteerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firstName = doc.getString("firstName");
                        tvWelcome.setText("Welcome, " + (firstName != null ? firstName : "Volunteer"));

                        Double hours = doc.getDouble("totalHours");
                        tvHoursVolunteered.setText(hours != null ? String.valueOf(hours.intValue()) : "0");

                        Long projects = doc.getLong("projectsCompleted");
                        tvProjectsCompleted.setText(projects != null ? String.valueOf(projects) : "0");

                        java.util.List<?> badges = (java.util.List<?>) doc.get("badgeIds");
                        tvBadgesEarned.setText(badges != null ? String.valueOf(badges.size()) : "0");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Active (approved) applications
        db.collection("applications")
                .whereEqualTo("volunteerId", volunteerId)
                .whereEqualTo("status", "approved")
                .whereEqualTo("withdrawnStatus", false)
                .get()
                .addOnSuccessListener(snap ->
                        tvActiveApplications.setText("Active Applications: " + snap.size()))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load active applications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Pending applications
        db.collection("applications")
                .whereEqualTo("volunteerId", volunteerId)
                .whereEqualTo("status", "pending")
                .whereEqualTo("withdrawnStatus", false)
                .get()
                .addOnSuccessListener(snap ->
                        tvPendingApplications.setText("Pending Applications: " + snap.size()))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load pending applications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_browse_opportunities) {
            startActivity(new Intent(this, BrowseOpportunitiesActivity.class));
        } else if (id == R.id.nav_ai_recommendations) {
            startActivity(new Intent(this, AiRecommendationsActivity.class));
        } else if (id == R.id.nav_my_applications) {
            // TODO: startActivity(new Intent(this, MyApplicationsActivity.class));
            Toast.makeText(this, "My Applications", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_passport) {
            startActivity(new Intent(this, PassportActivity.class));
        } else if (id == R.id.nav_chats) {
            // TODO: startActivity(new Intent(this, ChatsActivity.class));
            Toast.makeText(this, "Chats", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_profile) {
            // TODO: startActivity(new Intent(this, VolunteerProfileActivity.class));
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


}