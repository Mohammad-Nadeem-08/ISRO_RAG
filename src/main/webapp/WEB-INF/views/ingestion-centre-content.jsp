<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<style>
    .ingest-card {
        background: #ffffff;
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius-lg);
        box-shadow: var(--shadow-card);
        padding: 24px;
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
    }

    .ingest-card-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 20px;
        padding-bottom: 14px;
        border-bottom: 1px solid var(--border-subtle);
    }

    .ingest-icon-wrapper {
        width: 40px;
        height: 40px;
        border-radius: var(--radius-md);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
    }

    .icon-red {
        background: rgba(239, 68, 68, 0.1);
        color: #dc2626;
    }

    .icon-cyan {
        background: rgba(6, 182, 212, 0.1);
        color: #0891b2;
    }

    .ingest-dropzone {
        border: 2px dashed #cbd5e1;
        border-radius: var(--radius-md);
        padding: 36px 20px;
        text-align: center;
        cursor: pointer;
        background: #fafbfc;
        transition: all 0.2s ease;
    }

    .ingest-dropzone:hover, .ingest-dropzone.dragover {
        border-color: var(--primary);
        background: rgba(37, 99, 235, 0.04);
        transform: scale(1.01);
    }

    .selected-file-card {
        background: #f0fdf4;
        border: 1px solid #bbf7d0;
        border-radius: var(--radius-md);
        padding: 12px 14px;
        margin-top: 14px;
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    .recent-ingest-table th {
        font-size: 11px;
        text-transform: uppercase;
        color: var(--text-muted);
        letter-spacing: 0.6px;
        border-bottom: 1px solid var(--border-subtle);
        background: #f8fafc;
        padding: 10px 14px;
    }

    .recent-ingest-table td {
        font-size: 12.5px;
        padding: 12px 14px;
        vertical-align: middle;
        border-bottom: 1px solid var(--border-subtle);
    }
</style>

<div class="page-header">
    <h1 class="page-title">Ingestion Centre</h1>
    <p class="page-subtitle">Ingest technical PDFs or public ISRO URLs with automated text extraction, chunking, and vector embedding</p>
</div>

<!-- Alert Notifications -->
<div id="error-message" class="error-message">
    <i class="fas fa-exclamation-circle text-danger"></i>
    <span id="error-text">An error occurred</span>
</div>
<div id="success-message" class="success-message">
    <i class="fas fa-check-circle text-success"></i>
    <span id="success-text">Ingestion completed</span>
</div>

<div class="row g-4 mb-4">
    <!-- PDF File Upload Form -->
    <div class="col-lg-6">
        <div class="ingest-card">
            <div>
                <div class="ingest-card-header">
                    <div class="ingest-icon-wrapper icon-red">
                        <i class="fas fa-file-pdf"></i>
                    </div>
                    <div>
                        <h5 class="mb-0 fw-bold" style="font-size: 15px;">PDF Document Ingestion</h5>
                        <small class="text-muted">Upload technical PDFs (up to 50 MB)</small>
                    </div>
                </div>

                <form id="upload-form" enctype="multipart/form-data">
                    <!-- Drop Zone -->
                    <div class="ingest-dropzone" id="dropZone">
                        <i class="fas fa-cloud-upload-alt text-primary mb-2" style="font-size: 32px;"></i>
                        <div class="fw-semibold text-dark mb-1" style="font-size: 14px;">Drag and drop PDF document here</div>
                        <small class="text-muted">or click to browse your local device</small>
                        <input type="file" id="file-input" style="display: none;" accept=".pdf" required>
                    </div>

                    <!-- Selected File Preview -->
                    <div id="file-preview-container" style="display: none;">
                        <div class="selected-file-card">
                            <div class="d-flex align-items-center gap-2 text-truncate me-2">
                                <i class="fas fa-file-pdf text-danger fs-5"></i>
                                <div>
                                    <div class="fw-bold text-dark text-truncate" id="selected-file-name" style="font-size: 13px; max-width: 280px;">filename.pdf</div>
                                    <small class="text-muted"><span id="file-size-mb">0</span> MB • Ready for ingestion</small>
                                </div>
                            </div>
                            <button type="button" class="btn btn-sm btn-outline-secondary" id="change-file-btn">Change</button>
                        </div>
                    </div>

                    <div class="mt-3">
                        <label class="form-label fw-semibold" style="font-size: 12.5px;">Document Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control-custom w-100" id="title-input" placeholder="e.g. Mars Orbiter Mission Technical Specifications" required>
                    </div>

                    <div class="mt-3">
                        <label class="form-label fw-semibold" style="font-size: 12.5px;">Authoring Organization <span class="text-danger">*</span></label>
                        <input type="text" class="form-control-custom w-100" id="org-input" value="ISRO" placeholder="e.g. ISRO, VSSC, URSC" required>
                    </div>

                    <div id="upload-loading" class="text-center py-3" style="display: none;">
                        <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                        <span class="text-muted" style="font-size: 13px;">Extracting text, chunking, and computing nomic embeddings...</span>
                    </div>

                    <button type="submit" class="button-primary w-100 mt-4" id="upload-btn">
                        <i class="fas fa-upload"></i> Upload and Ingest PDF
                    </button>
                </form>
            </div>
        </div>
    </div>

    <!-- Web URL Submission Form -->
    <div class="col-lg-6">
        <div class="ingest-card">
            <div>
                <div class="ingest-card-header">
                    <div class="ingest-icon-wrapper icon-cyan">
                        <i class="fas fa-globe"></i>
                    </div>
                    <div>
                        <h5 class="mb-0 fw-bold" style="font-size: 15px;">Web Portal & URL Ingestion</h5>
                        <small class="text-muted">Crawl and extract authoritative ISRO web content</small>
                    </div>
                </div>

                <form id="url-form">
                    <div class="mb-3">
                        <label class="form-label fw-semibold" style="font-size: 12.5px;">Target Document URL <span class="text-danger">*</span></label>
                        <input type="url" class="form-control-custom w-100" id="url-input" placeholder="https://www.isro.gov.in/..." required>
                        <small class="text-muted d-block mt-1" style="font-size: 11px;">
                            <i class="fas fa-shield-alt text-success me-1"></i> SSRF protected: Private IPs, loopback, and metadata endpoints are blocked.
                        </small>
                    </div>

                    <div class="mb-3">
                        <label class="form-label fw-semibold" style="font-size: 12.5px;">Document Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control-custom w-100" id="url-title-input" placeholder="e.g. ISRO Official Portal Overview" required>
                    </div>

                    <div class="mb-3">
                        <label class="form-label fw-semibold" style="font-size: 12.5px;">Authoring Organization <span class="text-danger">*</span></label>
                        <input type="text" class="form-control-custom w-100" id="url-org-input" value="ISRO" placeholder="e.g. ISRO" required>
                    </div>

                    <div id="url-loading" class="text-center py-3" style="display: none;">
                        <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                        <span class="text-muted" style="font-size: 13px;">Fetching webpage, cleaning HTML, and generating vector chunks...</span>
                    </div>

                    <button type="submit" class="button-primary w-100 mt-4" id="url-btn">
                        <i class="fas fa-bolt"></i> Ingest Web URL
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<!-- Recent Ingestions History Table -->
<div class="content-card">
    <div class="d-flex align-items-center justify-content-between mb-3">
        <h5 class="mb-0 fw-bold" style="font-size: 15px;">
            <i class="fas fa-history text-primary me-2"></i> Recent Ingested Documents
        </h5>
        <a href="/document-library" class="button-secondary py-1 px-3" style="font-size: 12px;">
            View Full Library
        </a>
    </div>

    <div class="table-responsive">
        <table class="table recent-ingest-table mb-0">
            <thead>
                <tr>
                    <th>Type</th>
                    <th>Document Title</th>
                    <th>Organization</th>
                    <th>Active Revision</th>
                    <th>Chunks Embedded</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody id="recent-ingestions-body">
                <tr>
                    <td colspan="6" class="text-center py-4 text-muted">
                        <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                        Loading recent ingestions...
                    </td>
                </tr>
            </tbody>
        </table>
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

    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('file-input');
    const filePreview = document.getElementById('file-preview-container');
    const selectedFileName = document.getElementById('selected-file-name');
    const fileSizeMb = document.getElementById('file-size-mb');
    const changeFileBtn = document.getElementById('change-file-btn');

    document.addEventListener('DOMContentLoaded', function () {
        loadRecentIngestions();

        // File selection events
        if (dropZone) dropZone.addEventListener('click', () => fileInput.click());
        if (changeFileBtn) changeFileBtn.addEventListener('click', () => fileInput.click());

        if (fileInput) {
            fileInput.addEventListener('change', function () {
                if (this.files.length > 0) {
                    displayFile(this.files[0]);
                }
            });
        }

        // Drag and drop events
        if (dropZone) {
            dropZone.addEventListener('dragover', (e) => {
                e.preventDefault();
                dropZone.classList.add('dragover');
            });

            dropZone.addEventListener('dragleave', () => {
                dropZone.classList.remove('dragover');
            });

            dropZone.addEventListener('drop', (e) => {
                e.preventDefault();
                dropZone.classList.remove('dragover');
                const files = e.dataTransfer.files;
                if (files.length > 0 && files[0].name.toLowerCase().endsWith('.pdf')) {
                    fileInput.files = files;
                    displayFile(files[0]);
                } else {
                    showError('Please drop a valid PDF document (.pdf)');
                }
            });
        }

        // Setup forms
        setupUploadForm();
        setupUrlForm();
    });

    function displayFile(file) {
        if (file.size > 50 * 1024 * 1024) {
            showError('File exceeds the 50 MB upload limit');
            fileInput.value = '';
            return;
        }

        selectedFileName.textContent = file.name;
        fileSizeMb.textContent = (file.size / (1024 * 1024)).toFixed(2);
        dropZone.style.display = 'none';
        filePreview.style.display = 'block';

        // Auto-fill title if empty
        const titleInput = document.getElementById('title-input');
        if (titleInput && !titleInput.value) {
            titleInput.value = file.name.replace(/\.pdf$/i, '').replace(/[-_]/g, ' ');
        }
    }

    async function parseApiResponse(response, defaultActionName) {
        if (response.redirected) {
            throw new Error('Your session has expired. Please refresh the page and sign in again.');
        }

        const contentType = response.headers.get('content-type') || '';
        let data = null;

        if (contentType.includes('application/json')) {
            try {
                data = await response.json();
            } catch (parseErr) {
                console.warn('Failed to parse JSON response', parseErr);
            }
        }

        if (!response.ok) {
            if (data && data.errorMessage) {
                throw new Error(data.errorMessage);
            } else if (data && data.message) {
                throw new Error(data.message);
            } else if (response.status === 401 || response.status === 403) {
                throw new Error('Authentication required or session expired. Please sign in again.');
            } else if (response.status === 413) {
                throw new Error('File exceeds the 50 MB upload limit.');
            } else if (response.status === 422) {
                throw new Error(data && data.errorMessage ? data.errorMessage : 'The document could not be processed. Please ensure it is a text-based PDF or valid web page.');
            } else {
                throw new Error(defaultActionName + ' failed with status ' + response.status);
            }
        }

        if (!data) {
            throw new Error('Server returned an unexpected non-JSON response (status ' + response.status + ')');
        }

        return data;
    }

    function setupUploadForm() {
        const uploadForm = document.getElementById('upload-form');
        if (!uploadForm) return;

        uploadForm.addEventListener('submit', function (e) {
            e.preventDefault();

            if (!fileInput.files.length) {
                showError('Please select a PDF file to upload');
                return;
            }

            const file = fileInput.files[0];
            if (!file.name.toLowerCase().endsWith('.pdf')) {
                showError('Please select a valid PDF file (.pdf)');
                return;
            }

            const title = document.getElementById('title-input').value.trim();
            const org = document.getElementById('org-input').value.trim();

            if (!title) {
                showError('Please enter document title');
                return;
            }

            const formData = new FormData();
            formData.append('file', file);
            formData.append('title', title);
            formData.append('organization', org);

            showUploadLoading(true);

            fetch('/api/ingestion/upload', {
                method: 'POST',
                headers: getCsrfHeaders(),
                body: formData
            })
            .then(r => parseApiResponse(r, 'PDF Upload'))
            .then(data => {
                showUploadLoading(false);
                if (data.status === 'FAILED') {
                    showError('Upload failed: ' + data.errorMessage);
                } else {
                    showSuccess('Document uploaded and ingested successfully! (Job: ' + (data.jobKey || 'done') + ')');
                    uploadForm.reset();
                    dropZone.style.display = 'block';
                    filePreview.style.display = 'none';
                    loadRecentIngestions();
                }
            })
            .catch(e => {
                showUploadLoading(false);
                console.error('Upload Error:', e);
                showError(e.message);
            });
        });
    }

    function setupUrlForm() {
        const urlForm = document.getElementById('url-form');
        if (!urlForm) return;

        urlForm.addEventListener('submit', function (e) {
            e.preventDefault();

            const url = document.getElementById('url-input').value.trim();
            const title = document.getElementById('url-title-input').value.trim();
            const org = document.getElementById('url-org-input').value.trim();

            if (!url || !title) {
                showError('Please fill in required fields (URL and Title)');
                return;
            }

            if (!url.startsWith('http://') && !url.startsWith('https://')) {
                showError('Please enter a valid HTTP or HTTPS URL (e.g. https://www.isro.gov.in/)');
                return;
            }

            showUrlLoading(true);

            fetch('/api/ingestion/url', {
                method: 'POST',
                headers: Object.assign({
                    'Content-Type': 'application/json'
                }, getCsrfHeaders()),
                body: JSON.stringify({
                    sourceUrl: url,
                    title: title,
                    organization: org
                })
            })
            .then(r => parseApiResponse(r, 'URL Ingestion'))
            .then(data => {
                showUrlLoading(false);
                if (data.status === 'FAILED') {
                    showError('Submission failed: ' + data.errorMessage);
                } else if (data.status === 'SKIPPED') {
                    showSuccess('URL content already ingested with identical content hash (Skipped duplicate).');
                    urlForm.reset();
                    loadRecentIngestions();
                } else {
                    showSuccess('URL ingested successfully! Document has been chunked and embedded.');
                    urlForm.reset();
                    loadRecentIngestions();
                }
            })
            .catch(e => {
                showUrlLoading(false);
                console.error('URL Ingestion Error:', e);
                showError(e.message);
            });
        });
    }

    function loadRecentIngestions() {
        const tbody = document.getElementById('recent-ingestions-body');
        if (!tbody) return;

        fetch('/api/documents?page=0&size=5')
            .then(r => r.json())
            .then(data => {
                const docs = data.content || [];
                if (docs.length === 0) {
                    tbody.innerHTML = `
                        <tr>
                            <td colspan="6" class="text-center py-4 text-muted">
                                No documents ingested yet. Upload a PDF or submit a URL above.
                            </td>
                        </tr>
                    `;
                    return;
                }

                let html = '';
                docs.forEach(doc => {
                    const isWeb = doc.sourceType === 'WEB' || (doc.sourceUrl && doc.sourceUrl.length > 0);
                    const typeBadge = isWeb 
                        ? '<span class="badge bg-info-subtle text-info border border-info-subtle"><i class="fas fa-globe me-1"></i> WEB</span>'
                        : '<span class="badge bg-danger-subtle text-danger border border-danger-subtle"><i class="fas fa-file-pdf me-1"></i> PDF</span>';
                    
                    html += `
                        <tr>
                            <td>\${typeBadge}</td>
                            <td><strong class="text-dark">\${escapeHtml(doc.title || 'Untitled')}</strong></td>
                            <td><span class="badge bg-light text-secondary border">\${escapeHtml(doc.sourceOrganization || 'ISRO')}</span></td>
                            <td>Rev \${doc.currentRevision || 1}</td>
                            <td><strong>\${doc.totalChunks || 0}</strong> chunks</td>
                            <td><span class="badge bg-success-subtle text-success"><i class="fas fa-check-circle me-1"></i> ACTIVE</span></td>
                        </tr>
                    `;
                });
                tbody.innerHTML = html;
            })
            .catch(e => {
                console.error('Error loading recent ingestions:', e);
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-3">Could not retrieve recent documents.</td></tr>';
            });
    }

    function showUploadLoading(show) {
        document.getElementById('upload-loading').style.display = show ? 'block' : 'none';
        document.getElementById('upload-btn').disabled = show;
    }

    function showUrlLoading(show) {
        document.getElementById('url-loading').style.display = show ? 'block' : 'none';
        document.getElementById('url-btn').disabled = show;
    }

    function showError(message) {
        const elem = document.getElementById('error-message');
        const text = document.getElementById('error-text');
        if (elem && text) {
            text.textContent = message;
            elem.classList.add('show');
            setTimeout(() => elem.classList.remove('show'), 7000);
        }
    }

    function showSuccess(message) {
        const elem = document.getElementById('success-message');
        const text = document.getElementById('success-text');
        if (elem && text) {
            text.textContent = message;
            elem.classList.add('show');
            setTimeout(() => elem.classList.remove('show'), 5000);
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