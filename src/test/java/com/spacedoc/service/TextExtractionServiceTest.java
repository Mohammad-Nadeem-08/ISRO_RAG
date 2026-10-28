package com.spacedoc.service;

import com.spacedoc.exception.IngestionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TextExtractionService & SSRF Security Tests")
class TextExtractionServiceTest {

    private TextExtractionService textExtractionService;

    @BeforeEach
    void setUp() {
        textExtractionService = new TextExtractionService();
    }

    @Test
    @DisplayName("1. Extract text from valid text-based PDF")
    void testExtractTextFromValidPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("ISRO Mars Orbiter Mission telemetry specification.");
                contentStream.endText();
            }
            doc.save(baos);
        }

        String extracted = textExtractionService.extractText(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(extracted);
        assertTrue(extracted.contains("ISRO Mars Orbiter Mission telemetry specification"));
    }

    @Test
    @DisplayName("2. Scanned/image-only PDF with zero text throws clear IngestionException")
    void testExtractTextFromScannedEmptyPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page); // Page with no text content
            doc.save(baos);
        }

        IngestionException ex = assertThrows(IngestionException.class, () ->
            textExtractionService.extractText(new ByteArrayInputStream(baos.toByteArray()))
        );
        assertTrue(ex.getMessage().contains("no selectable text") || ex.getMessage().contains("scanned"),
            "Exception message must clearly explain that document has no selectable text / is scanned");
    }

    @Test
    @DisplayName("3. Null or empty PDF stream throws IngestionException")
    void testExtractTextNullOrEmptyStream() {
        assertThrows(IngestionException.class, () -> textExtractionService.extractText(null));
        assertThrows(IngestionException.class, () -> textExtractionService.extractText(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    @DisplayName("4. SSRF: Block loopback addresses (127.0.0.1, localhost)")
    void testSsrfBlocksLoopback() {
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("http://127.0.0.1/admin"));
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("http://localhost:8080/secret"));
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("http://[::1]/internal"));
    }

    @Test
    @DisplayName("5. SSRF: Block private RFC 1918 addresses")
    void testSsrfBlocksPrivateIps() {
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("http://192.168.1.1/router"));
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("http://10.0.0.5/api"));
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("http://172.16.0.1/db"));
    }

    @Test
    @DisplayName("6. SSRF: Block cloud metadata addresses")
    void testSsrfBlocksCloudMetadata() {
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("http://169.254.169.254/latest/meta-data/"));
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("http://metadata.google.internal/computeMetadata/v1/"));
    }

    @Test
    @DisplayName("7. SSRF: Block dangerous protocols (file, ftp, gopher)")
    void testSsrfBlocksDangerousProtocols() {
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("file:///etc/passwd"));
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("ftp://ftp.example.com/file"));
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("gopher://example.com/"));
    }

    @Test
    @DisplayName("8. SSRF: Block malformed or empty URLs")
    void testSsrfBlocksMalformedUrl() {
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety(""));
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety(null));
        assertThrows(IngestionException.class, () -> textExtractionService.validateUrlSafety("not-a-valid-url"));
    }

    @Test
    @DisplayName("9. Valid public HTTPS URL passes validation")
    void testValidPublicUrlPassesValidation() {
        assertDoesNotThrow(() -> textExtractionService.validateUrlSafety("https://www.isro.gov.in/"));
        assertDoesNotThrow(() -> textExtractionService.validateUrlSafety("https://en.wikipedia.org/wiki/ISRO"));
    }
}
