package org.example.educonnec_dacs4.model;

public class Message {
    private int messageId;
    private int conversationId;
    private int senderId; // Đã đổi tên từ user_id
    private String content;
    private String fileUrl;
    private String messageType; // ENUM: 'text', 'image', 'file'
    private String createdAt;

    // Constructor mặc định (cần thiết cho các DAO)
    public Message() {}

    // Constructor đầy đủ
    public Message(int messageId, int conversationId, int senderId, String content, String fileUrl, String messageType, String createdAt) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.fileUrl = fileUrl;
        this.messageType = messageType;
        this.createdAt = createdAt;
    }

    // Getters
    public int getMessageId() { return messageId; }
    public int getConversationId() { return conversationId; }
    public int getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getFileUrl() { return fileUrl; }
    public String getMessageType() { return messageType; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setMessageId(int messageId) { this.messageId = messageId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public void setContent(String content) { this.content = content; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}