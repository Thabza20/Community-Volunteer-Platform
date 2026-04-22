package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;

public class Chat {
    private String chatId;           // volunteerId_opportunityId
    private String volunteerId;
    private String orgId;
    private String opportunityId;
    private String opportunityTitle; // denormalised for display
    private String lastMessage;      // preview text for chat list
    private Timestamp lastMessageAt;

    public Chat() {} // Required for Firestore

    public Chat(String volunteerId, String orgId,
                String opportunityId, String opportunityTitle) {
        this.chatId = volunteerId + "_" + opportunityId;
        this.volunteerId = volunteerId;
        this.orgId = orgId;
        this.opportunityId = opportunityId;
        this.opportunityTitle = opportunityTitle;
        this.lastMessage = "";
        this.lastMessageAt = Timestamp.now();
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getVolunteerId() { return volunteerId; }
    public void setVolunteerId(String volunteerId) { this.volunteerId = volunteerId; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }

    public String getOpportunityTitle() { return opportunityTitle; }
    public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Timestamp getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Timestamp lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}