<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="page-header">
    <h1 class="page-title">Dashboard</h1>
    <p class="page-subtitle">Monitor your research documents and ingestion progress</p>
</div>

<!-- Metrics Grid -->
<div class="row mb-4">
    <div class="col-md-3 mb-3">
        <div class="metric-card">
            <div class="metric-icon"><i class="fas fa-file-alt"></i></div>
            <div class="metric-value" id="metric-documents">-</div>
            <div class="metric-label">Indexed Documents</div>
        </div>
    </div>
    <div class="col-md-3 mb-3">
        <div class="metric-card">
            <div class="metric-icon"><i class="fas fa-code-branch"></i></div>
            <div class="metric-value" id="metric-revisions">-</div>
            <div class="metric-label">Document Revisions</div>
        </div>
    </div>
    <div class="col-md-3 mb-3">
        <div class="metric-card">
            <div class="metric-icon"><i class="fas fa-cube"></i></div>
            <div class="metric-value" id="metric-chunks">-</div>
            <div class="metric-label">Content Chunks</div>
        </div>
    </div>
    <div class="col-md-3 mb-3">
        <div class="metric-card">
            <div class="metric-icon"><i class="fas fa-server"></i></div>
            <div class="metric-value" id="metric-status">OK</div>
            <div class="metric-label">System Health</div>
        </div>
    </div>
</div>

<!-- Recent Activity Section -->
<div class="row">
    <div class="col-md-6">
        <div class="metric-card">
            <h5 class="mb-3">
                <i class="fas fa-clock"></i> Recent Documents
            </h5>
            <div id="recent-documents-list">
                <div class="empty-state">
                    <div class="empty-state-icon">
                        <i class="fas fa-inbox"></i>
                    </div>
                    <div class="empty-state-title">No documents yet</div>
                    <p>Upload your first document to get started</p>
                </div>
            </div>
        </div>
    </div>

    <div class="col-md-6">
        <div class="metric-card">
            <h5 class="mb-3">
                <i class="fas fa-history"></i> Recent Queries
            </h5>
            <div id="recent-queries-list">
                <div class="empty-state">
                    <div class="empty-state-icon">
                        <i class="fas fa-inbox"></i>
                    </div>
                    <div class="empty-state-title">No queries yet</div>
                    <p>Try asking questions in the Research Assistant</p>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    loadDashboardMetrics();
    loadRecentDocuments();
    loadSystemHealth();
});

function loadDashboardMetrics() {
    // Load document metrics
    fetch('/api/documents?page=0&size=1')
        .then(r => r.json())
        .then(data => {
            document.getElementById('metric-documents').textContent = data.totalElements || 0;
        })
        .catch(e => {
            console.error('Error loading document metrics:', e);
            document.getElementById('metric-documents').textContent = '0';
        });

    // Load revision metrics (approximation)
    fetch('/api/documents?page=0&size=100')
        .then(r => r.json())
        .then(data => {
            let totalRevisions = 0;
            if (data.content && Array.isArray(data.content)) {
                totalRevisions = data.content.reduce((sum, doc) => sum + (doc.currentRevision || 1), 0);
            }
            document.getElementById('metric-revisions').textContent = totalRevisions;
        })
        .catch(e => {
            console.error('Error loading revision metrics:', e);
            document.getElementById('metric-revisions').textContent = '0';
        });

    // Load chunk metrics (approximation)
    fetch('/api/documents?page=0&size=100')
        .then(r => r.json())
        .then(data => {
            let totalChunks = 0;
            if (data.content && Array.isArray(data.content)) {
                totalChunks = data.content.reduce((sum, doc) => sum + (doc.totalChunks || 0), 0);
            }
            document.getElementById('metric-chunks').textContent = totalChunks;
        })
        .catch(e => {
            console.error('Error loading chunk metrics:', e);
            document.getElementById('metric-chunks').textContent = '0';
        });
}

function loadSystemHealth() {
    fetch('/api/system/health')
        .then(r => r.json())
        .then(data => {
            const status = data.status === 'UP' ? 'OK' : 'DOWN';
            document.getElementById('metric-status').textContent = status;
            document.getElementById('metric-status').parentElement.style.color =
                status === 'OK' ? '#10b981' : '#ef4444';
        })
        .catch(e => {
            console.error('Error loading system health:', e);
            document.getElementById('metric-status').textContent = 'ERROR';
        });
}

function loadRecentDocuments() {
    fetch('/api/documents?page=0&size=5')
        .then(r => r.json())
        .then(data => {
            const container = document.getElementById('recent-documents-list');

            if (!data.content || data.content.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">
                            <i class="fas fa-inbox"></i>
                        </div>
                        <div class="empty-state-title">No documents yet</div>
                        <p>Upload your first document to get started</p>
                    </div>
                `;
                return;
            }

            let html = '';
            data.content.forEach(doc => {
                html += `
                    <div class="document-meta mb-2">
                        <div class="document-title">
                            <a href="/document-library?id=\${doc.id}" style="color: #2563eb; text-decoration: none;">
                                \${escapeHtml(doc.title)}
                            </a>
                        </div>
                        <small class="text-muted">
                            \${doc.currentRevision || 1} revision(s) •
                            \${doc.totalChunks || 0} chunks
                        </small>
                    </div>
                `;
            });

            container.innerHTML = html;
        })
        .catch(e => {
            console.error('Error loading recent documents:', e);
            document.getElementById('recent-documents-list').innerHTML =
                '<div class="alert alert-danger">Failed to load documents</div>';
        });
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}
</script>
