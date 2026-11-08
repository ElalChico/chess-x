import re, os, urllib.request

PIECES = ['wK','wQ','wR','wB','wN','wP','bK','bQ','bR','bB','bN','bP']
OUT_DIR = r"C:\Users\elalc\Desktop\Proyecto\chess-bt android\app\src\main\res\drawable"

def resolve_color(c):
    if c is None: return None
    c = c.strip().lower()
    if c in ('#fff', '#ffffff', 'white'): return '#FFFFFF'
    if c in ('#000', '#000000', 'black'): return '#000000'
    if c == '#ececec': return '#ECECEC'
    return c

def parse_attrs(s):
    return {m.group(1): m.group(2) for m in re.finditer(r'([\w-]+)\s*=\s*"([^"]*)"', s or '')}

def svg_to_vector(svg_text):
    vb = re.search(r'viewBox="([^"]*)"', svg_text)
    parts = vb.group(1).split() if vb else ['0','0','45','45']
    vw, vh = parts[2], parts[3]

    # Flatten: walk tokens, tracking <g> stack attributes (with close handling)
    tokens = re.finditer(r'<(/?)(g|path|circle)\b([^>]*?)(/?)>', svg_text)
    gstack = []
    out_paths = []
    for m in tokens:
        closing, tag, attr_str, selfclose = m.group(1), m.group(2), m.group(3), m.group(4)
        attrs = parse_attrs(attr_str)
        if tag == 'g':
            if closing:
                if gstack:
                    gstack.pop()
            elif selfclose != '/':
                gstack.append(attrs)
            continue
        # Merge g attributes (nearest last), path overrides
        merged = {}
        for g in gstack:
            merged.update(g)
        merged.update(attrs)
        merged.pop('d', None)  # keep d separately

        if tag == 'circle':
            cx, cy, r = attrs.get('cx'), attrs.get('cy'), attrs.get('r')
            if cx and cy and r:
                d = "M %s,%s a %s,%s 0 1,0 %s,0 a %s,%s 0 1,0 -%s,0" % (
                    float(cx)-float(r), cy, r, r, float(r)*2, r, r, float(r)*2)
                merged['d'] = d
                tag = 'path'
            else:
                continue

        d = attrs.get('d')
        if not d:
            continue

        fill = merged.get('fill')
        stroke = merged.get('stroke')
        sw = merged.get('stroke-width')
        slc = merged.get('stroke-linecap')
        slj = merged.get('stroke-linejoin')
        fill_rule = merged.get('fill-rule')

        p = '    <path\n        android:pathData="%s"' % d
        fc = resolve_color(fill)
        if fill_rule == 'evenodd':
            p += '\n        android:fillType="evenOdd"'
        if fill is None or fill == 'none':
            p += '\n        android:fillColor="#00000000"'
        elif fc:
            p += '\n        android:fillColor="%s"' % fc
        sc = resolve_color(stroke)
        if sc and stroke != 'none':
            p += '\n        android:strokeColor="%s"' % sc
        if sw:
            p += '\n        android:strokeWidth="%s"' % sw
        if slc:
            p += '\n        android:strokeLineCap="%s"' % {'round':'round','butt':'butt','square':'square'}.get(slc, slc)
        if slj:
            p += '\n        android:strokeLineJoin="%s"' % {'round':'round','miter':'miter','bevel':'bevel'}.get(slj, slj)
        p += ' />'
        out_paths.append(p)

    return '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="45dp" android:height="45dp"
    android:viewportWidth="%s" android:viewportHeight="%s">
%s
</vector>''' % (vw, vh, '\n'.join(out_paths))

os.makedirs(OUT_DIR, exist_ok=True)
for piece in PIECES:
    url = "https://lichess1.org/assets/piece/cburnett/%s.svg" % piece
    with urllib.request.urlopen(url) as resp:
        svg = resp.read().decode('utf-8')
    vector = svg_to_vector(svg)
    filename = "piece_%s.xml" % piece.lower()
    with open(os.path.join(OUT_DIR, filename), 'w', encoding='utf-8') as f:
        f.write(vector)
    print("Created", filename)
print("Done!")
