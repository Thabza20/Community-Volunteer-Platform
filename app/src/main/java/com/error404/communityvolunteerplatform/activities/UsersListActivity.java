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
import com.error404.communityvolunteerplatform.adapters.UserAdapter;
import com.error404.communityvolunteerplatform.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UsersListActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<User> userList = new ArrayList<>();
    private UserAdapter adapter;

    public static List<User> cachedUsers = null;
    private static long userCacheTimestamp = 0;
    private static final long USER_CACHE_MS = 5 * 60 * 1000;

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
        adapter = new UserAdapter(userList, this::openChatWithUser);
        rvUsers.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        if (currentUserId == null) return;
        progressBar.setVisibility(View.VISIBLE);

        // Fetch from multiple collections to show everyone
        db.collection("users").get().addOnCompleteListener(task -> {
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
                
                // Also check volunteers for those not in 'users'
                db.collection("volunteers").get().addOnSuccessListener(vDocs -> {
                    for (DocumentSnapshot doc : vDocs) {
                        if (!isIdInList(doc.getId()) && !doc.getId().equals(currentUserId)) {
                            User u = new User();
                            u.setUserId(doc.getId());
                            u.setFirstName(doc.getString("firstName"));
                            u.setLastName(doc.getString("lastName"));
                            u.setRole("volunteer");
                            u.setProfilePicUrl(doc.getString("profilePicUrl"));
                            userList.add(u);
                        }
                    }
                    
                    // Also check organisations
                    db.collection("organisations").get().addOnSuccessListener(oDocs -> {
                        for (DocumentSnapshot doc : oDocs) {
                            if (!isIdInList(doc.getId()) && !doc.getId().equals(currentUserId)) {
                                User u = new User();
                                u.setUserId(doc.getId());
                                u.setOrgName(doc.getString("orgName"));
                                u.setRole("organisation");
                                String pic = doc.getString("logoUrl");
                                if (pic == null) pic = doc.getString("profilePicUrl");
                                u.setProfilePicUrl(pic);
                                userList.add(u);
                            }
                        }
                        progressBar.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    });
                });
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isIdInList(String id) {
        for (User u : userList) {
            if (u.getUserId().equals(id)) return true;
        }
        return false;
    }

    private void openChatWithUser(User otherUser) {
        String otherUserId = otherUser.getUserId();

        String deterministicChatId = currentUserId.compareTo(otherUserId) < 0
                ? currentUserId + "_" + otherUserId
                : otherUserId + "_" + currentUserId;

        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra("chatId", deterministicChatId);
        intent.putExtra("otherUserId", otherUserId);
        startActivity(intent);
    }
}
