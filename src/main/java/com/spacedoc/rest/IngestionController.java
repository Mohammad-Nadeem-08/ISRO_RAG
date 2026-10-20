package com.spacedoc.rest;

import com.spacedoc.dto.IngestionRequestDTO;
import com.spacedoc.dto.IngestionResponseDTO;
import com.spacedoc.entity.IngestionJob;
import com.spacedoc.service.DocumentIngestionService;
import com.spacedoc.service.TextExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final DocumentIngestionService ingestionService;
    private final TextExtractionService textExtractionService;

    public IngestionController(DocumentIngestionService ingestionService,
            TextExtractionService textExtractionService) {
        this.ingestionService = ingestionService;
        this.textExtractionService = textExtractionService;
    }

    /**
     * Upload a PDF document for ingestion
     * POST /api/ingestion/upload
     * Form parameters: file (MultipartFile), title, organization
     */
    @PostMapping("/upload")
    public ResponseEntity<IngestionResponseDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("organization") String organization) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "File is empty", 0));
            }

            if (file.getSize() > 50 * 1024 * 1024) { // 50 MB limit
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "File exceeds 50 MB limit", 0));
            }

            // Extract text from PDF
            String extractedText = textExtractionService.extractText(file.getInputStream());

            if (extractedText == null || extractedText.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "No text extracted from PDF", 0));
            }

            // Ingest document
            IngestionJob job = ingestionService.ingestDocument(
                    file.getOriginalFilename(),
                    title,
                    organization,
                    extractedText,
                    "PDF");

            IngestionResponseDTO response = new IngestionResponseDTO(
                    job.getJobKey(),
                    job.getStatus(),
                    job.getErrorMessage(),
                    job.getProgressPercent());

            log.info("Document uploaded: {} (jobKey: {})", title, job.getJobKey());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IOException e) {
            log.error("Error uploading document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new IngestionResponseDTO(null, "FAILED", "Error processing file: " + e.getMessage(), 0));
        } catch (Exception e) {
            log.error("Unexpected error during document upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new IngestionResponseDTO(null, "FAILED", "Unexpected error: " + e.getMessage(), 0));
        }
    }

    /**
     * Submit a URL for document ingestion
     * POST /api/ingestion/url
     * Body: IngestionRequestDTO with sourceUrl, title, organization
     */
    @PostMapping("/url")
    public ResponseEntity<IngestionResponseDTO> submitUrl(
            @RequestBody IngestionRequestDTO request) {

        try {
            if (request.getSourceUrl() == null || request.getSourceUrl().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "Source URL is required", 0));
            }

            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "Document title is required", 0));
            }

            // Extract text from URL
            String extractedText = textExtractionService.extractTextFromUrl(request.getSourceUrl());

            if (extractedText == null || extractedText.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "No text extracted from URL", 0));
            }

            // Ingest document
            IngestionJob job = ingestionService.ingestDocument(
                    request.getSourceUrl(),
                    request.getTitle(),
                    request.getOrganization() != null ? request.getOrganization() : "Unknown",
                    extractedText,
                    "WEB");

            IngestionResponseDTO response = new IngestionResponseDTO(
                    job.getJobKey(),
                    job.getStatus(),
                    job.getErrorMessage(),
                    job.getProgressPercent());

            log.info("URL submitted for ingestion: {} (jobKey: {})", request.getSourceUrl(), job.getJobKey());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            log.error("Error ingesting URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new IngestionResponseDTO(null, "FAILED", "Error: " + e.getMessage(), 0));
        }
    }

    /**
     * Get ingestion job status
     * GET /api/ingestion/jobs/{jobKey}
     */
    @GetMapping("/jobs/{jobKey}")
    public ResponseEntity<IngestionResponseDTO> getJobStatus(
            @PathVariable String jobKey) {

        try {
            // Would fetch from repository in production
            // For now, return placeholder
            return ResponseEntity.ok(
                    new IngestionResponseDTO(jobKey, "PROCESSING", null, 50));
        } catch (Exception e) {
            log.error("Error fetching job status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new IngestionResponseDTO(jobKey, "FAILED", "Error: " + e.getMessage(), 0));
        }
    }
}
