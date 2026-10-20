package com.spacedoc.repository;

import com.spacedoc.entity.DocumentChunk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentRevisionId(Long revisionId);

    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.documentRevision.id = ?1 ORDER BY dc.chunkNumber ASC")
    List<DocumentChunk> findChunksByRevisionOrderByNumber(Long revisionId);

    /**
     * Lexical search using PostgreSQL full-text search (tsvector)
     * Searches in content and section_title fields
     */
    @Query(value = "SELECT dc.* FROM document_chunks dc " +
                   "WHERE to_tsvector('english', dc.content || ' ' || COALESCE(dc.section_title, '')) " +
                   "@@ plainto_tsquery('english', ?1) " +
                   "ORDER BY ts_rank(to_tsvector('english', dc.content || ' ' || COALESCE(dc.section_title, '')), " +
                   "plainto_tsquery('english', ?1)) DESC",
            nativeQuery = true)
    List<DocumentChunk> searchByFullText(String query, Pageable pageable);

    /**
     * Dense vector similarity search using pgvector
     * Searches for chunks with embeddings closest to query embedding
     * Note: Requires embedding column to have pgvector type
     */
    @Query(value = "SELECT dc.* FROM document_chunks dc " +
                   "WHERE dc.embedding IS NOT NULL " +
                   "ORDER BY dc.embedding <-> CAST(?1 AS vector) ASC",
            nativeQuery = true)
    List<DocumentChunk> findByEmbeddingSimilarity(String embeddingVector, Pageable pageable);

    /**
     * Find chunks by document revision with current flag
     */
    @Query("SELECT dc FROM DocumentChunk dc " +
           "JOIN dc.documentRevision dr " +
           "WHERE dr.isCurrent = true AND dr.document.id = ?1")
    List<DocumentChunk> findChunksByDocumentCurrentRevision(Long documentId);

    /**
     * Find all chunks for a document across all revisions
     */
    @Query("SELECT dc FROM DocumentChunk dc " +
           "JOIN dc.documentRevision dr " +
           "WHERE dr.document.id = ?1 " +
           "ORDER BY dr.revisionNumber DESC, dc.chunkNumber ASC")
    List<DocumentChunk> findAllChunksByDocument(Long documentId);

    // findByDocumentRevisionId is defined above (line 13)
}
