package com.sudes.purwodadi

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

object EmailSenderHelper {

    private data class EmailConfig(
        val senderEmail: String,
        val appPassword: String,
        val targetEmail: String
    )

    private suspend fun fetchEmailConfigFromFirestore(): EmailConfig? {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("pengaturan")
                .document("email_desa")
                .get()
                .await()

            val sender = snapshot.getString("sender_email")?.trim().orEmpty()
            val password = snapshot.getString("sender_app_password")?.replace(" ", "")?.trim().orEmpty()
            val target = snapshot.getString("target_email")?.trim().orEmpty()

            if (sender.isBlank() || password.isBlank() || target.isBlank()) {
                null
            } else {
                EmailConfig(sender, password, target)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun sendEmailAutomatic(
        context: Context,
        pengajuan: PengajuanItem,
        onFinished: (Boolean) -> Unit = {}
    ) {
        // 1. Ambil konfigurasi email & App Password dari Firestore
        val config = fetchEmailConfigFromFirestore()
        if (config == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Konfigurasi email desa di Firestore belum lengkap.",
                    Toast.LENGTH_LONG
                ).show()
                onFinished(false)
            }
            return
        }

        // Ubah dari PdfSuratGenerator menjadi PdfGeneratorHelper:
        val pdfFile = PdfGeneratorHelper.generatePdfFile(context, pengajuan)
        if (pdfFile == null || !pdfFile.exists()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gagal membuat file PDF surat.", Toast.LENGTH_LONG).show()
                onFinished(false)
            }
            return
        }

        // 3. Kirim file PDF sebagai lampiran melalui SMTP Gmail di latar belakang
        withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.socketFactory.port", "465")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.port", "465")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(config.senderEmail, config.appPassword)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(config.senderEmail, "Admin SuDes"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.targetEmail))
                    subject = "Dokumen Resmi Desa (PDF): ${pengajuan.jenisSurat} - ${pengajuan.namaPemohon}"

                    val textPart = MimeBodyPart().apply {
                        setText(
                                    "Terlampir berkas resmi ${pengajuan.jenisSurat} atas nama ${pengajuan.namaPemohon} " +
                                    "(NIK: ${pengajuan.nik}) dalam format PDF ukuran A4 siap cetak.\n\n"
                        )
                    }

                    val pdfAttachmentPart = MimeBodyPart().apply {
                        dataHandler = DataHandler(FileDataSource(pdfFile))
                        fileName = pdfFile.name
                    }

                    val multipart = MimeMultipart().apply {
                        addBodyPart(textPart)
                        addBodyPart(pdfAttachmentPart)
                    }

                    setContent(multipart)
                }

                Transport.send(message)

                // Hapus file sementara di cache setelah berhasil terkirim
                pdfFile.delete()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "File PDF surat berhasil dikirim ke ${config.targetEmail}!",
                        Toast.LENGTH_LONG
                    ).show()
                    onFinished(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Gagal mengirim email PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onFinished(false)
                }
            }
        }
    }
}