# ==============================================================================
# SpaceDoc Intelligence - Production-Ready Windows PowerShell Startup Script
# ==============================================================================

$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " SpaceDoc Intelligence - Application Launcher" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Load configuration from .env if present
if (Test-Path ".env") {
    Write-Host "[1/4] Loading configuration from .env..." -ForegroundColor Green
    $lines = Get-Content ".env"
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if ($trimmed -and -not $trimmed.StartsWith("#") -and $trimmed.Contains("=")) {
            $parts = $trimmed.Split("=", 2)
            $key = $parts[0].Trim()
            $val = $parts[1].Trim()
            if (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'"))) {
                $val = $val.Substring(1, $val.Length - 2)
            }
            [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
        }
    }
} else {
    Write-Host "[1/4] WARNING: No .env file found. Copy .env.example to .env to configure local settings." -ForegroundColor Yellow
}

# 2. Check GROQ_API_KEY without exposing or logging it
$groqKey = [System.Environment]::GetEnvironmentVariable("GROQ_API_KEY", "Process")
if (-not $groqKey) {
    $groqKey = $env:GROQ_API_KEY
}
if ($groqKey -and $groqKey.Trim().Length -gt 0) {
    Write-Host "[2/4] Groq API Key: CONFIGURED (Key length: $($groqKey.Length) chars)" -ForegroundColor Green
} else {
    Write-Host "[2/4] WARNING: GROQ_API_KEY is not set. Live answer generation will fail until set." -ForegroundColor Yellow
}

# 3. Check PostgreSQL readiness on localhost:5432
Write-Host "[3/4] Checking PostgreSQL connectivity (localhost:5432)..." -ForegroundColor Cyan
try {
    $tcpPg = New-Object System.Net.Sockets.TcpClient
    $connectPg = $tcpPg.BeginConnect("127.0.0.1", 5432, $null, $null)
    $waitPg = $connectPg.AsyncWaitHandle.WaitOne(2000, $false)
    if ($waitPg -and $tcpPg.Connected) {
        $tcpPg.EndConnect($connectPg)
        $tcpPg.Close()
        Write-Host "      PostgreSQL is ONLINE on port 5432" -ForegroundColor Green
    } else {
        $tcpPg.Close()
        Write-Host "      WARNING: Could not connect to PostgreSQL on port 5432." -ForegroundColor Yellow
        Write-Host "      If using Docker, start it with: docker-compose up -d postgres" -ForegroundColor Yellow
    }
} catch {
    Write-Host "      WARNING: PostgreSQL check encountered an issue: $($_.Exception.Message)" -ForegroundColor Yellow
}

# 4. Check Ollama readiness on 127.0.0.1:11434
Write-Host "[4/4] Checking Ollama embedding service (127.0.0.1:11434)..." -ForegroundColor Cyan
try {
    $tcpOllama = New-Object System.Net.Sockets.TcpClient
    $connectOllama = $tcpOllama.BeginConnect("127.0.0.1", 11434, $null, $null)
    $waitOllama = $connectOllama.AsyncWaitHandle.WaitOne(2000, $false)
    if ($waitOllama -and $tcpOllama.Connected) {
        $tcpOllama.EndConnect($connectOllama)
        $tcpOllama.Close()
        Write-Host "      Ollama service is ONLINE on port 11434" -ForegroundColor Green
    } else {
        $tcpOllama.Close()
        Write-Host "      WARNING: Could not connect to Ollama on port 11434." -ForegroundColor Yellow
        Write-Host "      Start Ollama or run: ollama serve" -ForegroundColor Yellow
    }
} catch {
    Write-Host "      WARNING: Ollama check encountered an issue: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`nLaunching Spring Boot application..." -ForegroundColor Cyan
Write-Host "Access URLs once started:"
Write-Host "  - Frontend:       http://localhost:8080" -ForegroundColor White
Write-Host "  - Health Check:   http://localhost:8080/api/system/health" -ForegroundColor White
Write-Host "  - Ingestion:      http://localhost:8080/ingestion-centre" -ForegroundColor White
Write-Host "  - Research:       http://localhost:8080/research-assistant" -ForegroundColor White
Write-Host "==========================================================`n" -ForegroundColor Cyan

mvn spring-boot:run
