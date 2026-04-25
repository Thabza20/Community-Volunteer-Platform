// helpers/BadgeAwardHelper.java
// Fixed: correct package name (was Badgeawardhelper — Java class names are case-sensitive,
// the file must be named BadgeAwardHelper.java and the class declared as shown below)
package com.error404.communityvolunteerplatform.helpers;

import android.content.Context;
import android.widget.Toast;

import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BadgeAwardHelper {

    /**
     * Fetches the volunteer from Firestore, evaluates badges, saves any new ones.
     *
     * Call this from:
     *   - After org approves an application
     *   - After profile save
     *
     * @param volunteerId  Firebase UID
     * @param context      Pass 'this' from your Activity for the toast. Pass null to suppress it.
     */
    public static void checkAndAward(String volunteerId, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("volunteers")
                .document(volunteerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Volunteer volunteer = doc.toObject(Volunteer.class);
                    if (volunteer == null) return;

                    List<String> newBadges = BadgeEngine.evaluate(volunteer);
                    List<String> allBadges = BadgeEngine.mergeWithExisting(volunteer);

                    if (!newBadges.isEmpty()) {
                        db.collection("volunteers")
                                .document(volunteerId)
                                .update("badgeIds", allBadges)
                                .addOnSuccessListener(unused -> {
                                    if (context != null) {
                                        StringBuilder msg = new StringBuilder("🏅 New badge");
                                        if (newBadges.size() > 1) msg.append("s");
                                        msg.append(" earned: ");
                                        for (int i = 0; i < newBadges.size(); i++) {
                                            msg.append(BadgeEngine.getBadgeName(newBadges.get(i)));
                                            if (i < newBadges.size() - 1) msg.append(", ");
                                        }
                                        Toast.makeText(context, msg.toString(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
    }

    /**
     * Use this when recording that an event was completed.
     * Adds hours + 1 event count to Firestore, then checks for new badges.
     *
     * @param volunteerId  Firebase UID
     * @param hoursToAdd   Hours contributed (use 0 if unknown)
     * @param context      For toast
     */
    public static void recordEventCompletion(String volunteerId,
                                             double hoursToAdd,
                                             Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("volunteers")
                .document(volunteerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Volunteer volunteer = doc.toObject(Volunteer.class);
                    if (volunteer == null) return;

                    double updatedHours  = volunteer.getTotalHours() + hoursToAdd;
                    int    updatedEvents = volunteer.getProjectsCompleted() + 1;

                    db.collection("volunteers")
                            .document(volunteerId)
                            .update(
                                    "totalHours", updatedHours,
                                    "projectsCompleted", updatedEvents
                            )
                            .addOnSuccessListener(unused -> {
                                volunteer.setTotalHours(updatedHours);
                                volunteer.setProjectsCompleted(updatedEvents);

                                List<String> newBadges = BadgeEngine.evaluate(volunteer);
                                List<String> allBadges = BadgeEngine.mergeWithExisting(volunteer);

                                if (!newBadges.isEmpty()) {
                                    db.collection("volunteers")
                                            .document(volunteerId)
                                            .update("badgeIds", allBadges)
                                            .addOnSuccessListener(u -> {
                                                if (context != null) {
                                                    StringBuilder msg = new StringBuilder("🏅 New badge");
                                                    if (newBadges.size() > 1) msg.append("s");
                                                    msg.append(": ");
                                                    for (int i = 0; i < newBadges.size(); i++) {
                                                        msg.append(BadgeEngine.getBadgeName(newBadges.get(i)));
                                                        if (i < newBadges.size() - 1) msg.append(", ");
                                                    }
                                                    Toast.makeText(context, msg.toString(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                }
                            });
                });
    }
}