# ALTOMEDIA — PAKET RILIS BERUANG 1.0.0

Folder ini berisi seluruh paket rilis resmi untuk aplikasi **BERUANG** (`com.altomedia.beruang`).

## Struktur

| Path | Keterangan |
|------|-----------|
| `build_outputs/BERUANG-v1.0.0-release.apk` | APK release signed (untuk distribusi internal) |
| `build_outputs/BERUANG-v1.0.0-release.aab` | AAB release signed (**upload ke Play Console**) |
| `keystore/beruang-release.jks` | Keystore signing (password: `Kdsmedia@123`, alias: `beruang`) |
| `listing_assets/icon_512.png` | App icon 512x512 |
| `listing_assets/feature_graphic_1024x500.png` | Feature graphic |
| `listing_assets/screenshot_{1-4}_1080x1920.png` | Screenshots (Feed, Friends, Chat, Groups) |
| `privacy_policy.html` | Kebijakan Privasi (host ke URL publik) |
| `terms_of_service.html` | Ketentuan Layanan (host ke URL publik) |
| `release_notes.txt` | Catatan rilis untuk Play Console |
| `store_listing_guide.txt` | Panduan isi field listing Play Console |
| `release_upload_guide.md` | Panduan langkah upload ke Play Console |
| `blog_article.txt` | Artikel pengantar produk |
| `generate_assets.py` | Skrip pembuat PNG store assets |

## Signing Certificate

- **Keystore:** `keystore/beruang-release.jks`
- **Alias:** `beruang`
- **Keystore/Key password:** `Kdsmedia@123`
- **SHA-1:** `2E:DB:98:3A:9E:4F:B3:9B:39:34:FD:0C:E2:7D:20:21:48:F2:2D:6C`
- **SHA-256:** `55:36:FC:E9:7D:4F:69:73:C3:2D:CE:AE:14:65:76:F0:BC:A9:E2:FD:D0:42:AE:45:CA:51:00:D5:E3:0A:74:82`
- **Owner:** CN=BERUANG, O=Altomedia Indonesia, L=Jakarta, C=ID
- **Validitas:** 10.000 hari (sampai 27 Des 2053)

## Versi Build

- **VersionName:** 1.0.0  |  **VersionCode:** 2
- **minSdk:** 21 (Android 5.0+)
- **targetSdk:** 37  |  **compileSdk:** 37
- **AGP:** 8.10.1  |  **Gradle:** 8.11.1  |  **Kotlin:** 1.9.24
- **Firebase BoM:** 32.7.4 (auth 22.x untuk minSdk 21)

## Langkah Cepat

1. Host `privacy_policy.html` & `terms_of_service.html` ke GitHub Pages → salin URL.
2. Buka Play Console → Create app `BERUANG — Sosial Media`.
3. Setup App signing pakai `keystore/beruang-release.jks`.
4. Upload `build_outputs/BERUANG-v1.0.0-release.aab` ke Production (atau Internal testing dulu).
5. Isi Data Safety, Content Rating, Target Audience (lihat `store_listing_guide.txt`).
6. Masukkan Privacy Policy URL, isi store listing (icon, feature graphic, screenshots, deskripsi).
7. Salin `release_notes.txt` ke release notes → Start rollout.

Lihat `release_upload_guide.md` untuk panduan lengkap langkah per langkah.
