package com.spacedoc.service;

import com.spacedoc.dto.RetrievalResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * PHASE 4b: Answer Generation Service
 *
 * Synthesizes evidence into structured answers using LLM (Ollama).
 * Extracts citations and maps them back to source chunks.
 *
 * Key Features:
 * - Accepts routed evidence from EvidenceRoutingService
 * - Calls Ollama mistral model with evidence context
 * - Extracts citations with deterministic regex patterns
 * - Maps citations to source chunk IDs
 * - Returns structured response with answer, citations, and metadata
 *
 * For testing: Supports mock mode for deterministic test responses
 */
@Service
public class AnswerGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AnswerGenerationService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spacedoc.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${spacedoc.ollama.model:mistral}")
    private String ollamaModel;

    @Value("${spacedoc.answer.enable-mock:false}")
    private boolean enableMockMode;

    public AnswerGenerationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate answer from routed evidence
     *
     * @param query    Original user query
     * @param evidence Routed evidence set from EvidenceRoutingService
     * @return GeneratedAnswer with synthesized response, citations, and metadata
     */
    public GeneratedAnswer generateAnswer(String query, List<RetrievalResultDTO> evidence) {
        GeneratedAnswer answer = new GeneratedAnswer();
        answer.setQuery(query);
        answer.setEvidenceCount(evidence != null ? evidence.size() : 0);

        // Handle empty evidence
        if (evidence == null || evidence.isEmpty()) {
            answer.setAnswerText("No evidence found to answer this question.");
            answer.setCitations(Collections.emptyList());
            answer.setConfidence(0.0f);
            answer.setGenerationMethod("no_evidence");
            answer.setCitationCoverage(0.0f);
            log.warn("Answer generation with no evidence for query: {}", query);
            return answer;
        }

        try {
            // Build context from evidence
            String context = buildContext(evidence);

            // Generate answer using LLM
            String generatedText = generateLLMResponse(query, context);

            // Extract citations from generated text
            List<Citation> citations = extractCitations(generatedText, evidence);

            // Clean answer text (remove citation patterns)
            String cleanAnswer = cleanAnswerText(generatedText);

            // Build response
            answer.setAnswerText(cleanAnswer);
            answer.setCitations(citations);
            answer.setConfidence(computeConfidence(citations, evidence.size()));
            answer.setGenerationMethod("llm");
            answer.setContextWindowSize(context.length());
            answer.setCitationCoverage((float) citations.size() / evidence.size());

            log.info("Generated answer for query '{}' with {} citations from {} evidence items",
                    query, citations.size(), evidence.size());

            return answer;

        } catch (Exception e) {
            log.error("Error generating answer for query: {}", query, e);
            answer.setAnswerText("Error generating answer: " + e.getMessage());
            answer.setCitations(Collections.emptyList());
            answer.setConfidence(0.0f);
            answer.setGenerationMethod("error");
            return answer;
        }
    }

    /**
     * Build context string from evidence chunks
     */
    private String buildContext(List<RetrievalResultDTO> evidence) {
        StringBuilder context = new StringBuilder();
        context.append("You are a technical assistant helping answer questions about ISRO technical documents.\n");
        context.append("Based on the following evidence, provide a clear, concise answer.\n\n");
        context.append("EVIDENCE:\n");
        context.append("---\n");

        for (int i = 0; i < evidence.size(); i++) {
            RetrievalResultDTO chunk = evidence.get(i);
            context.append("Source ").append(i + 1).append(" (Chunk ID: ").append(chunk.getChunkId())
                    .append(", Score: ").append(String.format("%.2f", chunk.getRelevanceScore()))
                    .append("):\n");

            if (chunk.getSectionTitle() != null && !chunk.getSectionTitle().isEmpty()) {
                context.append("Section: ").append(chunk.getSectionTitle()).append("\n");
            }

            context.append(chunk.getContent()).append("\n\n");
        }

        context.append("---\n");
        context.append("QUESTION:\n");
        context.append(
                "Provide a comprehensive answer. When citing sources, use format: [Source N] where N is the source number.\n");

        return context.toString();
    }

    /**
     * Generate response using Ollama LLM
     */
    private String generateLLMResponse(String query, String context) {
        if (enableMockMode) {
            return generateMockResponse(query);
        }

        try {
            // Build Ollama request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", context + "\nAnswer: ");
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.3);

            log.debug("Calling Ollama at {} with model {}", ollamaUrl, ollamaModel);

            // Call Ollama API
            String response = restTemplate.postForObject(
                    ollamaUrl + "/api/generate",
                    requestBody,
                    String.class);

            if (response == null) {
                log.warn("Ollama returned null response");
                return "Unable to generate answer (Ollama unavailable)";
            }

            // Extract response text from JSON
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            Object responseText = responseMap.get("response");

            if (responseText == null) {
                log.warn("No response field in Ollama output");
                return "Unable to extract response from Ollama";
            }

            return responseText.toString().trim();

        } catch (Exception e) {
            log.error("Error calling Ollama: {}", e.getMessage());
            return "Unable to generate answer (LLM service unavailable)";
        }
    }

    /**
     * Generate deterministic mock response for testing
     */
    private String generateMockResponse(String query) {
        // Mock response that includes source citations
        // This is deterministic and doesn't require Ollama running
        return "Based on the provided evidence, the answer to your question about " + query +
                " can be found in the technical specifications. [Source 1] provides foundational information, " +
                "while [Source 2] offers additional technical details. The relationship between these concepts " +
                "is clarified in [Source 3].";
    }

    /**
     * Extract citations from answer text and map to source chunks
     */
    private List<Citation> extractCitations(String answerText, List<RetrievalResultDTO> evidence) {
        List<Citation> citations = new ArrayList<>();

        // Pattern: [Source N] or [Citation N]
        Pattern pattern = Pattern.compile("\\[Source (\\d+)\\]|\\[Citation (\\d+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(answerText);

        Set<Integer> citedSources = new HashSet<>();

        while (matcher.find()) {
            int sourceNum = -1;
            if (matcher.group(1) != null) {
                sourceNum = Integer.parseInt(matcher.group(1));
            } else if (matcher.group(2) != null) {
                sourceNum = Integer.parseInt(matcher.group(2));
            }

            if (sourceNum > 0 && sourceNum <= evidence.size()) {
                citedSources.add(sourceNum);
            }
        }

        // Create Citation objects for each cited source
        for (Integer sourceNum : citedSources) {
            RetrievalResultDTO evidence_item = evidence.get(sourceNum - 1);
            Citation citation = new Citation();
            citation.setSourceNumber(sourceNum);
            citation.setChunkId(evidence_item.getChunkId());
            citation.setRevisionId(evidence_item.getRevisionId());
            citation.setSectionTitle(evidence_item.getSectionTitle());
            citation.setRelevanceScore(evidence_item.getRelevanceScore());
            citation.setApplicabilityScore(evidence_item.getApplicabilityScore());

            citations.add(citation);
        }

        // Sort by source number
        citations.sort(Comparator.comparingInt(Citation::getSourceNumber));

        return citations;
    }

    /**
     * Clean citation markers from answer text
     */
    private String cleanAnswerText(String answerText) {
        // Remove [Source N] and [Citation N] patterns but keep the text
        return answerText.replaceAll("\\[(?:Source|Citation) \\d+\\]", "").trim();
    }

    /**
     * Compute confidence score based on citations and evidence quality
     */
    private Float computeConfidence(List<Citation> citations, int totalEvidence) {
        if (citations.isEmpty() || totalEvidence == 0) {
            return 0.0f;
        }

        // Citation coverage: 0 to 50% confidence
        float citationCoverage = (float) citations.size() / totalEvidence * 0.5f;

        // Average relevance score of cited sources: 0 to 50% confidence
        float avgRelevance = (float) citations.stream()
                .mapToDouble(c -> c.getRelevanceScore() != null ? c.getRelevanceScore() : 0.0)
                .average()
                .orElse(0.0);
        float relevanceConfidence = avgRelevance * 0.5f;

        return Math.min(1.0f, citationCoverage + relevanceConfidence);
    }

    /**
     * Generated Answer DTO
     */
    public static class GeneratedAnswer {
        private String query;
        private String answerText;
        private List<Citation> citations;
        private Float confidence;
        private Integer evidenceCount;
        private String generationMethod; // "llm", "mock", "no_evidence", "error"
        private Integer contextWindowSize;
        private Float citationCoverage;

        // Getters & Setters
        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getAnswerText() {
            return answerText;
        }

        public void setAnswerText(String answerText) {
            this.answerText = answerText;
        }

        public List<Citation> getCitations() {
            return citations;
        }

        public void setCitations(List<Citation> citations) {
            this.citations = citations;
        }

        public Float getConfidence() {
            return confidence;
        }

        public void setConfidence(Float confidence) {
            this.confidence = confidence;
        }

        public Integer getEvidenceCount() {
            return evidenceCount;
        }

        public void setEvidenceCount(Integer evidenceCount) {
            this.evidenceCount = evidenceCount;
        }

        public String getGenerationMethod() {
            return generationMethod;
        }

        public void setGenerationMethod(String generationMethod) {
            this.generationMethod = generationMethod;
        }

        public Integer getContextWindowSize() {
            return contextWindowSize;
        }

        public void setContextWindowSize(Integer contextWindowSize) {
            this.contextWindowSize = contextWindowSize;
        }

        public Float getCitationCoverage() {
            return citationCoverage;
        }

        public void setCitationCoverage(Float citationCoverage) {
            this.citationCoverage = citationCoverage;
        }
    }

    /**
     * Citation DTO
     */
    public static class Citation {
        private Integer sourceNumber;
        private Long chunkId;
        private Long revisionId;
        private String sectionTitle;
        private Float relevanceScore;
        private Float applicabilityScore;

        // Getters & Setters
        public Integer getSourceNumber() {
            return sourceNumber;
        }

        public void setSourceNumber(Integer sourceNumber) {
            this.sourceNumber = sourceNumber;
        }

        public Long getChunkId() {
            return chunkId;
        }

        public void setChunkId(Long chunkId) {
            this.chunkId = chunkId;
        }

        public Long getRevisionId() {
            return revisionId;
        }

        public void setRevisionId(Long revisionId) {
            this.revisionId = revisionId;
        }

        public String getSectionTitle() {
            return sectionTitle;
        }

        public void setSectionTitle(String sectionTitle) {
            this.sectionTitle = sectionTitle;
        }

        public Float getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(Float relevanceScore) {
            this.relevanceScore = relevanceScore;
        }

        public Float getApplicabilityScore() {
            return applicabilityScore;
        }

        public void setApplicabilityScore(Float applicabilityScore) {
            this.applicabilityScore = applicabilityScore;
        }
    }

}

    // Getters & Setters
    public Integer getSourceNumber() {
        return sourceNumber;
    }

    public void setSourceNumber(Integer sourceNumber) {
        this.sourceNumber = sourceNumber;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(Long revisionId) {
        this.revisionId = revisionId;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public Float getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Float relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Float getApplicabilityScore() {
        return applicabilityScore;
    }

    public void setApplicabilityScore(Float applicabilityScore) {
        this.applicabilityScore = applicabilityScore;
    }
}}
