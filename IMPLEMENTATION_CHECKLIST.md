# SpaceDoc Intelligence Implementation Checklist

**Project Start Date:** 2026-10-20

## Phase 1: Inspect & Research ✅ COMPLETED

- [x] Inspect workspace (empty)
- [x] Review reference repository (devPilot)
- [x] Create project README with vision and architecture
- [x] Document invention hypothesis (revision-aware evidence routing)
- [x] Create prior-art matrix with research findings
- [x] Identify research gaps and investigation needs

**Deliverables:**
- README.md with complete project overview
- invention-hypothesis.md with problem, solution, and success criteria
- prior-art-matrix.md with 40+ relevant publications and patents analyzed

---

## Phase 2: Foundation ✅ COMPLETED

- [x] Create Maven pom.xml with Spring Boot 3.3.0
- [x] Add all required dependencies (PostgreSQL, JSP, Security, PDF/Tika, Flyway)
- [x] Create application.properties with database and security config
- [x] Create Spring Boot main class (SpaceDocIntelligenceApplication)
- [x] Create SecurityConfiguration with in-memory auth
- [x] Create docker-compose.yml for PostgreSQL + Ollama
- [x] Create database migration V1__initial_schema.sql
- [x] Create core entities (Document, DocumentRevision, DocumentChunk, etc.)
- [x] Create login.jsp page with authentication UI
- [x] Create basic controller and repository stubs
- [x] **Verify initial Maven build succeeds**

**Build Status:** ✅ SUCCESS (mvn clean compile)

**Deliverables:**
- Fully functional Spring Boot WAR project
- PostgreSQL schema with pgvector support
- Docker Compose for local development
- Login page and security configuration
- 9 entity classes with proper relationships

---

## Phase 3: Functional RAG — Ingestion, Retrieval, Citations 🔄 IN PROGRESS

### 3.1 Document Ingestion Service
- [ ] DocumentIngestionService interface
- [ ] PDF text extraction (using Apache PDFBox)
- [ ] HTML extraction (using Jsoup)
- [ ] Text normalization and cleaning
- [ ] Section and page detection
- [ ] Content hashing for duplicate detection
- [ ] Revision comparison and fingerprinting

### 3.2 Document Chunking
- [ ] ChunkingService interface
- [ ] Configurable chunk size and overlap
- [ ] Semantic section detection
- [ ] Preserve page and section metadata
- [ ] Token counting

### 3.3 Embedding Generation
- [ ] EmbeddingService interface
- [ ] Ollama integration for local embeddings
- [ ] Optional OpenAI fallback
- [ ] Batch embedding support
- [ ] Vector storage in pgvector

### 3.4 Retrieval Pipeline
- [ ] **Dense Retrieval:** pgvector similarity search
- [ ] **Lexical Retrieval:** PostgreSQL full-text search
- [ ] **Hybrid Ranking:** Reciprocal Rank Fusion
- [ ] RetrievalService interface with pluggable strategies
- [ ] Result deduplication and normalization
- [ ] Configurable top-k and similarity thresholds

### 3.5 Evidence Verification
- [ ] Citation mapping to actual chunks
- [ ] Page and section reference validation
- [ ] Duplicate evidence detection
- [ ] Basic evidence sufficiency checks

### 3.6 UI & REST API
- [ ] /api/documents (list, search, filter)
- [ ] /api/documents/{id} (detail view)
- [ ] /api/ingestion/upload (PDF upload endpoint)
- [ ] /api/ingestion/jobs/{id} (status tracking)
- [ ] /api/rag/ask (query with retrieval)
- [ ] /api/conversations (chat history)
- [ ] Dashboard JSP page
- [ ] Research Assistant JSP page
- [ ] Document Library JSP page
- [ ] Ingestion Centre JSP page

### 3.7 Testing
- [ ] Unit tests for extraction
- [ ] Unit tests for chunking
- [ ] Unit tests for embedding (mocked)
- [ ] Unit tests for retrieval ranking
- [ ] Integration tests for ingestion
- [ ] Integration tests for retrieval

**Target:** Complete Phase 3 with functional RAG pipeline

---

## Phase 4: Revision-Aware Evidence Routing 📋 PENDING

### 4.1 Revision Management
- [ ] Revision fingerprinting (deterministic comparison)
- [ ] Revision diff implementation
- [ ] Detect added/removed/modified passages
- [ ] Track requirement changes
- [ ] Maintain revision history and applicability

### 4.2 Technical Evidence Graph
- [ ] Graph schema (relations between documents, requirements, entities)
- [ ] Store explicit relationships (REFERENCES, DESCRIBES, DEPENDS_ON, SUPERSEDES)
- [ ] Extract requirement identifiers
- [ ] Extract technical entity references
- [ ] Distinguish explicit from inferred relationships

### 4.3 Query-Conditioned Retrieval Planning (CORE RESEARCH CONTRIBUTION)
- [ ] EvidenceRoutingService interface
- [ ] Query intent analysis
- [ ] Revision applicability checking
- [ ] Retrieval plan selection logic
- [ ] Support for 7 retrieval strategies
- [ ] Deterministic routing decisions (same input → same plan)
- [ ] Record routing metadata and decisions

### 4.4 Dependency-Guided Evidence Expansion
- [ ] Graph traversal with depth limits
- [ ] Expand through explicit dependencies
- [ ] Apply result count limits
- [ ] Maintain expansion reasoning
- [ ] Avoid uncontrolled graph expansion

### 4.5 Evidence Convergence & Conflict Detection
- [ ] Combine retrieved candidates
- [ ] Evaluate revision applicability
- [ ] Detect contradicting statements
- [ ] Preserve conflict provenance
- [ ] Transparent conflict reporting

### 4.6 Evidence-Gated Generation
- [ ] Generate answers from evidence only
- [ ] Explicit evidence references with page numbers
- [ ] Acknowledge contradictions
- [ ] State insufficient evidence
- [ ] Distinguish facts from inferences

---

## Phase 5: Impact Mapping & Document Relationships 📋 PENDING

- [ ] Impact analysis trigger (on revision upload)
- [ ] Changed section identification
- [ ] Find explicit references to changed content
- [ ] Traverse dependency graph for related documents
- [ ] Rank impact candidates
- [ ] ImpactAnalysisService implementation
- [ ] Graph visualization (using D3.js or Cytoscape)
- [ ] Impact Mapper JSP page
- [ ] Reviewer feedback collection
- [ ] Impact persistence and history

---

## Phase 6: Evaluation Framework & Ablation Experiments 📋 PENDING

- [ ] Create evaluation dataset with test questions
- [ ] Implement baseline retrieval (dense only, lexical only)
- [ ] Implement conventional hybrid retrieval
- [ ] Implement revision-filtered retrieval
- [ ] Implement full routing controller pipeline
- [ ] Measure: Recall@k, Precision@k, MRR
- [ ] Measure: Citation validity, unsupported claims
- [ ] Run ablation experiments (disable each component)
- [ ] Evaluation Lab JSP page
- [ ] Results export (CSV, JSON)
- [ ] Record configuration and algorithm versions

---

## Phase 7: Security, Testing, Build Verification 📋 PENDING

- [ ] CSRF protection validation
- [ ] Secure session configuration
- [ ] Upload validation and SSRF protection
- [ ] Parameterized query verification
- [ ] Rate limiting (if appropriate)
- [ ] Error response safety
- [ ] Run full unit test suite
- [ ] Run full integration test suite
- [ ] Run Maven build (clean install)
- [ ] Fix any failing tests or build errors
- [ ] Verify no secrets in codebase

---

## Phase 8: Final Verification & Documentation 📋 PENDING

- [ ] Start Docker services (PostgreSQL, Ollama)
- [ ] Launch Spring Boot application
- [ ] Verify JSP pages render correctly
- [ ] Confirm database connections
- [ ] Ingest sample ISRO document (PDF)
- [ ] Ask technical question and inspect retrieval
- [ ] Compare two document revisions
- [ ] Run impact analysis
- [ ] Execute evaluation experiment
- [ ] Verify error states and edge cases
- [ ] Update README with actual implementation details
- [ ] Create architecture diagram (Mermaid)
- [ ] Generate final implementation report
- [ ] Document any limitations and future work

---

## Summary Statistics

| Phase | Status | Components | Tests | Docs |
|-------|--------|-----------|-------|------|
| Phase 1 | ✅ DONE | Research, Hypothesis, Prior Art | — | 3 |
| Phase 2 | ✅ DONE | Spring Boot, DB, Security, Auth | Build ✅ | 5 |
| Phase 3 | 🔄 IN PROGRESS | Ingestion, Retrieval, RAG | TBD | TBD |
| Phase 4 | 📋 PENDING | Evidence Routing, Graph, Gating | TBD | TBD |
| Phase 5 | 📋 PENDING | Impact Analysis, Visualization | TBD | TBD |
| Phase 6 | 📋 PENDING | Evaluation, Ablation, Metrics | TBD | TBD |
| Phase 7 | 📋 PENDING | Security, Testing, Build | TBD | TBD |
| Phase 8 | 📋 PENDING | Integration, Deployment, Docs | TBD | TBD |

---

## Key Architecture Files Created

```
spacedoc-intelligence/
├── pom.xml                                      # Maven configuration (Spring Boot 3.3)
├── docker-compose.yml                           # PostgreSQL + Ollama
├── .env.example                                 # Configuration template
├── README.md                                    # Project overview
├── src/main/resources/
│   ├── application.properties                   # Spring configuration
│   └── db/migration/
│       └── V1__initial_schema.sql               # Database schema (17 tables)
├── src/main/java/com/spacedoc/
│   ├── SpaceDocIntelligenceApplication.java     # Main class
│   ├── config/
│   │   └── SecurityConfiguration.java           # Spring Security config
│   ├── controller/
│   │   └── PageController.java                  # JSP routing
│   ├── rest/
│   │   └── SystemHealthController.java          # Health check API
│   ├── entity/                                  # 9 JPA entities
│   ├── repository/                              # 4 Spring Data repositories
│   ├── dto/                                     # Data transfer objects
│   ├── exception/                               # Custom exceptions
│   ├── service/                                 # Business logic (TBD)
│   └── util/                                    # Utilities (TBD)
├── src/main/webapp/WEB-INF/views/
│   └── login.jsp                                # Login page (styled)
└── docs/invention-research/
    ├── invention-hypothesis.md                  # Research hypothesis
    └── prior-art-matrix.md                      # Prior art analysis
```

---

## Next Steps (Phase 3 Immediate)

1. **Implement DocumentIngestionService**
   - PDF extraction using PdfBox
   - Text normalization
   - Content hashing
   - Persist to database

2. **Implement ChunkingService**
   - Configurable segmentation
   - Preserve metadata
   - Page/section mapping

3. **Implement EmbeddingService**
   - Ollama HTTP client
   - Batch processing
   - Vector storage

4. **Implement RetrievalService**
   - Dense + Lexical combined
   - RRF ranking
   - Deduplication

5. **Create REST endpoints** for ingestion and retrieval

6. **Build UI pages** for document management and Q&A

---

## Build Commands

```bash
# Clean build
mvn clean install -DskipTests

# Run tests
mvn test

# Run application
mvn spring-boot:run

# Docker startup
docker-compose up -d

# Apply migrations
# (Automatically applied on startup via Flyway)
```

---

**Last Updated:** 2026-10-20 12:20 UTC  
**Status:** Phase 2 Complete, Phase 3 In Progress
