package com.spacedoc.rest;

import com.spacedoc.dto.ImpactAnalysisDTO;
import com.spacedoc.service.ImpactAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Document Impact Analysis.
 *
 * RESEARCH CONTRIBUTION: This endpoint exposes the impact analysis functionality
 * that demonstrates "impact propagation" across document dependencies.
 *
 * POST /api/impact/analyze
 * {
 *   "documentId": 123,
 *   "revisionId": 456,  // optional, defaults to latest
 *   "changeDescription": "Updated propulsion requirements"
 * }
 */
@RestController
@RequestMapping("/api/impact")
public class ImpactAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(ImpactAnalysisController.class);

    private final ImpactAnalysisService impactAnalysisService;

    public ImpactAnalysisController(ImpactAnalysisService impactAnalysisService) {
        this.impactAnalysisService = impactAnalysisService;
    }

    /**
     * Analyze the impact of a document change.
     *
     * POST /api/impact/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeImpact(@RequestBody ImpactRequestDTO request) {
        try {
            if (request.getDocumentId() == null) {
                return ResponseEntity.badRequest()
                    .body(errorResponse("Document ID is required"));
            }

            log.info("Impact analysis requested for document {}", request.getDocumentId());

            ImpactAnalysisDTO analysis = impactAnalysisService.analyzeImpact(
                request.getDocumentId(),
                request.getRevisionId(),
                request.getChangeDescription()
            );

            log.info("Impact analysis complete: {} total impacted documents",
                analysis.getTotalImpactedDocuments());

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("Error during impact analysis", e);
            return ResponseEntity.status(500)
                .body(errorResponse("Impact analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Get a previously run impact analysis by ID.
     * (Placeholder - would persist results in production)
     *
     * GET /api/impact/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getImpactAnalysis(@PathVariable Long id) {
        // Placeholder: In production, would fetch from persistence store
        return ResponseEntity.notFound().build();
    }

    /**
     * Request DTO for impact analysis.
     */
    public static class ImpactRequestDTO {
        private Long documentId;
        private Long revisionId;
        private String changeDescription;

        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }

        public Long getRevisionId() { return revisionId; }
        public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }

        public String getChangeDescription() { return changeDescription; }
        public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
