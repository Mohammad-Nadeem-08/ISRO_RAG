package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConflictDetectionService.
 * Tests conflict detection between evidence items.
 */
class ConflictDetectionServiceTest {

    private ConflictDetectionService service;

    @BeforeEach
    void setUp() {
        service = new ConflictDetectionService();
    }

    @Test
    void testDetectConflicts_NoConflicts() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createEvidence(1L, "The temperature must be maintained at 25 degrees."));
        evidence.add(createEvidence(2L, "The pressure should be 1 atmosphere."));

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void testDetectConflicts_NegationConflict() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createEvidence(1L, "The system is required for operation."));
        evidence.add(createEvidence(2L, "The system is not required for operation."));

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertFalse(conflicts.isEmpty());
        assertEquals("NEGATION", conflicts.get(0).getConflictType());
        assertTrue(conflicts.get(0).getConfidenceScore() > 0.5f);
    }

    @Test
    void testDetectConflicts_DirectionalConflict() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createEvidence(1L, "We should increase the fuel capacity."));
        evidence.add(createEvidence(2L, "We should decrease the fuel capacity."));

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertFalse(conflicts.isEmpty());
        assertEquals("DIRECTIONAL", conflicts.get(0).getConflictType());
    }

    @Test
    void testDetectConflicts_QuantitativeConflict() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createEvidence(1L, "The minimum thrust required is 500kN."));
        evidence.add(createEvidence(2L, "The maximum thrust allowed is 400kN."));

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertFalse(conflicts.isEmpty());
        assertEquals("QUANTITATIVE", conflicts.get(0).getConflictType());
    }

    @Test
    void testDetectConflicts_TemporalConflict() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createEvidence(1L, "The launch must occur before 2025."));
        evidence.add(createEvidence(2L, "The launch must occur after 2026."));

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertFalse(conflicts.isEmpty());
        assertEquals("TEMPORAL", conflicts.get(0).getConflictType());
    }

    @Test
    void testDetectConflicts_MultipleConflicts() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createEvidence(1L, "The value is required."));
        evidence.add(createEvidence(2L, "The value is not required."));
        evidence.add(createEvidence(3L, "We must increase the speed."));
        evidence.add(createEvidence(4L, "We must decrease the speed."));

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertTrue(conflicts.size() >= 2);
    }

    @Test
    void testDetectConflicts_EmptyList() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void testDetectConflicts_SingleItem() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createEvidence(1L, "Single evidence item."));

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void testResolveConflicts_NoConflicts() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createEvidenceWithScore(1L, "Evidence 1", 0.9f));
        evidence.add(createEvidenceWithScore(2L, "Evidence 2", 0.8f));

        List<ConflictDetectionService.EvidenceConflict> conflicts = new ArrayList<>();

        List<RetrievalResultDTO> resolved = service.resolveConflicts(evidence, conflicts);

        assertEquals(2, resolved.size());
    }

    @Test
    void testResolveConflicts_KeepsHigherRelevance() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        RetrievalResultDTO highRelevance = createEvidenceWithScore(1L, "The system is required.", 0.95f);
        RetrievalResultDTO lowRelevance = createEvidenceWithScore(2L, "The system is not required.", 0.7f);
        evidence.add(highRelevance);
        evidence.add(lowRelevance);

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        List<RetrievalResultDTO> resolved = service.resolveConflicts(evidence, conflicts);

        // Should keep higher relevance evidence
        assertTrue(resolved.size() <= evidence.size());
        assertTrue(resolved.stream().anyMatch(e -> e.getChunkId().equals(1L)));
    }

    @Test
    void testEvidenceConflict_GettersAndSetters() {
        ConflictDetectionService.EvidenceConflict conflict = new ConflictDetectionService.EvidenceConflict();

        conflict.setChunkId1(1L);
        conflict.setChunkId2(2L);
        conflict.setRevisionId1(10L);
        conflict.setRevisionId2(20L);
        conflict.setConflictType("NEGATION");
        conflict.setDescription("Test conflict description");
        conflict.setConfidenceScore(0.85f);

        assertEquals(1L, conflict.getChunkId1());
        assertEquals(2L, conflict.getChunkId2());
        assertEquals(10L, conflict.getRevisionId1());
        assertEquals(20L, conflict.getRevisionId2());
        assertEquals("NEGATION", conflict.getConflictType());
        assertEquals("Test conflict description", conflict.getDescription());
        assertEquals(0.85f, conflict.getConfidenceScore(), 0.001);
    }

    @Test
    void testDetectConflicts_NullContent() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        RetrievalResultDTO evidence1 = new RetrievalResultDTO();
        evidence1.setChunkId(1L);
        evidence1.setContent(null);

        RetrievalResultDTO evidence2 = new RetrievalResultDTO();
        evidence2.setChunkId(2L);
        evidence2.setContent("Some content");

        evidence.add(evidence1);
        evidence.add(evidence2);

        // Should not throw exception
        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertNotNull(conflicts);
    }

    @Test
    void testDetectConflicts_CaseInsensitive() {
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createEvidence(1L, "THE SYSTEM IS REQUIRED."));
        evidence.add(createEvidence(2L, "the system is not required."));

        List<ConflictDetectionService.EvidenceConflict> conflicts = service.detectConflicts(evidence);

        assertFalse(conflicts.isEmpty());
        assertEquals("NEGATION", conflicts.get(0).getConflictType());
    }

    private RetrievalResultDTO createEvidence(Long chunkId, String content) {
        return createEvidenceWithScore(chunkId, content, 0.8f);
    }

    private RetrievalResultDTO createEvidenceWithScore(Long chunkId, String content, float score) {
        RetrievalResultDTO dto = new RetrievalResultDTO();
        dto.setChunkId(chunkId);
        dto.setContent(content);
        dto.setRelevanceScore(score);
        dto.setRevisionId(1L);
        return dto;
    }
}
