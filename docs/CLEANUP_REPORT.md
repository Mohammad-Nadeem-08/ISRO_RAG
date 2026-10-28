# SpaceDoc Intelligence (ISRO_RAG) - Production Cleanup Report

**Date:** 2026-10-28  
**Repository:** [Mohammad-Nadeem-08/ISRO_RAG](https://github.com/Mohammad-Nadeem-08/ISRO_RAG)  
**Target Branch:** `main`  
**Application:** SpaceDoc Intelligence (Revision-Aware Technical Document RAG)

---

## 1. Executive Summary

A comprehensive production-quality cleanup, security audit, architectural refinement, and verification of the SpaceDoc Intelligence ISRO_RAG codebase was completed. The system was transitioned from an experimental local-LLM setup to an enterprise-grade hybrid architecture utilizing the **Groq API** (`openai/gpt-oss-120b`) for rapid, grounded answer generation and a local **Ollama** service (`nomic-embed-text`, 768-dimensional embeddings) for dense vector generation, integrated with **PostgreSQL 16 / pgvector** HNSW indexing and full-text search.

All 95 automated regression tests pass with zero failures or errors, verifying full functional preservation across security, document ingestion, chunking, conflict detection, dependency graph traversal, evidence routing, subject-predicate reranking, and citation validation.

---

## 2. File Cleanup Inventory

### 2.1 Files Untracked / Removed from Git Index
The following categories of files were confirmed to be unnecessary or transient and were removed from Git tracking:

| File / Pattern | Category | Rationale |
|---|---|---|
| `target/*` (105 `.class` and metadata files) | Build Artifacts | Compiled Maven classes and surefire reports were previously checked into version control, polluting repository diffs. Untracked from Git while keeping `.gitignore` active. |
| `BUILD_STATUS.txt` | Transient Log | Stale build status log from previous manual test execution. |
| `build-test.log` | Transient Log | Ephemeral build logs in repository root. |
| `logs/spacedoc.log` & `logs/*.gz` | Runtime Logs | Application log files containing runtime traces and potential connection info. Untracked and excluded via `.gitignore`. |
| `.claude/settings.local.json` | Local IDE State | Tool-specific configuration file; excluded via `.gitignore`. |
| `experiment_results.json` | Generated Artifact | Output generated during evaluation test runs (`ReproducibleEvaluationTest`). Excluded via `.gitignore` to prevent test-run churn in Git. |

### 2.2 Files Retained with Justification
The following files were reviewed and preserved:

| File / Directory | Justification |
|---|---|
| `spacedoc-benchmark-v2-authorized.json` | Ground-truth benchmark dataset required by `ReproducibleEvaluationTest` and `EvaluationServiceTest`. |
| `docker/postgres-init.sql` | Database schema initialization script creating the `vector` extension, tables, and vector operators for Docker Compose. |
| `docker-compose.yml` | Container specification for PostgreSQL 16 + pgvector containerized deployment. |
| `docs/invention-research/` | Core research assets (`invention-hypothesis.md`, `prior-art-matrix.md`) detailing the academic contribution of revision-aware evidence routing. |
| `run_app.ps1` | Production PowerShell startup script with pre-flight TCP checks, safe `.env` loading, and Maven execution. |
| `src/main/webapp/WEB-INF/views/*.jsp` | Complete UI view templates (login, dashboard, research assistant, ingestion centre, document library, document detail). |

---

## 3. Code Refactoring & Security Audit

### 3.1 Security & Secret Management
- **`.env.example` Sanitization:** Replaced actual API key pattern strings with strict placeholder values (`GROQ_API_KEY=your_groq_api_key_here`, `admin_local_dev_password`).
- **`.gitignore` Hardening:** Ensured rules for `.env`, `target/`, `logs/`, `*.log`, `experiment_results.json`, and `.claude/` prevent any credential or log leaks.
- **Credential Masking:** Startup scripts and test suites verify environment variable presence without printing or exposing keys.

### 3.2 Dual-Stack IPv4 Networking Fix
- **Issue:** Windows 11 environments resolving `localhost` to IPv6 `[::1]` caused connection failures (`Connection refused`) when interacting with Ollama.
- **Resolution:** Updated `EmbeddingService.java` to normalize endpoint URLs, replacing both `localhost` and `[::1]` with explicit IPv4 `127.0.0.1`.

### 3.3 Subject-Predicate Query Reranking
- **Issue:** Queries such as *"What is Chandrayaan-3's propulsion system?"* retrieved LVM3 launch-vehicle CE-20 cryogenic engine chunks due to shared keywords (*propulsion*, *engine*).
- **Resolution:** Added `analyzeQuerySubject` and `rerankBySubjectAndRelationship` in `RetrievalService.java`. The system extracts the core query subject (e.g., *chandrayaan-3*) and target predicate (e.g., *propulsion*), applying a dynamic relevance boost (+0.35) to candidate chunks matching both subject and predicate, preventing off-target retrieval.

### 3.4 Grounded Generation & Citation Claim Validation
- **Negative Constraints:** Engineered strict system prompts in `AnswerGenerationService.java` explicitly distinguishing launch vehicles from spacecraft and instructing the LLM to abstain when evidence is insufficient.
- **Claim Support Validation:** Implemented `validateClaimSupport` to ensure cited sources actually contain key nouns and entities from the generated sentences.
- **Calibrated Confidence:** Automatically penalizes confidence scores (setting to 0.0) when the LLM abstains or when citations fail verification.

---

## 4. Test Suite & Verification Results

### 4.1 Maven Unit & Integration Tests
Command executed: `mvn clean test`

| Test Suite | Tests Run | Failures | Errors | Skipped |
|---|---|---|---|---|
| `ChunkingServiceTest` | 8 | 0 | 0 | 0 |
| `TextExtractionServiceTest` | 5 | 0 | 0 | 0 |
| `RevisionComparisonServiceTest` | 6 | 0 | 0 | 0 |
| `DependencyGraphServiceTest` | 6 | 0 | 0 | 0 |
| `ConflictDetectionServiceTest` | 5 | 0 | 0 | 0 |
| `EvidenceRoutingServiceTest` | 7 | 0 | 0 | 0 |
| `RetrievalServiceTest` | 8 | 0 | 0 | 0 |
| `AnswerGenerationServiceTest` | 13 | 0 | 0 | 0 |
| `EvaluationServiceTest` | 8 | 0 | 0 | 0 |
| `ReproducibleEvaluationTest` | 5 | 0 | 0 | 0 |
| `ImpactAnalysisServiceUnitTest` | 6 | 0 | 0 | 0 |
| `ImpactAnalysisServiceTest` | 4 | 0 | 0 | 0 |
| `SecurityIntegrationTest` | 6 | 0 | 0 | 0 |
| `DocumentIngestionIntegrationTest` | 4 | 0 | 0 | 0 |
| `ResearchAssistantIntegrationTest` | 2 | 0 | 0 | 0 |
| `IngestionControllerIntegrationTest` | 2 | 0 | 0 | 0 |
| **Total** | **95** | **0** | **0** | **0** |

### 4.2 Packaging Verification
Command executed: `mvn package -DskipTests`  
**Output:** `target/spacedoc-intelligence-1.0.0-SNAPSHOT.war` successfully packaged.

---

## 5. Live Runtime Verification

| Verification Item | Target | Result | Status |
|---|---|---|---|
| Health Check | `GET /api/system/health` | `HTTP 200` (`status: UP`, `version: 1.0.0-SNAPSHOT`) | **PASS** |
| PostgreSQL / pgvector | `127.0.0.1:5432` | TCP open; Flyway migrations V1 & V2 applied | **PASS** |
| Ollama Embeddings | `127.0.0.1:11434` | Vector generation with `nomic-embed-text` (768 dim) | **PASS** |
| Groq Answer Generation | `api.groq.com/openai/v1` | Model `openai/gpt-oss-120b` generated grounded answers | **PASS** |
| Grounding / Abstention | Query: *Chandrayaan-3 propulsion* | Correctly abstained, cited lack of spacecraft evidence, confidence 0.0 | **PASS** |
| Precision Answering | Query: *LVM3 upper stage engine* | Correctly identified CE-20, cited Source 1, confidence 0.9 | **PASS** |
| Security / Login | `GET /login` | Spring Security form rendered, CSRF tokens active | **PASS** |
