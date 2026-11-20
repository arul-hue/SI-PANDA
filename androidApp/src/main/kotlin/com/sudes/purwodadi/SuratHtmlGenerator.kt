package com.sudes.purwodadi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object SuratHtmlGenerator {

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getLogoBase64(context: Context? = appContext, maxHeight: Int = 140): String {
        val ctx = context ?: return ""
        return try {
            val original = BitmapFactory.decodeResource(
                ctx.resources,
                R.drawable.logo_desa
            ) ?: return ""

            val ratio = original.width.toFloat() / original.height.toFloat().coerceAtLeast(1f)
            val targetHeight = maxHeight
            val targetWidth = (targetHeight * ratio).roundToInt().coerceAtLeast(1)

            val scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
            ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.PNG, 92, output)
                val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                "data:image/png;base64,$encoded"
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            ""
        }
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun display(value: String): String {
        val cleanValue = value.trim()
        return escapeHtml(if (cleanValue.isBlank()) "-" else cleanValue)
    }

    private fun tanggalSekarang(): String {
        return SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date())
    }

    private fun tanggalSurat(item: PengajuanItem): String {
        val raw = item.tanggalKeputusan.trim()
        if (raw.isBlank()) return tanggalSekarang()

        val datePattern = Regex("""\d{1,2}\s+\p{L}+\s+\d{4}""")
        return datePattern.find(raw)?.value ?: raw.substringBefore(",").trim().ifBlank {
            tanggalSekarang()
        }
    }

    private fun isCompactDoc(item: PengajuanItem): Boolean {
        return item.jenisSurat.contains("Kematian", ignoreCase = true)
    }

    private fun getKopSuratHtml(logoBase64: String, compact: Boolean = false): String {
        val logoW = if (compact) "56px" else "64px"
        val logoH = if (compact) "76px" else "86px"
        val bottomGap = if (compact) "10px" else "16px"

        val logoHtml = if (logoBase64.isNotBlank()) {
            """<img src="$logoBase64" alt="Logo Desa" style="display: block; width: $logoW; height: $logoH; object-fit: contain;" />"""
        } else {
            """<div style="width: $logoW; height: $logoH;"></div>"""
        }

        return """
            <table style="width: 100%; border-collapse: collapse; margin: 0;">
                <tr>
                    <td style="width: 76px; text-align: left; vertical-align: middle; padding: 0 0 3px 0;">
                        $logoHtml
                    </td>
                    <td style="text-align: center; vertical-align: middle; padding: 0 18px 3px 0;">
                        <div style="font-size: 15pt; font-weight: bold; line-height: 1.12; letter-spacing: 0.3px;">PEMERINTAH KABUPATEN LAMPUNG SELATAN</div>
                        <div style="font-size: 12.5pt; font-weight: bold; line-height: 1.15; margin-top: 2px;">KECAMATAN TANJUNG BINTANG</div>
                        <div style="font-size: 16.5pt; font-weight: bold; line-height: 1.15; letter-spacing: 1.2px; margin-top: 2px;">DESA PURWODADI SIMPANG</div>
                    </td>
                </tr>
            </table>
            <div style="width: 100%; border-top: 2px solid #000000; border-bottom: 1px solid #000000; height: 1.5px; margin: 0;"></div>
            <div style="text-align: center; font-size: 9pt; line-height: 1.25; padding: 2px 0;">
                Sekretariat: Jl. Ir. Sutami Km 18 Purwodadi Simpang Kecamatan Tanjung Bintang Kabupaten Lampung Selatan 35361
            </div>
            <div style="width: 100%; border-top: 3px solid #000000; margin: 0 0 $bottomGap 0;"></div>
        """.trimIndent()
    }

    private fun getTtdHtml(item: PengajuanItem): String {
        val compact = isCompactDoc(item)
        val tanggal = escapeHtml(tanggalSurat(item))
        val topMargin = if (compact) "14px" else "24px"
        val signHeight = if (compact) "52px" else "68px"
        val fontSize = if (compact) "11pt" else "12pt"

        val headerTtd = if (item.jenisSurat.contains("Kehilangan", ignoreCase = true)) {
            """<div style="text-align: left;">Purwodadi Simpang, $tanggal</div>"""
        } else {
            """
                <table style="width: 100%; border-collapse: collapse; font-size: $fontSize;">
                    <tr>
                        <td style="width: 115px; padding: 1px 0; white-space: nowrap; text-align: left;">Dikeluarkan di</td>
                        <td style="width: 12px; text-align: center;">:</td>
                        <td style="padding: 1px 0; text-align: left;">Purwodadi Simpang</td>
                    </tr>
                    <tr>
                        <td style="width: 115px; padding: 1px 0; white-space: nowrap; text-align: left;">Pada tanggal</td>
                        <td style="width: 12px; text-align: center;">:</td>
                        <td style="padding: 1px 0; text-align: left;">$tanggal</td>
                    </tr>
                </table>
            """.trimIndent()
        }

        return """
            <div style="width: 46%; margin: $topMargin 0 0 auto; font-size: $fontSize; line-height: 1.25; text-align: left; page-break-inside: avoid;">
                $headerTtd
                <div style="text-align: left; padding-top: 2px;">Kepala Desa Purwodadi Simpang</div>
                <div style="height: $signHeight;">&nbsp;</div>
                <div style="text-align: left; font-weight: bold; letter-spacing: 0.8px; padding-left: 4px;">L A M I D I, S.E</div>
            </div>
        """.trimIndent()
    }

    private fun dataRow(
        label: String,
        value: String,
        bold: Boolean = false,
        indented: Boolean = false,
        compact: Boolean = false
    ): String {
        val shownValue = display(value)
        val valueHtml = if (bold && value.isNotBlank()) "<b>$shownValue</b>" else shownValue
        val padLeft = if (indented) "24px" else "0px"
        val padVert = if (compact) "0.8px" else "2px"
        val labelWidth = if (compact) "195px" else "210px"
        return """
            <tr>
                <td style="width: $labelWidth; padding: $padVert 4px $padVert $padLeft; vertical-align: top; white-space: nowrap;">${escapeHtml(label)}</td>
                <td style="width: 14px; padding: $padVert 0; text-align: center; vertical-align: top;">:</td>
                <td style="padding: $padVert 0; vertical-align: top;">$valueHtml</td>
            </tr>
        """.trimIndent()
    }

    private fun dataTable(
        rows: List<Triple<String, String, Boolean>>,
        indented: Boolean = false,
        compact: Boolean = false
    ): String {
        val marginStyle = if (compact) "2px 0 6px 0" else "6px 0 12px 0"
        val fontSize = if (compact) "11pt" else "12pt"
        val lineHeight = if (compact) "1.22" else "1.32"
        val rowHtml = rows.joinToString("\n") { (label, value, bold) ->
            dataRow(label, value, bold, indented, compact)
        }
        return """<table style="width: 100%; border-collapse: collapse; margin: $marginStyle; font-size: $fontSize; line-height: $lineHeight;">$rowHtml</table>"""
    }

    private fun applicantRows(item: PengajuanItem): List<Triple<String, String, Boolean>> {
        return listOf(
            Triple("Nama", item.namaPemohon, true),
            Triple("NIK", item.nik, false),
            Triple("Tempat Tanggal Lahir", item.tempatTanggalLahir, false),
            Triple("Jenis Kelamin", item.jenisKelamin, false),
            Triple("Agama", item.agama, false),
            Triple("Status", item.statusPerkawinan, false),
            Triple("Pekerjaan", item.pekerjaan, false),
            Triple("Alamat", item.alamatLengkap, false)
        )
    }

    private fun nomorSuratHtml(item: PengajuanItem): String {
        val compact = isCompactDoc(item)
        val bottomGap = if (compact) "10px" else "18px"
        val numFont = if (compact) "11pt" else "12pt"
        return """
            <div style="text-align: center; font-size: 13pt; font-weight: bold; text-decoration: underline; margin: 0 0 2px 0;">${escapeHtml(judulSurat(item.jenisSurat))}</div>
            <div style="text-align: center; font-size: $numFont; margin: 0 0 $bottomGap 0;">Nomor: ${display(item.nomorSurat)}</div>
        """.trimIndent()
    }

    private fun judulSurat(jenisSurat: String): String = when {
        jenisSurat.contains("Usaha", ignoreCase = true) -> "SURAT KETERANGAN USAHA"
        jenisSurat.contains("Domisili", ignoreCase = true) -> "SURAT KETERANGAN DOMISILI"
        jenisSurat.contains("Kehilangan", ignoreCase = true) -> "SURAT KETERANGAN KEHILANGAN"
        jenisSurat.contains("Kematian", ignoreCase = true) -> "SURAT KETERANGAN KEMATIAN"
        jenisSurat.contains("Tidak Mampu", ignoreCase = true) -> "SURAT KETERANGAN TIDAK MAMPU"
        else -> "SURAT KETERANGAN"
    }

    private fun buildBodyContent(item: PengajuanItem): String {
        return when {
            item.jenisSurat.contains("Usaha", ignoreCase = true) -> buildSuratUsaha(item)
            item.jenisSurat.contains("Domisili", ignoreCase = true) -> buildSuratDomisili(item)
            item.jenisSurat.contains("Kehilangan", ignoreCase = true) -> buildSuratKehilangan(item)
            item.jenisSurat.contains("Kematian", ignoreCase = true) -> buildSuratKematian(item)
            item.jenisSurat.contains("Tidak Mampu", ignoreCase = true) -> buildSuratSktm(item)
            else -> buildSuratUsaha(item)
        }
    }

    /**
     * Menghasilkan dokumen surat utuh (Bingkai A4 + Kop + Isi + TTD) untuk dikirim ke EmailJS via {{{surat_html}}}.
     */
    fun generateSuratLengkapEmail(context: Context, item: PengajuanItem): String {
        init(context)
        val compact = isCompactDoc(item)
        val logoBase64 = getLogoBase64(context, maxHeight = 96)
        val padPage = if (compact) "12mm 16mm 12mm 16mm" else "16mm 18mm 16mm 18mm"
        val baseFont = if (compact) "11pt" else "12pt"
        val baseLine = if (compact) "1.24" else "1.35"

        return """
            <div style="background-color: #e5e7eb; padding: 16px 10px; font-family: 'Times New Roman', Times, serif;">
              <div style="width: 210mm; min-height: 297mm; margin: 0 auto; background-color: #ffffff; padding: $padPage; border: 1px solid #d1d5db; color: #000000; font-size: $baseFont; line-height: $baseLine; box-sizing: border-box;">
                ${getKopSuratHtml(logoBase64, compact)}
                <div>
                  ${buildBodyContent(item)}
                </div>
                ${getTtdHtml(item)}
              </div>
            </div>
        """.trimIndent()
    }

    /**
     * Menghasilkan dokumen HTML lengkap untuk pratinjau WebView & cetak PDF di aplikasi Android.
     */
    fun generateHtml(context: Context, item: PengajuanItem): String {
        init(context)
        return generateHtml(item)
    }

    fun generateHtml(item: PengajuanItem): String {
        val compact = isCompactDoc(item)
        val logoBase64 = getLogoBase64(appContext, maxHeight = 150)
        val padPage = if (compact) "12mm 16mm 12mm 16mm" else "16mm 18mm 16mm 18mm"
        val baseFont = if (compact) "11pt" else "12pt"
        val baseLine = if (compact) "1.24" else "1.35"

        return """
            <!DOCTYPE html>
            <html lang="id">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>${escapeHtml(judulSurat(item.jenisSurat))}</title>
                <style>
                    @page {
                        size: A4 portrait;
                        margin: ${if (compact) "12mm 16mm" else "16mm 18mm"};
                    }
                    * {
                        box-sizing: border-box;
                    }
                    html, body {
                        width: 210mm;
                        margin: 0 auto;
                        padding: 0;
                        background: #ffffff;
                        color: #000000;
                        font-family: "Times New Roman", Times, serif;
                        font-size: $baseFont;
                        line-height: $baseLine;
                    }
                    .dokumen {
                        width: 210mm;
                        padding: $padPage;
                    }
                    @media print {
                        html, body {
                            width: 100%;
                        }
                        .dokumen {
                            padding: 0;
                            width: 100%;
                        }
                    }
                </style>
            </head>
            <body>
                <main class="dokumen">
                    ${getKopSuratHtml(logoBase64, compact)}
                    ${buildBodyContent(item)}
                    ${getTtdHtml(item)}
                </main>
            </body>
            </html>
        """.trimIndent()
    }

    // 1. SURAT KETERANGAN USAHA
    private fun buildSuratUsaha(item: PengajuanItem): String {
        val rows = applicantRows(item)
        val namaUsaha = display(item.namaUsaha.uppercase(Locale.forLanguageTag("id-ID")))
        return """
            ${nomorSuratHtml(item)}

            <p style="text-align: justify; text-indent: 36px; margin: 8px 0; font-size: 12pt; line-height: 1.35;">
                Yang bertanda tangan di bawah ini Kepala Desa Purwodadi Simpang Kecamatan Tanjung Bintang
                Kabupaten Lampung Selatan menerangkan dengan sebenarnya bahwa :
            </p>

            ${dataTable(rows, indented = true)}

            <p style="text-align: justify; text-indent: 36px; margin: 8px 0; font-size: 12pt; line-height: 1.35;">
                Bahwa nama tersebut di atas adalah benar warga Desa Purwodadi Simpang Kecamatan Tanjung
                Bintang Kabupaten Lampung Selatan, dan memiliki usaha yang sedang berjalan :
            </p>

            <div style="text-align: center; font-weight: bold; font-size: 12.5pt; margin: 14px 0;">
                “$namaUsaha”
            </div>

            <p style="text-align: justify; text-indent: 36px; margin: 8px 0; font-size: 12pt; line-height: 1.35;">
                Yang berlokasi di <b>${display(item.lokasiUsaha)}</b>.
            </p>

            <p style="text-align: justify; text-indent: 36px; margin: 8px 0; font-size: 12pt; line-height: 1.35;">
                Demikian Surat Keterangan Usaha ini dibuat untuk dapat dipergunakan dengan sebagaimana mestinya.
            </p>
        """.trimIndent()
    }

    // 2. SURAT KETERANGAN DOMISILI
    private fun buildSuratDomisili(item: PengajuanItem): String {
        val pejabatRows = listOf(
            Triple("Nama", "LAMIDI, S.E.", true),
            Triple("Jabatan", "Kepala Desa Purwodadi Simpang", false),
            Triple(
                "Alamat",
                "RT.003/RW.003 Dusun 3b Desa Purwodadi Simpang Kecamatan Tanjung Bintang Kabupaten Lampung Selatan Provinsi Lampung.",
                false
            )
        )

        val pemohonRows = listOf(
            Triple("Nama", item.namaPemohon, true),
            Triple("NIK", item.nik, false),
            Triple("Tempat, Tanggal Lahir", item.tempatTanggalLahir, false),
            Triple("Jenis Kelamin", item.jenisKelamin, false),
            Triple("Agama", item.agama, false),
            Triple("Pekerjaan", item.pekerjaan, false),
            Triple("Alamat", item.alamatLengkap, false)
        )

        return """
            ${nomorSuratHtml(item)}

            <p style="text-indent: 0; margin: 6px 0; font-size: 12pt;">Yang bertandatangan di bawah ini :</p>
            ${dataTable(pejabatRows)}

            <p style="text-indent: 0; margin: 8px 0 4px 0; font-size: 12pt;">Menerangkan Bahwa :</p>
            ${dataTable(pemohonRows)}

            <p style="text-align: justify; text-indent: 36px; margin: 10px 0; font-size: 12pt; line-height: 1.35;">
                Bahwa nama tersebut benar Berdomisili di <b>${display(item.lokasiUsaha)}</b>.
            </p>

            <p style="text-align: justify; text-indent: 36px; margin: 10px 0; font-size: 12pt; line-height: 1.35;">
                Demikian Surat Keterangan Domisili ini dibuat dengan sebenarnya untuk dapat dipergunakan
                sebagaimana mestinya.
            </p>
        """.trimIndent()
    }

    // 3. SURAT KETERANGAN KEHILANGAN
    private fun buildSuratKehilangan(item: PengajuanItem): String {
        val parts = item.lokasiUsaha.split("|").map { it.trim() }

        fun detail(prefix: String, fallback: String = "-"): String {
            val value = parts.firstOrNull { it.startsWith(prefix, ignoreCase = true) }
                ?.substringAfter(":")
                ?.trim()
            return value?.takeIf { it.isNotBlank() } ?: fallback
        }

        val hari = detail("Hari", item.hariKejadian)
        val tanggal = detail("Tanggal", item.tanggalKejadian)
        val jam = detail("Jam", item.jamKejadian)
        val barang = detail("Barang", item.barangHilang.ifBlank { item.namaUsaha })

        return """
            <table style="width: 100%; border-collapse: collapse; margin: 4px 0 18px 0; font-size: 12pt; line-height: 1.35;">
                <tr>
                    <td style="width: 60%; vertical-align: top;">
                        <table style="border-collapse: collapse; font-size: 12pt;">
                            <tr><td style="width: 55px; padding: 1px 2px;">No</td><td style="width: 14px; text-align: center;">:</td><td>${display(item.nomorSurat)}</td></tr>
                            <tr><td style="width: 55px; padding: 1px 2px;">Lamp</td><td style="width: 14px; text-align: center;">:</td><td>-</td></tr>
                            <tr><td style="width: 55px; padding: 1px 2px;">Hal</td><td style="width: 14px; text-align: center;">:</td><td><b>Laporan Kehilangan</b></td></tr>
                        </table>
                    </td>
                    <td style="width: 40%; padding-left: 8px; vertical-align: top;">
                        Kepada Yth,<br />
                        <b>Kapolsek Tanjung Bintang</b><br />
                        <b>Di-</b><br />
                        <span style="padding-left: 24px;"><b>Tempat</b></span>
                    </td>
                </tr>
            </table>

            <p style="text-align: justify; text-indent: 36px; margin: 8px 0; font-size: 12pt; line-height: 1.35;">
                Yang bertanda tangan di bawah ini Kepala Desa Purwodadi Simpang Kecamatan Tanjung Bintang,
                Kabupaten Lampung Selatan menerangkan dengan sebenarnya :
            </p>

            ${dataTable(listOf(
            Triple("Nama", item.namaPemohon, true),
            Triple("Nomor NIK", item.nik, false),
            Triple("Tempat / Tgl Lahir", item.tempatTanggalLahir, false),
            Triple("Pekerjaan", item.pekerjaan, false),
            Triple("Agama", item.agama, false),
            Triple("Status", item.statusPerkawinan, false),
            Triple("Alamat", item.alamatLengkap, false)
        ), compact = true)}

            <p style="text-align: justify; text-indent: 36px; margin: 10px 0; font-size: 12pt; line-height: 1.35;">
                Berdasarkan laporan nama tersebut di atas, bahwa pada hari <b>${display(hari)}</b> tanggal
                <b>${display(tanggal)}</b> Jam <b>${display(jam)}</b> WIB, nama di atas telah melaporkan
                kehilangan sebuah <b>${display(barang)}</b> di sekitaran Jalan Ir. Sutami, Desa Purwodadi
                Simpang, Kecamatan Tanjung Bintang, Kabupaten Lampung Selatan.
            </p>

            <p style="text-align: justify; text-indent: 36px; margin: 10px 0; font-size: 12pt; line-height: 1.35;">
                Demikian Laporan Kehilangan ini kami buat agar dapat dipergunakan perlunya. Atas perhatian
                serta bantuannya sebelumnya diucapkan banyak terima kasih.
            </p>
        """.trimIndent()
    }

    // 4. SURAT KETERANGAN KEMATIAN
    private fun buildSuratKematian(item: PengajuanItem): String {
        return """
            ${nomorSuratHtml(item)}

            <p style="text-align: justify; text-indent: 32px; margin: 4px 0; font-size: 11pt; line-height: 1.24;">
                Yang bertanda tangan di bawah ini Kepala Desa Purwodadi Simpang Kecamatan Tanjung
                Bintang Kabupaten Lampung Selatan menerangkan dengan sebenarnya bahwa :
            </p>

            ${dataTable(listOf(
            Triple("Nama", item.namaPemohon, true),
            Triple("NIK", item.nik, false),
            Triple("Umur", item.umur, false),
            Triple("Tempat Tanggal Lahir", item.tempatTanggalLahir, false),
            Triple("Jenis Kelamin", item.jenisKelamin, false),
            Triple("Agama", item.agama, false),
            Triple("Status", item.statusPerkawinan, false),
            Triple("Pekerjaan", item.pekerjaan, false),
            Triple("Alamat", item.alamatLengkap, false)
        ), indented = true, compact = true)}

            <p style="text-align: justify; text-indent: 32px; margin: 4px 0 2px 0; font-size: 11pt; line-height: 1.24;">
                Bahwa nama tersebut di atas adalah benar warga Desa Purwodadi Simpang Kecamatan Tanjung Bintang
                Kabupaten Lampung Selatan yang telah Meninggal Dunia pada :
            </p>
            ${dataTable(listOf(
            Triple("Pukul", item.pukulWafat, false),
            Triple("Hari", item.hariWafat, false),
            Triple("Tanggal", item.tanggalWafat, false),
            Triple("Tempat", item.tempatWafat, false)
        ), indented = true, compact = true)}

            <p style="text-indent: 0; margin: 4px 0 2px 0; font-size: 11pt; line-height: 1.24;">Yang dimakamkan pada :</p>
            ${dataTable(listOf(
            Triple("Pukul", item.pukulPemakaman, false),
            Triple("Hari", item.hariPemakaman, false),
            Triple("Tanggal", item.tanggalPemakaman, false),
            Triple("Tempat", item.tempatPemakaman, false)
        ), indented = true, compact = true)}

            <p style="text-indent: 0; margin: 4px 0 2px 0; font-size: 11pt; line-height: 1.24;">Pelapor</p>
            ${dataTable(listOf(
            Triple("Nama", item.namaPelapor, false),
            Triple("NIK", item.nikPelapor, false),
            Triple("Alamat", item.alamatPelapor, false),
            Triple("Status", item.namaUsaha, false)
        ), indented = true, compact = true)}

            <p style="text-align: justify; text-indent: 32px; margin: 6px 0 0 0; font-size: 11pt; line-height: 1.24;">
                Demikian Surat Keterangan Kematian ini dibuat untuk dapat dipergunakan dengan sebagaimana mestinya.
            </p>
        """.trimIndent()
    }

    // 5. SURAT KETERANGAN TIDAK MAMPU (SKTM)
    private fun buildSuratSktm(item: PengajuanItem): String {
        val parts = item.lokasiUsaha.split("|").map { it.trim() }
        val noKk = parts.firstOrNull { it.startsWith("No. KK:", ignoreCase = true) }
            ?.substringAfter(":")?.trim().orEmpty()
        val keperluan = parts.firstOrNull { it.startsWith("Keperluan:", ignoreCase = true) }
            ?.substringAfter(":")?.trim().orEmpty()

        val finalNoKk = noKk.ifBlank { item.lokasiUsaha.takeIf { it.isNotBlank() } ?: "-" }
        val finalKeperluan = keperluan.ifBlank { "-" }

        return """
            ${nomorSuratHtml(item)}

            <p style="text-align: justify; text-indent: 36px; margin: 8px 0; font-size: 12pt; line-height: 1.35;">
                Yang bertanda tangan di bawah ini Kepala Desa Purwodadi Simpang Kecamatan Tanjung Bintang
                Kabupaten Lampung Selatan menerangkan dengan sebenarnya bahwa :
            </p>

            ${dataTable(listOf(
            Triple("Nama", item.namaPemohon, true),
            Triple("NIK", item.nik, false),
            Triple("Tempat Tanggal Lahir", item.tempatTanggalLahir, false),
            Triple("Jenis Kelamin", item.jenisKelamin, false),
            Triple("Agama", item.agama, false),
            Triple("Status", item.statusPerkawinan, false),
            Triple("Pekerjaan", item.pekerjaan, false),
            Triple("Alamat", item.alamatLengkap, false)
        ), indented = true)}

            <p style="text-align: justify; text-indent: 36px; margin: 10px 0; font-size: 12pt; line-height: 1.35;">
                Bahwa nama tersebut di atas adalah benar warga Desa Purwodadi Simpang Kecamatan Tanjung
                Bintang Kabupaten Lampung Selatan. Berdasarkan keterangan yang ada pada kami bahwa benar
                tercantum dalam Kartu Keluarga dengan nomor <b>${display(finalNoKk)}</b> atas nama
                <b>${display(item.namaUsaha)}</b> dan menurut sepengetahuan kami keluarga yang bersangkutan
                tergolong keluarga yang tidak mampu. Surat Keterangan Tidak Mampu ini dibuat untuk
                persyaratan <b>${display(finalKeperluan)}</b>.
            </p>

            <p style="text-align: justify; text-indent: 36px; margin: 10px 0; font-size: 12pt; line-height: 1.35;">
                Demikian Surat Keterangan Tidak Mampu ini dibuat untuk dapat dipergunakan dengan sebagaimana mestinya.
            </p>
        """.trimIndent()
    }
}