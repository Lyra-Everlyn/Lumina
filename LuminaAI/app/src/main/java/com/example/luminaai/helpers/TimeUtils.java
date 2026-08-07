package com.example.luminaai.helpers;

public class TimeUtils {

    public static String getTimeAgo(long createdAt) {
        if (createdAt == 0) return "Recently";
        long now = System.currentTimeMillis();
        long diff = now - createdAt;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days == 1 ? "Yesterday" : days + " days ago";
        } else if (hours > 0) {
            return hours + " hours ago";
        } else if (minutes > 0) {
            return minutes + " mins ago";
        } else {
            return "Just now";
        }
    }
}