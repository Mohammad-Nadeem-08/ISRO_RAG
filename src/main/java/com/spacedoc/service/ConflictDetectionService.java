package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConflictDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ConflictDetectionService.class);

    /**
     * Detect contradictory evidence in retrieval results
     * Uses simple heuristics: opposing keywords, directional conflicts
     */
    public List<EvidenceConflict> detectConflicts(List<RetrievalResultDTO> evidence) {
        List<EvidenceConflict> conflicts = new ArrayList<>();

        // Compare each pair of evidence items
        for (int i = 0; i < evidence.size(); i++) {
            for (int j = i + 1; j < evidence.size(); j++) {
                RetrievalResultDTO item1 = evidence.get(i);
                RetrievalResultDTO item2 = evidence.get(j);

                EvidenceConflict conflict = detectPairwiseConflict(item1, item2);
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        log.info("Detected {} conflicts in {} evidence items", conflicts.size(), evidence.size());
        return conflicts;
    }

    /**
     * Detect conflict between two pieces of evidence
     */
    private EvidenceConflict detectPairwiseConflict(RetrievalResultDTO evidence1, RetrievalResultDTO evidence2) {
        String content1 = evidence1.getContent() != null ? evidence1.getContent().toLowerCase() : "";
        String content2 = evidence2.getContent() != null ? evidence2.getContent().toLowerCase() : "";

        // Detect contradictory patterns
        ConflictType type = detectConflictType(content1, content2);

        if (type != null) {
            EvidenceConflict conflict = new EvidenceConflict();
            conflict.setChunkId1(evidence1.getChunkId());
            conflict.setChunkId2(evidence2.getChunkId());
            conflict.setRevisionId1(evidence1.getRevisionId());
            conflict.setRevisionId2(evidence2.getRevisionId());
            conflict.setConflictType(type.toString());
            conflict.setDescription("Evidence conflict: " + type.toString());
            conflict.setConfidenceScore(computeConflictConfidence(content1, content2, type));
            return conflict;
        }

        return null;
    }

    /**
     * Detect type of conflict between two texts
     */
    private ConflictType detectConflictType(String text1, String text2) {
        // Negation patterns
        boolean text1Affirms = text1.contains("is") || text1.contains("must") || text1.contains("required");
        boolean text2Negates = text2.contains("is not") || text2.contains("should not") || text2.contains("prohibited");

        if (text1Affirms && text2Negates) {
            return ConflictType.NEGATION;
        }

        // Directional conflicts
        if ((text1.contains("increase") || text1.contains("improve")) &&
            (text2.contains("decrease") || text2.contains("reduce"))) {
            return ConflictType.DIRECTIONAL;
        }

        // Quantitative conflicts (e.g., "minimum 10" vs "maximum 5")
        if ((text1.contains("minimum") || text1.contains("at least")) &&
            (text2.contains("maximum") || text2.contains("no more than"))) {
            return ConflictType.QUANTITATIVE;
        }

        // Temporal conflicts (revision date ordering)
        if (text1.contains("before") && text2.contains("after")) {
            return ConflictType.TEMPORAL;
        }

        return null;
    }

    /**
     * Compute confidence score for detected conflict (0.0 to 1.0)
     */
    private float computeConflictConfidence(String text1, String text2, ConflictType type) {
        // Base confidence by type
        float baseConfidence = switch (type) {
            case NEGATION -> 0.9f;
            case DIRECTIONAL -> 0.8f;
            case QUANTITATIVE -> 0.7f;
            case TEMPORAL -> 0.6f;
            default -> 0.5f;
        };

        // Adjust based on text similarity (low similarity = higher confidence in conflict)
        float similarity = computeTextSimilarity(text1, text2);
        float adjustedConfidence = baseConfidence * (1.0f - similarity * 0.3f);

        return Math.min(1.0f, adjustedConfidence);
    }

    /**
     * Simple text similarity metric (0.0 to 1.0)
     */
    private float computeTextSimilarity(String text1, String text2) {
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");

        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0f;
        return (float) intersection.size() / union.size();
    }

    /**
     * Filter evidence to remove conflicted items, keeping highest-relevance versions
     */
    public List<RetrievalResultDTO> resolveConflicts(List<RetrievalResultDTO> evidence, List<EvidenceConflict> conflicts) {
        if (conflicts.isEmpty()) {
            return evidence;
        }

        // Build conflict graph
        Map<Long, List<EvidenceConflict>> conflictMap = new HashMap<>();
        for (EvidenceConflict conflict : conflicts) {
            conflictMap.computeIfAbsent(conflict.getChunkId1(), k -> new ArrayList<>()).add(conflict);
            conflictMap.computeIfAbsent(conflict.getChunkId2(), k -> new ArrayList<>()).add(conflict);
        }

        // Keep highest-relevance evidence, remove conflicted lower-relevance items
        List<RetrievalResultDTO> resolved = new ArrayList<>();
        Set<Long> removed = new HashSet<>();

        for (RetrievalResultDTO item : evidence) {
            if (removed.contains(item.getChunkId())) {
                continue;
            }

            List<EvidenceConflict> itemConflicts = conflictMap.getOrDefault(item.getChunkId(), new ArrayList<>());

            if (!itemConflicts.isEmpty()) {
                // Find the conflicting item with lower relevance and mark for removal
                for (EvidenceConflict conflict : itemConflicts) {
                    Long otherId = conflict.getChunkId1().equals(item.getChunkId())
                        ? conflict.getChunkId2()
                        : conflict.getChunkId1();

                    if (!removed.contains(otherId)) {
                        // Find the other evidence item in the original list
                        Optional<RetrievalResultDTO> otherItem = evidence.stream()
                            .filter(e -> e.getChunkId().equals(otherId))
                            .findFirst();

                        if (otherItem.isPresent()) {
                            Float itemScore = item.getRelevanceScore() != null ? item.getRelevanceScore() : 0.0f;
                            Float otherScore = otherItem.get().getRelevanceScore() != null ? otherItem.get().getRelevanceScore() : 0.0f;

                            // Remove lower-relevance item
                            if (otherScore < itemScore) {
                                removed.add(otherId);
                            } else {
                                removed.add(item.getChunkId());
                                continue;
                            }
                        }
                    }
                }
            }

            resolved.add(item);
        }

        log.info("Resolved conflicts: {} -> {} evidence items", evidence.size(), resolved.size());
        return resolved;
    }

    /**
     * DTO: Evidence conflict representation
     */
    public static class EvidenceConflict {
        private Long chunkId1;
        private Long chunkId2;
        private Long revisionId1;
        private Long revisionId2;
        private String conflictType;
        private String description;
        private float confidenceScore;

        public Long getChunkId1() { return chunkId1; }
        public void setChunkId1(Long chunkId1) { this.chunkId1 = chunkId1; }

        public Long getChunkId2() { return chunkId2; }
        public void setChunkId2(Long chunkId2) { this.chunkId2 = chunkId2; }

        public Long getRevisionId1() { return revisionId1; }
        public void setRevisionId1(Long revisionId1) { this.revisionId1 = revisionId1; }

        public Long getRevisionId2() { return revisionId2; }
        public void setRevisionId2(Long revisionId2) { this.revisionId2 = revisionId2; }

        public String getConflictType() { return conflictType; }
        public void setConflictType(String conflictType) { this.conflictType = conflictType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public float getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(float confidenceScore) { this.confidenceScore = confidenceScore; }
    }

    enum ConflictType {
        NEGATION,        // "is" vs "is not"
        DIRECTIONAL,     // "increase" vs "decrease"
        QUANTITATIVE,    // "minimum" vs "maximum"
        TEMPORAL,        // "before" vs "after"
        DOMAIN_SPECIFIC  // Custom domain logic
    }
}
