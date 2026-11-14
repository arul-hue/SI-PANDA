package com.sudes.purwodadi

import androidx.compose.runtime.*

@Composable
fun AdminDetailPengajuanScreen(
    pengajuanItem: PengajuanItem,
    fields: List<DetailFieldItem> = emptyList(), // Menerima fields dinamis yang dipetakan dari MainActivity
    onBackClicked: () -> Unit = {},
    onSelesaikanClicked: (PengajuanItem) -> Unit = {}
) {
    // Memanggil DetailPengajuanScreen secara langsung agar tampilan, status tracker,
    // data formulir, hingga pratinjau foto identik dan sebangun dengan sisi warga.
    DetailPengajuanScreen(
        jenisSurat = pengajuanItem.jenisSurat,
        status = pengajuanItem.status,
        tanggalPengajuan = pengajuanItem.tanggal,
        tanggalKeputusan = pengajuanItem.tanggalKeputusan,
        alasanDitolak = pengajuanItem.pesanStatus,
        pengantarUriString = pengajuanItem.pengantarUri,
        fields = fields,
        onBackClicked = onBackClicked,
        onResubmitClicked = { _ ->
            // Khusus admin, ketika tombol aksi bawah ditekan, arahkan ke form koreksi/edit
            onSelesaikanClicked(pengajuanItem)
        }
    )
}