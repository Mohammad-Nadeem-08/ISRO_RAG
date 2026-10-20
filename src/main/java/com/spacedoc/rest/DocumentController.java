package com.spacedoc.rest;

import com.spacedoc.dto.DocumentDTO;
import com.spacedoc.entity.Document;
import com.spacedoc.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentRepository documentRepository;

    public DocumentController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * List all documents with pagination
     * GET /api/documents?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Document> documents = documentRepository.findAll(pageable);

            List<DocumentDTO> dtos = documents.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("content", dtos);
            response.put("totalElements", documents.getTotalElements());
            response.put("totalPages", documents.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error listing documents", e);
            return ResponseEntity.status(500).body(errorResponse("Failed to list documents"));
        }
    }

    /**
     * Get document by ID
     * GET /api/documents/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
        try {
            return documentRepository.findById(id)
                .map(doc -> ResponseEntity.ok((Object) convertToDTO(doc)))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching document", e);
            return ResponseEntity.status(500).body(errorResponse("Failed to fetch document"));
        }
    }

    /**
     * Search documents by title or key
     * GET /api/documents/search?q=query
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDocuments(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            if (q == null || q.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(errorResponse("Search query cannot be empty"));
            }

            // Placeholder: would use repository search method
            List<DocumentDTO> results = documentRepository.findAll().stream()
                .filter(d -> d.getTitle().toLowerCase().contains(q.toLowerCase())
                         || d.getDocumentKey().toLowerCase().contains(q.toLowerCase()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("query", q);
            response.put("resultCount", results.size());
            response.put("results", results);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching documents", e);
            return ResponseEntity.status(500).body(errorResponse("Search failed"));
        }
    }

    /**
     * Get document revisions
     * GET /api/documents/{id}/revisions
     */
    @GetMapping("/{id}/revisions")
    public ResponseEntity<?> getDocumentRevisions(@PathVariable Long id) {
        try {
            return documentRepository.findById(id)
                .map(doc -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("documentId", doc.getId());
                    response.put("documentKey", doc.getDocumentKey());
                    response.put("title", doc.getTitle());
                    response.put("revisions", doc.getRevisions().size());
                    return ResponseEntity.ok((Object) response);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching revisions", e);
            return ResponseEntity.status(500).body(errorResponse("Failed to fetch revisions"));
        }
    }

    private DocumentDTO convertToDTO(Document doc) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(doc.getId());
        dto.setDocumentKey(doc.getDocumentKey());
        dto.setTitle(doc.getTitle());
        dto.setSourceUrl(doc.getSourceUrl());
        dto.setSourceType(doc.getSourceType());
        dto.setSourceOrganization(doc.getSourceOrganization());
        dto.setCurrentRevision(doc.getRevisions() != null && !doc.getRevisions().isEmpty()
            ? doc.getRevisions().size()
            : 0);
        dto.setTotalChunks(doc.getRevisions() != null && !doc.getRevisions().isEmpty()
            ? doc.getRevisions().iterator().next().getTotalChunks()
            : 0);
        return dto;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
