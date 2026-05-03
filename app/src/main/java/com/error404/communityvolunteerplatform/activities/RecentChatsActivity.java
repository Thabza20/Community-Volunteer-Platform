package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.ChatAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RecentChatsActivity extends AppCompatActivity {

    private RecyclerView rvRecentChats;
    private ChatAdapter adapter;
    private List<DocumentSnapshot> chatList;
    private TextView tvNoChats;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration chatsListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_chats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        rvRecentChats = findViewById(R.id.rvRecentChats);
        tvNoChats = findViewById(R.id.tvNoChats);
        FloatingActionButton fabNewChat = findViewById(R.id.fabNewChat);

        chatList = new ArrayList<>();
        adapter = new ChatAdapter(chatList, currentUserId, (chatId, otherUserId) -> {
            Intent intent = new Intent(RecentChatsActivity.this, MessagesActivity.class);
            intent.putExtra("chatId", chatId);
            intent.putExtra("otherUserId", otherUserId);
            startActivity(intent);
        });

        rvRecentChats.setLayoutManager(new LinearLayoutManager(this));
        rvRecentChats.setAdapter(adapter);

        fabNewChat.setOnClickListener(v -> {
            startActivity(new Intent(RecentChatsActivity.this, UsersListActivity.class));
        });

        loadRecentChats();
    }

    private void loadRecentChats() {
        chatsListener = db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("RecentChatsActivity", "Error loading chats", error);
                        return;
                    }
                    if (value != null) {
                        chatList.clear();
                        chatList.addAll(value.getDocuments());

                        // Sort by lastMessageAt descending
                        chatList.sort((d1, d2) -> {
                            Timestamp t1 = d1.getTimestamp("lastMessageAt");
                            Timestamp t2 = d2.getTimestamp("lastMessageAt");
                            if (t1 == null || t2 == null) return 0;
                            return t2.compareTo(t1);
                        });

                        adapter.notifyDataSetChanged();
                        tvNoChats.setVisibility(chatList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsListener != null) {
            chatsListener.remove();
        }
    }
}
