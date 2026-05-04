package com.error404.communityvolunteerplatform.helpers;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    public static void createNotification(String userId, String title, String body, String type, String referenceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("body", body);
        notification.put("type", type);
        notification.put("referenceId", referenceId);
        notification.put("read", false);
        notification.put("createdAt", Timestamp.now());

        db.collection("notifications").add(notification);
    }

    public static void markAllAsRead(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    boolean hasUpdates = false;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isRead = doc.getBoolean("read");
                        if (isRead != null && !isRead) {
                            batch.update(doc.getReference(), "read", true);
                            hasUpdates = true;
                        }
                    }
                    if (hasUpdates) {
                        batch.commit();
                    }
                });
    }
}
