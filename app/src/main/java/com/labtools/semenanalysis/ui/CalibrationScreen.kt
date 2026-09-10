package com.labtools.semenanalysis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.labtools.semenanalysis.R
import com.labtools.semenanalysis.data.CalibrationStore

/**
 * One-time (per setup) calibration screen. The user photographs a known
 * stage micrometer so the backend can compute microns-per-pixel, which is
 * required before any concentration figure means anything at all.
 *
 * For this scaffold, manual entry is the primary path (it works without
 * a backend call). Photographing the micrometer and having the backend
 * compute the ratio automatically is wired through ApiService.calibrate()
 * and can be hooked up to a camera picker the same way CaptureScreen
 * captures video.
 */
@Composable
fun CalibrationScreen(
    onCalibrationSaved: () -> Unit,
    onViewHistory: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var micronsPerPixelText by remember { mutableStateOf("") }
    var chamberDepthText by remember { mutableStateOf("20") } // Makler chamber default: 20 microns
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.calibration_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.calibration_instructions),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.calibration_manual_entry), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = micronsPerPixelText,
            onValueChange = { micronsPerPixelText = it },
            label = { Text("ميكرومتر / بكسل") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = chamberDepthText,
            onValueChange = { chamberDepthText = it },
            label = { Text(stringResource(R.string.capture_chamber_depth_label)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        errorText?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val mpp = micronsPerPixelText.toDoubleOrNull()
                val depth = chamberDepthText.toDoubleOrNull()
                if (mpp == null || mpp <= 0.0 || depth == null || depth <= 0.0) {
                    errorText = "أدخل قيمًا رقمية صحيحة أكبر من صفر"
                    return@Button
                }
                CalibrationStore.save(context, mpp, depth)
                onCalibrationSaved()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.calibration_save))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onViewHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.nav_history))
        }
    }
}
