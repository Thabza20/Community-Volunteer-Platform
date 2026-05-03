package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;

public class Opportunity {

    // ── Status constants ──────────────────────────────────────
    public static final String STATUS_ACTIVE               = "active";
    public static final String STATUS_HIDDEN               = "hidden";   // slots full
    public static final String STATUS_COMPLETED            = "completed";
    public static final String STATUS_OUTSTANDING          = "outstanding_applicants";
    public static final String STATUS_IN_PROGRESS          = "in_progress";
    public static final String STATUS_PENDING_APPROVAL     = "pending_approval";

    // ── Category constants ────────────────────────────────────
    public static final String CAT_COMMUNITY               = "Community Services";
    public static final String CAT_EDUCATION               = "Education & Mentoring";
    public static final String CAT_HEALTH                  = "Health & Social Care";
    public static final String CAT_ENVIRONMENT             = "Environment & Conservation";
    public static final String CAT_EMERGENCY               = "Emergency Response";
    public static final String CAT_ANIMAL                  = "Animal Welfare";
    public static final String CAT_ARTS                    = "Arts & Culture & Events";
    public static final String CAT_SKILLS                  = "Skills-based / Pro Bono";
    public static final String CAT_REMOTE                  = "Remote / Virtual Volunteering";

    private String opportunityId;        // Firestore document ID (NOT a field in the document)
    private String orgId;                // userId of the organisation
    private String orgName;              // denormalised for display
    private String title;                // opportunity name
    private String location;             // physical address
    private String orgDescription;       // about the org (character limited)
    private String opportunityDescription; // tasks, duties, roles
    private String category;             // one of the CAT_ constants above
    private String status;               // one of the STATUS_ constants above
    private Object eventDate;            // Can be Timestamp or String in DB
    private int slotsTotal;              // max applicants set by org
    private int slotsFilled;             // increments on approval, decrements on withdrawal
    private boolean requiresExperience;
    private String experienceRefPhone;   // nullable
    private String experienceRefEmail;   // nullable
    private boolean requiresQualification;
    private String qualificationFileUrl; // Firebase Storage URL — nullable
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean full;                // Whether the opportunity is full

    public Opportunity() {} // Required for Firestore

    public Opportunity(String orgId, String orgName, String title, String location,
                       String orgDescription, String opportunityDescription,
                       String category, int slotsTotal) {
        this.orgId = orgId;
        this.orgName = orgName;
        this.title = title;
        this.location = location;
        this.orgDescription = orgDescription;
        this.opportunityDescription = opportunityDescription;
        this.category = category;
        this.slotsTotal = slotsTotal;
        this.slotsFilled = 0;
        this.status = STATUS_ACTIVE;
        this.requiresExperience = false;
        this.requiresQualification = false;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // ── Slot logic helpers ────────────────────────────────────
    public void recalculateStatus() {
        if (slotsFilled >= slotsTotal) {
            this.status = STATUS_HIDDEN;
        } else if (STATUS_HIDDEN.equals(this.status)) {
            this.status = STATUS_ACTIVE;
        }
        this.updatedAt = Timestamp.now();
    }

    public boolean isFull() {
        return slotsFilled >= slotsTotal;
    }

    // Getters and setters
    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOrgDescription() { return orgDescription; }
    public void setOrgDescription(String orgDescription) { this.orgDescription = orgDescription; }

    public String getOpportunityDescription() { return opportunityDescription; }
    public void setOpportunityDescription(String opportunityDescription) { this.opportunityDescription = opportunityDescription; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Object getEventDate() { return eventDate; }
    public void setEventDate(Object eventDate) { this.eventDate = eventDate; }

    public int getSlotsTotal() { return slotsTotal; }
    public void setSlotsTotal(int slotsTotal) { this.slotsTotal = slotsTotal; }

    public int getSlotsFilled() { return slotsFilled; }
    public void setSlotsFilled(int slotsFilled) { this.slotsFilled = slotsFilled; }

    public boolean isRequiresExperience() { return requiresExperience; }
    public void setRequiresExperience(boolean requiresExperience) { this.requiresExperience = requiresExperience; }

    public String getExperienceRefPhone() { return experienceRefPhone; }
    public void setExperienceRefPhone(String experienceRefPhone) { this.experienceRefPhone = experienceRefPhone; }

    public String getExperienceRefEmail() { return experienceRefEmail; }
    public void setExperienceRefEmail(String experienceRefEmail) { this.experienceRefEmail = experienceRefEmail; }

    public boolean isRequiresQualification() { return requiresQualification; }
    public void setRequiresQualification(boolean requiresQualification) { this.requiresQualification = requiresQualification; }

    public String getQualificationFileUrl() { return qualificationFileUrl; }
    public void setQualificationFileUrl(String qualificationFileUrl) { this.qualificationFileUrl = qualificationFileUrl; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public void setFull(boolean full) { this.full = full; }
}
