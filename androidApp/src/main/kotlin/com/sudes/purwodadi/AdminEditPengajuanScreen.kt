package com.sudes.purwodadi

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.sudes.purwodadi.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun generateDefaultNomorSurat(jenisSurat: String): String {
    val tahunDinamis = Calendar.getInstance().get(Calendar.YEAR).toString()

    return when {
        jenisSurat.contains("Usaha", ignoreCase = true) -> "140 / ___ / VII.02.13 / IX / $tahunDinamis"
        jenisSurat.contains("Domisili", ignoreCase = true) -> "140 / ___ / VII.02.13 / IX / $tahunDinamis"
        jenisSurat.contains("Kehilangan", ignoreCase = true) -> "140 / ___ / LK / II / VII.02.03 / $tahunDinamis"
        jenisSurat.contains("Kematian", ignoreCase = true) -> "470 / ___ / IX / SKM / VII.02.03 / $tahunDinamis"
        jenisSurat.contains("Tidak Mampu", ignoreCase = true) -> "470 / ___ / VII.02.13 / IX / $tahunDinamis"
        else -> "140 / ___ / PEM-PD / $tahunDinamis"
    }
}

@Composable
fun AdminEditPengajuanScreen(
    pengajuan: PengajuanItem,
    onBackClicked: () -> Unit = {},
    onSaveSuccess: (updatedItem: PengajuanItem) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Inisialisasi state form data diri universal
    var nama by remember { mutableStateOf(pengajuan.namaPemohon) }
    var nik by remember { mutableStateOf(pengajuan.nik) }
    var ttl by remember { mutableStateOf(pengajuan.tempatTanggalLahir) }
    var jenisKelamin by remember { mutableStateOf(pengajuan.jenisKelamin) }
    var agama by remember { mutableStateOf(pengajuan.agama) }
    var statusPerkawinan by remember { mutableStateOf(pengajuan.statusPerkawinan) }
    var pekerjaan by remember { mutableStateOf(pengajuan.pekerjaan) }
    var alamatLengkap by remember { mutableStateOf(pengajuan.alamatLengkap) }

    // Cek Jenis Surat
    val isUsaha = pengajuan.jenisSurat.contains("Usaha", ignoreCase = true)
    val isDomisili = pengajuan.jenisSurat.contains("Domisili", ignoreCase = true)
    val isKehilangan = pengajuan.jenisSurat.contains("Kehilangan", ignoreCase = true)
    val isKematian = pengajuan.jenisSurat.contains("Kematian", ignoreCase = true)
    val isSktm = pengajuan.jenisSurat.contains("Tidak Mampu", ignoreCase = true)

    // State Khusus Kematian
    var umur by remember { mutableStateOf(pengajuan.umur) }
    var pukulWafat by remember { mutableStateOf(pengajuan.pukulWafat) }
    var hariWafat by remember { mutableStateOf(pengajuan.hariWafat) }
    var tanggalWafat by remember { mutableStateOf(pengajuan.tanggalWafat) }
    var tempatWafat by remember { mutableStateOf(pengajuan.tempatWafat) }

    var pukulPemakaman by remember { mutableStateOf(pengajuan.pukulPemakaman) }
    var hariPemakaman by remember { mutableStateOf(pengajuan.hariPemakaman) }
    var tanggalPemakaman by remember { mutableStateOf(pengajuan.tanggalPemakaman) }
    var tempatPemakaman by remember { mutableStateOf(pengajuan.tempatPemakaman) }

    var namaPelapor by remember { mutableStateOf(pengajuan.namaPelapor) }
    var nikPelapor by remember { mutableStateOf(pengajuan.nikPelapor) }
    var alamatPelapor by remember { mutableStateOf(pengajuan.alamatPelapor) }
    var hubunganPelapor by remember { mutableStateOf(pengajuan.namaUsaha) }

    // State Khusus Kehilangan (Prioritaskan properti terpisah, fallback ke format lama lokasiUsaha)
    val savedPartsKehilangan = if (isKehilangan) pengajuan.lokasiUsaha.split(" | ") else listOf()
    var hariKejadian by remember {
        mutableStateOf(
            pengajuan.hariKejadian.ifBlank {
                savedPartsKehilangan.find { it.startsWith("Hari:") }?.removePrefix("Hari: ") ?: ""
            }
        )
    }
    var tanggalKejadian by remember {
        mutableStateOf(
            pengajuan.tanggalKejadian.ifBlank {
                savedPartsKehilangan.find { it.startsWith("Tanggal:") }?.removePrefix("Tanggal: ") ?: ""
            }
        )
    }
    var jamKejadian by remember {
        mutableStateOf(
            pengajuan.jamKejadian.ifBlank {
                savedPartsKehilangan.find { it.startsWith("Jam:") }?.removePrefix("Jam: ") ?: ""
            }
        )
    }

    // Parsing data dinamis surat lain (Usaha, Domisili, Kehilangan, SKTM)
    val savedLokasi = pengajuan.lokasiUsaha
    val splitLokasi = if (isSktm) savedLokasi.split(" | Keperluan: ") else listOf()

    val initialField1 = when {
        isSktm -> if (splitLokasi.isNotEmpty() && splitLokasi[0].startsWith("No. KK: ")) splitLokasi[0].removePrefix("No. KK: ") else ""
        isKehilangan -> pengajuan.barangHilang.ifBlank {
            savedPartsKehilangan.find { it.startsWith("Barang:") }?.removePrefix("Barang: ") ?: pengajuan.namaUsaha
        }
        else -> pengajuan.namaUsaha
    }

    val initialField2 = when {
        isSktm -> if (splitLokasi.size > 1) splitLokasi[1] else savedLokasi
        else -> pengajuan.lokasiUsaha
    }

    var fieldExtra1 by remember { mutableStateOf(initialField1) }
    var fieldExtra2 by remember { mutableStateOf(initialField2) }

    // Inisialisasi state nomor surat dengan nilai default dinamis atau yang sudah tersimpan
    var nomorSurat by remember {
        mutableStateOf(
            if (pengajuan.nomorSurat.isNotBlank() && pengajuan.nomorSurat != "-")
                pengajuan.nomorSurat
            else
                generateDefaultNomorSurat(pengajuan.jenisSurat)
        )
    }

    // Pilihan Keputusan Kelurahan
    var isDiterima by remember { mutableStateOf(pengajuan.status != StatusPengajuan.DITOLAK) }
    var alasanDitolak by remember { mutableStateOf(if (pengajuan.status == StatusPengajuan.DITOLAK) pengajuan.pesanStatus else "") }

    // State Validasi, Dialog & Status Pengiriman Email
    var showErrorValidation by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showImagePreviewDialog by remember { mutableStateOf(false) }
    var showPdfHtmlPreviewDialog by remember { mutableStateOf(false) }
    var isSendingEmail by remember { mutableStateOf(false) }
    var sudahTerbitItem by remember {
        mutableStateOf(if (pengajuan.status == StatusPengajuan.DITERIMA) pengajuan else null)
    }

    val scrollState = rememberScrollState()

    fun buildCurrentUpdatedItem(keepExistingId: Boolean = false): PengajuanItem {
        val targetId = if (keepExistingId) pengajuan.id else System.currentTimeMillis().toString()
        val waktuKeputusan = getCurrentDateTime()

        val finalNamaUsaha = when {
            isKematian -> hubunganPelapor
            isSktm -> pengajuan.namaUsaha
            isKehilangan -> fieldExtra1
            else -> fieldExtra1
        }

        val finalLokasiUsaha = when {
            isKematian -> pengajuan.lokasiUsaha
            isSktm -> "No. KK: $fieldExtra1 | Keperluan: $fieldExtra2"
            isKehilangan -> "Hari: $hariKejadian | Tanggal: $tanggalKejadian | Jam: $jamKejadian | Barang: $fieldExtra1"
            else -> fieldExtra2
        }

        return pengajuan.copy(
            id = targetId,
            namaPemohon = nama,
            nik = nik,
            tempatTanggalLahir = ttl,
            jenisKelamin = jenisKelamin,
            agama = agama,
            statusPerkawinan = statusPerkawinan,
            pekerjaan = pekerjaan,
            alamatLengkap = alamatLengkap,
            umur = if (isKematian) umur else pengajuan.umur,
            pukulWafat = if (isKematian) pukulWafat else pengajuan.pukulWafat,
            hariWafat = if (isKematian) hariWafat else pengajuan.hariWafat,
            tanggalWafat = if (isKematian) tanggalWafat else pengajuan.tanggalWafat,
            tempatWafat = if (isKematian) tempatWafat else pengajuan.tempatWafat,
            pukulPemakaman = if (isKematian) pukulPemakaman else pengajuan.pukulPemakaman,
            hariPemakaman = if (isKematian) hariPemakaman else pengajuan.hariPemakaman,
            tanggalPemakaman = if (isKematian) tanggalPemakaman else pengajuan.tanggalPemakaman,
            tempatPemakaman = if (isKematian) tempatPemakaman else pengajuan.tempatPemakaman,
            namaPelapor = if (isKematian) namaPelapor else pengajuan.namaPelapor,
            nikPelapor = if (isKematian) nikPelapor else pengajuan.nikPelapor,
            alamatPelapor = if (isKematian) alamatPelapor else pengajuan.alamatPelapor,
            hariKejadian = if (isKehilangan) hariKejadian else pengajuan.hariKejadian,
            tanggalKejadian = if (isKehilangan) tanggalKejadian else pengajuan.tanggalKejadian,
            jamKejadian = if (isKehilangan) jamKejadian else pengajuan.jamKejadian,
            barangHilang = if (isKehilangan) fieldExtra1 else pengajuan.barangHilang,
            namaUsaha = finalNamaUsaha,
            lokasiUsaha = finalLokasiUsaha,
            nomorSurat = nomorSurat.ifBlank { "-" },
            status = if (isDiterima) StatusPengajuan.DITERIMA else StatusPengajuan.DITOLAK,
            pesanStatus = if (isDiterima) "Surat telah diterbitkan oleh perangkat desa. Silakan ambil di balai desa." else alasanDitolak,
            tanggalKeputusan = waktuKeputusan,
            pengantarUri = pengajuan.pengantarUri
        )
    }

    BackHandler(enabled = !isSendingEmail) {
        onBackClicked()
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = GreenDesaPrimary,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClicked,
                        enabled = !isSendingEmail
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Koreksi & Edit Data Warga",
                        style = AppTypography.HeadingMedium.copy(color = Color.White)
                    )
                }
            }
        },
        containerColor = GrayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // TOMBOL KIRIM ULANG OTOMATIS VIA EMAILJS (MUNCUL JIKA SURAT SUDAH TERBIT)
            sudahTerbitItem?.let { itemTerbit ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFECFDF5),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenDesaPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STATUS: SURAT TERBIT",
                                style = AppTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                color = GreenDesaPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = "Siap Kirim Ulang",
                                    style = AppTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                    color = GreenDesaPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Data surat resmi sudah tersimpan di database. Jika pengiriman email otomatis sebelumnya gagal karena gangguan jaringan, klik tombol di bawah ini:",
                            style = AppTypography.Caption,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isSendingEmail = true
                                coroutineScope.launch {
                                    EmailSenderHelper.sendEmailAutomatic(
                                        context = context,
                                        pengajuan = itemTerbit,
                                        onFinished = {
                                            isSendingEmail = false
                                        }
                                    )
                                }
                            },
                            enabled = !isSendingEmail,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSendingEmail) "Mengirim Surat ke Email..." else "Kirim Ulang Surat ke Email Admin",
                                style = AppTypography.ButtonText
                            )
                        }
                    }
                }
            }

            // INFO BANNER KUNING
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF3C7),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mode Koreksi & Edit Data Pemohon (${pengajuan.jenisSurat}):",
                        style = AppTypography.BodyRegular.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF92400E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Admin dapat langsung mengubah isian di bawah ini jika warga mengalami salah ketik (typo).",
                        style = AppTypography.Caption,
                        color = Color(0xFF78350F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KARTU FORMULIR EDIT DATA WARGA
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "FORMULIR DATA WARGA",
                        style = AppTypography.HeadingLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when {
                            isUsaha -> "Data Diri Warga"
                            isDomisili -> "Data Diri"
                            isKehilangan -> "Data Diri Pelapor"
                            isKematian -> "Data Diri Almarhum / Almarhumah"
                            else -> "Data Diri"
                        },
                        style = AppTypography.HeadingMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    AdminEditTextField(
                        label = "Nama Lengkap",
                        value = nama,
                        onValueChange = { nama = it },
                        isError = showErrorValidation && nama.isBlank()
                    )
                    AdminEditTextField(
                        label = "NIK",
                        value = nik,
                        onValueChange = { nik = it },
                        isError = showErrorValidation && nik.isBlank()
                    )

                    if (isKematian) {
                        AdminEditTextField(
                            label = "Umur",
                            value = umur,
                            onValueChange = { umur = it },
                            isError = showErrorValidation && umur.isBlank()
                        )
                    }

                    AdminEditTextField(
                        label = "Tempat, Tanggal Lahir",
                        value = ttl,
                        onValueChange = { ttl = it },
                        isError = showErrorValidation && ttl.isBlank()
                    )
                    if (!isKehilangan) {
                        AdminEditTextField(
                            label = "Jenis Kelamin",
                            value = jenisKelamin,
                            onValueChange = { jenisKelamin = it },
                            isError = showErrorValidation && jenisKelamin.isBlank()
                        )
                    }
                    if (isKehilangan) {
                        AdminEditTextField(
                            label = "Pekerjaan",
                            value = pekerjaan,
                            onValueChange = { pekerjaan = it },
                            isError = showErrorValidation && pekerjaan.isBlank()
                        )
                    }
                    AdminEditTextField(
                        label = "Agama",
                        value = agama,
                        onValueChange = { agama = it },
                        isError = showErrorValidation && agama.isBlank()
                    )
                    if (!isDomisili) {
                        AdminEditTextField(
                            label = "Status Perkawinan",
                            value = statusPerkawinan,
                            onValueChange = { statusPerkawinan = it },
                            isError = showErrorValidation && statusPerkawinan.isBlank()
                        )
                    }
                    if (!isKehilangan) {
                        AdminEditTextField(
                            label = "Pekerjaan",
                            value = pekerjaan,
                            onValueChange = { pekerjaan = it },
                            isError = showErrorValidation && pekerjaan.isBlank()
                        )
                    }
                    AdminEditTextField(
                        label = "Alamat Lengkap",
                        value = alamatLengkap,
                        onValueChange = { alamatLengkap = it },
                        singleLine = false,
                        minLines = 3,
                        maxLines = 4,
                        isError = showErrorValidation && alamatLengkap.isBlank()
                    )

                    // RENDER FIELD TAMBAHAN BERDASARKAN JENIS SURAT
                    when {
                        isKematian -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Detail Kematian", style = AppTypography.HeadingMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            AdminEditTextField(label = "Jam Wafat", value = pukulWafat, onValueChange = { pukulWafat = it }, isError = showErrorValidation && pukulWafat.isBlank())
                            AdminEditTextField(label = "Hari Wafat", value = hariWafat, onValueChange = { hariWafat = it }, isError = showErrorValidation && hariWafat.isBlank())
                            AdminEditTextField(label = "Tanggal Wafat", value = tanggalWafat, onValueChange = { tanggalWafat = it }, isError = showErrorValidation && tanggalWafat.isBlank())
                            AdminEditTextField(label = "Tempat Wafat", value = tempatWafat, onValueChange = { tempatWafat = it }, isError = showErrorValidation && tempatWafat.isBlank())

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Detail Pemakaman", style = AppTypography.HeadingMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            AdminEditTextField(label = "Jam Pemakaman", value = pukulPemakaman, onValueChange = { pukulPemakaman = it }, isError = showErrorValidation && pukulPemakaman.isBlank())
                            AdminEditTextField(label = "Hari Pemakaman", value = hariPemakaman, onValueChange = { hariPemakaman = it }, isError = showErrorValidation && hariPemakaman.isBlank())
                            AdminEditTextField(label = "Tanggal Pemakaman", value = tanggalPemakaman, onValueChange = { tanggalPemakaman = it }, isError = showErrorValidation && tanggalPemakaman.isBlank())
                            AdminEditTextField(label = "Tempat Pemakaman (TPU)", value = tempatPemakaman, onValueChange = { tempatPemakaman = it }, isError = showErrorValidation && tempatPemakaman.isBlank())

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Data Pelapor", style = AppTypography.HeadingMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            AdminEditTextField(label = "Nama Pelapor", value = namaPelapor, onValueChange = { namaPelapor = it }, isError = showErrorValidation && namaPelapor.isBlank())
                            AdminEditTextField(label = "NIK Pelapor", value = nikPelapor, onValueChange = { nikPelapor = it }, isError = showErrorValidation && nikPelapor.isBlank())
                            AdminEditTextField(label = "Alamat Pelapor", value = alamatPelapor, onValueChange = { alamatPelapor = it }, singleLine = false, minLines = 2, maxLines = 3, isError = showErrorValidation && alamatPelapor.isBlank())
                            AdminEditTextField(label = "Status / Hubungan dengan Almarhum", value = hubunganPelapor, onValueChange = { hubunganPelapor = it }, isError = showErrorValidation && hubunganPelapor.isBlank())
                        }
                        isUsaha -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Detail Usaha", style = AppTypography.HeadingMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            AdminEditTextField(
                                label = "Nama / Deskripsi Usaha",
                                value = fieldExtra1,
                                onValueChange = { fieldExtra1 = it },
                                isError = showErrorValidation && fieldExtra1.isBlank()
                            )
                            AdminEditTextField(
                                label = "Lokasi Usaha",
                                value = fieldExtra2,
                                onValueChange = { fieldExtra2 = it },
                                singleLine = false,
                                minLines = 2,
                                maxLines = 3,
                                isError = showErrorValidation && fieldExtra2.isBlank()
                            )
                        }
                        isDomisili -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Detail Domisili", style = AppTypography.HeadingMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            AdminEditTextField(
                                label = "Alamat Tempat Tinggal Saat Ini",
                                value = fieldExtra2,
                                onValueChange = { fieldExtra2 = it },
                                singleLine = false,
                                minLines = 2,
                                maxLines = 3,
                                isError = showErrorValidation && fieldExtra2.isBlank()
                            )
                        }
                        isKehilangan -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Detail Kehilangan", style = AppTypography.HeadingMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            AdminEditTextField(
                                label = "Hari Kejadian",
                                value = hariKejadian,
                                onValueChange = { hariKejadian = it },
                                placeholder = "Contoh: Senin",
                                isError = showErrorValidation && hariKejadian.isBlank()
                            )
                            AdminEditTextField(
                                label = "Tanggal Kejadian",
                                value = tanggalKejadian,
                                onValueChange = { tanggalKejadian = it },
                                placeholder = "Contoh: 12 Oktober 2026",
                                isError = showErrorValidation && tanggalKejadian.isBlank()
                            )
                            AdminEditTextField(
                                label = "Jam / Waktu Kejadian",
                                value = jamKejadian,
                                onValueChange = { jamKejadian = it },
                                placeholder = "Contoh: 14.30 WIB",
                                isError = showErrorValidation && jamKejadian.isBlank()
                            )
                            AdminEditTextField(
                                label = "Barang / Surat yang Hilang",
                                value = fieldExtra1,
                                onValueChange = { fieldExtra1 = it },
                                placeholder = "Contoh: KTP / Dompet / Sertifikat",
                                singleLine = false,
                                minLines = 2,
                                maxLines = 3,
                                isError = showErrorValidation && fieldExtra1.isBlank()
                            )
                        }
                        isSktm -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Detail Keluarga & Keperluan", style = AppTypography.HeadingMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            AdminEditTextField(
                                label = "Nomor Kartu Keluarga (KK)",
                                value = fieldExtra1,
                                onValueChange = { fieldExtra1 = it },
                                placeholder = "Masukkan nomor KK",
                                isError = showErrorValidation && fieldExtra1.isBlank()
                            )
                            AdminEditTextField(
                                label = "Nama Kepala Keluarga (di KK)",
                                value = pengajuan.namaUsaha,
                                onValueChange = { },
                                isError = false
                            )
                            AdminEditTextField(
                                label = "Keperluan Pembuatan Surat",
                                value = fieldExtra2,
                                onValueChange = { fieldExtra2 = it },
                                singleLine = false,
                                minLines = 2,
                                maxLines = 3,
                                isError = showErrorValidation && fieldExtra2.isBlank()
                            )
                        }
                    }

                    // Nomor surat resmi wajib diisi jika status diterima/terbitkan
                    AdminEditTextField(
                        label = "Nomor Surat Resmi (Diterbitkan Admin)",
                        value = nomorSurat,
                        onValueChange = { nomorSurat = it },
                        placeholder = "Masukkan nomor surat resmi",
                        isError = showErrorValidation && isDiterima && (nomorSurat.isBlank() || nomorSurat.contains("___"))
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Surat Pengantar RT",
                        style = AppTypography.Caption,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = GrayBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = GreenDesaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (pengajuan.pengantarUri.isNotBlank()) "Berkas Pengantar RT Terlampir" else "Tidak ada lampiran",
                                    style = AppTypography.BodyRegular.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                            }

                            if (pengajuan.pengantarUri.isNotBlank()) {
                                TextButton(onClick = { showImagePreviewDialog = true }) {
                                    Text(
                                        text = "Lihat Foto",
                                        style = AppTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                        color = GreenDesaPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KARTU PILIHAN KEPUTUSAN KELURAHAN
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "PILIHAN KEPUTUSAN KELURAHAN",
                        style = AppTypography.HeadingMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Opsi Diterima & Terbitkan
                    Surface(
                        onClick = { isDiterima = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDiterima) GreenDesaLight else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDiterima) GreenDesaPrimary else GrayBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isDiterima,
                                onClick = { isDiterima = true },
                                colors = RadioButtonDefaults.colors(selectedColor = GreenDesaPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Status: Diterima & Terbitkan Surat",
                                style = AppTypography.BodyRegular.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDiterima) GreenDesaPrimary else TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opsi Ditolak Pengajuan
                    Surface(
                        onClick = { isDiterima = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isDiterima) RedRejectedContainer else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isDiterima) RedRejectedText else GrayBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !isDiterima,
                                onClick = { isDiterima = false },
                                colors = RadioButtonDefaults.colors(selectedColor = RedRejectedText)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Status: Ditolak Pengajuan",
                                style = AppTypography.BodyRegular.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isDiterima) RedRejectedText else TextPrimary
                                )
                            )
                        }
                    }

                    if (!isDiterima) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Alasan Penolakan:",
                                style = AppTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                color = RedRejectedText
                            )
                            if (showErrorValidation && alasanDitolak.isBlank()) {
                                Text(
                                    text = "Wajib diisi",
                                    style = AppTypography.Caption.copy(
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = alasanDitolak,
                            onValueChange = { alasanDitolak = it },
                            placeholder = {
                                Text(
                                    "Contoh: Foto surat pengantar RT buram / tidak terbaca.",
                                    style = AppTypography.BodyRegular,
                                    color = TextPlaceholder
                                )
                            },
                            textStyle = AppTypography.BodyRegular.copy(fontWeight = FontWeight.SemiBold, color = RedRejectedText),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 4,
                            isError = showErrorValidation && alasanDitolak.isBlank(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = RedRejectedContainer.copy(alpha = 0.5f),
                                unfocusedContainerColor = RedRejectedContainer.copy(alpha = 0.3f),
                                focusedBorderColor = RedRejectedText,
                                unfocusedBorderColor = RedRejectedText.copy(alpha = 0.5f),
                                cursorColor = RedRejectedText,
                                errorBorderColor = Color(0xFFDC2626)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TOMBOL PRATINJAU DOKUMEN SURAT FULL SCREEN ZOOMABLE
            if (isDiterima) {
                OutlinedButton(
                    onClick = { showPdfHtmlPreviewDialog = true },
                    enabled = !isSendingEmail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenDesaPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenDesaPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pratinjau Dokumen Surat (Zoom)",
                        style = AppTypography.ButtonText.copy(color = GreenDesaPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // TOMBOL AKSI SIMPAN
            Button(
                onClick = {
                    val isBaseValid = nama.isNotBlank() &&
                            nik.isNotBlank() &&
                            ttl.isNotBlank() &&
                            (isKehilangan || jenisKelamin.isNotBlank()) &&
                            agama.isNotBlank() &&
                            (isDomisili || statusPerkawinan.isNotBlank()) &&
                            pekerjaan.isNotBlank() &&
                            alamatLengkap.isNotBlank()

                    val isSpecificValid = when {
                        isKematian -> umur.isNotBlank() && pukulWafat.isNotBlank() && hariWafat.isNotBlank() &&
                                tanggalWafat.isNotBlank() && tempatWafat.isNotBlank() && pukulPemakaman.isNotBlank() &&
                                hariPemakaman.isNotBlank() && tanggalPemakaman.isNotBlank() && tempatPemakaman.isNotBlank() &&
                                namaPelapor.isNotBlank() && nikPelapor.isNotBlank() && alamatPelapor.isNotBlank() &&
                                hubunganPelapor.isNotBlank()
                        isUsaha -> fieldExtra1.isNotBlank() && fieldExtra2.isNotBlank()
                        isDomisili -> fieldExtra2.isNotBlank()
                        isKehilangan -> hariKejadian.isNotBlank() && tanggalKejadian.isNotBlank() &&
                                jamKejadian.isNotBlank() && fieldExtra1.isNotBlank()
                        isSktm -> fieldExtra1.isNotBlank() && fieldExtra2.isNotBlank()
                        else -> true
                    }

                    val isDecisionValid = if (isDiterima) {
                        nomorSurat.isNotBlank() && !nomorSurat.contains("___")
                    } else {
                        alasanDitolak.isNotBlank()
                    }

                    if (isBaseValid && isSpecificValid && isDecisionValid) {
                        showErrorValidation = false
                        showConfirmDialog = true
                    } else {
                        showErrorValidation = true
                    }
                },
                enabled = !isSendingEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
            ) {
                Text(
                    text = if (isSendingEmail) "Mengirim Surat..." else "Simpan Keputusan & Proses",
                    style = AppTypography.ButtonText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBackClicked,
                enabled = !isSendingEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder)
            ) {
                Text(
                    text = "Kembali ke Daftar",
                    style = AppTypography.ButtonText.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // DIALOG PRATINJAU FOTO PENGANTAR RT FULL SCREEN (PERMANEN BASE64 & URI)
    if (showImagePreviewDialog && pengajuan.pengantarUri.isNotBlank()) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

        val permanentBitmap = remember(pengajuan.pengantarUri) {
            if (pengajuan.pengantarUri.startsWith("data:image")) {
                ImageStorageHelper.base64ToBitmap(pengajuan.pengantarUri)
            } else {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.Black,
            shape = RoundedCornerShape(0.dp),
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {},
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale == 1f) androidx.compose.ui.geometry.Offset.Zero else offset + pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (permanentBitmap != null) {
                        Image(
                            bitmap = permanentBitmap.asImageBitmap(),
                            contentDescription = "Pratinjau Full Screen Surat Pengantar RT",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        coil.compose.AsyncImage(
                            model = Uri.parse(pengajuan.pengantarUri),
                            contentDescription = "Pratinjau Full Screen Surat Pengantar RT",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showImagePreviewDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
                    ) {
                        Text(text = "Tutup Pratinjau", style = AppTypography.ButtonText)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // DIALOG PRATINJAU DOKUMEN SURAT FULL SCREEN (A4 & BISA DI-ZOOM)
    if (showPdfHtmlPreviewDialog) {
        val previewHtml = remember(
            nama, nik, ttl, jenisKelamin, agama, statusPerkawinan, pekerjaan, alamatLengkap,
            umur, pukulWafat, hariWafat, tanggalWafat, tempatWafat, pukulPemakaman, hariPemakaman,
            tanggalPemakaman, tempatPemakaman, namaPelapor, nikPelapor, alamatPelapor, hubunganPelapor,
            hariKejadian, tanggalKejadian, jamKejadian, fieldExtra1, fieldExtra2, nomorSurat
        ) {
            SuratHtmlGenerator.generateHtml(context, buildCurrentUpdatedItem(keepExistingId = true))
        }

        AlertDialog(
            onDismissRequest = { },
            containerColor = Color(0xFF333333),
            shape = RoundedCornerShape(0.dp),
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            modifier = Modifier.fillMaxSize(),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pratinjau Dokumen Surat A4",
                        style = AppTypography.HeadingMedium.copy(color = Color.White)
                    )
                    Text(
                        text = "Cubit layar untuk zoom",
                        style = AppTypography.Caption.copy(color = Color.LightGray)
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF525659))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = false
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.setSupportZoom(true)
                                    webViewClient = WebViewClient()
                                    loadDataWithBaseURL(null, previewHtml, "text/html", "UTF-8", null)
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPdfHtmlPreviewDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
                ) {
                    Text(text = "Tutup Pratinjau Dokumen", style = AppTypography.ButtonText)
                }
            }
        )
    }

    // DIALOG KONFIRMASI SIMPAN
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = if (isDiterima) GreenDesaLight else RedRejectedContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isDiterima) "✓" else "✕",
                                style = AppTypography.HeadingLarge.copy(
                                    color = if (isDiterima) GreenDesaPrimary else RedRejectedText
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isDiterima) "Yakin Terbitkan Surat?" else "Yakin Tolak Pengajuan Ini?",
                        style = AppTypography.HeadingMedium,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = if (isDiterima)
                        "Status akan disimpan sebagai TERBIT dan surat resmi akan otomatis dikirim ke email admin (jundilamtara0103@gmail.com)."
                    else
                        "Alasan penolakan akan dikirimkan ke warga.",
                    style = AppTypography.BodyRegular,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        val updatedItem = buildCurrentUpdatedItem(keepExistingId = false)

                        if (isDiterima) {
                            sudahTerbitItem = updatedItem
                            isSendingEmail = true
                            coroutineScope.launch {
                                EmailSenderHelper.sendEmailAutomatic(
                                    context = context,
                                    pengajuan = updatedItem,
                                    onFinished = {
                                        isSendingEmail = false
                                        onSaveSuccess(updatedItem)
                                    }
                                )
                            }
                        } else {
                            onSaveSuccess(updatedItem)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDiterima) GreenDesaPrimary else Color(0xFFDC2626)
                    )
                ) {
                    Text(
                        text = if (isDiterima) "Ya, Terbitkan Sekarang" else "Ya, Kirim Penolakan",
                        style = AppTypography.ButtonText
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorder)
                ) {
                    Text(
                        text = "Batal",
                        style = AppTypography.ButtonText.copy(color = TextPrimary)
                    )
                }
            }
        )
    }

    // DIALOG PENGUNCI LAYAR SAAT MENGIRIM EMAIL OTOMATIS
    if (isSendingEmail) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = GreenDesaPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Mengirim Surat Otomatis...",
                        style = AppTypography.HeadingMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mohon tunggu, dokumen sedang dikirim ke email admin.",
                        style = AppTypography.Caption,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun AdminEditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    isError: Boolean = false
) {
    val adminTextStyle = AppTypography.BodyRegular.copy(
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )

    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                Text(
                    text = label,
                    style = AppTypography.Caption,
                    color = if (isError) Color(0xFFDC2626) else TextSecondary
                )
                if (isError) {
                    Text(
                        text = " *",
                        style = AppTypography.Caption.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFDC2626)
                    )
                }
            }
            if (isError) {
                Text(
                    text = "Wajib diisi",
                    style = AppTypography.Caption.copy(color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = adminTextStyle.copy(color = TextPlaceholder)
                )
            },
            textStyle = adminTextStyle,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                unfocusedBorderColor = if (isError) Color(0xFFDC2626) else GrayBorder,
                focusedBorderColor = if (isError) Color(0xFFDC2626) else GreenDesaPrimary,
                cursorColor = GreenDesaPrimary,
                errorBorderColor = Color(0xFFDC2626)
            )
        )
    }
}

fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy, HH.mm 'WIB'", Locale.forLanguageTag("id-ID"))
    return sdf.format(Date())
}