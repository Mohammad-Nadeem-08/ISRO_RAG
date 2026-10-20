package com.spacedoc.entity;

import com.pgvector.PGvector;
import com.spacedoc.config.VectorUserType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_revision_id", nullable = false)
    private DocumentRevision documentRevision;

    @Column(nullable = false)
    private Integer chunkNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 500)
    private String sectionTitle;

    private Integer pageNumber;

    @Column(length = 50)
    private String sectionNumber;

    private Long startOffset;

    private Long endOffset;

    @Type(VectorUserType.class)
    @Column(columnDefinition = "vector(768)")
    private PGvector embedding;

    private Integer tokenCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public DocumentChunk() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DocumentRevision getDocumentRevision() { return documentRevision; }
    public void setDocumentRevision(DocumentRevision documentRevision) { this.documentRevision = documentRevision; }

    public Integer getChunkNumber() { return chunkNumber; }
    public void setChunkNumber(Integer chunkNumber) { this.chunkNumber = chunkNumber; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public String getSectionNumber() { return sectionNumber; }
    public void setSectionNumber(String sectionNumber) { this.sectionNumber = sectionNumber; }

    public Long getStartOffset() { return startOffset; }
    public void setStartOffset(Long startOffset) { this.startOffset = startOffset; }

    public Long getEndOffset() { return endOffset; }
    public void setEndOffset(Long endOffset) { this.endOffset = endOffset; }

    public PGvector getEmbedding() { return embedding; }
    public void setEmbedding(PGvector embedding) { this.embedding = embedding; }

    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
