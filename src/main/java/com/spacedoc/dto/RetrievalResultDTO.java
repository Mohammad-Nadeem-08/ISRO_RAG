package com.spacedoc.dto;

public class RetrievalResultDTO {
    private Long chunkId;
    private Long revisionId;
    private Long documentId;
    private String documentKey;
    private String documentTitle;
    private String content;
    private String sectionTitle;
    private Integer pageNumber;
    private Float relevanceScore;
    private String revisionApplicability;
    private String conflictStatus;
    private Float applicabilityScore;
    private String applicabilityReason;

    public RetrievalResultDTO() {}

    public RetrievalResultDTO(Long chunkId, Long revisionId, String content, String sectionTitle,
                              Integer pageNumber, Float relevanceScore, String revisionApplicability,
                              String conflictStatus) {
        this.chunkId = chunkId;
        this.revisionId = revisionId;
        this.content = content;
        this.sectionTitle = sectionTitle;
        this.pageNumber = pageNumber;
        this.relevanceScore = relevanceScore;
        this.revisionApplicability = revisionApplicability;
        this.conflictStatus = conflictStatus;
    }

    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }

    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentKey() { return documentKey; }
    public void setDocumentKey(String documentKey) { this.documentKey = documentKey; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public Float getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Float relevanceScore) { this.relevanceScore = relevanceScore; }

    public String getRevisionApplicability() { return revisionApplicability; }
    public void setRevisionApplicability(String revisionApplicability) { this.revisionApplicability = revisionApplicability; }

    public String getConflictStatus() { return conflictStatus; }
    public void setConflictStatus(String conflictStatus) { this.conflictStatus = conflictStatus; }

    public Float getApplicabilityScore() { return applicabilityScore; }
    public void setApplicabilityScore(Float applicabilityScore) { this.applicabilityScore = applicabilityScore; }

    public String getApplicabilityReason() { return applicabilityReason; }
    public void setApplicabilityReason(String applicabilityReason) { this.applicabilityReason = applicabilityReason; }
}
