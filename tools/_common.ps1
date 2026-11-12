# Funciones compartidas por los scripts de desarrollo con ADB.
# Localiza el SDK a partir de local.properties (sin rutas fijas en el repo).

function Get-RepoRoot {
    return (Split-Path $PSScriptRoot -Parent)
}

function Get-AdbPath {
    $root = Get-RepoRoot
    $lp = Join-Path $root "local.properties"
    if (-not (Test-Path $lp)) {
        throw "No se encuentra local.properties en $root (necesario para localizar el SDK)."
    }
    $raw = Get-Content $lp -Raw
    $m = [regex]::Match($raw, '(?m)^\s*sdk\.dir\s*=\s*(.+?)\s*$')
    if (-not $m.Success) {
        throw "No se encontro 'sdk.dir' en local.properties."
    }
    # local.properties escapa los backslashes: C\:\\Users\\...
    $sdk = $m.Groups[1].Value.Trim().Replace('\\', '\').Replace('\:', ':')
    $adb = Join-Path $sdk "platform-tools\adb.exe"
    if (-not (Test-Path $adb)) {
        throw "No se encuentra adb.exe en $adb."
    }
    return $adb
}

function Get-DeviceList {
    param([Parameter(Mandatory = $true)][string]$Adb)
    $out = & $Adb devices
    return @($out | Select-Object -Skip 1 | Where-Object { $_ -match '\S' })
}

function Assert-Device {
    <#
        Devuelve el serial del dispositivo listo para usar.
        Falla con un mensaje util si no hay movil, o si esta sin autorizar.
    #>
    param([Parameter(Mandatory = $true)][string]$Adb)

    $lines = Get-DeviceList -Adb $Adb
    foreach ($l in $lines) {
        if ($l -match '^(\S+)\s+device$') { return $Matches[1] }
    }

    $unauthorized = @($lines | Where-Object { $_ -match 'unauthorized' })
    if ($unauthorized.Count -gt 0) {
        throw "El movil esta conectado pero SIN AUTORIZAR. Acepta el dialogo 'Permitir depuracion USB' en el movil y vuelve a ejecutar."
    }
    $offline = @($lines | Where-Object { $_ -match 'offline' })
    if ($offline.Count -gt 0) {
        throw "El movil aparece 'offline'. Desconecta y vuelve a enchufar el cable USB."
    }
    throw "No hay ningun movil conectado por ADB. Conecta el cable USB, activa 'Depuracion USB' en Opciones de desarrollador y autoriza este PC."
}

function Get-AppPid {
    param([Parameter(Mandatory = $true)][string]$Adb, [string]$Package = "com.chessx")
    $out = & $Adb shell pidof $Package
    if ($out) { return ($out | Select-Object -First 1).Trim() }
    return ""
}

# Marcadores ASCII a proposito: la consola de Windows no usa UTF-8 por defecto
# y los simbolos Unicode saldrian como signos de interrogacion.
function Write-Step {
    param([string]$Message)
    Write-Host ">> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Err {
    param([string]$Message)
    Write-Host "[ERR] $Message" -ForegroundColor Red
}
