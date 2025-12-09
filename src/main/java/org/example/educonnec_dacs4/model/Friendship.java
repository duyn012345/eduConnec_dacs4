package org.example.educonnec_dacs4.model;

public class Friendship {
    private int friendshipId;
    private int user1Id;
    private int user2Id;
    private String createdAt;

    // Constructors
    public Friendship() {}
    public Friendship(int friendshipId, int user1Id, int user2Id, String createdAt) {
        this.friendshipId = friendshipId; this.user1Id = user1Id; this.user2Id = user2Id; this.createdAt = createdAt;
    }

    // Getters
    public int getFriendshipId() { return friendshipId; }
    public int getUser1Id() { return user1Id; }
    public int getUser2Id() { return user2Id; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setFriendshipId(int friendshipId) { this.friendshipId = friendshipId; }
    public void setUser1Id(int user1Id) { this.user1Id = user1Id; }
    public void setUser2Id(int user2Id) { this.user2Id = user2Id; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}