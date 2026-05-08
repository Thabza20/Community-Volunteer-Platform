package com.error404.communityvolunteerplatform.helpers;

import android.content.Context;
import android.widget.Toast;

import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.List;

public class BadgeAwardHelper {

    public static void recordEventCompletion(String volunteerId,
            double hoursToAdd, Context context) {

        if (volunteerId == null || volunteerId.trim().isEmpty()) {
            Toast.makeText(context,
                    "Badge award failed: volunteer ID is missing",
                    Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("volunteers").document(volunteerId.trim())
                .update(
                    "totalHours",        FieldValue.increment(hoursToAdd),
                    "projectsCompleted", FieldValue.increment(1)
                )
                .addOnSuccessListener(aVoid ->
                        checkAndAward(volunteerId.trim(), context))
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Failed to update volunteer stats: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    public static void checkAndAward(String volunteerId, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("volunteers").document(volunteerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Volunteer volunteer = documentSnapshot.toObject(Volunteer.class);
                    if (volunteer != null) {
                        volunteer.setUserId(documentSnapshot.getId());
                        List<String> newBadges = BadgeEngine.evaluate(volunteer);
                        if (!newBadges.isEmpty()) {
                            List<String> allBadges = BadgeEngine.mergeWithExisting(volunteer);
                            db.collection("volunteers").document(volunteerId)
                                    .update("badgeIds", allBadges)
                                    .addOnSuccessListener(aVoid -> {
                                        StringBuilder sb = new StringBuilder("New Badges Earned: ");
                                        for (int i = 0; i < newBadges.size(); i++) {
                                            sb.append(BadgeEngine.getBadgeName(newBadges.get(i)));
                                            if (i < newBadges.size() - 1) sb.append(", ");
                                        }
                                        Toast.makeText(context,
                                                sb.toString(), Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context,
                                                    "Failed to save badges: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show());
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Failed to check badges: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}