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

    private fun dash(s: String?): String = if (s.isNullOrBlank()) "—" else s
    private fun num(v: Double?, fmt: String = "%.1f"): String =
        if (v == null) "—" else String.format(Locale.US, fmt, v)

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
            color = brandRed; textSize = 20f; isFakeBoldText = true
        }
        val subTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(80, 80, 80); textSize = 9f; isFakeBoldText = true
        }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40); textSize = 8.5f
        }
        val smallBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30); textSize = 8.5f; isFakeBoldText = true
        }
        val sectionWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 9f; isFakeBoldText = true
        }
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 90, 90); textSize = 7.5f
        }
        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 0.8f; color = lineGray
        }

        var y = MARGIN

        c.drawText("MONO CHROME", MARGIN, y + 18f, titlePaint)
        c.drawText("FOR IVD SOLUTIONS", MARGIN, y + 32f, subTitle)
        c.drawText("Powered by Mono Chrome", PAGE_W - MARGIN - 110f, y + 18f, subTitle)
        y += 44f

        fillPaint.color = brandCyan
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 2.5f, fillPaint)
        y += 12f

        val dateFmt = SimpleDateFormat("dd-MMM-yy HH:mm", Locale.US)
        val dateStr = dateFmt.format(Date(r.timestampEpochMillis))
        val shortId = r.sampleId.take(12).uppercase(Locale.US)

        val ageSex = listOfNotNull(
            r.patientAge?.takeIf { it.isNotBlank() },
            r.patientSex?.takeIf { it.isNotBlank() }
        ).joinToString(" / ").ifBlank { "—" }

        val metaLeft = listOf(
            "Patient Name : ${dash(r.patientName)}",
            "Age / Sex    : $ageSex",
            "Referred By  : ${dash(r.referredBy)}",
            "Centre       : ${dash(r.centre)}"
        )
        val metaRight = listOf(
            "Lab No        : $shortId",
            "Registration  : $dateStr",
            "Patient ID    : ${dash(r.patientId)}",
            "Abstinence    : ${if (r.abstinenceDays != null) String.format(Locale.US, "%.0f days", r.abstinenceDays) else "—"}"
        )
        metaLeft.forEachIndexed { i, left ->
            c.drawText(left, MARGIN, y + 11f, small)
            c.drawText(metaRight[i], PAGE_W / 2f + 10f, y + 11f, small)
            y += 13f
        }
        y += 8f

        fillPaint.color = headerBg
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 18f, fillPaint)
        c.drawText("Semen Analysis", MARGIN + 8f, y + 13f, sectionWhite)
        c.drawText("Semen Sample", PAGE_W - MARGIN - 80f, y + 13f, sectionWhite)
        y += 22f

        c.drawText("Collected On: $dateStr", MARGIN, y + 10f, small)
        c.drawText("Received On: $dateStr", MARGIN + 180f, y + 10f, small)
        c.drawText("Accession: MC-$shortId", MARGIN + 360f, y + 10f, small)
        y += 16f

        val colX = floatArrayOf(MARGIN, MARGIN + 150f, MARGIN + 230f, MARGIN + 290f, MARGIN + 420f)
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
            listOf("Observation", "Result", "Unit", "Biological Ref. Interval", "Method")
                .forEachIndexed { i, h -> c.drawText(h, colX[i] + 3f, y + 10f, smallBold) }
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
                if (highlightLow) { color = Color.rgb(180, 0, 0); isFakeBoldText = true }
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

        drawColHeaders()

        drawHeaderRow("Physical Features")
        drawDataRow("Colour", dash(r.colour), "", "", "Physical Examination")
        drawDataRow("Semen Ph", num(r.semenPh), "pH", "7.2-7.8", "Physical Examination",
            highlightLow = r.semenPh != null && (r.semenPh < 7.2 || r.semenPh > 7.8))
        drawDataRow("Volume [Semen]", num(r.volumeMl), "ml", ">1.5", "Physical Examination",
            highlightLow = r.volumeMl != null && r.volumeMl < 1.5)
        drawDataRow("Viscosity", dash(r.viscosity), "", "NORMAL", "Physical Examination")
        drawDataRow("Appearance", dash(r.appearance), "", "", "Physical Examination")
        drawDataRow("Liquefaction Time", num(r.liquefactionTimeMin, "%.0f"), "Min.", "30-60", "Physical Examination")
        drawDataRow(
            "Abstinence Period",
            if (r.abstinenceDays != null) String.format(Locale.US, "%.0f", r.abstinenceDays) else "—",
            "Days",
            "2-7",
            "Patient History",
            highlightLow = r.abstinenceDays != null && (r.abstinenceDays < 2 || r.abstinenceDays > 7)
        )

        drawHeaderRow("Microscopic Features")

        val conc = r.humanCorrectedConcentration ?: r.estimatedConcentrationMillionPerMl
        drawDataRow(
            "Sperm Concentration",
            String.format(Locale.US, "%.1f", conc),
            "Millions / mL",
            ">${String.format(Locale.US, "%.0f", r.whoConcentrationLimitMillionPerMl)}",
            "AI Assist / Microscopy",
            highlightLow = conc < r.whoConcentrationLimitMillionPerMl
        )

        val totalCount = if (r.volumeMl != null) conc * r.volumeMl else null
        drawDataRow(
            "Total Sperm Count",
            num(totalCount),
            "Millions",
            ">39",
            "Calculated",
            highlightLow = totalCount != null && totalCount < 39.0
        )

        val pr = r.progressiveMotilityPercent
        drawDataRow(
            "Progressive Motility",
            num(pr),
            "%",
            ">32",
            "AI Tracking",
            highlightLow = pr != null && pr < 32.0
        )
        drawDataRow("Non-Progressive Motility", num(r.nonProgressiveMotilityPercent), "%", "", "AI Tracking")
        val im = r.immotilePercent
        drawDataRow(
            "Immotile",
            num(im),
            "%",
            "<50",
            "AI Tracking",
            highlightLow = im != null && im >= 50.0
        )
        val totalMot = r.totalMotilityPercent
        if (totalMot != null) {
            drawDataRow(
                "Total Motility (PR+NP)",
                num(totalMot),
                "%",
                ">42",
                "AI Tracking",
                highlightLow = totalMot < 42.0
            )
        }

        val normalForms = r.humanCorrectedNormalFormsPercent ?: r.estimatedNormalFormsPercent
        if (normalForms != null) {
            val abn = (100.0 - normalForms).coerceIn(0.0, 100.0)
            drawDataRow("Normal Forms", num(normalForms), "%", ">4", "Morphology Assist",
                highlightLow = normalForms < 4.0)
            drawDataRow("Abnormal Forms", num(abn), "%", "", "Morphology Assist")
        } else {
            drawDataRow("Normal Forms", "—", "%", ">4", "Morphology Assist")
            drawDataRow("Abnormal Forms", "—", "%", "", "Morphology Assist")
        }

        drawDataRow("Fructose [In Semen]", dash(r.fructose), "", "Positive", "Seliwanoff's")
        drawDataRow("Sperm Vitality-Dead", num(r.vitalityDeadPercent, "%.0f"), "%", "", "Microscopy")
        drawDataRow(
            "Sperm Vitality-Alive",
            num(r.vitalityAlivePercent, "%.0f"),
            "%",
            ">58",
            "Microscopy",
            highlightLow = r.vitalityAlivePercent != null && r.vitalityAlivePercent < 58.0
        )
        drawDataRow("Pus Cells [Semen]", dash(r.pusCells), "/HPF", "", "Microscopy")
        drawDataRow("Round Cells [Semen]", dash(r.roundCells), "", "", "Microscopy")

        y += 10f
        c.drawText(
            String.format(
                Locale.US,
                "AI confidence: %.0f%%  |  Frames: %d  |  Reviewed: %s",
                r.concentrationConfidenceScore * 100,
                r.framesAnalyzed,
                if (r.reviewedByHuman) "Yes" else "Pending"
            ),
            MARGIN, y, small
        )
        y += 12f
        if (!r.reviewerNote.isNullOrBlank()) {
            c.drawText("Technician note: ${r.reviewerNote!!.take(90)}", MARGIN, y, small)
            y += 12f
        }

        y += 4f
        fillPaint.color = Color.rgb(255, 250, 240)
        c.drawRect(MARGIN, y, tableRight, y + 34f, fillPaint)
        strokePaint.color = brandRed
        c.drawRect(MARGIN, y, tableRight, y + 34f, strokePaint)
        c.drawText(
            "NOTE: Computer-assisted assistive report. Interpret by a medical professional. Not a certified medical device.",
            MARGIN + 6f, y + 14f, notePaint
        )
        c.drawText(
            "WHO 6th edition reference intervals where applicable. Powered by Mono Chrome.",
            MARGIN + 6f, y + 26f, notePaint
        )
        y += 46f

        c.drawText("Authorized Signature", MARGIN, y, smallBold)
        strokePaint.color = lineGray
        c.drawLine(MARGIN, y + 26f, MARGIN + 150f, y + 26f, strokePaint)
        c.drawText("Doctor / Lab In-charge", MARGIN, y + 38f, notePaint)

        c.drawText("Verified By", PAGE_W / 2f, y, smallBold)
        c.drawLine(PAGE_W / 2f, y + 26f, PAGE_W / 2f + 150f, y + 26f, strokePaint)

        y = PAGE_H - 40f
        fillPaint.color = brandCyan
        c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 1.5f, fillPaint)
        c.drawText(
            "Powered by Mono Chrome — FOR IVD SOLUTIONS  |  Assistive prototype",
            MARGIN, y + 14f, notePaint
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
