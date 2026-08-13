# ALTOMEDIA — PAKET RILIS BERUANG 1.2.0

Folder ini berisi seluruh paket rilis resmi untuk aplikasi **BERUANG** (`com.altomedia.beruang`).

## Struktur

| Path | Keterangan |
|------|-----------|
| `build_outputs/BERUANG-v1.2.0-release.apk` | APK release signed (untuk distribusi internal) |
| `build_outputs/BERUANG-v1.2.0-release.aab` | AAB release signed (**upload ke Play Console**) |
| `build_outputs/BERUANG-v1.2.0-debug.apk` | APK debug (untuk pengujian) |
| `keystore/ALTOMEDIA.jks` | Keystore signing (password: `Kdsmedia@123`, alias: `kdsmedia`) |
| `listing_assets/icon_512.png` | App icon 512x512 |
| `listing_assets/feature_graphic_1024x500.png` | Feature graphic |
| `listing_assets/screenshot_{1-5}_1080x1920.png` | Screenshots (Feed, Friends, Chat, Groups, Dompet Poin) |
| `privacy_policy.html` | Kebijakan Privasi (host ke URL publik) |
| `terms_of_service.html` | Ketentuan Layanan (host ke URL publik) |
| `release_notes.txt` | Catatan rilis untuk Play Console |
| `store_listing_guide.txt` | Panduan isi field listing Play Console |
| `release_upload_guide.md` | Panduan langkah upload ke Play Console |
| `blog_article.txt` | Artikel pengantar produk |
| `generate_assets.py` | Skrip pembuat PNG store assets |

## Signing Certificate

- **Keystore:** `keystore/ALTOMEDIA.jks`
- **Alias:** `kdsmedia`
- **Keystore/Key password:** `Kdsmedia@123`
- **SHA-1:** `39:85:53:F3:74:3D:58:17:89:6F:0C:97:BA:DA:7C:51:88:7D:D6:BA`
- **SHA-256:** `FA:B4:6B:F4:E0:1F:97:C5:77:23:ED:6D:95:42:EE:A0:5E:38:47:95:32:9B:1E:D9:50:4C:1C:8B:85:15:13:A9`
- **Owner:** CN=ALTOMEDIA, OU=Developer, O=ALTOMEDIA, L=Karawang, ST=Jawa Barat, C=ID
- **Validitas:** 10.000 hari (13 Agu 2026 s/d 29 Des 2053)

## Versi Build

- **VersionName:** 1.2.0  |  **VersionCode:** 4
- **minSdk:** 21 (Android 5.0+)
- **targetSdk:** 37  |  **compileSdk:** 37
- **Gradle:** 8.11.1  |  **Kotlin:** 1.9.24
- **Firebase:** Auth / Cloud Firestore / Storage (project `altomedia-indonesia`)

## Highlight Fitur 1.2.0

- Sosial media: feed beranda, story, postingan, like & komentar
- Login nomor telepon (format 08xxxxxxxx, tanpa OTP)
- Pertemanan, pesan privat & chat global, grup komunitas
- Notifikasi terpadu, profil kaya, emoji picker
- **Sistem Poin Virtual:** +20 posting / +50 komentar / +10 teman
- **Dompet QR & Transfer PIN:** transfer poin antar pengguna, atomik, PIN di-hash
- **Tier peringatan:** Perunggu, Perak, Emas, Platina

## Langkah Cepat

1. Host `privacy_policy.html` & `terms_of_service.html` ke GitHub Pages → salin URL.
2. Buka Play Console → Create app `BERUANG — Sosial Media`.
3. Setup App signing pakai `keystore/ALTOMEDIA.jks` (alias `kdsmedia`).
4. Upload `build_outputs/BERUANG-v1.2.0-release.aab` ke Production (atau Internal testing dulu).
5. Isi Data Safety, Content Rating, Target Audience (lihat `store_listing_guide.txt`).
6. Masukkan Privacy Policy URL, isi store listing (icon, feature graphic, screenshots, deskripsi).
7. Salin `release_notes.txt` ke release notes → Start rollout.

Lihat `release_upload_guide.md` untuk panduan lengkap langkah per langkah.

## Kontak

Pengembang: **ALTOMEDIA**
Email: altomediaindonesia@gmail.com

