package com.sudes.purwodadi

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object EmailSenderHelper {

    private const val SERVICE_ID = "service_cgko1cg"
    private const val TEMPLATE_ID = "template_ak9kmrq"
    private const val PUBLIC_KEY = "44uDDCpdFJPgyY74y"

    suspend fun sendEmailAutomatic(
        context: Context,
        pengajuan: PengajuanItem,
        onFinished: (Boolean) -> Unit = {}
    ) {
        withContext(Dispatchers.IO) {
            try {
                val suratHtmlLengkap = SuratHtmlGenerator.generateSuratLengkapEmail(context, pengajuan)

                val templateParams = JSONObject().apply {
                    put("to_email", "jundilamtara0103@gmail.com")
                    put("subject", "Dokumen Resmi Desa: ${pengajuan.jenisSurat} - ${pengajuan.namaPemohon}")
                    put("surat_html", suratHtmlLengkap)
                }

                val payload = JSONObject().apply {
                    put("service_id", SERVICE_ID)
                    put("template_id", TEMPLATE_ID)
                    put("user_id", PUBLIC_KEY)
                    put("template_params", templateParams)
                }

                val url = URL("https://api.emailjs.com/api/v1.0/email/send")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Origin", "http://localhost")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                val isSuccess = responseCode in 200..299

                withContext(Dispatchers.Main) {
                    if (isSuccess) {
                        Toast.makeText(
                            context,
                            "Surat resmi berhasil dikirim otomatis ke email admin!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                        Toast.makeText(
                            context,
                            "Gagal mengirim email ($responseCode): $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    onFinished(isSuccess)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Terjadi kesalahan jaringan saat mengirim email: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onFinished(false)
                }
            }
        }
    }
}