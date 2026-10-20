package com.spacedoc.service;

import com.spacedoc.dto.ImpactAnalysisDTO;
import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentRevision;
import com.spacedoc.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ImpactAnalysisService.
 * Note: These tests verify the service structure and basic functionality
 * without mocking, as Mockito has issues with Java 25's module system.
 * Runs with H2 in-memory database via @ActiveProfiles("test")
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class ImpactAnalysisServiceTest {

    @Autowired(required = false)
    private ImpactAnalysisService impactAnalysisService;

    @Test
    void testServiceInjection() {
        // Service should be injected by Spring
        assertNotNull(impactAnalysisService, "ImpactAnalysisService should be injectable");
    }

    @Test
    void testAnalyzeImpact_DocumentNotFound() {
        if (impactAnalysisService == null) {
            return; // Skip if not injected
        }

        ImpactAnalysisDTO result = impactAnalysisService.analyzeImpact(99999L, null, "Test change");

        assertNotNull(result);
        assertEquals(0, result.getTotalImpactedDocuments());
        assertTrue(result.getReviewNotes() != null && result.getReviewNotes().contains("not found"));
    }

    @Test
    void testImpactAnalysisDTO_Fields() {
        ImpactAnalysisDTO dto = new ImpactAnalysisDTO();

        dto.setSourceDocumentId(1L);
        dto.setSourceDocumentKey("DOC-001");
        dto.setSourceDocumentTitle("Test Document");
        dto.setSourceRevisionNumber(1);
        dto.setChangeDescription("Test change");
        dto.setAnalysisMethod("dependency-graph-traversal");
        dto.setTraversalDepth(3);
        dto.setAnalysisDurationMs(100L);

        assertEquals(1L, dto.getSourceDocumentId());
        assertEquals("DOC-001", dto.getSourceDocumentKey());
        assertEquals("Test Document", dto.getSourceDocumentTitle());
        assertEquals(1, dto.getChangeDescription());
        assertEquals("Test change", dto.getChangeDescription());
        assertEquals("dependency-graph-traversal", dto.getAnalysisMethod());
        assertEquals(3, dto.getTraversalDepth());
        assertEquals(100L, dto.getAnalysisDurationMs());
    }

    @Test
    void testImpactedDocument_Fields() {
        ImpactAnalysisDTO.ImpactedDocument impacted = new ImpactAnalysisDTO.ImpactedDocument();

        impacted.setDocumentId(2L);
        impacted.setDocumentKey("DOC-002");
        impacted.setDocumentTitle("Impacted Document");
        impacted.setRelationshipType("REFERENCES");
        impacted.setImpactReason("Document directly references source");
        impacted.setConfidenceScore(0.85f);
        impacted.setIsConfirmed(true);
        impacted.setRequiresReview(false);

        assertEquals(2L, impacted.getDocumentId());
        assertEquals("DOC-002", impacted.getDocumentKey());
        assertEquals("Impacted Document", impacted.getDocumentTitle());
        assertEquals("REFERENCES", impacted.getRelationshipType());
        assertEquals("Document directly references source", impacted.getImpactReason());
        assertEquals(0.85f, impacted.getConfidenceScore());
        assertTrue(impacted.getIsConfirmed());
        assertFalse(impacted.getRequiresReview());
    }
}
