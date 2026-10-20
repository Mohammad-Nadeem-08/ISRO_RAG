# SpaceDoc Intelligence - Deployment Guide

**Build Date:** 2026-10-20  
**Version:** 1.0.0-SNAPSHOT  
**Status:** ✅ Build Complete, 38/38 Unit Tests Passing

---

## Quick Start

### Prerequisites
- **Java 21** (OpenJDK or Oracle)
- **Maven 3.8+**
- **PostgreSQL 14+** with pgvector extension
- **Ollama** (for embeddings)
- **Docker** (optional, for containerized deployment)

### 1. Build from Source

```bash
cd C:\Users\ELCOT\Desktop\Advance_JAVA\ISRO

# Build (unit tests included)
mvn clean package

# Output: target/spacedoc-intelligence-1.0.0-SNAPSHOT.war (72 MB)
```

**Build Statistics:**
- Compilation: ~5 seconds
- Tests (38 unit tests): ~7 seconds  
- WAR packaging: ~3 seconds
- **Total: ~25 seconds**

### 2. Database Setup

```bash
# Create PostgreSQL database
createdb -U postgres spacedoc

# Connect to database
psql -U postgres -d spacedoc

# Install pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

# Flyway migrations run automatically on app startup
```

### 3. Configuration

Create `.env` in project root (or set environment variables):

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/spacedoc
DATABASE_USERNAME=spacedoc
DATABASE_PASSWORD=spacedoc_local_dev_password

# Ollama (embeddings)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
OLLAMA_CHAT_MODEL=mistral

# Security (local dev defaults)
SECURITY_USER_NAME=admin
SECURITY_USER_PASSWORD=admin_local_dev_password
```

Alternative: Edit `src/main/resources/application.properties`

### 4. Deploy WAR

**Option A: Embedded Tomcat (Recommended for dev)**
```bash
java -jar target/spacedoc-intelligence-1.0.0-SNAPSHOT.war
# Runs on http://localhost:8080
```

**Option B: External Tomcat**
```bash
# Copy WAR to Tomcat webapps
cp target/spacedoc-intelligence-1.0.0-SNAPSHOT.war /path/to/tomcat/webapps/

# Start Tomcat
/path/to/tomcat/bin/startup.sh
# Access on http://localhost:8080/spacedoc-intelligence-1.0.0-SNAPSHOT
```

**Option C: Docker**
```bash
docker build -t spacedoc:latest .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/spacedoc \
  -e DATABASE_USERNAME=spacedoc \
  -e DATABASE_PASSWORD=spacedoc_local_dev_password \
  -e OLLAMA_BASE_URL=http://ollama:11434 \
  spacedoc:latest
```

---

## REST API Reference

### Authentication
Default credentials (local dev):
- Username: `admin`
- Password: `admin_local_dev_password`

All REST endpoints are secured with Spring Security. Include credentials in requests or use session cookies.

### Document Management

**List documents**
```
GET /api/documents?page=0&size=20
Response: {
  "content": [DocumentDTO, ...],
  "totalElements": int,
  "totalPages": int
}
```

**Get document by ID**
```
GET /api/documents/{id}
Response: DocumentDTO
```

**Search documents**
```
GET /api/documents/search?q=query&page=0&size=20
Response: [DocumentDTO, ...]
```

**Get document revisions**
```
GET /api/documents/{id}/revisions
Response: [DocumentRevisionDTO, ...]
```

### Document Ingestion

**Upload PDF**
```
POST /api/ingestion/upload
Content-Type: multipart/form-data
Body:
  file: <PDF file>
  documentKey: "DOC-001"  (optional)
  description: "Document description"  (optional)
Response: IngestionResponseDTO { jobKey, status, message }
```

**Ingest from URL**
```
POST /api/ingestion/url
Content-Type: application/json
Body: {
  "url": "https://example.com/document.pdf",
  "documentKey": "DOC-002",
  "description": "External document"
}
Response: IngestionResponseDTO
```

**Check ingestion job status**
```
GET /api/ingestion/jobs/{jobKey}
Response: IngestionJobDTO { 
  key, status, progress, createdAt, completedAt, error 
}
```

### Retrieval & RAG

**Hybrid retrieval (lexical + dense)**
```
POST /api/rag/retrieve
Content-Type: application/json
Body: {
  "query": "satellite propulsion specifications",
  "topK": 10
}
Response: [RetrievalResultDTO, ...]
```

**Lexical (full-text) retrieval only**
```
POST /api/rag/retrieve/lexical
Content-Type: application/json
Body: { "query": "...", "topK": 10 }
Response: [RetrievalResultDTO, ...]
```

**Dense (vector similarity) retrieval only**
```
POST /api/rag/retrieve/dense
Content-Type: application/json
Body: { "query": "...", "topK": 10 }
Response: [RetrievalResultDTO, ...]
```

**RAG query with evidence routing**
```
POST /api/rag/ask
Content-Type: application/json
Body: {
  "query": "What are ISRO's satellite specifications?",
  "topK": 5,
  "useRevisionRouting": true,
  "useConflictDetection": true
}
Response: {
  "answer": "Generated answer with citations...",
  "evidence": [RetrievalResultDTO, ...],
  "citationMap": { "citation_id": "source_chunk_id" },
  "conflictDetected": false,
  "routingApplied": true
}
```

### Impact Analysis

**Analyze impact of document change**
```
POST /api/impact/analyze
Content-Type: application/json
Body: {
  "documentId": 1,
  "revisionKey": "rev-2024-10-20",
  "changeDescription": "Updated satellite propulsion requirements"
}
Response: ImpactAnalysisDTO {
  sourceDocumentId, sourceDocumentTitle,
  totalDirectImpacts, totalIndirectImpacts,
  directlyImpactedDocuments: [{ docId, docTitle, relationshipType, impactReason }],
  indirectlyImpactedDocuments: [...],
  semanticSimilarityCandidates: [...]
}
```

**Get impact analysis result**
```
GET /api/impact/{analysisId}
Response: ImpactAnalysisDTO
```

### System Health

**Health check**
```
GET /api/system/health
Response: { status: "UP", timestamp: "2026-10-20T..." }
```

---

## Web UI Pages

### Entry Points

| URL | Purpose | Features |
|-----|---------|----------|
| `/login` | Authentication | Username/password login |
| `/dashboard` | Main dashboard | Metrics, recent uploads, system status |
| `/research-assistant` | Chat interface | Query the RAG system, get evidence-backed answers |
| `/document-library` | Document browser | Search, filter, view documents and revisions |
| `/ingestion-centre` | Document upload | Upload PDFs, submit URLs, monitor jobs |
| `/document-detail` | Document view | View chunks, revisions, relationships, impact |

### Key Features

**Research Assistant Page:**
- Chat-like interface for querying the RAG system
- Displays retrieved evidence documents
- Shows revision applicability for evidence
- Conflict detection warnings
- Citation tracking

**Document Library:**
- Full-text search with faceted filtering
- Document preview and metadata
- Revision history timeline
- Impact analysis visualization
- Relationship graph (dependency map)

**Ingestion Centre:**
- Drag-and-drop PDF upload
- URL submission form
- Real-time job progress tracking
- Error handling and retry

---

## Testing

### Run Unit Tests (38 tests)
```bash
mvn clean test
# Tests: AnswerGenerationServiceTest (18)
#        RetrievalServiceTest (14)
#        ImpactAnalysisServiceUnitTest (6)
```

### Run Integration Tests (requires PostgreSQL + Ollama)
```bash
# Start Docker services first
docker-compose up -d

# Run all tests including integration
mvn clean verify -Dgroups=""
```

### Test Coverage
- **Service Layer:** AnswerGenerationService, RetrievalService, ImpactAnalysisService
- **DTOs:** Input validation, field mapping
- **Retrieval Pipelines:** Lexical, dense, hybrid ranking with RRF

---

## Architecture Overview

### Core Services (11 total)

| Service | Purpose |
|---------|---------|
| **DocumentIngestionService** | Parse PDFs, manage versions, orchestrate pipeline |
| **TextExtractionService** | Extract text from PDFs and URLs |
| **ChunkingService** | Semantic chunking with configurable strategies |
| **EmbeddingService** | Generate embeddings via Ollama with fallback |
| **RetrievalService** | Hybrid retrieval (lexical + dense), RRF ranking |
| **RevisionComparisonService** | Detect changes between document versions |
| **ConflictDetectionService** | Identify contradictions in evidence |
| **EvidenceRoutingService** | Route queries based on revision applicability |
| **AnswerGenerationService** | Generate RAG answers with citations |
| **DependencyGraphService** | Build document dependency graph |
| **ImpactAnalysisService** | Analyze impact of document changes |

### Data Model (9 entities)

```
Document
├─ DocumentRevision (version history)
├─ DocumentChunk (semantic chunks)
├─ DocumentRelationship (explicit dependencies)
└─ IngestionJob (ingestion pipeline tracking)

Requirement (extracted requirements)
TechnicalEntity (extracted entities)
Conversation (chat history)
└─ ChatMessage (individual messages)
```

### REST Controllers (5 total)

- **IngestionController**: Document ingestion workflow
- **RetrievalController**: Query and RAG endpoints
- **DocumentController**: Document CRUD and search
- **ImpactAnalysisController**: Impact analysis endpoints
- **SystemHealthController**: System status monitoring

---

## Performance Baselines

**Current Metrics (estimated):**
- Document ingestion: ~500 docs/minute (CPU-limited)
- Embedding generation: ~10 chunks/second (Ollama network)
- Retrieval latency: <500ms for hybrid search
- Evidence routing: <100ms for 100 results
- Answer generation: <1 second per query

**Database:**
- PostgreSQL with pgvector indexes on embeddings
- Flyway-managed migrations
- Connection pooling (HikariCP)

---

## Troubleshooting

### PostgreSQL Connection Fails
```
Error: Connection to localhost:5432 refused
Solution:
1. Verify PostgreSQL is running: psql -U postgres -d spacedoc
2. Check DATABASE_URL in application.properties
3. Verify pgvector extension: \dx in psql
```

### Ollama Connection Fails
```
Error: Cannot reach http://localhost:11434
Solution:
1. Start Ollama: ollama serve
2. Verify model: ollama list
3. Check OLLAMA_BASE_URL in application.properties
```

### Test Failures
```
Error: "ApplicationContext failure threshold exceeded"
Solution:
1. Unit tests skip integration tests automatically (@Tag("integration"))
2. For integration tests, start Docker: docker-compose up -d
3. Run: mvn verify -Dgroups=""
```

### High Memory Usage
```
War runs in ~1.5GB with Ollama embeddings
Adjust: java -Xmx2g -jar spacedoc-intelligence-1.0.0-SNAPSHOT.war
```

---

## Files & Structure

```
Project Root
├── target/spacedoc-intelligence-1.0.0-SNAPSHOT.war (deployable artifact)
├── pom.xml (Maven build configuration)
├── docker-compose.yml (PostgreSQL + pgvector + Ollama)
├── .env.example (configuration template)
├── src/main/java/com/spacedoc/
│   ├── service/ (11 business logic services)
│   ├── rest/ (5 REST controllers)
│   ├── entity/ (9 JPA entities)
│   ├── repository/ (Spring Data repositories)
│   ├── dto/ (Data transfer objects)
│   ├── config/ (Spring Security, app config)
│   └── controller/ (MVC page controller)
├── src/main/webapp/WEB-INF/views/
│   ├── login.jsp
│   ├── layout.jsp (main template)
│   ├── dashboard-content.jsp
│   ├── research-assistant-content.jsp
│   ├── document-library-content.jsp
│   ├── ingestion-centre-content.jsp
│   ├── document-detail-content.jsp
│   └── (2013 lines total JSP code)
├── src/main/resources/
│   ├── application.properties (main config)
│   └── db/migration/ (Flyway migrations)
├── src/test/java/com/spacedoc/service/
│   ├── AnswerGenerationServiceTest (18 tests)
│   ├── RetrievalServiceTest (14 tests)
│   └── ImpactAnalysisServiceUnitTest (6 tests)
└── docs/
    ├── invention-hypothesis.md (research foundation)
    └── prior-art-matrix.md (literature review)
```

---

## Build & Deployment Checklist

- [x] Java 21 compilation succeeds
- [x] All 38 unit tests pass
- [x] WAR packaging succeeds (72 MB)
- [x] Integration test framework configured
- [x] All 5 REST controllers wired
- [x] All 6 JSP pages created
- [x] Database migrations defined
- [x] Security configuration enabled
- [x] Error handling implemented
- [x] Logging configured

---

## Next Steps for Full Deployment

1. **Database:** Provision PostgreSQL instance with pgvector
2. **Ollama:** Deploy Ollama service or use cloud API
3. **Configuration:** Update `.env` with production credentials
4. **Secrets:** Store DATABASE_PASSWORD, API keys in secure vault
5. **SSL/TLS:** Enable HTTPS with certificate
6. **Monitoring:** Set up logging aggregation (ELK, Datadog, etc.)
7. **Scaling:** Consider load balancer for multiple WAR instances
8. **Backup:** Set up PostgreSQL backup strategy

---

**Documentation Last Updated:** 2026-10-20T17:24 UTC  
**Status:** Ready for deployment and production evaluation
