package com.error404.communityvolunteerplatform.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.Application;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrgApplicationAdapter extends RecyclerView.Adapter<OrgApplicationAdapter.ViewHolder> {

    private List<Application> applicationList;
    private OnApplicationActionListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public interface OnApplicationActionListener {
        void onApprove(Application application);
        void onReject(Application application);
    }

    public OrgApplicationAdapter(List<Application> applicationList, OnApplicationActionListener listener) {
        this.applicationList = applicationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_org_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Application application = applicationList.get(position);

        holder.tvVolunteerName.setText(application.getVolunteerName());
        holder.tvVolunteerEmail.setText(application.getVolunteerEmail());

        if (application.getAppliedAt() != null) {
            holder.tvAppliedDate.setText("Applied on: " + sdf.format(application.getAppliedAt().toDate()));
        }

        String status = application.getStatus();
        if ("pending".equals(status)) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.tvStatus.setVisibility(View.GONE);
        } else {
            holder.layoutActions.setVisibility(View.GONE);
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(status.toUpperCase());
            if ("approved".equals(status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
            }
        }

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(application));
        holder.btnReject.setOnClickListener(v -> listener.onReject(application));
    }

    @Override
    public int getItemCount() {
        return applicationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVolunteerName, tvVolunteerEmail, tvAppliedDate, tvStatus;
        LinearLayout layoutActions;
        MaterialButton btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVolunteerName = itemView.findViewById(R.id.tvVolunteerName);
            tvVolunteerEmail = itemView.findViewById(R.id.tvVolunteerEmail);
            tvAppliedDate = itemView.findViewById(R.id.tvAppliedDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}