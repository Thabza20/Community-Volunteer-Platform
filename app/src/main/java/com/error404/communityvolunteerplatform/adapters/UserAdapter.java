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
import com.error404.communityvolunteerplatform.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserRole.setText(user.getRole());
        
        // Use display name if already present in User object
        holder.tvUserName.setText(user.getDisplayName());

        // Fetch details from specific collections for more up-to-date info
        String role = user.getRole();
        if ("volunteer".equals(role)) {
            db.collection("volunteers").document(user.getUserId()).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("firstName") + " " + doc.getString("surname");
                    holder.tvUserName.setText(name);
                    String picUrl = doc.getString("profilePicUrl");
                    if (picUrl != null && !picUrl.isEmpty()) {
                        Glide.with(holder.itemView.getContext()).load(picUrl).placeholder(R.drawable.ic_launcher_background).into(holder.ivProfilePic);
                    }
                }
            });
        } else if ("organisation".equals(role) || "organization".equals(role)) {
            db.collection("organisations").document(user.getUserId()).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    holder.tvUserName.setText(doc.getString("orgName"));
                    String logoUrl = doc.getString("logoUrl");
                    if (logoUrl != null && !logoUrl.isEmpty()) {
                        Glide.with(holder.itemView.getContext()).load(logoUrl).placeholder(R.drawable.ic_launcher_background).into(holder.ivProfilePic);
                    }
                }
            });
        }

        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePic;
        TextView tvUserName, tvUserRole;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
        }
    }
}
