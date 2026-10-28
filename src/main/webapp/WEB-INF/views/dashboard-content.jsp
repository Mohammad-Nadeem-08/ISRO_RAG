<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<style>
    .capability-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 12px 14px;
        background: #f8fafc;
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-md);
        margin-bottom: 10px;
        transition: all 0.2s ease;
    }

    .capability-item:hover {
        background: #ffffff;
        border-color: var(--border-hover);
        box-shadow: 0 2px 6px rgba(0,0,0,0.03);
    }

    .cap-icon {
        width: 32px;
        height: 32px;
        border-radius: var(--radius-sm);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        flex-shrink: 0;
    }
</style>

<div class="page-header">
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2">
        <div>
            <h1 class="page-title">Operational Dashboard</h1>
            <p class="page-subtitle">Real-time health, knowledge repository metrics, and RAG pipeline telemetry</p>
        </div>
        <div class="d-flex gap-2">
            <a href="/research-assistant" class="button-primary">
                <i class="fas fa-comments"></i> Launch Assistant
            </a>
            <a href="/ingestion-centre" class="button-secondary">
                <i class="fas fa-upload"></i> Ingest Data
            </a>
        </div>
    </div>
</div>

<!-- Metrics Overview Row -->
<div class="row g-3 mb-4">
    <div class="col-sm-6 col-xl-3">
        <div class="metric-card">
            <div class="metric-icon-box metric-icon-blue">
                <i class="fas fa-file-alt"></i>
            </div>
            <div class="metric-value" id="metric-documents">-</div>
            <div class="metric-label">Indexed Documents</div>
        </div>
    </div>
    
    <div class="col-sm-6 col-xl-3">
        <div class="metric-card">
            <div class="metric-icon-box metric-icon-purple">
                <i class="fas fa-code-branch"></i>
            </div>
            <div class="metric-value" id="metric-revisions">-</div>
            <div class="metric-label">Document Revisions</div>
        </div>
    </div>
    
    <div class="col-sm-6 col-xl-3">
        <div class="metric-card">
            <div class="metric-icon-box metric-icon-emerald">
                <i class="fas fa-cubes"></i>
            </div>
            <div class="metric-value" id="metric-chunks">-</div>
            <div class="metric-label">Vector Chunks (768d)</div>
        </div>
    </div>
    
    <div class="col-sm-6 col-xl-3">
        <div class="metric-card">
            <div class="metric-icon-box metric-icon-amber">
                <i class="fas fa-server"></i>
            </div>
            <div class="metric-value" id="metric-status" style="font-size: 22px;">CHECKING</div>
            <div class="metric-label">System Health & LLM</div>
        </div>
    </div>
</div>

<!-- Main Activity & Architecture Panels -->
<div class="row g-4">
    <!-- Recent Documents Panel -->
    <div class="col-lg-7">
        <div class="content-card">
            <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                <h5 class="fw-bold mb-0" style="font-size: 15px;">
                    <i class="fas fa-folder-open text-primary me-2"></i> Recent Ingested Documents
                </h5>
                <a href="/document-library" class="text-primary text-decoration-none fw-semibold" style="font-size: 12.5px;">
                    View All Library <i class="fas fa-arrow-right ms-1"></i>
                </a>
            </div>

            <div id="recent-documents-list">
                <div class="empty-state py-4">
                    <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                    <span class="ms-2 text-muted" style="font-size: 13px;">Loading repository documents...</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Active RAG Capabilities & System Status -->
    <div class="col-lg-5">
        <div class="content-card">
            <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                <h5 class="fw-bold mb-0" style="font-size: 15px;">
                    <i class="fas fa-microchip text-success me-2"></i> RAG Pipeline Architecture
                </h5>
                <span class="badge bg-success-subtle text-success border border-success-subtle" style="font-size: 10px;">ACTIVE</span>
            </div>

            <div class="capability-item">
                <div class="cap-icon bg-primary-subtle text-primary">
                    <i class="fas fa-brain"></i>
                </div>
                <div>
                    <strong class="d-block text-dark" style="font-size: 13px;">Groq API Generation</strong>
                    <small class="text-muted" style="font-size: 11.5px;">Model: <code>openai/gpt-oss-120b</code> with factual citation grounding</small>
                </div>
            </div>

            <div class="capability-item">
                <div class="cap-icon bg-info-subtle text-info">
                    <i class="fas fa-vector-square"></i>
                </div>
                <div>
                    <strong class="d-block text-dark" style="font-size: 13px;">Dense Embeddings & pgvector</strong>
                    <small class="text-muted" style="font-size: 11.5px;">Ollama <code>nomic-embed-text</code> (768 dimensions, cosine similarity)</small>
                </div>
            </div>

            <div class="capability-item">
                <div class="cap-icon bg-purple-subtle text-purple" style="background: rgba(124, 58, 237, 0.1); color: #7c3aed;">
                    <i class="fas fa-random"></i>
                </div>
                <div>
                    <strong class="d-block text-dark" style="font-size: 13px;">Reciprocal Rank Fusion (RRF)</strong>
                    <small class="text-muted" style="font-size: 11.5px;">Hybrid search combining dense vector space and lexical keyword scoring</small>
                </div>
            </div>

            <div class="capability-item mb-0">
                <div class="cap-icon bg-success-subtle text-success">
                    <i class="fas fa-shield-virus"></i>
                </div>
                <div>
                    <strong class="d-block text-dark" style="font-size: 13px;">SSRF-Guarded URL Ingestion</strong>
                    <small class="text-muted" style="font-size: 11.5px;">Sub-chunking text safety and private subnet address filtering</small>
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
    // 1. Total Documents
    fetch('/api/documents?page=0&size=1')
        .then(r => r.json())
        .then(data => {
            const count = data.totalElements != null ? data.totalElements : 0;
            document.getElementById('metric-documents').textContent = count;
        })
        .catch(e => {
            console.error('Error loading document count:', e);
            document.getElementById('metric-documents').textContent = '0';
        });

    // 2. Total Revisions & Chunks
    fetch('/api/documents?page=0&size=100')
        .then(r => r.json())
        .then(data => {
            let totalRevisions = 0;
            let totalChunks = 0;
            if (data.content && Array.isArray(data.content)) {
                totalRevisions = data.content.reduce((sum, doc) => sum + (doc.currentRevision || 1), 0);
                totalChunks = data.content.reduce((sum, doc) => sum + (doc.totalChunks || 0), 0);
            }
            document.getElementById('metric-revisions').textContent = totalRevisions;
            document.getElementById('metric-chunks').textContent = totalChunks;
        })
        .catch(e => {
            console.error('Error loading revisions & chunks:', e);
            document.getElementById('metric-revisions').textContent = '0';
            document.getElementById('metric-chunks').textContent = '0';
        });
}

function loadSystemHealth() {
    fetch('/api/system/health')
        .then(r => r.json())
        .then(data => {
            const statusElem = document.getElementById('metric-status');
            const isUp = data.status === 'UP';
            statusElem.textContent = isUp ? 'OPERATIONAL' : 'DEGRADED';
            statusElem.style.color = isUp ? '#10b981' : '#ef4444';
            statusElem.style.fontSize = '20px';
        })
        .catch(e => {
            console.error('Error loading health:', e);
            const statusElem = document.getElementById('metric-status');
            statusElem.textContent = 'CHECK FAILED';
            statusElem.style.color = '#ef4444';
            statusElem.style.fontSize = '18px';
        });
}

function loadRecentDocuments() {
    fetch('/api/documents?page=0&size=5')
        .then(r => r.json())
        .then(data => {
            const container = document.getElementById('recent-documents-list');

            if (!data.content || data.content.length === 0) {
                container.innerHTML = `
                    <div class="empty-state py-4 text-center">
                        <i class="fas fa-inbox text-light mb-2" style="font-size: 32px;"></i>
                        <div class="fw-semibold text-dark" style="font-size: 14px;">No documents indexed yet</div>
                        <p class="text-muted mb-2" style="font-size: 12.5px;">Upload mission documentation in Ingestion Centre to get started.</p>
                        <a href="/ingestion-centre" class="button-primary py-1 px-3" style="font-size: 12px;">
                            <i class="fas fa-upload"></i> Upload First PDF
                        </a>
                    </div>
                `;
                return;
            }

            let html = '<div class="d-flex flex-direction-column gap-2">';
            data.content.forEach(doc => {
                const isWeb = doc.sourceType === 'WEB' || (doc.sourceUrl && doc.sourceUrl.length > 0);
                const icon = isWeb ? 'fa-globe text-info' : 'fa-file-pdf text-danger';

                html += `
                    <div class="p-3 bg-light rounded d-flex align-items-center justify-content-between mb-2" style="border: 1px solid var(--border-subtle); transition: all 0.2s ease;">
                        <div class="d-flex align-items-center gap-3 text-truncate me-2">
                            <i class="fas \${icon} fs-5"></i>
                            <div class="text-truncate">
                                <a href="/document-library" class="text-dark fw-bold text-decoration-none text-truncate d-block" style="font-size: 13.5px;" title="\${escapeHtml(doc.title)}">
                                    \${escapeHtml(doc.title)}
                                </a>
                                <small class="text-muted" style="font-size: 11.5px;">
                                    Rev \${doc.currentRevision || 1} • \${doc.totalChunks || 0} chunks • \${escapeHtml(doc.sourceOrganization || 'ISRO')}
                                </small>
                            </div>
                        </div>
                        <span class="badge bg-white text-secondary border shadow-sm" style="font-size: 11px;">Active</span>
                    </div>
                `;
            });
            html += '</div>';

            container.innerHTML = html;
        })
        .catch(e => {
            console.error('Error loading recent documents:', e);
            document.getElementById('recent-documents-list').innerHTML =
                '<div class="alert alert-danger py-2 mb-0" style="font-size: 12.5px;">Failed to load documents</div>';
        });
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
</script>
