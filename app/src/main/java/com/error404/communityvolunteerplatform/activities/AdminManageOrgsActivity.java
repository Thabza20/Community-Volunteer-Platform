package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminManageOrgsActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private List<User> orgList;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Organisations");
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();

        rvUsers = findViewById(R.id.rvUsers);
        progressBar = findViewById(R.id.progressBar);

        orgList = new ArrayList<>();
        adapter = new UserAdapter(orgList, user -> {
            Intent intent = new Intent(this, AdminUserDetailsActivity.class);
            intent.putExtra("userId", user.getUserId());
            startActivity(intent);
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        loadOrganisations();
    }

    private void loadOrganisations() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users")
                .whereIn("role", java.util.Arrays.asList("organisation", "organization"))
                .get()
                .addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                orgList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    User user = document.toObject(User.class);
                    if (user.getUserId() == null) user.setUserId(document.getId());
                    orgList.add(user);
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to load organisations", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
