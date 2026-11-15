package com.sudes.purwodadi

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object PdfGeneratorHelper {

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    suspend fun generatePdfFile(
        context: Context,
        pengajuan: PengajuanItem
    ): File? = suspendCancellableCoroutine { continuation ->
        Handler(Looper.getMainLooper()).post {
            generatePdfFromHtml(context, pengajuan) { file ->
                if (continuation.isActive) {
                    continuation.resume(file)
                }
            }
        }
    }

    fun generatePdfFromHtml(
        context: Context,
        pengajuan: PengajuanItem,
        onComplete: (File?) -> Unit
    ) {
        val htmlContent = SuratHtmlGenerator.generateHtml(
            context,
            pengajuan
        )
        val activity = context.findActivity()

        // Ukuran A4 proporsional 144 DPI (1190 x 1684 px)
        // 210mm pada 96 DPI = 793.7 px -> dikali skala 150% = 1190.5 px (pas 1 halaman A4)
        val pageWidth = 1190
        val pageHeight = 1684

        val webView = WebView(context).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = false
            settings.loadWithOverviewMode = false
            settings.useWideViewPort = false
            setInitialScale(150)
            alpha = 1f
            translationX = -pageWidth.toFloat()
        }

        val rootView = activity?.window?.decorView as? ViewGroup
        val params = FrameLayout.LayoutParams(pageWidth, pageHeight)
        rootView?.addView(webView, params)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        val widthSpec = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY)
                        val heightSpec = View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
                        webView.measure(widthSpec, heightSpec)
                        webView.layout(0, 0, pageWidth, pageHeight)

                        val pdfDocument = PdfDocument()
                        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                        val page = pdfDocument.startPage(pageInfo)

                        page.canvas.drawColor(Color.WHITE)
                        webView.draw(page.canvas)
                        pdfDocument.finishPage(page)

                        val pdfDir = File(context.filesDir, "surat_pdf").apply {
                            if (!exists()) mkdirs()
                        }

                        val safeName = pengajuan.namaPemohon.replace("[^a-zA-Z0-9]".toRegex(), "_")
                        val safeJenis = pengajuan.jenisSurat.replace("[^a-zA-Z0-9]".toRegex(), "_")
                        val pdfFile = File(pdfDir, "${safeJenis}_${safeName}_${System.currentTimeMillis()}.pdf")

                        FileOutputStream(pdfFile).use { outputStream ->
                            pdfDocument.writeTo(outputStream)
                            outputStream.flush()
                        }
                        pdfDocument.close()

                        rootView?.removeView(webView)
                        webView.destroy()

                        if (pdfFile.exists() && pdfFile.length() > 0L) {
                            onComplete(pdfFile)
                        } else {
                            onComplete(null)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        rootView?.removeView(webView)
                        webView.destroy()
                        onComplete(null)
                    }
                }, 600L)
            }
        }

        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
    }
}