package com.error404.communityvolunteerplatform.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.Application;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder> {

    private List<Application> applicationList;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private Map<String, String> opportunityTitles = new HashMap<>();
    private Map<String, String> orgNames = new HashMap<>();
    private OnWithdrawClickListener withdrawClickListener;

    public interface OnWithdrawClickListener {
        void onWithdrawClick(Application application);
    }

    public ApplicationAdapter(List<Application> applicationList, OnWithdrawClickListener listener) {
        this.applicationList = applicationList;
        this.withdrawClickListener = listener;
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_application, parent, false);
        return new ApplicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        Application application = applicationList.get(position);

        String oppId = application.getOpportunityId();
        if (opportunityTitles.containsKey(oppId)) {
            holder.tvOppTitle.setText(opportunityTitles.get(oppId));
            holder.tvOrgName.setText(orgNames.get(oppId));
        } else {
            holder.tvOppTitle.setText("Loading...");
            holder.tvOrgName.setText("...");
            db.collection("opportunities").document(oppId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String title = doc.getString("title");
                    String orgName = doc.getString("orgName");
                    opportunityTitles.put(oppId, title);
                    orgNames.put(oppId, orgName);
                    notifyItemChanged(holder.getAdapterPosition());
                }
            });
        }

        if (application.getAppliedAt() != null) {
            holder.tvAppDate.setText(sdf.format(application.getAppliedAt().toDate()));
        }

        String status = application.getStatus();
        holder.chipStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));
        
        int chipColor;
        switch (status) {
            case "approved": chipColor = Color.parseColor("#10B981"); break;
            case "rejected": chipColor = Color.parseColor("#EF4444"); break;
            case "withdrawn": chipColor = Color.parseColor("#9CA3AF"); break;
            case "pending":
            default: chipColor = Color.parseColor("#F59E0B"); break;
        }
        holder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(chipColor));

        if ("pending".equals(status) && !application.isWithdrawnStatus()) {
            holder.btnWithdraw.setVisibility(View.VISIBLE);
            holder.btnWithdraw.setOnClickListener(v -> {
                if (withdrawClickListener != null) {
                    withdrawClickListener.onWithdrawClick(application);
                }
            });
        } else {
            holder.btnWithdraw.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return applicationList.size();
    }

    public void updateList(List<Application> newList) {
        this.applicationList = newList;
        notifyDataSetChanged();
    }

    static class ApplicationViewHolder extends RecyclerView.ViewHolder {
        TextView tvOppTitle, tvOrgName, tvAppDate;
        Chip chipStatus;
        Button btnWithdraw;

        public ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOppTitle = itemView.findViewById(R.id.tvAppOppTitle);
            tvOrgName = itemView.findViewById(R.id.tvAppOrgName);
            tvAppDate = itemView.findViewById(R.id.tvAppDate);
            chipStatus = itemView.findViewById(R.id.chipAppStatus);
            btnWithdraw = itemView.findViewById(R.id.btnWithdraw);
        }
    }
}
