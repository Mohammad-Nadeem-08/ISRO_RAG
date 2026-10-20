package com.spacedoc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "requirements")
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String requirementKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_revision_id", nullable = false)
    private DocumentRevision documentRevision;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requirementText;

    @Column(length = 100)
    private String requirementType;

    private Boolean isSuperseded = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Requirement() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequirementKey() { return requirementKey; }
    public void setRequirementKey(String requirementKey) { this.requirementKey = requirementKey; }

    public DocumentRevision getDocumentRevision() { return documentRevision; }
    public void setDocumentRevision(DocumentRevision documentRevision) { this.documentRevision = documentRevision; }

    public String getRequirementText() { return requirementText; }
    public void setRequirementText(String requirementText) { this.requirementText = requirementText; }

    public String getRequirementType() { return requirementType; }
    public void setRequirementType(String requirementType) { this.requirementType = requirementType; }

    public Boolean getIsSuperseded() { return isSuperseded; }
    public void setIsSuperseded(Boolean isSuperseded) { this.isSuperseded = isSuperseded; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
