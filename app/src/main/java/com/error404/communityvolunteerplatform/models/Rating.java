package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;

public class Rating {
    private String ratingId;
    private String fromVolunteerId;
    private String toOrgId;
    private String opportunityId;
    private int stars;           // 1 to 5
    private String comment;
    private Timestamp createdAt;

    public Rating() {} // Required for Firestore

    public Rating(String fromVolunteerId, String toOrgId,
                  String opportunityId, int stars, String comment) {
        this.fromVolunteerId = fromVolunteerId;
        this.toOrgId = toOrgId;
        this.opportunityId = opportunityId;
        this.stars = stars;
        this.comment = comment;
        this.createdAt = Timestamp.now();
    }

    public String getRatingId() { return ratingId; }
    public void setRatingId(String ratingId) { this.ratingId = ratingId; }

    public String getFromVolunteerId() { return fromVolunteerId; }
    public void setFromVolunteerId(String fromVolunteerId) { this.fromVolunteerId = fromVolunteerId; }

    public String getToOrgId() { return toOrgId; }
    public void setToOrgId(String toOrgId) { this.toOrgId = toOrgId; }

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}