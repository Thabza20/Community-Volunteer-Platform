package com.error404.communityvolunteerplatform.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.Application;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder> {

    private List<Application> applicationList;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public ApplicationAdapter(List<Application> applicationList) {
        this.applicationList = applicationList;
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
        
        holder.tvStatus.setText(application.getStatus().toUpperCase());
        setStatusColor(holder.tvStatus, application.getStatus());

        if (application.getAppliedAt() != null) {
            holder.tvAppliedDate.setText("Applied on: " + sdf.format(application.getAppliedAt().toDate()));
        }

        // Fetch Opportunity Title and Org Name
        holder.tvOppTitle.setText("Loading...");
        holder.tvOrgName.setText("Loading...");

        db.collection("opportunities").document(application.getOpportunityId()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                holder.tvOppTitle.setText(doc.getString("title"));
                holder.tvOrgName.setText(doc.getString("orgName"));
            }
        });
    }

    private void setStatusColor(TextView tv, String status) {
        switch (status) {
            case "approved":
                tv.setBackgroundColor(Color.parseColor("#C8E6C9"));
                tv.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case "rejected":
                tv.setBackgroundColor(Color.parseColor("#FFCDD2"));
                tv.setTextColor(Color.parseColor("#C62828"));
                break;
            case "pending":
            default:
                tv.setBackgroundColor(Color.parseColor("#FFE0B2"));
                tv.setTextColor(Color.parseColor("#F57C00"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return applicationList.size();
    }

    static class ApplicationViewHolder extends RecyclerView.ViewHolder {
        TextView tvOppTitle, tvOrgName, tvStatus, tvAppliedDate;

        public ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOppTitle = itemView.findViewById(R.id.tvOppTitle);
            tvOrgName = itemView.findViewById(R.id.tvOrgName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAppliedDate = itemView.findViewById(R.id.tvAppliedDate);
        }
    }
}
