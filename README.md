# 📡 tagihJaringan - Sistem Kasir & Billing WiFi Android Native

**tagihJaringan** adalah aplikasi Android Native berbasis **Kotlin** dan **Jetpack Compose** (Material Design 3) yang dirancang khusus untuk manajemen tagihan WiFi, kasir penagihan lapangan, dan isolir otomatis router MikroTik secara *real-time*.

**Developer:** **Muhammad Fahmy**  
**Repository GitHub:** [https://github.com/fahmy009/tagihJaringan](https://github.com/fahmy009/tagihJaringan)

---

## ✨ Fitur Utama Aplikasi

### 1. ⚡ Standalone Pure Firebase Firestore Database
* **Loading Instan (< 0,2 detik):** Aplikasi terbuka seketika tanpa ada *cold-start delay*.
* **Live Real-Time Sync (`addSnapshotListener`):** Pilihan Lunas, setoran, dan update data langsung ter-update seketika di seluruh perangkat HP tanpa perlu di-refresh.
* **Offline Mode Persistence:** Bekerja lancar di daerah susah sinyal dan otomatis menyinkronkan data saat kembali online.
* **In-App Dynamic Database Config:** Pengguna/klien dapat mengunggah file `google-services.json` atau menginput kredensial Firebase langsung dari layar Login tanpa perlu merubah kode APK!

### 2. 👥 Multi-Role & Multi-Profil Wilayah
* **ADMIN:** Memiliki wewenang penuh mengelola seluruh pelanggan dari semua profil wilayah, kelola akun user, dan pengaturan MikroTik.
* **MITRA (Pemilik Profile):** Mengelola laporan keuangan, edit harga, isolir massal, verifikasi setoran masuk, dan kelola user penagih yang terisolasi murni pada profil wilayah wewenangnya (dukungan multi-profil seperti `SUMBERGADUNG, PETTUNG`).
* **COLLECTOR (Tukang Tagih):** Layar khusus penagihan antrean pelanggan belum bayar, ringkasan *"Hasil Tagih Hari Ini"*, dialog *Setor Uang*, dan *Banner Peringatan Tanggungan Hutang Setoran*.

### 3. 💵 Akuntansi Keuangan & Verifikasi Setoran Fisik
* **Hasil Bersih Berbasis Uang Fisik Diterima (`amountReceived`):** Pemasukan kotor Mitra dihitung murni dari uang tunai fisik yang diserahkan penagih, mencegah selisih uang di atas kertas.
* **Pencatatan Selisih Hutang Penagih:** Jika terjadi selisih setoran (`selisihHutang > 0`), sistem otomatis mencatat hutang penagih dan menampilkan banner peringatan di HP penagih sampai dilunasi.
* **Potongan Operasional Murni per Wilayah (`PotonganItem.profile`):** Potongan operasional terisolasi murni per wilayah Mitra.
* **Tutup Buku & Analytics Bulanan:** Menyimpan *snapshot* riwayat laporan bulanan ke Firestore (`laporan_bulanan`) dan di-reset untuk bulan baru.

### 4. 🔒 Integrasi Router MikroTik REST API
* **Triple Combo Isolir:** Mengirim perintah `disabled: "true"` dan men-*kick* sesi aktif (`DELETE /rest/ppp/active`) secara instan.
* **Pencarian Cerdas Secret (*Smart Lookup*):** Mengisolasi secret per profil dan mendukung *fallback search* jika ada perbedaan casing nama.

### 5. 🚀 In-App GitHub Auto-Update
* Terhubung langsung ke **GitHub Releases API** (`fahmy009/tagihJaringan`).
* Mendeteksi rilis `.apk` baru, menampilkan pop-up catatan rilis (*changelog*), mendownload APK di latar belakang dengan indikator persen, dan membuka *Package Installer* Android secara otomatis.

---

## 📜 Panduan Setup Firebase Rules Permanen (Bebas Expired 30 Hari)

Agar database Firebase Firestore milik pengguna/klien **aktif permanen selamanya (tidak expired setelah 30 hari)**:

1. Buka **[Firebase Console](https://console.firebase.google.com/)** $\rightarrow$ buat Project baru.
2. Aktifkan **Cloud Firestore Database** $\rightarrow$ pilih lokasi server **`asia-southeast1 (Singapore)`**.
3. Masuk ke tab **Rules**, ganti kodenya dengan aturan permanen berikut:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

4. Klik **Publish**.
5. Download file `google-services.json` $\rightarrow$ buka aplikasi **tagihJaringan** di HP $\rightarrow$ klik **⚙️ Konfigurasi Firebase Database** di layar Login $\rightarrow$ upload file JSON tersebut. **Selesai!**

---

## 🛠️ Teknologi & Library Utama

* **Language:** Kotlin 2.1.0
* **UI Framework:** Jetpack Compose (Material 3) + Google Fonts ("Outfit")
* **Architecture:** MVVM (Model-View-ViewModel) + Coroutines & StateFlow
* **Database:** Firebase Firestore (`firebase-firestore-ktx`)
* **Local Storage:** DataStore Preferences
* **Network & Parsing:** Retrofit2, OkHttp3 (Unsafe Trust-All SSL for MikroTik Local IP), Gson
* **Min SDK:** 24 (Android 7.0 Nougat) | **Target SDK:** 35 (Android 15)

---

## 🚀 Panduan Kompilasi & Build (Gradle)

1. **Clone Repository:**
   ```bash
   git clone https://github.com/fahmy009/tagihJaringan.git
   ```

2. **Jalankan Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Build APK Debug:**
   ```bash
   ./gradlew assembleDebug
   ```
   *File APK hasil kompilasi berlokasi di:* `app/build/outputs/apk/debug/app-debug.apk`

---

© 2026 **tagihJaringan** • Developed by **Muhammad Fahmy**.
