package com.crisscross.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;
    private int notificationId;
    private String title;
    private String message;
    private String priority;   // 'INFO', 'WARNING', 'URGENT'
    private String targetRole; // 'ALL', 'CUSTOMER', 'ADMIN'
    private LocalDateTime createdAt;

    public Notification() {
        this.priority = "INFO";
        this.targetRole = "ALL";
        this.createdAt = LocalDateTime.now();
    }

    public Notification(int notificationId, String title, String message, String priority, String targetRole, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.priority = priority != null ? priority : "INFO";
        this.targetRole = targetRole != null ? targetRole : "ALL";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "[" + priority + "] " + title;
    }
}
