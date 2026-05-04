package com.error404.communityvolunteerplatform.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Map<String, Object> notification);
    }

    private List<Map<String, Object>> notificationList;
    private OnNotificationClickListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd MMM", Locale.getDefault());

    public NotificationsAdapter(List<Map<String, Object>> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Map<String, Object> notification = notificationList.get(position);

        holder.tvTitle.setText((String) notification.get("title"));
        holder.tvBody.setText((String) notification.get("body"));

        com.google.firebase.Timestamp createdAt = (com.google.firebase.Timestamp) notification.get("createdAt");
        if (createdAt != null) {
            holder.tvTime.setText(sdf.format(createdAt.toDate()));
        }

        String type = (String) notification.get("type");
        int dotColor = Color.GRAY;
        if (type != null) {
            switch (type) {
                case "application_approved": dotColor = Color.parseColor("#10B981"); break;
                case "application_rejected": dotColor = Color.parseColor("#EF4444"); break;
                case "application_pending": dotColor = Color.parseColor("#F59E0B"); break;
                case "chat_message": dotColor = Color.parseColor("#3B82F6"); break; // Blue for messages
            }
        }
        holder.vDot.getBackground().setTint(dotColor);

        boolean read = Boolean.TRUE.equals(notification.get("read"));
        if (!read) {
            holder.itemView.setBackgroundColor(Color.parseColor("#F3F0FF"));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        View vDot;
        TextView tvTitle, tvBody, tvTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            vDot = itemView.findViewById(R.id.vNotificationDot);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvBody = itemView.findViewById(R.id.tvNotificationBody);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
        }
    }
}
