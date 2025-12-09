package org.example.educonnec_dacs4.model;

public class ConversationMember {
    private int conversationMemberId;
    private int conversationId;
    private int userId;
    private String role; // ENUM: 'member', 'admin'
    private String joinedAt;

    // Constructors
    public ConversationMember() {}
    public ConversationMember(int id, int convId, int userId, String role, String joinedAt) {
        this.conversationMemberId = id; this.conversationId = convId; this.userId = userId;
        this.role = role; this.joinedAt = joinedAt;
    }

    // Getters
    public int getConversationMemberId() { return conversationMemberId; }
    public int getConversationId() { return conversationId; }
    public int getUserId() { return userId; }
    public String getRole() { return role; }
    public String getJoinedAt() { return joinedAt; }

    // Setters
    public void setConversationMemberId(int conversationMemberId) { this.conversationMemberId = conversationMemberId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setRole(String role) { this.role = role; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }
}