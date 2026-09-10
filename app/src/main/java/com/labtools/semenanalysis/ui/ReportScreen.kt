package com.labtools.semenanalysis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.labtools.semenanalysis.R
import com.labtools.semenanalysis.data.AppDatabase
import com.labtools.semenanalysis.data.SampleReportEntity
import com.labtools.semenanalysis.model.WhoReferenceLimits6thEdition
import com.labtools.semenanalysis.network.RetrofitClient
import com.labtools.semenanalysis.util.ReportPdfExporter
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun ReportScreen(
    sampleId: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).sampleReportDao() }
    val scope = rememberCoroutineScope()

    var report by remember { mutableStateOf<SampleReportEntity?>(null) }
    var loading by remember { mutableStateOf(true) }

    var correctedText by remember { mutableStateOf("") }
    var morphologyText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var reviewSaved by remember { mutableStateOf(false) }
    var serverMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sampleId) {
        report = dao.getById(sampleId)
        loading = false
        report?.let {
            correctedText = it.humanCorrectedConcentration?.toString()
                ?: it.estimatedConcentrationMillionPerMl.toString()
            morphologyText = (it.humanCorrectedNormalFormsPercent
                ?: it.estimatedNormalFormsPercent)?.toString() ?: ""
            noteText = it.reviewerNote ?: ""
        }
    }

    if (loading) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) { Text("جاري التحميل…") }
        return
    }

    val current = report
    if (current == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("لم يتم العثور على العينة")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.report_done))
            }
        }
        return
    }

    val displayConc = current.humanCorrectedConcentration
        ?: current.estimatedConcentrationMillionPerMl
    val concDisplay = String.format("%.1f", displayConc)
    val confidenceDisplay = String.format("%.0f", current.concentrationConfidenceScore * 100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.report_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.powered_by),
            color = Color(0xFF00B4E4),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.report_concentration),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = concDisplay + " مليون/مل",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(R.string.report_who_limit) + ": " +
                        WhoReferenceLimits6thEdition.CONCENTRATION_MILLION_PER_ML + " مليون/مل",
                    style = MaterialTheme.typography.bodySmall
                )
                if (displayConc < current.whoConcentrationLimitMillionPerMl) {
                    Text(
                        text = "⚠ أقل من الحد المرجعي",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = stringResource(R.string.report_confidence) + ": " + confidenceDisplay + "٪",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (current.totalMotilityPercent != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.report_motility),
                        style = MaterialTheme.typography.titleMedium
                    )
                    MotilityRow("تقدمية (PR)", current.progressiveMotilityPercent, emphasize = true)
                    MotilityRow("غير تقدمية (NP)", current.nonProgressiveMotilityPercent)
                    MotilityRow("غير متحركة (IM)", current.immotilePercent)
                    MotilityRow("إجمالي الحركة", current.totalMotilityPercent, emphasize = true)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.report_morphology),
                    style = MaterialTheme.typography.titleMedium
                )
                val forms = current.humanCorrectedNormalFormsPercent
                    ?: current.estimatedNormalFormsPercent
                if (forms != null) {
                    Text(
                        text = String.format("%.1f", forms) + "٪ أشكال طبيعية",
                        style = MaterialTheme.typography.headlineSmall
                    )
                } else {
                    Text(
                        text = "أدخل نسبة الأشكال الطبيعية بعد المراجعة اليدوية.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Charts based on results
        ResultChartsSection(report = current)

        Spacer(modifier = Modifier.height(12.dp))

        Text(stringResource(R.string.report_review_section), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.report_self_learning_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = correctedText,
            onValueChange = { correctedText = it },
            label = { Text(stringResource(R.string.report_correct_concentration)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = morphologyText,
            onValueChange = { morphologyText = it },
            label = { Text(stringResource(R.string.report_morphology_normal_percent)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text(stringResource(R.string.report_note)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        if (reviewSaved) {
            Text(
                text = "تم حفظ المراجعة محليًا ✓",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        serverMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val correctedValue = correctedText.toDoubleOrNull()
                val morphValue = morphologyText.toDoubleOrNull()
                scope.launch {
                    val updated = current.copy(
                        reviewedByHuman = true,
                        humanCorrectedConcentration = correctedValue,
                        humanCorrectedNormalFormsPercent = morphValue,
                        reviewerNote = noteText.ifBlank { null }
                    )
                    dao.update(updated)
                    report = updated
                    reviewSaved = true

                    try {
                        val plain = "text/plain".toMediaTypeOrNull()
                        val resp = RetrofitClient.api.submitFeedback(
                            sampleId = sampleId.toRequestBody(plain),
                            estimatedConcentration = current.estimatedConcentrationMillionPerMl
                                .toString().toRequestBody(plain),
                            humanConcentration = correctedValue?.toString()?.toRequestBody(plain),
                            humanNormalForms = morphValue?.toString()?.toRequestBody(plain),
                            reviewerNote = noteText.ifBlank { null }?.toRequestBody(plain)
                        )
                        serverMsg = if (resp.isSuccessful) {
                            resp.body()?.message ?: "تم رفع المراجعة للسيرفر (مكتبة التعلم)"
                        } else {
                            "حُفظ محليًا — رفع السيرفر فشل: " + resp.code()
                        }
                    } catch (e: Exception) {
                        serverMsg = "حُفظ محليًا — السيرفر غير متاح: " + (e.message ?: "")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.report_save_review))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { ReportPdfExporter.exportAndShare(context, current) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("طباعة / مشاركة التقرير PDF")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.report_done))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MotilityRow(label: String, percent: Double?, emphasize: Boolean = false) {
    val percentText = if (percent != null) String.format("%.1f", percent) + "٪" else "—"
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = percentText,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
    }
}
