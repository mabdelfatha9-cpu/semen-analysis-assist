package com.labtools.semenanalysis.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.labtools.semenanalysis.data.SampleReportEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a professional semen-analysis PDF close to standard lab reports
 * (Observation | Result | Unit | Biological Ref. Interval | Method).
 */
object ReportPdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f

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
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas

        val brandRed = Color.rgb(139, 0, 40)
        val brandCyan = Color.rgb(0, 160, 200)
        val headerBg = Color.rgb(139, 0, 40)
        val rowAlt = Color.rgb(245, 248, 250)
        val lineGray = Color.rgb(200, 200, 200)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandRed
            textSize = 20f
            isFakeBoldText = true
        }
        val subTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(80, 80, 80)
            textSize = 9f
            isFakeBoldText = true
        }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40)
            textSize = 8.5f
        }
        val smallBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30)
            textSize = 8.5f
            isFakeBoldText = true
        }
        val whiteBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            isFakeBoldText = true
        }
        val sectionWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            isFakeBoldText = true
        }
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 90, 90)
            textSize = 7.5f
        }
        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = lineGray
        }

        var y = MARGIN

        // —— Header brand ——
        c.drawText("MONO CHROME", MARGIN, y + 18f, titlePaint)
        c.drawText("FOR IVD SOLUTIONS", MARGIN, y + 32f, subTitle)
        c.drawText("Powered by Mono Chrome", PAGE_W - MARGIN - 110f, y + 18f, subTitle)
        y += 44f

        // thin brand line
        fillPaint.color = brandCyan
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 2.5f, fillPaint)
        y += 12f

        // —— Patient / lab meta (two columns) ——
        val dateFmt = SimpleDateFormat("dd-MMM-yy HH:mm", Locale.US)
        val dateStr = dateFmt.format(Date(r.timestampEpochMillis))
        val shortId = r.sampleId.take(12).uppercase(Locale.US)

        val metaLeft = listOf(
            "Patient Name : ________________", 
            "Age / Sex    : ________________",
            "Referred By  : ________________",
            "Centre       : ________________"
        )
        val metaRight = listOf(
            "Lab No        : $shortId",
            "Registration  : $dateStr",
            "Patient ID    : ________________",
            "Accession No  : MC-$shortId"
        )
        metaLeft.forEachIndexed { i, left ->
            c.drawText(left, MARGIN, y + 11f, small)
            c.drawText(metaRight[i], PAGE_W / 2f + 10f, y + 11f, small)
            y += 13f
        }
        y += 8f

        // —— Red section bar: Semen Analysis | Semen Sample ——
        fillPaint.color = headerBg
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 18f, fillPaint)
        c.drawText("Semen Analysis", MARGIN + 8f, y + 13f, sectionWhite)
        c.drawText("Semen Sample", PAGE_W - MARGIN - 80f, y + 13f, sectionWhite)
        y += 22f

        c.drawText("Collected On: $dateStr", MARGIN, y + 10f, small)
        c.drawText("Received On: $dateStr", MARGIN + 180f, y + 10f, small)
        c.drawText("Approved On: $dateStr", MARGIN + 360f, y + 10f, small)
        y += 16f

        // —— Table header ——
        val colX = floatArrayOf(
            MARGIN,                    // Observation
            MARGIN + 150f,             // Result
            MARGIN + 230f,             // Unit
            MARGIN + 290f,             // Ref interval
            MARGIN + 420f              // Method
        )
        val tableRight = PAGE_W - MARGIN
        val rowH = 14f

        fun drawHeaderRow(label: String) {
            fillPaint.color = headerBg
            c.drawRect(MARGIN, y, tableRight, y + rowH, fillPaint)
            c.drawText(label, MARGIN + 6f, y + 10f, sectionWhite)
            y += rowH
        }

        fun drawColHeaders() {
            fillPaint.color = Color.rgb(230, 230, 235)
            c.drawRect(MARGIN, y, tableRight, y + rowH, fillPaint)
            val headers = listOf("Observation", "Result", "Unit", "Biological Ref. Interval", "Method")
            headers.forEachIndexed { i, h ->
                c.drawText(h, colX[i] + 3f, y + 10f, smallBold)
            }
            c.drawLine(MARGIN, y, tableRight, y, strokePaint)
            c.drawLine(MARGIN, y + rowH, tableRight, y + rowH, strokePaint)
            y += rowH
        }

        var rowIndex = 0
        fun drawDataRow(
            observation: String,
            result: String,
            unit: String,
            ref: String,
            method: String,
            highlightLow: Boolean = false
        ) {
            if (rowIndex % 2 == 1) {
                fillPaint.color = rowAlt
                c.drawRect(MARGIN, y, tableRight, y + rowH, fillPaint)
            }
            val resultPaint = Paint(small).apply {
                if (highlightLow) {
                    color = Color.rgb(180, 0, 0)
                    isFakeBoldText = true
                }
            }
            c.drawText(observation, colX[0] + 3f, y + 10f, small)
            c.drawText(result, colX[1] + 3f, y + 10f, resultPaint)
            c.drawText(unit, colX[2] + 3f, y + 10f, small)
            c.drawText(ref, colX[3] + 3f, y + 10f, small)
            c.drawText(method, colX[4] + 3f, y + 10f, small)
            c.drawLine(MARGIN, y + rowH, tableRight, y + rowH, strokePaint)
            y += rowH
            rowIndex++
        }

        // Column vertical lines for header area only — draw outer border
        fun strokeTableTop() {
            c.drawRect(MARGIN, y, tableRight, y, strokePaint)
        }

        drawColHeaders()

        // Physical Features (manual / placeholder — filled when lab enters them)
        drawHeaderRow("Physical Features")
        drawDataRow("Colour", "—", "", "", "Physical Examination")
        drawDataRow("Semen Ph", "—", "pH", "7.2-7.8", "Physical Examination")
        drawDataRow("Volume [Semen]", "—", "ml", ">1.5", "Physical Examination")
        drawDataRow("Viscosity", "—", "", "NORMAL", "Physical Examination")
        drawDataRow("Appearance", "—", "", "", "Physical Examination")
        drawDataRow("Liquefaction Time", "—", "Min.", "30-60", "Physical Examination")

        // Microscopic Features from app analysis
        drawHeaderRow("Microscopic Features")

        val conc = r.humanCorrectedConcentration ?: r.estimatedConcentrationMillionPerMl
        val concStr = String.format(Locale.US, "%.1f", conc)
        val concLow = conc < r.whoConcentrationLimitMillionPerMl
        drawDataRow(
            "Sperm Concentration",
            concStr,
            "Millions / mL",
            ">${String.format(Locale.US, "%.0f", r.whoConcentrationLimitMillionPerMl)}",
            "AI Assist / Microscopy",
            highlightLow = concLow
        )

        // Total count needs volume — show N/A without volume
        drawDataRow("Total Sperm Count", "—", "Millions", ">39", "Calculated")

        val pr = r.progressiveMotilityPercent
        val np = r.nonProgressiveMotilityPercent
        val im = r.immotilePercent
        if (pr != null) {
            drawDataRow(
                "Progressive Motility",
                String.format(Locale.US, "%.1f", pr),
                "%",
                ">32",
                "AI Tracking",
                highlightLow = pr < 32.0
            )
        } else {
            drawDataRow("Progressive Motility", "—", "%", ">32", "AI Tracking")
        }
        if (np != null) {
            drawDataRow(
                "Non-Progressive Motility",
                String.format(Locale.US, "%.1f", np),
                "%",
                "",
                "AI Tracking"
            )
        } else {
            drawDataRow("Non-Progressive Motility", "—", "%", "", "AI Tracking")
        }
        if (im != null) {
            drawDataRow(
                "Immotile",
                String.format(Locale.US, "%.1f", im),
                "%",
                "<50",
                "AI Tracking",
                highlightLow = im >= 50.0
            )
        } else {
            drawDataRow("Immotile", "—", "%", "<50", "AI Tracking")
        }

        val totalMot = r.totalMotilityPercent
        if (totalMot != null) {
            drawDataRow(
                "Total Motility (PR+NP)",
                String.format(Locale.US, "%.1f", totalMot),
                "%",
                ">42",
                "AI Tracking",
                highlightLow = totalMot < 42.0
            )
        }

        val normalForms = r.humanCorrectedNormalFormsPercent ?: r.estimatedNormalFormsPercent
        if (normalForms != null) {
            val abn = (100.0 - normalForms).coerceIn(0.0, 100.0)
            drawDataRow(
                "Normal Forms",
                String.format(Locale.US, "%.1f", normalForms),
                "%",
                ">4",
                "Morphology Assist",
                highlightLow = normalForms < 4.0
            )
            drawDataRow(
                "Abnormal Forms",
                String.format(Locale.US, "%.1f", abn),
                "%",
                "",
                "Morphology Assist"
            )
        } else {
            drawDataRow("Normal Forms", "—", "%", ">4", "Morphology Assist")
            drawDataRow("Abnormal Forms", "—", "%", "", "Morphology Assist")
        }

        drawDataRow("Fructose [In Semen]", "—", "", "Positive", "Seliwanoff's")
        drawDataRow("Sperm Vitality-Dead", "—", "%", "", "Microscopy")
        drawDataRow("Sperm Vitality-Alive", "—", "%", ">58", "Microscopy")
        drawDataRow("Pus Cells [Semen]", "—", "/HPF", "", "Microscopy")
        drawDataRow("Round Cells [Semen]", "—", "", "", "Microscopy")

        // Outer border
        strokePaint.color = Color.rgb(160, 160, 160)
        strokePaint.strokeWidth = 1.2f
        // Approximate table top: we don't track exact top Y easily — skip heavy border

        y += 12f

        // Confidence / AI note
        c.drawText(
            String.format(
                Locale.US,
                "AI confidence (concentration): %.0f%%   |   Frames analyzed: %d   |   Reviewed: %s",
                r.concentrationConfidenceScore * 100,
                r.framesAnalyzed,
                if (r.reviewedByHuman) "Yes" else "Pending"
            ),
            MARGIN,
            y,
            small
        )
        y += 12f

        if (!r.reviewerNote.isNullOrBlank()) {
            c.drawText("Technician note: ${r.reviewerNote!!.take(100)}", MARGIN, y, small)
            y += 12f
        }

        // NOTE box
        y += 6f
        fillPaint.color = Color.rgb(255, 250, 240)
        c.drawRect(MARGIN, y, tableRight, y + 36f, fillPaint)
        strokePaint.color = brandRed
        c.drawRect(MARGIN, y, tableRight, y + 36f, strokePaint)
        c.drawText(
            "NOTE: This report is generated with computer-assisted analysis (assistive tool) and is designed",
            MARGIN + 6f, y + 12f, notePaint
        )
        c.drawText(
            "to be read and interpreted by a medical professional. Correlate results clinically. Not a certified medical device.",
            MARGIN + 6f, y + 24f, notePaint
        )
        y += 48f

        // Signature area
        c.drawText("Authorized Signature", MARGIN, y, smallBold)
        c.drawLine(MARGIN, y + 28f, MARGIN + 160f, y + 28f, strokePaint)
        c.drawText("Doctor / Lab In-charge", MARGIN, y + 40f, notePaint)

        c.drawText("Verified By", PAGE_W / 2f, y, smallBold)
        c.drawLine(PAGE_W / 2f, y + 28f, PAGE_W / 2f + 160f, y + 28f, strokePaint)

        // Footer
        y = PAGE_H - 40f
        fillPaint.color = brandCyan
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 1.5f, fillPaint)
        c.drawText(
            "Powered by Mono Chrome — FOR IVD SOLUTIONS  |  Assistive prototype — not for standalone diagnosis",
            MARGIN,
            y + 14f,
            notePaint
        )
        c.drawText("Page 1 of 1", PAGE_W - MARGIN - 50f, y + 14f, notePaint)

        doc.finishPage(page)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "SA_Report_${shortId}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
