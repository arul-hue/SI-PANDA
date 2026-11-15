package com.sudes.purwodadi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PengajuanMonitorService : Service() {

    private var listenerRegistration: ListenerRegistration? = null
    private val statusSebelumnya = mutableMapOf<String, StatusPengajuan>()
    private var isFirstSnapshot = true

    companion object {
        private const val SERVICE_CHANNEL_ID = "sudes_background_monitor"
        private const val SERVICE_NOTIF_ID = 9001

        fun start(context: Context) {
            val intent = Intent(context, PengajuanMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PengajuanMonitorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        mulaiPantauFirestore()
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Pemantau Status Surat Desa",
                NotificationManager.IMPORTANCE_MIN // Tidak berbunyi & tidak mengganggu layar
            ).apply {
                description = "Menjaga pembaruan status pengajuan surat tetap aktif"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_desa)
            .setContentTitle("SUDES Purwodadi Simpang")
            .setContentText("Memantau status pengajuan surat Anda")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                SERVICE_NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(SERVICE_NOTIF_ID, notification)
        }
    }

    private fun mulaiPantauFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        listenerRegistration = firestore.collection("pengajuan")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val prefs = getSharedPreferences("SudesPrefs", Context.MODE_PRIVATE)
                val roleUser = prefs.getString("USER_ROLE", "warga") ?: "warga"
                val nikUser = prefs.getString("USER_NIK", "") ?: ""
                val namaUser = prefs.getString("USER_NAMA", "") ?: ""

                // Jangan jalankan notifikasi warga jika belum login atau login sebagai admin
                if (auth.currentUser == null || roleUser == "admin") return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val jenisSurat = doc.getString("jenisSurat") ?: "Surat Keterangan"
                    val namaPemohon = doc.getString("namaPemohon") ?: ""
                    val nik = doc.getString("nik") ?: ""
                    val pesanStatus = doc.getString("pesanStatus") ?: ""
                    val statusStr = doc.getString("status") ?: "PROSES"
                    val status = try {
                        StatusPengajuan.valueOf(statusStr)
                    } catch (err: Exception) {
                        StatusPengajuan.PROSES
                    }

                    val statusLama = statusSebelumnya[id]
                    statusSebelumnya[id] = status

                    val baruDiputuskan = !isFirstSnapshot &&
                            status != StatusPengajuan.PROSES &&
                            (statusLama == null || statusLama != status)

                    if (baruDiputuskan) {
                        val nikSama = nikUser.isNotBlank() && nik.trim() == nikUser.trim()
                        val namaSama = namaUser.isNotBlank() &&
                                namaUser != "Warga" &&
                                (namaPemohon.trim().equals(namaUser.trim(), ignoreCase = true) ||
                                        namaPemohon.trim().contains(namaUser.trim(), ignoreCase = true) ||
                                        namaUser.trim().contains(namaPemohon.trim(), ignoreCase = true))

                        if (nikSama || namaSama) {
                            val itemNotif = PengajuanItem(
                                id = id,
                                jenisSurat = jenisSurat,
                                namaPemohon = namaPemohon,
                                status = status,
                                pesanStatus = pesanStatus,
                                nik = nik
                            )
                            StatusNotificationHelper.showStatusNotification(this, itemNotif)
                        }
                    }
                }
                isFirstSnapshot = false
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Otomatis hidup kembali jika dimatikan sistem Android
    }

    override fun onDestroy() {
        listenerRegistration?.remove()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}