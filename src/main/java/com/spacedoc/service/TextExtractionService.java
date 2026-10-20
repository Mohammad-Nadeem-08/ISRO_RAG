package com.spacedoc.service;

import com.spacedoc.exception.IngestionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);

    /**
     * Extract text from PDF input stream using Apache PDFBox
     */
    public String extractText(InputStream inputStream) {
        if (inputStream == null) {
            throw new IngestionException("PDF input stream cannot be null");
        }

        byte[] bytes;
        try {
            bytes = inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read PDF input stream", e);
            throw new IngestionException("Failed to read PDF stream: " + e.getMessage(), e);
        }

        if (bytes.length == 0) {
            throw new IngestionException("PDF file is empty (0 bytes)");
        }

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(bytes))) {
            if (document.isEncrypted()) {
                throw new IngestionException("PDF document is encrypted or password-protected");
            }

            int numberOfPages = document.getNumberOfPages();
            if (numberOfPages == 0) {
                throw new IngestionException("PDF document has 0 pages");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                log.warn("Extracted 0 characters from PDF across {} page(s). Document appears scanned or image-only.", numberOfPages);
                throw new IngestionException("PDF contains " + numberOfPages + " page(s) with no selectable text. " +
                        "The document may be scanned or image-only. OCR is required to extract text from scanned documents.");
            }

            String cleanText = text.trim();
            log.info("Extracted {} characters from PDF ({} pages)", cleanText.length(), numberOfPages);
            return cleanText;

        } catch (IngestionException ie) {
            throw ie;
        } catch (IOException e) {
            log.error("Error parsing PDF document", e);
            throw new IngestionException("Invalid or unreadable PDF document: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error extracting text from PDF", e);
            throw new IngestionException("Unexpected error processing PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from URL (web page or document)
     */
    public String extractTextFromUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IngestionException("Source URL cannot be null or empty");
        }

        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "SpaceDoc/1.0");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            String contentType = connection.getContentType();
            try (InputStream inputStream = connection.getInputStream()) {
                if ((contentType != null && contentType.toLowerCase().contains("pdf")) || urlString.toLowerCase().endsWith(".pdf")) {
                    return extractText(inputStream);
                } else {
                    // HTML parsing via Jsoup
                    Document doc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), urlString);
                    doc.select("script, style, nav, footer, header, noscript").remove();
                    String text = doc.body() != null ? doc.body().text() : doc.text();
                    if (text == null || text.trim().isEmpty()) {
                        throw new IngestionException("No text content found at URL: " + urlString);
                    }
                    String cleanText = text.trim();
                    log.info("Extracted {} characters from URL: {}", cleanText.length(), urlString);
                    return cleanText;
                }
            }
        } catch (IngestionException ie) {
            throw ie;
        } catch (Exception e) {
            log.error("Error extracting text from URL: {}", urlString, e);
            throw new IngestionException("Error extracting text from URL: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from generic file stream
     */
    public String extractTextGeneric(InputStream inputStream, String filename) {
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            return extractText(inputStream);
        }
        try {
            byte[] bytes = inputStream.readAllBytes();
            if (filename != null && (filename.toLowerCase().endsWith(".html") || filename.toLowerCase().endsWith(".htm"))) {
                Document doc = Jsoup.parse(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8.name(), "");
                doc.select("script, style, nav, footer, header, noscript").remove();
                String text = doc.body() != null ? doc.body().text() : doc.text();
                return text != null ? text.trim() : "";
            }
            try {
                return extractText(new ByteArrayInputStream(bytes));
            } catch (Exception ignored) {
                return new String(bytes, StandardCharsets.UTF_8).trim();
            }
        } catch (Exception e) {
            log.error("Error extracting text from file: {}", filename, e);
            throw new IngestionException("Error extracting text from file: " + e.getMessage(), e);
        }
    }
}
