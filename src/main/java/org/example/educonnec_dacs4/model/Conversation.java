package org.example.educonnec_dacs4.model;

public class Conversation {
    private int conversationId;
    private int userId; // Creator ID
    private String nameconversation;
    private String type; // ENUM: 'private', 'group'
    private String createdAt;

    // Constructors
    public Conversation() {}
    public Conversation(int conversationId, int userId, String nameconversation, String type, String createdAt) {
        this.conversationId = conversationId; this.userId = userId; this.nameconversation = nameconversation;
        this.type = type; this.createdAt = createdAt;
    }

    // Getters
    public int getConversationId() { return conversationId; }
    public int getUserId() { return userId; }
    public String getNameconversation() { return nameconversation; }
    public String getType() { return type; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setNameconversation(String nameconversation) { this.nameconversation = nameconversation; }
    public void setType(String type) { this.type = type; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}