<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SpaceDoc Intelligence - Operator Login</title>
    
    <!-- Modern Typography -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <link href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.0/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        * {
            box-sizing: border-box;
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        body {
            background: radial-gradient(circle at 50% 20%, #1e293b 0%, #0b0f19 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            margin: 0;
        }
        .login-container {
            width: 100%;
            max-width: 440px;
        }
        .login-card {
            background: #ffffff;
            border-radius: 16px;
            box-shadow: 0 25px 60px -15px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.1);
            padding: 36px 32px;
        }
        .logo-section {
            text-align: center;
            margin-bottom: 28px;
        }
        .logo-icon-box {
            width: 48px;
            height: 48px;
            margin: 0 auto 14px;
            background: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 22px;
            box-shadow: 0 0 20px rgba(37, 99, 235, 0.4);
        }
        .logo-text {
            font-size: 22px;
            font-weight: 700;
            color: #0f172a;
            margin: 0;
            letter-spacing: -0.5px;
        }
        .logo-subtitle {
            font-size: 13px;
            color: #64748b;
            margin-top: 4px;
        }
        .form-group {
            margin-bottom: 18px;
        }
        .form-label {
            font-size: 13px;
            font-weight: 600;
            color: #1e293b;
            margin-bottom: 6px;
        }
        .form-control {
            padding: 11px 14px;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            font-size: 14px;
            transition: all 0.2s ease;
        }
        .form-control:focus {
            border-color: #2563eb;
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
            outline: none;
        }
        .btn-login {
            width: 100%;
            padding: 11px;
            background: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
            color: white;
            border: none;
            border-radius: 8px;
            font-weight: 600;
            font-size: 14px;
            cursor: pointer;
            transition: all 0.2s ease;
            margin-top: 10px;
            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
        }
        .btn-login:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
        }
        .alert {
            border-radius: 8px;
            border: none;
            margin-bottom: 20px;
            font-size: 13px;
            padding: 12px 14px;
        }
        .demo-credentials {
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 12px 14px;
            margin-top: 22px;
            font-size: 12px;
            color: #475569;
        }
        .demo-credentials strong {
            display: flex;
            align-items: center;
            gap: 6px;
            color: #0f172a;
            margin-bottom: 6px;
        }
        .credential-pill {
            display: inline-block;
            background: #ffffff;
            border: 1px solid #cbd5e1;
            padding: 2px 6px;
            border-radius: 4px;
            font-family: monospace;
            font-size: 11px;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <div class="login-card">
            <div class="logo-section">
                <div class="logo-icon-box">
                    <i class="fas fa-rocket"></i>
                </div>
                <h1 class="logo-text">SpaceDoc Intelligence</h1>
                <p class="logo-subtitle">ISRO Technical Research & RAG Platform</p>
            </div>

            <c:if test="${not empty param.error}">
                <div class="alert alert-danger d-flex align-items-center gap-2">
                    <i class="fas fa-exclamation-circle text-danger"></i>
                    <span>Invalid credentials. Please verify username and password.</span>
                </div>
            </c:if>

            <c:if test="${not empty param.logout}">
                <div class="alert alert-success d-flex align-items-center gap-2">
                    <i class="fas fa-check-circle text-success"></i>
                    <span>You have been safely signed out.</span>
                </div>
            </c:if>

            <form method="POST" action="/login">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <div class="form-group">
                    <label class="form-label">Username</label>
                    <input type="text" name="username" class="form-control" placeholder="Enter operator username" required autofocus>
                </div>

                <div class="form-group">
                    <label class="form-label">Password</label>
                    <input type="password" name="password" class="form-control" placeholder="Enter password" required>
                </div>

                <button type="submit" class="btn-login">
                    <i class="fas fa-lock me-1"></i> Authenticate & Enter
                </button>
            </form>

            <div class="demo-credentials">
                <strong><i class="fas fa-key text-primary"></i> Local Development Credentials:</strong>
                <div class="d-flex justify-content-between mt-1">
                    <span>Admin:</span>
                    <span class="credential-pill">admin / admin_local_dev_password</span>
                </div>
                <div class="d-flex justify-content-between mt-1">
                    <span>User:</span>
                    <span class="credential-pill">user / user_local_dev_password</span>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
