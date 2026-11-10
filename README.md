# CHESS-X · Ajedrez por Bluetooth entre amigos

**CHESS-X · © Elal Chico**

App de ajedrez para Android, simple y directa: conecta dos móviles por
**Bluetooth (SPP)** y juega una partida entre amigos, **en local**, sin internet,
sin cuentas y sin servidores.

## Qué es

CHESS-X es una app de ajedrez **solo Bluetooth**. Un móvil crea la partida
(anfitrión) y el otro se une (invitado). Una vez vinculados, podéis jugar con
reloj, sonidos y moovimientos legales comprobados por un motor propio.

- **Solo Bluetooth.** No usa WiFi, no usa datos, no usa la nube.
- **Juego local entre amigos/as.** Dos móviles, uno frente al otro.
- **Sin registro.** Solo escribes tu nombre; si lo dejas vacío se asigna un ID.

## Cómo se juega

1. **Anfitrión:** abre la app → «Crear partida» → «Escuchar conexiones».
2. **Invitado:** abre la app → «Unirse a partida» → «Buscar rivales» → toca el
   móvil del anfitrión.
3. Ambos móviles se vinculan por Bluetooth al instante.
4. El anfitrión configura reloj y color y pulsa **Comenzar**.
5. Cuenta atrás **3-2-1** sincronizada en los dos móviles y ¡a jugar!

En la partida: toca tu pieza, los puntos marcan los moovimientos legales, el rey
se pone rojo en jaque, y hay pausa, tablas y abandono.

## Características

- Motor de reglas propio (FIDE): jaque, jaque mate, rey ahogado, tablas,
  enroque, captura al paso y promoción.
- Reloj sincronizado entre ambos móviles (el anfitrión es la referencia).
- Sonidos estándar de ajedrez (movimiento, captura, jaque, mate, cuenta atrás).
- Tablero clásico con piezas vectoriales de alta calidad.
- Perfil con foto (recorte con zoom), nombre e historial de partidas.
- Modo torneo: varias partidas seguidas con colores alternos.
- Sin GPS y sin permisos raros: solo Bluetooth.
- Conjuntos de piezas vectoriales Lichess y diseños originales modernos.
- Coordenadas del tablero configurables desde Ajustes.

## Instalar

Descarga el APK de la última versión desde la sección **Releases** de este
repositorio (por ejemplo `chess-x-X.51.apk`) e instálalo en tu móvil Android
(Android 8 o superior). Solo necesitas activar el Bluetooth.

## Compilar tú mismo

```bash
./gradlew assembleDebug      # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest  # tests del motor de reglas
```

Requiere JDK 17 y Android SDK 34. Ábrelo también directamente en Android Studio.

## Estructura

```
app/src/main/java/com/chessx/
  core/       motor de ajedrez (reglas, movimientos, FEN/SAN/UCI)
  data/       Bluetooth SPP, protocolo de mensajes, reloj, sonidos
  ui/         pantalla y ViewModel del juego
```

---

Hecho por **Elal Chico** · CHESS-X

## Licencia

Código bajo **MIT** (`LICENSE`): puedes modificarlo, contribuir y redistribuirlo,
incluso comercialmente, siempre que mantengas el aviso de copyright
`© 2026 Elal Chico`.

Las contribuciones son bienvenidas: abre un issue o manda tu pull request.

Nota: las piezas (cburnett SVG) y los sonidos incluidos conservan sus licencias
originales y se mantienen atribuidos a sus autores.
