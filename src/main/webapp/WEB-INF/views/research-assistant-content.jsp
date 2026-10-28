<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

        <style>
            .evidence-item {
                background: #ffffff;
                border: 1px solid var(--neutral-border, #e2e8f0);
                border-left: 3px solid var(--primary, #2563eb);
                border-radius: 6px;
                padding: 12px;
                margin-bottom: 12px;
                box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
                transition: all 0.2s ease;
                word-break: break-word;
                overflow-wrap: break-word;
            }
            .evidence-item:hover {
                border-color: #cbd5e1;
                border-left-color: var(--primary, #2563eb);
                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.08);
            }
            .evidence-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 6px;
                gap: 8px;
            }
            .evidence-source-title {
                font-weight: 600;
                font-size: 13px;
                color: var(--primary, #2563eb);
            }
            .evidence-score-badge {
                font-size: 10px;
                font-weight: 600;
                background: #eff6ff;
                color: #1d4ed8;
                border: 1px solid #bfdbfe;
                border-radius: 4px;
                padding: 2px 6px;
                white-space: nowrap;
            }
            .evidence-doc-title {
                font-size: 11px;
                font-weight: 600;
                color: #334155;
                margin-bottom: 4px;
                word-break: break-word;
                overflow-wrap: break-word;
            }
            .evidence-text {
                font-size: 12px;
                color: #475569;
                line-height: 1.5;
                margin-bottom: 8px;
                word-break: break-word;
                overflow-wrap: break-word;
            }
            .evidence-meta {
                display: flex;
                flex-wrap: wrap;
                gap: 6px;
                font-size: 11px;
                color: #64748b;
                border-top: 1px solid #f1f5f9;
                padding-top: 6px;
            }
            .evidence-meta-tag {
                display: inline-flex;
                align-items: center;
                background: #f8fafc;
                border: 1px solid #e2e8f0;
                border-radius: 4px;
                padding: 2px 6px;
                font-size: 10px;
                color: #64748b;
                white-space: nowrap;
            }
            .evidence-meta-tag i {
                margin-right: 4px;
                color: #94a3b8;
            }
            #evidence-panel {
                max-height: 520px;
                overflow-y: auto;
                padding-right: 4px;
            }
            #evidence-panel::-webkit-scrollbar {
                width: 5px;
            }
            #evidence-panel::-webkit-scrollbar-thumb {
                background: #cbd5e1;
                border-radius: 3px;
            }
        </style>

        <div class="page-header">
            <h1 class="page-title">Research Assistant</h1>
            <p class="page-subtitle">Ask questions about your documents</p>
        </div>

        <div class="row">
            <div class="col-md-8">
                <div class="conversation-thread">
                    <div id="message-container"
                        style="min-height: 400px; max-height: 600px; overflow-y: auto; margin-bottom: 20px;">
                        <div class="empty-state">
                            <div class="empty-state-icon">
                                <i class="fas fa-comments"></i>
                            </div>
                            <div class="empty-state-title">Welcome to Research Assistant</div>
                            <p>Ask a question about your documents to get started</p>
                        </div>
                    </div>

                    <!-- Error/Success Messages -->
                    <div id="error-message" class="error-message"></div>
                    <div id="success-message" class="success-message"></div>

                    <!-- Loading Indicator -->
                    <div id="loading-indicator" class="loading-spinner">
                        <div class="spinner-border text-primary" role="status">
                            <span class="visually-hidden">Loading...</span>
                        </div>
                        <p class="mt-2 text-muted">Generating answer...</p>
                    </div>

                    <!-- Query Input -->
                    <div class="input-group mb-3">
                        <input type="text" class="form-control" id="query-input" placeholder="Ask a question..."
                            autofocus>
                        <button class="button-primary" id="submit-btn" type="button">
                            <i class="fas fa-paper-plane"></i> Ask
                        </button>
                    </div>

                    <button class="button-secondary" id="clear-btn" type="button" style="width: 100%;">
                        <i class="fas fa-trash"></i> Clear Conversation
                    </button>
                </div>
            </div>

            <!-- Evidence Panel -->
            <div class="col-md-4">
                <div class="metric-card">
                    <h5 class="mb-3">
                        <i class="fas fa-layer-group"></i> Evidence
                    </h5>
                    <div id="evidence-panel">
                        <div class="empty-state" style="padding: 20px;">
                            <i class="fas fa-inbox" style="font-size: 24px; opacity: 0.3; margin-bottom: 10px;"></i>
                            <div style="font-size: 13px; color: #64748b;">
                                Evidence will appear here when you ask a question
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Answer Metrics -->
                <div class="metric-card mt-3">
                    <h5 class="mb-3">
                        <i class="fas fa-chart-pie"></i> Metrics
                    </h5>
                    <div id="metrics-panel">
                        <small class="text-muted">Ask a question to see metrics</small>
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

            document.addEventListener('DOMContentLoaded', function () {
                document.getElementById('submit-btn').addEventListener('click', submitQuery);
                document.getElementById('query-input').addEventListener('keypress', function (e) {
                    if (e.key === 'Enter') submitQuery();
                });
                document.getElementById('clear-btn').addEventListener('click', clearConversation);
                loadConversationHistory();
            });

            function submitQuery() {
                const query = document.getElementById('query-input').value.trim();

                if (!query) {
                    showError('Please enter a question');
                    return;
                }

                // Add user message to UI
                addMessage('user', query);

                // Clear input and show loading
                document.getElementById('query-input').value = '';
                showLoading(true);

                // Call API
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
                        showLoading(false);

                        // Add answer message
                        const answerHtml = formatAnswer(data);
                        addMessage('assistant', answerHtml);

                        // Display evidence safely
                        displayEvidence(data.evidence);

                        // Display metrics
                        displayMetrics(data);

                        // Store in history
                        conversationHistory.push({
                            query: query,
                            answer: data.answer,
                            citations: data.citations,
                            evidence: data.evidence,
                            confidence: data.confidence,
                            timestamp: new Date()
                        });

                        showSuccess('Answer generated successfully');
                    })
                    .catch(e => {
                        showLoading(false);
                        console.error('Error:', e);
                        addMessage('assistant', `<div class="alert alert-danger">Error: \${escapeHtml(e.message)}</div>`);
                        showError('Failed to generate answer: ' + e.message);
                    });
            }

            function formatAnswer(data) {
                if (!data || !data.answer) {
                    return '<div class="alert alert-warning">No answer generated</div>';
                }

                // Safely escape the answer text first to prevent XSS
                let text = escapeHtml(data.answer);

                // Replace [Source N] patterns with clickable citation badges
                text = text.replace(/\[(?:Source|Citation|Chunk)?[\\s\\h\\xA0]*#?(\\d+)\]/gi, (match, p1) => {
                    const idx = parseInt(p1, 10) - 1;
                    return `<span class="citation" onclick="highlightEvidence(\${idx})" title="Jump to Source \${p1}">Source \${p1}</span>`;
                });

                let formatted = `<div>\${text}</div>`;

                if (data.citations && Array.isArray(data.citations) && data.citations.length > 0) {
                    formatted += '<div class="mt-2 pt-2 border-top" style="border-color: rgba(0,0,0,0.06) !important;"><small class="text-muted d-block mb-1" style="font-size: 11px;">Referenced Sources:</small>';
                    data.citations.forEach((c) => {
                        const sNum = c.sourceNumber != null ? c.sourceNumber : 1;
                        const idx = sNum - 1;
                        formatted += `<span class="citation" onclick="highlightEvidence(\${idx})" title="Jump to Source \${sNum}">Source \${sNum}</span> `;
                    });
                    formatted += '</div>';
                }

                return formatted;
            }

            function displayEvidence(evidence) {
                const container = document.getElementById('evidence-panel');
                if (!container) return;

                // Safely clear previous evidence content
                container.textContent = '';

                if (!evidence || !Array.isArray(evidence) || evidence.length === 0) {
                    const emptyAlert = document.createElement('div');
                    emptyAlert.className = 'alert alert-info';
                    emptyAlert.style.fontSize = '12px';
                    emptyAlert.textContent = 'No evidence found';
                    container.appendChild(emptyAlert);
                    return;
                }

                evidence.forEach((item, idx) => {
                    if (!item) return;

                    const itemDiv = document.createElement('div');
                    itemDiv.className = 'evidence-item';
                    itemDiv.id = 'evidence-' + idx;

                    // Header: Source Title and Relevance Score Badge
                    const headerDiv = document.createElement('div');
                    headerDiv.className = 'evidence-header';

                    const sourceTitle = document.createElement('span');
                    sourceTitle.className = 'evidence-source-title';
                    sourceTitle.textContent = 'Source ' + (idx + 1);
                    headerDiv.appendChild(sourceTitle);

                    if (item.relevanceScore != null) {
                        const scoreSpan = document.createElement('span');
                        scoreSpan.className = 'evidence-score-badge';
                        const scoreVal = typeof item.relevanceScore === 'number' ? item.relevanceScore : parseFloat(item.relevanceScore);
                        const pct = scoreVal <= 0.05 ? Math.min(100, Math.round((scoreVal / 0.01639) * 100)) : Math.round(scoreVal * 100);
                        scoreSpan.textContent = 'Score: ' + (isNaN(pct) ? 'N/A' : pct + '%');
                        headerDiv.appendChild(scoreSpan);
                    }
                    itemDiv.appendChild(headerDiv);

                    // Document Title if present
                    if (item.documentTitle && item.documentTitle.trim()) {
                        const docTitleDiv = document.createElement('div');
                        docTitleDiv.className = 'evidence-doc-title';
                        docTitleDiv.textContent = item.documentTitle.trim();
                        itemDiv.appendChild(docTitleDiv);
                    }

                    // Content snippet safely inserted as textContent (NOT innerHTML)
                    const contentDiv = document.createElement('div');
                    contentDiv.className = 'evidence-text';
                    const rawContent = item.content || '';
                    const truncated = rawContent.length > 220 ? rawContent.substring(0, 220) + '...' : rawContent;
                    contentDiv.textContent = truncated;
                    itemDiv.appendChild(contentDiv);

                    // Metadata container
                    const metaDiv = document.createElement('div');
                    metaDiv.className = 'evidence-meta';

                    // Section Title
                    if (item.sectionTitle && item.sectionTitle.trim()) {
                        const sectionTag = document.createElement('span');
                        sectionTag.className = 'evidence-meta-tag';
                        sectionTag.innerHTML = '<i class="fas fa-bookmark"></i>';
                        sectionTag.appendChild(document.createTextNode('Section: ' + item.sectionTitle.trim()));
                        metaDiv.appendChild(sectionTag);
                    }

                    // Page Number
                    if (item.pageNumber != null) {
                        const pageTag = document.createElement('span');
                        pageTag.className = 'evidence-meta-tag';
                        pageTag.innerHTML = '<i class="fas fa-file-alt"></i>';
                        pageTag.appendChild(document.createTextNode('Page ' + item.pageNumber));
                        metaDiv.appendChild(pageTag);
                    }

                    // Chunk ID & Revision ID
                    if (item.chunkId != null || item.revisionId != null) {
                        const chunkTag = document.createElement('span');
                        chunkTag.className = 'evidence-meta-tag';
                        chunkTag.innerHTML = '<i class="fas fa-cube"></i>';
                        let chunkText = '';
                        if (item.chunkId != null) chunkText += 'Chunk #' + item.chunkId;
                        if (item.revisionId != null) chunkText += (chunkText ? ' (Rev ' : 'Rev ') + item.revisionId + (item.chunkId != null ? ')' : '');
                        chunkTag.appendChild(document.createTextNode(chunkText));
                        metaDiv.appendChild(chunkTag);
                    }

                    // Conflict status if any
                    if (item.conflictStatus && item.conflictStatus !== 'NO_CONFLICT') {
                        const conflictTag = document.createElement('span');
                        conflictTag.className = 'evidence-meta-tag';
                        conflictTag.style.borderColor = '#fde68a';
                        conflictTag.style.background = '#fffbeb';
                        conflictTag.style.color = '#b45309';
                        conflictTag.innerHTML = '<i class="fas fa-exclamation-triangle" style="color: #d97706;"></i>';
                        conflictTag.appendChild(document.createTextNode(item.conflictStatus));
                        metaDiv.appendChild(conflictTag);
                    }

                    itemDiv.appendChild(metaDiv);
                    container.appendChild(itemDiv);
                });
            }

            function displayMetrics(data) {
                const container = document.getElementById('metrics-panel');
                const confidence = data.confidence || 0;
                const coverage = data.citationCoverage || 0;

                const confidenceColor = confidence > 0.7 ? '#10b981' : confidence > 0.4 ? '#f59e0b' : '#ef4444';
                const coverageColor = coverage > 0.7 ? '#10b981' : coverage > 0.4 ? '#f59e0b' : '#ef4444';

                html = `
        <div>
            <div style="margin-bottom: 15px;">
                <small style="color: #64748b; display: block; margin-bottom: 4px;">Confidence</small>
                <div style="background: #f1f5f9; height: 6px; border-radius: 3px; overflow: hidden;">
                    <div style="background: \${confidenceColor}; height: 100%; width: \${confidence * 100}%; transition: width 0.3s;"></div>
                </div>
                <small style="color: #64748b; display: block; margin-top: 4px;">\${(confidence * 100).toFixed(0)}%</small>
            </div>

            <div style="margin-bottom: 15px;">
                <small style="color: #64748b; display: block; margin-bottom: 4px;">Citation Coverage</small>
                <div style="background: #f1f5f9; height: 6px; border-radius: 3px; overflow: hidden;">
                    <div style="background: \${coverageColor}; height: 100%; width: \${coverage * 100}%; transition: width 0.3s;"></div>
                </div>
                <small style="color: #64748b; display: block; margin-top: 4px;">\${(coverage * 100).toFixed(0)}%</small>
            </div>

            <div>
                <small style="color: #64748b; display: block;">Evidence Items: <strong style="color: #1e293b;">\${data.evidenceCount || 0}</strong></small>
                <small style="color: #64748b; display: block;">Citations: <strong style="color: #1e293b;">\${(data.citations && data.citations.length) || 0}</strong></small>
            </div>
        </div>
    `;

                container.innerHTML = html;
            }

            function addMessage(role, content) {
                const container = document.getElementById('message-container');

                // Clear empty state
                if (container.querySelector('.empty-state')) {
                    container.innerHTML = '';
                }

                const messageDiv = document.createElement('div');
                messageDiv.className = role === 'user' ? 'message-user' : 'message-assistant';
                messageDiv.innerHTML = role === 'user' ? escapeHtml(content) : content;

                container.appendChild(messageDiv);
                container.scrollTop = container.scrollHeight;
            }

            function clearConversation() {
                if (confirm('Clear all messages?')) {
                    document.getElementById('message-container').innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-comments"></i>
                </div>
                <div class="empty-state-title">Welcome to Research Assistant</div>
                <p>Ask a question about your documents to get started</p>
            </div>
        `;
                    document.getElementById('evidence-panel').innerHTML = `
            <div class="empty-state" style="padding: 20px;">
                <i class="fas fa-inbox" style="font-size: 24px; opacity: 0.3; margin-bottom: 10px;"></i>
                <div style="font-size: 13px; color: #64748b;">
                    Evidence will appear here when you ask a question
                </div>
            </div>
        `;
                    document.getElementById('metrics-panel').innerHTML = '<small class="text-muted">Ask a question to see metrics</small>';
                    conversationHistory = [];
                }
            }

            function loadConversationHistory() {
                // Placeholder: In a full implementation, fetch conversation history from server
                // For now, just initialize empty
                conversationHistory = [];
            }

            function showLoading(show) {
                const indicator = document.getElementById('loading-indicator');
                if (show) {
                    indicator.classList.add('show');
                } else {
                    indicator.classList.remove('show');
                }
            }

            function showError(message) {
                const elem = document.getElementById('error-message');
                elem.textContent = message;
                elem.classList.add('show');
                setTimeout(() => elem.classList.remove('show'), 5000);
            }

            function showSuccess(message) {
                const elem = document.getElementById('success-message');
                elem.textContent = message;
                elem.classList.add('show');
                setTimeout(() => elem.classList.remove('show'), 3000);
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
                return text.replace(/[&<>"']/g, m => map[m]);
            }

            function highlightEvidence(idx) {
                const elem = document.getElementById('evidence-' + idx);
                if (elem) {
                    elem.style.transition = 'all 0.3s ease';
                    elem.style.background = 'rgba(37, 99, 235, 0.12)';
                    elem.style.borderColor = 'var(--primary, #2563eb)';
                    elem.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                    setTimeout(() => {
                        elem.style.background = '';
                        elem.style.borderColor = '';
                    }, 2500);
                }
            }
        </script>