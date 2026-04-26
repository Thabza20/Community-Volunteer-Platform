package com.error404.communityvolunteerplatform.helpers;

import android.content.Context;
import android.widget.Toast;

import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.List;

public class BadgeAwardHelper {

    public static void checkAndAward(String volunteerId, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("volunteers").document(volunteerId).get().addOnSuccessListener(documentSnapshot -> {
            Volunteer volunteer = documentSnapshot.toObject(Volunteer.class);
            if (volunteer != null) {
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
                                Toast.makeText(context, sb.toString(), Toast.LENGTH_LONG).show();
                            });
                }
            }
        });
    }

    public static void recordEventCompletion(String volunteerId, double hoursToAdd, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("volunteers").document(volunteerId)
                .update("totalHours", FieldValue.increment(hoursToAdd),
                        "projectsCompleted", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> checkAndAward(volunteerId, context));
    }
}