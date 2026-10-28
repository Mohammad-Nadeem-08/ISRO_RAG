<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<style>
    /* Research Assistant Layout */
    .assistant-grid {
        display: grid;
        grid-template-columns: 1fr 380px;
        gap: 24px;
        align-items: start;
    }

    @media (max-width: 1080px) {
        .assistant-grid {
            grid-template-columns: 1fr 320px;
            gap: 18px;
        }
    }

    @media (max-width: 991.98px) {
        .assistant-grid {
            grid-template-columns: 1fr;
        }
    }

    /* Main Conversation Card */
    .chat-card {
        background: #ffffff;
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-lg);
        box-shadow: var(--shadow-card);
        display: flex;
        flex-direction: column;
        height: calc(100vh - 165px);
        min-height: 580px;
    }

    .chat-header {
        padding: 16px 20px;
        border-bottom: 1px solid var(--border-subtle);
        display: flex;
        align-items: center;
        justify-content: space-between;
        background: #fafbfc;
        border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    }

    .chat-header-info {
        display: flex;
        align-items: center;
        gap: 10px;
    }

    .chat-status-indicator {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background-color: var(--success);
        box-shadow: 0 0 6px var(--success);
    }

    .chat-header-title {
        font-size: 14.5px;
        font-weight: 600;
        color: var(--text-main);
        margin: 0;
    }

    .chat-header-subtitle {
        font-size: 11.5px;
        color: var(--text-muted);
    }

    /* Message Stream */
    .chat-messages {
        flex: 1;
        overflow-y: auto;
        padding: 24px;
        display: flex;
        flex-direction: column;
        gap: 20px;
        scroll-behavior: smooth;
    }

    /* Custom Scrollbar */
    .chat-messages::-webkit-scrollbar, .evidence-stream::-webkit-scrollbar {
        width: 6px;
    }
    .chat-messages::-webkit-scrollbar-thumb, .evidence-stream::-webkit-scrollbar-thumb {
        background: #cbd5e1;
        border-radius: 4px;
    }

    /* Message Rows & Avatars */
    .message-row {
        display: flex;
        gap: 14px;
        max-width: 90%;
    }

    .message-row.user {
        align-self: flex-end;
        flex-direction: row-reverse;
    }

    .message-row.assistant {
        align-self: flex-start;
    }

    .message-avatar {
        width: 34px;
        height: 34px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        flex-shrink: 0;
        box-shadow: 0 2px 5px rgba(0,0,0,0.08);
    }

    .message-row.user .message-avatar {
        background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
        color: white;
    }

    .message-row.assistant .message-avatar {
        background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
        color: var(--accent-cyan);
        border: 1px solid rgba(255,255,255,0.1);
    }

    /* Message Bubbles */
    .message-bubble {
        padding: 14px 18px;
        border-radius: var(--radius-lg);
        font-size: 13.5px;
        line-height: 1.6;
        position: relative;
        word-break: break-word;
    }

    .message-row.user .message-bubble {
        background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
        color: #ffffff;
        border-bottom-right-radius: 4px;
        box-shadow: 0 4px 10px rgba(37, 99, 235, 0.2);
    }

    .message-row.assistant .message-bubble {
        background: #ffffff;
        color: var(--text-main);
        border: 1px solid var(--border-subtle);
        border-bottom-left-radius: 4px;
        box-shadow: var(--shadow-subtle);
    }

    /* Markdown Formatted Elements inside Assistant Bubbles */
    .markdown-content p {
        margin: 0 0 10px 0;
    }
    .markdown-content p:last-child {
        margin-bottom: 0;
    }

    .markdown-content h1, .markdown-content h2, .markdown-content h3, .markdown-content h4 {
        color: var(--text-main);
        font-weight: 700;
        margin: 14px 0 8px 0;
        letter-spacing: -0.3px;
    }
    .markdown-content h1 { font-size: 17px; }
    .markdown-content h2 { font-size: 15.5px; }
    .markdown-content h3 { font-size: 14.5px; }

    .markdown-content ul, .markdown-content ol {
        margin: 6px 0 10px 0;
        padding-left: 20px;
    }

    .markdown-content li {
        margin-bottom: 4px;
    }

    .markdown-content strong {
        color: #0f172a;
        font-weight: 600;
    }

    .markdown-content code {
        font-family: 'JetBrains Mono', monospace;
        font-size: 12px;
        background: #f1f5f9;
        color: #0f172a;
        padding: 2px 6px;
        border-radius: 4px;
        border: 1px solid #e2e8f0;
    }

    .markdown-content pre {
        background: #0b0f19;
        color: #e2e8f0;
        padding: 12px 14px;
        border-radius: var(--radius-md);
        overflow-x: auto;
        font-family: 'JetBrains Mono', monospace;
        font-size: 12px;
        margin: 10px 0;
        border: 1px solid rgba(255,255,255,0.08);
    }

    .markdown-content pre code {
        background: transparent;
        color: inherit;
        padding: 0;
        border: none;
    }

    .markdown-content blockquote {
        border-left: 3px solid var(--primary);
        padding-left: 12px;
        margin: 10px 0;
        color: var(--text-secondary);
        font-style: italic;
    }

    /* Interactive Citation Pills */
    .citation-pill {
        display: inline-flex;
        align-items: center;
        gap: 3px;
        background: #eff6ff;
        color: #1d4ed8;
        border: 1px solid #bfdbfe;
        padding: 1px 7px;
        border-radius: var(--radius-full);
        font-size: 11px;
        font-weight: 600;
        margin: 0 3px;
        cursor: pointer;
        transition: all 0.15s ease;
        text-decoration: none;
        vertical-align: middle;
    }

    .citation-pill:hover {
        background: #dbeafe;
        color: #1e40af;
        border-color: #93c5fd;
        transform: translateY(-1px);
        box-shadow: 0 2px 4px rgba(37, 99, 235, 0.15);
    }

    .citation-pill.unresolved {
        background: #fffbeb !important;
        color: #b45309 !important;
        border-color: #fde68a !important;
        cursor: not-allowed;
    }

    .citation-pill.unresolved:hover {
        background: #fef3c7 !important;
        color: #92400e !important;
        border-color: #fcd34d !important;
        transform: none;
        box-shadow: none;
    }

    @keyframes pulseHighlight {
        0% { transform: scale(1.02); }
        50% { transform: scale(1.00); }
        100% { transform: scale(1.00); }
    }

    /* Chat Input Bar */
    .chat-input-bar {
        padding: 16px 20px;
        border-top: 1px solid var(--border-subtle);
        background: #fafbfc;
        border-radius: 0 0 var(--radius-lg) var(--radius-lg);
    }

    .chat-input-wrapper {
        display: flex;
        gap: 10px;
        background: #ffffff;
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-md);
        padding: 4px 6px 4px 14px;
        box-shadow: 0 1px 2px rgba(0,0,0,0.04);
        transition: border-color 0.2s ease, box-shadow 0.2s ease;
    }

    .chat-input-wrapper:focus-within {
        border-color: var(--primary);
        box-shadow: 0 0 0 3px var(--primary-glow);
    }

    .chat-input-field {
        flex: 1;
        border: none;
        outline: none;
        font-size: 13.5px;
        color: var(--text-main);
        background: transparent;
        padding: 8px 0;
    }

    .chat-input-field::placeholder {
        color: var(--text-light);
    }

    .starter-prompts {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-top: 14px;
    }

    .starter-chip {
        background: #f1f5f9;
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-full);
        padding: 5px 12px;
        font-size: 11.5px;
        color: var(--text-secondary);
        cursor: pointer;
        transition: all 0.15s ease;
        display: inline-flex;
        align-items: center;
        gap: 5px;
    }

    .starter-chip:hover {
        background: #e2e8f0;
        color: var(--primary);
        border-color: #cbd5e1;
        transform: translateY(-1px);
    }

    /* Evidence Sidebar */
    .evidence-sidebar {
        display: flex;
        flex-direction: column;
        gap: 18px;
    }

    .evidence-card {
        background: #ffffff;
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-lg);
        box-shadow: var(--shadow-card);
        padding: 18px;
    }

    .evidence-header-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 14px;
        padding-bottom: 10px;
        border-bottom: 1px solid var(--border-subtle);
    }

    .evidence-header-title {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-main);
        margin: 0;
        display: flex;
        align-items: center;
        gap: 8px;
    }

    .evidence-stream {
        max-height: 480px;
        overflow-y: auto;
        padding-right: 4px;
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .evidence-item-box {
        background: #f8fafc;
        border: 1px solid var(--border-subtle);
        border-left: 3px solid var(--primary);
        border-radius: var(--radius-md);
        padding: 12px;
        transition: all 0.2s ease;
    }

    .evidence-item-box:hover {
        border-color: #cbd5e1;
        border-left-color: var(--primary);
        box-shadow: 0 4px 8px rgba(0,0,0,0.04);
        background: #ffffff;
    }

    .evidence-item-box.highlighted {
        border-color: #3b82f6 !important;
        border-left-color: #2563eb !important;
        background: #eff6ff !important;
        box-shadow: 0 0 12px rgba(37, 99, 235, 0.25) !important;
    }

    .evidence-item-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 6px;
    }

    .evidence-item-source {
        font-size: 12px;
        font-weight: 700;
        color: var(--primary);
    }

    .evidence-item-score {
        font-size: 10px;
        font-weight: 600;
        background: #dbeafe;
        color: #1e40af;
        padding: 2px 6px;
        border-radius: 4px;
    }

    .evidence-item-text {
        font-size: 12px;
        color: var(--text-secondary);
        line-height: 1.5;
        margin-bottom: 8px;
    }

    .evidence-item-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        font-size: 10.5px;
        color: var(--text-muted);
        border-top: 1px solid #f1f5f9;
        padding-top: 6px;
    }

    .evidence-meta-pill {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 4px;
        padding: 1px 6px;
    }

    /* Metrics Summary Card */
    .metrics-summary-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
    }

    .metric-chip {
        background: #f8fafc;
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-md);
        padding: 8px 10px;
    }

    .metric-chip-label {
        font-size: 10px;
        font-weight: 600;
        color: var(--text-muted);
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }

    .metric-chip-value {
        font-size: 14px;
        font-weight: 700;
        color: var(--text-main);
        margin-top: 2px;
    }
</style>

<div class="page-header">
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2">
        <div>
            <h1 class="page-title">Research Assistant</h1>
            <p class="page-subtitle">Grounded technical question-answering with citation validation</p>
        </div>
        <button class="button-secondary" id="clear-btn" type="button" title="Clear current session messages">
            <i class="fas fa-trash-alt text-danger"></i>
            <span>Clear History</span>
        </button>
    </div>
</div>

<!-- Alert Containers -->
<div id="error-message" class="error-message">
    <i class="fas fa-exclamation-circle text-danger"></i>
    <span id="error-text">An error occurred</span>
</div>
<div id="success-message" class="success-message">
    <i class="fas fa-check-circle text-success"></i>
    <span id="success-text">Operation completed</span>
</div>

<!-- Assistant Main Grid -->
<div class="assistant-grid">
    <!-- Conversation Column -->
    <div class="chat-card">
        <div class="chat-header">
            <div class="chat-header-info">
                <span class="chat-status-indicator"></span>
                <div>
                    <div class="chat-header-title">Technical Inquiry Session</div>
                    <div class="chat-header-subtitle">Groq GPT-OSS • Ollama Vector Retrieval</div>
                </div>
            </div>
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle" style="font-size: 11px;">Hybrid RRF</span>
        </div>

        <!-- Scrollable Message Stream -->
        <div class="chat-messages" id="message-container">
            <!-- Welcome Empty State -->
            <div class="empty-state my-auto" id="welcome-state">
                <div class="empty-state-icon">
                    <i class="fas fa-satellite text-primary"></i>
                </div>
                <div class="empty-state-title">Welcome to SpaceDoc Intelligence</div>
                <p style="font-size: 13.5px; max-width: 480px; margin: 0 auto 16px;">
                    Ask technical questions across your ingested ISRO mission specifications, reports, and orbital data.
                </p>
                <div class="starter-prompts justify-content-center">
                    <div class="starter-chip" onclick="applyStarterPrompt('What is Chandrayaan-3\'s propulsion system?')">
                        <i class="fas fa-fire-alt text-warning"></i> Chandrayaan-3 Propulsion
                    </div>
                    <div class="starter-chip" onclick="applyStarterPrompt('What observations did Aditya-L1 make according to ISRO?')">
                        <i class="fas fa-sun text-warning"></i> Aditya-L1 Observations
                    </div>
                    <div class="starter-chip" onclick="applyStarterPrompt('Tell me about the CE-20 cryogenic upper stage engine.')">
                        <i class="fas fa-cog text-info"></i> CE-20 Cryogenic Engine
                    </div>
                    <div class="starter-chip" onclick="applyStarterPrompt('What recent missions are published on the ISRO portal?')">
                        <i class="fas fa-globe text-primary"></i> Recent ISRO Portal Missions
                    </div>
                </div>
            </div>
        </div>

        <!-- Chat Input Bar -->
        <div class="chat-input-bar">
            <div class="chat-input-wrapper">
                <i class="fas fa-search text-muted my-auto ms-1"></i>
                <input type="text" class="chat-input-field" id="query-input" placeholder="Type an ISRO technical query (e.g. telemetry, propulsion, orbits)..." autofocus>
                <button class="button-primary py-2 px-3" id="submit-btn" type="button">
                    <i class="fas fa-paper-plane"></i>
                    <span>Ask</span>
                </button>
            </div>
            <div class="d-flex align-items-center justify-content-between mt-2 px-1">
                <small class="text-muted" style="font-size: 11px;">
                    <i class="fas fa-info-circle me-1"></i> Press <strong>Enter</strong> to submit. Answers cite indexed evidence chunks.
                </small>
                <small class="text-muted" style="font-size: 11px;">
                    Citations are strictly validated
                </small>
            </div>
        </div>
    </div>

    <!-- Evidence & Metrics Sidebar -->
    <div class="evidence-sidebar">
        <!-- Retrieved Evidence Card -->
        <div class="evidence-card">
            <div class="evidence-header-row">
                <h5 class="evidence-header-title">
                    <i class="fas fa-layer-group text-primary"></i>
                    <span>Retrieved Evidence</span>
                </h5>
                <span class="badge bg-light text-secondary border" id="evidence-count-badge">0 sources</span>
            </div>
            
            <div class="evidence-stream" id="evidence-panel">
                <div class="empty-state py-4 text-center">
                    <i class="fas fa-inbox text-light mb-2" style="font-size: 26px;"></i>
                    <div style="font-size: 12.5px; color: #64748b;">
                        Retrieved evidence chunks will populate here with similarity scores and metadata.
                    </div>
                </div>
            </div>
        </div>

        <!-- Grounding & RAG Metrics Card -->
        <div class="evidence-card">
            <div class="evidence-header-row">
                <h5 class="evidence-header-title">
                    <i class="fas fa-chart-pie text-success"></i>
                    <span>Session Telemetry</span>
                </h5>
                <span class="badge bg-success-subtle text-success" id="confidence-badge">Ready</span>
            </div>
            
            <div class="metrics-summary-grid" id="metrics-panel">
                <div class="metric-chip">
                    <div class="metric-chip-label">LLM Engine</div>
                    <div class="metric-chip-value" style="font-size: 12px;">Groq GPT-OSS</div>
                </div>
                <div class="metric-chip">
                    <div class="metric-chip-label">Embedding</div>
                    <div class="metric-chip-value" style="font-size: 12px;">768d nomic</div>
                </div>
                <div class="metric-chip">
                    <div class="metric-chip-label">Confidence</div>
                    <div class="metric-chip-value" id="val-confidence">-</div>
                </div>
                <div class="metric-chip">
                    <div class="metric-chip-label">Citations</div>
                    <div class="metric-chip-value" id="val-citations">-</div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    function getCsrfHeaders() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        if (tokenMeta && headerMeta) {
            return {
                [headerMeta.getAttribute('content')]: tokenMeta.getAttribute('content')
            };
        }
        return {};
    }

    let conversationHistory = [];
    let currentEvidence = [];

    document.addEventListener('DOMContentLoaded', function () {
        const submitBtn = document.getElementById('submit-btn');
        const queryInput = document.getElementById('query-input');
        const clearBtn = document.getElementById('clear-btn');

        if (submitBtn) submitBtn.addEventListener('click', submitQuery);
        if (queryInput) {
            queryInput.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') submitQuery();
            });
        }
        if (clearBtn) clearBtn.addEventListener('click', clearConversation);
    });

    function applyStarterPrompt(promptText) {
        const input = document.getElementById('query-input');
        if (input) {
            input.value = promptText;
            submitQuery();
        }
    }

    function submitQuery() {
        const queryInput = document.getElementById('query-input');
        const submitBtn = document.getElementById('submit-btn');
        const query = queryInput.value.trim();

        if (!query) {
            showError('Please enter a question');
            return;
        }

        // Hide welcome state
        const welcomeState = document.getElementById('welcome-state');
        if (welcomeState) welcomeState.style.display = 'none';

        // Add user message
        addMessage('user', query);

        // Clear input and disable button
        queryInput.value = '';
        submitBtn.disabled = true;

        // Show loading bubble
        const loadingId = addLoadingBubble();

        fetch('/api/rag/ask', {
            method: 'POST',
            headers: Object.assign({
                'Content-Type': 'application/json'
            }, getCsrfHeaders()),
            body: JSON.stringify({
                query: query,
                topK: 5
            })
        })
        .then(r => {
            if (!r.ok) throw new Error('API error: ' + r.status);
            return r.json();
        })
        .then(data => {
            removeMessage(loadingId);
            submitBtn.disabled = false;

            // Store retrieved evidence for citation resolution
            currentEvidence = data.evidence || [];

            // Display evidence chunks in sidebar
            displayEvidence(currentEvidence);

            // Render Markdown-formatted answer with interactive validated citation pills
            const answerHtml = formatAnswer(data);
            addMessage('assistant', answerHtml);

            // Update session telemetry metrics
            displayMetrics(data);

            conversationHistory.push({
                query: query,
                answer: data.answer,
                citations: data.citations,
                evidence: data.evidence,
                confidence: data.confidence != null ? data.confidence : data.confidenceScore,
                timestamp: new Date()
            });
        })
        .catch(e => {
            removeMessage(loadingId);
            submitBtn.disabled = false;
            console.error('Error:', e);
            addMessage('assistant', `<div class="alert alert-danger mb-0" style="font-size: 13px;"><i class="fas fa-exclamation-triangle me-1"></i> Failed to generate answer: \${escapeHtml(e.message)}</div>`);
            showError('Failed to generate answer: ' + e.message);
        });
    }

    /**
     * Parse and validate all citation references from the answer text against retrieved evidence.
     */
    function extractAndValidateCitations(answerText, evidence) {
        if (!answerText) return { valid: [], unresolved: [], totalFound: 0 };
        const pattern = /[\[\u3010](?:Source|Citation|Chunk)?[\s\h\xA0\u202F]*#?(\d+)[\]\u3011]/gi;
        const evidenceCount = (evidence && Array.isArray(evidence)) ? evidence.length : 0;
        const seenValid = new Set();
        const seenUnresolved = new Set();
        let match;
        while ((match = pattern.exec(answerText)) !== null) {
            const num = parseInt(match[1], 10);
            if (num >= 1 && num <= evidenceCount) {
                seenValid.add(num);
            } else {
                seenUnresolved.add(num);
            }
        }
        return {
            valid: Array.from(seenValid).sort((a, b) => a - b),
            unresolved: Array.from(seenUnresolved).sort((a, b) => a - b),
            totalFound: seenValid.size + seenUnresolved.size
        };
    }

    /**
     * Safe Markdown Parser & Sanitizer
     * Escapes raw HTML to prevent XSS, converts markdown syntax, and embeds interactive citation pills.
     */
    function renderSafeMarkdown(rawText) {
        if (!rawText) return '';

        // 1. First escape all raw HTML characters to prevent XSS
        let safe = escapeHtml(rawText);

        // 2. Fenced code blocks ```lang\ncode\n```
        safe = safe.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, function(match, lang, code) {
            return `<pre><code class="language-\${lang}">\${code.trim()}</code></pre>`;
        });

        // 3. Inline code `code`
        safe = safe.replace(/`([^`\n]+)`/g, '<code>$1</code>');

        // 4. Headers: ###, ##, #
        safe = safe.replace(/^### (.*$)/gim, '<h3>$1</h3>');
        safe = safe.replace(/^## (.*$)/gim, '<h2>$1</h2>');
        safe = safe.replace(/^# (.*$)/gim, '<h1>$1</h1>');

        // 5. Bold & Italics
        safe = safe.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>');
        safe = safe.replace(/__([^_\n]+)__/g, '<strong>$1</strong>');
        safe = safe.replace(/(^|[^*])\*([^*\n]+)\*([^*]|$)/g, '$1<em>$2</em>$3');
        safe = safe.replace(/(^|[^_])_([^_\n]+)_([^_]|$)/g, '$1<em>$2</em>$3');

        // 6. Unordered lists: lines starting with * or -
        safe = safe.replace(/^\s*[-*]\s+(.+)$/gim, '<li>$1</li>');
        safe = safe.replace(/(<li>.*<\/li>)/gms, '<ul>$1</ul>');
        safe = safe.replace(/<\/ul>\s*<ul>/g, '');

        // 7. Ordered lists: lines starting with 1. 2. etc
        safe = safe.replace(/^\s*\d+\.\s+(.+)$/gim, '<li class="ol-item">$1</li>');
        safe = safe.replace(/(<li class="ol-item">.*<\/li>)/gms, '<ol>$1</ol>');
        safe = safe.replace(/<\/ol>\s*<ol>/g, '');

        // 8. Paragraphs: double newline to <p>
        const paragraphs = safe.split(/\n{2,}/);
        safe = paragraphs.map(p => {
            p = p.trim();
            if (!p) return '';
            if (p.startsWith('<h1>') || p.startsWith('<h2>') || p.startsWith('<h3>') ||
                p.startsWith('<ul>') || p.startsWith('<ol>') || p.startsWith('<pre>')) {
                return p;
            }
            return `<p>\${p.replace(/\n/g, '<br>')}</p>`;
        }).join('');

        // 9. Interactive Citation Pills: [Source N], [Citation N], [N], and Chinese brackets 【Source N】
        safe = safe.replace(/([\[\u3010](?:Source|Citation|Chunk)?[\s\h\xA0\u202F]*#?(\d+)[\]\u3011])/gi, function(match, full, p1) {
            const sNum = parseInt(p1, 10);
            const idx = sNum - 1;
            const evidenceCount = (currentEvidence && Array.isArray(currentEvidence)) ? currentEvidence.length : 0;
            if (sNum >= 1 && sNum <= evidenceCount) {
                const evidenceItem = currentEvidence[idx];
                const docTitle = evidenceItem && (evidenceItem.documentTitle || evidenceItem.documentKey) ? ` - \${escapeHtml(evidenceItem.documentTitle || evidenceItem.documentKey)}` : '';
                return `<span class="citation-pill" onclick="highlightEvidence(\${idx})" title="Jump to Source \${sNum}\${docTitle} in Evidence"><i class="fas fa-bookmark me-1" style="font-size: 9px;"></i>Source \${sNum}</span>`;
            } else {
                return `<span class="citation-pill unresolved" title="Unresolved citation: Source \${sNum} was not found in retrieved evidence (\${evidenceCount} sources retrieved)"><i class="fas fa-exclamation-triangle me-1 text-warning" style="font-size: 9px;"></i>[Source \${sNum} - Unresolved]</span>`;
            }
        });

        return safe;
    }

    function formatAnswer(data) {
        if (!data || !data.answer) {
            return '<div class="alert alert-warning mb-0" style="font-size: 13px;">No authoritative answer could be derived from indexed evidence.</div>';
        }

        let formatted = `<div class="markdown-content">\${renderSafeMarkdown(data.answer)}</div>`;

        // Cross-validate citations from answer or backend citations DTO
        const evidence = data.evidence || currentEvidence || [];
        const citResult = extractAndValidateCitations(data.answer, evidence);

        if (citResult.valid.length > 0) {
            formatted += '<div class="mt-3 pt-2 border-top" style="border-color: #f1f5f9 !important;">';
            formatted += '<div class="text-muted mb-2 d-flex align-items-center gap-1" style="font-size: 11px; font-weight: 600;"><i class="fas fa-check-circle text-success"></i> Referenced Evidence Sources:</div>';
            formatted += '<div class="d-flex flex-wrap gap-1">';
            citResult.valid.forEach((sNum) => {
                const idx = sNum - 1;
                const evidenceItem = evidence[idx];
                const srcTitle = (evidenceItem && (evidenceItem.documentTitle || evidenceItem.documentKey)) ? escapeHtml(evidenceItem.documentTitle || evidenceItem.documentKey) : `Source \${sNum}`;
                formatted += `<span class="citation-pill" onclick="highlightEvidence(\${idx})" title="Jump to \${srcTitle}"><i class="fas fa-bookmark me-1" style="font-size: 9px;"></i>[\${sNum}] \${srcTitle}</span>`;
            });
            formatted += '</div></div>';
        }

        if (citResult.unresolved.length > 0) {
            formatted += '<div class="mt-2 pt-1">';
            formatted += `<small class="text-warning d-flex align-items-center gap-1" style="font-size: 11px;"><i class="fas fa-exclamation-circle"></i> Unresolved citation reference(s): \${citResult.unresolved.map(n => '[' + n + ']').join(', ')}</small>`;
            formatted += '</div>';
        }

        return formatted;
    }

    function addMessage(sender, contentHtml) {
        const container = document.getElementById('message-container');
        if (!container) return;

        const row = document.createElement('div');
        row.className = `message-row \${sender}`;

        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        avatar.innerHTML = sender === 'user' ? '<i class="fas fa-user"></i>' : '<i class="fas fa-satellite"></i>';

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        if (sender === 'user') {
            bubble.textContent = contentHtml; // User input is pure text
        } else {
            bubble.innerHTML = contentHtml; // Assistant output is safe sanitized HTML
        }

        row.appendChild(avatar);
        row.appendChild(bubble);
        container.appendChild(row);
        container.scrollTop = container.scrollHeight;
    }

    function addLoadingBubble() {
        const container = document.getElementById('message-container');
        if (!container) return null;

        const id = 'loading-' + Date.now();
        const row = document.createElement('div');
        row.className = 'message-row assistant';
        row.id = id;

        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        avatar.innerHTML = '<i class="fas fa-satellite"></i>';

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble text-muted d-flex align-items-center gap-2';
        bubble.innerHTML = `
            <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
            <span style="font-size: 13px;">Analyzing indexed documents and generating grounded answer...</span>
        `;

        row.appendChild(avatar);
        row.appendChild(bubble);
        container.appendChild(row);
        container.scrollTop = container.scrollHeight;
        return id;
    }

    function removeMessage(id) {
        if (!id) return;
        const el = document.getElementById(id);
        if (el) el.remove();
    }

    function displayEvidence(evidence) {
        const container = document.getElementById('evidence-panel');
        const countBadge = document.getElementById('evidence-count-badge');
        if (!container) return;

        container.textContent = '';

        if (!evidence || !Array.isArray(evidence) || evidence.length === 0) {
            if (countBadge) countBadge.textContent = '0 sources';
            const empty = document.createElement('div');
            empty.className = 'empty-state py-4 text-center';
            empty.innerHTML = `
                <i class="fas fa-inbox text-light mb-2" style="font-size: 26px;"></i>
                <div style="font-size: 12.5px; color: #64748b;">No evidence chunks met the similarity threshold.</div>
            `;
            container.appendChild(empty);
            return;
        }

        if (countBadge) countBadge.textContent = `\${evidence.length} sources`;

        evidence.forEach((item, index) => {
            const itemBox = document.createElement('div');
            itemBox.className = 'evidence-item-box';
            itemBox.id = `evidence-item-\${index}`;
            itemBox.setAttribute('data-source-index', index);
            itemBox.setAttribute('data-source-number', index + 1);

            const header = document.createElement('div');
            header.className = 'evidence-item-header';

            const sourceTitle = document.createElement('span');
            sourceTitle.className = 'evidence-item-source';
            sourceTitle.textContent = `Source \${index + 1}`;

            const badgeContainer = document.createElement('div');
            badgeContainer.className = 'd-flex align-items-center gap-1';

            // Dense+Lexical retrieval method badge
            const methodBadge = document.createElement('span');
            methodBadge.className = 'badge bg-primary-subtle text-primary border border-primary-subtle';
            methodBadge.style.fontSize = '9.5px';
            methodBadge.style.padding = '2px 5px';
            methodBadge.textContent = 'Dense+Lexical';
            badgeContainer.appendChild(methodBadge);

            // Retrieval score or relevance percentage
            if (item.relevanceScore != null) {
                const scoreBadge = document.createElement('span');
                scoreBadge.className = 'evidence-item-score';
                const scoreVal = item.relevanceScore;
                if (scoreVal <= 0.05) {
                    scoreBadge.textContent = `RRF \${scoreVal.toFixed(3)}`;
                } else {
                    scoreBadge.textContent = `\${(scoreVal * 100).toFixed(1)}%`;
                }
                badgeContainer.appendChild(scoreBadge);
            }

            header.appendChild(sourceTitle);
            header.appendChild(badgeContainer);
            itemBox.appendChild(header);

            // Document Title or URL
            const docTitleText = item.documentTitle || item.documentKey || (item.sourceUrl ? item.sourceUrl : '');
            if (docTitleText) {
                const docRow = document.createElement('div');
                docRow.style.fontSize = '12px';
                docRow.style.fontWeight = '600';
                docRow.style.color = '#1e293b';
                docRow.style.marginBottom = '6px';
                docRow.style.display = 'flex';
                docRow.style.alignItems = 'center';
                docRow.style.gap = '5px';

                if (item.sourceUrl) {
                    const docLink = document.createElement('a');
                    docLink.href = item.sourceUrl;
                    docLink.target = '_blank';
                    docLink.rel = 'noopener noreferrer';
                    docLink.style.color = '#2563eb';
                    docLink.style.textDecoration = 'none';
                    docLink.title = item.sourceUrl;
                    docLink.innerHTML = `<i class="fas fa-external-link-alt" style="font-size: 10px;"></i> \${escapeHtml(docTitleText)}`;
                    docRow.appendChild(docLink);
                } else {
                    docRow.innerHTML = `<i class="fas fa-file-alt text-muted" style="font-size: 10.5px;"></i> <span>\${escapeHtml(docTitleText)}</span>`;
                }
                itemBox.appendChild(docRow);
            }

            // Readable excerpt of the retrieved chunk
            const rawContent = (item.content || item.chunkText || item.text || '').trim();
            if (rawContent) {
                const text = document.createElement('div');
                text.className = 'evidence-item-text';
                text.textContent = rawContent;
                itemBox.appendChild(text);
            }

            // Metadata footer: Page number, Chunk ID, Revision, Section Title
            const meta = document.createElement('div');
            meta.className = 'evidence-item-meta';

            if (item.pageNumber != null) {
                const pagePill = document.createElement('span');
                pagePill.className = 'evidence-meta-pill';
                pagePill.textContent = `Page \${item.pageNumber}`;
                meta.appendChild(pagePill);
            }

            const chunkIdVal = item.chunkId != null ? item.chunkId : item.chunkIndex;
            if (chunkIdVal != null) {
                const chunkPill = document.createElement('span');
                chunkPill.className = 'evidence-meta-pill';
                chunkPill.textContent = `Chunk #\${chunkIdVal}`;
                meta.appendChild(chunkPill);
            }

            const revIdVal = item.revisionId != null ? item.revisionId : item.revisionNumber;
            if (revIdVal != null) {
                const revPill = document.createElement('span');
                revPill.className = 'evidence-meta-pill';
                revPill.textContent = `Rev \${revIdVal}`;
                meta.appendChild(revPill);
            }

            if (item.sectionTitle) {
                const secPill = document.createElement('span');
                secPill.className = 'evidence-meta-pill';
                secPill.textContent = `Sec: \${item.sectionTitle}`;
                meta.appendChild(secPill);
            }

            if (meta.children.length > 0) {
                itemBox.appendChild(meta);
            }

            container.appendChild(itemBox);
        });
    }

    function highlightEvidence(index) {
        const item = document.getElementById(`evidence-item-\${index}`);
        if (!item) {
            showError(`Evidence Source \${index + 1} is not available in the Retrieved Evidence panel.`);
            return;
        }

        // Remove previous highlights
        document.querySelectorAll('.evidence-item-box').forEach(el => el.classList.remove('highlighted'));

        item.classList.add('highlighted');
        item.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function displayMetrics(data) {
        const confBadge = document.getElementById('confidence-badge');
        const valConf = document.getElementById('val-confidence');
        const valCit = document.getElementById('val-citations');

        const evidence = data.evidence || currentEvidence || [];
        const citResult = extractAndValidateCitations(data.answer || '', evidence);

        const validCount = citResult.valid.length;
        const unresCount = citResult.unresolved.length;

        // Telemetry citations count: actual validated citations
        if (valCit) {
            if (unresCount > 0) {
                valCit.innerHTML = `<span class="text-success">\${validCount} active</span> <span style="font-size: 11px; color: #f59e0b;" title="\${unresCount} unresolved citation(s) in answer">(\${unresCount} unres)</span>`;
            } else {
                valCit.textContent = `\${validCount} active`;
            }
        }

        // Calibrated confidence
        const rawConf = data.confidence != null ? data.confidence : data.confidenceScore;
        let confPct = '-';
        if (rawConf != null) {
            confPct = (rawConf * 100).toFixed(0) + '%';
        } else if (validCount > 0) {
            confPct = '85%';
        } else {
            confPct = '0%';
        }
        if (valConf) valConf.textContent = confPct;

        if (confBadge) {
            if (validCount > 0 && (rawConf == null || rawConf >= 0.5)) {
                confBadge.className = 'badge bg-success-subtle text-success';
                confBadge.textContent = `Grounded (\${validCount} sources)`;
            } else if (validCount > 0) {
                confBadge.className = 'badge bg-warning-subtle text-warning';
                confBadge.textContent = `Partial (\${validCount} sources)`;
            } else if (data.answer && data.answer.toLowerCase().includes('insufficient')) {
                confBadge.className = 'badge bg-secondary-subtle text-secondary';
                confBadge.textContent = 'Abstained';
            } else {
                confBadge.className = 'badge bg-secondary-subtle text-secondary';
                confBadge.textContent = 'Ready';
            }
        }
    }

    function clearConversation() {
        currentEvidence = [];
        const container = document.getElementById('message-container');
        if (!container) return;

        container.innerHTML = `
            <div class="empty-state my-auto" id="welcome-state">
                <div class="empty-state-icon">
                    <i class="fas fa-satellite text-primary"></i>
                </div>
                <div class="empty-state-title">Welcome to SpaceDoc Intelligence</div>
                <p style="font-size: 13.5px; max-width: 480px; margin: 0 auto 16px;">
                    Ask technical questions across your ingested ISRO mission specifications, reports, and orbital data.
                </p>
                <div class="starter-prompts justify-content-center">
                    <div class="starter-chip" onclick="applyStarterPrompt('What is Chandrayaan-3\\'s propulsion system?')">
                        <i class="fas fa-fire-alt text-warning"></i> Chandrayaan-3 Propulsion
                    </div>
                    <div class="starter-chip" onclick="applyStarterPrompt('What observations did Aditya-L1 make according to ISRO?')">
                        <i class="fas fa-sun text-warning"></i> Aditya-L1 Observations
                    </div>
                    <div class="starter-chip" onclick="applyStarterPrompt('Tell me about the CE-20 cryogenic upper stage engine.')">
                        <i class="fas fa-cog text-info"></i> CE-20 Cryogenic Engine
                    </div>
                </div>
            </div>
        `;

        const evidencePanel = document.getElementById('evidence-panel');
        if (evidencePanel) {
            evidencePanel.innerHTML = `
                <div class="empty-state py-4 text-center">
                    <i class="fas fa-inbox text-light mb-2" style="font-size: 26px;"></i>
                    <div style="font-size: 12.5px; color: #64748b;">Retrieved evidence chunks will populate here.</div>
                </div>
            `;
        }

        const countBadge = document.getElementById('evidence-count-badge');
        if (countBadge) countBadge.textContent = '0 sources';

        conversationHistory = [];
        showSuccess('Conversation history reset.');
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return String(text).replace(/[&<>"']/g, m => map[m]);
    }

    function showError(msg) {
        const box = document.getElementById('error-message');
        const text = document.getElementById('error-text');
        if (box && text) {
            text.textContent = msg;
            box.classList.add('show');
            setTimeout(() => box.classList.remove('show'), 6000);
        }
    }

    function showSuccess(msg) {
        const box = document.getElementById('success-message');
        const text = document.getElementById('success-text');
        if (box && text) {
            text.textContent = msg;
            box.classList.add('show');
            setTimeout(() => box.classList.remove('show'), 4000);
        }
    }
</script>