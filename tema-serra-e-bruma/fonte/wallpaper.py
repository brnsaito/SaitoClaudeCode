"""Gerador do papel de parede 'Serra & Bruma'.
Paleta: E4D2A8 fundo | 3D2B1F contraste | 5B6B45 decoracao | B8935A traço | A5502E detalhe.
Uso: python3 wallpaper.py <saida_dir>
"""
import sys, math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = sys.argv[1] if len(sys.argv) > 1 else "."
W, H = 1440, 3200          # QHD+ (cobre S22-S25 Ultra e cortes FHD+)
SS = 2                     # supersampling das máscaras

def hx(h): return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32)
PAPER, INK, MOSS, GOLD, RUST = map(hx, ["E4D2A8", "3D2B1F", "5B6B45", "B8935A", "A5502E"])

def ridge(n, base, amp, rough, seed, peaks=()):
    """Perfil de crista 1D: deslocamento de ponto médio + picos guiados."""
    r = np.random.default_rng(seed)
    size = 1
    while size < n: size *= 2
    y = np.zeros(size + 1)
    y[0], y[-1] = r.normal(0, amp * .4), r.normal(0, amp * .4)
    step, a = size, amp
    while step > 1:
        h = step // 2
        for i in range(h, size, step):
            y[i] = (y[i - h] + y[i + h]) / 2 + r.normal(0, a)
        step, a = h, a * rough
    y = y[:n]
    x = np.linspace(0, 1, n)
    for (px, ph, pw) in peaks:                       # picos: posição, altura, largura
        y -= ph * np.exp(-np.abs((x - px) / pw) ** 1.5 * 2.2) * (1 + .18 * np.tanh((x - px) / pw * 3))
    return base + y

def mask_from(profile, trees=None, seed=0):
    """Máscara 'abaixo da crista' (+ silhuetas de pinheiros) em supersampling."""
    w, h = W * SS, H * SS
    img = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(img)
    xs = np.linspace(0, w - 1, len(profile))
    pts = [(float(x), float(y * SS)) for x, y in zip(xs, profile)]
    d.polygon(pts + [(w - 1, h), (0, h)], fill=255)
    if trees:
        r = np.random.default_rng(seed)
        x0, x1, hmin, hmax, dens = trees
        x = x0 * w
        while x < x1 * w:
            i = int(x / w * (len(profile) - 1))
            yb = profile[i] * SS + r.uniform(4, 14) * SS
            th = r.uniform(hmin, hmax) * SS
            tw = th * r.uniform(.15, .21)
            tiers = 4
            poly = []
            # pinheiro em camadas: lado esquerdo sobe, direito desce
            for k in range(tiers):
                f = k / tiers
                poly.append((x - tw * (1 - f) * .9, yb - th * f))
                poly.append((x - tw * (1 - f) * .45, yb - th * (f + .12)))
            poly.append((x, yb - th))
            for k in reversed(range(tiers)):
                f = k / tiers
                poly.append((x + tw * (1 - f) * .45, yb - th * (f + .12)))
                poly.append((x + tw * (1 - f) * .9, yb - th * f))
            d.polygon(poly, fill=255)
            x += tw * r.uniform(.7, 1.5) / dens
    return np.asarray(img.resize((W, H), Image.LANCZOS), dtype=np.float32) / 255

def noise2d(seed, scale, octaves=4):
    r = np.random.default_rng(seed)
    acc = np.zeros((H, W), np.float32); amp = 1; tot = 0
    for o in range(octaves):
        s = max(2, int(scale / 2 ** o))
        small = r.random((H // s + 2, W // s + 2)).astype(np.float32)
        im = Image.fromarray((small * 255).astype(np.uint8)).resize((W + 2 * s, H + 2 * s), Image.BICUBIC)
        acc += amp * np.asarray(im, np.float32)[s:s + H, s:s + W] / 255
        tot += amp; amp *= .5
    return acc / tot

def blur(a, r):
    im = Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8))
    return np.asarray(im.filter(ImageFilter.GaussianBlur(r)), np.float32) / 255

def lines_mask(polys, width):
    img = Image.new("L", (W * SS, H * SS), 0)
    d = ImageDraw.Draw(img)
    for p in polys:
        d.line([(x * SS, y * SS) for x, y in p], fill=255, width=int(width * SS), joint="curve")
    return np.asarray(img.resize((W, H), Image.LANCZOS), np.float32) / 255

def build(night=False, shift=0, sun=True):
    n = 1400
    yy = np.arange(H, dtype=np.float32)[:, None]
    sky_c, far_c = (PAPER, PAPER * .82 + MOSS * .18) if not night else (INK * .58, INK * .78 + MOSS * .12)
    # céu: papel com leve degradê vertical (mantém-se dentro do tom E4D2A8)
    t = yy / H
    canvas = np.broadcast_to(sky_c, (H, W, 3)).astype(np.float32).copy()
    canvas *= (1.012 - .03 * t)[..., None] if not night else (1.0 + .06 * t)[..., None]

    S = shift
    layers = [
        # (perfil, cor, árvores, névoa_baixo, fio_ouro, ferrugem)
        dict(p=ridge(n, 1660 + S, 30, .55, 11, [(.72, 150, .16), (.30, 60, .18)]), c=far_c, mix=1.0, fog=.6),
        dict(p=ridge(n, 1830 + S, 45, .55, 12, [(.18, 130, .13), (.84, 100, .12)]), c=MOSS, mix=.45, fog=.85,
             trees=(.02, .45, 26, 46, 1.0)),
        dict(p=ridge(n, 2030 + S, 55, .56, 13, [(.06, 400, .17), (.60, 170, .14)]), c=MOSS, mix=.8, fog=.75,
             trees=(.35, .98, 34, 60, 1.0), gold=True, rust=(.0, .18)),
        dict(p=ridge(n, 2330 + S, 60, .56, 14, [(.95, 450, .16), (.68, 150, .13)]), c=MOSS * .65 + INK * .35, mix=.92, fog=.6,
             trees=(.0, .55, 44, 80, .9), gold=True),
        dict(p=ridge(n, 2560 + S, 70, .57, 15, [(.07, 380, .19), (.52, 130, .15)]), c=INK, mix=1.0, fog=.32,
             trees=(.30, 1.0, 60, 110, .85), gold=True, rust=(.12, .30)),
        dict(p=ridge(n, 2860 + S, 60, .57, 16, [(.86, 260, .2), (.35, 100, .15)]), c=INK, mix=1.0, fog=.0,
             trees=(.0, .62, 90, 150, .8), gold=True),
    ]
    if night:
        for L in layers:
            L["c"] = L["c"] * .8 + INK * .2

    # sol: disco ferrugem, meio escondido atrás da serra distante
    if sun:
        cx, cy, R = W * .585, 1655 + S, 88
        d2 = (np.arange(W)[None, :] - cx) ** 2 + (yy - cy) ** 2
        disc = np.clip(R - np.sqrt(d2) + .5, 0, 1)
        if not night:
            canvas = canvas * (1 - disc[..., None]) + RUST * disc[..., None]
        else:  # lua: apenas um anel fino dourado (ouro nunca preenche)
            ring = np.clip(1.1 - np.abs(np.sqrt(d2) - R), 0, 1)
            canvas = canvas * (1 - ring[..., None]) + GOLD * ring[..., None]
        # véu de neblina horizontal fino cortando o sol

    fogn = noise2d(99, 260, 5)
    gold_cov = []
    for i, L in enumerate(layers):
        m = mask_from(L["p"], L.get("trees"), seed=200 + i)
        # sombreamento vertical dentro da camada (mais escuro na crista, névoa embaixo)
        crest = np.interp(np.arange(W), np.linspace(0, W - 1, n), L["p"])[None, :]
        depth = np.clip((yy - crest) / 340, 0, 1)
        col = np.broadcast_to(L["c"], (H, W, 3)).astype(np.float32)
        # textura de pedra sutil
        tex = (noise2d(300 + i, 40, 4) - .5) * .10
        col = col * (1 + tex[..., None])
        # ferrugem: face iluminada próxima à crista (lado esquerdo dos picos, como na referência)
        if "rust" in L:
            a, b = L["rust"]
            xs = np.arange(W)[None, :] / W
            band = ((xs > a) & (xs < b)).astype(np.float32)
            band = blur(np.broadcast_to(band, (H, W)).copy(), 30)
            near = np.clip(1 - (yy - crest) / 90, 0, 1) * (yy > crest)
            rn = noise2d(400 + i, 30, 3)
            rust_a = np.clip(band * near * (rn * 1.6 - .35), 0, .92)
            col = col * (1 - rust_a[..., None]) + RUST * rust_a[..., None]
        fog_a = np.clip(L["fog"] * depth ** 1.4 * (.55 + .7 * fogn), 0, .97)
        col = col * (1 - fog_a[..., None]) + sky_c * fog_a[..., None]
        a = m * L["mix"]
        canvas = canvas * (1 - a[..., None]) + col * a[..., None]
        if L.get("gold"):
            # fio de ouro (traço fino, nunca preenchimento): acompanha a crista
            # e desce em esporões curvos pela encosta; aplicado camada a camada
            r = np.random.default_rng(500 + i)
            xs = np.linspace(0, W - 1, n)
            p = L["p"]
            crest_line = [(x, y + 6) for x, y in zip(xs[::3], p[::3])]
            spurs = []
            for _ in range(4):
                j = int(r.uniform(.05, .95) * (n - 1))
                slope = p[min(j + 8, n - 1)] - p[max(j - 8, 0)]
                dirx = 1.0 if slope > 0 else -1.0          # desce para o lado em que a crista cai
                x, y = xs[j], p[j] + 7
                pts = [(x, y)]
                ang = .55                                    # começa suave, fica mais íngreme
                for s_ in range(22):
                    ang = min(1.25, ang + .035)
                    x += dirx * 9 * math.cos(ang); y += 9 * math.sin(ang) + 3
                    pts.append((x, y))
                spurs.append(pts)
            gm = np.clip(lines_mask([crest_line], 2.2) * .95 + lines_mask(spurs, 1.5) * .7 * m, 0, 1)
            gold_cov.append(gm)
            canvas = canvas * (1 - gm[..., None]) + GOLD * gm[..., None]
        # névoa entre camadas (respira acima da próxima crista)
        if i < len(layers) - 1:
            nxt = np.interp(np.arange(W), np.linspace(0, W - 1, n), layers[i + 1]["p"])[None, :]
            band = np.exp(-((yy - (nxt - 30)) / 70) ** 2) * (yy > crest)
            fa = np.clip(band * (.25 + .75 * fogn) * .75, 0, .8)
            canvas = canvas * (1 - fa[..., None]) + sky_c * fa[..., None]


    # névoa rasteira final no rodapé
    low = np.clip((yy - (H - 330)) / 330, 0, 1) ** 1.6 * (.35 + .65 * fogn)
    canvas = canvas * (1 - .3 * low[..., None]) + sky_c * .3 * low[..., None]

    # grão de papel
    g = np.random.default_rng(7).normal(0, 2.2, (H, W)).astype(np.float32)
    fib = (noise2d(808, 6, 2) - .5) * 5
    canvas += (g + fib)[..., None]
    return np.clip(canvas, 0, 255).astype(np.uint8)

def palette_share(img):
    pal = np.stack([PAPER, INK, MOSS, GOLD, RUST])
    px = img.reshape(-1, 3).astype(np.float32)[::7]
    d = ((px[:, None, :] - pal[None]) ** 2).sum(-1)
    idx = d.argmin(1)
    return {k: round(float((idx == i).mean() * 100), 1) for i, k in enumerate(["E4D2A8", "3D2B1F", "5B6B45", "B8935A", "A5502E"])}

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    jobs = [("01_tela_inicial.png", dict(shift=-380)), ("02_tela_bloqueio.png", dict(shift=170)), ("05_noturno_opcional.png", dict(night=True))]
    for name, kw in jobs:
        a = build(**kw)
        Image.fromarray(a).save(os.path.join(OUT, name), optimize=True)
        print(name, palette_share(a))
