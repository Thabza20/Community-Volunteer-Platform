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
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    private TextView tvNewOppsCount, tvActiveProgramsCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

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

        tvNewOppsCount = findViewById(R.id.tvNewOppsCount);
        tvActiveProgramsCount = findViewById(R.id.tvActiveProgramsCount);

        findViewById(R.id.cardNewOpportunities).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminNewOpportunitiesActivity.class));
        });

        findViewById(R.id.cardTrackPrograms).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminTrackProgramsActivity.class));
        });

        loadStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        // Count pending opportunities
        db.collection("opportunities")
                .whereEqualTo("status", Opportunity.STATUS_PENDING_APPROVAL)
                .get()
                .addOnSuccessListener(snap -> tvNewOppsCount.setText("Pending approval: " + snap.size()));

        // Count active opportunities
        db.collection("opportunities")
                .whereEqualTo("status", Opportunity.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snap -> tvActiveProgramsCount.setText("Active programs: " + snap.size()));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_admin_dashboard) {
            // Already here
        } else if (id == R.id.nav_manage_users) {
            startActivity(new Intent(this, AdminManageUsersActivity.class));
        } else if (id == R.id.nav_manage_orgs) {
            startActivity(new Intent(this, AdminManageOrgsActivity.class));
        } else if (id == R.id.nav_blocked_users) {
            startActivity(new Intent(this, AdminBlockedUsersActivity.class));
        } else if (id == R.id.nav_admin_chats) {
            startActivity(new Intent(this, RecentChatsActivity.class));
        } else if (id == R.id.nav_admin_logout) {
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
