package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.NotificationsAdapter;
import com.error404.communityvolunteerplatform.helpers.NotificationHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity implements NotificationsAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private NotificationsAdapter adapter;
    private List<Map<String, Object>> notificationList = new ArrayList<>();
    private TextView tvNoNotifications;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        rvNotifications = findViewById(R.id.rvNotifications);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);

        adapter = new NotificationsAdapter(notificationList, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        listenToNotifications();
        NotificationHelper.markAllAsRead(currentUserId);
    }

    private void listenToNotifications() {
        registration = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            notificationList.add(doc.getData());
                        }
                        
                        // Sort locally to avoid "Missing Index" error (FAILED_PRECONDITION)
                        notificationList.sort((n1, n2) -> {
                            Timestamp t1 = (Timestamp) n1.get("createdAt");
                            Timestamp t2 = (Timestamp) n2.get("createdAt");
                            if (t1 == null || t2 == null) return 0;
                            return t2.compareTo(t1); // Descending order
                        });

                        adapter.notifyDataSetChanged();
                        tvNoNotifications.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    public void onNotificationClick(Map<String, Object> notification) {
        String type = (String) notification.get("type");
        String referenceId = (String) notification.get("referenceId");

        if (type == null) return;

        if (type.equals("chat_message")) {
            // For chat messages, we need the otherUserId. 
            // We can fetch the chat document to determine who the other person is.
            db.collection("chats").document(referenceId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> participants = (List<String>) documentSnapshot.get("participants");
                    if (participants != null) {
                        String otherUserId = null;
                        for (String uid : participants) {
                            if (!uid.equals(currentUserId)) {
                                otherUserId = uid;
                                break;
                            }
                        }
                        
                        Intent intent = new Intent(this, MessagesActivity.class);
                        intent.putExtra("chatId", referenceId);
                        intent.putExtra("otherUserId", otherUserId);
                        startActivity(intent);
                    }
                }
            });
        } else if (type.startsWith("application_")) {
            Intent intent = new Intent(this, ApplicationsListActivity.class);
            // We can pre-filter based on the notification type if we want
            String status = "all";
            if (type.equals("application_approved")) status = "approved";
            else if (type.equals("application_rejected")) status = "rejected";
            else if (type.equals("application_pending")) status = "pending";
            
            intent.putExtra("status", status);
            startActivity(intent);
        } else if (type.equals("new_application")) {
            Intent intent = new Intent(this, EventApplicantsActivity.class);
            intent.putExtra("OPPORTUNITY_ID", referenceId);
            
            // Extract opportunity title from the notification body if possible
            String body = (String) notification.get("body");
            if (body != null && body.contains("applied for ")) {
                String title = body.substring(body.indexOf("applied for ") + 12);
                intent.putExtra("OPPORTUNITY_TITLE", title);
            }
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }
}
