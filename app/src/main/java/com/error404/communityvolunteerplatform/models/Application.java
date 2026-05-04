package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;

public class Application {

    public static final String STATUS_PENDING   = "pending";
    public static final String STATUS_APPROVED  = "approved";
    public static final String STATUS_REJECTED  = "rejected";
    public static final String STATUS_WITHDRAWN = "withdrawn";

    public static final String DEFAULT_REJECTION_MESSAGE = "Application unsuccessful.";

    private String applicationId;        // Firestore auto-generated
    private String opportunityId;
    private String orgId;
    private String volunteerId;
    private String volunteerName;        // denormalised: firstName + surname
    private String volunteerEmail;       // denormalised for org search
    private String status;               // one of STATUS_ constants
    private boolean withdrawnStatus;     // true if volunteer withdrew
    private String rejectionReason;      // custom or DEFAULT_REJECTION_MESSAGE — nullable
    private String cvFileUrl;            // Firebase Storage URL — nullable
    private String refName;
    private String refPhone;
    private String refEmail;
    private CvParsed cvParsed;           // Affinda parsed data — nullable
    private String cvParseStatus;        // "pending" | "done" | "failed" | null
    private Timestamp appliedAt;
    private Timestamp updatedAt;

    public Application() {} // Required for Firestore

    public Application(String opportunityId, String orgId,
                       String volunteerId, String volunteerName, String volunteerEmail) {
        this.opportunityId = opportunityId;
        this.orgId = orgId;
        this.volunteerId = volunteerId;
        this.volunteerName = volunteerName;
        this.volunteerEmail = volunteerEmail;
        this.status = STATUS_PENDING;
        this.withdrawnStatus = false;
        this.appliedAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // ── Nested class for Affinda parsed CV data ───────────────
    public static class CvParsed {
        private java.util.List<String> skills;
        private java.util.List<String> education;
        private java.util.List<String> certifications;
        private java.util.List<String> experience;
        private String rawText; // full extracted text, used by Groq prompt

        public CvParsed() {}

        public java.util.List<String> getSkills() { return skills; }
        public void setSkills(java.util.List<String> skills) { this.skills = skills; }

        public java.util.List<String> getEducation() { return education; }
        public void setEducation(java.util.List<String> education) { this.education = education; }

        public java.util.List<String> getCertifications() { return certifications; }
        public void setCertifications(java.util.List<String> certifications) { this.certifications = certifications; }

        public java.util.List<String> getExperience() { return experience; }
        public void setExperience(java.util.List<String> experience) { this.experience = experience; }

        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }
    }

    // Getters and setters
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getVolunteerId() { return volunteerId; }
    public void setVolunteerId(String volunteerId) { this.volunteerId = volunteerId; }

    public String getVolunteerName() { return volunteerName; }
    public void setVolunteerName(String volunteerName) { this.volunteerName = volunteerName; }

    public String getVolunteerEmail() { return volunteerEmail; }
    public void setVolunteerEmail(String volunteerEmail) { this.volunteerEmail = volunteerEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isWithdrawnStatus() { return withdrawnStatus; }
    public void setWithdrawnStatus(boolean withdrawnStatus) { this.withdrawnStatus = withdrawnStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getCvFileUrl() { return cvFileUrl; }
    public void setCvFileUrl(String cvFileUrl) { this.cvFileUrl = cvFileUrl; }

    public String getRefName() { return refName; }
    public void setRefName(String refName) { this.refName = refName; }

    public String getRefPhone() { return refPhone; }
    public void setRefPhone(String refPhone) { this.refPhone = refPhone; }

    public String getRefEmail() { return refEmail; }
    public void setRefEmail(String refEmail) { this.refEmail = refEmail; }

    public CvParsed getCvParsed() { return cvParsed; }
    public void setCvParsed(CvParsed cvParsed) { this.cvParsed = cvParsed; }

    public String getCvParseStatus() { return cvParseStatus; }
    public void setCvParseStatus(String cvParseStatus) { this.cvParseStatus = cvParseStatus; }

    public Timestamp getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Timestamp appliedAt) { this.appliedAt = appliedAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}

