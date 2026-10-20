package com.spacedoc.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Document Impact Analysis results.
 *
 * Represents the potential impact of a document change on related documents,
 * distinguishing between direct dependencies, indirect relationships, and
 * semantic similarity candidates.
 */
public class ImpactAnalysisDTO {

    private Long sourceDocumentId;
    private String sourceDocumentKey;
    private String sourceDocumentTitle;
    private Integer sourceRevisionNumber;

    private String changeDescription;
    private LocalDateTime analysisTimestamp;

    // Impact categories
    private List<ImpactedDocument> directlyImpactedDocuments;
    private List<ImpactedDocument> indirectlyImpactedDocuments;
    private List<ImpactedDocument> semanticSimilarityCandidates;

    // Summary metrics
    private Integer totalDirectImpacts;
    private Integer totalIndirectImpacts;
    private Integer totalCandidates;
    private Integer totalImpactedDocuments;

    // Analysis metadata
    private String analysisMethod;
    private Integer traversalDepth;
    private Long analysisDurationMs;
    private Boolean requiresHumanReview;
    private String reviewNotes;

    public ImpactAnalysisDTO() {
        this.directlyImpactedDocuments = new ArrayList<>();
        this.indirectlyImpactedDocuments = new ArrayList<>();
        this.semanticSimilarityCandidates = new ArrayList<>();
        this.analysisTimestamp = LocalDateTime.now();
        this.totalDirectImpacts = 0;
        this.totalIndirectImpacts = 0;
        this.totalCandidates = 0;
        this.totalImpactedDocuments = 0;
        this.requiresHumanReview = false;
    }

    /**
     * Represents a single impacted document with reasoning.
     */
    public static class ImpactedDocument {
        private Long documentId;
        private String documentKey;
        private String documentTitle;
        private String impactReason;
        private Float confidenceScore;
        private String relationshipType;
        private String evidenceText;
        private Long evidenceChunkId;
        private String evidenceSectionTitle;
        private Boolean isConfirmed;
        private Boolean requiresReview;
        private List<String> dependencyPath;

        public ImpactedDocument() {
            this.isConfirmed = false;
            this.requiresReview = false;
            this.dependencyPath = new ArrayList<>();
        }

        // Getters and Setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }

        public String getDocumentKey() { return documentKey; }
        public void setDocumentKey(String documentKey) { this.documentKey = documentKey; }

        public String getDocumentTitle() { return documentTitle; }
        public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

        public String getImpactReason() { return impactReason; }
        public void setImpactReason(String impactReason) { this.impactReason = impactReason; }

        public Float getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(Float confidenceScore) { this.confidenceScore = confidenceScore; }

        public String getRelationshipType() { return relationshipType; }
        public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

        public String getEvidenceText() { return evidenceText; }
        public void setEvidenceText(String evidenceText) { this.evidenceText = evidenceText; }

        public Long getEvidenceChunkId() { return evidenceChunkId; }
        public void setEvidenceChunkId(Long evidenceChunkId) { this.evidenceChunkId = evidenceChunkId; }

        public String getEvidenceSectionTitle() { return evidenceSectionTitle; }
        public void setEvidenceSectionTitle(String evidenceSectionTitle) { this.evidenceSectionTitle = evidenceSectionTitle; }

        public Boolean getIsConfirmed() { return isConfirmed; }
        public void setIsConfirmed(Boolean isConfirmed) { this.isConfirmed = isConfirmed; }

        public Boolean getRequiresReview() { return requiresReview; }
        public void setRequiresReview(Boolean requiresReview) { this.requiresReview = requiresReview; }

        public List<String> getDependencyPath() { return dependencyPath; }
        public void setDependencyPath(List<String> dependencyPath) { this.dependencyPath = dependencyPath; }
    }

    // Getters and Setters
    public Long getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(Long sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }

    public String getSourceDocumentKey() { return sourceDocumentKey; }
    public void setSourceDocumentKey(String sourceDocumentKey) { this.sourceDocumentKey = sourceDocumentKey; }

    public String getSourceDocumentTitle() { return sourceDocumentTitle; }
    public void setSourceDocumentTitle(String sourceDocumentTitle) { this.sourceDocumentTitle = sourceDocumentTitle; }

    public Integer getSourceRevisionNumber() { return sourceRevisionNumber; }
    public void setSourceRevisionNumber(Integer sourceRevisionNumber) { this.sourceRevisionNumber = sourceRevisionNumber; }

    public String getChangeDescription() { return changeDescription; }
    public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }

    public LocalDateTime getAnalysisTimestamp() { return analysisTimestamp; }
    public void setAnalysisTimestamp(LocalDateTime analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }

    public List<ImpactedDocument> getDirectlyImpactedDocuments() { return directlyImpactedDocuments; }
    public void setDirectlyImpactedDocuments(List<ImpactedDocument> directlyImpactedDocuments) {
        this.directlyImpactedDocuments = directlyImpactedDocuments;
        this.totalDirectImpacts = directlyImpactedDocuments != null ? directlyImpactedDocuments.size() : 0;
        updateTotals();
    }

    public List<ImpactedDocument> getIndirectlyImpactedDocuments() { return indirectlyImpactedDocuments; }
    public void setIndirectlyImpactedDocuments(List<ImpactedDocument> indirectlyImpactedDocuments) {
        this.indirectlyImpactedDocuments = indirectlyImpactedDocuments;
        this.totalIndirectImpacts = indirectlyImpactedDocuments != null ? indirectlyImpactedDocuments.size() : 0;
        updateTotals();
    }

    public List<ImpactedDocument> getSemanticSimilarityCandidates() { return semanticSimilarityCandidates; }
    public void setSemanticSimilarityCandidates(List<ImpactedDocument> semanticSimilarityCandidates) {
        this.semanticSimilarityCandidates = semanticSimilarityCandidates;
        this.totalCandidates = semanticSimilarityCandidates != null ? semanticSimilarityCandidates.size() : 0;
        updateTotals();
    }

    public Integer getTotalDirectImpacts() { return totalDirectImpacts; }
    public void setTotalDirectImpacts(Integer totalDirectImpacts) { this.totalDirectImpacts = totalDirectImpacts; }

    public Integer getTotalIndirectImpacts() { return totalIndirectImpacts; }
    public void setTotalIndirectImpacts(Integer totalIndirectImpacts) { this.totalIndirectImpacts = totalIndirectImpacts; }

    public Integer getTotalCandidates() { return totalCandidates; }
    public void setTotalCandidates(Integer totalCandidates) { this.totalCandidates = totalCandidates; }

    public Integer getTotalImpactedDocuments() { return totalImpactedDocuments; }
    public void setTotalImpactedDocuments(Integer totalImpactedDocuments) { this.totalImpactedDocuments = totalImpactedDocuments; }

    public String getAnalysisMethod() { return analysisMethod; }
    public void setAnalysisMethod(String analysisMethod) { this.analysisMethod = analysisMethod; }

    public Integer getTraversalDepth() { return traversalDepth; }
    public void setTraversalDepth(Integer traversalDepth) { this.traversalDepth = traversalDepth; }

    public Long getAnalysisDurationMs() { return analysisDurationMs; }
    public void setAnalysisDurationMs(Long analysisDurationMs) { this.analysisDurationMs = analysisDurationMs; }

    public Boolean getRequiresHumanReview() { return requiresHumanReview; }
    public void setRequiresHumanReview(Boolean requiresHumanReview) { this.requiresHumanReview = requiresHumanReview; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    private void updateTotals() {
        this.totalImpactedDocuments =
            (this.totalDirectImpacts != null ? this.totalDirectImpacts : 0) +
            (this.totalIndirectImpacts != null ? this.totalIndirectImpacts : 0) +
            (this.totalCandidates != null ? this.totalCandidates : 0);

        this.requiresHumanReview = this.totalCandidates > 0;
    }
}
