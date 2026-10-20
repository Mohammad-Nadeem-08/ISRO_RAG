package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final RetrievalService retrievalService;
    private final EvidenceRoutingService evidenceRoutingService;
    private final ConflictDetectionService conflictDetectionService;
    private final AnswerGenerationService answerGenerationService;

    public EvaluationService(RetrievalService retrievalService,
                             EvidenceRoutingService evidenceRoutingService,
                             ConflictDetectionService conflictDetectionService,
                             AnswerGenerationService answerGenerationService) {
        this.retrievalService = retrievalService;
        this.evidenceRoutingService = evidenceRoutingService;
        this.conflictDetectionService = conflictDetectionService;
        this.answerGenerationService = answerGenerationService;
    }

    public EvaluationRunResult runEvaluation(String datasetName, List<EvalQuestion> questions, String method, boolean useRouting, boolean useConflictDetection) {
        long startTime = System.currentTimeMillis();
        EvaluationRunResult runResult = new EvaluationRunResult();
        runResult.setRunKey(UUID.randomUUID().toString());
        runResult.setDatasetName(datasetName);
        runResult.setRetrievalMethod(method);
        runResult.setStartedAt(LocalDateTime.now());

        List<QuestionResult> questionResults = new ArrayList<>();
        double totalRecall = 0.0;
        double totalPrecision = 0.0;
        double totalMrr = 0.0;
        int validCitationsCount = 0;
        int totalQuestions = questions.size();

        if (totalQuestions == 0) {
            runResult.setCompletedAt(LocalDateTime.now());
            runResult.setStatus("COMPLETED");
            return runResult;
        }

        for (EvalQuestion q : questions) {
            long qStart = System.currentTimeMillis();
            QuestionResult qr = new QuestionResult();
            qr.setQuestionId(q.getId());
            qr.setQuestionText(q.getQuestionText());

            // 1. Retrieve
            List<RetrievalResultDTO> retrieved;
            if ("LEXICAL".equalsIgnoreCase(method)) {
                retrieved = retrievalService.lexicalRetrieval(q.getQuestionText(), 5);
            } else if ("DENSE".equalsIgnoreCase(method)) {
                retrieved = retrievalService.denseRetrieval(q.getQuestionText(), 5);
            } else {
                retrieved = retrievalService.hybridRetrieval(q.getQuestionText(), 5);
                retrieved = retrievalService.deduplicateResults(retrieved);
            }

            // 2. Optional routing
            if (useRouting && q.getTargetRevision() != null) {
                EvidenceRoutingService.RoutedEvidenceSet routed = evidenceRoutingService.routeEvidenceByRevision(
                    q.getQuestionText(), retrieved, q.getTargetRevision()
                );
                retrieved = routed.getRoutedEvidence();
            }

            // 3. Optional conflict detection
            if (useConflictDetection && retrieved != null && retrieved.size() > 1) {
                List<ConflictDetectionService.EvidenceConflict> conflicts = conflictDetectionService.detectConflicts(retrieved);
                retrieved = conflictDetectionService.resolveConflicts(retrieved, conflicts);
                qr.setConflictCount(conflicts.size());
            }

            long retrievalTime = System.currentTimeMillis() - qStart;
            qr.setRetrievalLatencyMs(retrievalTime);

            // 4. Generate answer
            List<RetrievalResultDTO> finalRetrieved = retrieved != null ? retrieved : Collections.emptyList();
            AnswerGenerationService.GeneratedAnswer answer = answerGenerationService.generateAnswer(q.getQuestionText(), finalRetrieved);
            qr.setGeneratedAnswer(answer.getAnswerText());
            qr.setCitationCount(answer.getCitations().size());
            qr.setValidCitation(answer.getCitationCoverage() != null && answer.getCitationCoverage() > 0.0f);
            if (qr.isValidCitation()) validCitationsCount++;

            // 5. Compute metrics (Recall@k, Precision@k, MRR)
            Set<String> relevantDocs = new HashSet<>(q.getRelevantDocumentKeys());
            int hits = 0;
            int rank = 1;
            double firstRelevantReciprocal = 0.0;

            for (RetrievalResultDTO res : finalRetrieved) {
                if (relevantDocs.contains(res.getDocumentKey())) {
                    hits++;
                    if (firstRelevantReciprocal == 0.0) {
                        firstRelevantReciprocal = 1.0 / rank;
                    }
                }
                rank++;
            }

            double recall = relevantDocs.isEmpty() ? 1.0 : (double) hits / relevantDocs.size();
            double precision = finalRetrieved.isEmpty() ? 0.0 : (double) hits / finalRetrieved.size();

            qr.setRecall(recall);
            qr.setPrecision(precision);
            qr.setReciprocalRank(firstRelevantReciprocal);

            totalRecall += recall;
            totalPrecision += precision;
            totalMrr += firstRelevantReciprocal;

            questionResults.add(qr);
        }

        runResult.setQuestionsProcessed(totalQuestions);
        runResult.setMeanRecall(totalRecall / totalQuestions);
        runResult.setMeanPrecision(totalPrecision / totalQuestions);
        runResult.setMeanMrr(totalMrr / totalQuestions);
        runResult.setCitationValidityRate((double) validCitationsCount / totalQuestions);
        runResult.setTotalDurationMs(System.currentTimeMillis() - startTime);
        runResult.setQuestionResults(questionResults);
        runResult.setCompletedAt(LocalDateTime.now());
        runResult.setStatus("COMPLETED");

        log.info("Evaluation run completed for dataset '{}' method '{}': Mean Recall={}, Mean Precision={}, MRR={}",
            datasetName, method, runResult.getMeanRecall(), runResult.getMeanPrecision(), runResult.getMeanMrr());

        return runResult;
    }

    public static class EvalQuestion {
        private String id;
        private String questionText;
        private List<String> relevantDocumentKeys;
        private com.spacedoc.entity.DocumentRevision targetRevision;

        public EvalQuestion(String id, String questionText, List<String> relevantDocumentKeys) {
            this.id = id;
            this.questionText = questionText;
            this.relevantDocumentKeys = relevantDocumentKeys;
        }

        public String getId() { return id; }
        public String getQuestionText() { return questionText; }
        public List<String> getRelevantDocumentKeys() { return relevantDocumentKeys; }
        public com.spacedoc.entity.DocumentRevision getTargetRevision() { return targetRevision; }
        public void setTargetRevision(com.spacedoc.entity.DocumentRevision targetRevision) { this.targetRevision = targetRevision; }
    }

    public static class EvaluationRunResult {
        private String runKey;
        private String datasetName;
        private String retrievalMethod;
        private String status;
        private int questionsProcessed;
        private double meanRecall;
        private double meanPrecision;
        private double meanMrr;
        private double citationValidityRate;
        private long totalDurationMs;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private List<QuestionResult> questionResults;

        public String getRunKey() { return runKey; }
        public void setRunKey(String runKey) { this.runKey = runKey; }
        public String getDatasetName() { return datasetName; }
        public void setDatasetName(String datasetName) { this.datasetName = datasetName; }
        public String getRetrievalMethod() { return retrievalMethod; }
        public void setRetrievalMethod(String retrievalMethod) { this.retrievalMethod = retrievalMethod; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getQuestionsProcessed() { return questionsProcessed; }
        public void setQuestionsProcessed(int questionsProcessed) { this.questionsProcessed = questionsProcessed; }
        public double getMeanRecall() { return meanRecall; }
        public void setMeanRecall(double meanRecall) { this.meanRecall = meanRecall; }
        public double getMeanPrecision() { return meanPrecision; }
        public void setMeanPrecision(double meanPrecision) { this.meanPrecision = meanPrecision; }
        public double getMeanMrr() { return meanMrr; }
        public void setMeanMrr(double meanMrr) { this.meanMrr = meanMrr; }
        public double getCitationValidityRate() { return citationValidityRate; }
        public void setCitationValidityRate(double citationValidityRate) { this.citationValidityRate = citationValidityRate; }
        public long getTotalDurationMs() { return totalDurationMs; }
        public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public List<QuestionResult> getQuestionResults() { return questionResults; }
        public void setQuestionResults(List<QuestionResult> questionResults) { this.questionResults = questionResults; }
    }

    public static class QuestionResult {
        private String questionId;
        private String questionText;
        private double recall;
        private double precision;
        private double reciprocalRank;
        private boolean validCitation;
        private int citationCount;
        private int conflictCount;
        private long retrievalLatencyMs;
        private String generatedAnswer;

        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public double getRecall() { return recall; }
        public void setRecall(double recall) { this.recall = recall; }
        public double getPrecision() { return precision; }
        public void setPrecision(double precision) { this.precision = precision; }
        public double getReciprocalRank() { return reciprocalRank; }
        public void setReciprocalRank(double reciprocalRank) { this.reciprocalRank = reciprocalRank; }
        public boolean isValidCitation() { return validCitation; }
        public void setValidCitation(boolean validCitation) { this.validCitation = validCitation; }
        public int getCitationCount() { return citationCount; }
        public void setCitationCount(int citationCount) { this.citationCount = citationCount; }
        public int getConflictCount() { return conflictCount; }
        public void setConflictCount(int conflictCount) { this.conflictCount = conflictCount; }
        public long getRetrievalLatencyMs() { return retrievalLatencyMs; }
        public void setRetrievalLatencyMs(long retrievalLatencyMs) { this.retrievalLatencyMs = retrievalLatencyMs; }
        public String getGeneratedAnswer() { return generatedAnswer; }
        public void setGeneratedAnswer(String generatedAnswer) { this.generatedAnswer = generatedAnswer; }
    }
}
