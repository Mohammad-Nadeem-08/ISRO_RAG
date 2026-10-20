<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SpaceDoc Intelligence - Login</title>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.0/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
        }
        .login-container {
            width: 100%;
            max-width: 420px;
        }
        .login-card {
            background: #ffffff;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            padding: 40px;
        }
        .logo-section {
            text-align: center;
            margin-bottom: 40px;
        }
        .logo-text {
            font-size: 28px;
            font-weight: 700;
            color: #0f172a;
            margin: 0;
        }
        .logo-subtitle {
            font-size: 13px;
            color: #64748b;
            margin-top: 4px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-label {
            font-size: 14px;
            font-weight: 500;
            color: #1e293b;
            margin-bottom: 8px;
        }
        .form-control {
            padding: 12px 14px;
            border: 1px solid #e2e8f0;
            border-radius: 6px;
            font-size: 14px;
            transition: all 0.3s ease;
        }
        .form-control:focus {
            border-color: #3b82f6;
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
            outline: none;
        }
        .btn-login {
            width: 100%;
            padding: 12px;
            background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
            color: white;
            border: none;
            border-radius: 6px;
            font-weight: 600;
            font-size: 14px;
            cursor: pointer;
            transition: all 0.3s ease;
            margin-top: 20px;
        }
        .btn-login:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 25px rgba(37, 99, 235, 0.3);
        }
        .alert {
            border-radius: 6px;
            border: none;
            margin-bottom: 20px;
            font-size: 13px;
        }
        .demo-credentials {
            background: #f0f9ff;
            border: 1px solid #bfdbfe;
            border-radius: 6px;
            padding: 12px 14px;
            margin-top: 20px;
            font-size: 12px;
            color: #1e40af;
        }
        .demo-credentials strong {
            display: block;
            margin-bottom: 4px;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <div class="login-card">
            <div class="logo-section">
                <p class="logo-text">SpaceDoc Intelligence</p>
                <p class="logo-subtitle">Research-Driven RAG for Space Documents</p>
            </div>

            <c:if test="${not empty param.error}">
                <div class="alert alert-danger">
                    Invalid username or password
                </div>
            </c:if>

            <c:if test="${not empty param.logout}">
                <div class="alert alert-success">
                    You have been logged out successfully
                </div>
            </c:if>

            <form method="POST" action="/login">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <div class="form-group">
                    <label class="form-label">Username</label>
                    <input type="text" name="username" class="form-control" placeholder="Enter username" required autofocus>
                </div>

                <div class="form-group">
                    <label class="form-label">Password</label>
                    <input type="password" name="password" class="form-control" placeholder="Enter password" required>
                </div>

                <button type="submit" class="btn-login">Sign In</button>
            </form>

            <div class="demo-credentials">
                <strong>Demo Credentials:</strong>
                Admin: admin / admin_local_dev_password<br>
                User: user / user_local_dev_password
            </div>
        </div>
    </div>
</body>
</html>
