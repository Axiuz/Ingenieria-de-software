param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArgs
)

$ErrorActionPreference = "Stop"

$propertiesFile = Join-Path $PSScriptRoot "maven-wrapper.properties"
$props = ConvertFrom-StringData (Get-Content -Raw $propertiesFile)

$distributionUrl = $props.distributionUrl
$expectedSha256 = $props.distributionSha256Sum

if (-not $distributionUrl) {
    throw "maven-wrapper.properties no contiene distributionUrl"
}

$fileName = Split-Path $distributionUrl -Leaf
$distributionName = $fileName -replace '-bin\.zip$', ''

if ($env:MAVEN_USER_HOME) {
    $mavenUserHome = $env:MAVEN_USER_HOME
} else {
    $mavenUserHome = Join-Path $HOME ".m2"
}

$installDir = Join-Path $mavenUserHome "wrapper\dists\$distributionName"
$mavenCmd = Join-Path $installDir "bin\mvn.cmd"

if (-not (Test-Path $mavenCmd)) {
    Write-Host "Descargando $distributionName por única vez..."

    $tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("tecmilenio-maven-" + [guid]::NewGuid())
    $zipFile = Join-Path $tempDir $fileName
    $extractDir = Join-Path $tempDir "extract"

    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    New-Item -ItemType Directory -Path $extractDir -Force | Out-Null

    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $client = New-Object System.Net.WebClient
        $client.DownloadFile($distributionUrl, $zipFile)

        if ($expectedSha256) {
            $actualSha256 = (Get-FileHash $zipFile -Algorithm SHA256).Hash.ToLowerInvariant()
            if ($actualSha256 -ne $expectedSha256.ToLowerInvariant()) {
                throw "El SHA-256 de la distribución Maven no coincide."
            }
        }

        Expand-Archive -Path $zipFile -DestinationPath $extractDir -Force

        $extractedHome = Join-Path $extractDir $distributionName
        if (-not (Test-Path (Join-Path $extractedHome "bin\mvn.cmd"))) {
            throw "La distribución descargada no contiene bin\mvn.cmd"
        }

        New-Item -ItemType Directory -Path (Split-Path $installDir -Parent) -Force | Out-Null
        if (Test-Path $installDir) {
            Remove-Item $installDir -Recurse -Force
        }

        Move-Item $extractedHome $installDir
    }
    finally {
        if (Test-Path $tempDir) {
            Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

& $mavenCmd @MavenArgs
exit $LASTEXITCODE
