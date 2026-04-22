package com.error404.communityvolunteerplatform.models;

/**
 * Model class representing a Volunteer Event.
 */
public class VolunteerEvent {
    private String id;
    private String title;
    private String description;
    private String date;
    private String location;

    public VolunteerEvent() {
        // Default constructor for Firebase or other frameworks
    }

    public VolunteerEvent(String id, String title, String description, String date, String location) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.location = location;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}