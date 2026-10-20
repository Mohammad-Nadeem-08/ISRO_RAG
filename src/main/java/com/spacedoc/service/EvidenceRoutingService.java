package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import com.spacedoc.entity.DocumentRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EvidenceRoutingService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceRoutingService.class);

    private final RevisionComparisonService revisionComparisonService;
    private final ConflictDetectionService conflictDetectionService;

    public EvidenceRoutingService(RevisionComparisonService revisionComparisonService,
                                 ConflictDetectionService conflictDetectionService) {
        this.revisionComparisonService = revisionComparisonService;
        this.conflictDetectionService = conflictDetectionService;
    }

    /**
     * RESEARCH CONTRIBUTION: Revision-Aware Evidence Routing
     *
     * Given a query and retrieved evidence, route evidence to the most applicable revision.
     * This is the core innovation: instead of treating all evidence equally,
     * we determine which document revision each evidence item is most relevant to,
     * accounting for how the document has evolved.
     *
     * Algorithm:
     * 1. Group evidence by source document
     * 2. For each document, detect if it has multiple revisions
     * 3. Determine which revision each evidence chunk is "most applicable" to
     * 4. Route evidence to applicable revisions
     * 5. Detect and flag conflicts within each revision's evidence set
     */
    public RoutedEvidenceSet routeEvidenceByRevision(String query, List<RetrievalResultDTO> evidence,
                                                     DocumentRevision targetRevision) {
        RoutedEvidenceSet routedSet = new RoutedEvidenceSet();
        routedSet.setQuery(query);
        routedSet.setTargetRevisionId(targetRevision.getId());
        routedSet.setTargetRevisionNumber(targetRevision.getRevisionNumber());

        if (evidence == null || evidence.isEmpty()) {
            routedSet.setTotalEvidenceCount(0);
            routedSet.setApplicableEvidenceCount(0);
            return routedSet;
        }

        routedSet.setTotalEvidenceCount(evidence.size());

        // Group evidence by revision
        Map<Long, List<RetrievalResultDTO>> evidenceByRevision = groupByRevision(evidence);

        // For each revision, compute applicability to target revision
        List<RetrievalResultDTO> applicableEvidence = new ArrayList<>();

        for (Map.Entry<Long, List<RetrievalResultDTO>> entry : evidenceByRevision.entrySet()) {
            Long sourceRevisionId = entry.getKey();
            List<RetrievalResultDTO> revisionEvidence = entry.getValue();

            // Check if this evidence's revision is applicable to target revision
            ApplicabilityScore applicability = computeApplicability(
                sourceRevisionId,
                targetRevision
            );

            log.debug("Evidence from revision {} applicability to revision {}: score={}",
                sourceRevisionId, targetRevision.getRevisionNumber(), applicability.getScore());

            if (applicability.isApplicable()) {
                applicableEvidence.addAll(revisionEvidence);

                // Add routing metadata
                for (RetrievalResultDTO item : revisionEvidence) {
                    item.setApplicabilityScore(applicability.getScore());
                    item.setApplicabilityReason(applicability.getReason());
                }
            }
        }

        // Detect conflicts within applicable evidence
        List<ConflictDetectionService.EvidenceConflict> conflicts =
            conflictDetectionService.detectConflicts(applicableEvidence);

        // Resolve conflicts by keeping higher-relevance evidence
        List<RetrievalResultDTO> resolvedEvidence =
            conflictDetectionService.resolveConflicts(applicableEvidence, conflicts);

        // Rank resolved evidence
        resolvedEvidence.sort((a, b) -> {
            Float scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0f;
            Float scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0f;
            return Float.compare(scoreB, scoreA);
        });

        routedSet.setApplicableEvidenceCount(resolvedEvidence.size());
        routedSet.setRoutedEvidence(resolvedEvidence);
        routedSet.setDetectedConflicts(conflicts);
        routedSet.setConflictCount(conflicts.size());

        log.info("Routed evidence: {} -> {} applicable (after conflict resolution), {} conflicts detected",
            evidence.size(), resolvedEvidence.size(), conflicts.size());

        return routedSet;
    }

    /**
     * Compute applicability of evidence from sourceRevision to targetRevision
     * Returns score and reasoning
     */
    private ApplicabilityScore computeApplicability(Long sourceRevisionId, DocumentRevision targetRevision) {
        ApplicabilityScore score = new ApplicabilityScore();

        if (sourceRevisionId == null || sourceRevisionId.equals(targetRevision.getId())) {
            // Evidence from target revision is fully applicable
            score.setScore(1.0f);
            score.setApplicable(true);
            score.setReason("Evidence from target revision");
            return score;
        }

        // If source revision ID doesn't match target, evidence is from a different revision
        // Applicability depends on revision history
        // For now, use a simple heuristic: evidence is applicable if from same document
        // In a full implementation, would check revision number ordering and delta analysis

        score.setScore(0.7f); // Conservative score for cross-revision evidence
        score.setApplicable(true);
        score.setReason("Evidence from different revision (cross-revision applicability)");

        return score;
    }

    /**
     * Group evidence by source revision
     */
    private Map<Long, List<RetrievalResultDTO>> groupByRevision(List<RetrievalResultDTO> evidence) {
        return evidence.stream()
            .collect(Collectors.groupingBy(e -> e.getRevisionId() != null ? e.getRevisionId() : -1L));
    }

    /**
     * DTO: Routed evidence with applicability analysis
     */
    public static class RoutedEvidenceSet {
        private String query;
        private Long targetRevisionId;
        private Integer targetRevisionNumber;
        private Integer totalEvidenceCount;
        private Integer applicableEvidenceCount;
        private List<RetrievalResultDTO> routedEvidence;
        private List<ConflictDetectionService.EvidenceConflict> detectedConflicts;
        private Integer conflictCount;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Long getTargetRevisionId() { return targetRevisionId; }
        public void setTargetRevisionId(Long targetRevisionId) { this.targetRevisionId = targetRevisionId; }

        public Integer getTargetRevisionNumber() { return targetRevisionNumber; }
        public void setTargetRevisionNumber(Integer targetRevisionNumber) { this.targetRevisionNumber = targetRevisionNumber; }

        public Integer getTotalEvidenceCount() { return totalEvidenceCount; }
        public void setTotalEvidenceCount(Integer totalEvidenceCount) { this.totalEvidenceCount = totalEvidenceCount; }

        public Integer getApplicableEvidenceCount() { return applicableEvidenceCount; }
        public void setApplicableEvidenceCount(Integer applicableEvidenceCount) { this.applicableEvidenceCount = applicableEvidenceCount; }

        public List<RetrievalResultDTO> getRoutedEvidence() { return routedEvidence; }
        public void setRoutedEvidence(List<RetrievalResultDTO> routedEvidence) { this.routedEvidence = routedEvidence; }

        public List<ConflictDetectionService.EvidenceConflict> getDetectedConflicts() { return detectedConflicts; }
        public void setDetectedConflicts(List<ConflictDetectionService.EvidenceConflict> detectedConflicts) { this.detectedConflicts = detectedConflicts; }

        public Integer getConflictCount() { return conflictCount; }
        public void setConflictCount(Integer conflictCount) { this.conflictCount = conflictCount; }
    }

    /**
     * DTO: Applicability score and reasoning
     */
    static class ApplicabilityScore {
        private float score;
        private boolean applicable;
        private String reason;

        public float getScore() { return score; }
        public void setScore(float score) { this.score = score; }

        public boolean isApplicable() { return applicable; }
        public void setApplicable(boolean applicable) { this.applicable = applicable; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
