package org.example.educonnec_dacs4.model;

public class FriendRequest {
    private int friendRequestId;
    private int requesterId;
    private int receiverId;
    private String status; // pending, accepted, blocked

    public FriendRequest() {}

    public int getFriendRequestId() { return friendRequestId; }
    public int getRequesterId() { return requesterId; }
    public int getReceiverId() { return receiverId; }
    public String getStatus() { return status; }
}

