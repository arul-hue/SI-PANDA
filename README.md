# SI PANDA (Sistem Pelayanan Administrasi Desa) — Aplikasi Pelayanan Surat Desa Purwodadi Simpang

**Topik Proyek:** *Implementasi Sistem Pelayanan Persuratan Desa Berbasis Mobile dalam Mendukung Efisiensi Proses Administrasi di Desa Purwodadi Simpang*  
**Lokasi Mitra:** Desa Purwodadi Simpang, Kecamatan Tanjung Bintang, Kabupaten Lampung Selatan

**SI PANDA (Sistem Pelayanan Administrasi Desa)** adalah aplikasi mobile berbasis Android (Kotlin / Jetpack Compose) yang dirancang untuk mempermudah warga Desa Purwodadi Simpang dalam mengajukan berbagai surat keterangan administrasi secara daring (*online*) serta memantau status verifikasinya secara *real-time* langsung dari ponsel.

---

## Tim Pengembang — Kelompok 1

| NIM | Nama Anggota | Peran (*Role*) |
| :--- | :--- | :--- |
| `124140096` | **Syahrul Afwan** | Project Manager |
| `124140197` | **Muhammad Alfarabi** | Software Tester |
| `124140167` | **Sahal Alvin Zairy** | Frontend Developer |
| `124140190` | **Jundi Lamtara** | Backend Developer |
| `124140024` | **Roy Kurniawan** | UI/UX Design |

---

## Fitur Utama

### 1. Portal Warga
- **Autentikasi Mudah:** Mendukung pendaftaran dan login menggunakan Email/Kata Sandi serta **Google Sign-In**.
- **Pengajuan 5 Jenis Surat Administrasi:**
  1. Surat Keterangan Usaha (SKU)
  2. Surat Keterangan Domisili
  3. Surat Keterangan Tidak Mampu (SKTM)
  4. Surat Keterangan Kehilangan
  5. Surat Keterangan Kematian
- **Unggah Lampiran:** Mendukung pengambilan foto atau pemilihan berkas surat pengantar RT langsung dari perangkat.
- **Pelacakan Status Real-Time & Notifikasi:** Memantau progres pengajuan (`Diproses`, `Diterima`, atau `Ditolak`), menerima notifikasi saat status berubah, serta melihat catatan alasan dan mengajukan ulang apabila berkas ditolak.
- **Manajemen Profil Terpadu:** Pengisian otomatis (*auto-fill*) data kependudukan (NIK, TTL, alamat, pekerjaan, agama, status perkawinan) pada setiap formulir pengajuan.
- **Bantuan & Panduan:** Dilengkapi halaman Bantuan & Pertanyaan Umum (FAQ) mengenai alur pengajuan dan syarat 3 berkas fisik pengambilan surat di Balai Desa Purwodadi Simpang.

### 2. Dashboard Admin Desa
- **Manajemen Antrean Pengajuan:** Melihat seluruh daftar permohonan warga lengkap dengan pencarian nama/jenis surat, filter status (`Semua`, `Proses`, `Diterima`, `Ditolak`), dan pengurutan waktu.
- **Verifikasi Detail & Lampiran:** Memeriksa serta mengoreksi kelengkapan data diri pemohon dan memeriksa lampiran foto surat pengantar RT.
- **Keputusan, Pratinjau Cetak & Kirim PDF Otomatis:** Menerima atau menolak pengajuan dengan alasan yang jelas, memasukkan nomor surat resmi, menghasilkan pratinjau/cetak dokumen surat standar A4, serta mengirim otomatis file surat PDF ke email desa.

---

## Tampilan Aplikasi (Screenshot)

### 1. Portal Warga
| 1. Halaman Masuk | 2. Beranda Layanan Surat | 3. Formulir Pengajuan | 4. Status Pengajuan |
| :---: | :---: | :---: | :---: |
| <img width="720" height="1600" alt="Tampilan_All_Login" src="https://github.com/user-attachments/assets/d6e7ff45-4ae5-4a18-98be-481f5aaa65f6" />
 | <img src="LINK_Tampilan_User_Beranda" width="190" /> | <img src="LINK_Tampilan_User_Pengajuan_Surat" width="190" /> | <img src="LINK_Tampilan_User_Status" width="190" /> |

### 2. Profil Warga & Dashboard Admin Desa
| 5. Profil & Bantuan Warga | 6. Dashboard Pelayanan Admin | 7. Verifikasi & Keputusan Admin |
| :---: | :---: | :---: |
| <img src="LINK_Tampilan_User_Profil" width="210" /> | <img src="LINK_Tampilan_Admin_Daftar_Pengajuan_Surat" width="210" /> | <img src="LINK_Tampilan_Admin_Menolak_Surat" width="210" /> |

### 3. Pengiriman Otomatis Dokumen Surat PDF ke Email Desa
<p align="center">
  <img src="LINK_Foto_Kirim_Gmail" width="760" alt="Bukti Pengiriman Otomatis Surat PDF ke Gmail" />
</p>

---

## Teknologi yang Digunakan

- **Bahasa Pemrograman:** Kotlin (Kotlin Multiplatform)
- **UI Framework:** Jetpack Compose / Compose Multiplatform (Material Design 3)
- **Backend & Database:** Firebase Authentication, Cloud Firestore
- **Autentikasi Pihak Ketiga:** Android Credential Manager & Google Identity Services (Google Sign-In)
- **Generator Dokumen & Email:** HTML-to-PDF Generator (Android WebView / Print) & SMTP Mailer
- **Arsitektur & Latar Belakang:** MVVM (`ViewModel` & `StateFlow`), Android Service untuk pemantauan status notifikasi

---

## Unduh & Instalasi (Pengguna)

1. Buka halaman [**Releases**](https://github.com/arul-hue/SuDes/releases) pada repositori ini.
2. Unduh file `.apk` terbaru yang tersedia pada bagian **Assets**.
3. Buka file APK yang telah diunduh di perangkat Android Anda, lalu izinkan opsi **Instal dari sumber tidak dikenal** (*Install unknown apps*) jika diminta oleh sistem.
4. Jalankan aplikasi **SI PANDA**, lakukan pendaftaran akun baru atau masuk menggunakan akun Google, dan lengkapi data diri pada menu **Profil**.

---

## Panduan Pengembangan Lokal (Developer)

### Prasyarat
- Android Studio (versi terbaru yang mendukung Kotlin Multiplatform & Jetpack Compose)
- JDK 17 atau lebih baru
- Perangkat Android fisik atau Emulator (minimal Android 8.0 / API Level 26)

### Langkah Menjalankan Proyek
1. **Clone repositori ini:**
   ```bash
   git clone [https://github.com/arul-hue/SuDes.git](https://github.com/arul-hue/SuDes.git)
   cd SuDes
