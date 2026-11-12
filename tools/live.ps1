<#
.SYNOPSIS
    Ciclo de desarrollo CHESS-X: compila, instala y relanza la app en el movil por ADB.

.DESCRIPTION
    Es la alternativa sin Android Studio para iterar sobre el diseno.
    El build debug esta firmado con el mismo keystore que release (ver app/build.gradle.kts),
    por lo que se puede instalar encima de la version publicada sin perder datos del perfil.

.PARAMETER SkipBuild
    No recompila: reinstala y relanza el APK ya generado.

.PARAMETER Shot
    Guarda ademas una captura del estado actual en docs/screens/<Name>.png.

.PARAMETER Logs
    Muestra el logcat del proceso de la app en vivo (Ctrl+C para salir).

.PARAMETER Name
    Nombre de la captura cuando se usa -Shot.

.EXAMPLE
    tools\live.ps1
    tools\live.ps1 -Shot -Name home
    tools\live.ps1 -Logs
#>
[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$Shot,
    [switch]$Logs,
    [string]$Name = "home",
    [string]$Task = ":app:installDebug"
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "_common.ps1")

$root = Get-RepoRoot
$adb = Get-AdbPath
$serial = Assert-Device -Adb $adb
Write-Ok "Movil detectado: $serial"

if (-not $SkipBuild) {
    Write-Step "Compilando e instalando ($Task)..."
    Push-Location $root
    try {
        & (Join-Path $root "gradlew.bat") $Task --console=plain
    }
    finally {
        Pop-Location
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Err "La compilacion ha fallado (codigo $LASTEXITCODE). No se instala nada."
        exit 1
    }
}
else {
    Write-Step "Saltando compilacion (-SkipBuild)..."
}

Write-Step "Reiniciando CHESS-X..."
& $adb shell am force-stop com.chessx | Out-Null
Start-Sleep -Milliseconds 500
& $adb shell am start com.chessx/.MainActivity | Out-Null
Start-Sleep -Seconds 2

$appPid = Get-AppPid -Adb $adb
if (-not $appPid) {
    Write-Err "La app no esta corriendo. Revisa logcat: tools\live.ps1 -SkipBuild -Logs"
    exit 2
}
Write-Ok "CHESS-X en marcha (pid $appPid)"

if ($Shot) {
    & (Join-Path $PSScriptRoot "shot.ps1") -Name $Name
}

if ($Logs) {
    Write-Step "logcat en vivo (Ctrl+C para salir)"
    & $adb logcat --pid=$appPid -v brief
}
