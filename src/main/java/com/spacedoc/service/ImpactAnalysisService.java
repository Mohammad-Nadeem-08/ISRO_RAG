package com.spacedoc.service;

import com.spacedoc.dto.ImpactAnalysisDTO;
import com.spacedoc.dto.ImpactAnalysisDTO.ImpactedDocument;
import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentChunk;
import com.spacedoc.entity.DocumentRelationship;
import com.spacedoc.entity.DocumentRevision;
import com.spacedoc.repository.DocumentChunkRepository;
import com.spacedoc.repository.DocumentRepository;
import com.spacedoc.repository.DocumentRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RESEARCH CONTRIBUTION: Document Impact Analysis Service
 *
 * This service implements the "impact propagation" mechanism described in the
 * invention hypothesis. When a technical document changes, this service identifies
 * potentially affected documents through:
 *
 * 1. Direct Dependencies: Documents that explicitly reference the changed document
 * 2. Indirect Dependencies: Documents connected through multi-hop relationships
 * 3. Semantic Similarity Candidates: Documents with similar content (lower confidence)
 *
 * The service distinguishes between confirmed relationships (explicit evidence)
 * and candidates requiring human verification.
 */
@Service
public class ImpactAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ImpactAnalysisService.class);

    private final DocumentRepository documentRepository;
    private final DocumentRevisionRepository revisionRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DependencyGraphService dependencyGraphService;
    private final RetrievalService retrievalService;

    // Confidence thresholds
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.85f;
    private static final float MEDIUM_CONFIDENCE_THRESHOLD = 0.60f;
    private static final int DEFAULT_TRAVERSAL_DEPTH = 3;

    public ImpactAnalysisService(DocumentRepository documentRepository,
                                  DocumentRevisionRepository revisionRepository,
                                  DocumentChunkRepository chunkRepository,
                                  DependencyGraphService dependencyGraphService,
                                  RetrievalService retrievalService) {
        this.documentRepository = documentRepository;
        this.revisionRepository = revisionRepository;
        this.chunkRepository = chunkRepository;
        this.dependencyGraphService = dependencyGraphService;
        this.retrievalService = retrievalService;
    }

    /**
     * Analyze the impact of a document change on related documents.
     *
     * @param documentId The ID of the changed document
     * @param revisionId Optional specific revision (defaults to latest)
     * @param changeDescription Description of what changed
     * @return Impact analysis results
     */
    @Transactional(readOnly = true)
    public ImpactAnalysisDTO analyzeImpact(Long documentId, Long revisionId, String changeDescription) {
        long startTime = System.currentTimeMillis();

        ImpactAnalysisDTO analysis = new ImpactAnalysisDTO();
        analysis.setChangeDescription(changeDescription);
        analysis.setAnalysisMethod("dependency-graph-traversal");
        analysis.setTraversalDepth(DEFAULT_TRAVERSAL_DEPTH);

        // Fetch source document
        Document sourceDoc = documentRepository.findById(documentId).orElse(null);
        if (sourceDoc == null) {
            log.warn("Document {} not found for impact analysis", documentId);
            analysis.setReviewNotes("Source document not found");
            return analysis;
        }

        analysis.setSourceDocumentId(documentId);
        analysis.setSourceDocumentKey(sourceDoc.getDocumentKey());
        analysis.setSourceDocumentTitle(sourceDoc.getTitle());

        // Determine revision to analyze
        DocumentRevision targetRevision;
        if (revisionId != null) {
            targetRevision = revisionRepository.findById(revisionId).orElse(null);
        } else {
            // Get latest revision
            targetRevision = sourceDoc.getRevisions().stream()
                .max(Comparator.comparing(DocumentRevision::getRevisionNumber))
                .orElse(null);
        }

        if (targetRevision == null) {
            log.warn("No revision found for document {}", documentId);
            analysis.setReviewNotes("No revision available for analysis");
            return analysis;
        }

        analysis.setSourceRevisionNumber(targetRevision.getRevisionNumber());

        log.info("Starting impact analysis for document {} (revision {})",
            sourceDoc.getDocumentKey(), targetRevision.getRevisionNumber());

        // Step 1: Find directly impacted documents (1-hop dependencies)
        List<ImpactedDocument> directImpacts = findDirectImpacts(targetRevision);
        analysis.setDirectlyImpactedDocuments(directImpacts);

        // Step 2: Find indirectly impacted documents (multi-hop dependencies)
        List<ImpactedDocument> indirectImpacts = findIndirectImpacts(targetRevision, directImpacts);
        analysis.setIndirectlyImpactedDocuments(indirectImpacts);

        // Step 3: Find semantic similarity candidates (lower confidence)
        List<ImpactedDocument> similarityCandidates = findSemanticSimilarityCandidates(
            sourceDoc, targetRevision, directImpacts, indirectImpacts);
        analysis.setSemanticSimilarityCandidates(similarityCandidates);

        // Calculate duration
        long duration = System.currentTimeMillis() - startTime;
        analysis.setAnalysisDurationMs(duration);

        // Set review notes
        if (analysis.getTotalCandidates() > 0) {
            analysis.setReviewNotes(
                String.format("Found %d candidates requiring human verification. " +
                    "Review semantic similarity matches before confirming impact.",
                    analysis.getTotalCandidates()));
        }

        log.info("Impact analysis complete: {} direct, {} indirect, {} candidates ({}ms)",
            analysis.getTotalDirectImpacts(), analysis.getTotalIndirectImpacts(),
            analysis.getTotalCandidates(), duration);

        return analysis;
    }

    /**
     * Find documents that directly reference or depend on this revision.
     */
    private List<ImpactedDocument> findDirectImpacts(DocumentRevision targetRevision) {
        List<ImpactedDocument> impacts = new ArrayList<>();

        // Get documents that depend on this revision
        Map<Long, List<String>> dependents = dependencyGraphService.findDependentDocuments(targetRevision.getId());

        for (Map.Entry<Long, List<String>> entry : dependents.entrySet()) {
            Long docId = entry.getKey();
            List<String> relationshipTypes = entry.getValue();

            Document doc = documentRepository.findById(docId).orElse(null);
            if (doc == null) continue;

            ImpactedDocument impacted = new ImpactedDocument();
            impacted.setDocumentId(docId);
            impacted.setDocumentKey(doc.getDocumentKey());
            impacted.setDocumentTitle(doc.getTitle());
            impacted.setRelationshipType(String.join(", ", relationshipTypes));
            impacted.setImpactReason(buildDirectImpactReason(relationshipTypes));
            impacted.setConfidenceScore(HIGH_CONFIDENCE_THRESHOLD);
            impacted.setIsConfirmed(true);
            impacted.setRequiresReview(false);
            impacted.setDependencyPath(Collections.singletonList("DIRECT"));

            impacts.add(impacted);
        }

        log.debug("Found {} directly impacted documents", impacts.size());
        return impacts;
    }

    /**
     * Find documents that are indirectly connected through multi-hop dependencies.
     */
    private List<ImpactedDocument> findIndirectImpacts(DocumentRevision targetRevision,
                                                        List<ImpactedDocument> directImpacts) {
        List<ImpactedDocument> indirectImpacts = new ArrayList<>();
        Set<Long> alreadyImpacted = directImpacts.stream()
            .map(ImpactedDocument::getDocumentId)
            .collect(Collectors.toSet());
        alreadyImpacted.add(targetRevision.getDocument().getId());

        // Traverse dependency graph
        Map<Long, List<String>> relatedRevisions = dependencyGraphService.traverseDependencies(
            targetRevision.getId(), DEFAULT_TRAVERSAL_DEPTH);

        for (Map.Entry<Long, List<String>> entry : relatedRevisions.entrySet()) {
            Long revisionId = entry.getKey();
            List<String> path = entry.getValue();

            DocumentRevision relatedRevision = revisionRepository.findById(revisionId).orElse(null);
            if (relatedRevision == null) continue;

            Long docId = relatedRevision.getDocument().getId();
            if (alreadyImpacted.contains(docId)) continue;

            Document doc = relatedRevision.getDocument();

            ImpactedDocument impacted = new ImpactedDocument();
            impacted.setDocumentId(docId);
            impacted.setDocumentKey(doc.getDocumentKey());
            impacted.setDocumentTitle(doc.getTitle());
            impacted.setRelationshipType("INDIRECT_DEPENDENCY");
            impacted.setImpactReason(buildIndirectImpactReason(path));
            impacted.setConfidenceScore(calculateIndirectConfidence(path));
            impacted.setIsConfirmed(false);
            impacted.setRequiresReview(true);
            impacted.setDependencyPath(path);

            indirectImpacts.add(impacted);
            alreadyImpacted.add(docId);
        }

        log.debug("Found {} indirectly impacted documents", indirectImpacts.size());
        return indirectImpacts;
    }

    /**
     * Find documents with semantically similar content.
     * These are candidates requiring human review.
     */
    private List<ImpactedDocument> findSemanticSimilarityCandidates(Document sourceDoc,
                                                                      DocumentRevision targetRevision,
                                                                      List<ImpactedDocument> directImpacts,
                                                                      List<ImpactedDocument> indirectImpacts) {
        List<ImpactedDocument> candidates = new ArrayList<>();

        // Get chunks from target revision
        List<DocumentChunk> chunks = chunkRepository.findByDocumentRevisionId(targetRevision.getId());
        if (chunks.isEmpty()) {
            log.debug("No chunks found for revision {}", targetRevision.getId());
            return candidates;
        }

        // Use first substantial chunk as query (would be more sophisticated in production)
        String queryText = chunks.stream()
            .filter(c -> c.getContent() != null && c.getContent().length() > 100)
            .map(DocumentChunk::getContent)
            .findFirst()
            .orElse(null);

        if (queryText == null || queryText.trim().isEmpty()) {
            return candidates;
        }

        // Truncate query for retrieval
        queryText = queryText.substring(0, Math.min(queryText.length(), 500));

        // Perform retrieval to find similar content
        try {
            var results = retrievalService.denseRetrieval(queryText, 10);

            Set<Long> alreadyImpacted = new HashSet<>();
            alreadyImpacted.add(sourceDoc.getId());
            directImpacts.forEach(d -> alreadyImpacted.add(d.getDocumentId()));
            indirectImpacts.forEach(d -> alreadyImpacted.add(d.getDocumentId()));

            for (var result : results) {
                if (alreadyImpacted.contains(result.getDocumentId())) continue;

                ImpactedDocument candidate = new ImpactedDocument();
                candidate.setDocumentId(result.getDocumentId());
                candidate.setDocumentKey(result.getDocumentKey());
                candidate.setDocumentTitle(result.getDocumentTitle());
                candidate.setRelationshipType("SEMANTIC_SIMILARITY");
                candidate.setImpactReason("Content similarity detected (requires human verification)");
                candidate.setConfidenceScore(result.getRelevanceScore() * 0.6f); // Lower confidence
                candidate.setIsConfirmed(false);
                candidate.setRequiresReview(true);
                candidate.setEvidenceText(result.getContent());
                candidate.setEvidenceChunkId(result.getChunkId());
                candidate.setEvidenceSectionTitle(result.getSectionTitle());
                candidate.setDependencyPath(Collections.singletonList("SEMANTIC"));

                candidates.add(candidate);
                alreadyImpacted.add(result.getDocumentId());
            }

        } catch (Exception e) {
            log.error("Error during semantic similarity search", e);
        }

        log.debug("Found {} semantic similarity candidates", candidates.size());
        return candidates;
    }

    /**
     * Build human-readable reason for direct impact.
     */
    private String buildDirectImpactReason(List<String> relationshipTypes) {
        if (relationshipTypes.isEmpty()) {
            return "Direct dependency relationship";
        }

        StringBuilder reason = new StringBuilder("Document ");
        for (int i = 0; i < relationshipTypes.size(); i++) {
            if (i > 0) reason.append(" and ");
            reason.append(relationshipTypes.get(i).toLowerCase().replace("_", " "));
        }
        reason.append(" this document");

        return reason.toString();
    }

    /**
     * Build human-readable reason for indirect impact.
     */
    private String buildIndirectImpactReason(List<String> path) {
        if (path.isEmpty()) {
            return "Indirect dependency relationship";
        }

        return "Connected through " + path.size() + "-hop dependency chain: " +
            String.join(" → ", path);
    }

    /**
     * Calculate confidence score for indirect impacts based on path length.
     * Longer paths = lower confidence.
     */
    private float calculateIndirectConfidence(List<String> path) {
        int hops = path.size();
        float baseScore = MEDIUM_CONFIDENCE_THRESHOLD;

        // Reduce confidence by 0.1 per hop
        float confidence = baseScore - (hops * 0.1f);

        // Clamp to reasonable range
        return Math.max(0.3f, Math.min(confidence, MEDIUM_CONFIDENCE_THRESHOLD));
    }
}
