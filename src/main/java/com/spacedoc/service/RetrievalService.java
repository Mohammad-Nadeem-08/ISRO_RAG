package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import com.spacedoc.entity.DocumentChunk;
import com.spacedoc.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    public RetrievalService(DocumentChunkRepository chunkRepository, EmbeddingService embeddingService) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
    }

    @Value("${retrieval.top-k:10}")
    private int defaultTopK;

    @Value("${retrieval.similarity-threshold:0.5}")
    private float similarityThreshold;

    @Value("${retrieval.lexical-weight:0.3}")
    private float lexicalWeight;

    @Value("${retrieval.vector-weight:0.7}")
    private float vectorWeight;

    /**
     * Hybrid retrieval combining dense and lexical search
     * Uses RRF (Reciprocal Rank Fusion) to merge results
     */
    public List<RetrievalResultDTO> hybridRetrieval(String query, int topK) {
        try {
            if (query == null || query.trim().isEmpty()) {
                log.warn("Query is null or empty");
                return new ArrayList<>();
            }

            log.info("Executing hybrid retrieval for query: '{}' with topK={}", query, topK);

            // 1. Execute lexical search
            List<RetrievalResultDTO> lexicalResults = lexicalRetrieval(query, topK);
            log.debug("Lexical retrieval returned {} results", lexicalResults.size());

            // 2. Execute dense vector search
            List<RetrievalResultDTO> denseResults = new ArrayList<>();
            try {
                String queryEmbedding = embeddingService.embed(query);
                denseResults = denseRetrieval(queryEmbedding, topK);
                log.debug("Dense retrieval returned {} results", denseResults.size());
            } catch (Exception e) {
                log.warn("Dense retrieval skipped due to embedding failure: {}. Proceeding with lexical-only results.", e.getMessage());
            }

            if (lexicalResults.isEmpty() && denseResults.isEmpty()) {
                log.warn("No results found for query from either lexical or dense search: {}", query);
            }

            // 3. Merge using RRF
            List<RetrievalResultDTO> merged = reciprocalRankFusion(denseResults, lexicalResults, topK);

            log.info("Hybrid retrieval returned {} merged results", merged.size());
            return merged;
        } catch (Exception e) {
            log.error("Retrieval failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * Lexical-only retrieval using PostgreSQL full-text search (tsvector)
     */
    public List<RetrievalResultDTO> lexicalRetrieval(String query, int topK) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return Collections.emptyList();
            }

            log.debug("Executing lexical retrieval for: '{}' with topK={}", query, topK);

            Pageable pageable = PageRequest.of(0, Math.max(topK, 100));
            List<DocumentChunk> chunks = chunkRepository.searchByFullText(query, pageable);

            List<RetrievalResultDTO> results = chunks.stream()
                .map(chunk -> {
                    RetrievalResultDTO dto = convertToDTO(chunk);
                    dto.setRelevanceScore(1.0f);  // Lexical search sets baseline score
                    return dto;
                })
                .collect(Collectors.toList());

            log.debug("Lexical search found {} chunks for query: {}", results.size(), query);
            return results;
        } catch (Exception e) {
            log.error("Lexical retrieval failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * Dense-only retrieval using vector similarity (pgvector)
     * queryVector: JSON string representation of embedding vector
     */
    public List<RetrievalResultDTO> denseRetrieval(String queryVector, int topK) {
        try {
            if (queryVector == null || queryVector.trim().isEmpty()) {
                log.warn("Query vector is null or empty");
                return Collections.emptyList();
            }

            log.debug("Executing dense retrieval with vector similarity");

            Pageable pageable = PageRequest.of(0, Math.max(topK, 100));
            List<DocumentChunk> chunks = chunkRepository.findByEmbeddingSimilarity(queryVector, pageable);

            List<RetrievalResultDTO> results = chunks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            log.debug("Dense search found {} chunks", results.size());
            return results;
        } catch (Exception e) {
            log.error("Dense retrieval failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * Reciprocal Rank Fusion to combine multiple ranking results
     * RRF formula: score = sum(1 / (k + rank))
     * where k is typically 60
     */
    public List<RetrievalResultDTO> reciprocalRankFusion(
        List<RetrievalResultDTO> denseResults,
        List<RetrievalResultDTO> lexicalResults,
        int topK) {

        final int k = 60;  // Standard RRF constant
        Map<Long, Float> rrfScores = new HashMap<>();
        Map<Long, RetrievalResultDTO> resultMap = new HashMap<>();

        // Score dense results
        for (int rank = 0; rank < denseResults.size(); rank++) {
            RetrievalResultDTO result = denseResults.get(rank);
            float rrfScore = 1.0f / (k + rank + 1);
            float weightedScore = rrfScore * vectorWeight;
            rrfScores.put(result.getChunkId(), weightedScore);
            resultMap.put(result.getChunkId(), result);
        }

        // Score lexical results
        for (int rank = 0; rank < lexicalResults.size(); rank++) {
            RetrievalResultDTO result = lexicalResults.get(rank);
            float rrfScore = 1.0f / (k + rank + 1);
            float weightedScore = rrfScore * lexicalWeight;
            Long chunkId = result.getChunkId();

            if (rrfScores.containsKey(chunkId)) {
                // Chunk appeared in both lists: combine scores
                rrfScores.put(chunkId, rrfScores.get(chunkId) + weightedScore);
            } else {
                rrfScores.put(chunkId, weightedScore);
                resultMap.put(chunkId, result);
            }
        }

        // Sort by RRF score descending
        List<RetrievalResultDTO> combined = rrfScores.entrySet().stream()
            .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
            .limit(topK)
            .map(entry -> {
                RetrievalResultDTO dto = resultMap.get(entry.getKey());
                dto.setRelevanceScore(entry.getValue());
                return dto;
            })
            .collect(Collectors.toList());

        log.debug("RRF merged {} dense + {} lexical results into {} combined results",
            denseResults.size(), lexicalResults.size(), combined.size());

        return combined;
    }

    /**
     * Deduplicate similar chunks
     * Removes chunks with nearly identical content
     */
    public List<RetrievalResultDTO> deduplicateResults(List<RetrievalResultDTO> results) {
        Map<String, RetrievalResultDTO> seenContent = new LinkedHashMap<>();

        for (RetrievalResultDTO result : results) {
            String content = result.getContent();
            // Handle empty/null content by using chunk ID as key
            String contentHash;
            if (content == null || content.isEmpty()) {
                contentHash = "empty_" + result.getChunkId();
            } else {
                contentHash = hashPrefix(content, 50);
            }

            if (!seenContent.containsKey(contentHash)) {
                seenContent.put(contentHash, result);
            }
        }

        log.debug("Deduplication: {} -> {} results", results.size(), seenContent.size());
        return new ArrayList<>(seenContent.values());
    }

    /**
     * Convert DocumentChunk entity to DTO
     */
    private RetrievalResultDTO convertToDTO(DocumentChunk chunk) {
        RetrievalResultDTO dto = new RetrievalResultDTO();
        dto.setChunkId(chunk.getId());
        dto.setRevisionId(chunk.getDocumentRevision().getId());
        dto.setContent(chunk.getContent());
        dto.setSectionTitle(chunk.getSectionTitle());
        dto.setPageNumber(chunk.getPageNumber());
        dto.setRelevanceScore(1.0f);  // Default score; will be set by ranking algorithm
        return dto;
    }

    /**
     * Hash prefix of content for deduplication
     */
    private String hashPrefix(String content, int prefixLength) {
        String prefix = content.substring(0, Math.min(prefixLength, content.length()));
        return Integer.toString(prefix.hashCode());
    }
}
