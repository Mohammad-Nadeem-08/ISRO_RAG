package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EvidenceRoutingService.
 * Tests RoutedEvidenceSet and ApplicabilityScore DTOs.
 */
class EvidenceRoutingServiceTest {

    @Test
    void testRoutedEvidenceSet_GettersAndSetters() {
        EvidenceRoutingService.RoutedEvidenceSet set = new EvidenceRoutingService.RoutedEvidenceSet();

        set.setQuery("test query");
        set.setTargetRevisionId(1L);
        set.setTargetRevisionNumber(3);
        set.setTotalEvidenceCount(10);
        set.setApplicableEvidenceCount(8);
        set.setConflictCount(2);

        List<RetrievalResultDTO> routed = new ArrayList<>();
        set.setRoutedEvidence(routed);

        List<ConflictDetectionService.EvidenceConflict> conflicts = new ArrayList<>();
        set.setDetectedConflicts(conflicts);

        assertEquals("test query", set.getQuery());
        assertEquals(1L, set.getTargetRevisionId());
        assertEquals(3, set.getTargetRevisionNumber());
        assertEquals(10, set.getTotalEvidenceCount());
        assertEquals(8, set.getApplicableEvidenceCount());
        assertEquals(2, set.getConflictCount());
        assertNotNull(set.getRoutedEvidence());
        assertNotNull(set.getDetectedConflicts());
    }

    @Test
    void testRoutedEvidenceSet_DefaultValues() {
        EvidenceRoutingService.RoutedEvidenceSet set = new EvidenceRoutingService.RoutedEvidenceSet();

        assertNull(set.getQuery());
        assertNull(set.getTargetRevisionId());
        assertNull(set.getTargetRevisionNumber());
        assertNull(set.getTotalEvidenceCount());
        assertNull(set.getApplicableEvidenceCount());
        assertNull(set.getRoutedEvidence());
        assertNull(set.getDetectedConflicts());
        assertNull(set.getConflictCount());
    }

    @Test
    void testRoutedEvidenceSet_ZeroValues() {
        EvidenceRoutingService.RoutedEvidenceSet set = new EvidenceRoutingService.RoutedEvidenceSet();

        set.setTotalEvidenceCount(0);
        set.setApplicableEvidenceCount(0);
        set.setConflictCount(0);

        assertEquals(0, set.getTotalEvidenceCount());
        assertEquals(0, set.getApplicableEvidenceCount());
        assertEquals(0, set.getConflictCount());
    }

    @Test
    void testRoutedEvidenceSet_LargeEvidenceCount() {
        EvidenceRoutingService.RoutedEvidenceSet set = new EvidenceRoutingService.RoutedEvidenceSet();

        set.setTotalEvidenceCount(10000);
        set.setApplicableEvidenceCount(8500);

        assertEquals(10000, set.getTotalEvidenceCount());
        assertEquals(8500, set.getApplicableEvidenceCount());
    }

    @Test
    void testRoutedEvidenceSet_WithNullEvidence() {
        EvidenceRoutingService.RoutedEvidenceSet set = new EvidenceRoutingService.RoutedEvidenceSet();

        set.setRoutedEvidence(null);
        set.setDetectedConflicts(null);

        assertNull(set.getRoutedEvidence());
        assertNull(set.getDetectedConflicts());
    }

    @Test
    void testRoutedEvidenceSet_RevisionTracking() {
        EvidenceRoutingService.RoutedEvidenceSet set = new EvidenceRoutingService.RoutedEvidenceSet();

        set.setTargetRevisionId(42L);
        set.setTargetRevisionNumber(5);

        assertEquals(42L, set.getTargetRevisionId());
        assertEquals(5, set.getTargetRevisionNumber());
    }

    @Test
    void testRoutedEvidenceSet_QueryPreservation() {
        EvidenceRoutingService.RoutedEvidenceSet set = new EvidenceRoutingService.RoutedEvidenceSet();

        String complexQuery = "What are the requirements for satellite propulsion systems?";
        set.setQuery(complexQuery);

        assertEquals(complexQuery, set.getQuery());
    }
}
