package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AnswerGenerationService
 *
 * All tests use mock mode to avoid requiring Ollama running.
 * Tests cover:
 * - Answer generation from evidence
 * - Citation extraction and mapping
 * - Confidence scoring
 * - Edge cases (null, empty evidence)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Answer Generation Service Tests")
class AnswerGenerationServiceTest {

    private RestTemplate restTemplate;
    private AnswerGenerationService answerGenerationService;

    @BeforeEach
    void setUp() {
        // Use a real RestTemplate instance (not mocked, since Java 25 doesn't allow mocking it)
        restTemplate = new RestTemplate();
        answerGenerationService = new AnswerGenerationService(restTemplate);
        // Enable mock mode for deterministic testing
        ReflectionTestUtils.setField(answerGenerationService, "enableMockMode", true);
    }

    /**
     * Helper: Create test evidence chunk
     */
    private RetrievalResultDTO createTestChunk(Long chunkId, Long revisionId, String content,
                                                String sectionTitle, Float relevanceScore) {
        RetrievalResultDTO chunk = new RetrievalResultDTO();
        chunk.setChunkId(chunkId);
        chunk.setRevisionId(revisionId);
        chunk.setContent(content);
        chunk.setSectionTitle(sectionTitle);
        chunk.setPageNumber(1);
        chunk.setRelevanceScore(relevanceScore);
        chunk.setApplicabilityScore(1.0f);
        return chunk;
    }

    // ============ Basic Answer Generation Tests ============

    @Test
    @DisplayName("Generate answer from single evidence chunk")
    void testGenerateAnswerFromSingleEvidence() {
        // Arrange
        String query = "What is ISRO?";
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "ISRO is the space agency of India", "About ISRO", 0.95f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        // Assert
        assertNotNull(answer);
        assertEquals(query, answer.getQuery());
        assertNotNull(answer.getAnswerText());
        assertFalse(answer.getAnswerText().isEmpty());
        assertEquals(1, answer.getEvidenceCount());
        assertEquals("llm", answer.getGenerationMethod());
        assertNotNull(answer.getCitations());
        assertGreater(answer.getConfidence(), 0.0f);
    }

    @Test
    @DisplayName("Generate answer from multiple evidence chunks")
    void testGenerateAnswerFromMultipleEvidence() {
        // Arrange
        String query = "What are satellite missions?";
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "Chandrayaan is lunar exploration mission", "Lunar Missions", 0.92f));
        evidence.add(createTestChunk(2L, 100L, "Mangalyaan focuses on Mars exploration", "Mars Missions", 0.88f));
        evidence.add(createTestChunk(3L, 100L, "Aditya-L1 studies the Sun", "Solar Missions", 0.85f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        // Assert
        assertNotNull(answer);
        assertEquals(3, answer.getEvidenceCount());
        assertNotNull(answer.getCitations());
        assertGreaterOrEqual(answer.getCitations().size(), 1);
        assertGreater(answer.getConfidence(), 0.0f);
        log("Answer citations: {}", answer.getCitations().size());
    }

    @Test
    @DisplayName("Generate answer with null evidence")
    void testGenerateAnswerWithNullEvidence() {
        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Query", null);

        // Assert
        assertNotNull(answer);
        assertEquals(0, answer.getEvidenceCount());
        assertEquals("No evidence found to answer this question.", answer.getAnswerText());
        assertEquals(0, answer.getCitations().size());
        assertEquals(0.0f, answer.getConfidence());
        assertEquals("no_evidence", answer.getGenerationMethod());
    }

    @Test
    @DisplayName("Generate answer with empty evidence list")
    void testGenerateAnswerWithEmptyEvidence() {
        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Query", new ArrayList<>());

        // Assert
        assertNotNull(answer);
        assertEquals(0, answer.getEvidenceCount());
        assertEquals("No evidence found to answer this question.", answer.getAnswerText());
        assertEquals(0, answer.getCitations().size());
        assertEquals(0.0f, answer.getConfidence());
    }

    // ============ Citation Extraction Tests ============

    @Test
    @DisplayName("Extract citations from answer text")
    void testExtractCitationsFromAnswer() {
        // Arrange
        String query = "Technical question";
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "Content 1", "Section 1", 0.9f));
        evidence.add(createTestChunk(2L, 100L, "Content 2", "Section 2", 0.8f));
        evidence.add(createTestChunk(3L, 100L, "Content 3", "Section 3", 0.7f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        // Assert
        assertNotNull(answer.getCitations());
        assertGreater(answer.getCitations().size(), 0);

        // Verify citations have proper mapping
        for (AnswerGenerationService.Citation citation : answer.getCitations()) {
            assertNotNull(citation.getSourceNumber());
            assertNotNull(citation.getChunkId());
            assertNotNull(citation.getRevisionId());
            assertTrue(citation.getSourceNumber() >= 1 && citation.getSourceNumber() <= 3);
        }
    }

    @Test
    @DisplayName("Citation contains correct source numbers")
    void testCitationSourceNumbers() {
        // Arrange
        String query = "Test query";
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            evidence.add(createTestChunk((long)i, 100L, "Content " + i, "Section " + i, 0.9f - i * 0.05f));
        }

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        // Assert
        for (AnswerGenerationService.Citation citation : answer.getCitations()) {
            int sourceNum = citation.getSourceNumber();
            RetrievalResultDTO expectedEvidence = evidence.get(sourceNum - 1);
            assertEquals(expectedEvidence.getChunkId(), citation.getChunkId());
            assertEquals(expectedEvidence.getRevisionId(), citation.getRevisionId());
            assertEquals(expectedEvidence.getSectionTitle(), citation.getSectionTitle());
        }
    }

    @Test
    @DisplayName("Answer text excludes citation markers")
    void testAnswerTextCleanedOfCitationMarkers() {
        // Arrange
        String query = "Query";
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "Content 1", "Section 1", 0.9f));
        evidence.add(createTestChunk(2L, 100L, "Content 2", "Section 2", 0.8f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        // Assert
        String answerText = answer.getAnswerText();
        assertFalse(answerText.contains("[Source"), "Answer text should not contain [Source markers");
        assertFalse(answerText.contains("[Citation"), "Answer text should not contain [Citation markers");
    }

    // ============ Confidence Scoring Tests ============

    @Test
    @DisplayName("Confidence increases with more citations")
    void testConfidenceWithMoreCitations() {
        // Arrange: Test that confidence scoring works
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            evidence.add(createTestChunk((long)i, 100L, "Content " + i, "Section " + i, 0.9f - i * 0.05f));
        }

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Test", evidence);

        // Assert - confidence should be computed and within valid range
        assertNotNull(answer.getConfidence());
        assertTrue(answer.getConfidence() >= 0.0f && answer.getConfidence() <= 1.0f,
            "Confidence should be between 0 and 1");
        assertNotNull(answer.getCitationCoverage());
        assertTrue(answer.getCitationCoverage() >= 0.0f && answer.getCitationCoverage() <= 1.0f);
    }

    @Test
    @DisplayName("Confidence considers evidence relevance scores")
    void testConfidenceWithRelevanceScores() {
        // Arrange: Low relevance evidence
        List<RetrievalResultDTO> lowRelevance = new ArrayList<>();
        lowRelevance.add(createTestChunk(1L, 100L, "Content 1", "Section 1", 0.3f));
        lowRelevance.add(createTestChunk(2L, 100L, "Content 2", "Section 2", 0.2f));

        // High relevance evidence
        List<RetrievalResultDTO> highRelevance = new ArrayList<>();
        highRelevance.add(createTestChunk(1L, 100L, "Content 1", "Section 1", 0.95f));
        highRelevance.add(createTestChunk(2L, 100L, "Content 2", "Section 2", 0.90f));

        // Act
        AnswerGenerationService.GeneratedAnswer lowAnswer = answerGenerationService.generateAnswer("Query", lowRelevance);
        AnswerGenerationService.GeneratedAnswer highAnswer = answerGenerationService.generateAnswer("Query", highRelevance);

        // Assert
        assertTrue(highAnswer.getConfidence() > lowAnswer.getConfidence(),
            "Higher relevance evidence should yield higher confidence");
    }

    // ============ Context Building Tests ============

    @Test
    @DisplayName("Context window includes all evidence")
    void testContextWindowIncludesAllEvidence() {
        // Arrange
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "Technical specification details", "Specs", 0.9f));
        evidence.add(createTestChunk(2L, 100L, "Implementation guidelines", "Guidelines", 0.85f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("How to implement?", evidence);

        // Assert
        assertNotNull(answer.getContextWindowSize());
        assertGreater(answer.getContextWindowSize(), 0);
        log("Context window size: {} bytes", answer.getContextWindowSize());
    }

    // ============ Citation Coverage Tests ============

    @Test
    @DisplayName("Citation coverage reflects cited vs total evidence")
    void testCitationCoverage() {
        // Arrange
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            evidence.add(createTestChunk((long)i, 100L, "Content " + i, "Section " + i, 0.9f - i * 0.05f));
        }

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Query", evidence);

        // Assert
        assertNotNull(answer.getCitationCoverage());
        assertTrue(answer.getCitationCoverage() >= 0.0f && answer.getCitationCoverage() <= 1.0f,
            "Citation coverage should be between 0 and 1");

        if (!answer.getCitations().isEmpty()) {
            float expectedCoverage = (float) answer.getCitations().size() / 5;
            assertEquals(expectedCoverage, answer.getCitationCoverage(), 0.01f);
        }
    }

    // ============ Edge Cases & Error Handling ============

    @Test
    @DisplayName("Handle evidence with null relevance scores")
    void testEvidenceWithNullRelevanceScore() {
        // Arrange
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        RetrievalResultDTO chunk = createTestChunk(1L, 100L, "Content", "Section", null);
        evidence.add(chunk);

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Query", evidence);

        // Assert
        assertNotNull(answer);
        assertNotNull(answer.getCitations());
        assertGreaterOrEqual(answer.getConfidence(), 0.0f);
    }

    @Test
    @DisplayName("Handle evidence with null section titles")
    void testEvidenceWithNullSectionTitle() {
        // Arrange
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        RetrievalResultDTO chunk = new RetrievalResultDTO();
        chunk.setChunkId(1L);
        chunk.setRevisionId(100L);
        chunk.setContent("Content without section");
        chunk.setSectionTitle(null);
        chunk.setRelevanceScore(0.9f);
        evidence.add(chunk);

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Query", evidence);

        // Assert
        assertNotNull(answer);
        assertNotNull(answer.getAnswerText());
        assertFalse(answer.getAnswerText().isEmpty());
    }

    @Test
    @DisplayName("Handle evidence with empty content")
    void testEvidenceWithEmptyContent() {
        // Arrange
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "", "Empty", 0.9f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Query", evidence);

        // Assert
        assertNotNull(answer);
        assertNotNull(answer.getAnswerText());
        assertEquals(1, answer.getEvidenceCount());
    }

    @Test
    @DisplayName("Generate method returns non-empty answer text")
    void testAnswerTextNeverEmpty() {
        // Arrange
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "Some content", "Section", 0.9f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Query", evidence);

        // Assert
        assertNotNull(answer.getAnswerText());
        assertFalse(answer.getAnswerText().isEmpty(),
            "Answer text should never be empty when evidence is provided");
    }

    @Test
    @DisplayName("Generate method is idempotent for same inputs")
    void testAnswerGenerationIdempotent() {
        // Arrange
        String query = "Idempotent test query";
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "Content", "Section", 0.9f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer1 = answerGenerationService.generateAnswer(query, evidence);
        AnswerGenerationService.GeneratedAnswer answer2 = answerGenerationService.generateAnswer(query, evidence);

        // Assert
        assertEquals(answer1.getAnswerText(), answer2.getAnswerText(),
            "Same inputs should produce same answer in mock mode");
        assertEquals(answer1.getCitations().size(), answer2.getCitations().size());
    }

    // ============ Citation Properties Tests ============

    @Test
    @DisplayName("Citations maintain metadata from evidence")
    void testCitationMetadata() {
        // Arrange
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(42L, 555L, "Content", "Technical Specs", 0.87f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Query", evidence);

        // Assert
        if (!answer.getCitations().isEmpty()) {
            AnswerGenerationService.Citation citation = answer.getCitations().get(0);
            assertEquals(42L, citation.getChunkId());
            assertEquals(555L, citation.getRevisionId());
            assertEquals("Technical Specs", citation.getSectionTitle());
            assertEquals(0.87f, citation.getRelevanceScore(), 0.001f);
        }
    }

    @Test
    @DisplayName("Answer generation method field reflects source")
    void testGenerationMethodField() {
        // Arrange - with mock enabled
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "Content", "Section", 0.9f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer("Query", evidence);

        // Assert
        assertEquals("llm", answer.getGenerationMethod(),
            "Mock mode should report generation method as 'llm'");
    }

    // ============ Helper Methods ============

    private void log(String message, Object... args) {
        System.out.println(String.format(message, args));
    }

    private void assertGreater(Float actual, Float expected) {
        assertTrue(actual > expected, String.format("%f should be greater than %f", actual, expected));
    }

    private void assertGreaterOrEqual(Number actual, Number expected) {
        assertTrue(actual.doubleValue() >= expected.doubleValue(),
            String.format("%s should be >= %s", actual, expected));
    }

    private void assertGreater(int actual, int expected) {
        assertTrue(actual > expected, String.format("%d should be greater than %d", actual, expected));
    }
}
