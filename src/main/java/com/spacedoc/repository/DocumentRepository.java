package com.spacedoc.repository;

import com.spacedoc.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByDocumentKey(String documentKey);

    @Query("SELECT d FROM Document d WHERE d.id = ?1")
    Optional<Document> findById(Long id);
}
