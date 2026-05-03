package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;

public class Message {
    private String messageId;
    private String senderId;
    private String senderRole;   // "volunteer" | "organisation"
    private String text;
    private Timestamp sentAt;
    private boolean read;

    public Message() {}

    public Message(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
        this.sentAt = Timestamp.now();
        this.read = false;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Timestamp getSentAt() { return sentAt; }
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}


