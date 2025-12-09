package org.example.educonnec_dacs4.model;

// Dùng để đóng gói thông tin hiển thị lịch sử chat
public class MessageView {
    private String senderUsername;
    private String content;
    private String timeFormatted;

    // Constructor
    public MessageView(String senderUsername, String content, String timeFormatted) {
        this.senderUsername = senderUsername;
        this.content = content;
        this.timeFormatted = timeFormatted;
    }

    // Getters
    public String getSenderUsername() { return senderUsername; }
    public String getContent() { return content; }
    public String getTimeFormatted() { return timeFormatted; }

    // Setters (Nếu cần thay đổi, nhưng thường không cần cho View Model)
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public void setContent(String content) { this.content = content; }
    public void setTimeFormatted(String timeFormatted) { this.timeFormatted = timeFormatted; }
}