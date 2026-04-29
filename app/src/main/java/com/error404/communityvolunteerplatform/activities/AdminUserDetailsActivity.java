package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminUserDetailsActivity extends AppCompatActivity {

    private ImageView ivUserPhoto;
    private TextView tvUserName, tvUserRole, tvEmail, tvJoinedDate, tvStatus;
    private Button btnBlockUser;
    
    private FirebaseFirestore db;
    private String userId;
    private User user;
    private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_details);

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        ivUserPhoto = findViewById(R.id.ivUserPhoto);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserRole = findViewById(R.id.tvUserRole);
        tvEmail = findViewById(R.id.tvEmail);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);
        tvStatus = findViewById(R.id.tvStatus);
        btnBlockUser = findViewById(R.id.btnBlockUser);

        loadUserDetails();

        btnBlockUser.setOnClickListener(v -> toggleBlockStatus());
    }

    private void loadUserDetails() {
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    displayUserBasicInfo();
                    fetchRoleSpecificInfo();
                }
            }
        });
    }

    private void displayUserBasicInfo() {
        tvUserName.setText(user.getDisplayName());
        tvUserRole.setText(user.getRole());
        tvEmail.setText("Email: " + user.getEmail());
        if (user.getCreatedAt() != null) {
            tvJoinedDate.setText("Joined: " + sdf.format(user.getCreatedAt().toDate()));
        }
        
        updateBlockButtonUI();
    }

    private void updateBlockButtonUI() {
        if (user.isBlocked()) {
            tvStatus.setText("Status: Blocked");
            btnBlockUser.setText("Unblock User");
            btnBlockUser.setBackgroundTintList(getColorStateList(R.color.passport_card_accent));
        } else {
            tvStatus.setText("Status: Active");
            btnBlockUser.setText("Block User");
            btnBlockUser.setBackgroundTintList(getColorStateList(android.R.color.holo_red_dark));
        }
    }

    private void fetchRoleSpecificInfo() {
        String collection = "volunteers".equals(user.getRole()) ? "volunteers" : "organisations";
        db.collection(collection).document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String photoUrl = "volunteers".equals(user.getRole()) ? doc.getString("profilePicUrl") : doc.getString("logoUrl");
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this).load(photoUrl).placeholder(R.mipmap.ic_launcher_round).into(ivUserPhoto);
                }
            }
        });
    }

    private void toggleBlockStatus() {
        boolean newStatus = !user.isBlocked();
        db.collection("users").document(userId).update("blocked", newStatus).addOnSuccessListener(aVoid -> {
            user.setBlocked(newStatus);
            updateBlockButtonUI();
            String msg = newStatus ? "User blocked" : "User unblocked";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }
}
