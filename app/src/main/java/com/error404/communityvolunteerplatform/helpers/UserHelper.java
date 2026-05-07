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
        if (userId == null || userId.isEmpty()) {
            listener.onFetched("User", null);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Check users collection
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = extractName(doc);
                String pic = doc.getString("profilePicUrl");
                if (name != null) {
                    listener.onFetched(name, pic);
                    return;
                }
            }
            
            // 2. Fallback to volunteers
            db.collection("volunteers").document(userId).get().addOnSuccessListener(vDoc -> {
                if (vDoc.exists()) {
                    String name = extractName(vDoc);
                    String pic = vDoc.getString("profilePicUrl");
                    if (name != null) {
                        listener.onFetched(name, pic);
                        return;
                    }
                }
                
                // 3. Fallback to organisations
                db.collection("organisations").document(userId).get().addOnSuccessListener(oDoc -> {
                    if (oDoc.exists()) {
                        String name = oDoc.getString("orgName");
                        if (name == null || name.isEmpty()) name = extractName(oDoc);
                        
                        String pic = oDoc.getString("logoUrl");
                        if (pic == null) pic = oDoc.getString("profilePicUrl");
                        
                        if (name != null) {
                            listener.onFetched(name, pic);
                            return;
                        }
                    }
                    listener.onFetched("User", null);
                }).addOnFailureListener(e -> listener.onFetched("User", null));
            }).addOnFailureListener(e -> listener.onFetched("User", null));
        }).addOnFailureListener(e -> listener.onFetched("User", null));
    }

    private static String extractName(com.google.firebase.firestore.DocumentSnapshot doc) {
        // Try various common name fields
        String fullName = doc.getString("fullName");
        if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();
        
        String orgName = doc.getString("orgName");
        if (orgName != null && !orgName.trim().isEmpty()) return orgName.trim();

        String firstName = doc.getString("firstName");
        String lastName = doc.getString("lastName");
        if (lastName == null) lastName = doc.getString("surname");

        if (firstName != null && !firstName.trim().isEmpty()) {
            if (lastName != null && !lastName.trim().isEmpty()) {
                return firstName.trim() + " " + lastName.trim();
            }
            return firstName.trim();
        } else if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName.trim();
        }
        
        // Final effort: use email prefix if available
        String email = doc.getString("email");
        if (email != null && !email.isEmpty() && email.contains("@")) {
            return email.split("@")[0];
        }
        
        return null;
    }
}
