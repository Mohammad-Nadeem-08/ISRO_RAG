# Groq API Migration & End-to-End Verification

**Application:** SpaceDoc Intelligence (ISRO Technical Document RAG)  
**Date:** 2026-10-28  
**Repository:** [Mohammad-Nadeem-08/ISRO_RAG](https://github.com/Mohammad-Nadeem-08/ISRO_RAG)

---

## 1. Overview & Motivation

SpaceDoc Intelligence was originally configured to rely exclusively on local Ollama instances for both vector embeddings and large language model (LLM) generation. In technical document retrieval environments, local LLM generation encounters significant throughput bottlenecks, high VRAM consumption, and latency spikes on resource-constrained hardware.

To achieve production-grade response speeds without sacrificing document privacy, the architecture was refactored into a **hybrid dual-engine AI pipeline**:
1. **Local Embeddings (Ollama):** High-throughput, privacy-preserving dense vector generation using `nomic-embed-text` (768 dimensions), executed on localhost.
2. **Cloud Generation (Groq Cloud API):** Ultra-low-latency, instruction-tuned factual synthesis using Groq's high-speed LPU inference engine (`openai/gpt-oss-120b` or LLaMA 3.3).

---

## 2. Architecture & Service Design

```
+-------------------------------------------------------------+
|                     User Query / Request                    |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                      RetrievalService                       |
|  - PostgreSQL Full-Text Search (tsvector)                   |
|  - pgvector HNSW Cosine Search (via Ollama embeddings)      |
|  - Reciprocal Rank Fusion (RRF k=60)                        |
|  - Subject-Predicate Relationship Reranker                  |
+-------------------------------------------------------------+
                              |
                     Ranked Evidence Set
                              |
                              v
+-------------------------------------------------------------+
|                   AnswerGenerationService                   |
|  - Strict Grounding Prompt & Negative Constraint Injection   |
|  - HTTP POST to Groq API (openai/gpt-oss-120b)              |
|  - Citation Parsing & Reference Bounds Validation           |
|  - Claim Support Verification & Confidence Calibration       |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                      Generated Answer                       |
|  - Grounded Markdown text with inline citations [Source N]  |
|  - Calibrated confidence score (0.0 - 1.0)                  |
|  - Citation coverage ratio & validation flags               |
+-------------------------------------------------------------+
```

### 2.1 Configuration Properties (`application.properties`)
```properties
# Groq Cloud API Configuration
groq.api.key=${GROQ_API_KEY:}
groq.api.url=${GROQ_BASE_URL:https://api.groq.com/openai/v1}
groq.model=${GROQ_MODEL:openai/gpt-oss-120b}
groq.timeout=30000
groq.max-tokens=1024
groq.temperature=0.1
```

### 2.2 Secure Environment Loading
The Spring Boot application automatically resolves `${GROQ_API_KEY}` from:
- System environment variables
- Container environment variables (`docker-compose.yml`)
- `.env` file via `run_app.ps1` (safely loaded into the current process without printing or leaking keys)

---

## 3. Strict Factual Grounding & Constraint Engineering

To eliminate hallucinations and prevent domain confusion (e.g., conflating launch vehicle stages with spacecraft payloads), `AnswerGenerationService.java` enforces:

1. **Negative Constraints & Entity Differentiation:**
   > *"CRITICAL: Maintain strict technical distinctions between different aerospace systems (for example, a launch vehicle like LVM3 vs. a spacecraft/payload like Chandrayaan-3). Do not ascribe properties of a rocket stage to a spacecraft payload or vice versa."*

2. **Explicit Abstention Directive:**
   > *"If the retrieved evidence describes a related launch vehicle, rocket stage, or engine (such as CE-20 or LVM3) but DOES NOT explicitly describe the specific system requested for the spacecraft payload, you MUST explicitly state that the provided evidence does not contain that information."*

3. **Claim Support Validation (`validateClaimSupport`):**
   Every generated sentence is checked against the cited chunk content. If the sentence makes claims unbacked by the source chunk's terminology, the claim is flagged, and confidence is dynamically adjusted.

4. **Calibrated Confidence Scoring:**
   - Base confidence is computed from evidence retrieval similarity and citation coverage.
   - If the system triggers an abstention response (*"does not contain"*, *"insufficient evidence"*), confidence is automatically clamped to `0.0`.

---

## 4. Live Verification Results

Live end-to-end testing was performed against the running Spring Boot instance connected to Groq Cloud API with real ISRO technical documents ingested.

### Verification Case 1: Negative Constraint & Abstention
- **Input Query:** *"What is Chandrayaan-3's propulsion system?"*
- **Corpus State:** Corpus contains LVM3 CE-20 cryogenic upper-stage documentation, but no Chandrayaan-3 spacecraft propulsion documentation.
- **System Behavior:**
  - Subject-predicate reranker identified query subject as `chandrayaan-3` and predicate as `propulsion`.
  - Retrieved LVM3 chunks were flagged as subject mismatches.
  - Groq generated:
    > *"Based on the provided sources, there is no information detailing Chandrayaan-3's propulsion system. The retrieved documentation specifically describes the CE-20 cryogenic engine powering the upper stage of the LVM3 launch vehicle, not the propulsion system of the Chandrayaan-3 spacecraft itself."*
  - **Metrics:** `confidence: 0.0`, `citationCoverage: 0.0`, `generationMethod: "llm"`.
  - **Result:** **PASS** (Zero hallucination; successfully distinguished launch vehicle from spacecraft).

### Verification Case 2: Positive Grounded Answering
- **Input Query:** *"What engine powers the upper stage of the LVM3 launch vehicle?"*
- **Corpus State:** Corpus contains LVM3 CE-20 cryogenic stage documentation.
- **System Behavior:**
  - Subject-predicate reranker matched both subject (`lvm3`) and predicate (`engine`).
  - Groq generated:
    > *"The upper stage of the LVM3 launch vehicle is powered by the indigenously developed CE-20 cryogenic engine [Source 1]."*
  - **Metrics:** `confidence: 0.90`, `citationCoverage: 1.0`, `claimSupported: true`, `invalidCitations: []`.
  - **Result:** **PASS** (Precise answer with verified inline citation).

---

## 5. Summary Matrix

| Verification Metric | Expected Standard | Observed Value | Status |
|---|---|---|---|
| Groq HTTP Status | `200 OK` | `200 OK` | **PASS** |
| Inference Latency | < 3000 ms | ~1200 ms | **PASS** |
| Model Utilization | Configured `GROQ_MODEL` | `openai/gpt-oss-120b` | **PASS** |
| Citation Parsing | Non-empty citation array | Citations correctly parsed and linked | **PASS** |
| Unit Regression Tests | 95 / 95 passing | 95 passed, 0 failures | **PASS** |
