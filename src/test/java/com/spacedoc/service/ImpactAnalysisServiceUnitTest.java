package com.spacedoc.service;

import com.spacedoc.dto.ImpactAnalysisDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ImpactAnalysisDTO structure.
 * Tests the DTO fields without requiring Spring context or database.
 */
class ImpactAnalysisServiceUnitTest {

    @Test
    void testImpactAnalysisDTO_AllFields() {
        ImpactAnalysisDTO dto = new ImpactAnalysisDTO();

        dto.setSourceDocumentId(1L);
        dto.setSourceDocumentKey("DOC-001");
        dto.setSourceDocumentTitle("Test Document");
        dto.setSourceRevisionNumber(2);
        dto.setChangeDescription("Updated propulsion requirements");
        dto.setAnalysisMethod("dependency-graph-traversal");
        dto.setTraversalDepth(3);
        dto.setAnalysisDurationMs(150L);

        assertEquals(1L, dto.getSourceDocumentId());
        assertEquals("DOC-001", dto.getSourceDocumentKey());
        assertEquals("Test Document", dto.getSourceDocumentTitle());
        assertEquals(2, dto.getSourceRevisionNumber());
        assertEquals("Updated propulsion requirements", dto.getChangeDescription());
        assertEquals("dependency-graph-traversal", dto.getAnalysisMethod());
        assertEquals(3, dto.getTraversalDepth());
        assertEquals(150L, dto.getAnalysisDurationMs());
        assertNotNull(dto.getAnalysisTimestamp());
    }

    @Test
    void testImpactAnalysisDTO_DefaultValues() {
        ImpactAnalysisDTO dto = new ImpactAnalysisDTO();

        assertEquals(0, dto.getTotalDirectImpacts());
        assertEquals(0, dto.getTotalIndirectImpacts());
        assertEquals(0, dto.getTotalCandidates());
        assertEquals(0, dto.getTotalImpactedDocuments());
        assertFalse(dto.getRequiresHumanReview());
        assertNotNull(dto.getDirectlyImpactedDocuments());
        assertNotNull(dto.getIndirectlyImpactedDocuments());
        assertNotNull(dto.getSemanticSimilarityCandidates());
    }

    @Test
    void testImpactedDocument_AllFields() {
        ImpactAnalysisDTO.ImpactedDocument impacted = new ImpactAnalysisDTO.ImpactedDocument();

        impacted.setDocumentId(5L);
        impacted.setDocumentKey("DOC-005");
        impacted.setDocumentTitle("Related Document");
        impacted.setRelationshipType("REFERENCES");
        impacted.setImpactReason("Document explicitly references source requirement");
        impacted.setConfidenceScore(0.92f);
        impacted.setEvidenceText("As specified in DOC-001 section 3.2...");
        impacted.setEvidenceChunkId(100L);
        impacted.setEvidenceSectionTitle("Propulsion System");
        impacted.setIsConfirmed(true);
        impacted.setRequiresReview(false);

        assertEquals(5L, impacted.getDocumentId());
        assertEquals("DOC-005", impacted.getDocumentKey());
        assertEquals("Related Document", impacted.getDocumentTitle());
        assertEquals("REFERENCES", impacted.getRelationshipType());
        assertEquals("Document explicitly references source requirement", impacted.getImpactReason());
        assertEquals(0.92f, impacted.getConfidenceScore(), 0.01f);
        assertEquals("As specified in DOC-001 section 3.2...", impacted.getEvidenceText());
        assertEquals(100L, impacted.getEvidenceChunkId());
        assertEquals("Propulsion System", impacted.getEvidenceSectionTitle());
        assertTrue(impacted.getIsConfirmed());
        assertFalse(impacted.getRequiresReview());
    }

    @Test
    void testImpactedDocument_DefaultValues() {
        ImpactAnalysisDTO.ImpactedDocument impacted = new ImpactAnalysisDTO.ImpactedDocument();

        assertFalse(impacted.getIsConfirmed());
        assertFalse(impacted.getRequiresReview());
        assertNotNull(impacted.getDependencyPath());
        assertTrue(impacted.getDependencyPath().isEmpty());
    }

    @Test
    void testImpactAnalysisDTO_SetListsUpdatesTotals() {
        ImpactAnalysisDTO dto = new ImpactAnalysisDTO();

        ImpactAnalysisDTO.ImpactedDocument doc1 = new ImpactAnalysisDTO.ImpactedDocument();
        doc1.setDocumentId(2L);

        ImpactAnalysisDTO.ImpactedDocument doc2 = new ImpactAnalysisDTO.ImpactedDocument();
        doc2.setDocumentId(3L);

        dto.setDirectlyImpactedDocuments(java.util.Arrays.asList(doc1, doc2));

        assertEquals(2, dto.getTotalDirectImpacts());
        assertEquals(2, dto.getTotalImpactedDocuments());
    }

    @Test
    void testDependencyGraphService_Constants() {
        assertEquals("REFERENCES", DependencyGraphService.REFERENCES);
        assertEquals("DEPENDS_ON", DependencyGraphService.DEPENDS_ON);
        assertEquals("SUPERSEDES", DependencyGraphService.SUPERSEDES);
        assertEquals("CLARIFIES", DependencyGraphService.CLARIFIES);
        assertEquals("CONTRADICTS", DependencyGraphService.CONTRADICTS);
    }
}
