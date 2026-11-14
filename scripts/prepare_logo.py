"""Siapkan logo LapakHunian untuk Android: trim margin putih + generate mipmap densities."""
from PIL import Image
import os

SRC = r"D:\Libraries\Desktop\Logo Lapak hunian.png"
OUT = r"D:\Libraries\Desktop\All Project\android lapakhunian\app\src\main\res"

img = Image.open(SRC).convert("RGB")
w, h = img.size
print("src", img.size)

# bbox piksel non-putih (toleransi 245)
gray = img.convert("L")
mask = gray.point(lambda p: 0 if p > 245 else 255)
bbox = mask.getbbox()
print("bbox", bbox)

# crop + padding kecil proporsional
if bbox:
    pad = int(min(bbox[2]-bbox[0], bbox[3]-bbox[1]) * 0.03)
    left = max(0, bbox[0] - pad)
    top = max(0, bbox[1] - pad)
    right = min(w, bbox[2] + pad)
    bottom = min(h, bbox[3] + pad)
    cropped = img.crop((left, top, right, bottom))
else:
    cropped = img
print("cropped", cropped.size)

# jadikan square (canvas putih di tengah)
cw, ch = cropped.size
side = max(cw, ch)
canvas = Image.new("RGB", (side, side), (255, 255, 255))
canvas.paste(cropped, ((side-cw)//2, (side-ch)//2))

# launcher legacy: 1 density cukup (xxxhdpi 192px)
launcher = canvas.resize((192, 192), Image.LANCZOS)
os.makedirs(f"{OUT}/mipmap-xxxhdpi", exist_ok=True)
launcher.save(f"{OUT}/mipmap-xxxhdpi/ic_launcher.png", optimize=True)

# adaptive foreground: logo kecil di tengah (aman dari mask lingkaran launcher)
fg = Image.new("RGB", (864, 864), (255, 255, 255))
inner = int(864 * 0.46)
resized = canvas.resize((inner, inner), Image.LANCZOS)
fg.paste(resized, ((864-inner)//2, (864-inner)//2))
os.makedirs(f"{OUT}/drawable", exist_ok=True)
fg.save(f"{OUT}/drawable/ic_launcher_foreground_logo.png", optimize=True)

# brand in-app (crop asli, bukan square)
cropped.save(f"{OUT}/drawable/logo_web.png", optimize=True)

# splash icon: logo kecil di tengah kanvas putih — splash API mem-mask lingkaran
splash_canvas = Image.new("RGB", (1152, 1152), (255, 255, 255))
inner_s = int(1152 * 0.46)
resized_s = canvas.resize((inner_s, inner_s), Image.LANCZOS)
splash_canvas.paste(resized_s, ((1152-inner_s)//2, (1152-inner_s)//2))
splash = splash_canvas
os.makedirs(f"{OUT}/drawable", exist_ok=True)
splash.save(f"{OUT}/drawable/splash_logo.png", optimize=True)

print("OK — files written")
