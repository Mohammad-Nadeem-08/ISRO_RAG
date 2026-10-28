package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentChunk;
import com.spacedoc.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
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

            // 4. Rerank by Subject and Relationship matching
            merged = rerankBySubjectAndRelationship(merged, query);

            log.info("Hybrid retrieval returned {} merged results", merged.size());
            return merged;
        } catch (Exception e) {
            log.error("Retrieval failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * Rerank retrieved evidence so chunks must match the specific subject entity and
     * relationship asked about, rather than merely sharing generic technical terminology.
     */
    public List<RetrievalResultDTO> rerankBySubjectAndRelationship(List<RetrievalResultDTO> results, String query) {
        if (results == null || results.isEmpty() || query == null || query.trim().isEmpty()) {
            return results != null ? results : Collections.emptyList();
        }

        QuerySubjectAnalysis analysis = analyzeQuerySubject(query);
        if (!analysis.hasSpecificEntity()) {
            return results;
        }

        List<RetrievalResultDTO> reranked = new ArrayList<>(results);
        for (RetrievalResultDTO result : reranked) {
            float baseScore = result.getRelevanceScore() != null ? result.getRelevanceScore() : 1.0f;
            float subjectFactor = computeSubjectMatchFactor(result, analysis);
            float relationshipFactor = computeRelationshipMatchFactor(result, analysis);

            // Combined rerank score: penalize chunks that miss the target subject or describe a mismatched entity category
            float adjustedScore = baseScore * (0.65f * subjectFactor + 0.35f * relationshipFactor);
            result.setRelevanceScore(adjustedScore);
        }

        // Sort descending by adjusted score
        reranked.sort((a, b) -> Float.compare(
            b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0f,
            a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0f
        ));

        return reranked;
    }

    public static class QuerySubjectAnalysis {
        private final Set<String> targetEntities = new HashSet<>();
        private boolean asksAboutSpacecraft = false;
        private boolean asksAboutLaunchVehicle = false;
        private boolean asksAboutOrganization = false;
        private boolean definitionalQuery = false;
        private String definitionalTarget = null;
        private final Set<String> relationshipTerms = new HashSet<>();

        public boolean hasSpecificEntity() {
            return !targetEntities.isEmpty() || !relationshipTerms.isEmpty() || definitionalQuery;
        }

        public Set<String> getTargetEntities() { return targetEntities; }
        public boolean isAsksAboutSpacecraft() { return asksAboutSpacecraft; }
        public boolean isAsksAboutLaunchVehicle() { return asksAboutLaunchVehicle; }
        public boolean isAsksAboutOrganization() { return asksAboutOrganization; }
        public boolean isDefinitionalQuery() { return definitionalQuery; }
        public String getDefinitionalTarget() { return definitionalTarget; }
        public Set<String> getRelationshipTerms() { return relationshipTerms; }
    }

    public QuerySubjectAnalysis analyzeQuerySubject(String query) {
        QuerySubjectAnalysis analysis = new QuerySubjectAnalysis();
        String lowerQuery = query.toLowerCase();

        // Spacecraft / mission entities
        List<String> spacecraftList = List.of(
            "chandrayaan-3", "chandrayaan-2", "chandrayaan-1", "chandrayaan",
            "mangalyaan", "aditya-l1", "aditya", "gaganyaan", "nisar",
            "astrosat", "risat", "cartosat", "eos", "insat", "gsat",
            "lander", "rover", "orbiter", "spacecraft", "satellite"
        );

        // Launch vehicle / stage / engine entities
        List<String> lvList = List.of(
            "lvm3", "gslv mk iii", "gslv mk ii", "gslv", "pslv", "sslv",
            "ce-20", "ce20", "vikas", "c25", "cus", "cryogenic upper stage",
            "launch vehicle", "upper stage", "rocket stage", "rocket"
        );

        // Organization / agency entities
        List<String> orgList = List.of(
            "isro", "nasa", "esa", "jaxa", "cnsa", "roscosmos", "drdo"
        );

        for (String sc : spacecraftList) {
            if (lowerQuery.contains(sc)) {
                analysis.targetEntities.add(sc);
                analysis.asksAboutSpacecraft = true;
            }
        }

        for (String lv : lvList) {
            if (lowerQuery.contains(lv)) {
                analysis.targetEntities.add(lv);
                analysis.asksAboutLaunchVehicle = true;
            }
        }

        for (String org : orgList) {
            if (lowerQuery.matches(".*\\b" + Pattern.quote(org) + "\\b.*")) {
                analysis.targetEntities.add(org);
                analysis.asksAboutOrganization = true;
            }
        }

        // Definitional query detection (e.g. "what is isro", "who is isro", "define isro", "what does isro stand for")
        boolean isDefinitional = lowerQuery.matches(".*\\b(what\\s+is|who\\s+is|define|meaning\\s+of|stands?\\s+for|tell\\s+me\\s+about)\\b.*")
                || lowerQuery.equals("isro") || lowerQuery.equals("isro?") || lowerQuery.equals("what is isro") || lowerQuery.equals("what is isro?");

        if (isDefinitional && !analysis.targetEntities.isEmpty()) {
            analysis.definitionalQuery = true;
            analysis.definitionalTarget = analysis.targetEntities.iterator().next();
        }

        // Key relationship/attribute terms
        List<String> relationships = List.of(
            "propulsion", "engine", "thruster", "propellant", "thrust",
            "payload", "mass", "weight", "orbit", "trajectory", "telemetry",
            "power", "battery", "solar", "thermal", "frequency", "landing", "fuel"
        );

        for (String rel : relationships) {
            if (lowerQuery.contains(rel)) {
                analysis.relationshipTerms.add(rel);
            }
        }

        return analysis;
    }

    private float computeSubjectMatchFactor(RetrievalResultDTO result, QuerySubjectAnalysis analysis) {
        String content = (result.getContent() != null ? result.getContent() : "").toLowerCase();
        String title = (result.getSectionTitle() != null ? result.getSectionTitle() : "").toLowerCase();
        String combined = content + " " + title;

        // If the query asks for a general definition or overview of an organization (e.g. "What is ISRO?"):
        if (analysis.isDefinitionalQuery() && analysis.isAsksAboutOrganization()) {
            boolean definesTarget = isDefinitionalContentFor(combined, analysis.getDefinitionalTarget());
            if (definesTarget) {
                return 1.0f; // Directly defines or provides an overview of the organization
            }

            // If the chunk merely describes a specific rocket engine or launch vehicle without defining the organization:
            boolean describesHardwareOrStage = combined.contains("ce-20") || combined.contains("cryogenic")
                    || combined.contains("launch vehicle") || combined.contains("upper stage")
                    || combined.contains("engine") || combined.contains("c25") || combined.contains("cus");
            if (describesHardwareOrStage) {
                return 0.10f; // Heavy penalty for using engine specs to answer an organization definition query
            }

            return 0.25f;
        }

        // If the query specifically asks about a spacecraft (e.g. Chandrayaan-3):
        if (analysis.isAsksAboutSpacecraft()) {
            boolean mentionsTarget = analysis.getTargetEntities().stream().anyMatch(combined::contains);
            if (mentionsTarget) {
                return 1.0f; // Direct target subject match
            }

            // If it does NOT mention the spacecraft, but explicitly describes a launch vehicle / rocket engine:
            // This is an entity mismatch (launch-vehicle evidence for spacecraft query)
            boolean describesLaunchVehicle = combined.contains("lvm3") || combined.contains("ce-20")
                    || combined.contains("cryogenic") || combined.contains("launch vehicle")
                    || combined.contains("upper stage") || combined.contains("c25");
            if (describesLaunchVehicle) {
                return 0.15f; // Heavy penalty for entity category mismatch
            }

            return 0.35f;
        }

        // If query specifically asks about a launch vehicle (e.g. LVM3, CE-20):
        if (analysis.isAsksAboutLaunchVehicle()) {
            boolean mentionsTarget = analysis.getTargetEntities().stream().anyMatch(combined::contains);
            if (mentionsTarget) {
                return 1.0f;
            }
            return 0.30f;
        }

        // General entity matching
        if (!analysis.getTargetEntities().isEmpty()) {
            boolean anyMatch = analysis.getTargetEntities().stream().anyMatch(combined::contains);
            return anyMatch ? 1.0f : 0.40f;
        }

        return 1.0f;
    }

    public boolean isDefinitionalContentFor(String combined, String target) {
        if (combined == null || target == null || target.isEmpty()) return false;
        String t = target.toLowerCase();
        return combined.contains(t + " is the")
                || combined.contains(t + " is an")
                || combined.contains(t + " is a")
                || combined.contains(t + " was established")
                || combined.contains(t + " was founded")
                || combined.contains(t + " stands for")
                || combined.contains("indian space research organisation")
                || combined.contains("national space agency")
                || combined.contains("space agency of india")
                || combined.contains("department of space")
                || combined.contains("primary mandate")
                || combined.contains("headquartered in")
                || combined.contains("about " + t)
                || combined.contains("overview of " + t);
    }

    private float computeRelationshipMatchFactor(RetrievalResultDTO result, QuerySubjectAnalysis analysis) {
        if (analysis.getRelationshipTerms().isEmpty()) {
            return 1.0f;
        }

        String content = (result.getContent() != null ? result.getContent() : "").toLowerCase();
        String title = (result.getSectionTitle() != null ? result.getSectionTitle() : "").toLowerCase();
        String combined = content + " " + title;

        long matches = analysis.getRelationshipTerms().stream()
                .filter(combined::contains)
                .count();

        if (matches >= 2) {
            return 1.0f;
        } else if (matches == 1) {
            return 0.80f;
        } else {
            return 0.30f;
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
        if (chunk.getDocumentRevision() != null) {
            dto.setRevisionId(chunk.getDocumentRevision().getId());
            try {
                if (chunk.getDocumentRevision().getDocument() != null) {
                    Document doc = chunk.getDocumentRevision().getDocument();
                    dto.setDocumentId(doc.getId());
                    dto.setDocumentKey(doc.getDocumentKey());
                    dto.setDocumentTitle(doc.getTitle());
                    dto.setSourceUrl(doc.getSourceUrl());
                }
            } catch (Exception e) {
                log.debug("Could not resolve document details for chunk {}: {}", chunk.getId(), e.getMessage());
            }
        }
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
