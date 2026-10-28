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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;

@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);
    private static final int MAX_URL_BYTES = 10 * 1024 * 1024; // 10 MB maximum download
    private static final int MAX_REDIRECTS = 3;

    /**
     * Validate URL against SSRF (Server-Side Request Forgery) attacks.
     * Blocks loopback, link-local, site-local, private, and cloud metadata addresses.
     */
    public void validateUrlSafety(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IngestionException("Source URL cannot be null or empty");
        }

        URI uri;
        try {
            uri = new URI(urlString.trim());
        } catch (URISyntaxException e) {
            throw new IngestionException("Malformed URL syntax: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IngestionException("Invalid URL protocol '" + scheme + "'. Only HTTP and HTTPS are permitted.");
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new IngestionException("Invalid URL: missing host");
        }
        host = host.trim().toLowerCase();

        // Block internal and cloud metadata hostnames
        if (host.equals("metadata.google.internal") || host.equals("instance-data") ||
                host.endsWith(".internal") || host.endsWith(".local") || host.endsWith(".localhost")) {
            throw new IngestionException("Access to cloud metadata and internal domains is strictly blocked (SSRF protection)");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses == null || addresses.length == 0) {
                throw new IngestionException("Cannot resolve host: " + host);
            }
            for (InetAddress addr : addresses) {
                if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() ||
                        addr.isLinkLocalAddress() || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
                    throw new IngestionException("Access to private/local network address (" + addr.getHostAddress() + ") is strictly blocked (SSRF protection)");
                }
                String ip = addr.getHostAddress();
                if (ip.equals("169.254.169.254") || ip.equalsIgnoreCase("fd00:ec2::254")) {
                    throw new IngestionException("Access to cloud metadata IP (" + ip + ") is strictly blocked (SSRF protection)");
                }
            }
        } catch (UnknownHostException e) {
            throw new IngestionException("Unable to resolve host '" + host + "': " + e.getMessage());
        }
    }

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
                        "The document may be scanned or image-only. SpaceDoc Intelligence requires text-based PDFs or OCR-processed documents.");
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
     * Extract text from URL (web page or document) with SSRF and redirect protection
     */
    public String extractTextFromUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IngestionException("Source URL cannot be null or empty");
        }

        String currentUrl = urlString.trim();
        int redirects = 0;

        while (redirects <= MAX_REDIRECTS) {
            validateUrlSafety(currentUrl);

            try {
                URL url = new URI(currentUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "SpaceDoc-RAG/1.0 (ISRO Technical Research Assistant)");
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/pdf,text/plain;q=0.9,*/*;q=0.8");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(false);

                int responseCode = connection.getResponseCode();

                // Handle Redirects safely
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 || responseCode == 308) {

                    String location = connection.getHeaderField("Location");
                    if (location == null || location.trim().isEmpty()) {
                        throw new IngestionException("Received redirect from " + currentUrl + " without Location header");
                    }

                    URI redirectUri = new URI(currentUrl).resolve(location.trim());
                    currentUrl = redirectUri.toString();
                    redirects++;
                    log.info("Following redirect #{} to {}", redirects, currentUrl);
                    continue;
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IngestionException("Remote server returned HTTP " + responseCode + " " +
                            connection.getResponseMessage() + " for URL: " + currentUrl);
                }

                String contentType = connection.getContentType();
                if (contentType != null) {
                    String lowerType = contentType.toLowerCase();
                    boolean isSupported = lowerType.contains("html") ||
                            lowerType.contains("pdf") ||
                            lowerType.contains("text/plain") ||
                            lowerType.contains("xml");
                    if (!isSupported) {
                        throw new IngestionException("Unsupported content type '" + contentType + "'. Only web pages (HTML) and PDF documents are supported for ingestion.");
                    }
                }

                byte[] data = readWithLimit(connection.getInputStream(), MAX_URL_BYTES);

                boolean isPdf = (contentType != null && contentType.toLowerCase().contains("pdf")) ||
                        currentUrl.toLowerCase().endsWith(".pdf");

                if (isPdf) {
                    return extractText(new ByteArrayInputStream(data));
                } else {
                    // HTML parsing via Jsoup
                    Document doc = Jsoup.parse(new ByteArrayInputStream(data), StandardCharsets.UTF_8.name(), currentUrl);
                    doc.select("script, style, nav, footer, header, noscript, svg, form").remove();
                    String text = doc.body() != null ? doc.body().text() : doc.text();
                    if (text == null || text.trim().isEmpty()) {
                        throw new IngestionException("No text content found at URL: " + currentUrl);
                    }
                    String cleanText = text.trim();
                    log.info("Extracted {} characters from URL: {}", cleanText.length(), currentUrl);
                    return cleanText;
                }

            } catch (IngestionException ie) {
                throw ie;
            } catch (Exception e) {
                log.error("Error extracting text from URL: {}", currentUrl, e);
                throw new IngestionException("Error extracting text from URL (" + currentUrl + "): " + e.getMessage(), e);
            }
        }

        throw new IngestionException("Too many redirects (limit: " + MAX_REDIRECTS + ") while fetching URL");
    }

    private byte[] readWithLimit(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        int totalRead = 0;

        while ((bytesRead = in.read(data, 0, data.length)) != -1) {
            totalRead += bytesRead;
            if (totalRead > maxBytes) {
                throw new IngestionException("Content at URL exceeds maximum size limit (" + (maxBytes / (1024 * 1024)) + " MB)");
            }
            buffer.write(data, 0, bytesRead);
        }

        return buffer.toByteArray();
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
