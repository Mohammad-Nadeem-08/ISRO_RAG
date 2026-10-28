package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentChunk;
import com.spacedoc.entity.DocumentRevision;
import com.spacedoc.repository.DocumentChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("RetrievalService Tests")
public class RetrievalServiceTest {

    private RetrievalService retrievalService;

    @Mock
    private DocumentChunkRepository chunkRepository;

    @Mock
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        retrievalService = new RetrievalService(chunkRepository, embeddingService);
    }

    // ==================== LEXICAL RETRIEVAL TESTS ====================

    @Test
    @DisplayName("Lexical retrieval returns empty list for null query")
    void testLexicalRetrievalWithNullQuery() {
        List<RetrievalResultDTO> results = retrievalService.lexicalRetrieval(null, 10);
        assertTrue(results.isEmpty(), "Null query should return empty list");
    }

    @Test
    @DisplayName("Lexical retrieval returns empty list for empty query")
    void testLexicalRetrievalWithEmptyQuery() {
        List<RetrievalResultDTO> results = retrievalService.lexicalRetrieval("", 10);
        assertTrue(results.isEmpty(), "Empty query should return empty list");
    }

    @Test
    @DisplayName("Lexical retrieval returns results for valid query")
    void testLexicalRetrievalWithValidQuery() {
        // Setup
        String query = "ISRO technical specification";
        DocumentChunk chunk1 = createTestChunk(1L, "ISRO satellite technical specifications and requirements", 1L);
        DocumentChunk chunk2 = createTestChunk(2L, "ISRO mission control procedures", 1L);
        List<DocumentChunk> mockChunks = List.of(chunk1, chunk2);

        when(chunkRepository.searchByFullText(anyString(), any(Pageable.class)))
            .thenReturn(mockChunks);

        // Execute
        List<RetrievalResultDTO> results = retrievalService.lexicalRetrieval(query, 10);

        // Verify
        assertEquals(2, results.size(), "Should return 2 results");
        assertTrue(results.stream().allMatch(r -> r.getChunkId() != null), "All results should have chunk IDs");
    }

    // ==================== DENSE RETRIEVAL TESTS ====================

    @Test
    @DisplayName("Dense retrieval returns empty list for null vector")
    void testDenseRetrievalWithNullVector() {
        List<RetrievalResultDTO> results = retrievalService.denseRetrieval(null, 10);
        assertTrue(results.isEmpty(), "Null vector should return empty list");
    }

    @Test
    @DisplayName("Dense retrieval returns empty list for empty vector")
    void testDenseRetrievalWithEmptyVector() {
        List<RetrievalResultDTO> results = retrievalService.denseRetrieval("", 10);
        assertTrue(results.isEmpty(), "Empty vector should return empty list");
    }

    @Test
    @DisplayName("Dense retrieval respects topK limit")
    void testDenseRetrievalRespectTopK() {
        // Setup: Create more results than topK
        String vector = "[0.1, 0.2, 0.3]";
        List<DocumentChunk> allChunks = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            allChunks.add(createTestChunk((long)i, "Dense content " + i, 1L));
        }

        when(chunkRepository.findByEmbeddingSimilarity(anyString(), any(Pageable.class)))
            .thenReturn(allChunks.subList(0, 10));

        // Execute
        List<RetrievalResultDTO> results = retrievalService.denseRetrieval(vector, 10);

        // Verify
        assertTrue(results.size() <= 10, "Results should not exceed topK");
    }

    // ==================== HYBRID RETRIEVAL TESTS ====================

    @Test
    @DisplayName("Hybrid retrieval returns empty list for null query")
    void testHybridRetrievalWithNullQuery() {
        List<RetrievalResultDTO> results = retrievalService.hybridRetrieval(null, 10);
        assertTrue(results.isEmpty(), "Null query should return empty list");
    }

    @Test
    @DisplayName("Hybrid retrieval combines lexical and dense results")
    void testHybridRetrievalCombinesBothSources() {
        // Setup
        String query = "technical specifications";
        DocumentChunk chunk1 = createTestChunk(1L, "Technical specifications document", 1L);
        DocumentChunk chunk2 = createTestChunk(2L, "System architecture specifications", 1L);

        when(chunkRepository.searchByFullText(anyString(), any(Pageable.class)))
            .thenReturn(List.of(chunk1, chunk2));

        // Execute
        List<RetrievalResultDTO> results = retrievalService.hybridRetrieval(query, 10);

        // Verify: Results exist and are properly ranked
        assertEquals(2, results.size(), "Should return 2 results from lexical search");
        assertTrue(results.stream().allMatch(r -> r.getChunkId() != null), "All results should have chunk IDs");
        // Scores may be null/zero in RRF if no dense results, but at least lexical worked
        assertTrue(results.stream().anyMatch(r -> r.getContent() != null), "Results should have content");
    }

    @Test
    @DisplayName("Hybrid retrieval handles no lexical results gracefully")
    void testHybridRetrievalHandlesEmptyLexical() {
        // Setup
        when(chunkRepository.searchByFullText(anyString(), any(Pageable.class)))
            .thenReturn(new ArrayList<>());

        // Execute (should not throw)
        List<RetrievalResultDTO> results = retrievalService.hybridRetrieval("obscure query", 10);

        // Verify
        assertNotNull(results, "Should return non-null list");
    }

    // ==================== RECIPROCAL RANK FUSION TESTS ====================

    @Test
    @DisplayName("RRF correctly scores single-source results")
    void testRRFSingleSource() {
        List<RetrievalResultDTO> dense = List.of(
            createDTO(1L, "Result 1"),
            createDTO(2L, "Result 2")
        );
        List<RetrievalResultDTO> lexical = new ArrayList<>();

        List<RetrievalResultDTO> results = retrievalService.reciprocalRankFusion(dense, lexical, 10);

        assertEquals(2, results.size(), "Should return all single-source results");
        assertTrue(results.get(0).getRelevanceScore() >= results.get(1).getRelevanceScore(),
            "Results should be sorted by score descending");
    }

    @Test
    @DisplayName("RRF combines duplicate chunks from both sources")
    void testRRFCombinesDuplicateChunks() {
        RetrievalResultDTO chunk1Dense = createDTO(1L, "Result 1");
        RetrievalResultDTO chunk1Lexical = createDTO(1L, "Result 1");
        RetrievalResultDTO chunk2Lexical = createDTO(2L, "Result 2");

        List<RetrievalResultDTO> dense = List.of(chunk1Dense);
        List<RetrievalResultDTO> lexical = List.of(chunk1Lexical, chunk2Lexical);

        List<RetrievalResultDTO> results = retrievalService.reciprocalRankFusion(dense, lexical, 10);

        assertEquals(2, results.size(), "Should deduplicate and return 2 unique chunks");
        // First result should be chunk 1 (appeared in both lists)
        assertEquals(1L, results.get(0).getChunkId(), "Chunk 1 should rank highest (appeared twice)");
    }

    @Test
    @DisplayName("RRF respects topK limit")
    void testRRFRespectsTopK() {
        List<RetrievalResultDTO> dense = createResultList(1, 15);
        List<RetrievalResultDTO> lexical = createResultList(16, 30);

        List<RetrievalResultDTO> results = retrievalService.reciprocalRankFusion(dense, lexical, 5);

        assertEquals(5, results.size(), "RRF should respect topK=5");
    }

    // ==================== DEDUPLICATION TESTS ====================

    @Test
    @DisplayName("Deduplication removes chunks with identical content prefix")
    void testDeduplicationRemovesDuplicates() {
        List<RetrievalResultDTO> results = List.of(
            createDTO(1L, "The ISRO satellite mission is critical"),
            createDTO(2L, "The ISRO satellite mission is critical"), // Same prefix hash
            createDTO(3L, "Different content entirely")
        );

        List<RetrievalResultDTO> deduped = retrievalService.deduplicateResults(results);

        assertEquals(2, deduped.size(), "Should remove 1 duplicate");
    }

    @Test
    @DisplayName("Deduplication handles empty content")
    void testDeduplicationHandlesEmptyContent() {
        List<RetrievalResultDTO> results = List.of(
            createDTO(1L, ""),
            createDTO(2L, "Valid content")
        );

        // Should not throw
        List<RetrievalResultDTO> deduped = retrievalService.deduplicateResults(results);

        assertEquals(2, deduped.size(), "Should handle empty content gracefully");
    }

    // ==================== SUBJECT & RELATIONSHIP RERANKING REGRESSION TESTS ====================

    @Test
    @DisplayName("Regression: Reranking prioritizes direct spacecraft subject match over launch vehicle evidence")
    void testRerankingPrefersSpacecraftSubjectOverLaunchVehicle() {
        String query = "What is Chandrayaan-3's propulsion system?";

        // Chunk 1: Mentions CE-20 engine for LVM3 launch vehicle upper stage (no Chandrayaan-3 mention)
        RetrievalResultDTO lvChunk = createDTO(1L,
            "The CE-20 is an indigenous cryogenic rocket engine developed for the LVM3 launch vehicle upper stage producing 200 kN nominal thrust.");
        lvChunk.setSectionTitle("CE-20 Cryogenic Engine");
        lvChunk.setRelevanceScore(0.016f); // Raw RRF rank 1

        // Chunk 2: Mentions Chandrayaan-3 spacecraft launch by LVM3
        RetrievalResultDTO launchChunk = createDTO(2L,
            "LVM3 successfully launched Chandrayaan-3 into orbit. C25 stage powered by CE-20 injected the spacecraft.");
        launchChunk.setSectionTitle("Launch Mission History");
        launchChunk.setRelevanceScore(0.015f); // Raw RRF rank 2

        // Chunk 3: Directly describes Chandrayaan-3 propulsion module
        RetrievalResultDTO spacecraftPropulsionChunk = createDTO(3L,
            "Chandrayaan-3 propulsion module is equipped with a 440 N Liquid Apogee Motor (LAM) and 16 attitude control thrusters.");
        spacecraftPropulsionChunk.setSectionTitle("Spacecraft Propulsion System");
        spacecraftPropulsionChunk.setRelevanceScore(0.014f); // Raw RRF rank 3

        List<RetrievalResultDTO> inputList = List.of(lvChunk, launchChunk, spacecraftPropulsionChunk);

        List<RetrievalResultDTO> reranked = retrievalService.rerankBySubjectAndRelationship(inputList, query);

        // Verify that spacecraft propulsion chunk ranks #1 despite lower initial RRF score
        assertEquals(3L, reranked.get(0).getChunkId(),
            "Chunk describing Chandrayaan-3 propulsion module should rank highest");

        // Verify that chunk describing launch vehicle with 0 Chandrayaan-3 mentions is heavily penalized
        RetrievalResultDTO rerankedLv = reranked.stream().filter(r -> r.getChunkId() == 1L).findFirst().orElseThrow();
        RetrievalResultDTO rerankedProp = reranked.stream().filter(r -> r.getChunkId() == 3L).findFirst().orElseThrow();
        assertTrue(rerankedProp.getRelevanceScore() > rerankedLv.getRelevanceScore(),
            "Spacecraft propulsion evidence must have higher score than launch vehicle engine evidence");
    }

    @Test
    @DisplayName("Regression: Reranking penalizes entity category mismatch when subject entity is absent")
    void testRerankingPenalizesEntityMismatchWhenSubjectAbsent() {
        String query = "What is Chandrayaan-3's propulsion system?";

        RetrievalResultDTO mismatchChunk = createDTO(1L,
            "CE-20 cryogenic rocket engine powers the upper stage of the LVM3 launch vehicle.");
        mismatchChunk.setRelevanceScore(1.0f);

        List<RetrievalResultDTO> reranked = retrievalService.rerankBySubjectAndRelationship(List.of(mismatchChunk), query);

        // Score must be heavily penalized due to entity mismatch (query asks about spacecraft, chunk describes launch vehicle)
        assertTrue(reranked.get(0).getRelevanceScore() < 0.5f,
            "Entity mismatch chunk must be significantly penalized");
    }

    // ==================== HELPER METHODS ====================

    private DocumentChunk createTestChunk(Long id, String content, Long revisionId) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setContent(content);
        chunk.setChunkNumber(id.intValue());
        chunk.setPageNumber(1);
        chunk.setSectionTitle("Test Section");

        // Create minimal revision
        DocumentRevision revision = new DocumentRevision();
        revision.setId(revisionId);
        revision.setRevisionNumber(1);

        // Create minimal document
        Document document = new Document();
        document.setId(1L);
        document.setDocumentKey("test-doc");

        revision.setDocument(document);
        chunk.setDocumentRevision(revision);

        return chunk;
    }

    private RetrievalResultDTO createDTO(Long chunkId, String content) {
        RetrievalResultDTO dto = new RetrievalResultDTO();
        dto.setChunkId(chunkId);
        dto.setContent(content);
        dto.setRevisionId(1L);
        dto.setRelevanceScore(0.9f);
        return dto;
    }

    private List<RetrievalResultDTO> createResultList(int startId, int endId) {
        List<RetrievalResultDTO> list = new ArrayList<>();
        for (int i = startId; i <= endId; i++) {
            list.add(createDTO((long)i, "Result " + i));
        }
        return list;
    }
}
