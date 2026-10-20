# SpaceDoc Intelligence

A research-driven RAG (Retrieval-Augmented Generation) platform for ISRO-related technical and scientific documents, with a focus on **revision-aware evidence routing and consistency control**.

## Project Vision

SpaceDoc Intelligence enables researchers and engineers to:

1. Search and explore official space research documents with evidence-backed retrieval
2. Ask technical questions and receive answers grounded in indexed documents
3. Inspect citations, page numbers, excerpts, and source provenance
4. Compare multiple document revisions and track changes
5. Discover contradictions between technical statements
6. Analyze impact when documents change
7. Explore relationships between missions, requirements, and documents
8. Evaluate retrieval quality and answer grounding
9. Inspect how the RAG pipeline selected evidence
10. Reproduce and compare retrieval experiments

## Research Contribution Hypothesis

**Revision-Aware Evidence Routing and Consistency Control for Technical Document RAG**

This investigates whether a query-conditioned, revision-aware evidence-routing controller can improve evidence selection and answer reliability by:

- Combining query intent, document revision metadata, and dependency relationships
- Detecting and managing conflicting evidence
- Applying revision applicability checks
- Expanding retrieval through traceable document dependencies
- Implementing evidence-gated generation with explicit uncertainty handling

## Technology Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Java 21 LTS |
| **Framework** | Spring Boot 3.x |
| **Frontend** | JSP, HTML5, CSS3, Vanilla JavaScript |
| **Database** | PostgreSQL 16+ with pgvector |
| **Build** | Maven |
| **Testing** | JUnit 5, Mockito |
| **AI Runtime** | Ollama (local) or compatible hosted provider |
| **Deployment** | Docker Compose, WAR on Tomcat |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (JSP/HTML/JS)                    │
├─────────────────────────────────────────────────────────────┤
│  Dashboard │ Research Assistant │ Library │ Ingestion │
│  Impact Mapper │ Evaluation Lab │ Document Detail           │
├─────────────────────────────────────────────────────────────┤
│              REST API Controllers & Security               │
├──────────────┬──────────────────────────────────┬───────────┤
│ Ingestion    │ RAG Pipeline                     │ Analysis  │
│ Service      ├──────────────────────────────────┤ Service   │
│              │ Evidence Routing (Core Research) │           │
│ • Parsing    │ • Query Planning                 │ • Impact  │
│ • Extraction │ • Dense Retrieval                │ • Graph   │
│ • Chunking   │ • Lexical Retrieval              │ • Impact  │
│ • Embedding  │ • Hybrid Ranking                 │           │
│              │ • Dependency Expansion           │           │
│              │ • Conflict Detection             │           │
│              │ • Evidence Convergence           │           │
│              │ • Evidence-Gated Generation      │           │
│              │ • Citation Verification          │           │
├──────────────┴──────────────────────────────────┴───────────┤
│ Repository Layer (Spring Data JPA, JDBC)                    │
├──────────────────────────────────────────────────────────────┤
│ PostgreSQL (pgvector, Full-Text Search, Migrations)         │
└──────────────────────────────────────────────────────────────┘
```

## Package Structure

```
com.spacedoc
├── config/               # Spring configuration, security
├── controller/           # MVC controllers for JSP views
├── rest/                 # REST API endpoints
├── service/              # Business logic orchestration
├── repository/           # Spring Data JPA & JDBC
├── entity/               # JPA entities
├── dto/                  # Data transfer objects
├── security/             # Authentication, authorization
├── ingestion/            # Document intake pipeline
├── parsing/              # Text extraction and normalization
├── chunking/             # Document segmentation
├── embedding/            # Embedding generation
├── retrieval/            # Dense, lexical, hybrid search
├── reranking/            # Optional reranking layer
├── routing/              # Evidence routing controller (RESEARCH)
├── generation/           # Answer generation with gating
├── verification/         # Citation and evidence validation
├── citation/             # Citation mapping and resolution
├── impact/               # Impact analysis and document relationships
├── evaluation/           # Evaluation framework and metrics
├── graph/                # Technical evidence graph operations
├── exception/            # Custom exceptions
└── util/                 # Utilities
```

## Getting Started

### Prerequisites

- Java 21 LTS
- Maven 3.8+
- PostgreSQL 16+ with pgvector extension
- Docker and Docker Compose (optional, for containerization)
- Ollama (optional, for local model runtime)

### Quick Start

1. **Clone and navigate:**
   ```bash
   cd /path/to/spacedoc
   ```

2. **Configure environment:**
   ```bash
   cp .env.example .env
   # Edit .env with your local PostgreSQL and Ollama URLs
   ```

3. **Start services:**
   ```bash
   docker-compose up -d
   ```

4. **Build and run:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access application:**
   ```
   http://localhost:8080
   ```

## Core Pipeline: Revision-Aware Evidence Routing

### Stage 1: Provenance-Aware Document Ingestion
- Accept PDFs, URLs, and metadata
- Extract and normalize text
- Preserve page and section references
- Generate stable document and revision IDs
- Store content hashes for change detection

### Stage 2: Revision and Evidence Fingerprinting
- Compare revisions using deterministic text comparison
- Detect added, removed, and modified passages
- Extract requirement and entity references
- Maintain historical evidence

### Stage 3: Technical Evidence Graph
- Store documents, revisions, sections, requirements, and entities
- Track explicit relationships (REFERENCES, DESCRIBES, DEPENDS_ON, SUPERSEDES, CONTRADICTS)
- Distinguish explicit from inferred relationships
- Maintain relationship provenance

### Stage 4: Query-Conditioned Retrieval Planning
- Analyze query intent and required revision
- Choose appropriate retrieval plan (dense, lexical, hybrid, dependency-guided)
- Record routing decisions and reasons
- Avoid unnecessary retrieval stages

### Stage 5: Hybrid Retrieval
- Vector search with pgvector
- Lexical search with PostgreSQL full-text
- Combine rankings using Reciprocal Rank Fusion
- Support configurable filters and thresholds

### Stage 6: Dependency-Guided Evidence Expansion
- Traverse technical evidence graph
- Apply traversal depth and result limits
- Prefer explicit dependencies over inferred
- Maintain expansion reasons

### Stage 7: Evidence Convergence and Conflict Analysis
- Combine retrieved candidates
- Evaluate relevance, provenance, and applicability
- Detect conflicting statements
- Preserve both statements and provenance

### Stage 8: Evidence-Gated Generation
- Generate answers only from selected evidence
- Use explicit evidence references
- Avoid invented specifications
- Identify contradictions
- State when information is missing

### Stage 9: Feedback and Policy Refinement
- Collect authorized reviewer feedback
- Create evaluation datasets
- Optionally tune retrieval parameters
- Require regression testing before deployment

## Document Impact Analysis

When a document revision changes:

1. Identify changed sections, requirements, and entities
2. Find documents that explicitly reference the changed content
3. Find traceable related evidence through dependency graph
4. Apply revision applicability checks
5. Rank candidate impacted documents with explanations
6. Allow reviewer to confirm, reject, or investigate

## User Interface

### Pages

| Page | Purpose |
|------|---------|
| **Dashboard** | Metrics, recent activity, system health |
| **Research Assistant** | Conversational Q&A with evidence inspection |
| **Document Library** | Search, filter, pagination, metadata, revisions |
| **Ingestion Centre** | Upload, URL submission, job status, duplicates |
| **Impact Mapper** | Revision comparison, relationships, impact analysis |
| **Evaluation Lab** | Experiment history, metrics, ablation results |
| **Document Detail** | Metadata, chunks, provenance, relationships |

### Design Aesthetic

- Dark navy and neutral surfaces
- Readable typography with accessible contrast
- Responsive layouts
- Clean data visualization
- Scientific, restrained design

## REST API

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/dashboard` | GET | Metrics and recent activity |
| `/api/documents` | GET | Document list |
| `/api/documents/{id}` | GET | Document detail |
| `/api/documents/{id}/revisions` | GET | Revision history |
| `/api/ingestion/upload` | POST | PDF upload |
| `/api/ingestion/url` | POST | URL submission |
| `/api/ingestion/jobs/{id}` | GET | Job status |
| `/api/rag/ask` | POST | Query with evidence routing |
| `/api/conversations` | GET | Conversation list |
| `/api/conversations/{id}/messages` | GET | Message history |
| `/api/impact/analyze` | POST | Impact analysis |
| `/api/impact/{id}` | GET | Impact result |
| `/api/documents/{id}/relationships` | GET | Document graph |
| `/api/evaluation/run` | POST | Start evaluation |
| `/api/evaluation/runs` | GET | Evaluation history |
| `/api/system/health` | GET | System status |

## Security

- Authentication and authorization
- Restricted ingestion and review operations
- CSRF protection
- Secure session configuration
- Upload validation and SSRF protection
- Parameterized queries
- Safe error responses
- Secrets via environment variables
- Protection against prompt injection

## Testing

- Unit tests for extraction, chunking, retrieval, ranking, routing, verification
- Integration tests for database, ingestion, retrieval, citation, impact analysis
- No paid API calls required
- Deterministic fixtures and mocked models

## Research Documentation

All research-related documentation resides in `docs/invention-research/`:

- `invention-hypothesis.md` — The proposed mechanism
- `technical-problem-and-solution.md` — Problem formulation and solution overview
- `pipeline-design.md` — Detailed pipeline design
- `algorithm-specification.md` — Algorithm inputs, outputs, decision rules
- `data-model-and-state-transitions.md` — Data structures and state machines
- `prior-art-matrix.md` — Published prior art analysis
- `experiment-protocol.md` — Evaluation framework and baselines
- `experiment-results.md` — Actual measured results
- `ablation-analysis.md` — Component impact analysis
- `implementation-limitations.md` — Known constraints and future work
- `dated-development-log.md` — Timestamped design decisions and experiments

## Build and Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Test
mvn test

# Package
mvn package
```

## Configuration

See `.env.example` for all configuration options:

- Database URL and credentials
- pgvector settings
- Ollama base URL and model names
- Chunk size and overlap
- Retrieval parameters
- Ingestion and timeout limits
- Logging levels

## License

This project is provided as-is for research and demonstration purposes.

## Contributors

- Development started: 2026-10-20
- Architecture and implementation by Claude Code

---

**Status:** Under active development. This README reflects the planned architecture. See implementation reports for actual completed features.
