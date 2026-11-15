package com.sudes.purwodadi

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object StatusNotificationHelper {

    private const val CHANNEL_ID = "sudes_status_surat"
    private const val CHANNEL_NAME = "Status Pengajuan Surat Desa"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi otomatis ketika pengajuan surat diterima atau ditolak"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showStatusNotification(
        context: Context,
        pengajuan: PengajuanItem
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        createNotificationChannel(context)

        val isDitolak = pengajuan.status == StatusPengajuan.DITOLAK
        val title = if (isDitolak) {
            "Pengajuan ${pengajuan.jenisSurat} Ditolak"
        } else {
            "Pengajuan ${pengajuan.jenisSurat} Diterima"
        }

        val message = if (isDitolak) {
            val alasan = pengajuan.pesanStatus.takeIf { it.isNotBlank() }
                ?: "Silakan periksa kembali data atau lampiran Anda dan ajukan ulang."
            "Mohon maaf, pengajuan Anda ditolak. $alasan"
        } else {
            "Selamat! Pengajuan ${pengajuan.jenisSurat} Anda telah disetujui oleh Admin Desa Purwodadi Simpang."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            pengajuan.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_desa)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(pengajuan.id.hashCode(), notification)
    }
}