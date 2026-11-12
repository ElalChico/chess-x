# tools/ · Flujo de desarrollo sin Android Studio

Scripts para iterar sobre el diseño de CHESS-X usando **ADB** y el móvil por USB,
sin necesidad de instalar Android Studio.

## Requisitos

- Android SDK con `platform-tools` (la ruta se lee de `local.properties` → `sdk.dir`).
- JDK 17 (el que usa el Gradle wrapper).
- El móvil conectado por USB con **Depuración USB** activada y este PC autorizado.
- El build `debug` está firmado con el mismo keystore que `release`
  (`signingConfig = prodDebug` en `app/build.gradle.kts`), así que se puede instalar
  **encima** de la app publicada sin perder perfil, ajustes ni historial.

## Uso

```powershell
# ciclo completo: compila + instala + relanza
tools\live.ps1

# además guarda la captura en docs/screens\home.png
tools\live.ps1 -Shot -Name home

# logcat del proceso de la app en vivo (Ctrl+C para salir)
tools\live.ps1 -Logs

# solo reinstala y relanza el APK ya compilado (ciclo más corto)
tools\live.ps1 -SkipBuild

# captura aislada de la pantalla actual
tools\shot.ps1 -Name ajustes

# interacción con el móvil desde el PC
tools\tap.ps1 -X 540 -Y 1200     # toque
tools\tap.ps1 -Key BACK           # tecla
tools\tap.ps1 -Swipe "540 1800 540 600 300"
```

## El ciclo de diseño

| Paso | Qué hacer |
|---|---|
| 1 | Cambias el diseño en el **APK** (`app/src/main/java/com/chessx/ui/...`) |
| 2 | `tools\live.ps1` → compila, instala y relanza en el móvil (~10-25 s incremental) |
| 3 | `tools\shot.ps1 -Name <pantalla>` → captura real en `docs/screens/` |
| 4 | Comparas la captura con `preview-home.html` (la maqueta) |
| 5 | Ajustas la maqueta para que vuelva a coincidir con el APK |

`preview-home.html` es una **maqueta de referencia**: replica 1:1 el Compose de la app.
El comentario de cabecera de ese archivo indica qué fichero Kotlin corresponde a cada
pantalla. `docs/screens/` guarda la verdad visual (capturas reales del dispositivo).

## Tiempos aproximados

| Situación | Tiempo |
|---|---|
| Primer build (descargando dependencias) | 3-6 min |
| Build incremental tras tocar un composable | 10-25 s |
| `installDebug` + relanzar | 3-6 s |
| Captura (`screencap` + `pull`) | < 1 s |

## Problemas frecuentes

**`El movil esta conectado pero SIN AUTORIZAR`**
Acepta el diálogo *Permitir depuracion USB* en el móvil.

**`No hay ningun movil conectado por ADB`**
Revisa el cable (debe ser de datos, no solo de carga), activa
*Ajustes → Ajustes de desarrollador → Depuración USB* y vuelve a enchufar.

**Pantalla en negro en la captura**
El móvil está bloqueado o apagado:
`adb shell input keyevent KEYCODE_WAKEUP` y `adb shell wm dismiss-keyguard`.

**`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**
La app instalada tiene otra firma. Se evita por el `prodDebug`; si ocurre,
desinstala (`adb uninstall com.chessx`) y vuelve a instalar.
