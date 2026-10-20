<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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

                        // Display evidence
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
                if (!data.answer) {
                    return '<div class="alert alert-warning">No answer generated</div>';
                }

                // Format citations as clickable elements
                let formattedAnswer = data.answer;
                if (data.citations && data.citations.length > 0) {
                    formattedAnswer += '<div class="mt-2">';
                    data.citations.forEach((citation, idx) => {
                        formattedAnswer += `<span class="citation" onclick="highlightEvidence(\${idx})">Source \${citation.sourceNumber}</span>`;
                    });
                    formattedAnswer += '</div>';
                }

                return formattedAnswer;
            }

            function displayEvidence(evidence) {
                const container = document.getElementById('evidence-panel');

                if (!evidence || evidence.length === 0) {
                    container.innerHTML = '<div class="alert alert-info">No evidence found</div>';
                    return;
                }

                let html = '';
                evidence.forEach((item, idx) => {
                    const content = escapeHtml(item.content);
                    const truncated = content.length > 150 ? content.substring(0, 150) + '...' : content;
                    html += `
            <div class="evidence-item" id="evidence-\${idx}">
                <div style="font-weight: 600; font-size: 12px; color: #2563eb;">
                    Source \${idx + 1}
                </div>
                <div style="font-size: 12px; color: #64748b; margin-top: 4px;">
                    \${truncated}
                </div>
                \${item.sectionTitle ? `< small style = "color: #94a3b8; display: block; margin-top: 4px;" > Section: \${ escapeHtml(item.sectionTitle) }</small > ` : ''}
                <small style="color: #94a3b8; display: block; margin-top: 2px;">Score: \${(item.relevanceScore * 100).toFixed(0)}%</small>
            </div>
        `;
                });

                container.innerHTML = html;
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
                    elem.style.background = 'rgba(37, 99, 235, 0.1)';
                    setTimeout(() => elem.style.background = '', 2000);
                }
            }
        </script>