<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<style>
    .library-search-card {
        background: #ffffff;
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-lg);
        box-shadow: var(--shadow-card);
        padding: 16px 20px;
        margin-bottom: 24px;
    }

    .doc-grid-card {
        background: #ffffff;
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-lg);
        padding: 18px;
        box-shadow: var(--shadow-card);
        transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        height: 100%;
        cursor: pointer;
    }

    .doc-grid-card:hover {
        transform: translateY(-2px);
        box-shadow: var(--shadow-elevated);
        border-color: var(--border-hover);
    }

    .doc-type-icon {
        width: 38px;
        height: 38px;
        border-radius: var(--radius-md);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        flex-shrink: 0;
    }

    .doc-type-pdf {
        background: rgba(239, 68, 68, 0.1);
        color: #dc2626;
    }

    .doc-type-web {
        background: rgba(14, 165, 233, 0.1);
        color: #0284c7;
    }

    .doc-title-text {
        font-size: 15px;
        font-weight: 600;
        color: var(--text-main);
        margin: 0;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        line-height: 1.4;
    }

    .doc-meta-row {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        margin: 10px 0;
        font-size: 11.5px;
        color: var(--text-muted);
    }

    .doc-badge-pill {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 2px 8px;
        border-radius: var(--radius-full);
        font-size: 11px;
        font-weight: 500;
        border: 1px solid var(--border-subtle);
        background: #f8fafc;
        color: var(--text-secondary);
    }
</style>

<div class="page-header">
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2">
        <div>
            <h1 class="page-title">Document Library</h1>
            <p class="page-subtitle">Inspect indexed ISRO technical documents, revisions, and chunks</p>
        </div>
        <a href="/ingestion-centre" class="button-primary">
            <i class="fas fa-plus"></i> Ingest New Document
        </a>
    </div>
</div>

<!-- Search Bar -->
<div class="library-search-card">
    <div class="row g-2 align-items-center">
        <div class="col-md-9">
            <div class="input-group">
                <span class="input-group-text bg-white border-end-0" style="border-color: var(--border-subtle);">
                    <i class="fas fa-search text-muted"></i>
                </span>
                <input type="text" class="form-control border-start-0" id="search-input"
                    placeholder="Search documents by mission title, document key, or organization..." autofocus>
            </div>
        </div>
        <div class="col-md-3">
            <button class="button-primary w-100" id="search-btn" type="button">
                <span>Filter Library</span>
            </button>
        </div>
    </div>
</div>

<!-- Error Container -->
<div id="error-message" class="error-message">
    <i class="fas fa-exclamation-circle text-danger"></i>
    <span id="error-text">An error occurred</span>
</div>

<!-- Loading Spinner -->
<div id="loading-indicator" class="loading-spinner">
    <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
    </div>
    <p class="mt-2 text-muted" style="font-size: 13px;">Retrieving indexed documents...</p>
</div>

<!-- Documents Grid -->
<div id="documents-container" class="row g-3">
    <!-- Populated dynamically -->
</div>

<!-- Pagination Controls -->
<nav id="pagination-container" class="mt-4" style="display: none;">
    <ul class="pagination justify-content-center gap-1">
        <li class="page-item" id="prev-btn" style="display: none;">
            <a class="page-link rounded" href="#"><i class="fas fa-chevron-left me-1"></i> Previous</a>
        </li>
        <li class="page-item active">
            <span class="page-link rounded">
                Page <span id="current-page">1</span> of <span id="total-pages">1</span>
            </span>
        </li>
        <li class="page-item" id="next-btn" style="display: none;">
            <a class="page-link rounded" href="#">Next <i class="fas fa-chevron-right ms-1"></i></a>
        </li>
    </ul>
</nav>

<!-- Document Detail Modal -->
<div class="modal fade" id="documentModal" tabindex="-1" aria-labelledby="modalDocTitle" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content" style="border-radius: var(--radius-lg); border: 1px solid var(--border-subtle); box-shadow: var(--shadow-elevated);">
            <div class="modal-header" style="background: #f8fafc; border-bottom: 1px solid var(--border-subtle);">
                <div class="d-flex align-items-center gap-2">
                    <div class="doc-type-icon doc-type-pdf" id="modalTypeIcon">
                        <i class="fas fa-file-pdf"></i>
                    </div>
                    <div>
                        <h5 class="modal-title mb-0" id="modalDocTitle" style="font-size: 16px; font-weight: 700;">Document Details</h5>
                        <small class="text-muted" id="modalDocKey">Document Key</small>
                    </div>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <div id="modalDocContent">
                    <div class="text-center py-4">
                        <div class="spinner-border text-primary" role="status"></div>
                    </div>
                </div>
            </div>
            <div class="modal-footer" style="background: #f8fafc; border-top: 1px solid var(--border-subtle);">
                <button type="button" class="button-secondary" data-bs-dismiss="modal">Close</button>
                <a id="openDetailBtn" class="button-primary" href="#" target="_blank">
                    <i class="fas fa-external-link-alt"></i> Full Specs View
                </a>
            </div>
        </div>
    </div>
</div>

<script>
    let currentPage = 0;
    const pageSize = 12;
    let totalElements = 0;
    let isSearchMode = false;
    let searchQuery = '';

    document.addEventListener('DOMContentLoaded', function () {
        document.getElementById('search-btn').addEventListener('click', performSearch);
        document.getElementById('search-input').addEventListener('keypress', function (e) {
            if (e.key === 'Enter') performSearch();
        });
        document.getElementById('next-btn').addEventListener('click', nextPage);
        document.getElementById('prev-btn').addEventListener('click', prevPage);
        loadDocuments(0);
    });

    function performSearch() {
        searchQuery = document.getElementById('search-input').value.trim();
        isSearchMode = !!searchQuery;
        currentPage = 0;

        if (isSearchMode) {
            loadSearchResults(searchQuery);
        } else {
            loadDocuments(0);
        }
    }

    function loadDocuments(page) {
        currentPage = page;
        showLoading(true);

        fetch(`/api/documents?page=\${page}&size=\${pageSize}`)
            .then(r => {
                if (!r.ok) throw new Error('Failed to load documents');
                return r.json();
            })
            .then(data => {
                showLoading(false);
                totalElements = data.totalElements || 0;
                displayDocuments(data.content || []);
                updatePagination(data.currentPage || 0, data.totalPages || 1);
            })
            .catch(e => {
                showLoading(false);
                console.error('Error:', e);
                showError('Failed to load documents: ' + e.message);
            });
    }

    function loadSearchResults(query) {
        showLoading(true);

        fetch(`/api/documents/search?q=\${encodeURIComponent(query)}&page=\${currentPage}&size=\${pageSize}`)
            .then(r => {
                if (!r.ok) throw new Error('Search failed');
                return r.json();
            })
            .then(data => {
                showLoading(false);
                displayDocuments(data.results || []);
                updatePagination(0, 1);
            })
            .catch(e => {
                showLoading(false);
                console.error('Error:', e);
                showError('Search failed: ' + e.message);
            });
    }

    function displayDocuments(documents) {
        const container = document.getElementById('documents-container');

        if (!documents || documents.length === 0) {
            container.innerHTML = `
                <div class="col-12">
                    <div class="empty-state">
                        <div class="empty-state-icon">
                            <i class="fas fa-folder-open"></i>
                        </div>
                        <div class="empty-state-title">
                            \${isSearchMode ? 'No matching documents found' : 'No documents in repository yet'}
                        </div>
                        <p>\${isSearchMode ? 'Try broadening your search query' : 'Upload your first mission document or submit an ISRO portal URL.'}</p>
                        <a href="/ingestion-centre" class="button-primary mt-2">
                            <i class="fas fa-cloud-upload-alt"></i> Go to Ingestion Centre
                        </a>
                    </div>
                </div>
            `;
            return;
        }

        let html = '';
        documents.forEach(doc => {
            const title = escapeHtml(doc.title || 'Untitled Document');
            const key = escapeHtml(doc.documentKey || 'unassigned');
            const org = escapeHtml(doc.sourceOrganization || 'ISRO');
            const isWeb = doc.sourceType === 'WEB' || (doc.sourceUrl && doc.sourceUrl.length > 0);
            const iconClass = isWeb ? 'doc-type-web' : 'doc-type-pdf';
            const iconFa = isWeb ? 'fa-globe' : 'fa-file-pdf';
            const typeLabel = isWeb ? 'WEB' : 'PDF';

            html += `
                <div class="col-md-6 col-lg-4">
                    <div class="doc-grid-card" onclick="showDocumentDetail(\${doc.id})">
                        <div>
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <div class="d-flex align-items-center gap-2">
                                    <div class="doc-type-icon \${iconClass}">
                                        <i class="fas \${iconFa}"></i>
                                    </div>
                                    <span class="badge bg-light text-secondary border" style="font-size: 10px;">\${typeLabel}</span>
                                </div>
                                <span class="badge bg-primary-subtle text-primary border border-primary-subtle" style="font-size: 11px;">
                                    Rev \${doc.currentRevision || 1}
                                </span>
                            </div>

                            <h3 class="doc-title-text" title="\${title}">\${title}</h3>

                            <div class="doc-meta-row">
                                <span class="doc-badge-pill" title="Organization">
                                    <i class="fas fa-building text-muted"></i> \${org}
                                </span>
                                <span class="doc-badge-pill" title="Chunk count">
                                    <i class="fas fa-cube text-muted"></i> \${doc.totalChunks || 0} chunks
                                </span>
                            </div>
                        </div>

                        <div class="pt-2 border-top mt-2 d-flex align-items-center justify-content-between" style="border-color: #f1f5f9 !important;">
                            <small class="text-muted text-truncate" style="font-size: 11px; max-width: 180px;">
                                \${key}
                            </small>
                            <span class="text-primary fw-semibold" style="font-size: 12px;">
                                Details <i class="fas fa-arrow-right ms-1"></i>
                            </span>
                        </div>
                    </div>
                </div>
            `;
        });

        container.innerHTML = html;
    }

    function showDocumentDetail(docId) {
        const modal = new bootstrap.Modal(document.getElementById('documentModal'));
        const contentDiv = document.getElementById('modalDocContent');
        contentDiv.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></div>';

        fetch(`/api/documents/\${docId}`)
            .then(r => {
                if (!r.ok) throw new Error('Failed to load document');
                return r.json();
            })
            .then(doc => {
                document.getElementById('modalDocTitle').textContent = doc.title || 'Document';
                document.getElementById('modalDocKey').textContent = `Key: \${doc.documentKey || 'N/A'}`;
                document.getElementById('openDetailBtn').href = `/document-detail?id=\${docId}`;

                const isWeb = doc.sourceType === 'WEB' || (doc.sourceUrl && doc.sourceUrl.length > 0);
                const typeIcon = document.getElementById('modalTypeIcon');
                if (typeIcon) {
                    typeIcon.className = `doc-type-icon \${isWeb ? 'doc-type-web' : 'doc-type-pdf'}`;
                    typeIcon.innerHTML = `<i class="fas \${isWeb ? 'fa-globe' : 'fa-file-pdf'}"></i>`;
                }

                let html = `
                    <div class="row g-3">
                        <div class="col-md-6">
                            <div class="p-3 bg-light rounded" style="border: 1px solid var(--border-subtle);">
                                <h6 class="text-muted text-uppercase fw-bold" style="font-size: 11px;">Document Metadata</h6>
                                <div class="mt-2" style="font-size: 13px;">
                                    <div class="mb-1"><strong>Title:</strong> \${escapeHtml(doc.title || 'N/A')}</div>
                                    <div class="mb-1"><strong>Key:</strong> <code>\${escapeHtml(doc.documentKey || 'N/A')}</code></div>
                                    <div class="mb-1"><strong>Type:</strong> <span class="badge bg-secondary">\${escapeHtml(doc.sourceType || 'PDF')}</span></div>
                                    <div><strong>Organization:</strong> \${escapeHtml(doc.sourceOrganization || 'ISRO')}</div>
                                </div>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="p-3 bg-light rounded" style="border: 1px solid var(--border-subtle);">
                                <h6 class="text-muted text-uppercase fw-bold" style="font-size: 11px;">Vector & Chunk Metrics</h6>
                                <div class="mt-2" style="font-size: 13px;">
                                    <div class="mb-1"><strong>Active Revision:</strong> <span class="badge bg-primary">Rev \${doc.currentRevision || 1}</span></div>
                                    <div class="mb-1"><strong>Indexed Chunks:</strong> <strong>\${doc.totalChunks || 0}</strong> vector embeddings</div>
                                    <div class="mb-1"><strong>Vector Dimension:</strong> 768d (nomic-embed-text)</div>
                                    <div><strong>Indexing Status:</strong> <span class="text-success"><i class="fas fa-check-circle"></i> ACTIVE</span></div>
                                </div>
                            </div>
                        </div>
                    </div>
                `;

                if (doc.sourceUrl) {
                    html += `
                        <div class="mt-3 p-3 bg-light rounded" style="border: 1px solid var(--border-subtle);">
                            <h6 class="text-muted text-uppercase fw-bold" style="font-size: 11px;">Source URL</h6>
                            <a href="\${escapeHtml(doc.sourceUrl)}" target="_blank" class="text-break text-primary" style="font-size: 13px;">
                                <i class="fas fa-external-link-alt me-1"></i> \${escapeHtml(doc.sourceUrl)}
                            </a>
                        </div>
                    `;
                }

                contentDiv.innerHTML = html;
                modal.show();
            })
            .catch(e => {
                console.error('Error:', e);
                contentDiv.innerHTML = `<div class="alert alert-danger">Failed to load document details: \${escapeHtml(e.message)}</div>`;
            });
    }

    function updatePagination(page, totalPages) {
        document.getElementById('current-page').textContent = (page + 1).toString();
        document.getElementById('total-pages').textContent = totalPages.toString();

        const pagination = document.getElementById('pagination-container');
        const prevBtn = document.getElementById('prev-btn');
        const nextBtn = document.getElementById('next-btn');

        if (totalPages <= 1) {
            pagination.style.display = 'none';
        } else {
            pagination.style.display = 'block';
            prevBtn.style.display = page > 0 ? 'inline-block' : 'none';
            nextBtn.style.display = page < totalPages - 1 ? 'inline-block' : 'none';
        }
    }

    function nextPage(e) {
        e.preventDefault();
        if (isSearchMode) {
            currentPage++;
            loadSearchResults(searchQuery);
        } else {
            loadDocuments(currentPage + 1);
        }
    }

    function prevPage(e) {
        e.preventDefault();
        if (isSearchMode) {
            currentPage = Math.max(0, currentPage - 1);
            loadSearchResults(searchQuery);
        } else {
            loadDocuments(Math.max(0, currentPage - 1));
        }
    }

    function showLoading(show) {
        document.getElementById('loading-indicator').style.display = show ? 'block' : 'none';
    }

    function showError(message) {
        const elem = document.getElementById('error-message');
        const text = document.getElementById('error-text');
        if (elem && text) {
            text.textContent = message;
            elem.classList.add('show');
            setTimeout(() => elem.classList.remove('show'), 6000);
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
        return String(text).replace(/[&<>"']/g, m => map[m]);
    }
</script>