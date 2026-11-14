package com.sudes.purwodadi

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.sudes.purwodadi.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDetailPengajuanScreen(
    pengajuanItem: PengajuanItem,
    fields: List<DetailFieldItem> = emptyList(),
    onBackClicked: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showImagePreviewDialog by remember { mutableStateOf(false) }

    BackHandler {
        onBackClicked()
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClicked) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Detail Pengajuan Admin",
                            style = AppTypography.HeadingMedium.copy(color = Color.White)
                        )
                    }

                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        StatusBadge(status = pengajuanItem.status)
                    }
                }
            }
        },
        bottomBar = {
            // Bagian bawah: Hanya berisi tombol kembali ke dashboard admin
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenDesaPrimary)
                    ) {
                        Text(
                            text = "Kembali ke Dashboard Admin",
                            style = AppTypography.BodyRegular.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        containerColor = GrayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // KARTU 1: STATUS TRACKER (PROGRES SURAT DINAMIS)
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
                        text = pengajuanItem.jenisSurat,
                        style = AppTypography.HeadingMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    when (pengajuanItem.status) {
                        StatusPengajuan.PROSES -> {
                            TimelineStepItem(
                                title = "1. Diproses — Berkas dicek perangkat desa",
                                date = pengajuanItem.tanggal,
                                isCompleted = true,
                                isProcess = true,
                                showLine = true
                            )
                            TimelineStepItem(
                                title = "2. Selesai — Surat dapat diambil di Balai Desa",
                                date = "Menunggu verifikasi selesai",
                                isCompleted = false,
                                isProcess = false,
                                showLine = false
                            )
                        }
                        StatusPengajuan.DITERIMA -> {
                            TimelineStepItem(
                                title = "1. Diproses — Berkas telah diverifikasi",
                                date = pengajuanItem.tanggal,
                                isCompleted = true,
                                isProcess = false,
                                showLine = true
                            )
                            TimelineStepItem(
                                title = "2. Selesai — Surat dapat diambil di Balai Desa",
                                date = if (pengajuanItem.tanggalKeputusan.isNotBlank()) pengajuanItem.tanggalKeputusan else "Selesai",
                                isCompleted = true,
                                isProcess = false,
                                showLine = false
                            )
                        }
                        StatusPengajuan.DITOLAK -> {
                            TimelineStepItem(
                                title = "1. Diproses — Berkas diterima perangkat desa",
                                date = pengajuanItem.tanggal,
                                isCompleted = true,
                                isProcess = false,
                                showLine = true
                            )
                            TimelineStepItem(
                                title = "2. Ditolak — Pengajuan perlu diperbaiki",
                                date = if (pengajuanItem.tanggalKeputusan.isNotBlank()) pengajuanItem.tanggalKeputusan else "Ditolak",
                                isCompleted = true,
                                isProcess = false,
                                isRejected = true,
                                showLine = false
                            )

                            if (pengajuanItem.pesanStatus.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = RedRejectedContainer,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RedRejectedBorder.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Alasan Penolakan Admin:",
                                            style = AppTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                            color = RedRejectedText
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = pengajuanItem.pesanStatus,
                                            style = AppTypography.BodyRegular,
                                            color = RedRejectedDarkText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KARTU 2: DATA YANG DIINPUTKAN WARGA
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
                        text = "DATA PENGAJUAN WARGA",
                        style = AppTypography.HeadingMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (fields.isEmpty()) {
                        Text(
                            text = "Tidak ada data rinci yang tersedia.",
                            style = AppTypography.BodyRegular,
                            color = TextSecondary
                        )
                    } else {
                        fields.forEach { field ->
                            DetailItemRow(label = field.label, value = field.value)
                        }
                    }

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (pengajuanItem.pengantarUri.isNotBlank()) "pengantar_rt_warga.jpg" else "Tidak ada lampiran",
                                    style = AppTypography.BodyRegular.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                            }
                            if (pengajuanItem.pengantarUri.isNotBlank()) {
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DIALOG PRATINJAU FOTO FULL SCREEN DENGAN FITUR ZOOM & PAN
    if (showImagePreviewDialog && pengajuanItem.pengantarUri.isNotBlank()) {
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
                        model = Uri.parse(pengajuanItem.pengantarUri),
                        contentDescription = "Pratinjau Full Screen Surat Pengantar RT",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
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
}