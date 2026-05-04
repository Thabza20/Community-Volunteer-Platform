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
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.UserHelper;
import com.error404.communityvolunteerplatform.models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;
    private final Map<String, String> nameCache = new HashMap<>();
    private final Map<String, String> picCache = new HashMap<>();

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
        
        String userId = user.getUserId();
        if (nameCache.containsKey(userId)) {
            holder.tvUserName.setText(nameCache.get(userId));
            String picUrl = picCache.get(userId);
            if (picUrl != null && !picUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(picUrl)
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_default_avatar)
                        .into(holder.ivProfilePic);
            } else {
                holder.ivProfilePic.setImageResource(R.drawable.ic_default_avatar);
            }
        } else {
            holder.tvUserName.setText(holder.itemView.getContext().getString(R.string.loading));
            holder.ivProfilePic.setImageResource(R.drawable.ic_default_avatar);
            UserHelper.fetchDisplayName(userId, (name, picUrl) -> {
                nameCache.put(userId, name);
                picCache.put(userId, picUrl);
                notifyItemChanged(holder.getAdapterPosition());
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
