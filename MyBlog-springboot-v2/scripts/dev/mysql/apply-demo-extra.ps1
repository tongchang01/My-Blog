$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

# Requires PowerShell 7+ on Windows or Linux. Windows PowerShell 5.1 is unsupported.
if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw "PowerShell 7+ is required. Run this script with pwsh."
}

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptDirectory "demo-extra.sql"
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

$username = Require-EnvironmentVariable "MYBLOG_DATASOURCE_USERNAME"
$password = Require-EnvironmentVariable "MYBLOG_DATASOURCE_PASSWORD"
$datasourceUrl = [Environment]::GetEnvironmentVariable(
    "MYBLOG_DATASOURCE_URL",
    "Process")
if ([string]::IsNullOrWhiteSpace($datasourceUrl)) {
    $datasourceUrl = $defaultUrl
}

$match = [regex]::Match(
    $datasourceUrl,
    '^jdbc:mysql://(?<host>[^/:?]+)(?::(?<port>[0-9]+))?/(?<database>[^?]+)')
if (-not $match.Success) {
    throw "MYBLOG_DATASOURCE_URL must be a jdbc:mysql URL"
}
if ($match.Groups["database"].Value -ne $allowedDatabase) {
    throw "Only '$allowedDatabase' can be updated"
}

$port = 3306
if ($match.Groups["port"].Success) {
    $port = [int]$match.Groups["port"].Value
}

$mysql = Get-Command mysql -ErrorAction Stop
$mysqlArguments = @(
    "--protocol=TCP",
    "--host=$($match.Groups['host'].Value)",
    "--port=$port",
    "--user=$username",
    "--default-character-set=utf8mb4",
    "--batch",
    "--raw",
    "--skip-column-names",
    $allowedDatabase
)
$originalMysqlPassword = [Environment]::GetEnvironmentVariable("MYSQL_PWD", "Process")

try {
    [Environment]::SetEnvironmentVariable("MYSQL_PWD", $password, "Process")
    $sqlPath = $sqlFile.Replace('\', '/')
    $rows = & $mysql.Source @mysqlArguments "--execute=source $sqlPath" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Demo data import failed: $($rows -join [Environment]::NewLine)"
    }
} finally {
    [Environment]::SetEnvironmentVariable(
        "MYSQL_PWD",
        $originalMysqlPassword,
        "Process")
}

Write-Host "Demo data supplement applied"
