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
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Menjaga pembaruan pengajuan surat tetap aktif"
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

        val roleUser = getSharedPreferences("SudesPrefs", Context.MODE_PRIVATE)
            .getString("USER_ROLE", "warga") ?: "warga"

        val teksService = if (roleUser == "admin") {
            "Memantau pengajuan surat warga masuk"
        } else {
            "Memantau status pengajuan surat Anda"
        }

        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_desa)
            .setContentTitle("SI PANDA Purwodadi Simpang")
            .setContentText(teksService)
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
                val uidUser = auth.currentUser?.uid ?: return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val docUserId = doc.getString("userId") ?: ""
                    val jenisSurat = doc.getString("jenisSurat") ?: "Surat Keterangan"
                    val namaPemohon = doc.getString("namaPemohon") ?: "Warga"
                    val nik = doc.getString("nik") ?: ""
                    val namaPelapor = doc.getString("namaPelapor") ?: ""
                    val nikPelapor = doc.getString("nikPelapor") ?: ""
                    val pesanStatus = doc.getString("pesanStatus") ?: ""
                    val statusStr = doc.getString("status") ?: "PROSES"
                    val status = try {
                        StatusPengajuan.valueOf(statusStr)
                    } catch (err: Exception) {
                        StatusPengajuan.PROSES
                    }

                    val statusLama = statusSebelumnya[id]
                    statusSebelumnya[id] = status

                    if (!isFirstSnapshot) {
                        if (roleUser == "admin") {
                            // NOTIFIKASI UNTUK ADMIN: Jika ada dokumen baru masuk dengan status PROSES
                            // atau dokumen lama yang diajukan ulang kembali menjadi PROSES
                            val pengajuanBaruMasuk = status == StatusPengajuan.PROSES &&
                                    (statusLama == null || statusLama != StatusPengajuan.PROSES)

                            if (pengajuanBaruMasuk) {
                                val itemAdmin = PengajuanItem(
                                    id = id,
                                    userId = docUserId,
                                    jenisSurat = jenisSurat,
                                    namaPemohon = namaPemohon,
                                    status = status,
                                    pesanStatus = pesanStatus,
                                    nik = nik
                                )
                                StatusNotificationHelper.showNewPengajuanAdminNotification(this, itemAdmin)
                            }
                        } else {
                            // NOTIFIKASI UNTUK WARGA: Prioritaskan UID akun yang login,
                            // fallback ke NIK/Nama Pemohon maupun Pelapor (Surat Kematian)
                            val baruDiputuskan = status != StatusPengajuan.PROSES &&
                                    (statusLama == null || statusLama != status)

                            if (baruDiputuskan) {
                                val milikUserIni = if (docUserId.isNotBlank()) {
                                    docUserId == uidUser
                                } else {
                                    val nikSama = nikUser.isNotBlank() &&
                                            (nik.trim() == nikUser.trim() || nikPelapor.trim() == nikUser.trim())

                                    val namaPemohonCocok = namaUser.isNotBlank() &&
                                            namaUser != "Warga" &&
                                            (namaPemohon.trim().equals(namaUser.trim(), ignoreCase = true) ||
                                                    namaPemohon.trim().contains(namaUser.trim(), ignoreCase = true) ||
                                                    namaUser.trim().contains(namaPemohon.trim(), ignoreCase = true))

                                    val namaPelaporCocok = namaUser.isNotBlank() &&
                                            namaPelapor.isNotBlank() &&
                                            namaUser != "Warga" &&
                                            (namaPelapor.trim().equals(namaUser.trim(), ignoreCase = true) ||
                                                    namaPelapor.trim().contains(namaUser.trim(), ignoreCase = true) ||
                                                    namaUser.trim().contains(namaPelapor.trim(), ignoreCase = true))

                                    nikSama || namaPemohonCocok || namaPelaporCocok
                                }

                                if (milikUserIni) {
                                    val itemWarga = PengajuanItem(
                                        id = id,
                                        userId = docUserId,
                                        jenisSurat = jenisSurat,
                                        namaPemohon = namaPemohon,
                                        status = status,
                                        pesanStatus = pesanStatus,
                                        nik = nik
                                    )
                                    StatusNotificationHelper.showStatusNotification(this, itemWarga)
                                }
                            }
                        }
                    }
                }
                isFirstSnapshot = false
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        listenerRegistration?.remove()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}