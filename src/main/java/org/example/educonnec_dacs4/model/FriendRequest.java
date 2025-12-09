package org.example.educonnec_dacs4.model;

public class FriendRequest {
    private int friendRequestId;
    private int requesterId;
    private int receiverId;
    private String status; // ENUM: 'pending', 'accepted', 'blocked'
    private String createdAt;

    // Constructors
    public FriendRequest() {}
    public FriendRequest(int friendRequestId, int requesterId, int receiverId, String status, String createdAt) {
        this.friendRequestId = friendRequestId; this.requesterId = requesterId; this.receiverId = receiverId;
        this.status = status; this.createdAt = createdAt;
    }

    // Getters
    public int getFriendRequestId() { return friendRequestId; }
    public int getRequesterId() { return requesterId; }
    public int getReceiverId() { return receiverId; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setFriendRequestId(int friendRequestId) { this.friendRequestId = friendRequestId; }
    public void setRequesterId(int requesterId) { this.requesterId = requesterId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
