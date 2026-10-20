package com.spacedoc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String embeddingModel;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public EmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public String embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot generate embedding for null or empty text");
        }

        String model = "nomic-embed-text";
        String baseUrl = "http://[::1]:11434";
        if (this.embeddingModel != null && !this.embeddingModel.trim().isEmpty()) {
            model = this.embeddingModel.trim();
        }
        if (this.ollamaBaseUrl != null && !this.ollamaBaseUrl.trim().isEmpty()) {
            baseUrl = this.ollamaBaseUrl.trim().replace("localhost", "[::1]").replace("127.0.0.1", "[::1]");
        }

        try {
            String url = baseUrl + "/api/embeddings";

            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("prompt", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(request),
                headers
            );

            log.info("Calling Ollama embedding endpoint at {} with model {} and body: {}", url, model, objectMapper.writeValueAsString(request));
            String response = restTemplate.postForObject(url, entity, String.class);
            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            List<?> rawEmbedding = (List<?>) result.get("embedding");
            if (rawEmbedding == null || rawEmbedding.isEmpty()) {
                throw new RuntimeException("Ollama response did not contain 'embedding' field or embedding is empty");
            }

            List<Double> embedding = new ArrayList<>();
            for (Object val : rawEmbedding) {
                if (val instanceof Number) {
                    embedding.add(((Number) val).doubleValue());
                } else {
                    embedding.add(Double.valueOf(val.toString()));
                }
            }

            int dim = embedding.size();
            if (dim != 768) {
                log.warn("Embedding dimension is {}, expected 768 for model {}", dim, embeddingModel);
            }

            return objectMapper.writeValueAsString(embedding);

        } catch (Exception e) {
            log.error("Failed to generate embedding from Ollama: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding from Ollama service: " + e.getMessage(), e);
        }
    }
}
