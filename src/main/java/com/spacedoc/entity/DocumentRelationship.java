package com.spacedoc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_relationships")
public class DocumentRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_revision_id", nullable = false)
    private DocumentRevision sourceRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_document_revision_id")
    private DocumentRevision targetRevision;

    @Column(length = 100)
    private String relationshipType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_chunk_id")
    private DocumentChunk evidenceChunk;

    private Boolean isExplicit = true;

    @Column(columnDefinition = "TEXT")
    private String supportingText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public DocumentRelationship() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DocumentRevision getSourceRevision() { return sourceRevision; }
    public void setSourceRevision(DocumentRevision sourceRevision) { this.sourceRevision = sourceRevision; }

    public DocumentRevision getTargetRevision() { return targetRevision; }
    public void setTargetRevision(DocumentRevision targetRevision) { this.targetRevision = targetRevision; }

    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

    public DocumentChunk getEvidenceChunk() { return evidenceChunk; }
    public void setEvidenceChunk(DocumentChunk evidenceChunk) { this.evidenceChunk = evidenceChunk; }

    public Boolean getIsExplicit() { return isExplicit; }
    public void setIsExplicit(Boolean isExplicit) { this.isExplicit = isExplicit; }

    public String getSupportingText() { return supportingText; }
    public void setSupportingText(String supportingText) { this.supportingText = supportingText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
