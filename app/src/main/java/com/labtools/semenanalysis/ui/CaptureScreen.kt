package com.labtools.semenanalysis.ui

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.labtools.semenanalysis.R
import com.labtools.semenanalysis.data.AppDatabase
import com.labtools.semenanalysis.data.CalibrationStore
import com.labtools.semenanalysis.data.SampleReportEntity
import com.labtools.semenanalysis.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onAnalysisComplete: (sampleId: String) -> Unit,
    onRecalibrate: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val calibration = remember { CalibrationStore.load(context) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    if (!cameraPermissionState.status.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("التطبيق يحتاج إذن الكاميرا لتصوير العينة.")
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("منح الإذن")
            }
        }
        return
    }

    if (calibration == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.calibration_required_warning))
            Button(onClick = onRecalibrate) {
                Text(stringResource(R.string.nav_calibrate))
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.capture_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.capture_align_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val recorder = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HD))
                            .build()
                        val vc = VideoCapture.withOutput(recorder)
                        videoCapture = vc
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                vc
                            )
                        } catch (e: Exception) {
                            statusText = "خطأ في الكاميرا: ${e.message}"
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Alignment circle overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .then(
                        Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    )
            )
        }

        statusText?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }

        if (isRecording) {
            Text("جاري التسجيل... ${recordingSeconds}ث", modifier = Modifier.padding(8.dp))
        }

        Button(
            onClick = {
                val vc = videoCapture ?: return@Button
                if (!isRecording) {
                    val file = File(context.cacheDir, "sample_${System.currentTimeMillis()}.mp4")
                    val outputOptions = FileOutputOptions.Builder(file).build()
                    val executor = ContextCompat.getMainExecutor(context)
                    activeRecording = vc.output
                        .prepareRecording(context, outputOptions)
                        .start(executor) { event ->
                            when (event) {
                                is VideoRecordEvent.Start -> {
                                    isRecording = true
                                    recordingSeconds = 0
                                }
                                is VideoRecordEvent.Status -> {
                                    recordingSeconds = (event.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                                }
                                is VideoRecordEvent.Finalize -> {
                                    isRecording = false
                                    if (!event.hasError()) {
                                        recordedFile = file
                                        statusText = "تم التسجيل. اضغط رفع وتحليل."
                                    } else {
                                        statusText = "فشل التسجيل: ${event.cause?.message}"
                                    }
                                }
                                else -> {}
                            }
                        }
                } else {
                    activeRecording?.stop()
                    activeRecording = null
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            enabled = !isUploading
        ) {
            Text(
                if (isRecording) stringResource(R.string.capture_stop_recording)
                else stringResource(R.string.capture_start_recording)
            )
        }

        if (recordedFile != null && !isRecording) {
            Button(
                onClick = {
                    val file = recordedFile ?: return@Button
                    val cal = calibration ?: return@Button
                    isUploading = true
                    statusText = "جاري الرفع والتحليل..."
                    scope.launch {
                        try {
                            val sampleId = UUID.randomUUID().toString()
                            val requestFile = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                            val videoPart = MultipartBody.Part.createFormData("video", file.name, requestFile)
                            val mppBody = cal.micronsPerPixel.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                            val depthBody = cal.chamberDepthMicrons.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                            val dilutionBody = "1.0".toRequestBody("text/plain".toMediaTypeOrNull())

                            val concResponse = RetrofitClient.api.analyzeConcentration(
                                videoPart, mppBody, depthBody, dilutionBody
                            )
                            if (!concResponse.isSuccessful || concResponse.body() == null) {
                                statusText = "فشل تحليل التركيز: ${concResponse.code()}"
                                return@launch
                            }
                            val concentration = concResponse.body()!!

                            // Re-upload for motility (same file)
                            val requestFile2 = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                            val videoPart2 = MultipartBody.Part.createFormData("video", file.name, requestFile2)
                            val motilityResponse = try {
                                RetrofitClient.api.analyzeMotility(videoPart2, mppBody)
                            } catch (_: Exception) {
                                null
                            }
                            val motility = motilityResponse?.takeIf { it.isSuccessful }?.body()

                            val allWarnings = concentration.warnings + (motility?.warnings ?: emptyList())

                            val entity = SampleReportEntity(
                                sampleId = sampleId,
                                timestampEpochMillis = System.currentTimeMillis(),
                                framesAnalyzed = concentration.framesAnalyzed,
                                averageObjectsPerFrame = concentration.averageObjectsPerFrame,
                                estimatedConcentrationMillionPerMl = concentration.estimatedConcentrationMillionPerMl,
                                concentrationConfidenceScore = concentration.confidenceScore,
                                concentrationBelowReferenceLimit = concentration.belowReferenceLimit,
                                whoConcentrationLimitMillionPerMl = concentration.whoReferenceLimitMillionPerMl,
                                progressiveMotilityPercent = motility?.progressiveMotilityPercent,
                                nonProgressiveMotilityPercent = motility?.nonProgressiveMotilityPercent,
                                immotilePercent = motility?.immotilePercent,
                                totalMotilityPercent = motility?.totalMotilityPercent,
                                motilityConfidenceScore = motility?.confidenceScore,
                                motilityBelowReferenceLimit = motility?.belowReferenceLimit,
                                tracksAnalyzed = motility?.tracksAnalyzed,
                                warnings = allWarnings
                            )

                            AppDatabase.getInstance(context).sampleReportDao().insert(entity)
                            onAnalysisComplete(sampleId)
                        } catch (e: Exception) {
                            statusText = "خطأ في الاتصال بالسيرفر: ${e.message}"
                        } finally {
                            isUploading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading
            ) {
                Text(if (isUploading) "جاري الرفع..." else stringResource(R.string.capture_upload_button))
            }
        }

        Button(
            onClick = onRecalibrate,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.nav_calibrate))
        }
    }
}
