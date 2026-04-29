package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.OpportunityAdapter;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminNewOpportunitiesActivity extends AppCompatActivity {

    private RecyclerView rvOpps;
    private OpportunityAdapter adapter;
    private List<Opportunity> oppList;
    private ProgressBar progressBar;
    private TextView tvNoOpps;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications_list); // Reusing similar layout

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Pending Opportunities");
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();

        rvOpps = findViewById(R.id.rvApplications);
        progressBar = findViewById(R.id.progressBar);
        tvNoOpps = findViewById(R.id.tvNoApplications);

        oppList = new ArrayList<>();
        adapter = new OpportunityAdapter(oppList, this::showApprovalDialog);

        rvOpps.setLayoutManager(new LinearLayoutManager(this));
        rvOpps.setAdapter(adapter);

        loadPendingOpportunities();
    }

    private void loadPendingOpportunities() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("opportunities")
                .whereEqualTo("status", Opportunity.STATUS_PENDING_APPROVAL)
                .get()
                .addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                oppList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Opportunity opp = document.toObject(Opportunity.class);
                    if (opp.getOpportunityId() == null) opp.setOpportunityId(document.getId());
                    oppList.add(opp);
                }
                adapter.notifyDataSetChanged();
                tvNoOpps.setVisibility(oppList.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                Toast.makeText(this, "Failed to load opportunities", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showApprovalDialog(Opportunity opp) {
        new AlertDialog.Builder(this)
                .setTitle("Approve Opportunity")
                .setMessage("Do you want to approve '" + opp.getTitle() + "'?")
                .setPositiveButton("Approve", (dialog, which) -> updateStatus(opp, Opportunity.STATUS_ACTIVE))
                .setNegativeButton("Reject", (dialog, which) -> updateStatus(opp, "rejected"))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void updateStatus(Opportunity opp, String newStatus) {
        db.collection("opportunities").document(opp.getOpportunityId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Opportunity updated", Toast.LENGTH_SHORT).show();
                    loadPendingOpportunities();
                });
    }
}
