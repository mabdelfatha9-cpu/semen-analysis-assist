package com.labtools.semenanalysis.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
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

    val scope = rememberCoroutineScope()

    fun copyUriToCache(uri: Uri, suffix: String): File {
        val out = File(context.cacheDir, "upload_" + System.currentTimeMillis() + suffix)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        } ?: error("تعذر قراءة الملف")
        return out
    }

    suspend fun runVideoAnalysis(file: File) {
        val cal = calibration ?: return
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
            statusText = "فشل تحليل التركيز: " + concResponse.code()
            return
        }
        val concentration = concResponse.body()!!

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
    }

    suspend fun runImageAnalysis(file: File) {
        val cal = calibration ?: return
        val sampleId = UUID.randomUUID().toString()
        val mime = if (file.name.endsWith(".png", true)) "image/png" else "image/jpeg"
        val requestFile = file.asRequestBody(mime.toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
        val mppBody = cal.micronsPerPixel.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val depthBody = cal.chamberDepthMicrons.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val dilutionBody = "1.0".toRequestBody("text/plain".toMediaTypeOrNull())

        val concResponse = RetrofitClient.api.analyzeConcentrationImage(
            imagePart, mppBody, depthBody, dilutionBody
        )
        if (!concResponse.isSuccessful || concResponse.body() == null) {
            statusText = "فشل تحليل الصورة: " + concResponse.code() + " " + (concResponse.errorBody()?.string() ?: "")
            return
        }
        val concentration = concResponse.body()!!
        val entity = SampleReportEntity(
            sampleId = sampleId,
            timestampEpochMillis = System.currentTimeMillis(),
            framesAnalyzed = concentration.framesAnalyzed,
            averageObjectsPerFrame = concentration.averageObjectsPerFrame,
            estimatedConcentrationMillionPerMl = concentration.estimatedConcentrationMillionPerMl,
            concentrationConfidenceScore = concentration.confidenceScore,
            concentrationBelowReferenceLimit = concentration.belowReferenceLimit,
            whoConcentrationLimitMillionPerMl = concentration.whoReferenceLimitMillionPerMl,
            progressiveMotilityPercent = null,
            nonProgressiveMotilityPercent = null,
            immotilePercent = null,
            totalMotilityPercent = null,
            motilityConfidenceScore = null,
            motilityBelowReferenceLimit = null,
            tracksAnalyzed = null,
            warnings = concentration.warnings + listOf("تحليل صورة ثابتة — لا يشمل الحركة (motility).")
        )
        AppDatabase.getInstance(context).sampleReportDao().insert(entity)
        onAnalysisComplete(sampleId)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isUploading = true
        statusText = "جاري تحليل الصورة..."
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { copyUriToCache(uri, ".jpg") }
                runImageAnalysis(file)
            } catch (e: Exception) {
                statusText = "خطأ: " + (e.message ?: "")
            } finally {
                isUploading = false
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isUploading = true
        statusText = "جاري تحليل الفيديو..."
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { copyUriToCache(uri, ".mp4") }
                runVideoAnalysis(file)
            } catch (e: Exception) {
                statusText = "خطأ: " + (e.message ?: "")
            } finally {
                isUploading = false
            }
        }
    }

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
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                enabled = calibration != null && !isUploading
            ) {
                Text(stringResource(R.string.capture_pick_image))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
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
                            statusText = "خطأ في الكاميرا: " + (e.message ?: "")
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            )
        }

        statusText?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }

        if (isRecording) {
            Text("جاري التسجيل... " + recordingSeconds + "ث", modifier = Modifier.padding(8.dp))
        }

        Button(
            onClick = {
                val vc = videoCapture ?: return@Button
                if (!isRecording) {
                    val file = File(context.cacheDir, "sample_" + System.currentTimeMillis() + ".mp4")
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
                                        statusText = "فشل التسجيل: " + (event.cause?.message ?: "")
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
                    isUploading = true
                    statusText = "جاري الرفع والتحليل..."
                    scope.launch {
                        try {
                            runVideoAnalysis(file)
                        } catch (e: Exception) {
                            statusText = "خطأ في الاتصال بالسيرفر: " + (e.message ?: "")
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

        OutlinedButton(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !isUploading && !isRecording
        ) {
            Text(stringResource(R.string.capture_pick_image))
        }

        OutlinedButton(
            onClick = { videoPicker.launch("video/*") },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            enabled = !isUploading && !isRecording
        ) {
            Text(stringResource(R.string.capture_pick_video))
        }

        Button(
            onClick = onRecalibrate,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.nav_calibrate))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.powered_by),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00B4E4),
            fontSize = 14.sp
        )
        Text(
            text = stringResource(R.string.powered_by_subtitle),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp
        )
    }
}
