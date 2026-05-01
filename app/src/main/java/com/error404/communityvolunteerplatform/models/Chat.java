package com.error404.communityvolunteerplatform.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class Chat {
    private String chatId;
    private List<String> participants; // List of UIDs [uid1, uid2]
    private String lastMessage;
    private Timestamp lastMessageAt;
    private String lastSenderId;

    public Chat() {}

    public Chat(String chatId, List<String> participants) {
        this.chatId = chatId;
        this.participants = participants;
        this.lastMessage = "";
        this.lastMessageAt = Timestamp.now();
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Timestamp getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Timestamp lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }
}
