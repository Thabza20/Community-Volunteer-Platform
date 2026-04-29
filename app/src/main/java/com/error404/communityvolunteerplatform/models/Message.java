package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class Message {
    @DocumentId
    private String messageId;
    private String senderId;
    private String senderRole;   // "volunteer" | "organisation"
    private String text;
    private Timestamp timestamp;
    private boolean read;        // false until the other party opens the chat

    public Message() {} // Required for Firebase Realtime Database

    public Message(String senderId, String senderRole, String text) {
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.text = text;
        this.timestamp = Timestamp.now();
        this.read = false;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}


