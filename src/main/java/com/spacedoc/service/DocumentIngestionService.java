package com.spacedoc.service;

import com.pgvector.PGvector;
import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentChunk;
import com.spacedoc.entity.DocumentRevision;
import com.spacedoc.entity.IngestionJob;
import com.spacedoc.exception.IngestionException;
import com.spacedoc.repository.DocumentChunkRepository;
import com.spacedoc.repository.DocumentRepository;
import com.spacedoc.repository.DocumentRevisionRepository;
import com.spacedoc.repository.IngestionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentRevisionRepository revisionRepository;
    private final DocumentChunkRepository chunkRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;

    public DocumentIngestionService(DocumentRepository documentRepository,
                                   DocumentRevisionRepository revisionRepository,
                                   DocumentChunkRepository chunkRepository,
                                   IngestionJobRepository ingestionJobRepository,
                                   ChunkingService chunkingService,
                                   EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.revisionRepository = revisionRepository;
        this.chunkRepository = chunkRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }

    @Transactional(noRollbackFor = IngestionException.class)
    public IngestionJob ingestDocument(String sourceUrl, String documentTitle, String sourceOrganization,
                                       String extractedText, String sourceType) {
        String jobKey = UUID.randomUUID().toString();
        IngestionJob job = new IngestionJob();
        job.setJobKey(jobKey);
        job.setJobType("PDF".equalsIgnoreCase(sourceType) ? "PDF_INGESTION" : "URL_INGESTION");
        job.setSourceUrl(sourceUrl);
        job.setSourceFilename("PDF".equalsIgnoreCase(sourceType) ? sourceUrl : null);
        job.setStatus("PROCESSING");
        job.setProgressPercent(0);
        job.setCreatedAt(LocalDateTime.now());

        job = ingestionJobRepository.save(job);
        IngestionJob savedJob = job;

        boolean isNewDocument = false;
        Document document = null;
        DocumentRevision savedRevision = null;

        try {
            savedJob.setStartedAt(LocalDateTime.now());
            savedJob.setProgressPercent(10);
            ingestionJobRepository.save(savedJob);

            // Generate content hash
            String contentHash = generateContentHash(extractedText);
            savedJob.setProgressPercent(20);

            // Check if document exists
            String docKey = generateDocumentKey(documentTitle);
            Optional<Document> existingDoc = documentRepository.findByDocumentKey(docKey);

            if (existingDoc.isPresent()) {
                document = existingDoc.get();
                if (contentHash.equals(document.getContentHash())) {
                    log.info("Document {} has not changed, skipping ingestion", docKey);
                    savedJob.setStatus("SKIPPED");
                    savedJob.setCompletedAt(LocalDateTime.now());
                    ingestionJobRepository.save(savedJob);
                    return savedJob;
                }
            } else {
                isNewDocument = true;
                document = new Document();
                document.setDocumentKey(docKey);
                document.setTitle(documentTitle);
                document.setSourceUrl(sourceUrl);
                document.setSourceType(sourceType);
                document.setSourceOrganization(sourceOrganization);
                document.setContentHash(contentHash);
                document.setCreatedAt(LocalDateTime.now());
                document.setUpdatedAt(LocalDateTime.now());
                document = documentRepository.save(document);
            }

            savedJob.setSourceDocument(document);
            savedJob.setProgressPercent(30);
            ingestionJobRepository.save(savedJob);

            // Create new revision
            DocumentRevision prior = revisionRepository.findCurrentRevision(document.getId()).orElse(null);
            int newRevisionNumber = prior != null ? prior.getRevisionNumber() + 1 : 1;

            DocumentRevision revision = new DocumentRevision();
            revision.setDocument(document);
            revision.setRevisionNumber(newRevisionNumber);
            revision.setRevisionKey(docKey + "_v" + newRevisionNumber);
            revision.setContentHash(contentHash);
            revision.setPriorRevision(prior);
            revision.setChangeSummary("Ingested from " + sourceUrl);
            revision.setIsCurrent(true);
            revision.setIngestedAt(LocalDateTime.now());
            revision.setCreatedAt(LocalDateTime.now());

            if (prior != null) {
                prior.setIsCurrent(false);
                revisionRepository.save(prior);
            }

            revision = revisionRepository.save(revision);
            savedRevision = revision;

            savedJob.setProgressPercent(40);
            ingestionJobRepository.save(savedJob);

            // Chunk the text
            List<DocumentChunk> chunks = chunkingService.chunkDocument(extractedText, savedRevision);
            if (chunks.isEmpty()) {
                throw new IngestionException("Text extraction produced 0 usable chunks for document: " + documentTitle);
            }

            savedJob.setProgressPercent(60);
            ingestionJobRepository.save(savedJob);

            // Generate embeddings
            for (int i = 0; i < chunks.size(); i++) {
                String embedding = embeddingService.embed(chunks.get(i).getContent());
                try {
                    chunks.get(i).setEmbedding(embedding != null ? new PGvector(embedding) : null);
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException("Failed to create PGvector from embedding string", e);
                }

                if (i % 10 == 0) {
                    savedJob.setProgressPercent(60 + (int)(30 * (i / (double) chunks.size())));
                    ingestionJobRepository.save(savedJob);
                }
            }

            chunkRepository.saveAll(chunks);

            savedRevision.setTotalChunks(chunks.size());
            savedRevision.setExtractedTextLength((long) extractedText.length());
            revisionRepository.save(savedRevision);

            document.setContentHash(contentHash);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);

            savedJob.setChunksCreated(chunks.size());
            savedJob.setStatus("COMPLETED");
            savedJob.setProgressPercent(100);
            savedJob.setCompletedAt(LocalDateTime.now());
            ingestionJobRepository.save(savedJob);

            log.info("Successfully ingested document {} with {} chunks", docKey, chunks.size());
            return savedJob;

        } catch (Exception e) {
            log.error("Error ingesting document: {}", e.getMessage(), e);
            // Clean up partial entities to prevent duplicate or orphaned records
            try {
                if (savedRevision != null && savedRevision.getId() != null) {
                    revisionRepository.delete(savedRevision);
                }
                if (isNewDocument && document != null && document.getId() != null) {
                    documentRepository.delete(document);
                }
            } catch (Exception cleanEx) {
                log.warn("Error cleaning up partial records: {}", cleanEx.getMessage());
            }

            savedJob.setStatus("FAILED");
            savedJob.setErrorMessage(e.getMessage());
            savedJob.setCompletedAt(LocalDateTime.now());
            ingestionJobRepository.save(savedJob);
            throw new IngestionException("Failed to ingest document: " + e.getMessage(), e);
        }
    }

    private String generateDocumentKey(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_|_$", "");
    }

    private String generateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IngestionException("SHA-256 not available", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<IngestionJob> getJob(String jobKey) {
        if (jobKey == null || jobKey.trim().isEmpty()) {
            return Optional.empty();
        }
        return ingestionJobRepository.findByJobKey(jobKey.trim());
    }
}
