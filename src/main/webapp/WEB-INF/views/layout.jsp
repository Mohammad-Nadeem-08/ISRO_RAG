<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="_csrf" content="${_csrf.token}"/>
    <meta name="_csrf_header" content="${_csrf.headerName}"/>
    <title>SpaceDoc Intelligence - Dashboard</title>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.0/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --primary-dark: #0f172a;
            --primary: #2563eb;
            --primary-light: #3b82f6;
            --success: #10b981;
            --warning: #f59e0b;
            --danger: #ef4444;
            --neutral-light: #f8fafc;
            --neutral-border: #e2e8f0;
            --text-primary: #1e293b;
            --text-secondary: #64748b;
        }

        * {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
        }

        body {
            background: var(--neutral-light);
            color: var(--text-primary);
        }

        .navbar {
            background: white;
            border-bottom: 1px solid var(--neutral-border);
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
        }

        .navbar-brand {
            font-weight: 700;
            font-size: 18px;
            color: var(--primary-dark) !important;
        }

        .sidebar {
            background: white;
            border-right: 1px solid var(--neutral-border);
            min-height: calc(100vh - 60px);
            padding: 20px 0;
        }

        .nav-link {
            color: var(--text-secondary);
            padding: 12px 20px;
            border-left: 3px solid transparent;
            transition: all 0.2s ease;
        }

        .nav-link:hover {
            color: var(--primary);
            background: rgba(37, 99, 235, 0.05);
        }

        .nav-link.active {
            color: var(--primary);
            background: rgba(37, 99, 235, 0.05);
            border-left-color: var(--primary);
        }

        .nav-link i {
            margin-right: 10px;
            width: 18px;
        }

        .main-content {
            padding: 30px;
        }

        .page-header {
            margin-bottom: 30px;
        }

        .page-title {
            font-size: 28px;
            font-weight: 700;
            color: var(--primary-dark);
            margin: 0;
        }

        .page-subtitle {
            font-size: 14px;
            color: var(--text-secondary);
            margin-top: 4px;
        }

        .metric-card {
            background: white;
            border: 1px solid var(--neutral-border);
            border-radius: 8px;
            padding: 20px;
            transition: all 0.2s ease;
        }

        .metric-card:hover {
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
            border-color: var(--primary-light);
        }

        .metric-value {
            font-size: 32px;
            font-weight: 700;
            color: var(--primary-dark);
        }

        .metric-label {
            font-size: 13px;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-top: 8px;
        }

        .metric-icon {
            font-size: 24px;
            color: var(--primary-light);
            opacity: 0.3;
            float: right;
        }

        .document-card {
            background: white;
            border: 1px solid var(--neutral-border);
            border-radius: 8px;
            padding: 16px;
            transition: all 0.2s ease;
            cursor: pointer;
        }

        .document-card:hover {
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
            border-color: var(--primary-light);
        }

        .document-title {
            font-weight: 600;
            color: var(--primary-dark);
            margin-bottom: 8px;
            font-size: 15px;
        }

        .document-meta {
            font-size: 12px;
            color: var(--text-secondary);
            margin-bottom: 4px;
        }

        .badge {
            font-size: 11px;
            padding: 4px 8px;
        }

        .button-primary {
            background: linear-gradient(135deg, var(--primary) 0%, #1d4ed8 100%);
            color: white;
            border: none;
            padding: 10px 18px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 14px;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .button-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 25px rgba(37, 99, 235, 0.3);
            text-decoration: none;
            color: white;
        }

        .button-secondary {
            background: white;
            color: var(--primary);
            border: 1px solid var(--primary);
            padding: 10px 18px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 14px;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .button-secondary:hover {
            background: rgba(37, 99, 235, 0.05);
        }

        .form-control {
            border: 1px solid var(--neutral-border);
            border-radius: 6px;
            padding: 10px 14px;
            font-size: 14px;
            transition: all 0.2s ease;
        }

        .form-control:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
            outline: none;
        }

        .loading-spinner {
            display: none;
            text-align: center;
            padding: 40px 20px;
        }

        .loading-spinner.show {
            display: block;
        }

        .error-message {
            background: #fee2e2;
            border: 1px solid #fecaca;
            color: #991b1b;
            padding: 12px 16px;
            border-radius: 6px;
            margin-bottom: 20px;
            display: none;
        }

        .error-message.show {
            display: block;
        }

        .success-message {
            background: #dcfce7;
            border: 1px solid #86efac;
            color: #166534;
            padding: 12px 16px;
            border-radius: 6px;
            margin-bottom: 20px;
            display: none;
        }

        .success-message.show {
            display: block;
        }

        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: var(--text-secondary);
        }

        .empty-state-icon {
            font-size: 48px;
            opacity: 0.3;
            margin-bottom: 16px;
        }

        .empty-state-title {
            font-size: 18px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 8px;
        }

        .conversation-thread {
            background: white;
            border: 1px solid var(--neutral-border);
            border-radius: 8px;
            padding: 20px;
        }

        .message-user {
            background: rgba(37, 99, 235, 0.1);
            border-left: 3px solid var(--primary);
            padding: 12px 16px;
            margin-bottom: 12px;
            border-radius: 4px;
            text-align: right;
        }

        .message-assistant {
            background: var(--neutral-light);
            border-left: 3px solid var(--success);
            padding: 12px 16px;
            margin-bottom: 12px;
            border-radius: 4px;
        }

        .citation {
            display: inline-block;
            background: rgba(37, 99, 235, 0.1);
            color: var(--primary);
            padding: 2px 6px;
            border-radius: 3px;
            font-size: 12px;
            margin: 0 2px;
            cursor: pointer;
            font-weight: 500;
        }

        .citation:hover {
            background: rgba(37, 99, 235, 0.2);
        }

        .evidence-panel {
            background: var(--neutral-light);
            border: 1px solid var(--neutral-border);
            border-radius: 6px;
            padding: 12px;
            font-size: 13px;
            margin-top: 12px;
            max-height: 200px;
            overflow-y: auto;
        }

        .evidence-item {
            background: white;
            padding: 8px;
            margin-bottom: 8px;
            border-radius: 4px;
            border-left: 2px solid var(--primary-light);
        }

        .file-upload-zone {
            border: 2px dashed var(--neutral-border);
            border-radius: 8px;
            padding: 40px 20px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .file-upload-zone:hover {
            border-color: var(--primary);
            background: rgba(37, 99, 235, 0.02);
        }

        .file-upload-zone.dragover {
            border-color: var(--primary);
            background: rgba(37, 99, 235, 0.05);
        }

        .upload-icon {
            font-size: 32px;
            color: var(--primary-light);
            margin-bottom: 12px;
        }

        .pagination {
            margin-top: 20px;
        }

        .page-link {
            color: var(--primary);
            border-color: var(--neutral-border);
        }

        .page-link:hover {
            color: white;
            background-color: var(--primary);
            border-color: var(--primary);
        }

        .page-item.active .page-link {
            background-color: var(--primary);
            border-color: var(--primary);
        }

        @media (max-width: 768px) {
            .main-content {
                padding: 15px;
            }

            .page-title {
                font-size: 22px;
            }

            .metric-value {
                font-size: 24px;
            }
        }
    </style>
</head>
<body>
    <!-- Navigation -->
    <nav class="navbar navbar-expand-lg navbar-light">
        <div class="container-fluid">
            <a class="navbar-brand" href="/dashboard">
                <i class="fas fa-rocket"></i> SpaceDoc Intelligence
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav ms-auto">
                    <li class="nav-item">
                        <span class="navbar-text me-3">
                            <i class="fas fa-user-circle"></i>
                            <c:out default="User" escapeXml="true" value="${pageContext.request.userPrincipal.name}"/>
                        </span>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/logout">Logout</a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>

    <div class="row g-0">
        <!-- Sidebar -->
        <div class="col-md-2 sidebar">
            <ul class="nav flex-column">
                <li class="nav-item">
                    <a class="nav-link ${activeTab == 'dashboard' ? 'active' : ''}" href="/dashboard">
                        <i class="fas fa-chart-line"></i> Dashboard
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${activeTab == 'assistant' ? 'active' : ''}" href="/research-assistant">
                        <i class="fas fa-comments"></i> Research Assistant
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${activeTab == 'library' ? 'active' : ''}" href="/document-library">
                        <i class="fas fa-book"></i> Document Library
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${activeTab == 'ingestion' ? 'active' : ''}" href="/ingestion-centre">
                        <i class="fas fa-upload"></i> Ingestion Centre
                    </a>
                </li>
            </ul>
        </div>

        <!-- Main Content -->
        <div class="col-md-10">
            <div class="main-content">
                <jsp:include page="${contentPage}" />
            </div>
        </div>
    </div>

    <script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/js/all.min.js"></script>
</body>
</html>
