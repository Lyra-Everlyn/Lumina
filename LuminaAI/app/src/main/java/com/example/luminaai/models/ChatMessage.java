package com.example.luminaai.models;

public class ChatMessage {
    private long id;
    private String message;
    private boolean isUser;
    private boolean isBookmarked;

    public ChatMessage(long id, String message, boolean isUser, boolean isBookmarked) {
        this.id = id;
        this.message = message;
        this.isUser = isUser;
        this.isBookmarked = isBookmarked;
    }

    public long getId() { return id; }
    public String getMessage() { return message; }
    public boolean isUser() { return isUser; }

    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
}