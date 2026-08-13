package com.example.luminaai.models;

public class ChatSession {
    private long sessionId;
    private String sessionTitle;
    private String subjectName;
    private long createdAt;

    public ChatSession(long sessionId, String sessionTitle, String subjectName, long createdAt) {
        this.sessionId = sessionId;
        this.sessionTitle = sessionTitle;
        this.subjectName = subjectName;
        this.createdAt = createdAt;
    }

    public long getSessionId() { return sessionId; }
    public String getSessionTitle() { return sessionTitle; }
    public String getSubjectName() { return subjectName != null ? subjectName : "General"; }
    public long getCreatedAt() { return createdAt; }
}