package com.error404.communityvolunteerplatform.models;

public class AiChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;

    private String text;
    private int type;

    public AiChatMessage(String text, int type) {
        this.text = text;
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public int getType() {
        return type;
    }
}