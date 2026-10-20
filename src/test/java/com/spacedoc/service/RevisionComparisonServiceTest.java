package com.spacedoc.service;

import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentRevision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RevisionComparisonService.
 * Tests revision delta computation and DTO properties.
 */
class RevisionComparisonServiceTest {

    @Test
    void testRevisionDelta_GettersAndSetters() {
        RevisionComparisonService.RevisionDelta delta = new RevisionComparisonService.RevisionDelta();

        delta.setFromRevisionNumber(1);
        delta.setToRevisionNumber(2);
        delta.setFromRevisionKey("REV-001");
        delta.setToRevisionKey("REV-002");
        delta.setSizeDifference(500L);
        delta.setPercentageChange(25.0);
        delta.setChunkCountChange(5);
        delta.setContentChanged(true);
        delta.setChangeSummary("Major update");

        assertEquals(1, delta.getFromRevisionNumber());
        assertEquals(2, delta.getToRevisionNumber());
        assertEquals("REV-001", delta.getFromRevisionKey());
        assertEquals("REV-002", delta.getToRevisionKey());
        assertEquals(500L, delta.getSizeDifference());
        assertEquals(25.0, delta.getPercentageChange(), 0.001);
        assertEquals(5, delta.getChunkCountChange());
        assertTrue(delta.isContentChanged());
        assertEquals("Major update", delta.getChangeSummary());
    }

    @Test
    void testRevisionChangePoint_GettersAndSetters() {
        RevisionComparisonService.RevisionChangePoint point = new RevisionComparisonService.RevisionChangePoint();

        LocalDateTime now = LocalDateTime.now();
        point.setFromRevisionNumber(1);
        point.setToRevisionNumber(2);
        point.setChangeType("EXPANSION");
        point.setMagnitude(50.0);
        point.setChangedAt(now);

        assertEquals(1, point.getFromRevisionNumber());
        assertEquals(2, point.getToRevisionNumber());
        assertEquals("EXPANSION", point.getChangeType());
        assertEquals(50.0, point.getMagnitude(), 0.001);
        assertEquals(now, point.getChangedAt());
    }

    @Test
    void testRevisionDelta_DefaultValues() {
        RevisionComparisonService.RevisionDelta delta = new RevisionComparisonService.RevisionDelta();

        assertEquals(0, delta.getFromRevisionNumber());
        assertEquals(0, delta.getToRevisionNumber());
        assertNull(delta.getFromRevisionKey());
        assertNull(delta.getToRevisionKey());
        assertEquals(0L, delta.getSizeDifference());
        assertEquals(0.0, delta.getPercentageChange(), 0.001);
        assertEquals(0, delta.getChunkCountChange());
        assertFalse(delta.isContentChanged());
        assertNull(delta.getChangeSummary());
    }

    @Test
    void testRevisionChangePoint_DefaultValues() {
        RevisionComparisonService.RevisionChangePoint point = new RevisionComparisonService.RevisionChangePoint();

        assertEquals(0, point.getFromRevisionNumber());
        assertEquals(0, point.getToRevisionNumber());
        assertNull(point.getChangeType());
        assertEquals(0.0, point.getMagnitude(), 0.001);
        assertNull(point.getChangedAt());
    }

    @Test
    void testRevisionDelta_PositiveSizeChange() {
        RevisionComparisonService.RevisionDelta delta = new RevisionComparisonService.RevisionDelta();
        delta.setSizeDifference(1500L);
        delta.setPercentageChange(75.5);

        assertEquals(1500L, delta.getSizeDifference());
        assertEquals(75.5, delta.getPercentageChange(), 0.01);
    }

    @Test
    void testRevisionChangePoint_ContractionType() {
        RevisionComparisonService.RevisionChangePoint point = new RevisionComparisonService.RevisionChangePoint();
        point.setChangeType("CONTRACTION");
        point.setMagnitude(30.0);

        assertEquals("CONTRACTION", point.getChangeType());
        assertEquals(30.0, point.getMagnitude(), 0.001);
    }
}
