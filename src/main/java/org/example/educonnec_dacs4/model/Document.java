package org.example.educonnec_dacs4.model;

public class Document {
    private int documentId;
    private int userId;
    private Integer conversationId; // Dùng Integer vì có thể NULL
    private Integer studyGroupId;   // Dùng Integer vì có thể NULL
    private String filename;
    private String filePath;
    private String fileType;
    private String createdAt;

    // Constructors
    public Document() {}
    public Document(int id, int userId, Integer convId, Integer groupId, String filename, String filePath, String fileType, String createdAt) {
        this.documentId = id; this.userId = userId; this.conversationId = convId; this.studyGroupId = groupId;
        this.filename = filename; this.filePath = filePath; this.fileType = fileType; this.createdAt = createdAt;
    }

    // Getters
    public int getDocumentId() { return documentId; }
    public int getUserId() { return userId; }
    public Integer getConversationId() { return conversationId; }
    public Integer getStudyGroupId() { return studyGroupId; }
    public String getFilename() { return filename; }
    public String getFilePath() { return filePath; }
    public String getFileType() { return fileType; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setDocumentId(int documentId) { this.documentId = documentId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setConversationId(Integer conversationId) { this.conversationId = conversationId; }
    public void setStudyGroupId(Integer studyGroupId) { this.studyGroupId = studyGroupId; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}