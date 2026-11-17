package com.sudes.purwodadi

enum class StatusPengajuan {
    PROSES, DITERIMA, DITOLAK
}

data class PengajuanItem(
    val id: String = "",
    val userId: String = "",
    val jenisSurat: String = "",
    val namaPemohon: String = "",
    val tanggal: String = "",
    val tanggalKeputusan: String = "",
    val status: StatusPengajuan = StatusPengajuan.PROSES,
    val pesanStatus: String = "",
    val nik: String = "",
    val tempatTanggalLahir: String = "",
    val jenisKelamin: String = "",
    val agama: String = "",
    val statusPerkawinan: String = "",
    val pekerjaan: String = "",
    val alamatLengkap: String = "",

    // Atribut Spesifik Surat Kematian
    val umur: String = "",
    val pukulWafat: String = "",
    val hariWafat: String = "",
    val tanggalWafat: String = "",
    val tempatWafat: String = "",
    val pukulPemakaman: String = "",
    val hariPemakaman: String = "",
    val tanggalPemakaman: String = "",
    val tempatPemakaman: String = "",
    val namaPelapor: String = "",
    val nikPelapor: String = "",
    val alamatPelapor: String = "",

    // Atribut Spesifik Surat Kehilangan
    val barangHilang: String = "",
    val hariKejadian: String = "",
    val tanggalKejadian: String = "",
    val jamKejadian: String = "",

    // Properti generik untuk kompatibilitas surat lain
    val namaUsaha: String = "",
    val lokasiUsaha: String = "",

    val pengantarUri: String = "",
    val nomorSurat: String = "-"
)