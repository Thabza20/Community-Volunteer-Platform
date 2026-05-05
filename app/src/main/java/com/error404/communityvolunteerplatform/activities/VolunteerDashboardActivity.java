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
import java.util.Map;

public class VolunteerDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String volunteerId;

    private TextView tvWelcome, tvHoursVolunteered, tvProjectsCompleted, tvBadgesEarned;
    private TextView tvPendingCount, tvApprovedCount, tvRejectedCount;
    private TextView tvTotalApplications, tvAppBreakdown, tvNotificationBadge;
    private TextView tvRecentNotifyTitle, tvRecentNotifyBody;
    private View ibNotificationBell, cardRecentNotification, vRecentNotifyDot;
    private com.google.firebase.firestore.ListenerRegistration notificationListener;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVolunteerData();
        loadApplicationStats();
        listenToNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    private void listenToNotifications() {
        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", volunteerId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        int unreadCount = 0;
                        Map<String, Object> latestNotification = null;
                        com.google.firebase.Timestamp latestTimestamp = null;

                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            Boolean isRead = doc.getBoolean("read");
                            if (isRead != null && !isRead) {
                                unreadCount++;
                                
                                com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                                if (latestTimestamp == null || (ts != null && ts.compareTo(latestTimestamp) > 0)) {
                                    latestTimestamp = ts;
                                    latestNotification = doc.getData();
                                }
                            }
                        }
                        
                        // Update badge
                        if (unreadCount > 0) {
                            tvNotificationBadge.setText(String.valueOf(unreadCount));
                            tvNotificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            tvNotificationBadge.setVisibility(View.GONE);
                        }

                        // Update recent notification card
                        if (latestNotification != null) {
                            cardRecentNotification.setVisibility(View.VISIBLE);
                            tvRecentNotifyTitle.setText((String) latestNotification.get("title"));
                            tvRecentNotifyBody.setText((String) latestNotification.get("body"));
                            
                            String type = (String) latestNotification.get("type");
                            int dotColor = Color.GRAY;
                            if (type != null) {
                                switch (type) {
                                    case "application_approved": dotColor = Color.parseColor("#10B981"); break;
                                    case "application_rejected": dotColor = Color.parseColor("#EF4444"); break;
                                    case "application_pending": dotColor = Color.parseColor("#F59E0B"); break;
                                    case "chat_message": dotColor = Color.parseColor("#3B82F6"); break;
                                }
                            }
                            vRecentNotifyDot.getBackground().setTint(dotColor);
                        } else {
                            cardRecentNotification.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvHoursVolunteered = findViewById(R.id.tvHoursVolunteered);
        tvProjectsCompleted = findViewById(R.id.tvProjectsCompleted);
        tvBadgesEarned = findViewById(R.id.tvBadgesEarned);
        
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        tvRejectedCount = findViewById(R.id.tvRejectedCount);
        tvTotalApplications = findViewById(R.id.tvTotalApplications);
        tvAppBreakdown = findViewById(R.id.tvAppBreakdown);
        
        ibNotificationBell = findViewById(R.id.ibNotificationBell);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        
        cardRecentNotification = findViewById(R.id.cardRecentNotification);
        vRecentNotifyDot = findViewById(R.id.vRecentNotifyDot);
        tvRecentNotifyTitle = findViewById(R.id.tvRecentNotifyTitle);
        tvRecentNotifyBody = findViewById(R.id.tvRecentNotifyBody);
        
        cardAiRecommendation = findViewById(R.id.cardAiRecommendation);
        pbAiRec = findViewById(R.id.pbAiRec);
        tvAiRecTitle = findViewById(R.id.tvAiRecTitle);
        tvAiRecDescription = findViewById(R.id.tvAiRecDescription);
    }

    private void setupClickListeners() {
        findViewById(R.id.cardPending).setOnClickListener(v -> openApplicationsList("pending"));
        findViewById(R.id.cardApproved).setOnClickListener(v -> openApplicationsList("approved"));
        findViewById(R.id.cardRejected).setOnClickListener(v -> openApplicationsList("rejected"));
        findViewById(R.id.cardMyApplications).setOnClickListener(v -> openApplicationsList("all"));
        
        ibNotificationBell.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        cardRecentNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        
        findViewById(R.id.btnSeeAllRecommendations).setOnClickListener(v -> 
                startActivity(new Intent(this, AiRecommendationsActivity.class)));

        findViewById(R.id.fab_ai_assistant).setOnClickListener(v -> 
                startActivity(new Intent(this, AiChatbotActivity.class)));
    }

    private void openApplicationsList(String status) {
        Intent intent = new Intent(this, ApplicationsListActivity.class);
        intent.putExtra("status", status);
        startActivity(intent);
    }

    private void loadAiRecommendation() {
        cardAiRecommendation.setVisibility(View.VISIBLE);
        pbAiRec.setVisibility(View.VISIBLE);
        tvAiRecTitle.setText("Loading...");
        tvAiRecDescription.setText("");

        GroqRecommendationHelper.getRecommendations(volunteerId, new GroqRecommendationHelper.OnRecommendationsListener() {
            @Override
            public void onSuccess(List<Opportunity> opportunities, List<String> reasons) {
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
        db.collection("applications")
                .whereEqualTo("volunteerId", volunteerId)
                .get()
                .addOnSuccessListener(snap -> {
                    int total = snap.size();
                    int pending = 0;
                    int approved = 0;
                    int rejected = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        String status = doc.getString("status");
                        if ("pending".equals(status)) pending++;
                        else if ("approved".equals(status)) approved++;
                        else if ("rejected".equals(status)) rejected++;
                    }

                    tvPendingCount.setText(String.valueOf(pending));
                    tvApprovedCount.setText(String.valueOf(approved));
                    tvRejectedCount.setText(String.valueOf(rejected));
                    
                    tvTotalApplications.setText(total + (total == 1 ? " Application" : " Applications"));
                    tvAppBreakdown.setText(pending + " pending, " + approved + " approved");
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_browse_opportunities) {
            startActivity(new Intent(this, BrowseOpportunitiesActivity.class));
        } else if (id == R.id.nav_ai_recommendations) {
            startActivity(new Intent(this, AiRecommendationsActivity.class));
        } else if (id == R.id.nav_skills_gap) {
            startActivity(new Intent(this, SkillsGapActivity.class));
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
