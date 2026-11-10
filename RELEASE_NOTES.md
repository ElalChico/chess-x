# CHESS-X X.52 - Release Notes

## Nuevas características

**Actualizaciones in-app**
- Comprobación de versión contra GitHub Releases (formato tag X.NN)
- Descarga directa del APK e instalación mediante Intent
- Misma firma (keystore CN=CHESS-X): actualiza sin pérdida de datos (SharedPreferences, historial, fotos)

**Diálogo de fin de partida mejorado**
- Muestra foto y nombre del ganador
- Botón "Reproducir PGN" para abrir la reproducción completa
- Botones de configuración rápida de tiempo: 5+0, 3+2, 10+0, 15+10
- Opciones existentes conservadas: repetir mismo color, nuevo color aleatorio, siguiente torneo, volver a sala

**Historial PGN**
- Deduplicación por oponente + fecha (precisión minuto) + resultado + nº movimientos
- Guarda foto Base64 del oponente en cada registro
- Visualización con blancas primero (según color local)

**Reproducción de partidas (ReplayScreen)**
- Auto-play con velocidades 1x / 2x / 3x
- Controles: play / pausa / stop / navegación paso a paso
- Selector de velocidad integrado en la barra superior

**Bluetooth - Interfaz de dispositivos**
- Agrupación: dispositivos CHESS-X primero (acento dorado + barras RSSI)
- Dispositivos genéricos en sección separada
- Muestra rol (anfitrión/invitado), nombre de usuario y RSSI visual

**Notificación de conexión**
- Callback onConnected en BluetoothClassicManager
- Sonido de confirmación + toast temporal "¡Rival conectado!" al establecer socket SPP

**Protocolo de mensajes**
- RematchMessage extendido con timeMs e incrementMs para controles de tiempo personalizados
- Compatibilidad hacia atrás (valores -1 = usar configuración actual)

## Correcciones y mejoras

- versionCode 52 / versionName X.52
- Firmado con keystore de producción (CN=CHESS-X, OU=Dev, O=Elal Chico)
- Cuenta atrás 3-2-1 sincronizada entre dispositivos (host referencia)
- Sin permisos de ubicación (maxSdkVersion=30 para SPP)
- Licencia MIT, copyright © 2026 Elal Chico

## Instalación

Descargar `CHESS-X-X.52.apk` desde Releases e instalar.

Actualización desde X.46-X.51: `adb install -r CHESS-X-X.52.apk` (conserva datos por misma firma).

Requisitos: Android 8.0 (API 26) o superior, Bluetooth habilitado.