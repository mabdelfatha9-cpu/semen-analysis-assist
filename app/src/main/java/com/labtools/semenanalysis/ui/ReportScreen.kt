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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.labtools.semenanalysis.R
import com.labtools.semenanalysis.data.AppDatabase
import com.labtools.semenanalysis.data.SampleReportEntity
import com.labtools.semenanalysis.model.WhoReferenceLimits6thEdition
import kotlinx.coroutines.launch

@Composable
fun ReportScreen(
    sampleId: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).sampleReportDao() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var report by remember { mutableStateOf<SampleReportEntity?>(null) }
    var loading by remember { mutableStateOf(true) }

    var correctedText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var reviewSaved by remember { mutableStateOf(false) }

    LaunchedEffect(sampleId) {
        report = dao.getById(sampleId)
        loading = false
        report?.let {
            correctedText = it.humanCorrectedConcentration?.toString()
                ?: it.estimatedConcentrationMillionPerMl.toString()
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
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.report_concentration),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${\"%.1f\".format(current.estimatedConcentrationMillionPerMl)} مليون/مل",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${stringResource(R.string.report_who_limit)}: ${WhoReferenceLimits6thEdition.CONCENTRATION_MILLION_PER_ML} مليون/مل",
                    style = MaterialTheme.typography.bodySmall
                )
                if (current.concentrationBelowReferenceLimit) {
                    Text(
                        text = "⚠ أقل من الحد المرجعي",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "${stringResource(R.string.report_confidence)}: ${\"%.0f\".format(current.concentrationConfidenceScore * 100)}٪",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Motility section
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
                    if (current.motilityBelowReferenceLimit == true) {
                        Text(
                            text = "⚠ أقل من حد WHO للحركة",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (current.warnings.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.report_warnings),
                        style = MaterialTheme.typography.titleMedium
                    )
                    current.warnings.forEach { w ->
                        Text("• $w", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Human review section
        Text(
            text = stringResource(R.string.report_review_section),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = correctedText,
            onValueChange = { correctedText = it },
            label = { Text(stringResource(R.string.report_correct_concentration)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
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
                text = "تم حفظ المراجعة ✓",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val correctedValue = correctedText.toDoubleOrNull()
                scope.launch {
                    val updated = current.copy(
                        reviewedByHuman = true,
                        humanCorrectedConcentration = correctedValue,
                        reviewerNote = noteText.ifBlank { null }
                    )
                    dao.update(updated)
                    report = updated
                    reviewSaved = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.report_save_review))
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = percent?.let { "${\"%.1f\".format(it)}٪" } ?: "—",
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
    }
}
