package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.ApplicationAdapter;
import com.error404.communityvolunteerplatform.models.Application;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ApplicationsListActivity extends AppCompatActivity {

    private RecyclerView rvApplications;
    private ApplicationAdapter adapter;
    private List<Application> applicationList;
    private ProgressBar progressBar;
    private TextView tvNoApplications;
    private FirebaseFirestore db;
    private String currentUserId;
    private String filterStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications_list);

        filterStatus = getIntent().getStringExtra("status");
        if (filterStatus == null) filterStatus = "all";

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getTitleForStatus(filterStatus));
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        rvApplications = findViewById(R.id.rvApplications);
        progressBar = findViewById(R.id.progressBar);
        tvNoApplications = findViewById(R.id.tvNoApplications);

        applicationList = new ArrayList<>();
        adapter = new ApplicationAdapter(applicationList);
        rvApplications.setLayoutManager(new LinearLayoutManager(this));
        rvApplications.setAdapter(adapter);

        loadApplications();
    }

    private String getTitleForStatus(String status) {
        switch (status) {
            case "pending": return "Sent Applications";
            case "approved": return "Approved Applications";
            case "rejected": return "Rejected Applications";
            default: return "All Applications";
        }
    }

    private void loadApplications() {
        progressBar.setVisibility(View.VISIBLE);
        
        Query query = db.collection("applications")
                .whereEqualTo("volunteerId", currentUserId);
        
        if (!"all".equals(filterStatus)) {
            query = query.whereEqualTo("status", filterStatus);
        }
        
        query.orderBy("appliedAt", Query.Direction.DESCENDING)
            .get().addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    applicationList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Application app = document.toObject(Application.class);
                        app.setApplicationId(document.getId());
                        applicationList.add(app);
                    }
                    adapter.notifyDataSetChanged();
                    tvNoApplications.setVisibility(applicationList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
