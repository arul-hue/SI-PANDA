package com.sudes.purwodadi

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.sudes.purwodadi.ui.theme.*

@Composable
fun SuratPreviewScreen(
    pengajuan: PengajuanItem,
    onBackClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val htmlContent = remember(pengajuan) { SuratHtmlGenerator.generateHtml(pengajuan) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    BackHandler(enabled = !isGeneratingPdf) {
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
                        enabled = !isGeneratingPdf
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pratinjau Surat Resmi",
                        style = AppTypography.HeadingMedium.copy(color = Color.White)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            exportPdfDanKirimGmail(
                                context = context,
                                item = pengajuan,
                                onLoadingChange = { isGeneratingPdf = it }
                            )
                        },
                        enabled = !isGeneratingPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGeneratingPdf) "Menyiapkan PDF..." else "Export PDF & Kirim ke Email",
                            style = AppTypography.ButtonText
                        )
                    }
                }
            }
        },
        containerColor = GrayBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            webViewClient = WebViewClient()
                            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }
                    },
                    update = { wv ->
                        wv.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                )
            }
        }
    }

    if (isGeneratingPdf) {
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
                        text = "Menyiapkan Dokumen PDF...",
                        style = AppTypography.HeadingMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mohon tunggu, sedang mengalihkan otomatis ke Gmail.",
                        style = AppTypography.Caption,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {}
        )
    }
}

private fun exportPdfDanKirimGmail(
    context: Context,
    item: PengajuanItem,
    onLoadingChange: (Boolean) -> Unit
) {
    onLoadingChange(true)

    PdfGeneratorHelper.generatePdfFromHtml(context, item) { pdfFile ->
        onLoadingChange(false)

        if (pdfFile == null || !pdfFile.exists() || pdfFile.length() <= 0L) {
            Toast.makeText(
                context,
                "PDF gagal dibuat atau file kosong.",
                Toast.LENGTH_LONG
            ).show()
            return@generatePdfFromHtml
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            context.grantUriPermission(
                "com.google.android.gm",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("jundilamtara0103@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Dokumen Resmi Desa: ${item.jenisSurat} - ${item.namaPemohon}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    """
                    Halo Admin,

                    Berikut terlampir dokumen resmi ${item.jenisSurat}
                    atas nama ${item.namaPemohon} (NIK: ${item.nik})
                    dengan Nomor Surat: ${item.nomorSurat}
                    yang telah diterbitkan melalui aplikasi SUDES.

                    Terima kasih.
                    """.trimIndent()
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, pdfFile.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.google.android.gm")
            }

            try {
                context.startActivity(emailIntent)
            } catch (e: ActivityNotFoundException) {
                emailIntent.setPackage(null)
                context.startActivity(Intent.createChooser(emailIntent, "Kirim Dokumen PDF"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Gagal membuka lampiran PDF: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}