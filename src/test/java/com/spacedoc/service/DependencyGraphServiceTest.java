package com.spacedoc.service;

import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentRevision;
import com.spacedoc.repository.DocumentRevisionRepository;
import com.spacedoc.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DependencyGraphService.
 * Uses Spring injection instead of mocking due to Java 25 module system constraints.
 * Runs with H2 in-memory database via @ActiveProfiles("test")
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class DependencyGraphServiceTest {

    @Autowired(required = false)
    private DependencyGraphService dependencyGraphService;

    @Autowired(required = false)
    private DocumentRevisionRepository revisionRepository;

    @Autowired(required = false)
    private DocumentChunkRepository chunkRepository;

    @Test
    void testServiceInjection() {
        assertNotNull(dependencyGraphService, "DependencyGraphService should be injectable");
    }

    @Test
    void testBuildDependencyGraph_RevisionNotFound() {
        if (dependencyGraphService == null) return;

        List<?> result = dependencyGraphService.buildDependencyGraph(99999L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindDependentDocuments_RevisionNotFound() {
        if (dependencyGraphService == null) return;

        Map<Long, List<String>> result = dependencyGraphService.findDependentDocuments(99999L);
        assertNotNull(result);
    }

    @Test
    void testTraverseDependencies_RevisionNotFound() {
        if (dependencyGraphService == null) return;

        Map<Long, List<String>> result = dependencyGraphService.traverseDependencies(99999L, 3);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindPath_NoPathExists() {
        if (dependencyGraphService == null) return;

        List<String> path = dependencyGraphService.findPath(99999L, 88888L, 5);
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    void testRelationshipTypeConstants() {
        assertEquals("REFERENCES", DependencyGraphService.REFERENCES);
        assertEquals("DEPENDS_ON", DependencyGraphService.DEPENDS_ON);
        assertEquals("SUPERSEDES", DependencyGraphService.SUPERSEDES);
        assertEquals("CLARIFIES", DependencyGraphService.CLARIFIES);
        assertEquals("CONTRADICTS", DependencyGraphService.CONTRADICTS);
    }
}
