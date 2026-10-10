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

### 6. 🎨 Tampilan Minimalis & Fitur Tambahan
* **Ikon Mata Toggle Password:** Memudahkan pengisian password di layar Login.
* **Kredit Developer:** Teks *"Developed by Muhammad Fahmy"* di layar Login dan Profil Saya.
* **Adaptive Launcher Icon:** Ikon aplikasi modern bertema *Pink Accent* (`#EC4899`) dan *WiFi Billing*.

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
