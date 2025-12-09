package org.example.educonnec_dacs4.model;

public class SystemLog {
    private int logId;
    private int userId;
    private String action;
    private String ipAddress;
    private String createdAt;

    // Constructors
    public SystemLog() {}
    public SystemLog(int logId, int userId, String action, String ipAddress, String createdAt) {
        this.logId = logId; this.userId = userId; this.action = action;
        this.ipAddress = ipAddress; this.createdAt = createdAt;
    }

    // Getters
    public int getLogId() { return logId; }
    public int getUserId() { return userId; }
    public String getAction() { return action; }
    public String getIpAddress() { return ipAddress; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setLogId(int logId) { this.logId = logId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setAction(String action) { this.action = action; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}