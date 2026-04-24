package com.error404.communityvolunteerplatform.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.Opportunity;

import java.util.List;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.OpportunityViewHolder> {

    private List<Opportunity> opportunities;
    private OnOpportunityClickListener listener;

    public interface OnOpportunityClickListener {
        void onOpportunityClick(Opportunity opportunity);
    }

    public OpportunityAdapter(List<Opportunity> opportunities, OnOpportunityClickListener listener) {
        this.opportunities = opportunities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OpportunityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_opportunity, parent, false);
        return new OpportunityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OpportunityViewHolder holder, int position) {
        Opportunity opportunity = opportunities.get(position);
        holder.bind(opportunity, listener);
    }

    @Override
    public int getItemCount() {
        return opportunities.size();
    }

    static class OpportunityViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvOrgName, tvCategory, tvDescription, tvSlots;

        public OpportunityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvOrgName = itemView.findViewById(R.id.tvOrgName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvSlots = itemView.findViewById(R.id.tvSlots);
        }

        public void bind(Opportunity opportunity, OnOpportunityClickListener listener) {
            tvTitle.setText(opportunity.getTitle());
            tvOrgName.setText(opportunity.getOrgName());
            tvCategory.setText(opportunity.getCategory());
            tvDescription.setText(opportunity.getOpportunityDescription());
            tvSlots.setText("Slots: " + opportunity.getSlotsFilled() + "/" + opportunity.getSlotsTotal());

            itemView.setOnClickListener(v -> listener.onOpportunityClick(opportunity));
        }
    }
}
