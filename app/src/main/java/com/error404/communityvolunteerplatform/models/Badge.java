package com.error404.communityvolunteerplatform.models;


public class Badge {
    private String badgeId;       // e.g. "first_volunteer"
    private String name;          // e.g. "First Volunteer"
    private String description;   // e.g. "Completed your first opportunity"
    private String iconKey;       // maps to drawable resource name in res/drawable/

    public Badge() {} // Required for Firestore

    public Badge(String badgeId, String name, String description, String iconKey) {
        this.badgeId = badgeId;
        this.name = name;
        this.description = description;
        this.iconKey = iconKey;
    }

    public String getBadgeId() { return badgeId; }
    public void setBadgeId(String badgeId) { this.badgeId = badgeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
}
