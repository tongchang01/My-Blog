$ErrorActionPreference = "Stop"

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$initializeScript = Join-Path $scriptDirectory "initialize.ps1"
$powerShellExecutable = Join-Path $PSHOME "powershell.exe"
$temporaryDirectory = Join-Path ([System.IO.Path]::GetTempPath()) (
    "myblog-mysql-contract-" + [guid]::NewGuid().ToString("N"))
$fakeMysqlLog = Join-Path $temporaryDirectory "mysql.log"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-Initialize {
    param(
        [hashtable]$Environment,
        [string[]]$Arguments = @()
    )

    $names = @(
        "MYBLOG_DATASOURCE_URL",
        "MYBLOG_DATASOURCE_USERNAME",
        "MYBLOG_DATASOURCE_PASSWORD",
        "FAKE_MYSQL_LOG",
        "FAKE_MYSQL_ACTIVE_COUNT",
        "PATH"
    )
    $original = @{}

    foreach ($name in $names) {
        $original[$name] = [Environment]::GetEnvironmentVariable($name, "Process")
    }

    try {
        foreach ($name in $names) {
            [Environment]::SetEnvironmentVariable($name, $null, "Process")
        }
        foreach ($entry in $Environment.GetEnumerator()) {
            [Environment]::SetEnvironmentVariable(
                [string]$entry.Key,
                [string]$entry.Value,
                "Process")
        }

        $outputFile = Join-Path $temporaryDirectory (
            [guid]::NewGuid().ToString("N") + ".out.log")
        $errorFile = Join-Path $temporaryDirectory (
            [guid]::NewGuid().ToString("N") + ".err.log")
        $argumentList = @(
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            ('"' + $initializeScript + '"')
        ) + $Arguments
        $process = Start-Process -FilePath $powerShellExecutable `
            -ArgumentList $argumentList `
            -WindowStyle Hidden `
            -RedirectStandardOutput $outputFile `
            -RedirectStandardError $errorFile `
            -Wait `
            -PassThru
        $output = (
            (Get-Content -Raw $outputFile -ErrorAction SilentlyContinue) +
            (Get-Content -Raw $errorFile -ErrorAction SilentlyContinue))
        return [pscustomobject]@{
            ExitCode = $process.ExitCode
            Output = $output
        }
    } finally {
        foreach ($name in $names) {
            [Environment]::SetEnvironmentVariable(
                $name,
                $original[$name],
                "Process")
        }
    }
}

New-Item -ItemType Directory -Force $temporaryDirectory | Out-Null

try {
    $fakeMysql = @'
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
$joined = $Arguments -join " "
Add-Content -Path $env:FAKE_MYSQL_LOG -Value $joined
if ($joined.Contains("table_count")) {
    Write-Output "2"
}
if ($joined.Contains("active_count")) {
    Write-Output $env:FAKE_MYSQL_ACTIVE_COUNT
}
exit 0
'@
    Set-Content -Path (Join-Path $temporaryDirectory "mysql.ps1") `
        -Value $fakeMysql -Encoding UTF8

    $missingCredentials = Invoke-Initialize -Environment @{
        PATH = $temporaryDirectory
    }
    Assert-True ($missingCredentials.ExitCode -ne 0) `
        "缺少数据库凭据时必须退出非 0"
    Assert-True ($missingCredentials.Output -match "MYBLOG_DATASOURCE_USERNAME") `
        "缺少凭据时必须指出用户名环境变量"

    $wrongDatabase = Invoke-Initialize -Environment @{
        MYBLOG_DATASOURCE_URL = "jdbc:mysql://localhost:3306/not_myblog"
        MYBLOG_DATASOURCE_USERNAME = "contract-user"
        MYBLOG_DATASOURCE_PASSWORD = "contract-password"
        PATH = $temporaryDirectory
    }
    Assert-True ($wrongDatabase.ExitCode -ne 0) `
        "数据库名不正确时必须退出非 0"
    Assert-True ($wrongDatabase.Output -match "myblog_v2_dev") `
        "数据库名错误信息必须标明唯一允许的数据库"

    if (Test-Path $fakeMysqlLog) {
        Remove-Item $fakeMysqlLog -Force
    }
    $nonEmptyDatabase = Invoke-Initialize -Environment @{
        MYBLOG_DATASOURCE_URL = "jdbc:mysql://localhost:3306/myblog_v2_dev"
        MYBLOG_DATASOURCE_USERNAME = "contract-user"
        MYBLOG_DATASOURCE_PASSWORD = "contract-password"
        FAKE_MYSQL_LOG = $fakeMysqlLog
        FAKE_MYSQL_ACTIVE_COUNT = "2"
        PATH = $temporaryDirectory
    }
    Assert-True ($nonEmptyDatabase.ExitCode -ne 0) `
        "非空数据库未传 Reset 时必须退出非 0"
    Assert-True ($nonEmptyDatabase.Output -match "active") `
        ("非空数据库必须输出 active 数据拒绝原因。实际输出：" +
         $nonEmptyDatabase.Output)

    $mysqlCommands = Get-Content -Raw $fakeMysqlLog
    Assert-True ($mysqlCommands -notmatch "DROP DATABASE") `
        "未显式传 Reset 时不得执行 DROP DATABASE"

    Write-Host "initialize.ps1 contract tests passed"
} finally {
    Remove-Item $temporaryDirectory -Recurse -Force -ErrorAction SilentlyContinue
}
