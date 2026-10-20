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

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if ((currentChunk.length() + sentence.length()) > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    // Keep overlap
                    int overlapStart = Math.max(0, currentChunk.length() - chunkOverlap);
                    currentChunk = new StringBuilder(currentChunk.substring(overlapStart));
                }
            }
            currentChunk.append(sentence).append(" ");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
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
