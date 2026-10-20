package com.spacedoc.service;

import com.spacedoc.repository.DocumentChunkRepository;
import com.spacedoc.repository.DocumentRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        retrievalService = new RetrievalService(chunkRepository, embeddingService);
        revisionComparisonService = new RevisionComparisonService(revisionRepository);
        conflictDetectionService = new ConflictDetectionService();
        evidenceRoutingService = new EvidenceRoutingService(revisionComparisonService, conflictDetectionService);

        RestTemplate restTemplate = new RestTemplate();
        answerGenerationService = new AnswerGenerationService(restTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(answerGenerationService, "enableMockMode", true);

        evaluationService = new EvaluationService(
            retrievalService,
            evidenceRoutingService,
            conflictDetectionService,
            answerGenerationService
        );
    }

    @Test
    void testRunEvaluation_EmptyQuestions() {
        EvaluationService.EvaluationRunResult result = evaluationService.runEvaluation(
            "empty-fixture",
            Collections.emptyList(),
            "HYBRID",
            false,
            false
        );

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(0, result.getQuestionsProcessed());
        assertEquals(0.0, result.getMeanRecall());
    }
}
