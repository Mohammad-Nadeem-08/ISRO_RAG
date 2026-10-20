package com.spacedoc.repository;

import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRevisionRepository extends JpaRepository<DocumentRevision, Long> {
    Optional<DocumentRevision> findByRevisionKey(String revisionKey);

    List<DocumentRevision> findByDocumentIdOrderByRevisionNumberDesc(Long documentId);

    List<DocumentRevision> findByDocumentOrderByRevisionNumberDesc(Document document);

    List<DocumentRevision> findByDocumentOrderByRevisionNumberAsc(Document document);

    @Query("SELECT dr FROM DocumentRevision dr WHERE dr.document.id = ?1 AND dr.isCurrent = true")
    Optional<DocumentRevision> findCurrentRevision(Long documentId);
}
