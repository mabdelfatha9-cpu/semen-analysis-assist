package com.labtools.semenanalysis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.labtools.semenanalysis.data.SampleReportEntity
import kotlin.math.max
import kotlin.math.min

private val Cyan = Color(0xFF00B4E4)
private val Red = Color(0xFFC62828)
private val Green = Color(0xFF2E7D32)
private val Orange = Color(0xFFEF6C00)
private val GrayBar = Color(0xFFB0BEC5)

@Composable
fun ResultChartsSection(report: SampleReportEntity) {
    val conc = report.humanCorrectedConcentration ?: report.estimatedConcentrationMillionPerMl
    val who = report.whoConcentrationLimitMillionPerMl
    val pr = report.progressiveMotilityPercent
    val np = report.nonProgressiveMotilityPercent
    val im = report.immotilePercent
    val forms = report.humanCorrectedNormalFormsPercent ?: report.estimatedNormalFormsPercent

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "الرسوم البيانية / Charts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Powered by Mono Chrome",
                color = Cyan,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("التركيز vs حد WHO", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            ConcentrationBarChart(value = conc, limit = who)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format("النتيجة: %.1f  |  الحد: %.0f مليون/مل", conc, who),
                style = MaterialTheme.typography.bodySmall
            )

            if (pr != null || np != null || im != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("توزيع الحركة (Motility)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                MotilityStackedBar(
                    progressive = pr ?: 0.0,
                    nonProgressive = np ?: 0.0,
                    immotile = im ?: 0.0
                )
                Spacer(modifier = Modifier.height(8.dp))
                LegendRow(
                    listOf(
                        "PR (تقدمية)" to Green,
                        "NP" to Orange,
                        "IM (ساكنة)" to Red
                    )
                )
            }

            if (forms != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("الشكل (Normal forms)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                MorphologyBar(normalPercent = forms)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.1f%% طبيعي  |  حد WHO ≈ 4%%", forms),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ConcentrationBarChart(value: Double, limit: Double) {
    val maxVal = max(value, limit) * 1.25
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val barHeight = size.height * 0.45f
        val y = size.height * 0.35f
        val fullW = size.width

        // background track
        drawRoundRect(
            color = GrayBar.copy(alpha = 0.35f),
            topLeft = Offset(0f, y),
            size = Size(fullW, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )

        val valueW = ((value / maxVal).toFloat().coerceIn(0f, 1f)) * fullW
        val barColor = if (value < limit) Red else Green
        drawRoundRect(
            color = barColor,
            topLeft = Offset(0f, y),
            size = Size(valueW, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )

        // WHO limit line
        val limitX = ((limit / maxVal).toFloat().coerceIn(0f, 1f)) * fullW
        drawLine(
            color = Cyan,
            start = Offset(limitX, y - 8f),
            end = Offset(limitX, y + barHeight + 8f),
            strokeWidth = 3f
        )
    }
}

@Composable
private fun MotilityStackedBar(progressive: Double, nonProgressive: Double, immotile: Double) {
    val total = progressive + nonProgressive + immotile
    val safe = if (total <= 0.0) 1.0 else total
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val h = size.height
        var x = 0f
        val segments = listOf(
            progressive / safe to Green,
            nonProgressive / safe to Orange,
            immotile / safe to Red
        )
        segments.forEach { (frac, color) ->
            val w = (frac.toFloat() * size.width).coerceAtLeast(0f)
            drawRect(color = color, topLeft = Offset(x, 0f), size = Size(w, h))
            x += w
        }
    }
}

@Composable
private fun MorphologyBar(normalPercent: Double) {
    val normal = normalPercent.coerceIn(0.0, 100.0)
    val abnormal = 100.0 - normal
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val nW = (normal / 100.0).toFloat() * size.width
        drawRect(color = Green, topLeft = Offset(0f, 0f), size = Size(nW, size.height))
        drawRect(
            color = Red.copy(alpha = 0.75f),
            topLeft = Offset(nW, 0f),
            size = Size(size.width - nW, size.height)
        )
        // WHO ~4% marker
        val whoX = 0.04f * size.width
        drawLine(
            color = Cyan,
            start = Offset(whoX, -4f),
            end = Offset(whoX, size.height + 4f),
            strokeWidth = 2.5f
        )
    }
}

@Composable
private fun LegendRow(items: List<Pair<String, Color>>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        items.forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(color = color)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
