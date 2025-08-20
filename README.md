# Assessment-03-Mobpro1

Membuat aplikasi sederhana menggunakan Jetpack Compose untuk memenuhi tugas Assessment 03 mata kuliah Mobpro 1

### Mahasiswa:
| Nama  | Grandia Muhammad |
|:-----:|------------------|
|  NIM  | 6706213096       |
| Kelas | D3RPLA 47-04     |

## Aplikasi Personal Gallery

### :page_facing_up: Deskripsi:
<p>
Personal Gallery adalah aplikasi galeri foto offline-first. Kamu bisa membuat, mengedit, dan menghapus foto lengkap dengan judul & deskripsi tanpa koneksi internet. Saat kamu login Google dan online, semua perubahan otomatis tersinkron ke server sehingga bisa diakses lintas perangkat.
</p>

### Fitur utama
<p>
Offline-first: tambah/edit/hapus foto tetap bisa meski tanpa internet. Sinkron otomatis: antrean perubahan akan diunggah saat kamu online & login.
Login Google (opsional): cukup untuk menyinkronkan data ke server; tanpa login pun tetap bisa memakai mode lokal.
Profil pengguna: foto profil bulat, nama, dan email kamu tampil rapi di menu Profil.
CRUD lengkap: tambah foto (dengan judul & deskripsi), edit, dan hapus (dengan dialog konfirmasi).
Upload gambar + teks: unggah foto dari galeri/kamera beserta caption.
Pembaruan realtime: daftar otomatis ter-update setelah berhasil menyimpan atau menghapus.
Penanganan jaringan: indikator loading dan pesan kesalahan saat internet tidak tersedia.
Cache & Room DB: data disimpan lokal memakai Room agar cepat dan hemat kuota.
</p>

### Cara kerja singkat
<p>
Tambah foto dari galeri/kamera → isi judul & deskripsi → Simpan.
Foto langsung tersimpan di penyimpanan lokal dan muncul di beranda.
Saat kamu login Google dan tersambung internet, aplikasi otomatis mensinkronkan foto ke server.
Kamu bisa edit atau hapus foto kapan saja; perubahan tetap aman di lokal lalu disinkron saat online.
</p>