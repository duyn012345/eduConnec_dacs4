package org.example.educonnec_dacs4.model;

public class StudyGroup {
    private int studyGroupId;
    private int userId; // Creator ID
    private String namegroup;
    private String createdAt;

    // Constructors
    public StudyGroup() {}
    public StudyGroup(int id, int userId, String namegroup, String createdAt) {
        this.studyGroupId = id; this.userId = userId; this.namegroup = namegroup; this.createdAt = createdAt;
    }

    // Getters
    public int getStudyGroupId() { return studyGroupId; }
    public int getUserId() { return userId; }
    public String getNamegroup() { return namegroup; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setStudyGroupId(int studyGroupId) { this.studyGroupId = studyGroupId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setNamegroup(String namegroup) { this.namegroup = namegroup; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}