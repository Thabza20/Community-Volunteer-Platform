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
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class VolunteerDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String volunteerId;

    private TextView tvWelcome, tvHoursVolunteered, tvProjectsCompleted,
            tvBadgesEarned, tvActiveApplications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_dashboard);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // ── Safety check: redirect to login if user is not signed in ──
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
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

        // ── Replace deprecated onBackPressed() ──
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Let system handle normal back press
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        loadVolunteerData();
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

        // Active (pending/approved) applications
        // ⚠️ This query requires a composite index in Firestore (see note below)
        db.collection("applications")
                .whereEqualTo("volunteerId", volunteerId)
                .whereIn("status", java.util.Arrays.asList("pending", "approved"))
                .whereEqualTo("withdrawnStatus", false)
                .get()
                .addOnSuccessListener(snap ->
                        tvActiveApplications.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load applications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_browse_opportunities) {
            startActivity(new Intent(this, BrowseOpportunitiesActivity.class));
        } else if (id == R.id.nav_my_applications) {
            // TODO: startActivity(new Intent(this, MyApplicationsActivity.class));
            Toast.makeText(this, "My Applications", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_passport) {
            // TODO: startActivity(new Intent(this, PassportActivity.class));
            Toast.makeText(this, "My Passport", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_chats) {
            // TODO: startActivity(new Intent(this, ChatsActivity.class));
            Toast.makeText(this, "Chats", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_profile) {
            // TODO: startActivity(new Intent(this, VolunteerProfileActivity.class));
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            auth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


}