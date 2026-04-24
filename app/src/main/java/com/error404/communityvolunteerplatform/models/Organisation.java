package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;

public class Organisation {
    private String userId;
    private String orgName;
    private String email;
    private String location;
    private String logoUrl;              // Firebase Storage URL — nullable (null = system avatar)
    private String orgDetails;          // general description / about
    private String orgNumber;
    private String primaryPhoneNumber;
    private String secondaryPhoneNumber; // nullable
    private String status;              // "pending" | "approved"
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Organisation() {} // Required for Firestore

    public Organisation(String userId, String orgName, String email,
                        String location, String primaryPhoneNumber, String orgNumber) {
        this.userId = userId;
        this.orgName = orgName;
        this.email = email;
        this.location = location;
        this.primaryPhoneNumber = primaryPhoneNumber;
        this.orgNumber = orgNumber;
        this.status = "pending";
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getOrgDetails() { return orgDetails; }
    public void setOrgDetails(String orgDetails) { this.orgDetails = orgDetails; }

    public String getOrgNumber() { return orgNumber; }
    public void setOrgNumber(String orgNumber) { this.orgNumber = orgNumber; }

    public String getPrimaryPhoneNumber() { return primaryPhoneNumber; }
    public void setPrimaryPhoneNumber(String primaryPhoneNumber) { this.primaryPhoneNumber = primaryPhoneNumber; }

    public String getSecondaryPhoneNumber() { return secondaryPhoneNumber; }
    public void setSecondaryPhoneNumber(String secondaryPhoneNumber) { this.secondaryPhoneNumber = secondaryPhoneNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
