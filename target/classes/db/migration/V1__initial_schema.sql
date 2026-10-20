-- Flyway Migration V1: Initial Schema
-- Created: 2026-10-20
-- Purpose: Create core tables for SpaceDoc Intelligence

-- Documents table
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    document_key VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    source_url VARCHAR(2048),
    source_type VARCHAR(50),
    source_organization VARCHAR(255),
    publication_date TIMESTAMP,
    content_hash VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_key_unique UNIQUE(document_key)
);

CREATE INDEX idx_documents_source_org ON documents(source_organization);
CREATE INDEX idx_documents_publication_date ON documents(publication_date);

-- Document revisions table
CREATE TABLE IF NOT EXISTS document_revisions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    revision_number INT NOT NULL,
    revision_key VARCHAR(255) NOT NULL UNIQUE,
    content_hash VARCHAR(64) NOT NULL,
    change_summary TEXT,
    prior_revision_id BIGINT,
    total_chunks INT DEFAULT 0,
    extracted_text_length BIGINT,
    is_current BOOLEAN DEFAULT FALSE,
    ingested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_revisions_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_revisions_prior
        FOREIGN KEY (prior_revision_id) REFERENCES document_revisions(id) ON DELETE SET NULL,
    CONSTRAINT unique_doc_revision UNIQUE(document_id, revision_number)
);

CREATE INDEX idx_document_revisions_document ON document_revisions(document_id);
CREATE INDEX idx_document_revisions_is_current ON document_revisions(is_current);
CREATE INDEX idx_document_revisions_ingested_at ON document_revisions(ingested_at);

-- Document chunks table (with pgvector embeddings)
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_revision_id BIGINT NOT NULL,
    chunk_number INT NOT NULL,
    content TEXT NOT NULL,
    section_title VARCHAR(500),
    page_number INT,
    section_number VARCHAR(50),
    start_offset BIGINT,
    end_offset BIGINT,
    embedding vector(768),
    token_count INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chunks_revision
        FOREIGN KEY (document_revision_id) REFERENCES document_revisions(id) ON DELETE CASCADE,
    CONSTRAINT unique_chunk_per_revision UNIQUE(document_revision_id, chunk_number)
);

CREATE INDEX idx_chunks_revision ON document_chunks(document_revision_id);
CREATE INDEX idx_chunks_page ON document_chunks(page_number);
CREATE INDEX idx_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops);

-- Ingestion jobs table
CREATE TABLE IF NOT EXISTS ingestion_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_key VARCHAR(100) NOT NULL UNIQUE,
    document_id BIGINT,
    job_type VARCHAR(50),
    source_url VARCHAR(2048),
    source_filename VARCHAR(500),
    status VARCHAR(50) DEFAULT 'PENDING',
    progress_percent INT DEFAULT 0,
    error_message TEXT,
    processed_bytes BIGINT,
    total_bytes BIGINT,
    chunks_created INT DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ingestion_jobs_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL
);

CREATE INDEX idx_ingestion_jobs_status ON ingestion_jobs(status);
CREATE INDEX idx_ingestion_jobs_document ON ingestion_jobs(document_id);
CREATE INDEX idx_ingestion_jobs_created_at ON ingestion_jobs(created_at);

-- Requirements table
CREATE TABLE IF NOT EXISTS requirements (
    id BIGSERIAL PRIMARY KEY,
    requirement_key VARCHAR(255) NOT NULL,
    document_revision_id BIGINT NOT NULL,
    requirement_text TEXT NOT NULL,
    requirement_type VARCHAR(100),
    is_superseded BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_requirements_revision
        FOREIGN KEY (document_revision_id) REFERENCES document_revisions(id) ON DELETE CASCADE,
    CONSTRAINT unique_requirement_per_revision UNIQUE(requirement_key, document_revision_id)
);

CREATE INDEX idx_requirements_revision ON requirements(document_revision_id);
CREATE INDEX idx_requirements_key ON requirements(requirement_key);

-- Technical entities table
CREATE TABLE IF NOT EXISTS technical_entities (
    id BIGSERIAL PRIMARY KEY,
    entity_key VARCHAR(255) NOT NULL UNIQUE,
    entity_type VARCHAR(100),
    entity_name VARCHAR(500),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_technical_entities_type ON technical_entities(entity_type);
CREATE INDEX idx_technical_entities_key ON technical_entities(entity_key);

-- Document relationships table (evidence graph)
CREATE TABLE IF NOT EXISTS document_relationships (
    id BIGSERIAL PRIMARY KEY,
    source_document_revision_id BIGINT NOT NULL,
    target_document_revision_id BIGINT,
    relationship_type VARCHAR(100),
    evidence_chunk_id BIGINT,
    is_explicit BOOLEAN DEFAULT TRUE,
    supporting_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rel_source
        FOREIGN KEY (source_document_revision_id) REFERENCES document_revisions(id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_target
        FOREIGN KEY (target_document_revision_id) REFERENCES document_revisions(id) ON DELETE SET NULL,
    CONSTRAINT fk_rel_chunk
        FOREIGN KEY (evidence_chunk_id) REFERENCES document_chunks(id) ON DELETE SET NULL
);

CREATE INDEX idx_rel_source ON document_relationships(source_document_revision_id);
CREATE INDEX idx_rel_target ON document_relationships(target_document_revision_id);
CREATE INDEX idx_rel_type ON document_relationships(relationship_type);

-- Conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id BIGSERIAL PRIMARY KEY,
    conversation_key VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversations_created_at ON conversations(created_at);

-- Chat messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    message_number INT NOT NULL,
    role VARCHAR(50),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT unique_message_number UNIQUE(conversation_id, message_number)
);

CREATE INDEX idx_messages_conversation ON chat_messages(conversation_id);

-- Retrieved evidence table
CREATE TABLE IF NOT EXISTS retrieved_evidence (
    id BIGSERIAL PRIMARY KEY,
    chat_message_id BIGINT,
    document_chunk_id BIGINT NOT NULL,
    rank_position INT,
    relevance_score FLOAT,
    revision_applicability VARCHAR(50),
    conflict_status VARCHAR(50),
    retrieved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evidence_message
        FOREIGN KEY (chat_message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_evidence_chunk
        FOREIGN KEY (document_chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE
);

CREATE INDEX idx_evidence_message ON retrieved_evidence(chat_message_id);
CREATE INDEX idx_evidence_chunk ON retrieved_evidence(document_chunk_id);

-- Evidence verification table
CREATE TABLE IF NOT EXISTS evidence_verification (
    id BIGSERIAL PRIMARY KEY,
    retrieved_evidence_id BIGINT NOT NULL,
    verification_check VARCHAR(100),
    check_result VARCHAR(50),
    check_detail TEXT,
    verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_evidence
        FOREIGN KEY (retrieved_evidence_id) REFERENCES retrieved_evidence(id) ON DELETE CASCADE
);

CREATE INDEX idx_verification_evidence ON evidence_verification(retrieved_evidence_id);

-- Impact analysis table
CREATE TABLE IF NOT EXISTS impact_analyses (
    id BIGSERIAL PRIMARY KEY,
    source_revision_id BIGINT NOT NULL,
    target_revision_id BIGINT,
    analysis_status VARCHAR(50),
    total_candidates INT,
    confirmed_impacts INT,
    analysis_completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_impact_source
        FOREIGN KEY (source_revision_id) REFERENCES document_revisions(id) ON DELETE CASCADE
);

CREATE INDEX idx_impact_source ON impact_analyses(source_revision_id);
CREATE INDEX idx_impact_status ON impact_analyses(analysis_status);

-- Impact candidates table
CREATE TABLE IF NOT EXISTS impact_candidates (
    id BIGSERIAL PRIMARY KEY,
    impact_analysis_id BIGINT NOT NULL,
    target_document_revision_id BIGINT NOT NULL,
    impact_score FLOAT,
    impact_reason TEXT,
    reviewer_status VARCHAR(50),
    reviewer_notes TEXT,
    reviewed_at TIMESTAMP,
    CONSTRAINT fk_candidate_analysis
        FOREIGN KEY (impact_analysis_id) REFERENCES impact_analyses(id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_target
        FOREIGN KEY (target_document_revision_id) REFERENCES document_revisions(id) ON DELETE CASCADE
);

CREATE INDEX idx_candidate_analysis ON impact_candidates(impact_analysis_id);
CREATE INDEX idx_candidate_status ON impact_candidates(reviewer_status);

-- Reviewer feedback table
CREATE TABLE IF NOT EXISTS reviewer_feedback (
    id BIGSERIAL PRIMARY KEY,
    chat_message_id BIGINT,
    evidence_id BIGINT,
    feedback_type VARCHAR(50),
    feedback_value VARCHAR(50),
    notes TEXT,
    reviewer_username VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_message
        FOREIGN KEY (chat_message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_evidence
        FOREIGN KEY (evidence_id) REFERENCES retrieved_evidence(id) ON DELETE CASCADE
);

CREATE INDEX idx_feedback_message ON reviewer_feedback(chat_message_id);
CREATE INDEX idx_feedback_type ON reviewer_feedback(feedback_type);

-- Evaluation datasets table
CREATE TABLE IF NOT EXISTS evaluation_datasets (
    id BIGSERIAL PRIMARY KEY,
    dataset_key VARCHAR(100) NOT NULL UNIQUE,
    dataset_name VARCHAR(255),
    description TEXT,
    question_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Evaluation questions table
CREATE TABLE IF NOT EXISTS evaluation_questions (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    expected_answer TEXT,
    relevant_documents TEXT,
    question_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eval_question_dataset
        FOREIGN KEY (dataset_id) REFERENCES evaluation_datasets(id) ON DELETE CASCADE
);

CREATE INDEX idx_eval_question_dataset ON evaluation_questions(dataset_id);

-- Evaluation runs table
CREATE TABLE IF NOT EXISTS evaluation_runs (
    id BIGSERIAL PRIMARY KEY,
    run_key VARCHAR(100) NOT NULL UNIQUE,
    dataset_id BIGINT NOT NULL,
    retrieval_method VARCHAR(100),
    model_name VARCHAR(255),
    run_status VARCHAR(50) DEFAULT 'RUNNING',
    questions_processed INT DEFAULT 0,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_eval_run_dataset
        FOREIGN KEY (dataset_id) REFERENCES evaluation_datasets(id) ON DELETE CASCADE
);

CREATE INDEX idx_eval_run_dataset ON evaluation_runs(dataset_id);
CREATE INDEX idx_eval_run_status ON evaluation_runs(run_status);

-- Evaluation results table
CREATE TABLE IF NOT EXISTS evaluation_results (
    id BIGSERIAL PRIMARY KEY,
    eval_run_id BIGINT NOT NULL,
    eval_question_id BIGINT NOT NULL,
    retrieved_chunk_ids TEXT,
    generated_answer TEXT,
    recall_at_k FLOAT,
    precision_at_k FLOAT,
    mrr FLOAT,
    citation_valid BOOLEAN,
    unsupported_claims INT DEFAULT 0,
    evaluation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_result_run
        FOREIGN KEY (eval_run_id) REFERENCES evaluation_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_result_question
        FOREIGN KEY (eval_question_id) REFERENCES evaluation_questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_result_run ON evaluation_results(eval_run_id);
CREATE INDEX idx_result_question ON evaluation_results(eval_question_id);

-- Retrieval policy versions table
CREATE TABLE IF NOT EXISTS retrieval_policy_versions (
    id BIGSERIAL PRIMARY KEY,
    policy_name VARCHAR(255),
    policy_version INT,
    config_json TEXT,
    top_k INT,
    similarity_threshold FLOAT,
    use_revision_filtering BOOLEAN,
    use_dependency_expansion BOOLEAN,
    max_graph_depth INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_policy_name ON retrieval_policy_versions(policy_name);

COMMIT;
