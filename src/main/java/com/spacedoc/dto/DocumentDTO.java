package com.spacedoc.dto;

public class DocumentDTO {
    private Long id;
    private String documentKey;
    private String title;
    private String sourceUrl;
    private String sourceType;
    private String sourceOrganization;
    private Integer currentRevision;
    private Integer totalChunks;

    public DocumentDTO() {}

    public DocumentDTO(Long id, String documentKey, String title, String sourceUrl,
                      String sourceType, String sourceOrganization, Integer currentRevision, Integer totalChunks) {
        this.id = id;
        this.documentKey = documentKey;
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.sourceType = sourceType;
        this.sourceOrganization = sourceOrganization;
        this.currentRevision = currentRevision;
        this.totalChunks = totalChunks;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDocumentKey() { return documentKey; }
    public void setDocumentKey(String documentKey) { this.documentKey = documentKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceOrganization() { return sourceOrganization; }
    public void setSourceOrganization(String sourceOrganization) { this.sourceOrganization = sourceOrganization; }

    public Integer getCurrentRevision() { return currentRevision; }
    public void setCurrentRevision(Integer currentRevision) { this.currentRevision = currentRevision; }

    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
}
