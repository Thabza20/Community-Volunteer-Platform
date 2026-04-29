package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.UserAdapter;
import com.error404.communityvolunteerplatform.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminManageUsersActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private List<User> userList;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Users");
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        rvUsers = findViewById(R.id.rvUsers);
        progressBar = findViewById(R.id.progressBar);

        userList = new ArrayList<>();
        adapter = new UserAdapter(userList, user -> {
            Intent intent = new Intent(this, AdminUserDetailsActivity.class);
            intent.putExtra("userId", user.getUserId());
            startActivity(intent);
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                userList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        User user = document.toObject(User.class);
                        if (user.getUserId() == null) user.setUserId(document.getId());
                        
                        if (!user.getUserId().equals(currentUserId)) {
                            userList.add(user);
                        }
                    } catch (Exception e) {
                        Log.e("AdminManageUsers", "Error parsing user", e);
                    }
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
