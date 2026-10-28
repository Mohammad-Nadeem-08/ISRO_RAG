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
    <title>SpaceDoc Intelligence - <c:choose><c:when test="${activeTab == 'assistant'}">Research Assistant</c:when><c:when test="${activeTab == 'library'}">Document Library</c:when><c:when test="${activeTab == 'ingestion'}">Ingestion Centre</c:when><c:otherwise>Dashboard</c:otherwise></c:choose></title>
    
    <!-- Modern Typography: Inter & JetBrains Mono -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
    
    <!-- Bootstrap 5.3 & FontAwesome 6.4 -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.0/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    
    <style>
        :root {
            --app-bg: #f8fafc;
            --surface-bg: #ffffff;
            --header-bg: #0b0f19;
            --sidebar-bg: #ffffff;
            --sidebar-border: #e2e8f0;
            --primary: #2563eb;
            --primary-hover: #1d4ed8;
            --primary-glow: rgba(37, 99, 235, 0.15);
            --primary-gradient: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
            --accent-purple: #7c3aed;
            --accent-cyan: #06b6d4;
            --success: #10b981;
            --warning: #f59e0b;
            --danger: #ef4444;
            --text-main: #0f172a;
            --text-secondary: #475569;
            --text-muted: #64748b;
            --text-light: #94a3b8;
            --border-subtle: #e2e8f0;
            --border-hover: #cbd5e1;
            --radius-sm: 6px;
            --radius-md: 10px;
            --radius-lg: 14px;
            --radius-full: 9999px;
            --shadow-subtle: 0 1px 3px 0 rgba(0, 0, 0, 0.04), 0 1px 2px -1px rgba(0, 0, 0, 0.04);
            --shadow-card: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.04);
            --shadow-elevated: 0 12px 24px -4px rgba(15, 23, 42, 0.08), 0 4px 6px -2px rgba(15, 23, 42, 0.04);
            --sidebar-width: 250px;
            --header-height: 64px;
        }

        * {
            box-sizing: border-box;
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            -webkit-font-smoothing: antialiased;
        }

        body {
            background-color: var(--app-bg);
            color: var(--text-main);
            margin: 0;
            padding: 0;
            overflow-x: hidden;
            display: flex;
            flex-direction: column;
            min-height: 100vh;
        }

        /* Top Header Navigation */
        .app-navbar {
            background: var(--header-bg);
            height: var(--header-height);
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
            position: sticky;
            top: 0;
            z-index: 1040;
            display: flex;
            align-items: center;
            padding: 0 24px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }

        .navbar-brand-group {
            display: flex;
            align-items: center;
            gap: 12px;
            text-decoration: none;
        }

        .brand-icon-wrapper {
            width: 36px;
            height: 36px;
            background: var(--primary-gradient);
            border-radius: var(--radius-md);
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 16px;
            box-shadow: 0 0 15px rgba(37, 99, 235, 0.4);
            transition: transform 0.2s ease;
        }

        .brand-icon-wrapper:hover {
            transform: scale(1.05);
        }

        .brand-text-container {
            display: flex;
            flex-direction: column;
        }

        .brand-title {
            color: #ffffff;
            font-size: 16px;
            font-weight: 700;
            letter-spacing: -0.3px;
            line-height: 1.2;
            margin: 0;
        }

        .brand-subtitle {
            color: var(--accent-cyan);
            font-size: 10.5px;
            font-weight: 600;
            letter-spacing: 0.5px;
            text-transform: uppercase;
            line-height: 1.2;
        }

        /* Tech Badges in Header */
        .header-meta-group {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-left: 28px;
        }

        .tech-pill {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(255, 255, 255, 0.06);
            border: 1px solid rgba(255, 255, 255, 0.12);
            color: #e2e8f0;
            padding: 4px 10px;
            border-radius: var(--radius-full);
            font-size: 11px;
            font-weight: 500;
            letter-spacing: 0.2px;
        }

        .tech-pill i {
            font-size: 10px;
        }

        .tech-pill-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background-color: var(--success);
            box-shadow: 0 0 8px var(--success);
        }

        /* Header Right Controls */
        .header-user-section {
            margin-left: auto;
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .user-badge {
            display: flex;
            align-items: center;
            gap: 8px;
            color: #e2e8f0;
            font-size: 13px;
            font-weight: 500;
            background: rgba(255, 255, 255, 0.04);
            border: 1px solid rgba(255, 255, 255, 0.1);
            padding: 5px 12px;
            border-radius: var(--radius-full);
        }

        .user-avatar-circle {
            width: 24px;
            height: 24px;
            background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 11px;
            font-weight: 600;
        }

        .btn-logout {
            color: #cbd5e1;
            background: transparent;
            border: 1px solid rgba(255, 255, 255, 0.15);
            padding: 5px 14px;
            border-radius: var(--radius-md);
            font-size: 12.5px;
            font-weight: 500;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s ease;
        }

        .btn-logout:hover {
            color: #ffffff;
            background: rgba(239, 68, 68, 0.15);
            border-color: rgba(239, 68, 68, 0.4);
        }

        .mobile-nav-toggle {
            display: none;
            background: transparent;
            border: 1px solid rgba(255, 255, 255, 0.2);
            color: white;
            padding: 6px 10px;
            border-radius: var(--radius-sm);
            font-size: 16px;
            cursor: pointer;
            margin-right: 12px;
        }

        /* App Shell: Sidebar + Content */
        .app-shell {
            display: flex;
            flex: 1;
            width: 100%;
            min-height: calc(100vh - var(--header-height));
            position: relative;
        }

        /* Left Sidebar */
        .app-sidebar {
            width: var(--sidebar-width);
            flex: 0 0 var(--sidebar-width);
            background: var(--sidebar-bg);
            border-right: 1px solid var(--sidebar-border);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            padding: 20px 14px;
            position: sticky;
            top: var(--header-height);
            height: calc(100vh - var(--header-height));
            overflow-y: auto;
            z-index: 1030;
            transition: transform 0.3s ease;
        }

        .nav-section-label {
            font-size: 10.5px;
            font-weight: 700;
            color: var(--text-light);
            text-transform: uppercase;
            letter-spacing: 0.8px;
            padding: 8px 12px 6px;
            margin-bottom: 4px;
        }

        .sidebar-nav-list {
            list-style: none;
            padding: 0;
            margin: 0;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .sidebar-nav-item .nav-link {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 10px 14px;
            color: var(--text-secondary);
            font-size: 13.5px;
            font-weight: 500;
            border-radius: var(--radius-md);
            text-decoration: none;
            transition: all 0.15s ease;
            position: relative;
        }

        .sidebar-nav-item .nav-link i {
            font-size: 15px;
            width: 18px;
            text-align: center;
            color: var(--text-muted);
            transition: color 0.15s ease;
        }

        .sidebar-nav-item .nav-link:hover {
            color: var(--primary);
            background: rgba(37, 99, 235, 0.06);
        }

        .sidebar-nav-item .nav-link:hover i {
            color: var(--primary);
        }

        .sidebar-nav-item .nav-link.active {
            color: var(--primary);
            background: rgba(37, 99, 235, 0.08);
            font-weight: 600;
        }

        .sidebar-nav-item .nav-link.active i {
            color: var(--primary);
        }

        .sidebar-nav-item .nav-link.active::before {
            content: '';
            position: absolute;
            left: 0;
            top: 20%;
            bottom: 20%;
            width: 3.5px;
            border-radius: 4px;
            background: var(--primary);
        }

        /* Sidebar Footer Status */
        .sidebar-footer {
            border-top: 1px solid var(--border-subtle);
            padding-top: 14px;
            margin-top: 20px;
        }

        .engine-status-card {
            background: #f1f5f9;
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-md);
            padding: 10px 12px;
        }

        .engine-status-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 11px;
            font-weight: 600;
            color: var(--text-main);
            margin-bottom: 4px;
        }

        .engine-status-details {
            font-size: 10px;
            color: var(--text-muted);
            line-height: 1.4;
            margin: 0;
        }

        /* Main Content Container */
        .app-main {
            flex: 1;
            min-width: 0;
            background: var(--app-bg);
            padding: 28px 36px 48px;
        }

        .app-content-container {
            max-width: 1400px;
            margin: 0 auto;
        }

        /* Global Header & Card Components */
        .page-header {
            margin-bottom: 24px;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .page-title {
            font-size: 24px;
            font-weight: 700;
            color: var(--text-main);
            letter-spacing: -0.5px;
            margin: 0;
        }

        .page-subtitle {
            font-size: 13.5px;
            color: var(--text-muted);
            margin: 0;
        }

        /* Cards */
        .metric-card, .content-card {
            background: var(--surface-bg);
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-lg);
            padding: 20px;
            box-shadow: var(--shadow-card);
            transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
        }

        .metric-card:hover {
            border-color: var(--border-hover);
            box-shadow: var(--shadow-elevated);
        }

        .metric-value {
            font-size: 28px;
            font-weight: 700;
            color: var(--text-main);
            letter-spacing: -0.5px;
            line-height: 1.1;
        }

        .metric-label {
            font-size: 11.5px;
            font-weight: 600;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.6px;
            margin-top: 8px;
        }

        .metric-icon-box {
            width: 44px;
            height: 44px;
            border-radius: var(--radius-md);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            float: right;
        }

        .metric-icon-blue {
            background: rgba(37, 99, 235, 0.1);
            color: var(--primary);
        }

        .metric-icon-purple {
            background: rgba(124, 58, 237, 0.1);
            color: var(--accent-purple);
        }

        .metric-icon-emerald {
            background: rgba(16, 185, 129, 0.1);
            color: var(--success);
        }

        .metric-icon-amber {
            background: rgba(245, 158, 11, 0.1);
            color: var(--warning);
        }

        /* Global Buttons */
        .button-primary {
            background: var(--primary-gradient);
            color: white;
            border: none;
            padding: 9px 18px;
            border-radius: var(--radius-md);
            font-weight: 600;
            font-size: 13.5px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            cursor: pointer;
            box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
            transition: all 0.2s ease;
            text-decoration: none;
        }

        .button-primary:hover:not(:disabled) {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
            color: white;
        }

        .button-primary:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none !important;
            box-shadow: none !important;
        }

        .button-secondary {
            background: #ffffff;
            color: var(--text-secondary);
            border: 1px solid var(--border-subtle);
            padding: 9px 16px;
            border-radius: var(--radius-md);
            font-weight: 500;
            font-size: 13.5px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            cursor: pointer;
            transition: all 0.2s ease;
            text-decoration: none;
        }

        .button-secondary:hover:not(:disabled) {
            color: var(--text-main);
            background: #f8fafc;
            border-color: var(--border-hover);
        }

        .button-secondary:disabled {
            opacity: 0.6;
            cursor: not-allowed;
        }

        /* Form Controls */
        .form-control-custom, .form-control {
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-md);
            padding: 10px 14px;
            font-size: 13.5px;
            color: var(--text-main);
            background-color: #ffffff;
            transition: all 0.2s ease;
        }

        .form-control-custom:focus, .form-control:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px var(--primary-glow);
            outline: none;
        }

        /* Notifications & Alerts */
        .error-message {
            background: #fef2f2;
            border: 1px solid #fecaca;
            color: #991b1b;
            padding: 12px 16px;
            border-radius: var(--radius-md);
            margin-bottom: 18px;
            font-size: 13px;
            display: none;
            align-items: center;
            gap: 10px;
        }

        .error-message.show {
            display: flex;
        }

        .success-message {
            background: #f0fdf4;
            border: 1px solid #bbf7d0;
            color: #166534;
            padding: 12px 16px;
            border-radius: var(--radius-md);
            margin-bottom: 18px;
            font-size: 13px;
            display: none;
            align-items: center;
            gap: 10px;
        }

        .success-message.show {
            display: flex;
        }

        .empty-state {
            text-align: center;
            padding: 48px 24px;
            color: var(--text-muted);
        }

        .empty-state-icon {
            font-size: 38px;
            color: var(--text-light);
            margin-bottom: 12px;
        }

        .empty-state-title {
            font-size: 16px;
            font-weight: 600;
            color: var(--text-main);
            margin-bottom: 6px;
        }

        /* Mobile Sidebar Backdrop */
        .sidebar-backdrop {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(15, 23, 42, 0.4);
            backdrop-filter: blur(2px);
            z-index: 1025;
        }

        .sidebar-backdrop.active {
            display: block;
        }

        /* Responsive Breakpoints */
        @media (max-width: 991.98px) {
            .mobile-nav-toggle {
                display: block;
            }

            .header-meta-group {
                display: none;
            }

            .app-sidebar {
                position: fixed;
                top: var(--header-height);
                left: 0;
                bottom: 0;
                transform: translateX(-100%);
                box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
            }

            .app-sidebar.mobile-open {
                transform: translateX(0);
            }

            .app-main {
                padding: 18px 16px 36px;
            }

            .page-title {
                font-size: 20px;
            }
        }
    </style>
</head>
<body>
    <!-- Top Application Navbar -->
    <header class="app-navbar">
        <button class="mobile-nav-toggle" id="mobileNavToggle" aria-label="Toggle navigation">
            <i class="fas fa-bars"></i>
        </button>

        <a class="navbar-brand-group" href="/dashboard">
            <div class="brand-icon-wrapper">
                <i class="fas fa-rocket"></i>
            </div>
            <div class="brand-text-container">
                <span class="brand-title">SpaceDoc Intelligence</span>
                <span class="brand-subtitle">ISRO Technical RAG</span>
            </div>
        </a>

        <!-- Live Architecture Status Pills -->
        <div class="header-meta-group">
            <div class="tech-pill" title="Active LLM: Groq API">
                <span class="tech-pill-dot"></span>
                <i class="fas fa-brain text-info"></i>
                <span>Groq GPT-OSS</span>
            </div>
            <div class="tech-pill" title="Embeddings & Vector Store">
                <i class="fas fa-database text-warning"></i>
                <span>pgvector & nomic-embed</span>
            </div>
        </div>

        <!-- User Controls -->
        <div class="header-user-section">
            <div class="user-badge" title="Authenticated Operator">
                <div class="user-avatar-circle">
                    <c:choose>
                        <c:when test="${not empty pageContext.request.userPrincipal.name}">
                            ${pageContext.request.userPrincipal.name.substring(0, 1).toUpperCase()}
                        </c:when>
                        <c:otherwise>A</c:otherwise>
                    </c:choose>
                </div>
                <span><c:out default="admin" escapeXml="true" value="${pageContext.request.userPrincipal.name}"/></span>
            </div>
            <a class="btn-logout" href="/logout" title="Sign out of SpaceDoc">
                <i class="fas fa-sign-out-alt"></i>
                <span class="d-none d-sm-inline">Logout</span>
            </a>
        </div>
    </header>

    <!-- Mobile Drawer Backdrop -->
    <div class="sidebar-backdrop" id="sidebarBackdrop"></div>

    <!-- App Shell: Sidebar + Content -->
    <div class="app-shell">
        <!-- Sidebar Navigation -->
        <aside class="app-sidebar" id="appSidebar">
            <div>
                <div class="nav-section-label">Platform Modules</div>
                <ul class="sidebar-nav-list">
                    <li class="sidebar-nav-item">
                        <a class="nav-link ${activeTab == 'dashboard' ? 'active' : ''}" href="/dashboard">
                            <i class="fas fa-chart-line"></i>
                            <span>Dashboard</span>
                        </a>
                    </li>
                    <li class="sidebar-nav-item">
                        <a class="nav-link ${activeTab == 'assistant' ? 'active' : ''}" href="/research-assistant">
                            <i class="fas fa-comments"></i>
                            <span>Research Assistant</span>
                        </a>
                    </li>
                    <li class="sidebar-nav-item">
                        <a class="nav-link ${activeTab == 'library' ? 'active' : ''}" href="/document-library">
                            <i class="fas fa-book"></i>
                            <span>Document Library</span>
                        </a>
                    </li>
                    <li class="sidebar-nav-item">
                        <a class="nav-link ${activeTab == 'ingestion' ? 'active' : ''}" href="/ingestion-centre">
                            <i class="fas fa-cloud-upload-alt"></i>
                            <span>Ingestion Centre</span>
                        </a>
                    </li>
                </ul>
            </div>

            <div class="sidebar-footer">
                <div class="engine-status-card">
                    <div class="engine-status-header">
                        <span><i class="fas fa-shield-alt text-primary me-1"></i> System Secure</span>
                        <span class="badge bg-success-subtle text-success" style="font-size: 9px; padding: 2px 6px;">ONLINE</span>
                    </div>
                    <p class="engine-status-details">
                        RRF Hybrid Retrieval • Revision-Aware • Citation Grounded
                    </p>
                </div>
            </div>
        </aside>

        <!-- Main Dynamic View Content -->
        <main class="app-main">
            <div class="app-content-container">
                <jsp:include page="${contentPage}" />
            </div>
        </main>
    </div>

    <!-- Bootstrap Bundle JS -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
    <script>
        // Mobile Sidebar Drawer Controller
        document.addEventListener('DOMContentLoaded', function() {
            const toggle = document.getElementById('mobileNavToggle');
            const sidebar = document.getElementById('appSidebar');
            const backdrop = document.getElementById('sidebarBackdrop');

            if (toggle && sidebar && backdrop) {
                toggle.addEventListener('click', function() {
                    sidebar.classList.toggle('mobile-open');
                    backdrop.classList.toggle('active');
                });

                backdrop.addEventListener('click', function() {
                    sidebar.classList.remove('mobile-open');
                    backdrop.classList.remove('active');
                });
            }
        });
    </script>
</body>
</html>
