package com.spacedoc.rest;

import com.spacedoc.entity.IngestionJob;
import com.spacedoc.exception.IngestionException;
import com.spacedoc.service.DocumentIngestionService;
import com.spacedoc.service.TextExtractionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:ingestcontrollertestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.flyway.enabled=false"
})
@DisplayName("IngestionController REST API Tests")
public class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentIngestionService ingestionService;

    @MockBean
    private TextExtractionService textExtractionService;

    private byte[] createSamplePdfBytes(String text) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            if (text != null && !text.isEmpty()) {
                try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.newLineAtOffset(100, 700);
                    contentStream.showText(text);
                    contentStream.endText();
                }
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }

    private IngestionJob createMockJob(String jobKey, String status) {
        IngestionJob job = new IngestionJob();
        job.setJobKey(jobKey);
        job.setStatus(status);
        job.setProgressPercent(100);
        job.setCreatedAt(LocalDateTime.now());
        return job;
    }

    @Test
    @DisplayName("1. Successful PDF upload with valid file, title, and CSRF returns 202 ACCEPTED with JSON")
    @WithMockUser(username = "admin")
    void testSuccessfulPdfUpload() throws Exception {
        byte[] pdfBytes = createSamplePdfBytes("ISRO Space Research Document Content");
        MockMultipartFile file = new MockMultipartFile("file", "valid_doc.pdf", "application/pdf", pdfBytes);

        when(textExtractionService.extractText(any(InputStream.class)))
                .thenReturn("ISRO Space Research Document Content");
        when(ingestionService.ingestDocument(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(createMockJob("job-pdf-001", "COMPLETED"));

        mockMvc.perform(multipart("/api/ingestion/upload")
                .file(file)
                .param("title", "Mars Mission Report")
                .param("organization", "ISRO")
                .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.jobKey").value("job-pdf-001"));
    }

    @Test
    @DisplayName("2. PDF upload with empty file returns 400 Bad Request with JSON")
    @WithMockUser(username = "admin")
    void testPdfUploadEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/ingestion/upload")
                .file(emptyFile)
                .param("title", "Mars Mission Report")
                .param("organization", "ISRO")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").isNotEmpty());
    }

    @Test
    @DisplayName("3. PDF upload with invalid file extension returns 400 Bad Request with JSON")
    @WithMockUser(username = "admin")
    void testPdfUploadInvalidExtension() throws Exception {
        MockMultipartFile txtFile = new MockMultipartFile("file", "malicious.exe", "application/octet-stream", "data".getBytes());

        mockMvc.perform(multipart("/api/ingestion/upload")
                .file(txtFile)
                .param("title", "Malicious Document")
                .param("organization", "ISRO")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid file type. Only PDF documents (.pdf) are supported for upload"));
    }

    @Test
    @DisplayName("4. PDF upload missing title returns 400 Bad Request with JSON")
    @WithMockUser(username = "admin")
    void testPdfUploadMissingTitle() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy pdf content".getBytes());

        mockMvc.perform(multipart("/api/ingestion/upload")
                .file(file)
                .param("title", "  ")
                .param("organization", "ISRO")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("Document title is required"));
    }

    @Test
    @DisplayName("5. Scanned PDF with no selectable text returns 422 Unprocessable Entity with informative JSON")
    @WithMockUser(username = "admin")
    void testPdfUploadScannedImageOnlyPdfReturns422() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "scanned_doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(textExtractionService.extractText(any(InputStream.class)))
                .thenThrow(new IngestionException("PDF contains 11 page(s) with no selectable text. The document may be scanned or image-only. SpaceDoc Intelligence requires text-based PDFs or OCR-processed documents."));

        mockMvc.perform(multipart("/api/ingestion/upload")
                .file(file)
                .param("title", "Scanned Doc")
                .param("organization", "ISRO")
                .with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("no selectable text")));
    }

    @Test
    @DisplayName("6. Successful URL submission returns 202 ACCEPTED with JSON")
    @WithMockUser(username = "admin")
    void testSuccessfulUrlSubmission() throws Exception {
        when(textExtractionService.extractTextFromUrl("https://www.isro.gov.in/"))
                .thenReturn("Indian Space Research Organisation official mission overview.");
        when(ingestionService.ingestDocument(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(createMockJob("job-url-001", "COMPLETED"));

        String requestBody = "{\"sourceUrl\":\"https://www.isro.gov.in/\",\"title\":\"isro\",\"organization\":\"ISRO\"}";

        mockMvc.perform(post("/api/ingestion/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.jobKey").value("job-url-001"));
    }

    @Test
    @DisplayName("7. URL submission with missing URL returns 400 Bad Request with JSON")
    @WithMockUser(username = "admin")
    void testUrlSubmissionMissingUrl() throws Exception {
        String requestBody = "{\"sourceUrl\":\"\",\"title\":\"isro\",\"organization\":\"ISRO\"}";

        mockMvc.perform(post("/api/ingestion/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("Source URL is required"));
    }

    @Test
    @DisplayName("8. URL submission with SSRF attempt returns 400 Bad Request with JSON")
    @WithMockUser(username = "admin")
    void testUrlSubmissionSsrfBlocked() throws Exception {
        when(textExtractionService.extractTextFromUrl("http://127.0.0.1:8080/admin"))
                .thenThrow(new IngestionException("Access to private/local network address (127.0.0.1) is strictly blocked (SSRF protection)"));

        String requestBody = "{\"sourceUrl\":\"http://127.0.0.1:8080/admin\",\"title\":\"Internal Admin\",\"organization\":\"ISRO\"}";

        mockMvc.perform(post("/api/ingestion/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("SSRF protection")));
    }

    @Test
    @DisplayName("9. Unauthenticated request to /api/ingestion/url returns 401 Unauthorized with JSON (not HTML login redirect)")
    void testUnauthenticatedApiRequestReturns401Json() throws Exception {
        String requestBody = "{\"sourceUrl\":\"https://www.isro.gov.in/\",\"title\":\"isro\",\"organization\":\"ISRO\"}";

        mockMvc.perform(post("/api/ingestion/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("Authentication required")));
    }

    @Test
    @DisplayName("10. API request without CSRF token returns 403 Forbidden with JSON")
    @WithMockUser(username = "admin")
    void testApiRequestWithoutCsrfReturns403Json() throws Exception {
        String requestBody = "{\"sourceUrl\":\"https://www.isro.gov.in/\",\"title\":\"isro\",\"organization\":\"ISRO\"}";

        mockMvc.perform(post("/api/ingestion/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("Access denied or invalid CSRF")));
    }
}
