<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

        <div class="page-header">
            <h1 class="page-title">Document Library</h1>
            <p class="page-subtitle">Search and explore your indexed documents</p>
        </div>

        <!-- Search Section -->
        <div class="metric-card mb-4">
            <div class="row g-3">
                <div class="col-md-8">
                    <input type="text" class="form-control" id="search-input"
                        placeholder="Search documents by title or key...">
                </div>
                <div class="col-md-4">
                    <button class="button-primary" id="search-btn" type="button" style="width: 100%;">
                        <i class="fas fa-search"></i> Search
                    </button>
                </div>
            </div>
        </div>

        <!-- Error/Success Messages -->
        <div id="error-message" class="error-message"></div>

        <!-- Loading Indicator -->
        <div id="loading-indicator" class="loading-spinner">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2 text-muted">Loading documents...</p>
        </div>

        <!-- Documents Grid -->
        <div id="documents-container" class="row g-3">
            <div class="col-12">
                <div class="empty-state">
                    <div class="empty-state-icon">
                        <i class="fas fa-inbox"></i>
                    </div>
                    <div class="empty-state-title">No documents yet</div>
                    <p>Start by uploading documents in the Ingestion Centre</p>
                </div>
            </div>
        </div>

        <!-- Pagination -->
        <nav id="pagination-container" style="margin-top: 30px; display: none;">
            <ul class="pagination justify-content-center">
                <li class="page-item" id="prev-btn" style="display: none;">
                    <a class="page-link" href="#">Previous</a>
                </li>
                <li class="page-item active">
                    <span class="page-link">
                        Page <span id="current-page">1</span> of <span id="total-pages">1</span>
                    </span>
                </li>
                <li class="page-item" id="next-btn" style="display: none;">
                    <a class="page-link" href="#">Next</a>
                </li>
            </ul>
        </nav>

        <!-- Document Detail Modal -->
        <div class="modal fade" id="documentModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="modalDocTitle">Document Details</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div id="modalDocContent">
                            <div class="spinner-border text-primary" role="status">
                                <span class="visually-hidden">Loading...</span>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <a id="openDetailBtn" class="button-primary" href="#" target="_blank">
                            <i class="fas fa-external-link-alt"></i> View Details
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
                        <i class="fas fa-inbox"></i>
                    </div>
                    <div class="empty-state-title">
                        ${isSearchMode ? 'No results found' : 'No documents yet'}
                    </div>
                    <p>${isSearchMode ? 'Try a different search term' : 'Start by uploading documents in the Ingestion Centre'}</p>
                </div>
            </div>
        `;
                    return;
                }

                let html = '';
                documents.forEach(doc => {
                    const title = escapeHtml(doc.title || 'Untitled');
                    const key = escapeHtml(doc.documentKey || '');
                    html += `
            <div class="col-md-4">
                <div class="document-card" onclick="showDocumentDetail(\${doc.id})">
                    <div class="document-title">\${title}</div>
                    <div class="document-meta">
                        <small><strong>Key:</strong> \${key}</small>
                    </div>
                    <div class="document-meta">
                        <small><strong>Type:</strong> \${escapeHtml(doc.sourceType || 'PDF')}</small>
                    </div>
                    <div class="document-meta mb-2">
                        <small><strong>Organization:</strong> \${escapeHtml(doc.sourceOrganization || 'N/A')}</small>
                    </div>
                    <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                        <span class="badge bg-primary">\${doc.currentRevision || 1} revision(s)</span>
                        <span class="badge bg-info">\${doc.totalChunks || 0} chunks</span>
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
                contentDiv.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>';

                fetch(`/api/documents/${docId}`)
                    .then(r => {
                        if (!r.ok) throw new Error('Failed to load document');
                        return r.json();
                    })
                    .then(doc => {
                        document.getElementById('modalDocTitle').textContent = escapeHtml(doc.title || 'Document');
                        document.getElementById('openDetailBtn').href = `/document-detail?id=${docId}`;

                        let html = `
                <div class="mb-3">
                    <h6>Basic Information</h6>
                    <table class="table table-sm">
                        <tr>
                            <td><strong>Title:</strong></td>
                            <td>\${escapeHtml(doc.title || 'N/A')}</td>
                        </tr>
                        <tr>
                            <td><strong>Key:</strong></td>
                            <td>\${escapeHtml(doc.documentKey || 'N/A')}</td>
                        </tr>
                        <tr>
                            <td><strong>Source Type:</strong></td>
                            <td>\${escapeHtml(doc.sourceType || 'N/A')}</td>
                        </tr>
                        <tr>
                            <td><strong>Organization:</strong></td>
                            <td>\${escapeHtml(doc.sourceOrganization || 'N/A')}</td>
                        </tr>
                    </table>
                </div>

                <div class="mb-3">
                    <h6>Content Metrics</h6>
                    <table class="table table-sm">
                        <tr>
                            <td><strong>Current Revision:</strong></td>
                            <td>\${doc.currentRevision || 1}</td>
                        </tr>
                        <tr>
                            <td><strong>Total Chunks:</strong></td>
                            <td>\${doc.totalChunks || 0}</td>
                        </tr>
                    </table>
                </div>

                \${doc.sourceUrl ? `
                            < div class="mb-3" >
                        <h6>Source URL</h6>
                        <small class="text-break">\${escapeHtml(doc.sourceUrl)}</small>
                    </div >
                            ` : ''}
            `;

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
                    prevBtn.style.display = page > 0 ? 'block' : 'none';
                    nextBtn.style.display = page < totalPages - 1 ? 'block' : 'none';
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
                elem.textContent = message;
                elem.classList.add('show');
                setTimeout(() => elem.classList.remove('show'), 5000);
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
        </script>