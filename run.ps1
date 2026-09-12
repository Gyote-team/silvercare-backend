$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Import-DotEnv([string]$path) {
    if (-not (Test-Path $path)) {
        return
    }
    Get-Content $path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $parts = $line.Split("=", 2)
        Set-Item -Path ("Env:" + $parts[0].Trim()) -Value $parts[1].Trim().Trim('"')
    }
}

$localEnv = Join-Path $PSScriptRoot ".env"
if (Test-Path $localEnv) {
    Import-DotEnv $localEnv
} else {
    # 기존 통합 작업 폴더와의 호환을 위해 부모 .env도 보조로 허용한다.
    Import-DotEnv (Join-Path $PSScriptRoot "..\.env")
}

.\mvnw.cmd spring-boot:run
