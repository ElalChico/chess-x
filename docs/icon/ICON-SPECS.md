# CHESS-X — Icono: especificaciones exactas

## Lienzo
- Viewport: **108 x 108 unidades** (el foreground adaptativo usa 108dp)
- Centro del lienzo: **(54, 54)**

## Zonas seguras
- **Zona segura (recomendada)**: círculo de centro (54,54), **radio 33** (diámetro 66). Todo lo importante debe caber aquí.
- Radio máximo antes de recorte en máscara circular: **54** (borde del lienzo)
- El launcher de tu móvil aplica máscara circular (la raíz de que se vea cortado)

## Colores
- Fondo oscuro (foreground actual): **#FF101820**
- Caballo (dorado): **#E2B94E**  (RGB 226,185,78)
- X (blanca): **#FFFFFFFF**, grosor 5.5, extremos redondeados (stroke-linecap round)
- Silueta para modo monocromo: mismo vector, el sistema lo pinta de un solo color

## Posición actual del caballo (origen de los archivos)
- Transform: `translate(6, 7.5)` + `scale(1.42)` sobre el path original
- Path original: x [6.00, 38.03]  y [7.00, 39.00]  (w 32.03 x h 32.00)
- **En el lienzo 108**: x [14.52, 60.00]  y [17.44, 62.88]  (w 45.48 x h 45.44)
- Centro visual: **(37.3, 40.2)** — está arriba-izquierda, no centrado
- **Distancia máxima al centro: 53.8** → casi tocando el borde circular (por esto se ve cortado)

## Posición actual de la X
- Stroke `M66,59 L75,68 M75,59 L66,68` → raya en diagonal, cruce en **(70.5, 63.5)**
- Con grosor 5.5, la X ocupa aprox: **x [63.3, 77.8], y [56.3, 70.8]**
- Toca/solapa ligeramente con la grupa del caballo en x 59–63

## Archivos
- `knight-solo.svg`  — caballo solo (fondo transparente) en la posición actual. Edita este.
- `knight-guide.svg` — caballo + retícula + círculo seguro + guía de X tenue (para maquetar encima)
- `knight-solo.png`  — render 2160x2160 transparente (para editores raster)
- `ICON-SPECS.md`    — este archivo

## Qué necesito cuando acabes
Edita el SVG (o dime las coordenadas), en concreto:
1. Escala/traslación del caballo (o el path nuevo si lo modificaste)
2. Coordenadas exactas (x1,y1)-(x2,y2) de la X y su grosor
3. Colores si los cambias

Con eso regenero el foreground (vector), el fondo, las capturas de verificación y publico al momento.
