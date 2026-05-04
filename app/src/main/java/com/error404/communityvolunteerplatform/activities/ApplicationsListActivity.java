package com.error404.communityvolunteerplatform.activities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.ApplicationAdapter;
import com.error404.communityvolunteerplatform.models.Application;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationsListActivity extends AppCompatActivity implements ApplicationAdapter.OnWithdrawClickListener {

    private RecyclerView rvApplications;
    private ApplicationAdapter adapter;
    private List<Application> allApplications = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvNoApplications;
    private TabLayout tabLayout;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration registration;
    private Map<String, String> previousStatuses = new HashMap<>();
    private boolean isFirstLoad = true;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        rvApplications = findViewById(R.id.rvApplications);
        progressBar = findViewById(R.id.progressBar);
        tvNoApplications = findViewById(R.id.tvNoApplications);
        tabLayout = findViewById(R.id.tabLayout);

        String initialStatus = getIntent().getStringExtra("status");
        if ("pending".equals(initialStatus)) tabLayout.getTabAt(1).select();
        else if ("approved".equals(initialStatus)) tabLayout.getTabAt(2).select();
        else if ("rejected".equals(initialStatus)) tabLayout.getTabAt(3).select();

        adapter = new ApplicationAdapter(new ArrayList<>(), this);
        rvApplications.setLayoutManager(new LinearLayoutManager(this));
        rvApplications.setAdapter(adapter);

        setupTabs();
        listenToApplications();
        checkNotificationPermission();
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterList(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void listenToApplications() {
        progressBar.setVisibility(View.VISIBLE);
        registration = db.collection("applications")
                .whereEqualTo("volunteerId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Application app = dc.getDocument().toObject(Application.class);
                            app.setApplicationId(dc.getDocument().getId());

                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                // Add and sort later or find insertion point. 
                                // For simplicity with real-time, we'll refresh the whole list view.
                                allApplications.add(app);
                                previousStatuses.put(app.getApplicationId(), app.getStatus());
                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                // Find and update
                                for (int i = 0; i < allApplications.size(); i++) {
                                    if (allApplications.get(i).getApplicationId().equals(app.getApplicationId())) {
                                        allApplications.set(i, app);
                                        break;
                                    }
                                }
                                checkStatusChange(app);
                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                                allApplications.removeIf(a -> a.getApplicationId().equals(app.getApplicationId()));
                                previousStatuses.remove(app.getApplicationId());
                            }
                        }
                        
                        // Local Sort by date (descending)
                        allApplications.sort((a1, a2) -> {
                            if (a1.getAppliedAt() == null || a2.getAppliedAt() == null) return 0;
                            return a2.getAppliedAt().compareTo(a1.getAppliedAt());
                        });

                        isFirstLoad = false;
                        filterList(tabLayout.getSelectedTabPosition());
                    }
                });
    }

    private void checkStatusChange(Application app) {
        String oldStatus = previousStatuses.get(app.getApplicationId());
        String newStatus = app.getStatus();

        if (!isFirstLoad && oldStatus != null && !oldStatus.equals(newStatus)) {
            fireLocalNotification("Application Update", "Your application status is now: " + newStatus);
        }
        previousStatuses.put(app.getApplicationId(), newStatus);
    }

    private void filterList(int tabPosition) {
        List<Application> filtered = new ArrayList<>();
        String statusFilter = "";
        switch (tabPosition) {
            case 1: statusFilter = "pending"; break;
            case 2: statusFilter = "approved"; break;
            case 3: statusFilter = "rejected"; break;
        }

        if (statusFilter.isEmpty()) {
            filtered.addAll(allApplications);
        } else {
            for (Application app : allApplications) {
                if (statusFilter.equals(app.getStatus())) {
                    filtered.add(app);
                }
            }
        }

        adapter.updateList(filtered);
        tvNoApplications.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onWithdrawClick(Application application) {
        new AlertDialog.Builder(this)
                .setTitle("Withdraw Application")
                .setMessage("Are you sure you want to withdraw this application?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "withdrawn");
                    updates.put("withdrawnStatus", true);
                    updates.put("updatedAt", Timestamp.now());

                    db.collection("applications").document(application.getApplicationId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Withdrawn successfully", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void fireLocalNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "cvp_channel", "Application Updates", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "cvp_channel")
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }
}
