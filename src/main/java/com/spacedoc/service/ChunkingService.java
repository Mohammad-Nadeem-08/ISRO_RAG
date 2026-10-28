package com.spacedoc.service;

import com.spacedoc.entity.DocumentChunk;
import com.spacedoc.entity.DocumentRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    @Value("${chunk.size:512}")
    private int chunkSize;

    @Value("${chunk.overlap:128}")
    private int chunkOverlap;

    @Value("${chunk.min-size:50}")
    private int minChunkSize;

    private static final Pattern SECTION_PATTERN = Pattern.compile(
        "^(#+\\s+|\\d+\\.\\s+|[A-Z][A-Z0-9]*\\s*:|Section\\s+[\\d.]+)",
        Pattern.MULTILINE
    );

    public List<DocumentChunk> chunkDocument(String text, DocumentRevision revision) {
        List<DocumentChunk> chunks = new ArrayList<>();

        // Split by sections first
        List<Section> sections = detectSections(text);

        int globalChunkNumber = 0;
        int currentPageNumber = 1;

        for (Section section : sections) {
            String sectionText = section.content;
            int sectionStartOffset = section.startOffset;

            // Split section into chunks
            List<String> sectionChunks = splitIntoChunks(sectionText);

            for (int i = 0; i < sectionChunks.size(); i++) {
                String chunkText = sectionChunks.get(i);

                if (chunkText.trim().length() < minChunkSize) {
                    continue;
                }

                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentRevision(revision);
                chunk.setChunkNumber(globalChunkNumber++);
                chunk.setContent(chunkText);
                chunk.setSectionTitle(section.title);
                chunk.setPageNumber(currentPageNumber);
                chunk.setSectionNumber(section.sectionNumber);
                chunk.setStartOffset((long)sectionStartOffset);
                chunk.setEndOffset((long)(sectionStartOffset + chunkText.length()));
                chunk.setTokenCount(estimateTokenCount(chunkText));
                chunk.setCreatedAt(java.time.LocalDateTime.now());

                chunks.add(chunk);

                // Estimate page breaks (rough approximation: ~3000 chars per page)
                if (sectionStartOffset + chunkText.length() % 3000 < chunkText.length()) {
                    currentPageNumber++;
                }

                sectionStartOffset += chunkText.length();
            }
        }

        log.info("Created {} chunks from document revision {}", chunks.size(), revision.getId());
        return chunks;
    }

    private List<Section> detectSections(String text) {
        List<Section> sections = new ArrayList<>();
        String[] lines = text.split("\n");

        StringBuilder currentSectionText = new StringBuilder();
        String currentSectionTitle = "Introduction";
        String currentSectionNumber = "0";
        int sectionStartOffset = 0;

        for (String line : lines) {
            if (isSectionHeader(line)) {
                if (currentSectionText.length() > 0) {
                    sections.add(new Section(
                        currentSectionTitle,
                        currentSectionNumber,
                        currentSectionText.toString(),
                        sectionStartOffset
                    ));
                }
                currentSectionTitle = line.trim();
                currentSectionNumber = extractSectionNumber(line);
                currentSectionText = new StringBuilder();
                sectionStartOffset = text.indexOf(line);
            } else {
                currentSectionText.append(line).append("\n");
            }
        }

        if (currentSectionText.length() > 0) {
            sections.add(new Section(
                currentSectionTitle,
                currentSectionNumber,
                currentSectionText.toString(),
                sectionStartOffset
            ));
        }

        return sections;
    }

    private boolean isSectionHeader(String line) {
        return SECTION_PATTERN.matcher(line).find() && line.trim().length() < 200;
    }

    private String extractSectionNumber(String line) {
        Matcher matcher = Pattern.compile("^(\\d+[.\\d]*|[A-Z])").matcher(line.trim());
        return matcher.find() ? matcher.group(1) : "0";
    }

    public List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        int targetChunkSize = this.chunkSize > 0 ? this.chunkSize : 512;
        int targetOverlap = this.chunkOverlap > 0 ? Math.min(this.chunkOverlap, targetChunkSize / 3) : 128;
        int maxUnitLen = Math.max(100, targetChunkSize - targetOverlap);

        if (text.trim().length() <= targetChunkSize) {
            chunks.add(text.trim());
            return chunks;
        }

        // Split by sentence delimiters (. ! ?) or line breaks
        String[] rawUnits = text.split("((?<=[.!?])\\s+|(?<=\\n))");
        List<String> normalizedUnits = new ArrayList<>();

        for (String unit : rawUnits) {
            String trimmed = unit.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= maxUnitLen) {
                normalizedUnits.add(trimmed);
            } else {
                normalizedUnits.addAll(splitLongSegment(trimmed, maxUnitLen, targetOverlap / 2));
            }
        }

        StringBuilder currentChunk = new StringBuilder();

        for (String unit : normalizedUnits) {
            if (currentChunk.length() > 0 && (currentChunk.length() + 1 + unit.length()) > targetChunkSize) {
                chunks.add(currentChunk.toString().trim());
                int overlapStart = Math.max(0, currentChunk.length() - targetOverlap);
                String overlapText = currentChunk.substring(overlapStart).trim();
                currentChunk = new StringBuilder();
                if (!overlapText.isEmpty()) {
                    currentChunk.append(overlapText).append(" ");
                }
            }

            if (currentChunk.length() + (currentChunk.length() > 0 ? 1 : 0) + unit.length() > targetChunkSize) {
                // If even after flushing or with overlap it would exceed, flush without overlap
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
            }

            if (currentChunk.length() > 0 && !currentChunk.toString().endsWith(" ")) {
                currentChunk.append(" ");
            }
            currentChunk.append(unit);
        }

        if (currentChunk.length() > 0) {
            String finalChunk = currentChunk.toString().trim();
            if (!finalChunk.isEmpty()) {
                chunks.add(finalChunk);
            }
        }

        return chunks;
    }

    private List<String> splitLongSegment(String segment, int maxLen, int overlap) {
        List<String> pieces = new ArrayList<>();
        int start = 0;
        int length = segment.length();

        while (start < length) {
            int end = Math.min(start + maxLen, length);

            // Try to find a whitespace boundary if not at the very end
            if (end < length) {
                int lastSpace = segment.lastIndexOf(' ', end);
                if (lastSpace > start + maxLen / 3) {
                    end = lastSpace;
                }
            }

            String piece = segment.substring(start, end).trim();
            if (!piece.isEmpty()) {
                pieces.add(piece);
            }

            if (end >= length) {
                break;
            }

            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }

        return pieces;
    }

    private int estimateTokenCount(String text) {
        // Rough estimation: 1 token ≈ 4 characters
        return Math.max(1, text.length() / 4);
    }

    private static class Section {
        String title;
        String sectionNumber;
        String content;
        int startOffset;

        Section(String title, String sectionNumber, String content, int startOffset) {
            this.title = title;
            this.sectionNumber = sectionNumber;
            this.content = content;
            this.startOffset = startOffset;
        }
    }
}
