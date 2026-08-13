#!/usr/bin/env python3
"""Generate BERUANG Play Store listing assets (PNG) with the green/yellow theme."""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(OUT, "listing_assets")
os.makedirs(ASSETS, exist_ok=True)

GREEN = (34, 139, 87)
GREEN_DK = (20, 100, 60)
YELLOW = (255, 209, 26)
WHITE = (255, 255, 255)
DARK = (24, 30, 24)
GREY = (110, 120, 110)


def font(size, bold=False):
    cands = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf" if bold else
        "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    ]
    for c in cands:
        if os.path.exists(c):
            return ImageFont.truetype(c, size)
    return ImageFont.load_default()


def draw_bear_face(d, cx, cy, r, body=YELLOW, ear=GREEN):
    d.ellipse([cx - r, cy - int(r * 1.05), cx - int(r * 0.35), cy - int(r * 0.45)], fill=ear)
    d.ellipse([cx + int(r * 0.35), cy - int(r * 1.05), cx + r, cy - int(r * 0.45)], fill=ear)
    d.ellipse([cx - int(r * 0.8), cy - int(r * 0.95), cx - int(r * 0.5), cy - int(r * 0.6)], fill=body)
    d.ellipse([cx + int(r * 0.5), cy - int(r * 0.95), cx + int(r * 0.8), cy - int(r * 0.6)], fill=body)
    d.ellipse([cx - r, cy - int(r * 0.6), cx + r, cy + int(r * 0.9)], fill=body)
    mw = int(r * 0.7)
    d.ellipse([cx - mw // 2, cy + int(r * 0.15), cx + mw // 2, cy + int(r * 0.7)], fill=WHITE)
    nw = int(r * 0.22)
    d.ellipse([cx - nw, cy + int(r * 0.2), cx + nw, cy + int(r * 0.42)], fill=DARK)
    ew = int(r * 0.13)
    d.ellipse([cx - int(r * 0.45) - ew, cy - int(r * 0.25) - ew, cx - int(r * 0.45) + ew, cy - int(r * 0.25) + ew], fill=DARK)
    d.ellipse([cx + int(r * 0.45) - ew, cy - int(r * 0.25) - ew, cx + int(r * 0.45) + ew, cy - int(r * 0.25) + ew], fill=DARK)
    d.ellipse([cx - int(r * 0.42), cy - int(r * 0.28), cx - int(r * 0.38), cy - int(r * 0.24)], fill=WHITE)
    d.ellipse([cx + int(r * 0.38), cy - int(r * 0.28), cx + int(r * 0.42), cy - int(r * 0.24)], fill=WHITE)


def make_icon():
    S = 512
    img = Image.new("RGB", (S, S), WHITE)
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, S - 1, S - 1], fill=GREEN)
    d.ellipse([8, 8, S - 9, S - 9], outline=GREEN_DK, width=6)
    draw_bear_face(d, S // 2, int(S * 0.52), int(S * 0.32))
    img.save(os.path.join(ASSETS, "icon_512.png"))


def make_feature_graphic():
    W, H = 1024, 500
    img = Image.new("RGB", (W, H), GREEN)
    d = ImageDraw.Draw(img)
    d.polygon([(0, H), (W, int(H * 0.35)), (W, H), (0, H)], fill=GREEN_DK)
    draw_bear_face(d, 270, 250, 130)
    d.text((460, 150), "BERUANG", font=font(110, True), fill=YELLOW)
    d.text((465, 275), "Sosial Media Indonesia", font=font(38), fill=WHITE)
    d.text((465, 330), "Bagikan momenmu - Berteman - Ngobrol", font=font(26), fill=GREY)
    img.save(os.path.join(ASSETS, "feature_graphic_1024x500.png"))


def make_screenshot(idx, title, subtitle, mock_fn):
    W, H = 1080, 1920
    img = Image.new("RGB", (W, H), (245, 247, 245))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W, 160], fill=GREEN)
    d.text((40, 55), "BERUANG", font=font(54, True), fill=YELLOW)
    d.text((40, 200), title, font=font(48, True), fill=DARK)
    d.text((40, 260), subtitle, font=font(34), fill=GREY)
    mock_fn(d, W, H)
    d.rectangle([0, H - 130, W, H], fill=WHITE)
    for i, (label, x) in enumerate([("Home", 90), ("Friends", 280), ("Chat", 470), ("Groups", 660), ("Profile", 860)]):
        d.ellipse([x, H - 110, x + 60, H - 50], fill=GREEN if i == idx else GREY)
        d.text((x - 5, H - 45), label, font=font(22), fill=DARK)
    img.save(os.path.join(ASSETS, f"screenshot_{idx}_1080x1920.png"))


def mock_feed(d, W, H):
    y = 330
    for i in range(3):
        d.rectangle([40, y, W - 40, y + 360], fill=WHITE, outline=(225, 228, 225), width=2)
        d.ellipse([60, y + 20, 120, y + 80], fill=GREEN)
        d.text((140, y + 35), f"Pengguna BERUANG {i+1}", font=font(30, True), fill=DARK)
        d.text((140, y + 70), "2 jam lalu", font=font(22), fill=GREY)
        d.text((60, y + 110), "Hari yang indah bersama teman!", font=font(28), fill=DARK)
        d.rectangle([60, y + 160, W - 60, y + 330], fill=YELLOW)
        d.ellipse([60, y + 345, 90, y + 360], fill=GREEN)
        d.text((100, y + 343), "24 Suka   5 Komentar", font=font(22), fill=GREY)
        y += 390


def mock_friends(d, W, H):
    d.rectangle([40, 330, W - 40, 430], fill=WHITE, outline=(225, 228, 225), width=2)
    d.text((60, 350), "Permintaan Pertemanan", font=font(32, True), fill=DARK)
    for i in range(3):
        y = 470 + i * 110
        d.ellipse([60, y, 120, y + 60], fill=GREEN)
        d.text((140, y + 10), f"Teman {i+1}", font=font(28, True), fill=DARK)
        d.rectangle([W - 230, y + 8, W - 130, y + 52], fill=GREEN)
        d.text((W - 215, y + 18), "Terima", font=font(22, True), fill=WHITE)
        d.rectangle([W - 120, y + 8, W - 50, y + 52], fill=(180, 180, 180))
        d.text((W - 110, y + 18), "Tolak", font=font(22, True), fill=WHITE)
    d.text((60, 820), "Saran Teman", font=font(32, True), fill=DARK)
    for i in range(3):
        y = 880 + i * 110
        d.ellipse([60, y, 120, y + 60], fill=YELLOW)
        d.text((140, y + 10), f"Pengguna {i+4}", font=font(28, True), fill=DARK)
        d.rectangle([W - 180, y + 8, W - 50, y + 52], fill=GREEN)
        d.text((W - 160, y + 18), "Tambah", font=font(22, True), fill=WHITE)


def mock_chat(d, W, H):
    d.text((60, 330), "Pesan", font=font(32, True), fill=DARK)
    bubbles = [("Halo, apa kabar?", True), ("Baik dong, kamu?", False),
               ("Lagi santai di rumah", True), ("Wah asik, mau ngobrol?", False),
               ("Boleh!", True)]
    y = 400
    for msg, me in bubbles:
        tw = d.textlength(msg, font=font(30))
        bw = int(tw + 60)
        if me:
            d.rounded_rectangle([W - 40 - bw, y, W - 40, y + 70], radius=20, fill=GREEN)
            d.text((W - 40 - bw + 25, y + 18), msg, font=font(30), fill=WHITE)
        else:
            d.rounded_rectangle([40, y, 40 + bw, y + 70], radius=20, fill=WHITE, outline=(220, 222, 220), width=2)
            d.text((65, y + 18), msg, font=font(30), fill=DARK)
        y += 90
    d.rectangle([40, H - 200, W - 40, H - 140], fill=WHITE, outline=(220, 222, 220), width=2)
    d.text((70, H - 190), "Ketik pesan...", font=font(26), fill=GREY)
    d.ellipse([W - 130, H - 195, W - 65, H - 130], fill=GREEN)


def mock_groups(d, W, H):
    d.text((60, 330), "Grup Saya", font=font(32, True), fill=DARK)
    for i in range(4):
        y = 400 + i * 170
        d.rectangle([40, y, W - 40, y + 150], fill=WHITE, outline=(225, 228, 225), width=2)
        d.rectangle([60, y + 20, 200, y + 130], fill=YELLOW)
        d.text((220, y + 40), f"Grup BERUANG {i+1}", font=font(28, True), fill=DARK)
        d.text((220, y + 80), f"{(i+1)*23} anggota", font=font(24), fill=GREY)
        d.ellipse([220, y + 105, 250, y + 135], fill=GREEN)


def mock_wallet(d, W, H):
    d.text((60, 330), "Dompet Poin", font=font(32, True), fill=DARK)
    # Balance card
    d.rounded_rectangle([40, 400, W - 40, 620], radius=28, fill=GREEN)
    d.text((70, 440), "Saldo Poin", font=font(30), fill=YELLOW)
    d.text((70, 485), "12.450", font=font(88, True), fill=WHITE)
    d.text((70, 580), "Tier: BERUANG EMAS", font=font(26, True), fill=YELLOW)
    # Actions
    for i, label in enumerate(["Transfer Poin", "Pindai QR", "Riwayat Transaksi"]):
        y = 670 + i * 120
        d.rounded_rectangle([40, y, W - 40, y + 100], radius=20, fill=WHITE, outline=(225, 228, 225), width=2)
        d.ellipse([60, y + 20, 120, y + 80], fill=YELLOW)
        d.text((140, y + 32), label, font=font(30, True), fill=DARK)
    # Recent transaction
    d.text((60, 1050), "Transaksi Terbaru", font=font(30, True), fill=DARK)
    for i in range(3):
        y = 1110 + i * 90
        d.ellipse([60, y, 110, y + 50], fill=GREEN)
        d.text((130, y + 8), f"Transfer ke Teman {i+1}", font=font(26), fill=DARK)
        d.text((130, y + 38), "2 jam lalu", font=font(22), fill=GREY)
        d.text((W - 240, y + 14), f"-{(i+1)*50}", font=font(30, True), fill=(200, 60, 60))


if __name__ == "__main__":
    make_icon()
    make_feature_graphic()
    make_screenshot(1, "Feed Beranda", "Lihat postingan teman-temanmu", mock_feed)
    make_screenshot(2, "Pertemanan", "Terima & tambah teman baru", mock_friends)
    make_screenshot(3, "Pesan Langsung", "Ngobrol privat & chat global", mock_chat)
    make_screenshot(4, "Grup Komunitas", "Bergabung & buat grup sendiri", mock_groups)
    make_screenshot(0, "Dompet Poin", "Transfer poin lewat QR dengan aman", mock_wallet)
    print("Generated:", os.listdir(ASSETS))
