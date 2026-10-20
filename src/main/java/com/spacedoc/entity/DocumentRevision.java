package com.spacedoc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "document_revisions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"document_id", "revision_number"}),
    @UniqueConstraint(columnNames = {"revision_key"})
})
public class DocumentRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private Integer revisionNumber;

    @Column(unique = true, nullable = false, length = 255)
    private String revisionKey;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(columnDefinition = "TEXT")
    private String changeSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prior_revision_id")
    private DocumentRevision priorRevision;

    @Column(nullable = false)
    private Integer totalChunks = 0;

    private Long extractedTextLength;

    @Column(nullable = false)
    private Boolean isCurrent = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime ingestedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "documentRevision", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentChunk> chunks = new HashSet<>();

    @OneToMany(mappedBy = "documentRevision", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Requirement> requirements = new HashSet<>();

    @OneToMany(mappedBy = "sourceRevision", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentRelationship> sourceRelationships = new HashSet<>();

    @OneToMany(mappedBy = "targetRevision", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentRelationship> targetRelationships = new HashSet<>();

    // Constructors
    public DocumentRevision() {
        this.ingestedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public Integer getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(Integer revisionNumber) { this.revisionNumber = revisionNumber; }

    public String getRevisionKey() { return revisionKey; }
    public void setRevisionKey(String revisionKey) { this.revisionKey = revisionKey; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }

    public DocumentRevision getPriorRevision() { return priorRevision; }
    public void setPriorRevision(DocumentRevision priorRevision) { this.priorRevision = priorRevision; }

    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }

    public Long getExtractedTextLength() { return extractedTextLength; }
    public void setExtractedTextLength(Long extractedTextLength) { this.extractedTextLength = extractedTextLength; }

    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }

    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Set<DocumentChunk> getChunks() { return chunks; }
    public void setChunks(Set<DocumentChunk> chunks) { this.chunks = chunks; }

    public Set<Requirement> getRequirements() { return requirements; }
    public void setRequirements(Set<Requirement> requirements) { this.requirements = requirements; }

    public Set<DocumentRelationship> getSourceRelationships() { return sourceRelationships; }
    public void setSourceRelationships(Set<DocumentRelationship> sourceRelationships) { this.sourceRelationships = sourceRelationships; }

    public Set<DocumentRelationship> getTargetRelationships() { return targetRelationships; }
    public void setTargetRelationships(Set<DocumentRelationship> targetRelationships) { this.targetRelationships = targetRelationships; }
}
