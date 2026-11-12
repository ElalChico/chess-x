<#
.SYNOPSIS
    Interactua con el movil por ADB: toques, deslizamientos y teclas.

.DESCRIPTION
    Permite navegar la app desde el PC sin tocar el telefono, para probar
    estados que no se pueden alcanzar de otra forma (sala, ajustes, partida...).

.PARAMETER X
    Coordenada X del toque.

.PARAMETER Y
    Coordenada Y del toque.

.PARAMETER Key
    Tecla Android (BACK, HOME, ENTER, 4, 66...). En vez de toque.

.PARAMETER Text
    Texto para escribir (equivale a adb shell input text).

.PARAMETER Swipe
    Deslizamiento "x1 y1 x2 y2 duracion_ms".

.EXAMPLE
    tools\tap.ps1 -X 540 -Y 1200
    tools\tap.ps1 -Key BACK
    tools\tap.ps1 -Swipe "540 1800 540 600 300"
#>
[CmdletBinding()]
param(
    [int]$X = -1,
    [int]$Y = -1,
    [string]$Key = "",
    [string]$Text = "",
    [string]$Swipe = ""
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "_common.ps1")

$adb = Get-AdbPath
$serial = Assert-Device -Adb $adb

if ($Swipe) {
    Write-Step "Deslizando ($Swipe)"
    & $adb shell input swipe $Swipe
}
elseif ($Key) {
    Write-Step "Enviando tecla $Key"
    & $adb shell input keyevent $Key
}
elseif ($Text) {
    Write-Step "Escribiendo '$Text'"
    & $adb shell input text ($Text -replace ' ', '%s')
}
elseif ($X -ge 0 -and $Y -ge 0) {
    Write-Step "Tocando ($X, $Y)"
    & $adb shell input tap $X $Y
}
else {
    throw "Indica -X/-Y, -Key, -Text o -Swipe."
}
