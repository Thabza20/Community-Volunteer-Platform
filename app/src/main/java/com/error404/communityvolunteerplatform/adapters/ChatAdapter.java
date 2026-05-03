package com.error404.communityvolunteerplatform.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.Chat;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<DocumentSnapshot> chatSnapshots;
    private final String currentUserId;
    private final OnChatClickListener listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnChatClickListener {
        void onChatClick(String chatId, String otherUserId);
    }

    public ChatAdapter(List<DocumentSnapshot> chatSnapshots, String currentUserId, OnChatClickListener listener) {
        this.chatSnapshots = chatSnapshots;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        DocumentSnapshot chatDoc = chatSnapshots.get(position);
        String chatId = chatDoc.getId();
        
        List<String> participants = (List<String>) chatDoc.get("participants");
        String otherUserId = "";
        if (participants != null) {
            for (String uid : participants) {
                if (!uid.equals(currentUserId)) {
                    otherUserId = uid;
                    break;
                }
            }
        }

        holder.tvLastMessage.setText(chatDoc.getString("lastMessage"));
        
        Timestamp timestamp = chatDoc.getTimestamp("lastMessageAt");
        if (timestamp != null) {
            holder.tvTime.setText(formatDate(timestamp));
        } else {
            holder.tvTime.setText("");
        }

        Long unreadCount = chatDoc.getLong("unreadCount_" + currentUserId);
        if (unreadCount != null && unreadCount > 0) {
            holder.tvUnreadCount.setText(String.valueOf(unreadCount));
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        // Fetch other user details
        String finalOtherUserId = otherUserId;
        db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                String fullName = userDoc.getString("fullName");
                if (fullName == null) fullName = userDoc.getString("orgName"); // Fallback
                if (fullName == null) fullName = "User";
                
                holder.tvUserName.setText(fullName);
                
                String profilePicUrl = userDoc.getString("profilePicUrl");
                Glide.with(holder.itemView.getContext())
                        .load(profilePicUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .into(holder.ivProfilePic);
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chatId, finalOtherUserId));
    }

    private String formatDate(Timestamp timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp.toDate());
        Calendar now = Calendar.getInstance();
        
        if (now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate());
        } else {
            return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(timestamp.toDate());
        }
    }

    @Override
    public int getItemCount() {
        return chatSnapshots.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePic;
        TextView tvUserName, tvLastMessage, tvTime, tvUnreadCount;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}
