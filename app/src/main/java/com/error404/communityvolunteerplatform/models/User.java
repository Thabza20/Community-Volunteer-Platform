package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;

public class User {
    private String userId;       // Firebase Auth UID
    private String role;         // "volunteer" | "organisation" | "admin"
    private String email;
    private boolean emailVerified;
    private boolean popiaAccepted;
    private Timestamp createdAt;

    public User() {} // Required for Firestore

    public User(String userId, String role, String email) {
        this.userId = userId;
        this.role = role;
        this.email = email;
        this.emailVerified = false;
        this.popiaAccepted = false;
        this.createdAt = Timestamp.now();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isPopiaAccepted() { return popiaAccepted; }
    public void setPopiaAccepted(boolean popiaAccepted) { this.popiaAccepted = popiaAccepted; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}