package com.spacedoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.spacedoc.repository.DocumentChunkRepository;
import com.spacedoc.repository.DocumentRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproducible Evaluation Suite for SpaceDoc Intelligence.
 *
 * Implements Milestone 2:
 * - Uses a fixed, versioned benchmark dataset (spacedoc-benchmark-v1) with reviewed expected evidence.
 * - Compares lexical, dense, standard hybrid, and revision-aware hybrid retrieval.
 * - Runs ablation experiments (disabling revision routing and conflict detection individually).
 * - Calculates Recall@k, Precision@k, MRR, citation validity, conflict detection count, and latency.
 * - Exports experiment results to a machine-readable JSON file (experiment_results.json).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Reproducible Evaluation Suite & Experiments")
class ReproducibleEvaluationTest {

    @Mock
    private DocumentChunkRepository chunkRepository;

    @Mock
    private DocumentRevisionRepository revisionRepository;

    @Mock
    private EmbeddingService embeddingService;

    private RetrievalService retrievalService;
    private RevisionComparisonService revisionComparisonService;
    private ConflictDetectionService conflictDetectionService;
    private EvidenceRoutingService evidenceRoutingService;
    private AnswerGenerationService answerGenerationService;
    private EvaluationService evaluationService;

    private List<EvaluationService.EvalQuestion> benchmarkDataset;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        retrievalService = new RetrievalService(chunkRepository, embeddingService);
        revisionComparisonService = new RevisionComparisonService(revisionRepository);
        conflictDetectionService = new ConflictDetectionService();
        evidenceRoutingService = new EvidenceRoutingService(revisionComparisonService, conflictDetectionService);

        RestTemplate restTemplate = new RestTemplate();
        answerGenerationService = new AnswerGenerationService(restTemplate);
        ReflectionTestUtils.setField(answerGenerationService, "enableMockMode", true);

        evaluationService = new EvaluationService(
            retrievalService,
            evidenceRoutingService,
            conflictDetectionService,
            answerGenerationService
        );

        // Initialize versioned benchmark dataset: spacedoc-benchmark-v1
        benchmarkDataset = createBenchmarkDataset();
    }

    private List<EvaluationService.EvalQuestion> createBenchmarkDataset() {
        List<EvaluationService.EvalQuestion> questions = new ArrayList<>();

        // Question 1: Lunar Mission Propulsion
        EvaluationService.EvalQuestion q1 = new EvaluationService.EvalQuestion(
            "Q-001",
            "What are the primary propulsion parameters for the lunar orbital insertion burn?",
            Arrays.asList("DOC-LUNAR-01", "DOC-PROP-02")
        );
        questions.add(q1);

        // Question 2: Telemetry Encryption Standards
        EvaluationService.EvalQuestion q2 = new EvaluationService.EvalQuestion(
            "Q-002",
            "Which cryptographic standards are mandated for S-band telemetry downlink?",
            Arrays.asList("DOC-SEC-01", "DOC-COMM-04")
        );
        questions.add(q2);

        // Question 3: Power Subsystem Thermal Limits
        EvaluationService.EvalQuestion q3 = new EvaluationService.EvalQuestion(
            "Q-003",
            "What is the maximum operating junction temperature for solar array regulator electronics?",
            Arrays.asList("DOC-PWR-03")
        );
        questions.add(q3);

        // Question 4: Orbit Control System Requirements
        EvaluationService.EvalQuestion q4 = new EvaluationService.EvalQuestion(
            "Q-004",
            "Detail the reaction wheel desaturation protocol and momentum management limits.",
            Arrays.asList("DOC-AOCS-01", "DOC-AOCS-02")
        );
        questions.add(q4);

        return questions;
    }

    @Test
    @DisplayName("Run complete benchmark evaluation across all methods and ablations, export JSON results")
    void testRunCompleteBenchmarkAndExport() throws Exception {
        Map<String, EvaluationService.EvaluationRunResult> experimentResults = new LinkedHashMap<>();

        // 1. Baseline: LEXICAL
        EvaluationService.EvaluationRunResult lexicalResult = evaluationService.runEvaluation(
            "spacedoc-benchmark-v1", benchmarkDataset, "LEXICAL", false, false
        );
        experimentResults.put("LEXICAL_BASELINE", lexicalResult);

        // 2. Baseline: DENSE
        EvaluationService.EvaluationRunResult denseResult = evaluationService.runEvaluation(
            "spacedoc-benchmark-v1", benchmarkDataset, "DENSE", false, false
        );
        experimentResults.put("DENSE_BASELINE", denseResult);

        // 3. Baseline: STANDARD_HYBRID
        EvaluationService.EvaluationRunResult hybridResult = evaluationService.runEvaluation(
            "spacedoc-benchmark-v1", benchmarkDataset, "HYBRID", false, false
        );
        experimentResults.put("STANDARD_HYBRID", hybridResult);

        // 4. Proposed: REVISION_AWARE_HYBRID (Routing + Conflict Detection)
        EvaluationService.EvaluationRunResult fullProposedResult = evaluationService.runEvaluation(
            "spacedoc-benchmark-v1", benchmarkDataset, "HYBRID", true, true
        );
        experimentResults.put("REVISION_AWARE_HYBRID_FULL", fullProposedResult);

        // 5. Ablation 1: Disable Routing (Conflict Detection only)
        EvaluationService.EvaluationRunResult ablationNoRouting = evaluationService.runEvaluation(
            "spacedoc-benchmark-v1", benchmarkDataset, "HYBRID", false, true
        );
        experimentResults.put("ABLATION_NO_ROUTING", ablationNoRouting);

        // 6. Ablation 2: Disable Conflict Detection (Routing only)
        EvaluationService.EvaluationRunResult ablationNoConflicts = evaluationService.runEvaluation(
            "spacedoc-benchmark-v1", benchmarkDataset, "HYBRID", true, false
        );
        experimentResults.put("ABLATION_NO_CONFLICTS", ablationNoConflicts);

        // Assertions & Validation
        for (Map.Entry<String, EvaluationService.EvaluationRunResult> entry : experimentResults.entrySet()) {
            String configName = entry.getKey();
            EvaluationService.EvaluationRunResult res = entry.getValue();

            assertNotNull(res, configName + " result should not be null");
            assertEquals("COMPLETED", res.getStatus(), configName + " status should be COMPLETED");
            assertEquals(4, res.getQuestionsProcessed(), configName + " should process 4 questions");
            assertTrue(res.getMeanRecall() >= 0.0 && res.getMeanRecall() <= 1.0);
            assertTrue(res.getMeanPrecision() >= 0.0 && res.getMeanPrecision() <= 1.0);
            assertTrue(res.getMeanMrr() >= 0.0 && res.getMeanMrr() <= 1.0);
            assertTrue(res.getCitationValidityRate() >= 0.0 && res.getCitationValidityRate() <= 1.0);

            System.out.printf("Experiment [%s]: Recall=%.3f, Precision=%.3f, MRR=%.3f, CitationValidity=%.3f, Duration=%dms%n",
                configName, res.getMeanRecall(), res.getMeanPrecision(), res.getMeanMrr(),
                res.getCitationValidityRate(), res.getTotalDurationMs());
        }

        // Export experiment results to machine-readable JSON file
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        File outputFile = Paths.get("experiment_results.json").toFile();
        mapper.writeValue(outputFile, experimentResults);

        assertTrue(outputFile.exists(), "experiment_results.json should be successfully exported");
        assertTrue(outputFile.length() > 0, "experiment_results.json should contain data");

        System.out.println("Successfully exported experiment results to: " + outputFile.getAbsolutePath());
    }
}
