package com.error404.communityvolunteerplatform.helpers;

import com.google.firebase.firestore.FirebaseFirestore;

public class UserHelper {
    public interface OnNameFetchedListener {
        void onFetched(String displayName, String profilePicUrl);
    }

    /**
     * Fetches a user's display name by checking users, volunteers, and organisations
     * collections in order. Always returns something — never returns null.
     */
    public static void fetchDisplayName(String userId, OnNameFetchedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Try users collection first
        db.collection("users").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("fullName");
                    String pic = doc.getString("profilePicUrl");
                    if (name != null && !name.trim().isEmpty()) {
                        listener.onFetched(name, pic);
                        return;
                    }
                }
                // Fallback: try volunteers collection
                db.collection("volunteers").document(userId).get()
                    .addOnSuccessListener(vDoc -> {
                        if (vDoc.exists()) {
                            String name = vDoc.getString("fullName");
                            String pic = vDoc.getString("profilePicUrl");
                            if (name != null && !name.trim().isEmpty()) {
                                listener.onFetched(name, pic);
                                return;
                            }
                        }
                        // Fallback: try organisations collection
                        db.collection("organisations").document(userId).get()
                            .addOnSuccessListener(oDoc -> {
                                if (oDoc.exists()) {
                                    String name = oDoc.getString("orgName");
                                    String pic = oDoc.getString("logoUrl");
                                    if (name != null && !name.trim().isEmpty()) {
                                        listener.onFetched(name, pic);
                                        return;
                                    }
                                }
                                // Final fallback
                                listener.onFetched("User", null);
                            })
                            .addOnFailureListener(e -> listener.onFetched("User", null));
                    })
                    .addOnFailureListener(e -> listener.onFetched("User", null));
            })
            .addOnFailureListener(e -> listener.onFetched("User", null));
    }
}
