package com.sudes.purwodadi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageStorageHelper {

    /**
     * Mengubah URI sementara galeri/kamera menjadi string Base64 permanen
     * yang sudah dikompres agar aman disimpan langsung di Firestore (< 300 KB).
     */
    fun uriToPermanentBase64(context: Context, uriString: String): String {
        // Jika sudah berbentuk Base64 permanen atau URL HTTP, langsung kembalikan
        if (uriString.startsWith("data:image") || uriString.startsWith("http")) {
            return uriString
        }

        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return ""

            // Perkecil resolusi maksimal sisi terpanjang 1024px agar tetap tajam dibaca tapi ringan
            val maxDimension = 1024
            val ratio = minOf(
                maxDimension.toFloat() / originalBitmap.width,
                maxDimension.toFloat() / originalBitmap.height,
                1.0f
            )
            val targetWidth = (originalBitmap.width * ratio).toInt()
            val targetHeight = (originalBitmap.height * ratio).toInt()
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            val outputStream = ByteArrayOutputStream()
            // Kompres kualitas JPEG 75% (ukuran sekitar 80KB - 200KB, jauh di bawah batas 1MB Firestore)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val byteArray = outputStream.toByteArray()

            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Mengubah string Base64 kembali menjadi Bitmap untuk ditampilkan di layar Detail Warga / Admin.
     */
    fun base64ToBitmap(dataString: String): Bitmap? {
        return try {
            val cleanBase64 = if (dataString.contains(",")) {
                dataString.substringAfter(",")
            } else {
                dataString
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}