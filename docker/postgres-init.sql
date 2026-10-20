-- PostgreSQL initialization for SpaceDoc Intelligence
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Set default search_path
SET search_path TO public;
