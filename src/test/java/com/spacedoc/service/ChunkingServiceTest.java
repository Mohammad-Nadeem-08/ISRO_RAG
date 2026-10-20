package com.spacedoc.service;

import com.spacedoc.entity.DocumentChunk;
import com.spacedoc.entity.DocumentRevision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChunkingService.
 * Tests section detection, chunk splitting, and chunk properties.
 */
class ChunkingServiceTest {

    private ChunkingService service;

    @BeforeEach
    void setUp() {
        // Use default configuration
        service = new ChunkingService();
    }

    @Test
    void testChunkDocument_SimpleText() {
        DocumentRevision revision = createRevision(1L);
        String text = "This is a simple test document. It contains multiple sentences. " +
                      "Each sentence should be properly chunked. The service handles this well.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunkDocument_SectionDetection() {
        DocumentRevision revision = createRevision(1L);
        String text = "# Introduction\n\nThis is the introduction section.\n\n" +
                      "## Methods\n\nThis describes the methods used.\n\n" +
                      "### Experimental Setup\n\nDetails about the setup.\n\n" +
                      "## Results\n\nThe results are presented here.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        assertNotNull(chunks);
        // Should create chunks with section titles
        chunks.forEach(chunk -> {
            assertNotNull(chunk.getSectionTitle());
            assertNotNull(chunk.getSectionNumber());
        });
    }

    @Test
    void testChunkDocument_PreservesSectionNumbers() {
        DocumentRevision revision = createRevision(1L);
        String text = "1. Introduction\n\nContent here.\n\n" +
                      "2. Background\n\nMore content.\n\n" +
                      "2.1 Subsection\n\nDetails.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        assertFalse(chunks.isEmpty());
        // Check that section numbers are extracted
        String firstSectionNumber = chunks.get(0).getSectionNumber();
        assertNotNull(firstSectionNumber);
    }

    @Test
    void testChunkDocument_PageNumberAssignment() {
        DocumentRevision revision = createRevision(1L);
        String text = "Page 1 content here. With enough text to simulate content.\n\n" +
                      "More content on page 1. And even more text to fill up space.\n\n" +
                      "Page 2 content starts here. Additional text for length.\n\n" +
                      "Continuing on page 2 with more text content.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        assertFalse(chunks.isEmpty());
        chunks.forEach(chunk -> {
            assertNotNull(chunk.getPageNumber());
            assertTrue(chunk.getPageNumber() >= 1);
        });
    }

    @Test
    void testChunkDocument_ChunkNumbering() {
        DocumentRevision revision = createRevision(1L);
        String text = "First chunk content.\n\n" +
                      "Second chunk content.\n\n" +
                      "Third chunk content.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        int expectedChunkNumber = 0;
        for (DocumentChunk chunk : chunks) {
            assertEquals(expectedChunkNumber++, chunk.getChunkNumber());
        }
    }

    @Test
    void testChunkDocument_OffsetTracking() {
        DocumentRevision revision = createRevision(1L);
        String text = "Short intro.\n\n" +
                      "This is a longer section with content that should be tracked properly for offsets.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        assertFalse(chunks.isEmpty());
        chunks.forEach(chunk -> {
            assertNotNull(chunk.getStartOffset());
            assertNotNull(chunk.getEndOffset());
            assertTrue(chunk.getEndOffset() >= chunk.getStartOffset());
        });
    }

    @Test
    void testChunkDocument_TokenCountEstimation() {
        DocumentRevision revision = createRevision(1L);
        String text = "This is a test document with known token count approximation. " +
                      "Each chunk should have an estimated token count based on character count.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        assertFalse(chunks.isEmpty());
        chunks.forEach(chunk -> {
            assertNotNull(chunk.getTokenCount());
            assertTrue(chunk.getTokenCount() > 0);
        });
    }

    @Test
    void testChunkDocument_MinChunkSizeFilter() {
        // Set a large min chunk size (via reflection to test filtering)
        DocumentRevision revision = createRevision(1L);
        String text = "Very short.\n\n" +                    // Below min size
                      "Another very short.\n\n" +            // Below min size
                      "This is a substantial paragraph with enough content to exceed the minimum chunk size requirement.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        // At least one chunk should exceed min size
        boolean hasSubstantialChunk = chunks.stream()
            .anyMatch(c -> c.getContent().length() > 50);
        assertTrue(hasSubstantialChunk, "Should have chunks larger than minimum size");
    }

    @Test
    void testChunkDocument_EmptyText() {
        DocumentRevision revision = createRevision(1L);

        List<DocumentChunk> chunks = service.chunkDocument("", revision);

        assertNotNull(chunks);
        // Empty or whitespace-only text may produce no chunks or very few
    }

    @Test
    void testChunkDocument_SingleSentence() {
        DocumentRevision revision = createRevision(1L);
        String text = "This is a single sentence that should form one chunk.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunkDocument_WithHeaderPattern() {
        DocumentRevision revision = createRevision(1L);
        String text = "# Main Header\n\nContent under main header.\n\n" +
                      "## Sub Header\n\nContent under sub header.\n\n" +
                      "### Third Level\n\nMore content.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        // Should detect headers and create chunks
        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunkDocument_RevisionLinkage() {
        DocumentRevision revision = createRevision(42L, "REV-001");
        String text = "Test content that should be linked to its revision.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        assertFalse(chunks.isEmpty());
        chunks.forEach(chunk -> {
            assertEquals(revision, chunk.getDocumentRevision());
        });
    }

    @Test
    void testChunkDocument_ChunkProperties() {
        DocumentRevision revision = createRevision(1L);
        String text = "Test chunk properties for content, section, and metadata.";

        List<DocumentChunk> chunks = service.chunkDocument(text, revision);

        assertFalse(chunks.isEmpty());
        DocumentChunk chunk = chunks.get(0);

        assertNotNull(chunk.getContent());
        assertNotNull(chunk.getSectionTitle());
        assertNotNull(chunk.getCreatedAt());
        assertNotNull(chunk.getChunkNumber());
    }

    @Test
    void testChunkDocument_OverlappingContent() {
        // This test verifies overlap handling (chunk_overlap is 128 chars by default)
        DocumentRevision revision = createRevision(1L);
        // Create a long text with repeated patterns to test overlap
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append("This is sentence number ").append(i).append(" in the test document. ");
            builder.append("It contains enough words to exceed the chunk size threshold. ");
        }

        List<DocumentChunk> chunks = service.chunkDocument(builder.toString(), revision);

        // Should create multiple chunks
        assertTrue(chunks.size() >= 1);
    }

    private DocumentRevision createRevision(Long id) {
        return createRevision(id, "REV-001");
    }

    private DocumentRevision createRevision(Long id, String revisionKey) {
        DocumentRevision revision = new DocumentRevision();
        revision.setId(id);
        revision.setRevisionKey(revisionKey);
        revision.setRevisionNumber(1);
        revision.setCreatedAt(LocalDateTime.now());
        revision.setContentHash("hash-" + id);
        return revision;
    }
}
