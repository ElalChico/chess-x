<#
.SYNOPSIS
    Captura la pantalla actual del movil y la guarda en docs/screens/.

.DESCRIPTION
    Sirve como referencia visual REAL de la app (la fuente de verdad del diseno),
    frente a la maqueta HTML preview-home.html.

.PARAMETER Name
    Nombre del archivo PNG (sin extension).

.PARAMETER OutDir
    Carpeta de destino. Por defecto docs/screens.

.EXAMPLE
    tools\shot.ps1 -Name home
    tools\shot.ps1 -Name settings -OutDir C:\temp
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Name,
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "_common.ps1")

$root = Get-RepoRoot
$adb = Get-AdbPath
$serial = Assert-Device -Adb $adb

if (-not $OutDir) { $OutDir = Join-Path $root "docs\screens" }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$remote = "/sdcard/_chessx_shot.png"
Write-Step "Capturando pantalla en $serial..."
& $adb shell screencap -p $remote | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Err "screencap ha fallado."
    exit 1
}

$dest = Join-Path $OutDir "$Name.png"
& $adb pull $remote $dest | Out-Null
& $adb shell rm -f $remote | Out-Null

if (-not (Test-Path $dest)) {
    Write-Err "No se pudo descargar la captura a $dest."
    exit 1
}
Write-Ok "Captura guardada: $dest"
