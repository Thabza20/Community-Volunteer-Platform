package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.GroqRecommendationHelper;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AiRecommendationsActivity extends AppCompatActivity {

    private RecyclerView rvRecommendations;
    private ProgressBar progressBar;
    private View errorLayout;
    private TextView tvErrorMessage;
    private RecommendationAdapter adapter;
    private List<Opportunity> recommendations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_recommendations);

        rvRecommendations = findViewById(R.id.rvRecommendations);
        progressBar = findViewById(R.id.progressBar);
        errorLayout = findViewById(R.id.errorLayout);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            v.setEnabled(false);
            v.postDelayed(() -> {
                v.setEnabled(true);
            }, 5000);
            fetchRecommendations();
        });

        rvRecommendations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecommendationAdapter(recommendations);
        rvRecommendations.setAdapter(adapter);

        fetchRecommendations();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void fetchRecommendations() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        progressBar.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        rvRecommendations.setVisibility(View.GONE);

        GroqRecommendationHelper.getRecommendations(uid, new GroqRecommendationHelper.OnRecommendationsListener() {
            @Override
            public void onSuccess(List<Opportunity> opportunities) {
                progressBar.setVisibility(View.GONE);
                if (opportunities.isEmpty()) {
                    showError("No recommendations found at this time.");
                } else {
                    recommendations.clear();
                    recommendations.addAll(opportunities);
                    adapter.notifyDataSetChanged();
                    rvRecommendations.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                showError(message);
            }
        });
    }

    private void showError(String message) {
        errorLayout.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);
    }

    private class RecommendationAdapter extends RecyclerView.Adapter<RecommendationViewHolder> {
        private final List<Opportunity> list;

        RecommendationAdapter(List<Opportunity> list) { this.list = list; }

        @NonNull
        @Override
        public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_recommendation, parent, false);
            return new RecommendationViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
            Opportunity opp = list.get(position);
            holder.tvTitle.setText(opp.getTitle());
            holder.tvDescription.setText(opp.getOpportunityDescription());
            holder.tvLocation.setText(opp.getLocation()); 

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(AiRecommendationsActivity.this, OpportunityDetailsActivity.class);
                intent.putExtra("OPPORTUNITY_ID", opp.getOpportunityId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }
    }

    private static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvLocation;
        RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }
    }
}
