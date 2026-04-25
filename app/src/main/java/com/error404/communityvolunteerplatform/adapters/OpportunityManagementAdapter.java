package com.error404.communityvolunteerplatform.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.Opportunity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpportunityManagementAdapter extends RecyclerView.Adapter<OpportunityManagementAdapter.ViewHolder> {

    private List<Opportunity> opportunities;
    private Map<String, Integer> applicantCounts;
    private OnOpportunityClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnOpportunityClickListener {
        void onOpportunityClick(Opportunity opportunity);
    }

    public OpportunityManagementAdapter(List<Opportunity> opportunities, Map<String, Integer> applicantCounts, OnOpportunityClickListener listener) {
        this.opportunities = opportunities;
        this.applicantCounts = applicantCounts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_opportunity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Opportunity opportunity = opportunities.get(position);
        holder.tvTitle.setText(opportunity.getTitle());
        
        if (opportunity.getCreatedAt() != null) {
            holder.tvDate.setText(dateFormat.format(opportunity.getCreatedAt().toDate()));
        }

        Integer countObj = applicantCounts.get(opportunity.getOpportunityId());
        int count = countObj != null ? countObj : 0;
        holder.tvApplicants.setText("Applicants: " + count);

        // Show "NEW" badge if created in the last 24 hours
        long currentTime = System.currentTimeMillis();
        if (opportunity.getCreatedAt() != null) {
            long createdTime = opportunity.getCreatedAt().toDate().getTime();
            if (currentTime - createdTime < 24 * 60 * 60 * 1000) {
                holder.tvNewBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvNewBadge.setVisibility(View.GONE);
            }
        } else {
            holder.tvNewBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onOpportunityClick(opportunity));
    }

    @Override
    public int getItemCount() {
        return opportunities.size();
    }

    public void updateData(List<Opportunity> newOpportunities, Map<String, Integer> newCounts) {
        this.opportunities = newOpportunities;
        this.applicantCounts = newCounts;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvApplicants, tvNewBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvApplicants = itemView.findViewById(R.id.tvApplicants);
            tvNewBadge = itemView.findViewById(R.id.tvNewBadge);
        }
    }
}