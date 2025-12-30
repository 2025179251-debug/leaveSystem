<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Employee Login</title>
    <style>
        * { box-sizing: border-box; }
        body {
            margin: 0; padding: 0;
            font-family: Arial, sans-serif;
            background: #f4f6fb;
            display: flex; align-items: center; justify-content: center;
            min-height: 100vh;
        }
        .card {
            background: #ffffff;
            width: 380px;
            border-radius: 16px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.08);
            overflow: hidden;
        }
        .card-header {
            background: #2563eb;
            color: #ffffff;
            padding: 24px;
            text-align: center;
        }
        .logo-box {
            width: 56px; height: 56px;
            border-radius: 14px;
            border: 2px solid rgba(255,255,255,0.5);
            display: flex; align-items: center; justify-content: center;
            margin: 0 auto 10px;
            font-weight: bold; font-size: 24px;
        }
        .card-header h1 { margin: 0; font-size: 22px; }
        .card-header p { margin: 4px 0 0; font-size: 13px; opacity: 0.9; }

        .card-body { padding: 24px 24px 28px; }
        .form-group { margin-bottom: 14px; }
        label {
            display: block; margin-bottom: 4px;
            font-size: 13px; color: #374151; font-weight: 600;
        }
        input[type="email"], input[type="password"] {
            width: 100%;
            padding: 10px 12px;
            border-radius: 8px;
            border: 1px solid #cbd5e1;
            font-size: 14px;
        }
        input[type="email"]:focus, input[type="password"]:focus {
            outline: none;
            border-color: #2563eb;
            box-shadow: 0 0 0 2px rgba(37,99,235,0.25);
        }
        .btn-primary {
            width: 100%;
            padding: 10px 12px;
            border-radius: 8px;
            border: none;
            background: #2563eb;
            color: white;
            font-size: 15px;
            font-weight: bold;
            cursor: pointer;
            margin-top: 6px;
        }
        .btn-primary:hover { background: #1d4ed8; }

        .alert-error {
            background: #fee2e2;
            color: #b91c1c;
            border: 1px solid #fecaca;
            padding: 8px 10px;
            border-radius: 8px;
            font-size: 13px;
            margin-bottom: 10px;
        }
        .demo-box {
            margin-top: 14px;
            padding: 8px 10px;
            background: #f8fafc;
            border-radius: 8px;
            border: 1px solid #e2e8f0;
            font-size: 11px;
            color: #64748b;
        }
    </style>
</head>
<body>

<div class="card">
    <div class="card-header">
        <div class="logo-box">ES</div>
        <h1>Employee Leave System</h1>
        <p>Login using your registered account</p>
    </div>

    <div class="card-body">
        <!-- Error from servlet -->
        <c:if test="${not empty param.error}">
            <div class="alert-error">
                <c:out value="${param.error}" />
            </div>
        </c:if>

        <!-- Optional message -->
        <c:if test="${not empty param.msg}">
            <div class="demo-box">
                <c:out value="${param.msg}" />
            </div>
        </c:if>

        <form action="LoginServlet" method="post">
            <div class="form-group">
                <label for="email">Email Address</label>
                <input type="email" id="email" name="email"
                       placeholder="you@example.com" required />
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password"
                       placeholder="Enter your password" required />
            </div>

            <button type="submit" class="btn-primary">Sign In</button>

            <div class="demo-box">
                <strong>Note:</strong> Use the email and password that you registered
                in the Employee Registration form (from the Employee table).
            </div>
        </form>
    </div>
</div>

</body>
</html>
