package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersListActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<User> userList = new ArrayList<>();
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        rvUsers = findViewById(R.id.rvUsers);
        progressBar = findViewById(R.id.progressBar);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter();
        rvUsers.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        if (currentUserId == null) return;
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult() != null) {
                userList.clear();
                for (DocumentSnapshot doc : task.getResult()) {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        user.setUserId(doc.getId());
                        if (!user.getUserId().equals(currentUserId)) {
                            userList.add(user);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChatWithUser(User otherUser) {
        progressBar.setVisibility(View.VISIBLE);
        String otherUserId = otherUser.getUserId();

        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String existingChatId = null;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        List<String> participants = (List<String>) doc.get("participants");
                        if (participants != null && participants.contains(otherUserId)) {
                            existingChatId = doc.getId();
                            break;
                        }
                    }

                    if (existingChatId != null) {
                        progressBar.setVisibility(View.GONE);
                        navigateToMessages(existingChatId, otherUserId);
                    } else {
                        createNewChat(otherUserId);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewChat(String otherUserId) {
        Map<String, Object> chat = new HashMap<>();
        chat.put("participants", Arrays.asList(currentUserId, otherUserId));
        chat.put("lastMessage", "");
        chat.put("lastMessageAt", Timestamp.now());
        chat.put("unreadCount_" + currentUserId, 0);
        chat.put("unreadCount_" + otherUserId, 0);

        db.collection("chats").add(chat)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    navigateToMessages(documentReference.getId(), otherUserId);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToMessages(String chatId, String otherUserId) {
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("otherUserId", otherUserId);
        startActivity(intent);
        finish();
    }

    private class UserAdapter extends RecyclerView.Adapter<UserViewHolder> {
        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = userList.get(position);
            holder.tvName.setText(user.getDisplayName());
            holder.tvType.setText(user.getRole() != null ? user.getRole() : "volunteer");
            holder.itemView.setOnClickListener(v -> openChatWithUser(user));
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }
    }

    private static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(android.R.id.text1);
            tvType = itemView.findViewById(android.R.id.text2);

            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextSize(16);
            tvType.setTextSize(12);
            tvType.setTextColor(0xFF888888); // Grey
        }
    }
}
