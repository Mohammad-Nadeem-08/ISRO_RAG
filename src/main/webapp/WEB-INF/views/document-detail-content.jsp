<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

        <div class="page-header">
            <h1 class="page-title" id="doc-title">Document Detail</h1>
            <p class="page-subtitle">View detailed information about this document</p>
        </div>

        <!-- Loading -->
        <div id="loading-indicator" class="loading-spinner show">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2 text-muted">Loading document details...</p>
        </div>

        <!-- Error Message -->
        <div id="error-message" class="error-message"></div>

        <!-- Content (hidden until loaded) -->
        <div id="content" style="display: none;">
            <div class="row">
                <div class="col-md-8">
                    <div class="metric-card mb-4">
                        <h5 class="mb-3"><i class="fas fa-info-circle"></i> Basic Information</h5>
                        <table class="table table-sm">
                            <tr>
                                <td style="width: 30%;"><strong>Title</strong></td>
                                <td id="doc-title-detail">-</td>
                            </tr>
                            <tr>
                                <td><strong>Document Key</strong></td>
                                <td id="doc-key">-</td>
                            </tr>
                            <tr>
                                <td><strong>Source Type</strong></td>
                                <td id="doc-source-type">-</td>
                            </tr>
                            <tr>
                                <td><strong>Organization</strong></td>
                                <td id="doc-org">-</td>
                            </tr>
                            <tr>
                                <td><strong>Source URL</strong></td>
                                <td>
                                    <a id="doc-url" href="#" target="_blank" style="word-break: break-all;">-</a>
                                </td>
                            </tr>
                        </table>
                    </div>

                    <div class="metric-card mb-4">
                        <h5 class="mb-3"><i class="fas fa-cube"></i> Content Metrics</h5>
                        <div class="row">
                            <div class="col-md-3 text-center">
                                <div style="font-size: 32px; font-weight: 700; color: #2563eb;" id="current-revision">-
                                </div>
                                <small style="color: #64748b; text-transform: uppercase;">Current Revision</small>
                            </div>
                            <div class="col-md-3 text-center">
                                <div style="font-size: 32px; font-weight: 700; color: #2563eb;" id="total-chunks">-
                                </div>
                                <small style="color: #64748b; text-transform: uppercase;">Total Chunks</small>
                            </div>
                            <div class="col-md-3 text-center">
                                <div style="font-size: 32px; font-weight: 700; color: #2563eb;" id="indexed-status">-
                                </div>
                                <small style="color: #64748b; text-transform: uppercase;">Status</small>
                            </div>
                            <div class="col-md-3 text-center">
                                <div style="font-size: 32px; font-weight: 700; color: #2563eb;" id="avg-chunk-size">-
                                </div>
                                <small style="color: #64748b; text-transform: uppercase;">Avg Chunk Size</small>
                            </div>
                        </div>
                    </div>

                    <div class="metric-card">
                        <h5 class="mb-3"><i class="fas fa-code-branch"></i> Revision History</h5>
                        <div id="revision-list">
                            <small class="text-muted">Loading revisions...</small>
                        </div>
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="metric-card">
                        <h5 class="mb-3"><i class="fas fa-chart-bar"></i> Quick Stats</h5>
                        <div style="font-size: 13px;">
                            <div style="margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #e2e8f0;">
                                <small style="color: #64748b; display: block; margin-bottom: 4px;">Latest Update</small>
                                <strong id="last-update">-</strong>
                            </div>
                            <div style="margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #e2e8f0;">
                                <small style="color: #64748b; display: block; margin-bottom: 4px;">Estimated
                                    Tokens</small>
                                <strong id="estimated-tokens">-</strong>
                            </div>
                            <div style="margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #e2e8f0;">
                                <small style="color: #64748b; display: block; margin-bottom: 4px;">Searchable
                                    Content</small>
                                <strong id="searchable">Yes</strong>
                            </div>
                            <div>
                                <small style="color: #64748b; display: block; margin-bottom: 4px;">Embedded</small>
                                <strong id="embedded">Pending</strong>
                            </div>
                        </div>
                    </div>

                    <div class="metric-card mt-3">
                        <h5 class="mb-3"><i class="fas fa-actions"></i> Actions</h5>
                        <button class="button-secondary" style="width: 100%; margin-bottom: 8px;"
                            onclick="downloadDocument()">
                            <i class="fas fa-download"></i> Download
                        </button>
                        <button class="button-secondary" style="width: 100%; margin-bottom: 8px;"
                            onclick="searchInDocument()">
                            <i class="fas fa-search"></i> Search Content
                        </button>
                        <button class="button-secondary" style="width: 100%;" onclick="deleteDocument()">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <script>
            const urlParams = new URLSearchParams(window.location.search);
            const docId = urlParams.get('id');

            document.addEventListener('DOMContentLoaded', function () {
                if (!docId) {
                    showError('No document ID provided');
                    return;
                }
                loadDocumentDetail();
            });

            function loadDocumentDetail() {
                fetch(`/api/documents/${docId}`)
                    .then(r => {
                        if (!r.ok) throw new Error('Failed to load document');
                        return r.json();
                    })
                    .then(doc => {
                        document.getElementById('loading-indicator').style.display = 'none';
                        document.getElementById('content').style.display = 'block';

                        // Populate basic info
                        document.getElementById('doc-title').textContent = escapeHtml(doc.title || 'Document');
                        document.getElementById('doc-title-detail').textContent = escapeHtml(doc.title || '-');
                        document.getElementById('doc-key').textContent = escapeHtml(doc.documentKey || '-');
                        document.getElementById('doc-source-type').textContent = escapeHtml(doc.sourceType || '-');
                        document.getElementById('doc-org').textContent = escapeHtml(doc.sourceOrganization || '-');

                        // Source URL
                        const urlElem = document.getElementById('doc-url');
                        if (doc.sourceUrl) {
                            urlElem.href = doc.sourceUrl;
                            urlElem.textContent = escapeHtml(doc.sourceUrl);
                        } else {
                            urlElem.textContent = 'N/A';
                        }

                        // Content metrics
                        document.getElementById('current-revision').textContent = doc.currentRevision || 1;
                        document.getElementById('total-chunks').textContent = doc.totalChunks || 0;
                        document.getElementById('indexed-status').textContent = 'Indexed';

                        // Estimated chunk size
                        const avgSize = doc.totalChunks > 0 ? Math.round(10000 / doc.totalChunks) : 0;
                        document.getElementById('avg-chunk-size').textContent = avgSize + ' chars';

                        // Stats
                        document.getElementById('last-update').textContent = new Date().toLocaleDateString();
                        const estimatedTokens = Math.round((doc.totalChunks || 0) * 150);
                        document.getElementById('estimated-tokens').textContent = estimatedTokens.toLocaleString();

                        // Revisions
                        loadRevisions();
                    })
                    .catch(e => {
                        console.error('Error:', e);
                        document.getElementById('loading-indicator').style.display = 'none';
                        showError('Failed to load document: ' + e.message);
                    });
            }

            function loadRevisions() {
                fetch(`/api/documents/${docId}/revisions`)
                    .then(r => r.json())
                    .then(data => {
                        const container = document.getElementById('revision-list');
                        if (!data.revisions || data.revisions === 0) {
                            container.innerHTML = '<small class="text-muted">No revision history available</small>';
                            return;
                        }

                        let html = '<ol style="margin: 0; padding-left: 20px;">';
                        for (let i = 1; i <= Math.min(data.revisions, 5); i++) {
                            html += `<li style="margin-bottom: 8px;"><strong>Revision ${i}</strong></li>`;
                        }
                        if (data.revisions > 5) {
                            html += `<li style="color: #64748b;">... and ${data.revisions - 5} more revisions</li>`;
                        }
                        html += '</ol>';
                        container.innerHTML = html;
                    })
                    .catch(e => {
                        console.error('Error loading revisions:', e);
                        document.getElementById('revision-list').innerHTML = '<small class="text-danger">Failed to load revisions</small>';
                    });
            }

            function downloadDocument() {
                alert('Download feature coming soon');
            }

            function searchInDocument() {
                const query = prompt('Enter search query:');
                if (query) {
                    window.location.href = `/research-assistant?doc=\${docId}&q=\${encodeURIComponent(query)}`;
                }
            }

            function deleteDocument() {
                if (confirm('Are you sure you want to delete this document? This action cannot be undone.')) {
                    alert('Delete feature requires confirmation. This will be implemented in the next phase.');
                }
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

            function showError(message) {
                const elem = document.getElementById('error-message');
                elem.textContent = message;
                elem.classList.add('show');
            }
        </script>