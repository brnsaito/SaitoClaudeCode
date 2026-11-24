"""Imagem para Always On Display: fundo preto puro (economia OLED), só traço dourado + sol ferrugem."""
import sys, os, numpy as np
from PIL import Image, ImageDraw
sys.argv += [] ; OUT = sys.argv[1]
from wallpaper import ridge
S, SS = 1080, 3
img = Image.new("RGB", (S*SS, S*SS), (0, 0, 0)); d = ImageDraw.Draw(img)
GOLD, RUST = (184, 147, 90), (165, 80, 46)
cx, cy, R = .575*S, .565*S, 34
d.ellipse([(cx-R)*SS, (cy-R)*SS, (cx+R)*SS, (cy+R)*SS], fill=RUST)
n = 700
profiles = [ridge(n, .60*S, 10, .55, 21, [(.70, 70, .16), (.25, 30, .2)]),
            ridge(n, .70*S, 14, .55, 22, [(.12, 150, .18), (.62, 60, .14)]),
            ridge(n, .82*S, 16, .56, 23, [(.92, 170, .16), (.45, 50, .15)])]
xs = np.linspace(.06*S, .94*S, n)
for k, p in enumerate(profiles):
    # apaga o que está atrás (oclusão), depois traça a crista
    poly = [(x*SS, y*SS) for x, y in zip(xs, p)] + [(xs[-1]*SS, S*SS), (xs[0]*SS, S*SS)]
    d.polygon(poly, fill=(0, 0, 0))
    d.line([(x*SS, y*SS) for x, y in zip(xs, p)], fill=GOLD, width=int((2.4 - .3*k)*SS)+1, joint="curve")
# fade lateral para não ter corte seco
a = np.asarray(img.resize((S, S), Image.LANCZOS)).astype(np.float32)
x = np.linspace(0, 1, S)[None, :]
fade = np.clip(np.minimum(x, 1-x)/.12, 0, 1)
yv = np.linspace(0, 1, S)[:, None]; fade = fade*np.clip((1-yv)/.1, 0, 1)
Image.fromarray((a*fade[..., None]).astype(np.uint8)).save(os.path.join(OUT, "04_aod_always_on_display.png"))
print("aod ok")
