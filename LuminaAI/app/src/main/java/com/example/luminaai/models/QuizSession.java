package com.example.luminaai.models;

public class QuizSession {
    private long sessionId;
    private String topic;
    private String difficulty;
    private int score;
    private int totalQuestions;
    private long createdAt;

    public QuizSession(long sessionId, String topic, String difficulty, int score, int totalQuestions, long createdAt) {
        this.sessionId = sessionId;
        this.topic = topic;
        this.difficulty = difficulty;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.createdAt = createdAt;
    }

    // Getters
    public long getSessionId() { return sessionId; }
    public String getTopic() { return topic; }
    public String getDifficulty() { return difficulty; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public long getCreatedAt() { return createdAt; }
}