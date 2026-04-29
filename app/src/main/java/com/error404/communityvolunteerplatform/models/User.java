package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    @DocumentId
    private String userId;       // Document ID
    
    private String uid;          // Some scripts use a 'uid' field
    private String role;         // "volunteer" | "organisation" | "admin"
    private String email;
    private boolean emailVerified;
    private boolean popiaAccepted;
    private boolean blocked;
    private Timestamp createdAt;
    
    // Fields that might be present in 'users' collection depending on seeding
    private String firstName;
    private String lastName;
    private String surname;
    private String orgName;

    public User() {} // Required for Firestore

    public User(String userId, String role, String email) {
        this.userId = userId;
        this.uid = userId;
        this.role = role;
        this.email = email;
        this.emailVerified = false;
        this.popiaAccepted = false;
        this.createdAt = Timestamp.now();
    }

    public String getUserId() { 
        return userId != null ? userId : uid; 
    }
    public void setUserId(String userId) { 
        this.userId = userId; 
        if (this.uid == null) this.uid = userId;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { 
        this.uid = uid; 
        if (this.userId == null) this.userId = uid;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isPopiaAccepted() { return popiaAccepted; }
    public void setPopiaAccepted(boolean popiaAccepted) { this.popiaAccepted = popiaAccepted; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    
    public String getDisplayName() {
        if (orgName != null && !orgName.isEmpty()) return orgName;
        if (firstName != null && !firstName.isEmpty()) {
            String last = surname != null ? surname : lastName;
            return firstName + (last != null ? " " + last : "");
        }
        return email;
    }
}
