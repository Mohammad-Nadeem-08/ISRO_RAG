package com.spacedoc.rest;

import com.spacedoc.dto.QueryRequestDTO;
import com.spacedoc.dto.RetrievalResultDTO;
import com.spacedoc.service.RetrievalService;
import com.spacedoc.service.AnswerGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RetrievalController {

    private static final Logger log = LoggerFactory.getLogger(RetrievalController.class);

    private final RetrievalService retrievalService;
    private final AnswerGenerationService answerGenerationService;

    public RetrievalController(RetrievalService retrievalService,
                              AnswerGenerationService answerGenerationService) {
        this.retrievalService = retrievalService;
        this.answerGenerationService = answerGenerationService;
    }

    /**
     * Execute hybrid retrieval for a query
     * POST /api/rag/retrieve
     * Body: QueryRequestDTO with query, topK
     */
    @PostMapping("/retrieve")
    public ResponseEntity<Map<String, Object>> retrieve(
            @RequestBody QueryRequestDTO request) {

        try {
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(errorResponse("Query cannot be empty"));
            }

            int topK = request.getTopK() != null ? request.getTopK() : 10;
            if (topK < 1 || topK > 100) {
                return ResponseEntity.badRequest()
                    .body(errorResponse("topK must be between 1 and 100"));
            }

            // Perform hybrid retrieval
            List<RetrievalResultDTO> results = retrievalService.hybridRetrieval(
                request.getQuery(),
                topK
            );

            // Deduplicate results
            results = retrievalService.deduplicateResults(results);

            Map<String, Object> response = new HashMap<>();
            response.put("query", request.getQuery());
            response.put("resultCount", results.size());
            response.put("results", results);

            log.info("Retrieved {} results for query: {}", results.size(), request.getQuery());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during retrieval", e);
            return ResponseEntity.status(500)
                .body(errorResponse("Retrieval failed: " + e.getMessage()));
        }
    }

    /**
     * Lexical-only retrieval (full-text search)
     * POST /api/rag/retrieve/lexical
     */
    @PostMapping("/retrieve/lexical")
    public ResponseEntity<Map<String, Object>> retrieveLexical(
            @RequestBody QueryRequestDTO request) {

        try {
            int topK = request.getTopK() != null ? request.getTopK() : 10;
            List<RetrievalResultDTO> results = retrievalService.lexicalRetrieval(
                request.getQuery(),
                topK
            );

            Map<String, Object> response = new HashMap<>();
            response.put("query", request.getQuery());
            response.put("resultCount", results.size());
            response.put("results", results);
            response.put("method", "lexical");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during lexical retrieval", e);
            return ResponseEntity.status(500)
                .body(errorResponse("Lexical retrieval failed: " + e.getMessage()));
        }
    }

    /**
     * Dense vector retrieval
     * POST /api/rag/retrieve/dense
     */
    @PostMapping("/retrieve/dense")
    public ResponseEntity<Map<String, Object>> retrieveDense(
            @RequestBody QueryRequestDTO request) {

        try {
            int topK = request.getTopK() != null ? request.getTopK() : 10;

            // In production, would embed the query here
            // String queryVector = embeddingService.embed(request.getQuery());
            // List<RetrievalResultDTO> results = retrievalService.denseRetrieval(queryVector, topK);

            List<RetrievalResultDTO> results = retrievalService.denseRetrieval(
                request.getQuery(),
                topK
            );

            Map<String, Object> response = new HashMap<>();
            response.put("query", request.getQuery());
            response.put("resultCount", results.size());
            response.put("results", results);
            response.put("method", "dense");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during dense retrieval", e);
            return ResponseEntity.status(500)
                .body(errorResponse("Dense retrieval failed: " + e.getMessage()));
        }
    }

    /**
     * Query with RAG answer generation (Phase 4b - IMPLEMENTED)
     * POST /api/rag/ask
     * Body: QueryRequestDTO with query and optional topK
     * Response: Answer with citations mapped to evidence chunks
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(
            @RequestBody QueryRequestDTO request) {

        try {
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(errorResponse("Query cannot be empty"));
            }

            int topK = request.getTopK() != null ? request.getTopK() : 5;

            // Retrieve evidence using hybrid retrieval
            List<RetrievalResultDTO> evidence = retrievalService.hybridRetrieval(
                request.getQuery(),
                topK
            );

            evidence = retrievalService.deduplicateResults(evidence);

            // Generate answer from evidence
            AnswerGenerationService.GeneratedAnswer generatedAnswer =
                answerGenerationService.generateAnswer(request.getQuery(), evidence);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("query", request.getQuery());
            response.put("evidenceCount", evidence.size());
            response.put("evidence", evidence);
            response.put("answer", generatedAnswer.getAnswerText());
            response.put("citations", generatedAnswer.getCitations());
            response.put("confidence", generatedAnswer.getConfidence());
            response.put("citationCoverage", generatedAnswer.getCitationCoverage());
            response.put("generationMethod", generatedAnswer.getGenerationMethod());

            log.info("RAG query: {} → Answer generated with {} citations from {} evidence items",
                request.getQuery(), generatedAnswer.getCitations().size(), evidence.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during RAG query", e);
            return ResponseEntity.status(500)
                .body(errorResponse("RAG query failed: " + e.getMessage()));
        }
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
