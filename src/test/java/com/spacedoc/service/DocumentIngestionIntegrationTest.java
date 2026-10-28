package com.spacedoc.service;

import com.pgvector.PGvector;
import com.spacedoc.dto.RetrievalResultDTO;
import com.spacedoc.entity.Document;
import com.spacedoc.entity.DocumentChunk;
import com.spacedoc.entity.DocumentRevision;
import com.spacedoc.repository.DocumentChunkRepository;
import com.spacedoc.repository.DocumentRepository;
import com.spacedoc.repository.DocumentRevisionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/spacedoc",
    "spring.datasource.username=spacedoc",
    "spring.datasource.password=spacedoc_local_dev_password",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@Tag("integration")
@DisplayName("Live RAG Pipeline Integration Tests (PostgreSQL + pgvector + Ollama)")
public class DocumentIngestionIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentRevisionRepository revisionRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private RetrievalService retrievalService;

    private Long testDocumentId;
    private Long testRevisionId;
    private Long testChunkId;

    @BeforeEach
    void setUp() {
        Document doc = new Document();
        doc.setDocumentKey("isro-chandrayaan-3-spec-live");
        doc.setTitle("ISRO Chandrayaan-3 Technical Specification & Lunar Orbit Insertion");
        doc.setSourceUrl("https://www.isro.gov.in/Chandrayaan3_Spec.pdf");
        doc.setSourceType("PDF");
        doc.setSourceOrganization("ISRO");
        doc.setPublicationDate(LocalDateTime.now());
        doc = documentRepository.save(doc);
        testDocumentId = doc.getId();

        DocumentRevision rev = new DocumentRevision();
        rev.setDocument(doc);
        rev.setRevisionNumber(1);
        rev.setRevisionKey("isro-chandrayaan-3-spec-live-v1");
        rev.setContentHash("abc123hashlive");
        rev.setChangeSummary("Initial live test revision");
        rev.setIsCurrent(true);
        rev = revisionRepository.save(rev);
        testRevisionId = rev.getId();

        String sampleContent = "ISRO Chandrayaan-3 propulsion system utilizes liquid apogee motor (LAM) and reaction control system (RCS) thrusters for lunar orbit insertion burn parameters and trajectory correction maneuvers.";

        String vectorEmbedding = null;
        try {
            vectorEmbedding = embeddingService.embed(sampleContent);
        } catch (Exception e) {
            System.err.println("Ollama embedding generation warning in test: " + e.getMessage());
        }

        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentRevision(rev);
        chunk.setChunkNumber(1);
        chunk.setContent(sampleContent);
        chunk.setSectionTitle("Propulsion and Orbit Insertion");
        chunk.setPageNumber(12);
        try {
            chunk.setEmbedding(vectorEmbedding != null ? new PGvector(vectorEmbedding) : null);
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
        chunk = chunkRepository.save(chunk);
        testChunkId = chunk.getId();
    }

    @AfterEach
    void tearDown() {
        try {
            if (testDocumentId != null) {
                documentRepository.deleteById(testDocumentId);
            }
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("1. Test Live Embedding Generation")
    void testLiveEmbeddingGeneration() {
        String text = "ISRO lunar orbital insertion burn parameters";
        String embedding = embeddingService.embed(text);
        assertNotNull(embedding, "Embedding should not be null");
        assertTrue(embedding.startsWith("[") && embedding.endsWith("]"), "Embedding should be a JSON array string");
        assertTrue(embedding.length() > 100, "Embedding should contain vector float values");
        System.out.println("=== TEST EVIDENCE: Live Embedding Generated Successfully ===");
        System.out.println("Embedding sample: " + embedding.substring(0, Math.min(100, embedding.length())) + "...");
    }

    @Test
    @DisplayName("2. Test Live Lexical Retrieval")
    void testLiveLexicalRetrieval() {
        List<RetrievalResultDTO> results = retrievalService.lexicalRetrieval("propulsion system", 10);
        assertNotNull(results, "Results should not be null");
        assertFalse(results.isEmpty(), "Lexical retrieval should find the ingested document chunk");

        boolean found = results.stream().anyMatch(r -> r.getChunkId().equals(testChunkId));
        assertTrue(found, "Retrieved results should contain the test chunk ID");

        System.out.println("=== TEST EVIDENCE: Lexical Retrieval Successful ===");
        results.forEach(r -> System.out.println(" - [Lexical] Chunk ID: " + r.getChunkId() + ", Score: " + r.getRelevanceScore() + ", Content: " + r.getContent()));
    }

    @Test
    @DisplayName("3. Test Live Dense Vector Retrieval")
    void testLiveDenseRetrieval() {
        String queryVector = embeddingService.embed("Chandrayaan-3 lunar orbit insertion");
        List<RetrievalResultDTO> results = retrievalService.denseRetrieval(queryVector, 10);

        assertNotNull(results, "Dense results should not be null");
        assertFalse(results.isEmpty(), "Dense retrieval should find the ingested chunk via pgvector <-> distance");

        boolean found = results.stream().anyMatch(r -> r.getChunkId().equals(testChunkId));
        assertTrue(found, "Dense retrieved results should contain the test chunk ID");

        System.out.println("=== TEST EVIDENCE: Dense Vector Retrieval Successful (pgvector) ===");
        results.forEach(r -> System.out.println(" - [Dense] Chunk ID: " + r.getChunkId() + ", Score: " + r.getRelevanceScore() + ", Content: " + r.getContent()));
    }

    @Test
    @DisplayName("4. Test Live Hybrid Retrieval with RRF")
    void testLiveHybridRetrieval() {
        List<RetrievalResultDTO> results = retrievalService.hybridRetrieval("ISRO lunar orbit insertion", 10);

        assertNotNull(results, "Hybrid results should not be null");
        assertFalse(results.isEmpty(), "Hybrid retrieval should return combined results");

        boolean found = results.stream().anyMatch(r -> r.getChunkId().equals(testChunkId));
        assertTrue(found, "Hybrid retrieved results should contain the test chunk ID");

        System.out.println("=== TEST EVIDENCE: Hybrid Retrieval & RRF Successful ===");
        results.forEach(r -> System.out.println(" - [Hybrid RRF] Chunk ID: " + r.getChunkId() + ", RRF Score: " + r.getRelevanceScore() + ", Content: " + r.getContent()));
    }
}
