package com.labtools.semenanalysis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.labtools.semenanalysis.R
import com.labtools.semenanalysis.data.AppDatabase
import com.labtools.semenanalysis.data.SampleReportEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lists every saved sample. This is the "library" the earlier discussion
 * promised: each row shows whether a technician has reviewed/corrected it
 * yet, since unreviewed rows are just computer guesses, not labeled data.
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenSample: (sampleId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).sampleReportDao() }
    val reports by dao.observeAll().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(text = stringResource(R.string.history_title), style = MaterialTheme.typography.headlineSmall)
        Text(text = stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (reports.isEmpty()) {
            Text(text = stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
                items(reports, key = { it.sampleId }) { report ->
                    HistoryRow(report = report, onClick = { onOpenSample(report.sampleId) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("رجوع")
        }
    }
}

@Composable
private fun HistoryRow(report: SampleReportEntity, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(report.timestampEpochMillis)),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${\"%.1f\".format(report.estimatedConcentrationMillionPerMl)} مليون/مل"
                        + (report.totalMotilityPercent?.let { "  •  حركة ${\"%.0f\".format(it)}٪" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val statusText = if (report.reviewedByHuman) "✓ تمت المراجعة" else "بانتظار المراجعة"
            val statusColor = if (report.reviewedByHuman) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
            Text(text = statusText, color = statusColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}
