package com.labtools.semenanalysis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.labtools.semenanalysis.data.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun ClinicalDataScreen(
    sampleId: String,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).sampleReportDao() }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var patientName by remember { mutableStateOf("") }
    var patientAge by remember { mutableStateOf("") }
    var patientSex by remember { mutableStateOf("M") }
    var referredBy by remember { mutableStateOf("") }
    var centre by remember { mutableStateOf("") }
    var patientId by remember { mutableStateOf("") }
    var abstinenceDays by remember { mutableStateOf("") }

    var colour by remember { mutableStateOf("Whitish Gray") }
    var semenPh by remember { mutableStateOf("") }
    var volumeMl by remember { mutableStateOf("") }
    var viscosity by remember { mutableStateOf("NORMAL") }
    var appearance by remember { mutableStateOf("OPAQUE") }
    var liquefaction by remember { mutableStateOf("") }

    var fructose by remember { mutableStateOf("Positive") }
    var vitalityAlive by remember { mutableStateOf("") }
    var vitalityDead by remember { mutableStateOf("") }
    var pusCells by remember { mutableStateOf("") }
    var roundCells by remember { mutableStateOf("") }

    LaunchedEffect(sampleId) {
        val r = dao.getById(sampleId)
        r?.let {
            patientName = it.patientName ?: ""
            patientAge = it.patientAge ?: ""
            patientSex = it.patientSex ?: "M"
            referredBy = it.referredBy ?: ""
            centre = it.centre ?: ""
            patientId = it.patientId ?: ""
            abstinenceDays = it.abstinenceDays?.toString() ?: ""
            colour = it.colour ?: "Whitish Gray"
            semenPh = it.semenPh?.toString() ?: ""
            volumeMl = it.volumeMl?.toString() ?: ""
            viscosity = it.viscosity ?: "NORMAL"
            appearance = it.appearance ?: "OPAQUE"
            liquefaction = it.liquefactionTimeMin?.toString() ?: ""
            fructose = it.fructose ?: "Positive"
            vitalityAlive = it.vitalityAlivePercent?.toString() ?: ""
            vitalityDead = it.vitalityDeadPercent?.toString() ?: ""
            pusCells = it.pusCells ?: ""
            roundCells = it.roundCells ?: ""
        }
        loading = false
    }

    if (loading) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("جاري التحميل…")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "بيانات المريض والفحص السريري",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Powered by Mono Chrome",
            color = Color(0xFF00B4E4),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("بيانات المريض")
        Field(patientName, { patientName = it }, "اسم المريض (Patient Name)")
        Field(patientAge, { patientAge = it }, "العمر (Age)", KeyboardType.Number)
        Field(patientSex, { patientSex = it }, "الجنس (M / F)")
        Field(patientId, { patientId = it }, "رقم المريض (Patient ID)")
        Field(referredBy, { referredBy = it }, "محوَّل من (Referred By)")
        Field(centre, { centre = it }, "المركز / Centre")
        Field(
            abstinenceDays,
            { abstinenceDays = it },
            "مدة الامتناع بالأيام (Abstinence days)",
            KeyboardType.Decimal
        )

        Spacer(modifier = Modifier.height(12.dp))
        SectionTitle("الفحص الفيزيائي (Physical Features)")
        Field(colour, { colour = it }, "اللون (Colour)")
        Field(semenPh, { semenPh = it }, "pH", KeyboardType.Decimal)
        Field(volumeMl, { volumeMl = it }, "الحجم ml (Volume)", KeyboardType.Decimal)
        Field(viscosity, { viscosity = it }, "اللزوجة (Viscosity)")
        Field(appearance, { appearance = it }, "المظهر (Appearance)")
        Field(
            liquefaction,
            { liquefaction = it },
            "زمن السيولة بالدقائق (Liquefaction)",
            KeyboardType.Decimal
        )

        Spacer(modifier = Modifier.height(12.dp))
        SectionTitle("حقول إضافية (اختياري)")
        Field(fructose, { fructose = it }, "Fructose")
        Field(vitalityAlive, { vitalityAlive = it }, "Vitality Alive %", KeyboardType.Decimal)
        Field(vitalityDead, { vitalityDead = it }, "Vitality Dead %", KeyboardType.Decimal)
        Field(pusCells, { pusCells = it }, "Pus Cells")
        Field(roundCells, { roundCells = it }, "Round Cells")

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    val current = dao.getById(sampleId) ?: return@launch
                    val updated = current.copy(
                        patientName = patientName.ifBlank { null },
                        patientAge = patientAge.ifBlank { null },
                        patientSex = patientSex.ifBlank { null },
                        referredBy = referredBy.ifBlank { null },
                        centre = centre.ifBlank { null },
                        patientId = patientId.ifBlank { null },
                        abstinenceDays = abstinenceDays.toDoubleOrNull(),
                        colour = colour.ifBlank { null },
                        semenPh = semenPh.toDoubleOrNull(),
                        volumeMl = volumeMl.toDoubleOrNull(),
                        viscosity = viscosity.ifBlank { null },
                        appearance = appearance.ifBlank { null },
                        liquefactionTimeMin = liquefaction.toDoubleOrNull(),
                        fructose = fructose.ifBlank { null },
                        vitalityAlivePercent = vitalityAlive.toDoubleOrNull(),
                        vitalityDeadPercent = vitalityDead.toDoubleOrNull(),
                        pusCells = pusCells.ifBlank { null },
                        roundCells = roundCells.ifBlank { null }
                    )
                    dao.update(updated)
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ والمتابعة للتقرير")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = true
    )
}
