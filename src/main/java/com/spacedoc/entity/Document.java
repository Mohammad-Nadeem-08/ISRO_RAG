package com.spacedoc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String documentKey;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2048)
    private String sourceUrl;

    @Column(length = 50)
    private String sourceType;

    @Column(length = 255)
    private String sourceOrganization;

    private LocalDateTime publicationDate;

    @Column(length = 64)
    private String contentHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentRevision> revisions = new HashSet<>();

    @OneToMany(mappedBy = "sourceDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IngestionJob> ingestionJobs = new HashSet<>();

    // Constructors
    public Document() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Document(String documentKey, String title) {
        this.documentKey = documentKey;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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

    public LocalDateTime getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDateTime publicationDate) { this.publicationDate = publicationDate; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<DocumentRevision> getRevisions() { return revisions; }
    public void setRevisions(Set<DocumentRevision> revisions) { this.revisions = revisions; }

    public Set<IngestionJob> getIngestionJobs() { return ingestionJobs; }
    public void setIngestionJobs(Set<IngestionJob> ingestionJobs) { this.ingestionJobs = ingestionJobs; }
}
