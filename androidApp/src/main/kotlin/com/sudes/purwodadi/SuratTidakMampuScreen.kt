package com.sudes.purwodadi

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sudes.purwodadi.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Fungsi helper format tanggal Indonesia khusus SKTM
fun getSktmDateFormatted(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
    return sdf.format(Date())
}

// Fungsi helper: Menyalin file ke direktori internal permanen
fun saveFilePermanentlySktm(context: Context, sourceUri: Uri): Uri? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val permanentDir = File(context.filesDir, "surat_pengantar_sktm").apply {
            if (!exists()) mkdirs()
        }
        val permanentFile = File(permanentDir, "SKTM_${timeStamp}.jpg")

        val outputStream = FileOutputStream(permanentFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        Uri.fromFile(permanentFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Fungsi helper pembuat file sementara kamera
fun createSktmImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

// Fungsi helper untuk menyimpan draft sementara ke SharedPreferences
fun saveDraftSuratSktm(
    context: Context,
    nama: String,
    nik: String,
    ttl: String,
    jk: String,
    agama: String,
    statusPerkawinan: String,
    pekerjaan: String,
    alamat: String,
    nomorKK: String,
    namaKK: String,
    keperluan: String,
    uriString: String
) {
    val prefs = context.getSharedPreferences("DraftSuratSktmPrefs", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putString("nama", nama)
        putString("nik", nik)
        putString("ttl", ttl)
        putString("jk", jk)
        putString("agama", agama)
        putString("statusPerkawinan", statusPerkawinan)
        putString("pekerjaan", pekerjaan)
        putString("alamat", alamat)
        putString("nomorKK", nomorKK)
        putString("namaKK", namaKK)
        putString("keperluan", keperluan)
        putString("pengantarUri", uriString)
        apply()
    }
}

// Fungsi helper untuk menghapus draft jika pengajuan sukses
fun clearDraftSuratSktm(context: Context) {
    val prefs = context.getSharedPreferences("DraftSuratSktmPrefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuratTidakMampuScreen(
    initialNama: String = "",
    initialNik: String = "",
    initialTempatTgl: String = "",
    initialAgama: String = "",
    initialStatusPerkawinan: String = "",
    initialPekerjaan: String = "",
    initialAlamat: String = "",
    existingItem: PengajuanItem? = null,
    onBackClicked: () -> Unit = {},
    onSubmitPengajuan: (PengajuanItem) -> Unit = {}
) {
    val context = LocalContext.current
    val draftPrefs = remember { context.getSharedPreferences("DraftSuratSktmPrefs", Context.MODE_PRIVATE) }

    // Membaca data draft lokal (jika ada)
    val savedNama = draftPrefs.getString("nama", "") ?: ""
    val savedNik = draftPrefs.getString("nik", "") ?: ""
    val savedTtl = draftPrefs.getString("ttl", "") ?: ""
    val savedJk = draftPrefs.getString("jk", "") ?: ""
    val savedAgama = draftPrefs.getString("agama", "") ?: ""
    val savedStatus = draftPrefs.getString("statusPerkawinan", "") ?: ""
    val savedPekerjaan = draftPrefs.getString("pekerjaan", "") ?: ""
    val savedAlamat = draftPrefs.getString("alamat", "") ?: ""
    val savedNomorKK = draftPrefs.getString("nomorKK", "") ?: ""
    val savedNamaKK = draftPrefs.getString("namaKK", "") ?: ""
    val savedKeperluan = draftPrefs.getString("keperluan", "") ?: ""
    val savedUriStr = draftPrefs.getString("pengantarUri", "") ?: ""

    // State Data Diri (Prioritas: existingItem -> draft lokal -> kosong)
    var namaLengkap by remember { mutableStateOf(existingItem?.namaPemohon ?: if (savedNama.isNotBlank()) savedNama else "") }
    var nik by remember { mutableStateOf(existingItem?.nik ?: if (savedNik.isNotBlank()) savedNik else "") }
    var tempatTanggalLahir by remember { mutableStateOf(existingItem?.tempatTanggalLahir ?: if (savedTtl.isNotBlank()) savedTtl else "") }
    var jenisKelamin by remember { mutableStateOf(existingItem?.jenisKelamin ?: if (savedJk.isNotBlank()) savedJk else "") }
    var agama by remember { mutableStateOf(existingItem?.agama ?: if (savedAgama.isNotBlank()) savedAgama else "") }
    var statusPerkawinan by remember { mutableStateOf(existingItem?.statusPerkawinan ?: if (savedStatus.isNotBlank()) savedStatus else "") }
    var pekerjaan by remember { mutableStateOf(existingItem?.pekerjaan ?: if (savedPekerjaan.isNotBlank()) savedPekerjaan else "") }
    var alamatLengkap by remember { mutableStateOf(existingItem?.alamatLengkap ?: if (savedAlamat.isNotBlank()) savedAlamat else "") }

    var isAutoFilled by remember { mutableStateOf(false) }

    // State Detail Keluarga & Keperluan
    // Catatan: lokasiUsaha pada model item digunakan untuk menyimpan gabungan "No. KK: [nomorKK] | Keperluan: [keperluan]"
    val existingLokasi = existingItem?.lokasiUsaha ?: ""
    val splitLokasi = existingLokasi.split(" | Keperluan: ")
    val defaultNoKK = if (splitLokasi.size > 0 && splitLokasi[0].startsWith("No. KK: ")) splitLokasi[0].removePrefix("No. KK: ") else savedNomorKK
    val defaultKeperluan = if (splitLokasi.size > 1) splitLokasi[1] else (existingItem?.lokasiUsaha?.takeIf { !it.startsWith("No. KK: ") } ?: savedKeperluan)

    var nomorKK by remember { mutableStateOf(defaultNoKK) }
    var namaKepalaKeluarga by remember { mutableStateOf(existingItem?.namaUsaha ?: if (savedNamaKK.isNotBlank()) savedNamaKK else "") }
    var keperluanSurat by remember { mutableStateOf(defaultKeperluan) }

    // State Lampiran File Pengantar RT
    val initialUri = existingItem?.pengantarUri?.takeIf { it.isNotBlank() } ?: if (savedUriStr.isNotBlank()) savedUriStr else null
    var pengantarUri by remember { mutableStateOf<Uri?>(initialUri?.let { Uri.parse(it) }) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Otomatis simpan draft ke HP
    LaunchedEffect(namaLengkap, nik, tempatTanggalLahir, jenisKelamin, agama, statusPerkawinan, pekerjaan, alamatLengkap, nomorKK, namaKepalaKeluarga, keperluanSurat, pengantarUri) {
        val uriToSave = pengantarUri?.toString() ?: existingItem?.pengantarUri ?: ""
        saveDraftSuratSktm(
            context = context,
            nama = namaLengkap,
            nik = nik,
            ttl = tempatTanggalLahir,
            jk = jenisKelamin,
            agama = agama,
            statusPerkawinan = statusPerkawinan,
            pekerjaan = pekerjaan,
            alamat = alamatLengkap,
            nomorKK = nomorKK,
            namaKK = namaKepalaKeluarga,
            keperluan = keperluanSurat,
            uriString = uriToSave
        )
    }

    // State Dialog & Launcher Gambar
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showImagePreviewDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val permanentUri = saveFilePermanentlySktm(context, uri)
            pengantarUri = permanentUri ?: uri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            val permanentUri = saveFilePermanentlySktm(context, tempCameraUri!!)
            pengantarUri = permanentUri ?: tempCameraUri
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val photoFile = createSktmImageFile(context)
            val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraUri = photoUri
            cameraLauncher.launch(photoUri)
        }
    }

    var showErrorValidation by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isSubmittedSuccess by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    BackHandler {
        when {
            isSubmittedSuccess -> {
                isSubmittedSuccess = false
                onSubmitPengajuan(
                    createSktmPengajuanItem(
                        nama = namaLengkap,
                        nik = nik,
                        ttl = tempatTanggalLahir,
                        jk = jenisKelamin,
                        agama = agama,
                        statusPerkawinan = statusPerkawinan,
                        pekerjaan = pekerjaan,
                        alamat = alamatLengkap,
                        nomorKK = nomorKK,
                        namaKK = namaKepalaKeluarga,
                        keperluan = keperluanSurat,
                        uri = pengantarUri
                    )
                )
            }
            showConfirmDialog -> {
                showConfirmDialog = false
            }
            else -> {
                onBackClicked()
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                    IconButton(onClick = onBackClicked) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Surat Keterangan Tidak Mampu", style = AppTypography.HeadingMedium.copy(color = Color.White))
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
            if (existingItem != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Catatan Penolakan dari Admin:", style = AppTypography.Caption.copy(fontWeight = FontWeight.Bold), color = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = existingItem.pesanStatus, style = AppTypography.BodyRegular.copy(fontWeight = FontWeight.Medium), color = Color(0xFF991B1B))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Silakan perbaiki data di bawah ini, lalu kirimkan kembali pengajuan Anda.", style = AppTypography.Caption, color = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // TOMBOL ISI OTOMATIS DARI PROFIL
            Surface(
                onClick = {
                    isAutoFilled = !isAutoFilled
                    if (isAutoFilled) {
                        namaLengkap = initialNama.takeIf { it != "-" && it.isNotBlank() } ?: ""
                        nik = initialNik.takeIf { it != "-" && it.isNotBlank() } ?: ""
                        tempatTanggalLahir = initialTempatTgl.takeIf { it != "-" && it.isNotBlank() } ?: ""
                        jenisKelamin = ""
                        agama = initialAgama.takeIf { it != "-" && it.isNotBlank() } ?: ""
                        statusPerkawinan = initialStatusPerkawinan.takeIf { it != "-" && it.isNotBlank() } ?: ""
                        pekerjaan = initialPekerjaan.takeIf { it != "-" && it.isNotBlank() } ?: ""
                        alamatLengkap = initialAlamat.takeIf { it != "-" && it.isNotBlank() } ?: ""
                    } else {
                        namaLengkap = existingItem?.namaPemohon ?: ""
                        nik = existingItem?.nik ?: ""
                        tempatTanggalLahir = existingItem?.tempatTanggalLahir ?: ""
                        jenisKelamin = existingItem?.jenisKelamin ?: ""
                        agama = existingItem?.agama ?: ""
                        statusPerkawinan = existingItem?.statusPerkawinan ?: ""
                        pekerjaan = existingItem?.pekerjaan ?: ""
                        alamatLengkap = existingItem?.alamatLengkap ?: ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isAutoFilled) GreenDesaPrimary else GreenDesaLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, GreenDesaPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAutoFilled) "Data Terisi Otomatis dari Profil" else "Isi Otomatis dari Profil",
                        style = AppTypography.LabelForm.copy(color = if (isAutoFilled) Color.White else GreenDesaPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KARTU 1: DATA DIRI
            SktmCardSection(title = "DATA DIRI") {
                SktmFormTextField(label = "Nama Lengkap", value = namaLengkap, onValueChange = { namaLengkap = it }, placeholder = "Masukkan nama lengkap", isError = showErrorValidation && namaLengkap.isBlank())
                SktmFormTextField(label = "NIK", value = nik, onValueChange = { nik = it }, placeholder = "Masukkan 16 digit NIK", isError = showErrorValidation && nik.isBlank())
                SktmFormTextField(label = "Tempat, Tanggal Lahir", value = tempatTanggalLahir, onValueChange = { tempatTanggalLahir = it }, placeholder = "Contoh: Bandar Lampung, 01 Januari 1990", isError = showErrorValidation && tempatTanggalLahir.isBlank())

                SktmDropdownField(
                    label = "Jenis Kelamin",
                    selectedValue = jenisKelamin,
                    options = listOf("Laki-laki", "Perempuan"),
                    onOptionSelected = { jenisKelamin = it },
                    isError = showErrorValidation && jenisKelamin.isBlank()
                )

                SktmDropdownField(
                    label = "Agama",
                    selectedValue = agama,
                    options = listOf("Islam", "Kristen Protestan", "Kristen Katolik", "Hindu", "Buddha", "Konghucu"),
                    onOptionSelected = { agama = it },
                    isError = showErrorValidation && agama.isBlank()
                )

                SktmDropdownField(
                    label = "Status Perkawinan",
                    selectedValue = statusPerkawinan,
                    options = listOf("Belum Kawin", "Kawin", "Cerai Hidup", "Cerai Mati"),
                    onOptionSelected = { statusPerkawinan = it },
                    isError = showErrorValidation && statusPerkawinan.isBlank()
                )

                SktmFormTextField(label = "Pekerjaan", value = pekerjaan, onValueChange = { pekerjaan = it }, placeholder = "Masukkan pekerjaan", isError = showErrorValidation && pekerjaan.isBlank())

                SktmFormTextField(
                    label = "Alamat Lengkap",
                    value = alamatLengkap,
                    onValueChange = { alamatLengkap = it },
                    placeholder = "Masukkan alamat sesuai KTP",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 4,
                    isError = showErrorValidation && alamatLengkap.isBlank()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KARTU 2: DETAIL KELUARGA & KEPERLUAN
            SktmCardSection(title = "DETAIL KELUARGA & KEPERLUAN") {
                SktmFormTextField(label = "Nomor Kartu Keluarga (KK)", value = nomorKK, onValueChange = { nomorKK = it }, placeholder = "Masukkan 16 digit nomor KK", isError = showErrorValidation && nomorKK.isBlank())
                SktmFormTextField(label = "Nama Kepala Keluarga (di KK)", value = namaKepalaKeluarga, onValueChange = { namaKepalaKeluarga = it }, placeholder = "Contoh: Budi Santoso", isError = showErrorValidation && namaKepalaKeluarga.isBlank())

                SktmFormTextField(
                    label = "Keperluan Pembuatan Surat",
                    value = keperluanSurat,
                    onValueChange = { keperluanSurat = it },
                    placeholder = "Contoh: Persyaratan Berobat / Sekolah / Beasiswa",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                    isError = showErrorValidation && keperluanSurat.isBlank()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KARTU 3: SURAT PENGANTAR RT
            SktmCardSection(title = "SURAT PENGANTAR RT") {
                OutlinedButton(
                    onClick = { showImageSourceDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (showErrorValidation && pengantarUri == null) Color(0xFFFEF2F2) else GrayBackground
                    ),
                    border = if (showErrorValidation && pengantarUri == null) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)) else null
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Upload",
                        tint = if (pengantarUri != null) GreenDesaPrimary else if (showErrorValidation) Color(0xFFDC2626) else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pengantarUri != null) "Ubah Berkas / Foto Pengantar" else "Unggah atau Foto Surat Pengantar RT",
                        color = if (pengantarUri != null) GreenDesaPrimary else if (showErrorValidation) Color(0xFFDC2626) else TextSecondary,
                        style = AppTypography.BodyRegular
                    )
                }

                if (pengantarUri != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = GreenDesaLight.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenDesaPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = GreenDesaPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Pengantar RT Terlampir", style = AppTypography.BodyRegular.copy(fontWeight = FontWeight.Medium), color = GreenDesaPrimary)
                            }
                            Row {
                                TextButton(onClick = { showImagePreviewDialog = true }) {
                                    Text(text = "Lihat", color = GreenDesaPrimary, style = AppTypography.LabelForm)
                                }
                                TextButton(onClick = { pengantarUri = null }) {
                                    Text(text = "Hapus", color = Color(0xFFDC2626), style = AppTypography.Caption)
                                }
                            }
                        }
                    }
                }

                if (showErrorValidation && pengantarUri == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Lampiran surat pengantar RT wajib dilampirkan", style = AppTypography.Caption.copy(color = Color(0xFFDC2626)))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KARTU 4: INFORMASI SURAT
            SktmCardSection(title = "INFORMASI SURAT") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Nomor Surat (Diisi Admin)", style = AppTypography.Caption, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "-", style = AppTypography.BodyRegular, color = TextPlaceholder)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Tanggal Pengajuan", style = AppTypography.Caption, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = getSktmDateFormatted(), style = AppTypography.LabelForm, color = GreenDesaPrimary, textAlign = TextAlign.End)
                    }
                }
            }

            // PESAN PERINGATAN GLOBAL JIKA ADA YANG KOSONG
            if (showErrorValidation && (namaLengkap.isBlank() || nik.isBlank() || tempatTanggalLahir.isBlank() || jenisKelamin.isBlank() || agama.isBlank() || statusPerkawinan.isBlank() || pekerjaan.isBlank() || alamatLengkap.isBlank() || nomorKK.isBlank() || namaKepalaKeluarga.isBlank() || keperluanSurat.isBlank() || pengantarUri == null)) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF2F2)
                ) {
                    Text(
                        text = "⚠️ Mohon lengkapi seluruh data formulir dan surat pengantar RT yang bertanda merah.",
                        style = AppTypography.BodyRegular.copy(color = Color(0xFFDC2626), fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TOMBOL KIRIM PENGAJUAN
            Button(
                onClick = {
                    val isFormValid = namaLengkap.isNotBlank() &&
                            nik.isNotBlank() &&
                            tempatTanggalLahir.isNotBlank() &&
                            jenisKelamin.isNotBlank() &&
                            agama.isNotBlank() &&
                            statusPerkawinan.isNotBlank() &&
                            pekerjaan.isNotBlank() &&
                            alamatLengkap.isNotBlank() &&
                            nomorKK.isNotBlank() &&
                            namaKepalaKeluarga.isNotBlank() &&
                            keperluanSurat.isNotBlank() &&
                            pengantarUri != null

                    if (isFormValid) {
                        showErrorValidation = false
                        showConfirmDialog = true
                    } else {
                        showErrorValidation = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
            ) {
                Text(text = "Kirim Pengajuan Surat", style = AppTypography.ButtonText)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DIALOG PILIHAN SUMBER GAMBAR
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(text = "Pilih Sumber Berkas", style = AppTypography.HeadingMedium, color = TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            showImageSourceDialog = false
                            val permissionCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                val photoFile = createSktmImageFile(context)
                                val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                                tempCameraUri = photoUri
                                cameraLauncher.launch(photoUri)
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GreenDesaPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Ambil Foto Langsung", color = TextPrimary, style = AppTypography.ButtonText)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Pilih dari Galeri", style = AppTypography.ButtonText)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // DIALOG PREVIEW FOTO FULLSCREEN
    if (showImagePreviewDialog && pengantarUri != null) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

        AlertDialog(
            onDismissRequest = { showImagePreviewDialog = false },
            containerColor = Color.Black,
            shape = RoundedCornerShape(0.dp),
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
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
                    coil.compose.AsyncImage(
                        model = pengantarUri,
                        contentDescription = "Pratinjau Surat Pengantar RT",
                        modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Button(onClick = { showImagePreviewDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)) {
                        Text(text = "Tutup Pratinjau", style = AppTypography.ButtonText)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // DIALOG KONFIRMASI
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color(0xFFFEF3C7)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "!", style = AppTypography.HeadingLarge, color = Color(0xFFD97706))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Kirim Pengajuan Surat?", style = AppTypography.HeadingMedium, color = TextPrimary)
                }
            },
            text = {
                Text(text = "Pastikan data dan dokumen pengantar RT yang Anda masukkan sudah benar.", style = AppTypography.BodyRegular, color = TextSecondary, textAlign = TextAlign.Center)
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            isSubmittedSuccess = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
                    ) {
                        Text(text = "Ya, Kirim Sekarang", style = AppTypography.ButtonText)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showConfirmDialog = false },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Periksa Kembali", color = TextPrimary, style = AppTypography.ButtonText)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // DIALOG SUKSES
    if (isSubmittedSuccess) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = GreenDesaLight) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Success", tint = GreenDesaPrimary, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Pengajuan Berhasil!", style = AppTypography.HeadingMedium, color = TextPrimary)
                }
            },
            text = {
                Text(text = "Dokumen Anda telah masuk ke sistem desa dan menunggu verifikasi admin.", style = AppTypography.BodyRegular, color = TextSecondary, textAlign = TextAlign.Center)
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittedSuccess = false
                        clearDraftSuratSktm(context)
                        onSubmitPengajuan(
                            createSktmPengajuanItem(
                                nama = namaLengkap,
                                nik = nik,
                                ttl = tempatTanggalLahir,
                                jk = jenisKelamin,
                                agama = agama,
                                statusPerkawinan = statusPerkawinan,
                                pekerjaan = pekerjaan,
                                alamat = alamatLengkap,
                                nomorKK = nomorKK,
                                namaKK = namaKepalaKeluarga,
                                keperluan = keperluanSurat,
                                uri = pengantarUri
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
                ) {
                    Text(text = "Lihat Status Pengajuan", style = AppTypography.ButtonText)
                }
            }
        )
    }
}

// Fungsi bantu pembuat objek PengajuanItem khusus SKTM
private fun createSktmPengajuanItem(
    nama: String,
    nik: String,
    ttl: String,
    jk: String,
    agama: String,
    statusPerkawinan: String,
    pekerjaan: String,
    alamat: String,
    nomorKK: String,
    namaKK: String,
    keperluan: String,
    uri: Uri?
): PengajuanItem {
    return PengajuanItem(
        id = System.currentTimeMillis().toString(),
        jenisSurat = "Surat Keterangan Tidak Mampu",
        namaPemohon = nama.ifBlank { "Warga Pemohon" },
        tanggal = getSktmDateFormatted(),
        status = StatusPengajuan.PROSES,
        pesanStatus = "Berkas sedang dicek oleh perangkat desa",
        nik = nik,
        tempatTanggalLahir = ttl,
        jenisKelamin = jk.ifBlank { "Laki-laki" },
        agama = agama,
        statusPerkawinan = statusPerkawinan,
        pekerjaan = pekerjaan,
        alamatLengkap = alamat,
        namaUsaha = namaKK,       // Menampung Nama Kepala Keluarga di KK
        lokasiUsaha = "No. KK: $nomorKK | Keperluan: $keperluan", // Menampung gabungan Nomor KK dan Keperluan
        pengantarUri = uri?.toString() ?: "",
        nomorSurat = "-"
    )
}

// Komponen Pendukung Card Section
@Composable
fun SktmCardSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = AppTypography.HeadingMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// Komponen Input TextField Standar
@Composable
fun SktmFormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    isError: Boolean = false
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = AppTypography.LabelForm, color = if (isError) Color(0xFFDC2626) else TextPrimary)
            if (isError) {
                Text(text = "Wajib diisi", style = AppTypography.Caption.copy(color = Color(0xFFDC2626)))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextPlaceholder, style = AppTypography.BodyRegular) },
            textStyle = AppTypography.BodyRegular.copy(color = TextPrimary),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                unfocusedBorderColor = if (isError) Color(0xFFDC2626) else GrayBorder,
                focusedBorderColor = if (isError) Color(0xFFDC2626) else GreenDesaPrimary,
                focusedLabelColor = GreenDesaPrimary,
                cursorColor = GreenDesaPrimary,
                errorBorderColor = Color(0xFFDC2626)
            )
        )
    }
}

// Komponen Dropdown Interaktif Khusus SKTM
@Composable
fun SktmDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = AppTypography.LabelForm, color = if (isError) Color(0xFFDC2626) else TextPrimary)
            if (isError) {
                Text(text = "Wajib dipilih", style = AppTypography.Caption.copy(color = Color(0xFFDC2626)))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isError) Color(0xFFDC2626) else GrayBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isError) Color(0xFFFEF2F2) else Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedValue.isBlank()) "Pilih $label" else selectedValue,
                        color = if (selectedValue.isBlank()) TextPlaceholder else TextPrimary,
                        style = AppTypography.BodyRegular
                    )
                    Text(
                        text = if (expanded) "▲" else "▼",
                        color = if (isError) Color(0xFFDC2626) else GreenDesaPrimary,
                        style = AppTypography.BodyRegular
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).background(Color.White),
                offset = androidx.compose.ui.unit.DpOffset(0.dp, 4.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        text = {
                            Text(
                                text = option,
                                style = AppTypography.BodyRegular.copy(fontWeight = if (option == selectedValue) FontWeight.Bold else FontWeight.Normal),
                                color = if (option == selectedValue) GreenDesaPrimary else TextPrimary
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}