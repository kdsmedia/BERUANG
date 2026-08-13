# PANDUAN UPLOAD RILIS & STORE LISTING — BERUANG

**App:** BERUANG — Sosial Media
**Package:** com.altomedia.beruang
**Project Firebase:** altomedia-indonesia
**Storage bucket:** altomedia-indonesia.firebasestorage.app
**VersionCode:** 4  |  **VersionName:** 1.2.0
**Min SDK:** 21 (Android 5.0)  |  **Target SDK:** 37  |  **Compile SDK:** 37
**Tanggal build:** 11 Agustus 2026

---

## A. LOKASI ARTIFAK

```
ALTOMEDIA/
├── build_outputs/
│   ├── BERUANG-v1.2.0-release.apk      ← untuk distribusi internal/sideloading
│   └── BERUANG-v1.2.0-release.aab      ← UNTUK PLAY CONSOLE (upload ini)
├── keystore/
│   └── ALTOMEDIA.jks            ← JANGAN HILANGKAN! (signing key)
├── listing_assets/
│   ├── icon_512.png                   ← App icon 512x512
│   ├── feature_graphic_1024x500.png   ← Feature graphic
│   ├── screenshot_1_1080x1920.png     ← Screenshot: Feed Beranda
│   ├── screenshot_2_1080x1920.png     ← Screenshot: Pertemanan
│   ├── screenshot_3_1080x1920.png     ← Screenshot: Pesan Langsung
│   └── screenshot_4_1080x1920.png     ← Screenshot: Grup Komunitas
├── privacy_policy.html                ← URL host ke GitHub Pages / hosting Anda
├── terms_of_service.html              ← URL host ke GitHub Pages / hosting Anda
├── release_notes.txt                  ← Catatan rilis (salin ke Play Console)
├── blog_article.txt                   ← Artikel pengantar produk
└── store_listing_guide.txt            ← Panduan isi field listing Play Console
```

---

## B. PANDUAN UPLOAD KE PLAY CONSOLE

### 1. Buat Aplikasi Baru
1. Login ke https://play.google.com/console
2. **Set up → Create app**
3. App name: `BERUANG — Sosial Media`
4. Default language: `Indonesia (id)`
5. App type: `Application`
6. Free/Paid: `Free`
7. Centang Declarations → **Create app**

### 2. Set up app signing (Play App Signing)
1. Buka **Setup → App signing**
2. Pilih **Use Play App Signing** (direkomendasikan)
3. Nanti upload AAB, Play akan minta upload key. Gunakan keystore di `keystore/ALTOMEDIA.jks`:
   - **Keystore password:** Kdsmedia@123
   - **Key alias:** kdsmedia
   - **Key password:** Kdsmedia@123
   - SHA-1 & SHA-256 dapat dari `keytool -list -v -keystore ALTOMEDIA.jks`

### 3. Setup app access
1. **Set up → App access** → pilih "All functionality is available without restrictions" (tidak ada login berbayar/pembayaran).

### 4. Setup ads
1. **Set up → Ads** → pilih "No, my app doesn't contain ads".

### 5. Content rating
1. **App content → Content rating** → isi kuesioner. Untuk sosial media, pilih kategori **Social**
2. Tandai tidak ada konten dewasa/kekerasan berlebih → rating biasanya PEGI 12 / Usia 13+.

### 6. Target audience
1. **App content → Target audience** → pilih 13+ (tidak untuk anak di bawah 13).

### 7. Privacy Policy (WAJIB)
1. Host file `privacy_policy.html` ke GitHub Pages (repo kdsmedia/BERUANG → Settings → Pages) atau hosting Anda.
2. Masukkan URL ke **App content → Privacy Policy**.
3. Contoh URL: https://kdsmedia.github.io/BERUANG/privacy_policy.html

### 8. Data safety (WAJIB)
Di **App content → Data safety**, nyatakan:
- Data dikumpulkan: Yes
- Email/contact: Yes (nomor telepon sebagai identitas)
- Photos/videos: Yes (user posts)
- App activity: Yes (post interactions)
- App history: Yes (timestamps)
- Penggunaan: App functionality, Account management
- Dibagikan: Yes (konten publik antar pengguna)
- Enkripsi in transit: Yes (HTTPS Firebase)
- Permintaan penghapusan: Yes

### 9. Government apps / Financial features
- Government apps: No
- Financial features: No

### 10. Upload AAB (Production)
1. Buka **Release → Production** (atau Testing → Internal untuk uji dulu)
2. **Create new release**
3. Upload: `build_outputs/BERUANG-v1.2.0-release.aab`
4. Release name: `BERUANG 1.2.0` (otomatis)
5. Release notes: salin dari `release_notes.txt`
6. **Review release** → selesaikan warning → **Start rollout to Production**

### 11. Store listing (Grow → Store presence → Main store listing)
- App name: `BERUANG — Sosial Media`
- Short description (max 80 char): `Sosial media khas Indonesia. Posting, berteman, & ngobrol bareng BERUANG!`
- Full description: salin dari `store_listing_guide.txt` bagian "Full Description"
- App icon: `icon_512.png` (512x512 PNG, 32-bit, tanpa alpha)
- Feature graphic: `feature_graphic_1024x500.png`
- Phone screenshots: upload minimal 2 (rekomendasi 4) dari `screenshot_*.png`
- Application category: **Social**
- Tags: Social, Community, Chat
- Privacy Policy URL: (sama dengan no.7)

---

## C. CATATAN KEAMANAN

- **Keystore `ALTOMEDIA.jks` adalah kunci signing resmi.** Simpan di tempat aman (password manager / backup terenkripsi). Jika hilang, Anda tidak bisa update app di Play Store dengan identitas yang sama.
- **Jangan commit keystore ke repo publik.** Repo kdsmedia/BERUANG saat ini privat; bila diubah ke publik, segera pindahkan keystore keluar dari repo.
- Kredensial:
  - Keystore password: `Kdsmedia@123`
  - Key alias: `kdsmedia`
  - Key password: `Kdsmedia@123`

---

## D. ENVIRONMENT BUILD (CATATAN TEKNIS)

Build dijalankan di environment dengan toolchain:
- JDK: OpenJDK 21
- Android SDK: Platform 37 (dir `android-37.0`, dibuat duplikat `android-37` untuk kompatibilitas AGP)
- AGP: 8.10.1
- Gradle: 8.11.1
- Kotlin: 1.9.24
- Compose compiler: 1.5.14
- Firebase BoM: 32.7.4 (dipilih karena BoM 33.x menarik firebase-auth 23.0.0 yang butuh minSdk 23, sedangkan target kita minSdk 21)

AGP 8.10.1 dipilih karena AGP 8.5.x tidak mendukung compileSdk 37. Symlink/duplikat direktori `android-37.0 → android-37` dibuat karena AGP mencari folder `android-<sdk>` sementara paket SDK 37 didistribusikan sebagai `android-37.0`.

---

## E. CHECKLIST RILIS

- [x] Kode bebas bug/dummy/simulation (audit T1–T6)
- [x] Build config: minSdk 21, targetSdk 37, compileSdk 37
- [x] Keystore ALTOMEDIA.jks dibuat (alias kdsmedia, Kdsmedia@123)
- [x] APK release signed
- [x] AAB release signed
- [x] Store listing assets (icon, feature graphic, 5 screenshots)
- [x] Privacy policy
- [x] Terms of service
- [ ] Host privacy policy + terms ke URL publik (GitHub Pages)
- [ ] Upload AAB ke Play Console
- [ ] Isi data safety, content rating, target audience
- [ ] Submit untuk review

---

## F. KONTAK

Pengembang: **ALTOMEDIA**
Email: altomediaindonesia@gmail.com
