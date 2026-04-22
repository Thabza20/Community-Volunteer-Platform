package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Volunteer {
    private String userId;
    private String firstName;
    private String surname;
    private String email;
    private String phoneNumber;
    private boolean phoneVerified;
    private String bio;
    private String location;
    private List<String> skills;         // e.g. ["Teaching", "First Aid", "Driving"]
    private List<String> interests;      // e.g. ["Education", "Health"]
    private String profilePicUrl;        // Firebase Storage URL — nullable (null = use default avatar)
    private double totalHours;           // default 0
    private int projectsCompleted;       // default 0
    private List<String> badgeIds;       // e.g. ["first_volunteer", "10_hours"]
    private boolean accountDeleteRequested; // default false
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Volunteer() {} // Required for Firestore

    public Volunteer(String userId, String firstName, String surname, String email, String location) {
        this.userId = userId;
        this.firstName = firstName;
        this.surname = surname;
        this.email = email;
        this.location = location;
        this.phoneVerified = false;
        this.totalHours = 0;
        this.projectsCompleted = 0;
        this.skills = new ArrayList<>();
        this.interests = new ArrayList<>();
        this.badgeIds = new ArrayList<>();
        this.accountDeleteRequested = false;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Full name helper
    public String getFullName() {
        return firstName + " " + surname;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }

    public int getProjectsCompleted() { return projectsCompleted; }
    public void setProjectsCompleted(int projectsCompleted) { this.projectsCompleted = projectsCompleted; }

    public List<String> getBadgeIds() { return badgeIds; }
    public void setBadgeIds(List<String> badgeIds) { this.badgeIds = badgeIds; }

    public boolean isAccountDeleteRequested() { return accountDeleteRequested; }
    public void setAccountDeleteRequested(boolean accountDeleteRequested) { this.accountDeleteRequested = accountDeleteRequested; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}