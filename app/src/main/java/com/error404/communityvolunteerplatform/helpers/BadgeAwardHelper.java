package com.error404.communityvolunteerplatform.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BadgeAwardHelper {

    private static final String TAG = "BadgeAwardHelper";

    public static void recordEventCompletion(String volunteerId,
            double hoursToAdd, Context context) {

        if (volunteerId == null || volunteerId.trim().isEmpty()) {
            Log.e(TAG, "recordEventCompletion called with NULL or empty volunteerId");
            Toast.makeText(context,
                    "Badge award failed: volunteer ID is missing",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String cleanId = volunteerId.trim();
        Log.d(TAG, "recordEventCompletion → volunteerId='" + cleanId + "'");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference volunteerRef = db.collection("volunteers").document(cleanId);

        // Step 1: Verify the document actually exists before updating
        volunteerRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // Document does not exist — log exactly what we tried
                        Log.e(TAG, "No volunteer document found at volunteers/" + cleanId);
                        Toast.makeText(context,
                                "Volunteer record not found (ID: " + cleanId + ")",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Log.d(TAG, "Volunteer document found. Updating stats...");

                    // Step 2: Use a Map with FieldValue.increment so we can use
                    // set+merge OR update — both work on an existing document.
                    // Using update() here since we confirmed the doc exists.
                    volunteerRef
                            .update(
                                "totalHours",        FieldValue.increment(hoursToAdd),
                                "projectsCompleted", FieldValue.increment(1)
                            )
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Stats updated successfully for " + cleanId);
                                checkAndAward(cleanId, context);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update stats: " + e.getMessage());
                                Toast.makeText(context,
                                        "Failed to update volunteer stats: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch volunteer document: " + e.getMessage());
                    Toast.makeText(context,
                            "Failed to reach volunteer record: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    public static void checkAndAward(String volunteerId, Context context) {
        Log.d(TAG, "checkAndAward → volunteerId='" + volunteerId + "'");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("volunteers").document(volunteerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "checkAndAward: document missing for " + volunteerId);
                        return;
                    }
                    Volunteer volunteer = documentSnapshot.toObject(Volunteer.class);
                    if (volunteer != null) {
                        volunteer.setUserId(documentSnapshot.getId());
                        Log.d(TAG, "Volunteer loaded: hours=" + volunteer.getTotalHours()
                                + " events=" + volunteer.getProjectsCompleted());
                        List<String> newBadges = BadgeEngine.evaluate(volunteer);
                        if (!newBadges.isEmpty()) {
                            List<String> allBadges = BadgeEngine.mergeWithExisting(volunteer);
                            db.collection("volunteers").document(volunteerId)
                                    .update("badgeIds", allBadges)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Badges saved: " + allBadges);
                                        StringBuilder sb = new StringBuilder("New Badge: ");
                                        for (int i = 0; i < newBadges.size(); i++) {
                                            sb.append(BadgeEngine.getBadgeName(newBadges.get(i)));
                                            if (i < newBadges.size() - 1) sb.append(", ");
                                        }
                                        Toast.makeText(context,
                                                sb.toString(), Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to save badges: " + e.getMessage());
                                        Toast.makeText(context,
                                                "Failed to save badges: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            Log.d(TAG, "No new badges to award.");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkAndAward fetch failed: " + e.getMessage());
                    Toast.makeText(context,
                            "Failed to check badges: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}