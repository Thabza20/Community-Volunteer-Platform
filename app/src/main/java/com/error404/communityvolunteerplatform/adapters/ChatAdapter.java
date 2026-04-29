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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private String currentUserId;
    private OnChatClickListener listener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatAdapter(List<Chat> chatList, String currentUserId, OnChatClickListener listener) {
        this.chatList = chatList;
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
        Chat chat = chatList.get(position);
        
        String otherUserId = "";
        for (String uid : chat.getParticipants()) {
            if (!uid.equals(currentUserId)) {
                otherUserId = uid;
                break;
            }
        }

        holder.tvLastMessage.setText(chat.getLastMessage());
        if (chat.getLastMessageAt() != null) {
            holder.tvTime.setText(sdf.format(chat.getLastMessageAt().toDate()));
        }

        // Fetch other user's name
        String finalOtherUserId = otherUserId;
        db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                String role = userDoc.getString("role");
                if ("volunteer".equals(role)) {
                    db.collection("volunteers").document(finalOtherUserId).get().addOnSuccessListener(volDoc -> {
                        if (volDoc.exists()) {
                            holder.tvUserName.setText(volDoc.getString("firstName") + " " + volDoc.getString("surname"));
                            String picUrl = volDoc.getString("profilePicUrl");
                            if (picUrl != null && !picUrl.isEmpty()) {
                                Glide.with(holder.itemView.getContext()).load(picUrl).placeholder(R.drawable.ic_launcher_background).into(holder.ivProfilePic);
                            }
                        }
                    });
                } else if ("organisation".equals(role)) {
                    db.collection("organisations").document(finalOtherUserId).get().addOnSuccessListener(orgDoc -> {
                        if (orgDoc.exists()) {
                            holder.tvUserName.setText(orgDoc.getString("orgName"));
                            String logoUrl = orgDoc.getString("logoUrl");
                            if (logoUrl != null && !logoUrl.isEmpty()) {
                                Glide.with(holder.itemView.getContext()).load(logoUrl).placeholder(R.drawable.ic_launcher_background).into(holder.ivProfilePic);
                            }
                        }
                    });
                } else {
                    holder.tvUserName.setText("Admin (" + userDoc.getString("email") + ")");
                }
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePic;
        TextView tvUserName, tvLastMessage, tvTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
