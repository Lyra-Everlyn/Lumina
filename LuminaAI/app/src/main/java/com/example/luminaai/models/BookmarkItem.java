package com.example.luminaai.models;

public class BookmarkItem {
    private long messageId;
    private long sessionId;
    private String sessionTitle;
    private String messageText;
    private long createdAt;
    private String type = "chat";

    public BookmarkItem(long messageId, long sessionId, String sessionTitle, String messageText, long createdAt) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.sessionTitle = sessionTitle;
        this.messageText = messageText;
        this.createdAt = createdAt;
    }

    public long getMessageId() { return messageId; }
    public long getSessionId() { return sessionId; }
    public String getSessionTitle() { return sessionTitle; }
    public String getMessageText() { return messageText; }
    public long getCreatedAt() { return createdAt; }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}