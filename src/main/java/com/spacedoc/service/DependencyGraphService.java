package com.spacedoc.service;

import com.spacedoc.entity.DocumentChunk;
import com.spacedoc.entity.DocumentRelationship;
import com.spacedoc.entity.DocumentRevision;
import com.spacedoc.repository.DocumentChunkRepository;
import com.spacedoc.repository.DocumentRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for building and querying the document dependency graph.
 *
 * RESEARCH CONTRIBUTION: This service implements the dependency graph structure
 * that enables "dependency-guided evidence expansion" and impact propagation.
 *
 * The graph stores explicit relationships between document revisions, including:
 * - REFERENCES: Document A cites Document B
 * - DEPENDS_ON: Document A requires Document B
 * - SUPERSEDES: Document A replaces Document B
 * - CLARIFIES: Document A explains Document B
 * - CONTRADICTS: Document A conflicts with Document B
 */
@Service
public class DependencyGraphService {

    private static final Logger log = LoggerFactory.getLogger(DependencyGraphService.class);

    private final DocumentRevisionRepository revisionRepository;
    private final DocumentChunkRepository chunkRepository;

    // Relationship types
    public static final String REFERENCES = "REFERENCES";
    public static final String DEPENDS_ON = "DEPENDS_ON";
    public static final String SUPERSEDES = "SUPERSEDES";
    public static final String CLARIFIES = "CLARIFIES";
    public static final String CONTRADICTS = "CONTRADICTS";

    public DependencyGraphService(DocumentRevisionRepository revisionRepository,
                                   DocumentChunkRepository chunkRepository) {
        this.revisionRepository = revisionRepository;
        this.chunkRepository = chunkRepository;
    }

    /**
     * Build dependency graph for a specific document revision.
     * Returns all relationships where this revision is the source.
     *
     * @param revisionId The revision to build the graph for
     * @return List of outgoing relationships from this revision
     */
    @Transactional(readOnly = true)
    public List<DocumentRelationship> buildDependencyGraph(Long revisionId) {
        List<DocumentRelationship> relationships = new ArrayList<>();

        DocumentRevision revision = revisionRepository.findById(revisionId).orElse(null);
        if (revision == null) {
            log.warn("Revision {} not found for dependency graph", revisionId);
            return relationships;
        }

        // Extract relationships from revision's chunks
        List<DocumentChunk> chunks = chunkRepository.findByDocumentRevisionId(revisionId);

        for (DocumentChunk chunk : chunks) {
            // In a full implementation, would extract explicit references from chunk text
            // For now, use chunk metadata if available
            List<DocumentRelationship> chunkRelationships = extractRelationshipsFromChunk(chunk, revision);
            relationships.addAll(chunkRelationships);
        }

        log.info("Built dependency graph for revision {}: {} relationships", revisionId, relationships.size());
        return relationships;
    }

    /**
     * Find all documents that depend on a specific revision.
     * This is the inverse query: find documents that reference/depend on this one.
     *
     * @param revisionId The revision to find dependents for
     * @return Map of document ID to list of relationship types
     */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> findDependentDocuments(Long revisionId) {
        Map<Long, List<String>> dependents = new HashMap<>();

        // In production, would query DocumentRelationship table directly
        // For now, search through all revisions
        List<DocumentRevision> allRevisions = revisionRepository.findAll();

        for (DocumentRevision rev : allRevisions) {
            if (rev.getId().equals(revisionId)) continue;

            List<DocumentRelationship> rels = buildDependencyGraph(rev.getId());
            for (DocumentRelationship rel : rels) {
                if (rel.getTargetRevision() != null &&
                    rel.getTargetRevision().getId().equals(revisionId)) {

                    dependents.computeIfAbsent(rev.getDocument().getId(), k -> new ArrayList<>())
                        .add(rel.getRelationshipType());
                }
            }
        }

        log.info("Found {} documents depending on revision {}", dependents.size(), revisionId);
        return dependents;
    }

    /**
     * Traverse dependency graph to find all related documents within depth limit.
     *
     * @param startRevisionId Starting revision
     * @param maxDepth Maximum traversal depth
     * @return Map of revision ID to traversal path
     */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> traverseDependencies(Long startRevisionId, int maxDepth) {
        Map<Long, List<String>> visited = new HashMap<>();
        Queue<TraversalNode> queue = new LinkedList<>();

        DocumentRevision start = revisionRepository.findById(startRevisionId).orElse(null);
        if (start == null) {
            log.warn("Start revision {} not found for traversal", startRevisionId);
            return visited;
        }

        queue.add(new TraversalNode(startRevisionId, new ArrayList<>(), 0));
        visited.put(startRevisionId, new ArrayList<>());

        while (!queue.isEmpty()) {
            TraversalNode current = queue.poll();

            if (current.depth >= maxDepth) {
                continue;
            }

            List<DocumentRelationship> relationships = buildDependencyGraph(current.revisionId);

            for (DocumentRelationship rel : relationships) {
                if (rel.getTargetRevision() != null) {
                    Long targetId = rel.getTargetRevision().getId();

                    if (!visited.containsKey(targetId)) {
                        List<String> path = new ArrayList<>(current.path);
                        path.add(rel.getRelationshipType());

                        visited.put(targetId, path);
                        queue.add(new TraversalNode(targetId, path, current.depth + 1));
                    }
                }
            }
        }

        // Remove start node from results
        visited.remove(startRevisionId);

        log.info("Dependency traversal from revision {} (depth={}): found {} related revisions",
            startRevisionId, maxDepth, visited.size());
        return visited;
    }

    /**
     * Find the shortest path between two revisions.
     *
     * @param fromRevisionId Source revision
     * @param toRevisionId Target revision
     * @param maxDepth Maximum search depth
     * @return Path of relationship types, or empty if no path found
     */
    public List<String> findPath(Long fromRevisionId, Long toRevisionId, int maxDepth) {
        Map<Long, List<String>> paths = traverseDependencies(fromRevisionId, maxDepth);
        return paths.getOrDefault(toRevisionId, new ArrayList<>());
    }

    /**
     * Extract relationships from a chunk's content.
     * This would use NER or pattern matching in a full implementation.
     *
     * @param chunk The chunk to analyze
     * @param sourceRevision The revision this chunk belongs to
     * @return List of extracted relationships
     */
    private List<DocumentRelationship> extractRelationshipsFromChunk(DocumentChunk chunk, DocumentRevision sourceRevision) {
        List<DocumentRelationship> relationships = new ArrayList<>();

        // Placeholder: In production, would extract explicit references
        // using pattern matching, NER, or requirement identifiers
        // For now, return empty list

        return relationships;
    }

    /**
     * Node for BFS traversal.
     */
    private static class TraversalNode {
        Long revisionId;
        List<String> path;
        int depth;

        TraversalNode(Long revisionId, List<String> path, int depth) {
            this.revisionId = revisionId;
            this.path = path;
            this.depth = depth;
        }
    }
}
