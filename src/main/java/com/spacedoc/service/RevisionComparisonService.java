package com.spacedoc.service;

import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentRevision;
import com.spacedoc.repository.DocumentRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RevisionComparisonService {

    private static final Logger log = LoggerFactory.getLogger(RevisionComparisonService.class);

    private final DocumentRevisionRepository revisionRepository;

    public RevisionComparisonService(DocumentRevisionRepository revisionRepository) {
        this.revisionRepository = revisionRepository;
    }

    /**
     * Compare two revisions of the same document
     * Detects what changed between priorRevision and currentRevision
     */
    public RevisionDelta compareRevisions(DocumentRevision priorRevision, DocumentRevision currentRevision) {
        RevisionDelta delta = new RevisionDelta();
        delta.setFromRevisionNumber(priorRevision.getRevisionNumber());
        delta.setToRevisionNumber(currentRevision.getRevisionNumber());
        delta.setFromRevisionKey(priorRevision.getRevisionKey());
        delta.setToRevisionKey(currentRevision.getRevisionKey());

        if (priorRevision.getExtractedTextLength() != null && currentRevision.getExtractedTextLength() != null) {
            long sizeDifference = currentRevision.getExtractedTextLength() - priorRevision.getExtractedTextLength();
            delta.setSizeDifference(sizeDifference);
            delta.setPercentageChange((sizeDifference / (double) priorRevision.getExtractedTextLength()) * 100);
        }

        // Chunk count change
        Integer priorChunks = priorRevision.getTotalChunks() != null ? priorRevision.getTotalChunks() : 0;
        Integer currentChunks = currentRevision.getTotalChunks() != null ? currentRevision.getTotalChunks() : 0;
        delta.setChunkCountChange(currentChunks - priorChunks);

        // Content hash changed
        boolean contentChanged = !priorRevision.getContentHash().equals(currentRevision.getContentHash());
        delta.setContentChanged(contentChanged);

        // Summary
        delta.setChangeSummary(currentRevision.getChangeSummary());

        log.info("Compared revisions {}->{} for document {}: content changed={}, size diff={}",
            priorRevision.getRevisionNumber(), currentRevision.getRevisionNumber(),
            currentRevision.getDocument().getDocumentKey(), contentChanged, delta.getSizeDifference());

        return delta;
    }

    /**
     * Get revision history for a document
     */
    public List<DocumentRevision> getRevisionHistory(Document document) {
        return revisionRepository.findByDocumentOrderByRevisionNumberDesc(document);
    }

    /**
     * Check if evidence chunk is applicable to given revision
     * Chunk is applicable if it was created before or during the revision
     */
    public boolean isChunkApplicableToRevision(Long chunkRevisionId, DocumentRevision targetRevision) {
        Optional<DocumentRevision> chunkRevision = revisionRepository.findById(chunkRevisionId);

        if (chunkRevision.isEmpty()) {
            return false;
        }

        // Chunk is applicable if it's from this revision or an earlier revision
        return chunkRevision.get().getRevisionNumber() <= targetRevision.getRevisionNumber();
    }

    /**
     * Get all revisions between two revision numbers (inclusive)
     */
    public List<DocumentRevision> getRevisionsBetween(Document document, int fromRevision, int toRevision) {
        List<DocumentRevision> allRevisions = revisionRepository.findByDocumentOrderByRevisionNumberAsc(document);
        List<DocumentRevision> filtered = new ArrayList<>();

        for (DocumentRevision rev : allRevisions) {
            if (rev.getRevisionNumber() >= fromRevision && rev.getRevisionNumber() <= toRevision) {
                filtered.add(rev);
            }
        }

        return filtered;
    }

    /**
     * Get the "impact range" - revisions where significant changes occurred
     * Used to determine which revisions' evidence is still valid
     */
    public List<RevisionChangePoint> identifyChangePoints(Document document) {
        List<DocumentRevision> revisions = getRevisionHistory(document);
        List<RevisionChangePoint> changePoints = new ArrayList<>();

        for (int i = 1; i < revisions.size(); i++) {
            DocumentRevision prior = revisions.get(i);
            DocumentRevision current = revisions.get(i - 1);

            RevisionDelta delta = compareRevisions(prior, current);

            // Change point if content significantly changed or chunks changed significantly
            boolean significantChange = delta.isContentChanged() &&
                                       Math.abs(delta.getPercentageChange()) > 5.0; // More than 5% change

            if (significantChange || Math.abs(delta.getChunkCountChange()) > 10) {
                RevisionChangePoint changePoint = new RevisionChangePoint();
                changePoint.setFromRevisionNumber(prior.getRevisionNumber());
                changePoint.setToRevisionNumber(current.getRevisionNumber());
                changePoint.setChangeType(delta.getPercentageChange() > 0 ? "EXPANSION" : "CONTRACTION");
                changePoint.setMagnitude(Math.abs(delta.getPercentageChange()));
                changePoint.setChangedAt(current.getCreatedAt());
                changePoints.add(changePoint);
            }
        }

        return changePoints;
    }

    /**
     * DTO: Encapsulates changes between two revisions
     */
    public static class RevisionDelta {
        private int fromRevisionNumber;
        private int toRevisionNumber;
        private String fromRevisionKey;
        private String toRevisionKey;
        private long sizeDifference;
        private double percentageChange;
        private int chunkCountChange;
        private boolean contentChanged;
        private String changeSummary;

        // Getters and setters
        public int getFromRevisionNumber() { return fromRevisionNumber; }
        public void setFromRevisionNumber(int fromRevisionNumber) { this.fromRevisionNumber = fromRevisionNumber; }

        public int getToRevisionNumber() { return toRevisionNumber; }
        public void setToRevisionNumber(int toRevisionNumber) { this.toRevisionNumber = toRevisionNumber; }

        public String getFromRevisionKey() { return fromRevisionKey; }
        public void setFromRevisionKey(String fromRevisionKey) { this.fromRevisionKey = fromRevisionKey; }

        public String getToRevisionKey() { return toRevisionKey; }
        public void setToRevisionKey(String toRevisionKey) { this.toRevisionKey = toRevisionKey; }

        public long getSizeDifference() { return sizeDifference; }
        public void setSizeDifference(long sizeDifference) { this.sizeDifference = sizeDifference; }

        public double getPercentageChange() { return percentageChange; }
        public void setPercentageChange(double percentageChange) { this.percentageChange = percentageChange; }

        public int getChunkCountChange() { return chunkCountChange; }
        public void setChunkCountChange(int chunkCountChange) { this.chunkCountChange = chunkCountChange; }

        public boolean isContentChanged() { return contentChanged; }
        public void setContentChanged(boolean contentChanged) { this.contentChanged = contentChanged; }

        public String getChangeSummary() { return changeSummary; }
        public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
    }

    /**
     * DTO: Marks significant revision boundaries
     */
    public static class RevisionChangePoint {
        private int fromRevisionNumber;
        private int toRevisionNumber;
        private String changeType; // EXPANSION, CONTRACTION, REORG
        private double magnitude;
        private LocalDateTime changedAt;

        public int getFromRevisionNumber() { return fromRevisionNumber; }
        public void setFromRevisionNumber(int fromRevisionNumber) { this.fromRevisionNumber = fromRevisionNumber; }

        public int getToRevisionNumber() { return toRevisionNumber; }
        public void setToRevisionNumber(int toRevisionNumber) { this.toRevisionNumber = toRevisionNumber; }

        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }

        public double getMagnitude() { return magnitude; }
        public void setMagnitude(double magnitude) { this.magnitude = magnitude; }

        public LocalDateTime getChangedAt() { return changedAt; }
        public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    }
}
