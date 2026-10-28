package com.spacedoc.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests verifying the Markdown formatting rules, HTML sanitization,
 * and citation extraction contracts used in the SpaceDoc Research Assistant UI.
 */
class MarkdownRenderingSafetyTest {

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }

    private String renderSafeMarkdown(String raw) {
        if (raw == null) return "";
        // 1. Escape HTML
        String safe = escapeHtml(raw);

        // 2. Code blocks
        safe = safe.replaceAll("(?s)```([a-zA-Z0-9_-]*)\\n(.*?)```", "<pre><code class=\"language-$1\">$2</code></pre>");

        // 3. Inline code
        safe = safe.replaceAll("`([^`\\n]+)`", "<code>$1</code>");

        // 4. Headers
        safe = safe.replaceAll("(?m)^### (.*$)", "<h3>$1</h3>");
        safe = safe.replaceAll("(?m)^## (.*$)", "<h2>$1</h2>");
        safe = safe.replaceAll("(?m)^# (.*$)", "<h1>$1</h1>");

        // 5. Bold & Italics
        safe = safe.replaceAll("\\*\\*([^\\*\\n]+)\\*\\*", "<strong>$1</strong>");
        safe = safe.replaceAll("__([^__\\n]+)__", "<strong>$1</strong>");

        // 6. Citation links
        safe = safe.replaceAll("(?i)\\[(?:Source|Citation|Chunk)?[\\s\\h\\xA0]*#?(\\d+)\\]", "<span class=\"citation-pill\">Source $1</span>");

        return safe;
    }

    @Test
    @DisplayName("Should escape malicious script and HTML tags to prevent XSS")
    void testXssSanitization() {
        String xssPayload = "<script>alert('pwned')</script><img src=x onerror=alert(1)><b>bold</b>";
        String rendered = renderSafeMarkdown(xssPayload);

        assertFalse(rendered.contains("<script>"), "Raw script tag must not be present");
        assertFalse(rendered.contains("<img"), "Raw img tag must not be present");
        assertTrue(rendered.contains("&lt;script&gt;"), "Script tag must be escaped");
        assertTrue(rendered.contains("&lt;img src=x onerror=alert(1)&gt;"), "Image tag must be escaped");
    }

    @Test
    @DisplayName("Should correctly format bold technical terms and headers")
    void testMarkdownFormatting() {
        String markdown = "### Mission Status\n**GSLV-F18/NVS-03 mission** was successful.";
        String rendered = renderSafeMarkdown(markdown);

        assertTrue(rendered.contains("<h3>Mission Status</h3>"), "H3 header must be rendered");
        assertTrue(rendered.contains("<strong>GSLV-F18/NVS-03 mission</strong>"), "Bold markdown must be rendered as strong");
    }

    @Test
    @DisplayName("Should correctly format inline code and code blocks")
    void testCodeFormatting() {
        String markdown = "The engine is `CE-20` cryogenic.\n```json\n{\"thrust\": 200}\n```";
        String rendered = renderSafeMarkdown(markdown);

        assertTrue(rendered.contains("<code>CE-20</code>"), "Inline code must be wrapped in <code>");
        assertTrue(rendered.contains("<pre><code class=\"language-json\">"), "Fenced code block must be rendered with pre code");
    }

    @Test
    @DisplayName("Should detect and render interactive citation pills")
    void testCitationPillRendering() {
        String markdown = "ISRO launched Chandrayaan-3 [Source 1] using LVM3 [2].";
        String rendered = renderSafeMarkdown(markdown);

        assertTrue(rendered.contains("<span class=\"citation-pill\">Source 1</span>"), "Citation [Source 1] must be rendered as pill");
        assertTrue(rendered.contains("<span class=\"citation-pill\">Source 2</span>"), "Citation [2] must be rendered as pill");
    }

    @Test
    @DisplayName("Should preserve technical terms with hyphens, slashes, and numbers without corruption")
    void testTechnicalTermsIntegrity() {
        String technicalText = "Aditya-L1 SUIT instrument, PSLV-C59/PROBA-3, and nomic-embed-text (768d).";
        String rendered = renderSafeMarkdown(technicalText);

        assertTrue(rendered.contains("Aditya-L1"), "Hyphenated mission name must be preserved");
        assertTrue(rendered.contains("PSLV-C59/PROBA-3"), "Slashed mission identifier must be preserved");
        assertTrue(rendered.contains("nomic-embed-text (768d)"), "Model identifier must be preserved");
    }
}
