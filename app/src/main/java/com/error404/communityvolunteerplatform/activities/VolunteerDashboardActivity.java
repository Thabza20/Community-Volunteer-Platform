package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class VolunteerDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String volunteerId;

    private TextView tvWelcome, tvHoursVolunteered, tvProjectsCompleted, tvBadgesEarned;
    private TextView tvPendingCount, tvApprovedCount, tvRejectedCount;
    private LineChart activityChart;

    private View cardAiRecommendation;
    private View pbAiRec;
    private TextView tvAiRecTitle, tvAiRecDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        volunteerId = auth.getCurrentUser().getUid();

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

        initViews();
        setupClickListeners();
        
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

        loadVolunteerData();
        loadApplicationStats();
        loadAiRecommendation();
        setupChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVolunteerData();
        loadApplicationStats();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvHoursVolunteered = findViewById(R.id.tvHoursVolunteered);
        tvProjectsCompleted = findViewById(R.id.tvProjectsCompleted);
        tvBadgesEarned = findViewById(R.id.tvBadgesEarned);
        
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        tvRejectedCount = findViewById(R.id.tvRejectedCount);
        
        activityChart = findViewById(R.id.activityChart);
        
        cardAiRecommendation = findViewById(R.id.cardAiRecommendation);
        pbAiRec = findViewById(R.id.pbAiRec);
        tvAiRecTitle = findViewById(R.id.tvAiRecTitle);
        tvAiRecDescription = findViewById(R.id.tvAiRecDescription);
    }

    private void setupClickListeners() {
        findViewById(R.id.cardPending).setOnClickListener(v -> openApplicationsList("pending"));
        findViewById(R.id.cardApproved).setOnClickListener(v -> openApplicationsList("approved"));
        findViewById(R.id.cardRejected).setOnClickListener(v -> openApplicationsList("rejected"));
        
        findViewById(R.id.btnSeeAllRecommendations).setOnClickListener(v -> 
                startActivity(new Intent(this, AiRecommendationsActivity.class)));
    }

    private void openApplicationsList(String status) {
        Intent intent = new Intent(this, ApplicationsListActivity.class);
        intent.putExtra("status", status);
        startActivity(intent);
    }

    private void setupChart() {
        List<Entry> entries = new ArrayList<>();
        // Dummy data for now: Recent activity over 5 days
        entries.add(new Entry(0, 2));
        entries.add(new Entry(1, 4));
        entries.add(new Entry(2, 1));
        entries.add(new Entry(3, 5));
        entries.add(new Entry(4, 3));

        LineDataSet dataSet = new LineDataSet(entries, "Hours Volunteered");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.BLUE);
        dataSet.setFillAlpha(75);

        LineData lineData = new LineData(dataSet);
        activityChart.setData(lineData);
        
        activityChart.getDescription().setEnabled(false);
        activityChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        activityChart.getXAxis().setDrawGridLines(false);
        
        String[] days = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri"};
        activityChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        activityChart.getAxisRight().setEnabled(false);
        
        activityChart.invalidate(); 
    }

    private void loadAiRecommendation() {
        cardAiRecommendation.setVisibility(View.VISIBLE);
        pbAiRec.setVisibility(View.VISIBLE);
        tvAiRecTitle.setText("Loading...");
        tvAiRecDescription.setText("");

        GroqRecommendationHelper.getRecommendations(volunteerId, new GroqRecommendationHelper.OnRecommendationsListener() {
            @Override
            public void onSuccess(List<Opportunity> opportunities) {
                pbAiRec.setVisibility(View.GONE);
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
                    cardAiRecommendation.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                cardAiRecommendation.setVisibility(View.GONE);
            }
        });
    }

    private void loadVolunteerData() {
        db.collection("volunteers").document(volunteerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firstName = doc.getString("firstName");
                        tvWelcome.setText("Welcome, " + (firstName != null ? firstName : "Volunteer"));

                        Double hours = doc.getDouble("totalHours");
                        tvHoursVolunteered.setText(hours != null ? String.valueOf(hours.intValue()) : "0");

                        Long projects = doc.getLong("projectsCompleted");
                        tvProjectsCompleted.setText(projects != null ? String.valueOf(projects) : "0");

                        List<?> badges = (List<?>) doc.get("badgeIds");
                        tvBadgesEarned.setText(badges != null ? String.valueOf(badges.size()) : "0");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadApplicationStats() {
        // Pending (Sent)
        db.collection("applications")
                .whereEqualTo("volunteerId", volunteerId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> tvPendingCount.setText(String.valueOf(snap.size())));

        // Approved
        db.collection("applications")
                .whereEqualTo("volunteerId", volunteerId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(snap -> tvApprovedCount.setText(String.valueOf(snap.size())));

        // Rejected
        db.collection("applications")
                .whereEqualTo("volunteerId", volunteerId)
                .whereEqualTo("status", "rejected")
                .get()
                .addOnSuccessListener(snap -> tvRejectedCount.setText(String.valueOf(snap.size())));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_browse_opportunities) {
            startActivity(new Intent(this, BrowseOpportunitiesActivity.class));
        } else if (id == R.id.nav_ai_recommendations) {
            startActivity(new Intent(this, AiRecommendationsActivity.class));
        } else if (id == R.id.nav_my_applications) {
            openApplicationsList("all");
        } else if (id == R.id.nav_passport) {
            startActivity(new Intent(this, PassportActivity.class));
        } else if (id == R.id.nav_chats) {
            startActivity(new Intent(this, RecentChatsActivity.class));
        } else if (id == R.id.nav_profile) {
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
