package com.labtools.semenanalysis.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.labtools.semenanalysis.data.SampleReportEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportPdfExporter {

    fun exportAndShare(context: Context, report: SampleReportEntity) {
        val file = writePdf(context, report)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Semen Analysis Report - Mono Chrome")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة / طباعة التقرير"))
    }

    private fun writePdf(context: Context, r: SampleReportEntity): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4-ish points
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 180, 228)
            textSize = 22f
            isFakeBoldText = true
        }
        val heading = Paint().apply {
            color = Color.rgb(120, 20, 40)
            textSize = 16f
            isFakeBoldText = true
        }
        val body = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val muted = Paint().apply {
            color = Color.GRAY
            textSize = 10f
        }

        var y = 50f
        fun line(text: String, paint: Paint = body, dy: Float = 18f) {
            canvas.drawText(text, 40f, y, paint)
            y += dy
        }

        line("MONO CHROME", titlePaint, 26f)
        line("FOR IVD SOLUTIONS — Powered by Mono Chrome", muted, 22f)
        line("تقرير تحليل السائل المنوي (مساعد حاسوبي)", heading, 28f)

        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            .format(Date(r.timestampEpochMillis))
        line("Sample ID: ${r.sampleId}")
        line("Date: $date", body, 24f)

        line("التركيز (Concentration)", heading, 22f)
        line(String.format(Locale.US, "تقديري: %.2f مليون/مل", r.estimatedConcentrationMillionPerMl))
        line(String.format(Locale.US, "حد WHO: %.1f مليون/مل", r.whoConcentrationLimitMillionPerMl))
        line(String.format(Locale.US, "ثقة: %.0f%%", r.concentrationConfidenceScore * 100))
        if (r.humanCorrectedConcentration != null) {
            line(String.format(Locale.US, "تصحيح الفني: %.2f مليون/مل", r.humanCorrectedConcentration))
        }
        y += 8f

        if (r.totalMotilityPercent != null) {
            line("الحركة (Motility)", heading, 22f)
            line(String.format(Locale.US, "PR: %.1f%%   NP: %.1f%%   IM: %.1f%%",
                r.progressiveMotilityPercent ?: 0.0,
                r.nonProgressiveMotilityPercent ?: 0.0,
                r.immotilePercent ?: 0.0))
            line(String.format(Locale.US, "إجمالي الحركة: %.1f%%", r.totalMotilityPercent))
            y += 8f
        }

        val forms = r.humanCorrectedNormalFormsPercent ?: r.estimatedNormalFormsPercent
        if (forms != null) {
            line("الشكل (Morphology / Forms)", heading, 22f)
            line(String.format(Locale.US, "أشكال طبيعية: %.1f%%", forms))
            if (r.morphologyConfidenceScore != null) {
                line(String.format(Locale.US, "ثقة AI: %.0f%%", r.morphologyConfidenceScore * 100))
            }
            y += 8f
        }

        if (r.warnings.isNotEmpty()) {
            line("تحذيرات", heading, 22f)
            r.warnings.take(8).forEach { w ->
                val t = if (w.length > 80) w.take(77) + "..." else w
                line("• $t", muted, 14f)
            }
            y += 8f
        }

        if (!r.reviewerNote.isNullOrBlank()) {
            line("ملاحظة الفني", heading, 22f)
            line(r.reviewerNote!!.take(120))
        }

        y = 780f
        line("ليس جهازًا طبيًا معتمدًا — للمساعدة البحثية فقط.", muted, 14f)
        line("Powered by Mono Chrome — FOR IVD SOLUTIONS", muted)

        doc.finishPage(page)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "report_${r.sampleId.take(8)}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
