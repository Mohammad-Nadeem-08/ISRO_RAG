package com.spacedoc.rest;

import com.spacedoc.dto.IngestionRequestDTO;
import com.spacedoc.dto.IngestionResponseDTO;
import com.spacedoc.entity.IngestionJob;
import com.spacedoc.exception.IngestionException;
import com.spacedoc.service.DocumentIngestionService;
import com.spacedoc.service.TextExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "Please select a valid non-empty file to upload", 0));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "Invalid file type. Only PDF documents (.pdf) are supported for upload", 0));
            }

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "Document title is required", 0));
            }

            if (file.getSize() > 50 * 1024 * 1024) { // 50 MB limit
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(new IngestionResponseDTO(null, "FAILED", "File exceeds the 50 MB upload limit", 0));
            }

            // Extract text from PDF
            String extractedText = textExtractionService.extractText(file.getInputStream());

            if (extractedText == null || extractedText.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(new IngestionResponseDTO(null, "FAILED", "No selectable text could be extracted from PDF (document may be scanned or image-only)", 0));
            }

            // Ingest document
            IngestionJob job = ingestionService.ingestDocument(
                    filename,
                    title.trim(),
                    organization != null && !organization.trim().isEmpty() ? organization.trim() : "ISRO",
                    extractedText,
                    "PDF");

            IngestionResponseDTO response = new IngestionResponseDTO(
                    job.getJobKey(),
                    job.getStatus(),
                    job.getErrorMessage(),
                    job.getProgressPercent());

            log.info("Document uploaded: {} (jobKey: {})", title, job.getJobKey());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IngestionException e) {
            log.warn("Ingestion error processing uploaded PDF: {}", e.getMessage());
            HttpStatus status = (e.getMessage() != null && e.getMessage().contains("no selectable text"))
                    ? HttpStatus.UNPROCESSABLE_ENTITY
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new IngestionResponseDTO(null, "FAILED", e.getMessage(), 0));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid upload arguments: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new IngestionResponseDTO(null, "FAILED", e.getMessage(), 0));
        } catch (IOException e) {
            log.error("IO error reading uploaded document", e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new IngestionResponseDTO(null, "FAILED", "Error reading file stream: " + e.getMessage(), 0));
        } catch (Exception e) {
            log.error("Unexpected error during document upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new IngestionResponseDTO(null, "FAILED", "Unexpected server error during upload: " + e.getMessage(), 0));
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
            if (request == null) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "Request body is required", 0));
            }

            if (request.getSourceUrl() == null || request.getSourceUrl().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "Source URL is required", 0));
            }

            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new IngestionResponseDTO(null, "FAILED", "Document title is required", 0));
            }

            // Extract text from URL (includes SSRF check, redirect following, size limits)
            String extractedText = textExtractionService.extractTextFromUrl(request.getSourceUrl());

            if (extractedText == null || extractedText.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(new IngestionResponseDTO(null, "FAILED", "No text extracted from URL", 0));
            }

            // Ingest document
            IngestionJob job = ingestionService.ingestDocument(
                    request.getSourceUrl().trim(),
                    request.getTitle().trim(),
                    request.getOrganization() != null && !request.getOrganization().trim().isEmpty()
                            ? request.getOrganization().trim() : "ISRO",
                    extractedText,
                    "WEB");

            IngestionResponseDTO response = new IngestionResponseDTO(
                    job.getJobKey(),
                    job.getStatus(),
                    job.getErrorMessage(),
                    job.getProgressPercent());

            log.info("URL submitted for ingestion: {} (jobKey: {})", request.getSourceUrl(), job.getJobKey());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IngestionException e) {
            log.warn("Ingestion error processing URL: {}", e.getMessage());
            HttpStatus status = (e.getMessage() != null && (e.getMessage().contains("SSRF") || e.getMessage().contains("protocol") || e.getMessage().contains("syntax")))
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.UNPROCESSABLE_ENTITY;
            return ResponseEntity.status(status)
                    .body(new IngestionResponseDTO(null, "FAILED", e.getMessage(), 0));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid URL ingestion arguments: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new IngestionResponseDTO(null, "FAILED", e.getMessage(), 0));
        } catch (Exception e) {
            log.error("Error ingesting URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new IngestionResponseDTO(null, "FAILED", "Error processing URL: " + e.getMessage(), 0));
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<IngestionResponseDTO> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        log.warn("File upload size exceeded limit: {}", exc.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new IngestionResponseDTO(null, "FAILED", "File exceeds the 50 MB upload size limit", 0));
    }

    /**
     * Get ingestion job status
     * GET /api/ingestion/jobs/{jobKey}
     * GET /api/ingestion/status/{jobKey}
     */
    @GetMapping({"/jobs/{jobKey}", "/status/{jobKey}"})
    public ResponseEntity<IngestionResponseDTO> getJobStatus(
            @PathVariable String jobKey) {

        try {
            return ingestionService.getJob(jobKey)
                    .map(job -> ResponseEntity.ok(new IngestionResponseDTO(
                            job.getJobKey(),
                            job.getStatus(),
                            job.getErrorMessage(),
                            job.getProgressPercent() != null ? job.getProgressPercent() : ("COMPLETED".equals(job.getStatus()) ? 100 : 0)
                    )))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new IngestionResponseDTO(jobKey, "NOT_FOUND", "Ingestion job not found", 0)));
        } catch (Exception e) {
            log.error("Error fetching job status for key: {}", jobKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new IngestionResponseDTO(jobKey, "FAILED", "Error: " + e.getMessage(), 0));
        }
    }
}
