package com.error404.communityvolunteerplatform.activities;

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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SkillsGapActivity extends AppCompatActivity {

    private ChipGroup cgCurrentSkills;
    private RecyclerView rvSkillsGap;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private List<String> missingSkills = new ArrayList<>();
    private List<String> missingReasons = new ArrayList<>();
    private SkillGapAdapter adapter;
    private FirebaseFirestore db;
    private String volunteerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skills_gap);

        db = FirebaseFirestore.getInstance();
        volunteerId = FirebaseAuth.getInstance().getUid();

        initViews();
        setupToolbar();
        
        if (volunteerId != null) {
            loadCurrentSkills();
            fetchSkillsGap();
        }
    }

    private void initViews() {
        cgCurrentSkills = findViewById(R.id.cgCurrentSkills);
        rvSkillsGap = findViewById(R.id.rvSkillsGap);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvSkillsGap.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SkillGapAdapter();
        rvSkillsGap.setAdapter(adapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadCurrentSkills() {
        db.collection("volunteers").document(volunteerId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> skills = (List<String>) documentSnapshot.get("skills");
                if (skills != null) {
                    cgCurrentSkills.removeAllViews();
                    for (String s : skills) {
                        Chip chip = new Chip(this);
                        chip.setText(s);
                        chip.setChipBackgroundColorResource(R.color.chip_skill_background);
                        chip.setTextColor(getResources().getColor(R.color.chip_skill_text));
                        cgCurrentSkills.addView(chip);
                    }
                }
            }
        });
    }

    private void fetchSkillsGap() {
        progressBar.setVisibility(View.VISIBLE);
        GroqRecommendationHelper.getSkillsGapAnalysis(volunteerId, new GroqRecommendationHelper.OnSkillsGapListener() {
            @Override
            public void onSuccess(List<String> skills, List<String> reasons) {
                progressBar.setVisibility(View.GONE);
                missingSkills.clear();
                missingSkills.addAll(skills);
                missingReasons.clear();
                missingReasons.addAll(reasons);
                
                if (missingSkills.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvSkillsGap.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvSkillsGap.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SkillsGapActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class SkillGapAdapter extends RecyclerView.Adapter<SkillGapViewHolder> {
        @NonNull
        @Override
        public SkillGapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_skill_gap, parent, false);
            return new SkillGapViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SkillGapViewHolder holder, int position) {
            String skill = missingSkills.get(position);
            String reason = missingReasons.get(position);
            
            holder.tvSkillName.setText(skill);
            holder.tvReason.setText(reason);
            
            holder.btnAddProfile.setOnClickListener(v -> {
                db.collection("volunteers").document(volunteerId)
                        .update("skills", FieldValue.arrayUnion(skill))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(SkillsGapActivity.this, "Added to your profile", Toast.LENGTH_SHORT).show();
                            missingSkills.remove(position);
                            missingReasons.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, missingSkills.size());
                            loadCurrentSkills();
                            if (missingSkills.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                            }
                        });
            });
        }

        @Override
        public int getItemCount() {
            return missingSkills.size();
        }
    }

    private static class SkillGapViewHolder extends RecyclerView.ViewHolder {
        TextView tvSkillName, tvReason;
        View btnAddProfile;
        SkillGapViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSkillName = itemView.findViewById(R.id.tvSkillName);
            tvReason = itemView.findViewById(R.id.tvReason);
            btnAddProfile = itemView.findViewById(R.id.btnAddProfile);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}