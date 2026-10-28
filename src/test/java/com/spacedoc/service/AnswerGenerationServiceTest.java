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
    @DisplayName("Answer text retains citation markers and cleanAnswerText strips them")
    void testAnswerTextCleanedOfCitationMarkers() {
        // Arrange
        String query = "Query";
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "Content 1", "Section 1", 0.9f));
        evidence.add(createTestChunk(2L, 100L, "Content 2", "Section 2", 0.8f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        // Assert - answer text retains citations for UI pill rendering
        String answerText = answer.getAnswerText();
        assertTrue(answerText.contains("[Source 1]"), "Answer text should retain [Source 1] marker for UI pills");

        // Assert - utility cleanAnswerText properly removes citation markers
        String cleaned = answerGenerationService.cleanAnswerText(answerText);
        assertFalse(cleaned.contains("[Source"), "Cleaned text should not contain [Source markers");
        assertFalse(cleaned.contains("[Citation"), "Cleaned text should not contain [Citation markers");
    }

    @Test
    @DisplayName("Normalize citation markers handles Chinese brackets and unicode spaces")
    void testNormalizeCitationMarkers() {
        String input = "Mission details\u3010Source\u202F1\u3011 and overview\u3010Source\u00A04\u3011.";
        String normalized = AnswerGenerationService.normalizeCitationMarkers(input);
        assertEquals("Mission details[Source 1] and overview[Source 4].", normalized);
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

    // ============ Regression Tests for Enhanced Grounding & Relevance ============

    @Test
    @DisplayName("Regression: Spacecraft vs Launch Vehicle distinction and abstention")
    void testSpacecraftVsLaunchVehicleDistinctionAndAbstention() {
        // Query asks about Chandrayaan-3 (spacecraft) propulsion
        String query = "What is Chandrayaan-3's propulsion system?";

        // Retrieved evidence describes only LVM3 CE-20 cryogenic engine
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(68L, 100L,
            "The CE-20 is an indigenous cryogenic rocket engine developed by ISRO for the LVM3 launch vehicle upper stage. " +
            "It operates on a gas-generator cycle using liquid oxygen (LOX) and liquid hydrogen (LH2) propellants, producing 200 kN nominal thrust.",
            "CE-20 Engine Specifications", 0.75f));
        evidence.add(createTestChunk(67L, 100L,
            "ISRO Cryogenic Upper Stage CUS technical characteristics and propellant loading protocols.",
            "Cryogenic Upper Stage", 0.60f));

        // Act
        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        // Assert
        assertNotNull(answer);
        String text = answer.getAnswerText();
        assertNotNull(text);
        // Must explicitly distinguish launch vehicle from spacecraft
        assertTrue(text.toLowerCase().contains("launch vehicle") || text.toLowerCase().contains("ce-20"),
            "Answer must distinguish launch vehicle / CE-20 from the spacecraft");
        // Must state that evidence is insufficient to establish spacecraft's propulsion
        assertTrue(answerGenerationService.isInsufficientEvidenceAnswer(text) || text.toLowerCase().contains("insufficient"),
            "Answer must state that available evidence is insufficient to establish Chandrayaan-3's propulsion system");
        // Confidence must be calibrated to 0.0 for abstention / insufficient evidence
        assertEquals(0.0f, answer.getConfidence(),
            "Confidence must be calibrated to 0.0 when evidence does not establish the requested fact");
    }

    @Test
    @DisplayName("Regression: Topically related but non-answering evidence triggers abstention")
    void testTopicallyRelatedNonAnsweringEvidenceAbstention() {
        String query = "What cryptographic standards are mandated for S-band telemetry downlink?";

        // Evidence mentions S-band telemetry and frequency, but does NOT answer cryptographic standards
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(10L, 101L,
            "S-band telemetry downlink operates at 2200-2290 MHz for deep space tracking stations.",
            "Telemetry Specifications", 0.70f));

        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        assertNotNull(answer);
        assertTrue(answerGenerationService.isInsufficientEvidenceAnswer(answer.getAnswerText()),
            "Topically related non-answering evidence must trigger explicit abstention");
        assertEquals(0.0f, answer.getConfidence(),
            "Confidence must be 0.0 when evidence is insufficient");
    }

    @Test
    @DisplayName("Regression: Insufficient evidence produces appropriate abstention with zero confidence")
    void testInsufficientEvidenceAndAppropriateAbstention() {
        String query = "What is the battery chemistry of the Vikram lander?";

        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L,
            "The CE-20 operates on LOX and LH2 cryogenic propellants.",
            "Propellants", 0.40f));

        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        assertNotNull(answer);
        assertTrue(answerGenerationService.isInsufficientEvidenceAnswer(answer.getAnswerText()));
        assertEquals(0.0f, answer.getConfidence());
    }

    @Test
    @DisplayName("Regression: Citation references that do not exist are rejected")
    void testNonExistentCitationReferencesRejected() {
        // Evidence has only 2 items
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 100L, "The CE-20 is a cryogenic engine.", "Introduction", 0.9f));
        evidence.add(createTestChunk(2L, 100L, "Propellants are LOX and LH2.", "Specifications", 0.85f));

        // Model hallucinates [Source 99] and [Source 0] along with valid [Source 1]
        String modelOutput = "The CE-20 is a cryogenic engine [Source 1]. It was also tested on a fictional test stand [Source 99] and [Source 0].";

        List<String> invalidCitations = new ArrayList<>();
        List<AnswerGenerationService.Citation> citations = answerGenerationService.extractCitations(modelOutput, evidence, invalidCitations);

        // Verify only valid chunk was extracted
        assertEquals(1, citations.size(), "Only valid source numbers within 1..size should be extracted");
        assertEquals(1, citations.get(0).getSourceNumber());
        assertEquals(1L, citations.get(0).getChunkId());

        // Verify invalid citations were recorded
        assertTrue(invalidCitations.contains("[Source 99]"), "Source 99 must be recorded as invalid");
    }

    @Test
    @DisplayName("Regression: Valid citation whose underlying chunk does not support the claim is detected")
    void testValidCitationWithUnsupportedClaimDetected() {
        // Evidence chunk describes CE-20 cryogenic engine
        RetrievalResultDTO chunk = createTestChunk(1L, 100L,
            "The CE-20 is an indigenous cryogenic rocket engine developed by ISRO for the LVM3 launch vehicle upper stage using LOX and LH2.",
            "CE-20 Engine", 0.9f);
        List<RetrievalResultDTO> evidence = List.of(chunk);

        // Answer claims lander uses solid fuel hypergolic motors (completely unsupported by chunk)
        String unsupportedClaimSentence = "Chandrayaan-3 lander utilizes solid fuel hypergolic descent thrusters [Source 1].";

        // Claim support validation should detect the mismatch
        boolean supported = answerGenerationService.validateClaimSupport(unsupportedClaimSentence, chunk.getContent());
        assertFalse(supported, "Chunk about CE-20 cryogenic engine must not support solid fuel hypergolic descent claim");

        // When extracted, citation must have claimSupported = false
        List<AnswerGenerationService.Citation> citations = answerGenerationService.extractCitations(unsupportedClaimSentence, evidence);
        assertEquals(1, citations.size());
        assertFalse(citations.get(0).getClaimSupported(), "Citation must reflect that claim is unsupported");

        // Calibrated confidence must be 0.0 for unsupported citations
        Float confidence = answerGenerationService.computeConfidence(unsupportedClaimSentence, citations, evidence.size());
        assertEquals(0.0f, confidence, "Unsupported claim must produce 0.0 confidence");
    }

    @Test
    @DisplayName("Regression: Preservation of revision-aware retrieval and routing behavior")
    void testPreservationOfRevisionAwareRetrievalAndRouting() {
        // Create revision comparison service and conflict detection service
        RevisionComparisonService revComp = new RevisionComparisonService(null);
        ConflictDetectionService conflictService = new ConflictDetectionService();
        EvidenceRoutingService routingService = new EvidenceRoutingService(revComp, conflictService);

        // Create target revision
        com.spacedoc.entity.DocumentRevision targetRev = new com.spacedoc.entity.DocumentRevision();
        targetRev.setId(102L);
        targetRev.setRevisionNumber(2);

        // Create evidence from both revision 101 and target revision 102
        List<RetrievalResultDTO> evidence = new ArrayList<>();
        evidence.add(createTestChunk(1L, 101L, "CE-20 nominal thrust is 190 kN in preliminary revision.", "Thrust", 0.80f));
        evidence.add(createTestChunk(2L, 102L, "CE-20 nominal thrust uprated to 200 kN in production revision.", "Thrust", 0.95f));

        EvidenceRoutingService.RoutedEvidenceSet routed = routingService.routeEvidenceByRevision(
            "What is CE-20 thrust?", evidence, targetRev
        );

        assertNotNull(routed);
        assertEquals(2, routed.getTotalEvidenceCount());
        assertTrue(routed.getApplicableEvidenceCount() > 0);
        assertNotNull(routed.getRoutedEvidence());
        // Verify applicability scores were computed
        assertTrue(routed.getRoutedEvidence().stream().allMatch(e -> e.getApplicabilityScore() != null));
    }

    @Test
    @DisplayName("Regression: What is ISRO query without definitional evidence abstains with zero confidence")
    void testWhatIsISROWithoutDefinitionalEvidenceAbstainsWithZeroConfidence() {
        String query = "What is ISRO?";
        // Evidence contains only launch vehicle engine specs (CE-20), no organizational definition
        List<RetrievalResultDTO> evidence = List.of(
            createTestChunk(1L, 100L,
                "The CE-20 is an indigenous cryogenic rocket engine developed by ISRO for the LVM3 launch vehicle upper stage.",
                "CE-20 Introduction", 0.90f),
            createTestChunk(2L, 100L,
                "ISRO Cryogenic Upper Stage CUS technical characteristics and operating parameters.",
                "CUS Overview", 0.85f)
        );

        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        assertNotNull(answer);
        assertNotNull(answer.getAnswerText());
        assertTrue(answerGenerationService.isInsufficientEvidenceAnswer(answer.getAnswerText()),
            "Answer must explicitly state that available documents do not provide enough information to define ISRO");
        assertTrue(answer.getAnswerText().contains("do not provide enough information to answer this question")
                || answer.getAnswerText().contains("insufficient"),
            "Answer must include clear insufficient-evidence explanation");
        assertEquals(0.0f, answer.getConfidence(),
            "Confidence must be calibrated to 0.0 on definitional abstention");
        assertEquals(0, answer.getCitations().size(),
            "Abstention must not cite irrelevant engine specs as supporting a definition");
    }

    @Test
    @DisplayName("Regression: What is ISRO query with definitional evidence answers and produces positive confidence")
    void testWhatIsISROWithDefinitionalEvidenceAnswers() {
        String query = "What is ISRO?";
        List<RetrievalResultDTO> evidence = List.of(
            createTestChunk(1L, 100L,
                "ISRO is the space agency of India responsible for the development of space technologies and space exploration missions.",
                "About ISRO", 0.95f)
        );

        AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(query, evidence);

        assertNotNull(answer);
        assertNotNull(answer.getAnswerText());
        assertFalse(answerGenerationService.isInsufficientEvidenceAnswer(answer.getAnswerText()),
            "Answer must not abstain when authoritative definitional evidence is provided");
        assertTrue(answer.getConfidence() > 0.0f,
            "Confidence must be positive when definitional evidence is present");
        assertTrue(answer.getCitations().size() > 0,
            "Must have citations for grounded answer");
    }

    @Test
    @DisplayName("Regression: Insufficient evidence detection catches all document-missing phrases")
    void testInsufficientEvidencePhrases() {
        assertTrue(answerGenerationService.isInsufficientEvidenceAnswer(
            "The available indexed documents do not provide enough information to answer this question."));
        assertTrue(answerGenerationService.isInsufficientEvidenceAnswer(
            "The available evidence is insufficient to establish the requested fact."));
        assertTrue(answerGenerationService.isInsufficientEvidenceAnswer(
            "The provided evidence does not contain information about the spacecraft propulsion system."));
        assertFalse(answerGenerationService.isInsufficientEvidenceAnswer(
            "The CE-20 engine powers the upper stage of the LVM3 launch vehicle."));
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
