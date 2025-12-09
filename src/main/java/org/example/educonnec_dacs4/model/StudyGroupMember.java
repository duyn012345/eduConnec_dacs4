package org.example.educonnec_dacs4.model;

public class StudyGroupMember {
    private int studyGroupMemberId;
    private int studyGroupId;
    private int userId;
    private String joinedAt;

    // Constructors
    public StudyGroupMember() {}
    public StudyGroupMember(int id, int groupId, int userId, String joinedAt) {
        this.studyGroupMemberId = id; this.studyGroupId = groupId; this.userId = userId; this.joinedAt = joinedAt;
    }

    // Getters
    public int getStudyGroupMemberId() { return studyGroupMemberId; }
    public int getStudyGroupId() { return studyGroupId; }
    public int getUserId() { return userId; }
    public String getJoinedAt() { return joinedAt; }

    // Setters
    public void setStudyGroupMemberId(int studyGroupMemberId) { this.studyGroupMemberId = studyGroupMemberId; }
    public void setStudyGroupId(int studyGroupId) { this.studyGroupId = studyGroupId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }
}