<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

        <div class="page-header">
            <h1 class="page-title">Ingestion Centre</h1>
            <p class="page-subtitle">Upload documents or submit URLs for analysis</p>
        </div>

        <!-- Error/Success Messages -->
        <div id="error-message" class="error-message"></div>
        <div id="success-message" class="success-message"></div>

        <div class="row">
            <!-- File Upload Section -->
            <div class="col-md-6">
                <div class="metric-card">
                    <h5 class="mb-4">
                        <i class="fas fa-file-upload"></i> Upload PDF
                    </h5>

                    <form id="upload-form" enctype="multipart/form-data">
                        <!-- File Drop Zone -->
                        <div class="file-upload-zone" id="dropZone">
                            <div class="upload-icon">
                                <i class="fas fa-cloud-upload-alt"></i>
                            </div>
                            <div style="font-weight: 600; margin-bottom: 8px;">Drag and drop your PDF here</div>
                            <small style="color: #64748b;">or click to browse</small>
                            <input type="file" id="file-input" style="display: none;" accept=".pdf" required>
                        </div>

                        <div id="file-name" style="margin-top: 16px; display: none;">
                            <small style="color: #64748b;">Selected file:</small>
                            <div id="selected-file-name" style="font-weight: 600; color: #2563eb;"></div>
                        </div>

                        <!-- Document Details -->
                        <div class="mt-4">
                            <label class="form-label">Document Title *</label>
                            <input type="text" class="form-control" id="title-input"
                                placeholder="e.g., Mars Mission Report 2024" required>
                        </div>

                        <div class="mt-3">
                            <label class="form-label">Organization *</label>
                            <input type="text" class="form-control" id="org-input" placeholder="e.g., ISRO" value="ISRO"
                                required>
                        </div>

                        <!-- File Info -->
                        <div id="file-info"
                            style="margin-top: 16px; padding: 12px; background: #f0f9ff; border-radius: 6px; display: none;">
                            <small style="color: #0c4a6e;">
                                <strong>File size:</strong> <span id="file-size">0</span> MB
                                <br><strong>Status:</strong> Ready to upload
                            </small>
                        </div>

                        <!-- Loading -->
                        <div id="upload-loading" class="loading-spinner" style="margin-top: 20px;">
                            <div class="spinner-border spinner-border-sm text-primary" role="status">
                                <span class="visually-hidden">Uploading...</span>
                            </div>
                            <p class="mt-2 text-muted" style="font-size: 13px;">Uploading and processing your
                                document...</p>
                        </div>

                        <!-- Submit -->
                        <button type="submit" class="button-primary" id="upload-btn"
                            style="width: 100%; margin-top: 20px;">
                            <i class="fas fa-upload"></i> Upload Document
                        </button>
                    </form>
                </div>
            </div>

            <!-- URL Submission Section -->
            <div class="col-md-6">
                <div class="metric-card">
                    <h5 class="mb-4">
                        <i class="fas fa-link"></i> Submit URL
                    </h5>

                    <form id="url-form">
                        <div class="mb-3">
                            <label class="form-label">Document URL *</label>
                            <input type="url" class="form-control" id="url-input"
                                placeholder="https://example.com/document" required>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Document Title *</label>
                            <input type="text" class="form-control" id="url-title-input"
                                placeholder="e.g., Research Paper" required>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Organization *</label>
                            <input type="text" class="form-control" id="url-org-input" placeholder="e.g., ISRO"
                                value="ISRO" required>
                        </div>

                        <!-- Loading -->
                        <div id="url-loading" class="loading-spinner" style="margin-top: 20px;">
                            <div class="spinner-border spinner-border-sm text-primary" role="status">
                                <span class="visually-hidden">Processing...</span>
                            </div>
                            <p class="mt-2 text-muted" style="font-size: 13px;">Processing URL...</p>
                        </div>

                        <button type="submit" class="button-primary" id="url-btn"
                            style="width: 100%; margin-top: 20px;">
                            <i class="fas fa-paper-plane"></i> Submit URL
                        </button>
                    </form>
                </div>
            </div>
        </div>

        <!-- Recent Ingestions -->
        <div class="row mt-4">
            <div class="col-12">
                <div class="metric-card">
                    <h5 class="mb-3">
                        <i class="fas fa-history"></i> Recent Ingestions
                    </h5>
                    <div id="recent-ingestions">
                        <div class="empty-state" style="padding: 40px 20px;">
                            <i class="fas fa-inbox" style="font-size: 32px; opacity: 0.3; margin-bottom: 10px;"></i>
                            <div style="font-size: 14px; color: #64748b;">
                                No recent ingestions. Upload a document to get started.
                            </div>
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

            const dropZone = document.getElementById('dropZone');
            const fileInput = document.getElementById('file-input');
            const fileNameDisplay = document.getElementById('file-name');
            const selectedFileName = document.getElementById('selected-file-name');
            const fileInfo = document.getElementById('file-info');

            // File selection
            dropZone.addEventListener('click', () => fileInput.click());

            fileInput.addEventListener('change', function () {
                if (this.files.length > 0) {
                    displayFile(this.files[0]);
                }
            });

            // Drag and drop
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
                if (files.length > 0 && files[0].type === 'application/pdf') {
                    fileInput.files = files;
                    displayFile(files[0]);
                } else {
                    showError('Please drop a PDF file');
                }
            });

            function displayFile(file) {
                if (file.size > 50 * 1024 * 1024) {
                    showError('File exceeds 50 MB limit');
                    fileInput.value = '';
                    return;
                }

                selectedFileName.textContent = file.name;
                fileNameDisplay.style.display = 'block';
                document.getElementById('file-size').textContent = (file.size / (1024 * 1024)).toFixed(2);
                fileInfo.style.display = 'block';
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
                        throw new Error('The document could not be processed. Please verify file format and content.');
                    } else {
                        throw new Error(defaultActionName + ' failed with status ' + response.status);
                    }
                }

                if (!data) {
                    throw new Error('Server returned an unexpected non-JSON response (status ' + response.status + ')');
                }

                return data;
            }

            // Upload form
            document.getElementById('upload-form').addEventListener('submit', function (e) {
                e.preventDefault();

                if (!fileInput.files.length) {
                    showError('Please select a file');
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
                    .then(r => parseApiResponse(r, 'Upload'))
                    .then(data => {
                        showUploadLoading(false);
                        if (data.status === 'FAILED') {
                            showError('Upload failed: ' + data.errorMessage);
                        } else {
                            showSuccess('Document uploaded successfully! Job: ' + data.jobKey);
                            document.getElementById('upload-form').reset();
                            fileNameDisplay.style.display = 'none';
                            fileInfo.style.display = 'none';
                            loadRecentIngestions();
                        }
                    })
                    .catch(e => {
                        showUploadLoading(false);
                        console.error('Error:', e);
                        showError('Upload error: ' + e.message);
                    });
            });

            // URL form
            document.getElementById('url-form').addEventListener('submit', function (e) {
                e.preventDefault();

                const url = document.getElementById('url-input').value.trim();
                const title = document.getElementById('url-title-input').value.trim();
                const org = document.getElementById('url-org-input').value.trim();

                if (!url || !title) {
                    showError('Please fill in all required fields');
                    return;
                }

                if (!url.startsWith('http://') && !url.startsWith('https://')) {
                    showError('Please enter a valid HTTP or HTTPS URL');
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
                    .then(r => parseApiResponse(r, 'URL submission'))
                    .then(data => {
                        showUrlLoading(false);
                        if (data.status === 'FAILED') {
                            showError('Submission failed: ' + data.errorMessage);
                        } else {
                            showSuccess('URL submitted successfully! Job: ' + data.jobKey);
                            document.getElementById('url-form').reset();
                            loadRecentIngestions();
                        }
                    })
                    .catch(e => {
                        showUrlLoading(false);
                        console.error('Error:', e);
                        showError('Submission error: ' + e.message);
                    });
            });

            function loadRecentIngestions() {
                const container = document.getElementById('recent-ingestions');
                // In a production system, this would fetch actual ingestion history
                // For now, show a placeholder
                container.innerHTML = `
        <div class="alert alert-info">
            <i class="fas fa-info-circle"></i>
            Documents are being processed. Check the document library to see your uploaded files.
        </div>
    `;
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
        </script>