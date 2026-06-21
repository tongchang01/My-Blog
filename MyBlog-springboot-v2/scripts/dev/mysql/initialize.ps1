param(
    [switch]$Reset,
    [switch]$SkipSeed
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDirectory = (Resolve-Path (Join-Path $scriptDirectory "../../..")).Path
$seedFile = Join-Path $scriptDirectory "seed.sql"
$verifyScript = Join-Path $scriptDirectory "verify.ps1"
$allowedDatabase = "myblog_v2_dev"
$defaultUrl = "jdbc:mysql://localhost:3306/myblog_v2_dev"

function Require-EnvironmentVariable {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Missing required environment variable: $Name"
    }
    return $value
}

function Parse-MySqlUrl {
    param([string]$Url)

    $match = [regex]::Match(
        $Url,
        '^jdbc:mysql://(?<host>[^/:?]+)(?::(?<port>[0-9]+))?/(?<database>[^?]+)')
    if (-not $match.Success) {
        throw "MYBLOG_DATASOURCE_URL must be a jdbc:mysql URL"
    }

    $port = 3306
    if ($match.Groups["port"].Success) {
        $port = [int]$match.Groups["port"].Value
    }

    return [pscustomobject]@{
        Host = $match.Groups["host"].Value
        Port = $port
        Database = $match.Groups["database"].Value
    }
}

$username = Require-EnvironmentVariable "MYBLOG_DATASOURCE_USERNAME"
$password = Require-EnvironmentVariable "MYBLOG_DATASOURCE_PASSWORD"
$datasourceUrl = [Environment]::GetEnvironmentVariable(
    "MYBLOG_DATASOURCE_URL",
    "Process")
if ([string]::IsNullOrWhiteSpace($datasourceUrl)) {
    $datasourceUrl = $defaultUrl
}

$connection = Parse-MySqlUrl $datasourceUrl
if ($connection.Database -ne $allowedDatabase) {
    throw "Refusing database '$($connection.Database)'; only '$allowedDatabase' is allowed"
}

$mysql = Get-Command mysql -ErrorAction Stop
$mysqlBaseArguments = @(
    "--protocol=TCP",
    "--host=$($connection.Host)",
    "--port=$($connection.Port)",
    "--user=$username",
    "--default-character-set=utf8mb4",
    "--batch",
    "--raw",
    "--skip-column-names"
)

function Invoke-MySqlQuery {
    param(
        [string]$Sql,
        [switch]$WithoutDatabase
    )

    $arguments = @($mysqlBaseArguments)
    if (-not $WithoutDatabase) {
        $arguments += $allowedDatabase
    }
    $arguments += "--execute=$Sql"

    $output = & $mysql.Source @arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "mysql command failed: $($output -join [Environment]::NewLine)"
    }
    return @($output)
}

function Get-ActiveRowCount {
    $tableCountOutput = Invoke-MySqlQuery -WithoutDatabase -Sql (
        "SELECT COUNT(*) AS table_count FROM information_schema.tables " +
        "WHERE table_schema = '$allowedDatabase' " +
        "AND table_name IN ('t_user_auth', 't_article')")
    $tableCount = [int]($tableCountOutput | Select-Object -First 1)
    if ($tableCount -lt 2) {
        return 0
    }

    $activeCountOutput = Invoke-MySqlQuery -Sql (
        "SELECT ((SELECT COUNT(*) FROM t_user_auth WHERE deleted = 0) + " +
        "(SELECT COUNT(*) FROM t_article WHERE deleted = 0)) AS active_count")
    return [int]($activeCountOutput | Select-Object -First 1)
}

function Stop-ProcessTree {
    param([System.Diagnostics.Process]$Process)

    if ($null -eq $Process -or $Process.HasExited) {
        return
    }

    & taskkill.exe /PID $Process.Id /T /F 2>&1 | Out-Null
}

$originalMysqlPassword = [Environment]::GetEnvironmentVariable("MYSQL_PWD", "Process")
$backendProcess = $null
$standardOutputLog = Join-Path $env:TEMP "myblog-v2-local-mysql.out.log"
$standardErrorLog = Join-Path $env:TEMP "myblog-v2-local-mysql.err.log"

try {
    [Environment]::SetEnvironmentVariable("MYSQL_PWD", $password, "Process")

    if ($Reset) {
        $resetSql = (
            "DROP DATABASE IF EXISTS ``$allowedDatabase``; " +
            "CREATE DATABASE ``$allowedDatabase`` " +
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
        Invoke-MySqlQuery -WithoutDatabase -Sql $resetSql | Out-Null
    } else {
        $activeCount = Get-ActiveRowCount
        if ($activeCount -gt 0) {
            throw "Refusing to seed database with $activeCount active user/article rows; use -Reset explicitly"
        }
    }

    Remove-Item $standardOutputLog, $standardErrorLog `
        -Force -ErrorAction SilentlyContinue
    $backendProcess = Start-Process -FilePath "mvn.cmd" `
        -ArgumentList @(
            "spring-boot:run",
            "-Dspring-boot.run.profiles=local"
        ) `
        -WorkingDirectory $projectDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $standardOutputLog `
        -RedirectStandardError $standardErrorLog `
        -PassThru

    $deadline = [DateTime]::UtcNow.AddSeconds(120)
    $healthy = $false
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($backendProcess.HasExited) {
            $errorTail = Get-Content $standardErrorLog -Tail 30 `
                -ErrorAction SilentlyContinue
            throw "Spring Boot exited before health check: $($errorTail -join [Environment]::NewLine)"
        }

        try {
            $response = Invoke-WebRequest -UseBasicParsing `
                -Uri "http://localhost:8080/actuator/health" `
                -TimeoutSec 2
            if ($response.StatusCode -eq 200 -and $response.Content -match '"status"\s*:\s*"UP"') {
                $healthy = $true
                break
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }

    if (-not $healthy) {
        throw "Spring Boot health check timed out after 120 seconds"
    }
} finally {
    Stop-ProcessTree $backendProcess
    [Environment]::SetEnvironmentVariable(
        "MYSQL_PWD",
        $originalMysqlPassword,
        "Process")
}

if ($SkipSeed) {
    Write-Host "Flyway migration completed; seed import skipped"
    exit 0
}

[Environment]::SetEnvironmentVariable("MYSQL_PWD", $password, "Process")
try {
    if (-not $Reset) {
        $activeCount = Get-ActiveRowCount
        if ($activeCount -gt 0) {
            throw "Refusing to seed database with $activeCount active user/article rows"
        }
    }

    $seedPath = $seedFile.Replace('\', '/')
    Invoke-MySqlQuery -Sql "source $seedPath" | Out-Null

    & (Join-Path $PSHOME "powershell.exe") -NoProfile `
        -ExecutionPolicy Bypass -File $verifyScript
    if ($LASTEXITCODE -ne 0) {
        throw "Seed verification failed"
    }
} finally {
    [Environment]::SetEnvironmentVariable(
        "MYSQL_PWD",
        $originalMysqlPassword,
        "Process")
}

Write-Host "Local MySQL initialization completed"
